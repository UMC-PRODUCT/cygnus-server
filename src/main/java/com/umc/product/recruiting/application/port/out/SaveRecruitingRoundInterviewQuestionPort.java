package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;

public interface SaveRecruitingRoundInterviewQuestionPort {

    RecruitingRoundInterviewQuestion save(RecruitingRoundInterviewQuestion question);

    void deleteByRoundId(Long roundId);
}
