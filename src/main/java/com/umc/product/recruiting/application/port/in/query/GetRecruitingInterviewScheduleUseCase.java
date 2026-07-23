package com.umc.product.recruiting.application.port.in.query;

import java.util.Optional;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleInfo;

public interface GetRecruitingInterviewScheduleUseCase {

    Optional<RecruitingInterviewScheduleInfo> findByApplicationId(Long applicationId, Long requesterMemberId);
}
