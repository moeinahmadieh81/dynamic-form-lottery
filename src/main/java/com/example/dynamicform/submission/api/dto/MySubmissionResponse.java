package com.example.dynamicform.submission.api.dto;

import com.example.dynamicform.form.domain.FormStatus;
import com.example.dynamicform.submission.domain.SubmissionStatus;

import java.time.Instant;
import java.util.Map;

public record MySubmissionResponse(
        Long id,
        Long formId,
        String formName,
        FormStatus formStatus,
        Integer formVersion,
        SubmissionStatus status,
        Map<String, Object> answers,
        Instant submittedAt
) {
}
