package com.warehouse.system.Controller;

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
import com.warehouse.system.Exception.RefreshTokenReuseException;
import com.warehouse.system.Service.Auth.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult authResult = service.login(request);

        ResponseCookie access = ResponseCookie.from("accessToken",authResult.tokenPair().getAccessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(service.getAccessTtl())
                .build();

        ResponseCookie refresh = ResponseCookie.from("refreshToken",authResult.tokenPair().getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth/refresh-token")
                .maxAge(service.getRefreshTtl())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, access.toString())
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .body(authResult.body());
    }

    @GetMapping("/me")
    public AuthCheckResponse authCheck(Authentication authentication) {
        return service.getCurrentUser(authentication);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Void> refreshToken(@CookieValue(value = "refreshToken",required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        TokenPair pair;
        try {
            pair = service.refreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException | RefreshTokenReuseException
                 | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clear("refreshToken", "/api/auth/refresh-token").toString())
                    .header(HttpHeaders.SET_COOKIE, clear("accessToken", "/").toString())
                    .build();
        }

        ResponseCookie access = ResponseCookie.from("accessToken", pair.getAccessToken())
                .httpOnly(true).secure(true).sameSite("None")
                .path("/").maxAge(service.getAccessTtl()).build();

        ResponseCookie refresh = ResponseCookie.from("refreshToken", pair.getRefreshToken())
                .httpOnly(true).secure(true).sameSite("None")
                .path("/api/auth/refresh-token").maxAge(service.getRefreshTtl()).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, access.toString())
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @RequestParam(defaultValue = "false") boolean allDevices) {

        service.logout(refreshToken, allDevices);

        ResponseCookie refresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth/refresh-token")
                .maxAge(0)
                .build();

        ResponseCookie access = ResponseCookie.from("accessToken","")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .header(HttpHeaders.SET_COOKIE, access.toString())
                .build();
    }

    @GetMapping("/health")
    public ResponseEntity<UserResponse> health() {
        return ResponseEntity.ok(new UserResponse(true, "OTP Auth Service is running!", AuthAction.HEALTH_CHECK));
    }

    private ResponseCookie clear(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true).secure(true).sameSite("None")
                .path(path).maxAge(0).build();
    }
}
