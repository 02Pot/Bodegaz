package com.warehouse.system.Service.Auth;

import com.warehouse.system.DTO.Request.LoginRequest;
import com.warehouse.system.DTO.Request.RegisterRequest;
import com.warehouse.system.DTO.Request.SendOtpRequest;
import com.warehouse.system.DTO.Request.VerifyOtpRequest;
import com.warehouse.system.DTO.Response.AuthCheckResponse;
import com.warehouse.system.DTO.Response.LoginResponse;
import com.warehouse.system.DTO.Response.UserResponse;
import com.warehouse.system.DTO.TokenPair;
import com.warehouse.system.Enums.AuthAction;
import com.warehouse.system.Exception.UnauthorizedException;
import com.warehouse.system.Model.OtpModel;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Repository.OtpRepository;
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

    @Transactional
    public UserResponse register (RegisterRequest registerRequest){
        UserModel user = userModelRepository.findByEmail(registerRequest.email())
                .orElseThrow(() -> new IllegalArgumentException("No verified account found for this email"));

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
        if (userModelRepository.existsByEmail(request.email())) {
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

    public LoginResponse login(LoginRequest request) {
        UserModel user = userModelRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getPassword() == null) {
            throw new IllegalArgumentException("No password set for this account. Please set a password first.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return buildAuthResponse(user,"Login Success");
    }

    @GetMapping("/me")
    public AuthCheckResponse getCurrentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof UserModel user)) {
            throw new UnauthorizedException("Not authenticated");
        }

        return AuthCheckResponse.from(user);
    }


    public TokenPair refreshToken(String refreshTokenRequest) {
        String username = jwtService.extractUserName(refreshTokenRequest);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if(userDetails == null){
            throw new IllegalArgumentException("User not found");
        }

        if (!jwtService.isRefreshToken(refreshTokenRequest)) {
            throw new IllegalArgumentException("Provided token is not a refresh token");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        String accessToken = jwtService.generateAccessToken(authentication);
        return new TokenPair(accessToken,refreshTokenRequest);
    }

    private LoginResponse buildAuthResponse(UserModel user, String message) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        TokenPair tokenPair = jwtService.generateTokenPair(authentication);

        return LoginResponse.from(user, tokenPair, message);

    }


}
