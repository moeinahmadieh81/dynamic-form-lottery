package com.example.dynamicform.auth.application;

import com.example.dynamicform.user.infrastructure.persistence.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long accessTokenTtlSeconds;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           @Value("${app.security.jwt.issuer}") String issuer,
                           @Value("${app.security.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String createAccessToken(UserEntity user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenTtlSeconds))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("displayName", user.getDisplayName())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}
