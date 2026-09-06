package com.example.dynamicform.form.application;

import com.example.dynamicform.form.infrastructure.persistence.FormEntity;
import com.example.dynamicform.form.infrastructure.persistence.FormRepository;
import com.example.dynamicform.form.infrastructure.persistence.FormVersionEntity;
import com.example.dynamicform.form.infrastructure.persistence.FormVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormSubmissionQueryService {

    private final FormRepository formRepository;
    private final FormVersionRepository formVersionRepository;

    public FormSubmissionQueryService(FormRepository formRepository,
                                      FormVersionRepository formVersionRepository) {
        this.formRepository = formRepository;
        this.formVersionRepository = formVersionRepository;
    }

    @Transactional(readOnly = true)
    public FormSubmissionDefinition getDefinition(Long formId) {
        FormEntity form = formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        FormVersionEntity version = formVersionRepository
                .findByFormIdAndVersion(form.getId(), form.getCurrentVersion())
                .orElseThrow(() -> new IllegalStateException("Current form version not found"));

        return new FormSubmissionDefinition(
                form.getId(),
                version.getId(),
                version.getVersion(),
                form.getStatus(),
                form.getStartAt(),
                form.getEndAt(),
                version.getSchema()
        );
    }
}
