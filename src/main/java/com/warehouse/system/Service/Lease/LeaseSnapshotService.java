package com.warehouse.system.Service.Lease;

import com.warehouse.system.DTO.Lease.*;
import com.warehouse.system.DTO.Lease.Response.LeaseSummaryResponse;
import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Enums.LeaseStatus;
import com.warehouse.system.Model.*;
import com.warehouse.system.Repository.LeaseAgreementRepository;
import com.warehouse.system.Repository.StorageBookingRepository;
import com.warehouse.system.Repository.StorageRepository;
import com.warehouse.system.Utils.Computations;
import com.warehouse.system.Utils.LeaseTextRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LeaseSnapshotService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final LeaseTemplateService templateService;
    private final LeaseAgreementRepository leaseRepo;
    private final StorageBookingRepository bookingRepo;
    private final LeaseTextRenderer renderer;
    private final StorageRepository storageRepository;
    private final Clock clock;
    private final Computations computations;
    @Value("${booking.request-expiry-hours:48}")
    private long requestExpiryHours;

    private static final Set<String> VOLATILE_KEYS = Set.of("lease_number", "agreement_date", "unit_number");

    @Value("${lease.currency:PHP}")
    private String currency;

    @Transactional(propagation = Propagation.MANDATORY)
    public LeaseAgreement createForBooking(StorageBooking booking) {
        if (leaseRepo.findByStorageBooking_OrderId(booking.getOrderId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease already exists for this booking");
        }

        Storage storage = booking.getStorage();
        Warehouse warehouse = storage.getWarehouse();

        LeaseTemplate template = templateService.resolveForBooking(null, null);

        LeaseTemplateConfig config = template.getConfig();
        String agreementNumber = generateAgreementNumber();

        Map<String, String> placeholders = buildPlaceholders(booking, storage, warehouse, config, agreementNumber);
        LeaseTermsSnapshot snapshot = new LeaseTermsSnapshot(config, template.getClauses(), placeholders);
        String renderedText = renderer.render(snapshot);

        LeaseAgreement lease = new LeaseAgreement();
        lease.setStorageBooking(booking);
        lease.setAgreementNumber(agreementNumber);
        lease.setTemplateId(template.getId());
        lease.setTemplateVersion(template.getVersion());
        lease.setTermsSnapshot(snapshot);
        lease.setTermsConditions(renderedText);
        lease.setTermsHash(sha256(renderedText));
        lease.setLeaseStatus(LeaseStatus.REVIEWING);

        booking.setLeaseAgreement(lease);
        return leaseRepo.save(lease);
    }

    @Transactional(readOnly = true)
    public LeaseView getLeaseView(UUID bookingId, UUID requesterUserId) {
        StorageBooking booking = loadBooking(bookingId);
        UUID renterId = booking.getUser().getId();
        UUID ownerId = booking.getStorage().getWarehouse().getUser().getId();
        if (!requesterUserId.equals(renterId) && !requesterUserId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this lease");
        }
        LeaseAgreement lease = loadLease(bookingId);
        return LeaseView.from(lease);
    }

    @Transactional
    public LeaseAgreement acceptTerms(UUID bookingId, UUID renterUserId, String clientIp, String termsHash) {
        LocalDateTime now = LocalDateTime.now(clock);
        StorageBooking booking = loadBooking(bookingId);

        if (!booking.getUser().getId().equals(renterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the renter can accept these terms");
        }
        if (booking.getStatus() != BookingStatus.PENDING_TERMS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is not awaiting terms acceptance");
        }
        if (booking.getHoldExpiresAt() != null && now.isAfter(booking.getHoldExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "The hold on this storage has expired");
        }

        LeaseAgreement lease = loadLease(bookingId);
        if (lease.getLeaseStatus() != LeaseStatus.REVIEWING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease has already been accepted");
        }
        if (!MessageDigest.isEqual(lease.getTermsHash().getBytes(StandardCharsets.UTF_8),
                termsHash.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The terms shown do not match the current agreement. Please reload and review again.");
        }

        lease.setSignDate(now);
        lease.setAcceptedBy(renterUserId);
        lease.setAcceptedIp(clientIp);
        lease.setLeaseStatus(LeaseStatus.ACCEPTED);

        booking.setStatus(BookingStatus.PENDING_OWNER);
        booking.setHoldExpiresAt(now.plusHours(requestExpiryHours));
        bookingRepo.save(booking);
        return leaseRepo.save(lease);
    }

    @Transactional(readOnly = true)
    public LeasePreview preview(PreviewLease req, UserModel renter) {
        Storage storage = storageRepository.findById(req.storageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Storage not found"));

        //PASS THE START, END DATE and THE storage price per cubic m3 and request rented
        BigDecimal totalPrice = computations.billableMonths(
            req.startDate().atStartOfDay(),
            req.endDate().atStartOfDay(),
            BigDecimal.valueOf(req.rentedWeightKg()),
            req.rentedVolumeCubicMeter()
        );

        StorageBooking draft = new StorageBooking();
        draft.setUser(renter);
        draft.setStorage(storage);
        draft.setStartDate(req.startDate().atStartOfDay());
        draft.setEndDate(req.endDate().atStartOfDay());
        draft.setRentedVolumeCubicMeter(req.rentedVolumeCubicMeter());
        draft.setRentedWeightKg(req.rentedWeightKg());
        draft.setUnitNumber("Assigned on booking");
        draft.setTotalPrice(totalPrice);

        LeaseTemplate template = templateService.resolveForBooking(null, null);
        LeaseTemplateConfig config = template.getConfig();

        Map<String, String> placeholders = buildPlaceholders(
                draft, storage, storage.getWarehouse(), config, "Assigned after you accept");
        String text = renderer.render(new LeaseTermsSnapshot(config, template.getClauses(), placeholders));

        return new LeasePreview(text, placeholders, fingerprint(template, placeholders));
    }

    String fingerprint(LeaseTemplate template, Map<String, String> placeholders) {
        Map<String, String> stable = new TreeMap<>(placeholders);
        VOLATILE_KEYS.forEach(stable::remove);
        return sha256(template.getId() + ":" + template.getVersion() + ":" + stable);
    }

    @Transactional(readOnly = true)
    public List<LeaseSummaryResponse> listRenterLeases(UUID renterUserId) {
        return leaseRepo.findByStorageBooking_User_IdOrderByCreatedAtDesc(renterUserId)
                .stream().map(LeaseSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaseSummaryResponse> listOwnerLeases(UUID ownerUserId) {
        return leaseRepo.findByStorageBooking_Storage_Warehouse_User_IdOrderByCreatedAtDesc(ownerUserId)
                .stream().map(LeaseSummaryResponse::from).toList();
    }

    private Map<String, String> buildPlaceholders(StorageBooking booking, Storage storage, Warehouse warehouse,
                                                  LeaseTemplateConfig config, String agreementNumber) {
        BigDecimal volume = BigDecimal.valueOf(booking.getRentedVolumeCubicMeter());
        BigDecimal monthlyRent = storage.getPricePerCubicMeter().multiply(volume)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal rentPerCycle = monthlyRent.multiply(BigDecimal.valueOf(monthsIn(config.billingCycle())));

        BigDecimal deposit = switch (config.deposit().type()) {
            case MONTHS_OF_RENT -> monthlyRent.multiply(config.deposit().value());
            case FIXED -> config.deposit().value();
        };

        UserModel tenant = booking.getUser();
        UserModel owner = warehouse.getUser();
        String warehouseAddress = formatAddress(warehouse.getWarehouseAddress());

        Map<String, String> p = new LinkedHashMap<>();
        p.put("lease_number", agreementNumber);
        p.put("agreement_date", LocalDate.now().format(DATE_FMT));
        p.put("company_name", ownerName(owner));
        p.put("company_address", warehouseAddress);
        p.put("tenant_name", tenantName(tenant));
        p.put("tenant_contact", tenantContact(tenant));

        p.put("unit_number", booking.getUnitNumber());
        p.put("block_name", storage.getBlockName());
        p.put("section", storage.getSection() == null ? "" : storage.getSection());
        p.put("rented_volume", plain(volume));
        p.put("rented_weight", plain(BigDecimal.valueOf(booking.getRentedWeightKg())));
        p.put("warehouse_name", warehouse.getName());
        p.put("warehouse_address", warehouseAddress);

        p.put("start_date", booking.getStartDate().format(DATE_FMT));
        p.put("end_date", booking.getEndDate().format(DATE_FMT));
        p.put("notice_days", String.valueOf(config.noticeDaysToTerminate()));

        p.put("rent_amount", money(rentPerCycle));
        p.put("billing_cycle", config.billingCycle().name().toLowerCase(Locale.ENGLISH));
        p.put("billing_day", String.valueOf(config.billingDay()));
        p.put("grace_period_days", String.valueOf(config.gracePeriodDays()));
        p.put("late_fee", formatLateFee(config.lateFee()));
        p.put("total_price", money(booking.getTotalPrice()));

        p.put("deposit_amount", money(deposit));
        p.put("deposit_refund_days", String.valueOf(config.depositRefundDays()));

        p.put("permitted_items", String.join(", ", config.permittedItems()));
        p.put("access_hours", config.accessHours());
        p.put("default_days", String.valueOf(config.defaultDays()));
        p.put("insurance_notice", config.insuranceRequired()
                ? "The Tenant must maintain insurance on stored goods. " : "");
        return p;
    }

    private String tenantName(UserModel u)    {
        return u.getName();
    }
    private String tenantContact(UserModel u) {
        return u.getEmail();
    }
    private String ownerName(UserModel u)     {
        return u.getName();
    }

    private String formatAddress(WarehouseAddress a) {
        return Stream.of(a.getAddressLine1(), a.getAddressLine2(), a.getCity(),
                        a.getStateProvince(), a.getPostalCode(), a.getCountry())
                .filter(s -> s != null && !s.isBlank())
                .map(String::strip)
                .collect(Collectors.joining(", "));
    }

    private int monthsIn(LeaseTemplateConfig.BillingCycle cycle) {
        return switch (cycle) { case MONTHLY -> 1; case QUARTERLY -> 3; case ANNUAL -> 12; };
    }

    private String formatLateFee(LeaseTemplateConfig.LateFee fee) {
        return switch (fee.type()) {
            case PERCENT -> plain(fee.value()) + "% of the overdue amount";
            case FIXED -> money(fee.value());
        };
    }

    private String money(BigDecimal amount) {
        return String.format(Locale.ENGLISH, "%s %,.2f", currency, amount.setScale(2, RoundingMode.HALF_UP));
    }

    private String plain(BigDecimal n) {
        return n.stripTrailingZeros().toPlainString();
    }

    private String generateAgreementNumber() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ENGLISH);
        return "LA-" + LocalDate.now().getYear() + "-" + suffix;
    }

    private String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private StorageBooking loadBooking(UUID bookingId) {
        return bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private LeaseAgreement loadLease(UUID bookingId) {
        return leaseRepo.findByStorageBooking_OrderId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found"));
    }
}
