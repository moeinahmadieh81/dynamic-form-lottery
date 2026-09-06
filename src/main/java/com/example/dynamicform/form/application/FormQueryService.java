package com.example.dynamicform.form.application;

import com.example.dynamicform.common.api.PageResponse;
import com.example.dynamicform.common.api.PagingSupport;
import com.example.dynamicform.form.api.dto.FormResponse;
import com.example.dynamicform.form.api.dto.FormSummaryResponse;
import com.example.dynamicform.form.domain.FormStatus;
import com.example.dynamicform.form.infrastructure.persistence.FormEntity;
import com.example.dynamicform.form.infrastructure.persistence.FormRepository;
import com.example.dynamicform.form.infrastructure.persistence.FormVersionEntity;
import com.example.dynamicform.form.infrastructure.persistence.FormVersionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class FormQueryService {

    private static final Set<FormStatus> USER_VISIBLE_STATUSES = Set.of(
            FormStatus.PUBLISHED,
            FormStatus.CLOSED,
            FormStatus.DRAWN
    );

    private static final Set<String> SORT_FIELDS = Set.of(
            "id",
            "name",
            "status",
            "startAt",
            "endAt",
            "createdAt",
            "updatedAt"
    );

    private final FormRepository formRepository;
    private final FormVersionRepository versionRepository;

    public FormQueryService(FormRepository formRepository,
                            FormVersionRepository versionRepository) {
        this.formRepository = formRepository;
        this.versionRepository = versionRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<FormSummaryResponse> list(boolean admin,
                                                  FormStatus status,
                                                  int page,
                                                  int size,
                                                  String sort,
                                                  String direction) {
        Pageable pageable = PagingSupport.pageRequest(
                page,
                size,
                sort,
                direction,
                SORT_FIELDS,
                "updatedAt"
        );

        Page<FormEntity> forms;
        if (admin) {
            forms = status == null
                    ? formRepository.findAll(pageable)
                    : formRepository.findAllByStatus(status, pageable);
        } else {
            if (status != null && !USER_VISIBLE_STATUSES.contains(status)) {
                throw new IllegalArgumentException("Requested form status is not visible to regular users: " + status);
            }
            forms = status == null
                    ? formRepository.findAllByStatusIn(USER_VISIBLE_STATUSES, pageable)
                    : formRepository.findAllByStatus(status, pageable);
        }

        return PageResponse.from(forms.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public FormResponse get(Long formId, boolean admin) {
        FormEntity form = formRepository.findById(formId)
                .orElseThrow(() -> new FormNotFoundException(formId));

        if (!admin && !USER_VISIBLE_STATUSES.contains(form.getStatus())) {
            // Return the same not-found response to avoid exposing draft/admin-only forms.
            throw new FormNotFoundException(formId);
        }

        FormVersionEntity currentVersion = versionRepository
                .findByFormIdAndVersion(form.getId(), form.getCurrentVersion())
                .orElseThrow(() -> new IllegalStateException("Current form version not found"));

        return new FormResponse(
                form.getId(),
                form.getName(),
                form.getDescription(),
                form.getStatus(),
                form.getCurrentVersion(),
                form.getStartAt(),
                form.getEndAt(),
                currentVersion.getSchema()
        );
    }

    private FormSummaryResponse toSummary(FormEntity form) {
        return new FormSummaryResponse(
                form.getId(),
                form.getName(),
                form.getDescription(),
                form.getStatus(),
                form.getCurrentVersion(),
                form.getStartAt(),
                form.getEndAt(),
                form.getCreatedAt(),
                form.getUpdatedAt()
        );
    }
}
