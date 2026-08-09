package com.umc.product.curriculum.application.port.out;

import com.umc.product.curriculum.domain.MissionFeedback;

public interface SaveMissionFeedbackPort {

    MissionFeedback save(MissionFeedback missionFeedback);

    void delete(MissionFeedback missionFeedback);

    void deleteByMissionSubmissionId(Long missionSubmissionId);
}
