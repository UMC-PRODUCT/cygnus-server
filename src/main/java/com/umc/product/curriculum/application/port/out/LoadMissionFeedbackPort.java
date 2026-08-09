package com.umc.product.curriculum.application.port.out;

import java.util.List;

import com.umc.product.curriculum.domain.MissionFeedback;

public interface LoadMissionFeedbackPort {

    MissionFeedback getById(Long missionFeedbackId);

    /**
     * 여러 미션 제출물에 대한 피드백 일괄 조회 (N+1 방지)
     */
    List<MissionFeedback> listByMissionSubmissionIdIn(List<Long> submissionIds);
}
