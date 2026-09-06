package com.example.dynamicform.submission.application;

import com.example.dynamicform.form.application.FormSubmissionDefinition;
import com.example.dynamicform.form.application.FormSubmissionQueryService;
import com.example.dynamicform.form.domain.FormStatus;
import com.example.dynamicform.submission.api.dto.SubmitFormRequest;
import com.example.dynamicform.submission.api.dto.SubmissionResponse;
import com.example.dynamicform.submission.domain.DuplicateSubmissionException;
import com.example.dynamicform.submission.domain.DynamicSubmissionValidator;
import com.example.dynamicform.submission.domain.SubmissionNotAllowedException;
import com.example.dynamicform.submission.infrastructure.persistence.SubmissionEntity;
import com.example.dynamicform.submission.infrastructure.persistence.SubmissionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SubmissionService {

    private final FormSubmissionQueryService formQueryService;
    private final DynamicSubmissionValidator submissionValidator;
    private final SubmissionRepository submissionRepository;

    public SubmissionService(FormSubmissionQueryService formQueryService,
                             DynamicSubmissionValidator submissionValidator,
                             SubmissionRepository submissionRepository) {
        this.formQueryService = formQueryService;
        this.submissionValidator = submissionValidator;
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public SubmissionResponse submit(Long formId, Long userId, SubmitFormRequest request) {
        FormSubmissionDefinition definition = formQueryService.getDefinition(formId);

        ensureSubmissionsAreOpen(definition, Instant.now());

        if (submissionRepository.existsByFormIdAndUserId(formId, userId)) {
            throw new DuplicateSubmissionException(formId);
        }

        submissionValidator.validate(definition.schema(), request.answers());

        SubmissionEntity submission = SubmissionEntity.submitted(
                definition.formId(),
                definition.formVersionId(),
                userId,
                request.answers()
        );

        try {
            submissionRepository.saveAndFlush(submission);
        } catch (DataIntegrityViolationException ex) {
            String message = ex.getMostSpecificCause().getMessage();
            if (message != null && message.contains("uq_submissions_form_user")) {
                throw new DuplicateSubmissionException(formId);
            }
            throw ex;
        }

        return new SubmissionResponse(
                submission.getId(),
                submission.getFormId(),
                definition.formVersion(),
                submission.getUserId(),
                submission.getStatus(),
                submission.getAnswers(),
                submission.getSubmittedAt()
        );
    }

    private void ensureSubmissionsAreOpen(FormSubmissionDefinition definition, Instant now) {
        if (definition.status() != FormStatus.PUBLISHED) {
            throw new SubmissionNotAllowedException("Form is not open for submissions");
        }

        if (definition.startAt() != null && now.isBefore(definition.startAt())) {
            throw new SubmissionNotAllowedException("Form submission period has not started yet");
        }

        if (definition.endAt() != null && !now.isBefore(definition.endAt())) {
            throw new SubmissionNotAllowedException("Form submission period has ended");
        }
    }
}
