package com.example.dynamicform.form.api.dto;

import com.example.dynamicform.form.domain.FormSchema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateFormRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 4000) String description,
        Instant startAt,
        Instant endAt,
        @NotNull @Valid FormSchema schema
) {
}
