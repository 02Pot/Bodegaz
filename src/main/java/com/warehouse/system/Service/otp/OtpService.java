package com.warehouse.system.Service.otp;

import com.warehouse.system.Model.OtpModel;
import com.warehouse.system.Repository.OtpRepository;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Utils.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpTokenRepository;
    private final UserModelRepository userRepository;
    private final OtpGenerator otpGenerator;
    private final EmailService emailService;

    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    private static final int MAX_ATTEMPTS = 3;


    @Transactional
    public void generateAndSendOtp(String email, String name) {
        otpTokenRepository.deleteAllByEmail(email);

        String otp = otpGenerator.generate();

        OtpModel token = OtpModel.builder()
                .email(email)
                .name(name)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .attemptCount(0)
                .build();

        otpTokenRepository.save(token);
        emailService.sendOtpEmail(email, otp, name);
    }

    @Transactional
    public OtpModel verifyOtp(String inputOtp) {
        OtpModel token = otpTokenRepository
                .findTopByOtpAndUsedFalseOrderByCreatedAtDesc(inputOtp)
                .orElseThrow(() -> new RuntimeException("No active OTP found. Please request a new one."));

        if (token.isExpired()) {
            otpTokenRepository.delete(token);
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        if (token.getAttemptCount() >= MAX_ATTEMPTS) {
            otpTokenRepository.delete(token);
            throw new RuntimeException("Maximum attempts exceeded. Please request a new OTP.");
        }

        if (!token.getOtp().equals(inputOtp)) {
            token.setAttemptCount(token.getAttemptCount() + 1);
            otpTokenRepository.save(token);
            int remaining = MAX_ATTEMPTS - token.getAttemptCount();
            throw new RuntimeException("Invalid OTP. " + remaining + " attempt(s) remaining.");
        }

        token.setUsed(true);
        otpTokenRepository.save(token);
        return token;
    }


    @Scheduled(fixedRate = 600_000)
    public void cleanExpiredOtps() {
        otpTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

}
