package com.example.dynamicform.form.infrastructure.persistence;

import com.example.dynamicform.form.domain.FormSchema;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "form_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_form_version",
                columnNames = {"form_id", "version"}
        )
)
public class FormVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Column(nullable = false)
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private FormSchema schema;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FormVersionEntity() {
    }

    public FormVersionEntity(Long formId, Integer version, FormSchema schema) {
        this.formId = formId;
        this.version = version;
        this.schema = schema;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getFormId() { return formId; }
    public Integer getVersion() { return version; }
    public FormSchema getSchema() { return schema; }
    public Instant getCreatedAt() { return createdAt; }
}
