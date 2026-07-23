package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;

public interface SaveRecruitingInterviewSchedulePort {

    RecruitingInterviewSchedule saveSchedule(RecruitingInterviewSchedule schedule);
}
