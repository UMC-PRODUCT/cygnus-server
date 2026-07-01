package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;

public interface FindRecruitingScheduleOverlapPort {

    List<RecruitingInterviewScheduleCandidate> findOverlaps(Long formId, List<Long> formResponseIds);
}
