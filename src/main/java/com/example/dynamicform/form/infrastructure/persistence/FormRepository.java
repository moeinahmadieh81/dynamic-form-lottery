package com.example.dynamicform.form.infrastructure.persistence;

import com.example.dynamicform.form.domain.FormStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface FormRepository extends JpaRepository<FormEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FormEntity f where f.id = :formId")
    Optional<FormEntity> findByIdForUpdate(@Param("formId") Long formId);

    Page<FormEntity> findAllByStatus(FormStatus status, Pageable pageable);

    Page<FormEntity> findAllByStatusIn(Collection<FormStatus> statuses, Pageable pageable);
}
