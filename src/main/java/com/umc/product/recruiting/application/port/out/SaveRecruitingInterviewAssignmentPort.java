package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingInterviewAssignment;

public interface SaveRecruitingInterviewAssignmentPort {

    RecruitingInterviewAssignment saveAssignment(RecruitingInterviewAssignment assignment);
}
