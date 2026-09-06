package com.example.dynamicform.form.domain;

import java.util.List;

public record FormSchema(List<FieldDefinition> fields) {
    public FormSchema {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
