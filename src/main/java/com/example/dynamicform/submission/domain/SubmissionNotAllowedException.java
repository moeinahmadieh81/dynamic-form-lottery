package com.example.dynamicform.submission.domain;

public class SubmissionNotAllowedException extends RuntimeException {

    public SubmissionNotAllowedException(String message) {
        super(message);
    }
}
