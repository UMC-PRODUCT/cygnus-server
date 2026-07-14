package com.umc.product.project.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;

public interface ProjectApplicationJpaRepository extends JpaRepository<ProjectApplication, Long> {

    boolean existsByAppliedMatchingRound_Id(Long matchingRoundId);

    boolean existsByApplicationForm_Project_Id(Long projectId);

    List<ProjectApplication> findAllByAppliedMatchingRound_Id(Long matchingRoundId);

    @Query("SELECT a FROM ProjectApplication a "
        + "WHERE a.applicationForm.project.id = :projectId "
        + "AND a.status IN :statuses")
    List<ProjectApplication> findAllByProjectIdAndStatusIn(
        @Param("projectId") Long projectId,
        @Param("statuses") List<ProjectApplicationStatus> statuses
    );
}
