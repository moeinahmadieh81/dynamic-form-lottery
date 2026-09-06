package com.example.dynamicform.form.application;

import com.example.dynamicform.form.domain.FormSchema;
import com.example.dynamicform.form.domain.FormStatus;

import java.time.Instant;

public record FormSubmissionDefinition(
        Long formId,
        Long formVersionId,
        Integer formVersion,
        FormStatus status,
        Instant startAt,
        Instant endAt,
        FormSchema schema
) {
}
