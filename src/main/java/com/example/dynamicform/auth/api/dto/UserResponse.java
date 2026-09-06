package com.example.dynamicform.auth.api.dto;

import com.example.dynamicform.user.domain.UserRole;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        UserRole role
) {
}
