package com.umc.product.recruiting.application.port.in.command;

import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerTrack;

public interface ValidateRecruitingApplicationFormUseCase {

    Set<ChallengerTrack> validateForPublish(Long applicationFormId);
}
