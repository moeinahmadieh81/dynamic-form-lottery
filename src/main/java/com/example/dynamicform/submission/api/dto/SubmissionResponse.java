package com.example.dynamicform.submission.api.dto;

import com.example.dynamicform.submission.domain.SubmissionStatus;

import java.time.Instant;
import java.util.Map;

public record SubmissionResponse(
        Long id,
        Long formId,
        Integer formVersion,
        Long userId,
        SubmissionStatus status,
        Map<String, Object> answers,
        Instant submittedAt
) {
}
