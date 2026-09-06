package com.example.dynamicform.form.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FormVersionRepository extends JpaRepository<FormVersionEntity, Long> {
    Optional<FormVersionEntity> findByFormIdAndVersion(Long formId, Integer version);
}
