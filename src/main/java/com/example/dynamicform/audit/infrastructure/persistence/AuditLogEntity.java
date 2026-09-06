package com.example.dynamicform.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLogEntity() {
    }

    public static AuditLogEntity create(Long actorId,
                                        String action,
                                        String entityType,
                                        Long entityId,
                                        Map<String, Object> metadata) {
        AuditLogEntity log = new AuditLogEntity();
        log.actorId = actorId;
        log.action = action;
        log.entityType = entityType;
        log.entityId = entityId;
        log.metadata = metadata == null ? null : new LinkedHashMap<>(metadata);
        return log;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
