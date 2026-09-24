package com.warehouse.system.Utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {

    @Value("${otp.length:6}")
    private int otpLength;

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(RANDOM.nextInt(10));
        }
        return otp.toString();
    }

}
