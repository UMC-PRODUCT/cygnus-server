package com.umc.product.curriculum.application.port.out;

import java.util.List;

import com.umc.product.curriculum.domain.MissionFeedback;

public interface LoadMissionFeedbackPort {

    /**
     * 미션 피드백 단건 조회. 존재하지 않으면 CurriculumDomainException(MISSION_FEEDBACK_NOT_FOUND) 던짐
     */
    MissionFeedback getById(Long missionFeedbackId);

    /**
     * 여러 미션 제출물에 대한 피드백 일괄 조회 (N+1 방지)
     */
    List<MissionFeedback> listByMissionSubmissionIdIn(List<Long> submissionIds);
}
