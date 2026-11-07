package com.warehouse.system.Service.Lease;


import com.warehouse.system.DTO.Lease.*;
import com.warehouse.system.DTO.Lease.Request.CreateLeaseTemplateRequest;
import com.warehouse.system.DTO.Lease.Request.UpdateLeaseTemplateRequest;
import com.warehouse.system.Exception.UnauthorizedException;
import com.warehouse.system.Model.LeaseTemplate;
import com.warehouse.system.Model.Storage;
import com.warehouse.system.Repository.LeaseTemplateRepository;
import com.warehouse.system.Utils.DefaultLeaseTemplate;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaseTemplateService {

    private final LeaseTemplateRepository repo;

    @Transactional
    public LeaseTemplate ensurePlatformDefault() {
        return repo.findFirstByPlatformDefaultTrueAndActiveTrue().orElseGet(() -> {
            LeaseTemplate t = build(null, DefaultLeaseTemplate.NAME,
                    DefaultLeaseTemplate.config(), DefaultLeaseTemplate.clauses());
            t.setPlatformDefault(true);
            return repo.save(t);
        });
    }

    @Transactional
    public LeaseTemplate createTemplate(UUID ownerId, CreateLeaseTemplateRequest req) {
        return repo.save(build(ownerId, req.name(), req.config(), req.clauses()));
    }

    @Transactional
    public LeaseTemplate cloneDefaultForOwner(UUID ownerId, String name,
                                              LeaseTemplateConfig config, List<LeaseClause> clauses) {
        LeaseTemplate base = getPlatformDefault();
        return repo.save(build(ownerId, name,
                config != null ? config : base.getConfig(),
                clauses != null ? clauses : base.getClauses()));
    }

    @Transactional
    public LeaseTemplate updateTemplate(UUID ownerId, UUID id, UpdateLeaseTemplateRequest req) {
        LeaseTemplate t = getOwnedTemplate(ownerId, id);
        if (req.name() != null) t.setName(req.name());
        if (req.config() != null) t.setConfig(req.config());
        if (req.clauses() != null) t.setClauses(req.clauses());
        t.setVersion(t.getVersion() + 1);
        return repo.save(t);
    }

    @Transactional
    public LeaseTemplate deactivateTemplate(UUID ownerId, UUID id) {
        LeaseTemplate t = getOwnedTemplate(ownerId, id);
        t.setActive(false);
        return repo.save(t);
    }

    @Transactional(readOnly = true)
    public LeaseTemplate getTemplate(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + id));
    }

    @Transactional(readOnly = true)
    public LeaseTemplate getTemplateFor(UUID userId, UUID id) {
        LeaseTemplate t = getTemplate(id);
        boolean platformLevel = t.getOwnerId() == null;
        if (!platformLevel && !userId.equals(t.getOwnerId())) {
            throw new UnauthorizedException("Not your template");
        }
        return t;
    }

    @Transactional(readOnly = true)
    public List<LeaseTemplate> listMine(UUID ownerId) {
        return repo.findByOwnerIdAndActiveTrueOrderByCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public LeaseTemplate getPlatformDefaultTemplate() {
        return getPlatformDefault();
    }

    @Transactional(readOnly = true)
    public LeaseTemplate resolveForBooking(UUID storageTemplateId, UUID warehouseTemplateId) {
        for (UUID id : new UUID[]{storageTemplateId, warehouseTemplateId}) {
            if (id == null) continue;
            var found = repo.findByIdAndActiveTrue(id);
            if (found.isPresent()) return found.get();
        }
        return getPlatformDefault();
    }

    private LeaseTemplate build(UUID ownerId, String name,
                                LeaseTemplateConfig config, List<LeaseClause> clauses) {
        LeaseTemplate t = new LeaseTemplate();
        t.setOwnerId(ownerId);
        t.setName(name);
        t.setConfig(config);
        t.setClauses(clauses);
        return t;
    }

    private LeaseTemplate getPlatformDefault() {
        return repo.findFirstByPlatformDefaultTrueAndActiveTrue()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Platform default template missing; ensurePlatformDefault() has not run"));
    }

    private LeaseTemplate getOwnedTemplate(UUID userId, UUID id) {
        LeaseTemplate t = getTemplate(id);
        if (!userId.equals(t.getOwnerId())) {
            throw new UnauthorizedException("Not your template");
        }
        return t;
    }
}
