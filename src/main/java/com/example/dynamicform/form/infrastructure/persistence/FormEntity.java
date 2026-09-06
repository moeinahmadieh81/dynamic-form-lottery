package com.example.dynamicform.form.infrastructure.persistence;

import com.example.dynamicform.form.domain.FormStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "forms")
public class FormEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FormStatus status;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected FormEntity() {
    }

    public static FormEntity draft(String name, String description, Instant startAt, Instant endAt) {
        FormEntity form = new FormEntity();
        form.name = name;
        form.description = description;
        form.status = FormStatus.DRAFT;
        form.currentVersion = 1;
        form.startAt = startAt;
        form.endAt = endAt;
        return form;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateDraft(String name, String description, Instant startAt, Instant endAt) {
        ensureDraft();
        this.name = name;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void publish() {
        ensureDraft();
        this.status = FormStatus.PUBLISHED;
    }

    public void close() {
        if (status != FormStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED forms can be closed");
        }
        this.status = FormStatus.CLOSED;
    }

    public void markDrawn() {
        if (status != FormStatus.CLOSED) {
            throw new IllegalStateException("Only CLOSED forms can be marked as drawn");
        }
        this.status = FormStatus.DRAWN;
    }

    public void incrementCurrentVersion() {
        ensureDraft();
        this.currentVersion++;
    }

    private void ensureDraft() {
        if (status != FormStatus.DRAFT) {
            throw new IllegalStateException("Only draft forms can be modified");
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public FormStatus getStatus() { return status; }
    public Integer getCurrentVersion() { return currentVersion; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
