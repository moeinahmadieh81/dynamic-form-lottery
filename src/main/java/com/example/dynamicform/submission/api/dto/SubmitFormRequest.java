package com.example.dynamicform.submission.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SubmitFormRequest(
        @NotNull Map<String, Object> answers
) {
}
