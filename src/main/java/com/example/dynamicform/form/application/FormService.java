package com.example.dynamicform.form.application;

import com.example.dynamicform.audit.application.AuditLogService;
import com.example.dynamicform.form.api.dto.CreateFormRequest;
import com.example.dynamicform.form.api.dto.FormResponse;
import com.example.dynamicform.form.api.dto.UpdateDraftFormRequest;
import com.example.dynamicform.form.domain.FormSchema;
import com.example.dynamicform.form.domain.FormSchemaValidator;
import com.example.dynamicform.form.infrastructure.persistence.FormEntity;
import com.example.dynamicform.form.infrastructure.persistence.FormRepository;
import com.example.dynamicform.form.infrastructure.persistence.FormVersionEntity;
import com.example.dynamicform.form.infrastructure.persistence.FormVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormService {

    private final FormRepository formRepository;
    private final FormVersionRepository versionRepository;
    private final FormSchemaValidator schemaValidator;
    private final AuditLogService auditLogService;

    public FormService(FormRepository formRepository,
                       FormVersionRepository versionRepository,
                       FormSchemaValidator schemaValidator,
                       AuditLogService auditLogService) {
        this.formRepository = formRepository;
        this.versionRepository = versionRepository;
        this.schemaValidator = schemaValidator;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public FormResponse create(CreateFormRequest request) {
        validateDates(request.startAt(), request.endAt());
        schemaValidator.validateForSave(request.schema());

        FormEntity form = FormEntity.draft(
                request.name(),
                request.description(),
                request.startAt(),
                request.endAt()
        );
        formRepository.save(form);

        FormVersionEntity version = new FormVersionEntity(
                form.getId(),
                form.getCurrentVersion(),
                request.schema()
        );
        versionRepository.save(version);

        return toResponse(form, version.getSchema());
    }

    @Transactional
    public FormResponse updateDraft(Long formId, UpdateDraftFormRequest request) {
        validateDates(request.startAt(), request.endAt());
        schemaValidator.validateForSave(request.schema());

        FormEntity form = getForm(formId);
        form.updateDraft(request.name(), request.description(), request.startAt(), request.endAt());
        form.incrementCurrentVersion();

        FormVersionEntity newVersion = new FormVersionEntity(
                form.getId(),
                form.getCurrentVersion(),
                request.schema()
        );
        versionRepository.save(newVersion);

        return toResponse(form, newVersion.getSchema());
    }

    @Transactional
    public FormResponse publish(Long formId) {
        FormEntity form = getForm(formId);
        FormVersionEntity currentVersion = getCurrentVersion(form);

        schemaValidator.validateForPublish(currentVersion.getSchema());
        validateDates(form.getStartAt(), form.getEndAt());
        form.publish();

        return toResponse(form, currentVersion.getSchema());
    }

    @Transactional
    public FormResponse close(Long formId, Long actorId) {
        FormEntity form = formRepository.findByIdForUpdate(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));
        FormVersionEntity currentVersion = getCurrentVersion(form);

        form.close();
        auditLogService.record(
                actorId,
                "FORM_CLOSED",
                "FORM",
                formId,
                java.util.Map.of("currentVersion", form.getCurrentVersion())
        );

        return toResponse(form, currentVersion.getSchema());
    }

    @Transactional(readOnly = true)
    public FormResponse get(Long formId) {
        FormEntity form = getForm(formId);
        FormVersionEntity currentVersion = getCurrentVersion(form);
        return toResponse(form, currentVersion.getSchema());
    }

    private FormEntity getForm(Long formId) {
        return formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));
    }

    private FormVersionEntity getCurrentVersion(FormEntity form) {
        return versionRepository.findByFormIdAndVersion(form.getId(), form.getCurrentVersion())
                .orElseThrow(() -> new IllegalStateException("Current form version not found"));
    }

    private void validateDates(java.time.Instant startAt, java.time.Instant endAt) {
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
    }

    private FormResponse toResponse(FormEntity form, FormSchema schema) {
        return new FormResponse(
                form.getId(),
                form.getName(),
                form.getDescription(),
                form.getStatus(),
                form.getCurrentVersion(),
                form.getStartAt(),
                form.getEndAt(),
                schema
        );
    }
}
