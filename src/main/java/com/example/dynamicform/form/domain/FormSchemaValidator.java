package com.example.dynamicform.form.domain;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class FormSchemaValidator {

    public void validateForSave(FormSchema schema) {
        if (schema == null) {
            throw new IllegalArgumentException("Form schema is required");
        }

        Set<String> keys = new HashSet<>();
        Set<Integer> orders = new HashSet<>();

        for (FieldDefinition field : schema.fields()) {
            validateKey(field, keys);
            validateOrder(field, orders);
            validateType(field);
            validateLabel(field);
            validateOptions(field);
            validateFieldValidation(field);
        }
    }

    public void validateForPublish(FormSchema schema) {
        validateForSave(schema);

        if (schema.fields().isEmpty()) {
            throw new IllegalArgumentException("A form must have at least one field before publishing");
        }
    }

    private void validateKey(FieldDefinition field, Set<String> keys) {
        if (field.key() == null || field.key().isBlank()) {
            throw new IllegalArgumentException("Field key is required");
        }

        if (!field.key().matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid field key: " + field.key());
        }

        if (!keys.add(field.key())) {
            throw new IllegalArgumentException("Duplicate field key: " + field.key());
        }
    }

    private void validateOrder(FieldDefinition field, Set<Integer> orders) {
        if (field.order() < 1) {
            throw new IllegalArgumentException("Field order must be greater than zero: " + field.key());
        }

        if (!orders.add(field.order())) {
            throw new IllegalArgumentException("Duplicate field order: " + field.order());
        }
    }

    private void validateType(FieldDefinition field) {
        if (field.type() == null) {
            throw new IllegalArgumentException("Field type is required for: " + field.key());
        }
    }

    private void validateLabel(FieldDefinition field) {
        if (field.label() == null || field.label().isBlank()) {
            throw new IllegalArgumentException("Field label is required for: " + field.key());
        }
    }

    private void validateOptions(FieldDefinition field) {
        if (requiresOptions(field.type()) && field.options().isEmpty()) {
            throw new IllegalArgumentException("Options are required for field: " + field.key());
        }

        Set<String> optionValues = new HashSet<>();

        for (FieldOption option : field.options()) {
            if (option.value() == null || option.value().isBlank()) {
                throw new IllegalArgumentException("Option value is required for field: " + field.key());
            }

            if (option.label() == null || option.label().isBlank()) {
                throw new IllegalArgumentException("Option label is required for field: " + field.key());
            }

            if (!optionValues.add(option.value())) {
                throw new IllegalArgumentException(
                        "Duplicate option value '" + option.value() + "' for field: " + field.key()
                );
            }
        }
    }

    private void validateFieldValidation(FieldDefinition field) {
        FieldValidation validation = field.validation();

        if (validation == null) {
            return;
        }

        if (validation.minLength() != null && validation.minLength() < 0) {
            throw new IllegalArgumentException("minLength cannot be negative for field: " + field.key());
        }

        if (validation.maxLength() != null && validation.maxLength() < 0) {
            throw new IllegalArgumentException("maxLength cannot be negative for field: " + field.key());
        }

        if (validation.minLength() != null
                && validation.maxLength() != null
                && validation.minLength() > validation.maxLength()) {
            throw new IllegalArgumentException(
                    "minLength cannot be greater than maxLength for field: " + field.key()
            );
        }

        BigDecimal min = validation.min();
        BigDecimal max = validation.max();

        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min cannot be greater than max for field: " + field.key());
        }

        if (validation.pattern() != null && !validation.pattern().isBlank()) {
            try {
                Pattern.compile(validation.pattern());
            } catch (PatternSyntaxException ex) {
                throw new IllegalArgumentException("Invalid regex pattern for field: " + field.key());
            }
        }
    }

    private boolean requiresOptions(FieldType type) {
        return type == FieldType.SELECT
                || type == FieldType.MULTI_SELECT
                || type == FieldType.RADIO
                || type == FieldType.CHECKBOX;
    }
}
