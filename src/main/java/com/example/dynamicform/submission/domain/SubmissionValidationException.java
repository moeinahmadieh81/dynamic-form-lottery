package com.example.dynamicform.submission.domain;

import java.util.List;

public class SubmissionValidationException extends RuntimeException {

    private final List<SubmissionFieldError> errors;

    public SubmissionValidationException(List<SubmissionFieldError> errors) {
        super("Submission contains invalid answers");
        this.errors = List.copyOf(errors);
    }

    public List<SubmissionFieldError> getErrors() {
        return errors;
    }
}
