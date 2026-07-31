package com.umc.product.recruiting.application.service.query;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingInterviewScheduleQueryService implements GetRecruitingInterviewScheduleUseCase {

    private final LoadRecruitingInterviewSchedulePort loadSchedulePort;
    private final CheckPermissionUseCase checkPermissionUseCase;

    @Override
    public Optional<RecruitingInterviewScheduleInfo> findByApplicationId(
        Long applicationId,
        Long requesterMemberId
    ) {
        return loadSchedulePort.findByApplicationId(applicationId)
            .map(schedule -> requireReadable(schedule, requesterMemberId))
            .map(RecruitingInterviewScheduleInfo::from);
    }

    private RecruitingInterviewSchedule requireReadable(
        RecruitingInterviewSchedule schedule,
        Long requesterMemberId
    ) {
        if (Objects.equals(requesterMemberId, schedule.getApplication().getApplicantMemberId())) {
            return schedule;
        }
        boolean privileged = checkPermissionUseCase.check(
            requesterMemberId,
            ResourcePermission.of(
                ResourceType.RECRUITMENT,
                schedule.getApplication().getRound().getSeason().getId(),
                PermissionType.READ
            )
        );
        if (!privileged) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ACCESS_DENIED);
        }
        return schedule;
    }
}
