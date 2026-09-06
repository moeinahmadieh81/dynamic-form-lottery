package com.example.dynamicform.form.application;

import com.example.dynamicform.form.domain.FormStatus;
import com.example.dynamicform.form.infrastructure.persistence.FormEntity;
import com.example.dynamicform.form.infrastructure.persistence.FormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormLotteryService {

    private final FormRepository formRepository;

    public FormLotteryService(FormRepository formRepository) {
        this.formRepository = formRepository;
    }

    @Transactional
    public void lockAndRequireClosed(Long formId) {
        FormEntity form = formRepository.findByIdForUpdate(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        if (form.getStatus() != FormStatus.CLOSED) {
            throw new IllegalStateException("Form must be CLOSED before creating or running a lottery");
        }
    }

    @Transactional
    public void markDrawn(Long formId) {
        FormEntity form = formRepository.findByIdForUpdate(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));
        form.markDrawn();
    }
}
