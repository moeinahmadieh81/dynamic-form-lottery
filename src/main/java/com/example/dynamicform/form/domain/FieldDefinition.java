package com.example.dynamicform.form.domain;

import java.util.List;

public record FieldDefinition(
        String key,
        FieldType type,
        String label,
        boolean required,
        int order,
        FieldValidation validation,
        List<FieldOption> options
) {
    public FieldDefinition {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
