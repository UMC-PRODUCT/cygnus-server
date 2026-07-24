package com.umc.product.curriculum.application.port.out;

import com.umc.product.curriculum.domain.MissionSubmission;

public interface SaveMissionSubmissionPort {

    MissionSubmission save(MissionSubmission missionSubmission);
}
