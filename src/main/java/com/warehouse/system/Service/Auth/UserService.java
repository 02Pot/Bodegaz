package com.warehouse.system.Service.Auth;

import com.warehouse.system.DTO.Request.LoginRequest;
import com.warehouse.system.DTO.Request.RegisterRequest;
import com.warehouse.system.DTO.Request.SendOtpRequest;
import com.warehouse.system.DTO.Request.VerifyOtpRequest;
import com.warehouse.system.DTO.Response.AuthCheckResponse;
import com.warehouse.system.DTO.Response.LoginResponse;
import com.warehouse.system.DTO.Response.UserResponse;
import com.warehouse.system.DTO.Result.AuthResult;
import com.warehouse.system.DTO.TokenPair;
import com.warehouse.system.Enums.AuthAction;
import com.warehouse.system.Exception.InvalidCredentials;
import com.warehouse.system.Exception.RefreshTokenReuseException;
import com.warehouse.system.Exception.UnauthorizedException;
import com.warehouse.system.Model.OtpModel;
import com.warehouse.system.Model.RefreshTokenModel;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Repository.OtpRepository;
import com.warehouse.system.Repository.RefreshTokenRepository;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Service.otp.EmailService;
import com.warehouse.system.Service.otp.OtpService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;


@Service
@RequiredArgsConstructor
public class UserService {

    private final JwtFilterService jwtService;
    private final UserModelRepository userModelRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public UserResponse register (RegisterRequest registerRequest){
        UserModel user = userModelRepository.findByEmail(registerRequest.email())
                .orElseThrow(InvalidCredentials::new);

        user.setUserType(registerRequest.userType());
        user.setContactNumber(registerRequest.contactNumber());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setRegistered(true);

        userModelRepository.save(user);
        otpRepository.deleteAllByEmail(registerRequest.email());
        return new UserResponse(true, "Registration complete", AuthAction.ONBOARD);
    }

    @Transactional
    public UserResponse sendOtp(SendOtpRequest request) {
//        UserModel user = userModelRepository.findByEmail(request.email()).orElseThrow(() -> new RuntimeException("No account found"));

        if (userModelRepository.existsByEmail(request.email()) ) {
            emailService.sendAccountExist(request.email());
        }else{
            otpService.generateAndSendOtp(request.email(), request.name());
        }
        return new UserResponse(true, "OTP sent to " + request.email() + ". Please verify to continue.",AuthAction.OTP_SENT);
    }

    @Transactional
    public UserResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        OtpModel otp = otpService.verifyOtp(request.otp());
        userModelRepository.findByEmail(otp.getEmail())
                .orElseGet(() -> userModelRepository.save(UserModel.builder()
                        .email(otp.getEmail())
                        .name(otp.getName())
                        .isVerified(true)
                        .isRegistered(false)
                        .build()));
        return new UserResponse(true, "Verified Success",AuthAction.ONBOARDING);
    }

    public AuthResult login(LoginRequest request) {
        UserModel user = userModelRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentials::new);

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentials();
        }

        return buildAuthResponse(user,"Login Success");
    }

    @Transactional
    public void logout(String presentedToken, boolean allDevices) {
        if (presentedToken == null || presentedToken.isBlank()) return;
        refreshTokenRepository.findByToken(hash(presentedToken)).ifPresent(stored -> {
            if (allDevices) {
                refreshTokenRepository.deleteAllByUserAndRevokedFalse(stored.getUser());
            } else {
                stored.setRevoked(true);
                refreshTokenRepository.save(stored);
            }
        });
    }

    @GetMapping("/me")
    public AuthCheckResponse getCurrentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken)
            throw new UnauthorizedException("Not authenticated");

         UserModel user = userModelRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        return AuthCheckResponse.from(user);
    }


    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public TokenPair refreshToken(String presentedToken) {
        if (!jwtService.isRefreshToken(presentedToken)) {
            throw new IllegalArgumentException("Provided token is not a refresh token");
        }

        String username = jwtService.extractUserName(presentedToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        RefreshTokenModel stored = refreshTokenRepository.findByToken(hash(presentedToken))
                .orElseThrow(() -> new RefreshTokenReuseException("Refresh Token not recognized"));

        if (stored.isRevoked()) {
            refreshTokenRepository.deleteAllByUserAndRevokedFalse(stored.getUser());
            throw new RefreshTokenReuseException("Refresh token reuse detected");
        }

        if (stored.getExpireDate().isBefore(Instant.now())) {
            throw new RefreshTokenReuseException("Refresh token expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken  = jwtService.generateAccessToken(authentication);
        String newRefreshToken = jwtService.generateRefreshToken(authentication);
        refreshTokenRepository.save(RefreshTokenModel.builder()
                .token(hash(newRefreshToken))
                .expireDate(Instant.now().plus(Duration.ofDays(7)))
                .revoked(false)
                .user(stored.getUser())
                .build());

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    private AuthResult buildAuthResponse(UserModel user, String message) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        TokenPair tokenPair = jwtService.generateTokenPair(authentication);

        return new AuthResult(LoginResponse.from(user,message),tokenPair);

    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
