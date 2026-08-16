package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;

public interface SaveRecruitingInterviewSchedulePort {

    RecruitingInterviewSchedule saveSchedule(RecruitingInterviewSchedule schedule);

    void saveAllAndFlush(List<RecruitingInterviewSchedule> schedules);
}
