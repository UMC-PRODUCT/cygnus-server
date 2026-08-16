package com.umc.product.project.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.project.domain.ProjectApplicationFormPolicy;

public interface ProjectApplicationFormPolicyJpaRepository
    extends JpaRepository<ProjectApplicationFormPolicy, Long> {

    List<ProjectApplicationFormPolicy> findAllByApplicationFormId(Long applicationFormId);

    List<ProjectApplicationFormPolicy> findAllByApplicationFormIdIn(Collection<Long> applicationFormIds);

    void deleteByFormSectionId(Long formSectionId);

    @Modifying
    @Query("DELETE FROM ProjectApplicationFormPolicy p WHERE p.applicationForm.id = :applicationFormId")
    void deleteAllByApplicationFormId(@Param("applicationFormId") Long applicationFormId);
}
