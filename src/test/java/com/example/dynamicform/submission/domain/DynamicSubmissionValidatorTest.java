package com.example.dynamicform.submission.domain;

import com.example.dynamicform.form.domain.FieldDefinition;
import com.example.dynamicform.form.domain.FieldOption;
import com.example.dynamicform.form.domain.FieldType;
import com.example.dynamicform.form.domain.FieldValidation;
import com.example.dynamicform.form.domain.FormSchema;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicSubmissionValidatorTest {

    private final DynamicSubmissionValidator validator = new DynamicSubmissionValidator();

    @Test
    void validSubmissionPasses() {
        FormSchema schema = sampleSchema();

        assertDoesNotThrow(() -> validator.validate(schema, Map.of(
                "fullName", "Ali Ahmadi",
                "age", 28,
                "city", "tehran"
        )));
    }

    @Test
    void unknownFieldIsRejected() {
        SubmissionValidationException ex = assertThrows(
                SubmissionValidationException.class,
                () -> validator.validate(sampleSchema(), Map.of(
                        "fullName", "Ali Ahmadi",
                        "age", 28,
                        "city", "tehran",
                        "isWinner", true
                ))
        );

        assertEquals("UNKNOWN_FIELD", ex.getErrors().getFirst().code());
    }

    @Test
    void requiredFieldIsRejectedWhenMissing() {
        SubmissionValidationException ex = assertThrows(
                SubmissionValidationException.class,
                () -> validator.validate(sampleSchema(), Map.of(
                        "age", 28,
                        "city", "tehran"
                ))
        );

        assertEquals("REQUIRED", ex.getErrors().getFirst().code());
    }

    @Test
    void numberBelowMinimumIsRejected() {
        SubmissionValidationException ex = assertThrows(
                SubmissionValidationException.class,
                () -> validator.validate(sampleSchema(), Map.of(
                        "fullName", "Ali Ahmadi",
                        "age", 12,
                        "city", "tehran"
                ))
        );

        assertEquals("MIN_VALUE", ex.getErrors().getFirst().code());
    }

    @Test
    void invalidSelectOptionIsRejected() {
        SubmissionValidationException ex = assertThrows(
                SubmissionValidationException.class,
                () -> validator.validate(sampleSchema(), Map.of(
                        "fullName", "Ali Ahmadi",
                        "age", 28,
                        "city", "shiraz"
                ))
        );

        assertEquals("INVALID_OPTION", ex.getErrors().getFirst().code());
    }

    private FormSchema sampleSchema() {
        return new FormSchema(List.of(
                new FieldDefinition(
                        "fullName",
                        FieldType.TEXT,
                        "Full Name",
                        true,
                        1,
                        new FieldValidation(3, 100, null, null, null),
                        List.of()
                ),
                new FieldDefinition(
                        "age",
                        FieldType.NUMBER,
                        "Age",
                        true,
                        2,
                        new FieldValidation(null, null, BigDecimal.valueOf(18), BigDecimal.valueOf(80), null),
                        List.of()
                ),
                new FieldDefinition(
                        "city",
                        FieldType.SELECT,
                        "City",
                        true,
                        3,
                        null,
                        List.of(
                                new FieldOption("tehran", "Tehran"),
                                new FieldOption("tabriz", "Tabriz")
                        )
                )
        ));
    }
}
