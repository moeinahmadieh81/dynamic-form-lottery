package com.example.dynamicform.submission.infrastructure.persistence;

import com.example.dynamicform.submission.domain.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, Long> {
    List<SubmissionEntity> findAllByFormIdOrderBySubmittedAtDesc(Long formId);

    Page<SubmissionEntity> findAllByFormId(Long formId, Pageable pageable);

    Page<SubmissionEntity> findAllByUserId(Long userId, Pageable pageable);

    boolean existsByFormIdAndUserId(Long formId, Long userId);

    List<SubmissionEntity> findAllByFormIdAndStatusAndUserIdIsNotNullOrderByIdAsc(
            Long formId,
            SubmissionStatus status
    );
}
