package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Request.LoginRequest;
import com.warehouse.system.DTO.Request.RegisterRequest;
import com.warehouse.system.DTO.Request.SendOtpRequest;
import com.warehouse.system.DTO.Request.VerifyOtpRequest;
import com.warehouse.system.DTO.Response.AuthCheckResponse;
import com.warehouse.system.DTO.Response.LoginResponse;
import com.warehouse.system.DTO.Response.UserResponse;
import com.warehouse.system.DTO.TokenPair;
import com.warehouse.system.Enums.AuthAction;
import com.warehouse.system.Model.RefreshTokenModel;
import com.warehouse.system.Service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService service;

    @PostMapping("/otp/send")
    public ResponseEntity<UserResponse> sendOtp(@Valid @RequestBody SendOtpRequest request){
        return ResponseEntity.ok(service.sendOtp(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<UserResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(service.verifyOtpAndLogin(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = service.login(request);
        ResponseCookie cookie = ResponseCookie.from("accessToken",loginResponse.getToken().getAccessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/me")
    public AuthCheckResponse authCheck(Authentication authentication) {
        return service.getCurrentUser(authentication);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@CookieValue("refreshToken") String refreshTokenRequest) {
        TokenPair token = service.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(token);
    }

    @GetMapping("/health")
    public ResponseEntity<UserResponse> health() {
        return ResponseEntity.ok(new UserResponse(true, "OTP Auth Service is running!", AuthAction.HEALTH_CHECK));
    }

}
