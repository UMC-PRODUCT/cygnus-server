package com.umc.product.recruiting.adapter.out.persistence;

import org.springframework.data.repository.Repository;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;

public interface RecruitingInterviewSessionReferenceJpaRepository
    extends Repository<RecruitingInterviewSchedule, Long> {

    boolean existsByInterviewSessionIdAndStatus(
        Long interviewSessionId,
        RecruitingInterviewScheduleStatus status
    );

    boolean existsByInterviewSessionId(Long interviewSessionId);
}
