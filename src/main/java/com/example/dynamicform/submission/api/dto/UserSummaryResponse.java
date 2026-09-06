package com.example.dynamicform.submission.api.dto;

public record UserSummaryResponse(
        Long id,
        String email,
        String displayName
) {
}
