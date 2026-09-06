package com.example.dynamicform.form.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldValidation(
        Integer minLength,
        Integer maxLength,
        BigDecimal min,
        BigDecimal max,
        String pattern
) {
}
