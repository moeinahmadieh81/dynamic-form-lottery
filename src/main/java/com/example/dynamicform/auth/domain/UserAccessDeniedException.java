package com.example.dynamicform.auth.domain;

public class UserAccessDeniedException extends RuntimeException {
    public UserAccessDeniedException(String message) {
        super(message);
    }
}
