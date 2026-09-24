package com.warehouse.system.Service;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import javax.crypto.SecretKey;

import com.warehouse.system.DTO.TokenPair;
import com.warehouse.system.Model.RefreshTokenModel;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtFilterService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;
    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Map<String, String> claims = new HashMap<>();
        String role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(authority -> authority.getAuthority())
                .orElseThrow(null);

        claims.put("role", role);
        return generateToken(
                authentication,
                jwtExpiration,
                claims
        );
    }

    private String generateToken(Authentication authentication,long expiration,Map<String,String> claims) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        Map<String,String> claims = new HashMap<>();
        claims.put("tokenType","refresh");

        return generateToken(authentication,refreshExpiration,claims);
    }

    public TokenPair generateTokenPair(Authentication authentication) {
        String accessToken = generateAccessToken(authentication);
        String refreshToken = generateRefreshToken(authentication);
        return new TokenPair(accessToken,refreshToken);
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractTokenType(String token){
        return extractClaim(token,claims -> claims.get("tokenType",String.class));
    }

    public boolean isRefreshToken(String token){
        return "refresh".equals(extractTokenType(token));
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserType(String token) {
        return extractClaim(token, claims -> claims.get("userType", String.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Optional<RefreshTokenModel> findByToken(String token){
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshTokenModel verifyExpiration(RefreshTokenModel refreshTokenRequest){
        if(refreshTokenRequest.getExpireDate().compareTo(Instant.now())<0){
            refreshTokenRepository.delete(refreshTokenRequest);
            throw new RuntimeException(refreshTokenRequest.getToken() + "refresh token expired");
        }
        return refreshTokenRequest;
    }
}
