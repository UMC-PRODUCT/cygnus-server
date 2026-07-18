package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingInterviewScheduleCommandService implements ManageRecruitingInterviewScheduleUseCase {

    private final LoadRecruitingInterviewSchedulePort loadSchedulePort;
    private final SaveRecruitingInterviewSchedulePort saveSchedulePort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    private final RecruitingInterviewAvailabilityRequestCoordinator availabilityRequestCoordinator;
    private final RecruitingConcurrencyLockService concurrencyLockService;

    @Override
    public Long requestAvailability(RequestRecruitingInterviewScheduleCommand command) {
        RecruitingApplication application = concurrencyLockService.lockApplication(command.applicationId());
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            application.getRound().getSeason().getId()
        );
        validateInterviewAssigned(application);
        return availabilityRequestCoordinator.request(application, command.contactSnapshot()).getId();
    }

    @Override
    public void submitAvailability(SubmitRecruitingInterviewAvailabilityCommand command) {
        RecruitingInterviewSchedule schedule = loadSchedulePort.getByApplicationId(command.applicationId());
        schedule.getApplication().validateApplicant(command.requesterMemberId());
        validateInterviewAssigned(schedule.getApplication());
        // TODO(#1146): availability FormResponse 소유권 검증 계약이 제공되면 응답 ID를 기록하기 전에 확인한다.
        schedule.submitAvailability(command.availabilityFormResponseId());
        saveSchedulePort.saveSchedule(schedule);
    }

    @Override
    public void confirm(ConfirmRecruitingInterviewScheduleCommand command) {
        RecruitingInterviewSchedule schedule = loadSchedulePort.getByApplicationId(command.applicationId());
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            schedule.getApplication().getRound().getSeason().getId()
        );
        validateInterviewAssigned(schedule.getApplication());
        // TODO(#1146): Form 일정 교집합 계약이 제공되면 확정 전에 실제 겹침을 검증한다.
        schedule.confirm(
            command.startsAt(),
            command.endsAt(),
            command.location(),
            command.contactSnapshot()
        );
        saveSchedulePort.saveSchedule(schedule);
        // TODO(#1147): HTML 메일 계약이 제공되면 확정 메일을 발송하고 delivery 상태를 기록한다.
    }

    private void validateInterviewAssigned(RecruitingApplication application) {
        if (application.getStatus() != RecruitingApplicationStatus.INTERVIEW_ASSIGNED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION);
        }
    }
}
