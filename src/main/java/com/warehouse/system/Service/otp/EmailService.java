package com.warehouse.system.Service.otp;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendAccountExist(String toEmail){
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Someone tried to login in your account");
            helper.setText(buildSecurityAlertEmailBody(), true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send send email. Please try again.");
        }
    }

    public void sendOtpEmail(String toEmail, String otp, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your OTP Code - Login Verification");
            helper.setText(buildEmailBody(name, otp), true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    private String buildEmailBody(String name, String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 520px; margin: 40px auto; background: #ffffff;
                                     border-radius: 10px; padding: 40px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { text-align: center; margin-bottom: 30px; }
                        .header h2 { color: #333333; }
                        .otp-box { background: #f0f4ff; border: 2px dashed #4f46e5;
                                   border-radius: 8px; text-align: center; padding: 20px; margin: 20px 0; }
                        .otp-code { font-size: 42px; font-weight: bold; color: #4f46e5; letter-spacing: 8px; }
                        .note { color: #666666; font-size: 14px; margin-top: 20px; }
                        .warning { color: #e53e3e; font-size: 13px; margin-top: 10px; }
                        .footer { text-align: center; color: #999; font-size: 12px; margin-top: 30px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>🔐 Login Verification</h2>
                        </div>
                        <p>Hi <strong>%s</strong>,</p>
                        <p>Use the OTP code below to complete your login. This code is valid for <strong>5 minutes</strong>.</p>
                        <div class="otp-box">
                            <div class="otp-code">%s</div>
                        </div>
                        <p class="note">Enter this code on the verification page to sign in to your account.</p>
                        <p class="warning">⚠️ Do not share this code with anyone. We will never ask for your OTP.</p>
                        <div class="footer">
                            <p>If you didn't request this, please ignore this email.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, otp);
    }

    private String buildSecurityAlertEmailBody() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 520px; margin: 40px auto; background: #ffffff;
                                     border-radius: 10px; padding: 40px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { text-align: center; margin-bottom: 30px; }
                        .header h2 { color: #e53e3e; margin-bottom: 5px; }
                        .alert-box { background: #fff5f5; border-left: 4px solid #e53e3e;
                                     border-radius: 4px; padding: 15px 20px; margin: 20px 0; }
                        .details { margin: 10px 0; font-size: 15px; color: #333; line-height: 1.6; }
                        .details strong { color: #111; display: inline-block; width: 80px; }
                        .action-btn { display: block; width: 100%%; max-width: 250px; margin: 30px auto;
                                      background-color: #e53e3e; color: #ffffff; text-align: center;
                                      padding: 14px 20px; text-decoration: none; border-radius: 6px;
                                      font-weight: bold; font-size: 16px; }
                        .action-btn:hover { background-color: #c53030; }
                        .note { color: #666666; font-size: 14px; margin-top: 20px; line-height: 1.5; }
                        .footer { text-align: center; color: #999; font-size: 12px; margin-top: 30px;
                                  border-top: 1px solid #eee; padding-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>🚨 Security Alert</h2>
                            <p style="color: #666; margin-top: 0;">New Login Attempt Detected</p>
                        </div>
                        <p>Hi <strong>%s</strong>,</p>
                        <p>We noticed a recent login attempt to your account from an unrecognized device. Here are the details:</p>
                        
                        <div class="alert-box">
                            <div class="details"><strong>Device:</strong> %s</div>
                            <div class="details"><strong>Location:</strong> %s</div>
                            <div class="details"><strong>Time:</strong> %s</div>
                        </div>
                        
                        <p class="note">If this was you, you can safely ignore this email. No further action is required.</p>
                        <p class="note" style="color: #e53e3e; font-weight: bold;">If you do not recognize this activity, please secure your account immediately.</p>
                        
                        <a href="%s" class="action-btn">Secure My Account</a>
                        
                        <div class="footer">
                            <p>To keep your account secure, we recommend updating your password regularly and never sharing your login details.</p>
                        </div>
                    </div>
                </body>
                </html>
                """;
    }

}
