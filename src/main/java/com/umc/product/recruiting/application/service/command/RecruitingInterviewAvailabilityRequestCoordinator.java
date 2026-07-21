package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Component;

import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.recruiting.application.event.InterviewAvailabilityRequestedEvent;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingInterviewAvailabilityRequestCoordinator {

    private final LoadRecruitingInterviewSchedulePort loadSchedulePort;
    private final SaveRecruitingInterviewSchedulePort saveSchedulePort;
    private final DomainEventPublisher eventPublisher;

    public RecruitingInterviewSchedule request(RecruitingApplication application, String contactSnapshot) {
        return loadSchedulePort.findByApplicationId(application.getId())
            .map(this::retryFailedRequest)
            .orElseGet(() -> createRequest(application, contactSnapshot));
    }

    private RecruitingInterviewSchedule createRequest(
        RecruitingApplication application,
        String contactSnapshot
    ) {
        validateAvailabilityForm(application);
        RecruitingInterviewSchedule schedule = saveSchedulePort.saveSchedule(
            RecruitingInterviewSchedule.requestAvailability(application, contactSnapshot)
        );
        eventPublisher.publish(event(application));
        return schedule;
    }

    private RecruitingInterviewSchedule retryFailedRequest(RecruitingInterviewSchedule schedule) {
        if (schedule.getRequestMailStatus() != RecruitingMailDeliveryStatus.FAILED) {
            return schedule;
        }
        schedule.retryRequestMail();
        RecruitingInterviewSchedule saved = saveSchedulePort.saveSchedule(schedule);
        eventPublisher.publish(event(schedule.getApplication()));
        return saved;
    }

    private InterviewAvailabilityRequestedEvent event(RecruitingApplication application) {
        return InterviewAvailabilityRequestedEvent.of(application.getId());
    }

    private void validateAvailabilityForm(RecruitingApplication application) {
        Long availabilityFormId = application.getRound().getAvailabilityFormId();
        if (availabilityFormId == null || availabilityFormId <= 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_INVALID_SCHEDULE);
        }
    }
}
