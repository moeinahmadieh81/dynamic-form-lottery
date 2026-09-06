package com.example.dynamicform.form.application;

public class FormNotFoundException extends RuntimeException {
    public FormNotFoundException(Long id) {
        super("Form not found: " + id);
    }
}
