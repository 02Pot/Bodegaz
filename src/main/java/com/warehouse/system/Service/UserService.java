package com.warehouse.system.Service;

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
import com.warehouse.system.Model.RefreshTokenModel;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Repository.OtpRepository;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Service.otp.EmailService;
import com.warehouse.system.Service.otp.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

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
        UserModel user = UserModel.builder()
                .userType(registerRequest.getUserType())
                .contactNumber(registerRequest.getContactNumber())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .isRegistered(true)
                .build();
        userModelRepository.save(user);
        otpRepository.deleteAllByEmail(registerRequest.getEmail());
        return new UserResponse(true, "Registration complete", AuthAction.ONBOARD);
    }

    @Transactional
    public UserResponse sendOtp(SendOtpRequest request) {
        if (userModelRepository.existsByEmail(request.getEmail())) {
            emailService.sendAccountExist(request.getEmail());
        }else{
            otpService.generateAndSendOtp(request.getEmail(), request.getName());
        }
        return new UserResponse(true, "OTP sent to " + request.getEmail() + ". Please verify to continue.",AuthAction.OTP_SENT);
    }

    @Transactional
    public UserResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        OtpModel otp = otpService.verifyOtp(request.getOtp());
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
        UserModel user = userModelRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getPassword() == null) {
            throw new IllegalArgumentException("No password set for this account. Please set a password first.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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

        return AuthCheckResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType() != null ? user.getUserType().name() : null)
                .isVerified(user.isVerified())
                .isRegistered(user.isRegistered())
                .build();
    }


    public TokenPair refreshToken(RefreshTokenModel refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getToken();
        String username = jwtService.extractUserName(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if(userDetails == null){
            throw new IllegalArgumentException("User not found");
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        if (!jwtService.isRefreshToken(refreshTokenRequest.getToken())) {
            throw new IllegalArgumentException("Provided token is not a refresh token");
        }
        String accessToken = jwtService.generateAccessToken(authentication);
        return new TokenPair(accessToken,refreshToken);
    }

    private LoginResponse buildAuthResponse(UserModel user, String message) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        TokenPair tokenPair = jwtService.generateTokenPair(authentication);

        return LoginResponse.builder()
                .token(tokenPair)
                .id(user.getId())
                .email(user.getEmail())
                .userType(user.getUserType() != null ? user.getUserType() : null)
                .name(user.getName())
                .message(message)
                .build();
    }


}
