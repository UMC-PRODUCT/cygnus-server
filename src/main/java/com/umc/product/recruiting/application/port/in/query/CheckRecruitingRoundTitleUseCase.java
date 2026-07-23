package com.umc.product.recruiting.application.port.in.query;

public interface CheckRecruitingRoundTitleUseCase {

    boolean isTitleAvailable(Long seasonId, String title, Long excludedRoundId);
}
