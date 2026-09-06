package com.example.dynamicform.submission.domain;

import com.example.dynamicform.form.domain.FieldDefinition;
import com.example.dynamicform.form.domain.FieldOption;
import com.example.dynamicform.form.domain.FieldType;
import com.example.dynamicform.form.domain.FieldValidation;
import com.example.dynamicform.form.domain.FormSchema;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DynamicSubmissionValidator {

    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void validate(FormSchema schema, Map<String, Object> answers) {
        Map<String, Object> safeAnswers = answers == null ? Map.of() : answers;
        List<SubmissionFieldError> errors = new ArrayList<>();

        Set<String> schemaKeys = new HashSet<>();
        for (FieldDefinition field : schema.fields()) {
            schemaKeys.add(field.key());
        }

        for (String answerKey : safeAnswers.keySet()) {
            if (!schemaKeys.contains(answerKey)) {
                errors.add(error(answerKey, "UNKNOWN_FIELD", "Field is not defined in the form schema"));
            }
        }

        for (FieldDefinition field : schema.fields()) {
            Object value = safeAnswers.get(field.key());

            if (isEmpty(value)) {
                if (field.required()) {
                    errors.add(error(field.key(), "REQUIRED", "Field is required"));
                }
                continue;
            }

            validateValue(field, value, errors);
        }

        if (!errors.isEmpty()) {
            throw new SubmissionValidationException(errors);
        }
    }

    private void validateValue(FieldDefinition field,
                               Object value,
                               List<SubmissionFieldError> errors) {
        switch (field.type()) {
            case TEXT, TEXTAREA -> validateText(field, value, errors, false);
            case EMAIL -> validateText(field, value, errors, true);
            case PHONE -> validateText(field, value, errors, false);
            case NUMBER -> validateNumber(field, value, errors);
            case DATE -> validateDate(field, value, errors);
            case DATETIME -> validateDateTime(field, value, errors);
            case SELECT, RADIO -> validateSingleOption(field, value, errors);
            case MULTI_SELECT, CHECKBOX -> validateMultipleOptions(field, value, errors);
            case BOOLEAN -> validateBoolean(field, value, errors);
        }
    }

    private void validateText(FieldDefinition field,
                              Object value,
                              List<SubmissionFieldError> errors,
                              boolean validateEmail) {
        if (!(value instanceof String text)) {
            errors.add(error(field.key(), "INVALID_TYPE", "Value must be a string"));
            return;
        }

        FieldValidation validation = field.validation();
        if (validation != null) {
            if (validation.minLength() != null && text.length() < validation.minLength()) {
                errors.add(error(
                        field.key(),
                        "MIN_LENGTH",
                        "Length must be at least " + validation.minLength()
                ));
            }

            if (validation.maxLength() != null && text.length() > validation.maxLength()) {
                errors.add(error(
                        field.key(),
                        "MAX_LENGTH",
                        "Length must be at most " + validation.maxLength()
                ));
            }

            if (validation.pattern() != null
                    && !validation.pattern().isBlank()
                    && !Pattern.matches(validation.pattern(), text)) {
                errors.add(error(field.key(), "PATTERN", "Value does not match the required pattern"));
            }
        }

        if (validateEmail && !SIMPLE_EMAIL.matcher(text).matches()) {
            errors.add(error(field.key(), "INVALID_EMAIL", "Value must be a valid email address"));
        }
    }

    private void validateNumber(FieldDefinition field,
                                Object value,
                                List<SubmissionFieldError> errors) {
        if (!(value instanceof Number number)) {
            errors.add(error(field.key(), "INVALID_TYPE", "Value must be a number"));
            return;
        }

        BigDecimal decimalValue;
        try {
            decimalValue = new BigDecimal(number.toString());
        } catch (NumberFormatException ex) {
            errors.add(error(field.key(), "INVALID_NUMBER", "Value must be a valid number"));
            return;
        }

        FieldValidation validation = field.validation();
        if (validation == null) {
            return;
        }

        if (validation.min() != null && decimalValue.compareTo(validation.min()) < 0) {
            errors.add(error(
                    field.key(),
                    "MIN_VALUE",
                    "Value must be at least " + validation.min()
            ));
        }

        if (validation.max() != null && decimalValue.compareTo(validation.max()) > 0) {
            errors.add(error(
                    field.key(),
                    "MAX_VALUE",
                    "Value must be at most " + validation.max()
            ));
        }
    }

    private void validateDate(FieldDefinition field,
                              Object value,
                              List<SubmissionFieldError> errors) {
        if (!(value instanceof String text)) {
            errors.add(error(field.key(), "INVALID_TYPE", "Date must be an ISO-8601 string"));
            return;
        }

        try {
            LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            errors.add(error(field.key(), "INVALID_DATE", "Date must use ISO format yyyy-MM-dd"));
        }
    }

    private void validateDateTime(FieldDefinition field,
                                  Object value,
                                  List<SubmissionFieldError> errors) {
        if (!(value instanceof String text)) {
            errors.add(error(field.key(), "INVALID_TYPE", "Datetime must be an ISO-8601 string"));
            return;
        }

        try {
            OffsetDateTime.parse(text);
        } catch (DateTimeParseException ex) {
            errors.add(error(
                    field.key(),
                    "INVALID_DATETIME",
                    "Datetime must be ISO-8601 and include an offset"
            ));
        }
    }

    private void validateSingleOption(FieldDefinition field,
                                      Object value,
                                      List<SubmissionFieldError> errors) {
        if (!(value instanceof String selectedValue)) {
            errors.add(error(field.key(), "INVALID_TYPE", "Value must be a string option value"));
            return;
        }

        if (!allowedOptions(field).contains(selectedValue)) {
            errors.add(error(field.key(), "INVALID_OPTION", "Selected option is not allowed"));
        }
    }

    private void validateMultipleOptions(FieldDefinition field,
                                         Object value,
                                         List<SubmissionFieldError> errors) {
        if (!(value instanceof Collection<?> values)) {
            errors.add(error(field.key(), "INVALID_TYPE", "Value must be an array of option values"));
            return;
        }

        Set<String> allowed = allowedOptions(field);
        Set<String> seen = new HashSet<>();

        for (Object item : values) {
            if (!(item instanceof String selectedValue)) {
                errors.add(error(field.key(), "INVALID_TYPE", "Every selected option must be a string"));
                continue;
            }

            if (!seen.add(selectedValue)) {
                errors.add(error(field.key(), "DUPLICATE_OPTION", "Selected options must be unique"));
            }

            if (!allowed.contains(selectedValue)) {
                errors.add(error(
                        field.key(),
                        "INVALID_OPTION",
                        "Selected option is not allowed: " + selectedValue
                ));
            }
        }
    }

    private void validateBoolean(FieldDefinition field,
                                 Object value,
                                 List<SubmissionFieldError> errors) {
        if (!(value instanceof Boolean)) {
            errors.add(error(field.key(), "INVALID_TYPE", "Value must be true or false"));
        }
    }

    private Set<String> allowedOptions(FieldDefinition field) {
        Set<String> allowed = new HashSet<>();
        for (FieldOption option : field.options()) {
            allowed.add(option.value());
        }
        return allowed;
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof String text) {
            return text.isBlank();
        }

        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }

        return false;
    }

    private SubmissionFieldError error(String field, String code, String message) {
        return new SubmissionFieldError(field, code, message);
    }
}
