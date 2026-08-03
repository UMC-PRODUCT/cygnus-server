package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;

public interface LoadRecruitingInterviewSchedulePort {

    RecruitingInterviewSchedule getByApplicationId(Long applicationId);

    Optional<RecruitingInterviewSchedule> findByApplicationId(Long applicationId);

    List<RecruitingInterviewSchedule> getAllByApplicationIdsForUpdate(List<Long> applicationIds);

    List<RecruitingInterviewSchedule> getAllConfirmedByInterviewSessionIds(List<Long> sessionIds);
}
