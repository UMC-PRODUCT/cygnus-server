package com.umc.product.recruiting.application.port.out;

import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;

public interface LoadRecruitingInterviewSchedulePort {

    RecruitingInterviewSchedule getByApplicationId(Long applicationId);

    Optional<RecruitingInterviewSchedule> findByApplicationId(Long applicationId);
}
