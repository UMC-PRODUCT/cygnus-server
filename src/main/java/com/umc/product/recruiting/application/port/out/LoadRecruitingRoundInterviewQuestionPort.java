package com.umc.product.recruiting.application.port.out;

import java.util.List;

import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;

public interface LoadRecruitingRoundInterviewQuestionPort {

    RecruitingRoundInterviewQuestion getById(Long id);

    List<RecruitingRoundInterviewQuestion> listByRoundId(Long roundId);

    List<RecruitingRoundInterviewQuestion> listActiveByRoundId(Long roundId);
}
