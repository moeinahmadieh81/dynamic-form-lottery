package com.example.dynamicform.auth.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public record CurrentUser(
        Long id,
        String email,
        boolean admin
) {
    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        return new CurrentUser(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                isAdmin
        );
    }
}
