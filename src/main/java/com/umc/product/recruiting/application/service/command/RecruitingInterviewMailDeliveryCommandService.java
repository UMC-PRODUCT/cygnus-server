package com.umc.product.recruiting.application.service.command;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewMailDeliveryUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingInterviewMailDeliveryCommandService implements ManageRecruitingInterviewMailDeliveryUseCase {

    private final LoadRecruitingInterviewSchedulePort loadSchedulePort;
    private final SaveRecruitingInterviewSchedulePort saveSchedulePort;

    @Override
    public void markRequestMailSent(Long applicationId, Instant sentAt) {
        RecruitingInterviewSchedule schedule = loadSchedulePort.getByApplicationId(applicationId);
        if (schedule.getRequestMailStatus() == RecruitingMailDeliveryStatus.SENT) {
            return;
        }
        schedule.markRequestMailSent(sentAt);
        saveSchedulePort.saveSchedule(schedule);
    }

    @Override
    public void markRequestMailFailed(Long applicationId, String error) {
        RecruitingInterviewSchedule schedule = loadSchedulePort.getByApplicationId(applicationId);
        if (schedule.getRequestMailStatus() == RecruitingMailDeliveryStatus.SENT) {
            return;
        }
        schedule.markRequestMailFailed(error);
        saveSchedulePort.saveSchedule(schedule);
    }
}
