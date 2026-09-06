package com.example.dynamicform.submission.application;

import com.example.dynamicform.common.api.PageResponse;
import com.example.dynamicform.common.api.PagingSupport;
import com.example.dynamicform.form.application.FormNotFoundException;
import com.example.dynamicform.form.infrastructure.persistence.FormEntity;
import com.example.dynamicform.form.infrastructure.persistence.FormRepository;
import com.example.dynamicform.form.infrastructure.persistence.FormVersionEntity;
import com.example.dynamicform.form.infrastructure.persistence.FormVersionRepository;
import com.example.dynamicform.submission.api.dto.AdminSubmissionResponse;
import com.example.dynamicform.submission.api.dto.MySubmissionResponse;
import com.example.dynamicform.submission.api.dto.UserSummaryResponse;
import com.example.dynamicform.submission.infrastructure.persistence.SubmissionEntity;
import com.example.dynamicform.submission.infrastructure.persistence.SubmissionRepository;
import com.example.dynamicform.user.infrastructure.persistence.UserEntity;
import com.example.dynamicform.user.infrastructure.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SubmissionQueryService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "id",
            "formId",
            "status",
            "submittedAt"
    );

    private final SubmissionRepository submissionRepository;
    private final FormRepository formRepository;
    private final FormVersionRepository formVersionRepository;
    private final UserRepository userRepository;

    public SubmissionQueryService(SubmissionRepository submissionRepository,
                                  FormRepository formRepository,
                                  FormVersionRepository formVersionRepository,
                                  UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.formRepository = formRepository;
        this.formVersionRepository = formVersionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminSubmissionResponse> listForAdmin(Long formId,
                                                              int page,
                                                              int size,
                                                              String sort,
                                                              String direction) {
        if (!formRepository.existsById(formId)) {
            throw new FormNotFoundException(formId);
        }

        Pageable pageable = PagingSupport.pageRequest(
                page,
                size,
                sort,
                direction,
                SORT_FIELDS,
                "submittedAt"
        );

        Page<SubmissionEntity> submissions = submissionRepository.findAllByFormId(formId, pageable);
        Map<Long, Integer> versionsById = loadVersions(submissions.getContent());
        Map<Long, UserEntity> usersById = loadUsers(submissions.getContent());

        Page<AdminSubmissionResponse> mapped = submissions.map(submission -> {
            UserSummaryResponse user = null;
            if (submission.getUserId() != null) {
                UserEntity userEntity = usersById.get(submission.getUserId());
                if (userEntity != null) {
                    user = new UserSummaryResponse(
                            userEntity.getId(),
                            userEntity.getEmail(),
                            userEntity.getDisplayName()
                    );
                }
            }

            return new AdminSubmissionResponse(
                    submission.getId(),
                    submission.getFormId(),
                    versionsById.get(submission.getFormVersionId()),
                    user,
                    submission.getStatus(),
                    submission.getAnswers(),
                    submission.getSubmittedAt()
            );
        });

        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PageResponse<MySubmissionResponse> listMine(Long userId,
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
                "submittedAt"
        );

        Page<SubmissionEntity> submissions = submissionRepository.findAllByUserId(userId, pageable);

        Map<Long, FormEntity> formsById = formRepository.findAllById(
                        submissions.getContent().stream()
                                .map(SubmissionEntity::getFormId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(FormEntity::getId, Function.identity()));

        Map<Long, Integer> versionsById = loadVersions(submissions.getContent());

        Page<MySubmissionResponse> mapped = submissions.map(submission -> {
            FormEntity form = formsById.get(submission.getFormId());
            if (form == null) {
                throw new IllegalStateException("Submission references a missing form: " + submission.getFormId());
            }

            return new MySubmissionResponse(
                    submission.getId(),
                    form.getId(),
                    form.getName(),
                    form.getStatus(),
                    versionsById.get(submission.getFormVersionId()),
                    submission.getStatus(),
                    submission.getAnswers(),
                    submission.getSubmittedAt()
            );
        });

        return PageResponse.from(mapped);
    }

    private Map<Long, Integer> loadVersions(List<SubmissionEntity> submissions) {
        List<Long> versionIds = submissions.stream()
                .map(SubmissionEntity::getFormVersionId)
                .distinct()
                .toList();

        Map<Long, Integer> result = new HashMap<>();
        for (FormVersionEntity version : formVersionRepository.findAllById(versionIds)) {
            result.put(version.getId(), version.getVersion());
        }

        for (Long versionId : versionIds) {
            if (!result.containsKey(versionId)) {
                throw new IllegalStateException("Submission references a missing form version: " + versionId);
            }
        }
        return result;
    }

    private Map<Long, UserEntity> loadUsers(List<SubmissionEntity> submissions) {
        List<Long> userIds = submissions.stream()
                .map(SubmissionEntity::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }
}
