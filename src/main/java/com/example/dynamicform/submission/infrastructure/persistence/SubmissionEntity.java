package com.example.dynamicform.submission.infrastructure.persistence;

import com.example.dynamicform.submission.domain.SubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "submissions")
public class SubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Column(name = "form_version_id", nullable = false)
    private Long formVersionId;

    @Column(name = "user_id")
    private Long userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> answers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected SubmissionEntity() {
    }

    public static SubmissionEntity submitted(Long formId,
                                             Long formVersionId,
                                             Long userId,
                                             Map<String, Object> answers) {
        SubmissionEntity entity = new SubmissionEntity();
        entity.formId = formId;
        entity.formVersionId = formVersionId;
        entity.userId = userId;
        entity.answers = new LinkedHashMap<>(answers);
        entity.status = SubmissionStatus.SUBMITTED;
        return entity;
    }

    @PrePersist
    void onCreate() {
        submittedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getFormId() {
        return formId;
    }

    public Long getFormVersionId() {
        return formVersionId;
    }

    public Long getUserId() {
        return userId;
    }

    public Map<String, Object> getAnswers() {
        return answers;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
