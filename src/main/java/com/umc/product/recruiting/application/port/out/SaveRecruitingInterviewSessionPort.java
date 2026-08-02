package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingInterviewSession;

public interface SaveRecruitingInterviewSessionPort {

    RecruitingInterviewSession save(RecruitingInterviewSession session);

    void delete(RecruitingInterviewSession session);
}
