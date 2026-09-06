package com.example.dynamicform.form.api.dto;

import com.example.dynamicform.form.domain.FormStatus;

import java.time.Instant;

public record FormSummaryResponse(
        Long id,
        String name,
        String description,
        FormStatus status,
        Integer currentVersion,
        Instant startAt,
        Instant endAt,
        Instant createdAt,
        Instant updatedAt
) {
}
