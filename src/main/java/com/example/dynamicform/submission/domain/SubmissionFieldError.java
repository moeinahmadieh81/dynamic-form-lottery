package com.example.dynamicform.submission.domain;

public record SubmissionFieldError(
        String field,
        String code,
        String message
) {
}
