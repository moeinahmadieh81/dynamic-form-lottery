package com.example.dynamicform.submission.api.dto;

import com.example.dynamicform.submission.domain.SubmissionStatus;

import java.time.Instant;
import java.util.Map;

public record AdminSubmissionResponse(
        Long id,
        Long formId,
        Integer formVersion,
        UserSummaryResponse user,
        SubmissionStatus status,
        Map<String, Object> answers,
        Instant submittedAt
) {
}
