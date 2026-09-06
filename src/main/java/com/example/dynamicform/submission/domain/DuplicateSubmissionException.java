package com.example.dynamicform.submission.domain;

public class DuplicateSubmissionException extends RuntimeException {
    public DuplicateSubmissionException(Long formId) {
        super("User has already submitted form " + formId);
    }
}
