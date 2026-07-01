package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingInterviewAssignment;

public interface LoadRecruitingInterviewAssignmentPort {

    Optional<RecruitingInterviewAssignment> findAssignmentById(Long id);

    RecruitingInterviewAssignment getAssignmentById(Long id);

    Optional<RecruitingInterviewAssignment> findByApplicationIdAndInterviewerMemberId(
        Long applicationId,
        Long interviewerMemberId
    );

    List<RecruitingInterviewAssignment> listByApplicationId(Long applicationId);

    List<RecruitingInterviewAssignment> listByInterviewerMemberId(Long interviewerMemberId);
}
