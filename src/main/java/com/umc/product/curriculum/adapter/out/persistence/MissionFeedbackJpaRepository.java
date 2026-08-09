package com.umc.product.curriculum.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.curriculum.domain.MissionFeedback;

public interface MissionFeedbackJpaRepository extends JpaRepository<MissionFeedback, Long> {

    List<MissionFeedback> findByMissionSubmission_IdIn(List<Long> submissionIds);

    void deleteByMissionSubmission_Id(Long missionSubmissionId);
}
