package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingInterviewSchedulePersistenceAdapter
    implements LoadRecruitingInterviewSchedulePort, SaveRecruitingInterviewSchedulePort {

    private final RecruitingInterviewScheduleJpaRepository repository;

    @Override
    public RecruitingInterviewSchedule getByApplicationId(Long applicationId) {
        return findByApplicationId(applicationId)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_NOT_FOUND
            ));
    }

    @Override
    public Optional<RecruitingInterviewSchedule> findByApplicationId(Long applicationId) {
        return repository.findByApplication_Id(applicationId);
    }

    @Override
    public RecruitingInterviewSchedule saveSchedule(RecruitingInterviewSchedule schedule) {
        return repository.save(schedule);
    }
}
