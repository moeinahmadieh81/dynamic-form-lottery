package com.example.dynamicform.audit.application;

import com.example.dynamicform.audit.infrastructure.persistence.AuditLogEntity;
import com.example.dynamicform.audit.infrastructure.persistence.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(Long actorId,
                       String action,
                       String entityType,
                       Long entityId,
                       Map<String, Object> metadata) {
        repository.save(AuditLogEntity.create(actorId, action, entityType, entityId, metadata));
    }
}
