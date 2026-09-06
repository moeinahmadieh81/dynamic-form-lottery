package com.example.dynamicform.auth.domain;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("An account with this email already exists: " + email);
    }
}
