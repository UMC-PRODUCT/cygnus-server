package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundInterviewQuestionInfo;

public interface GetRecruitingInterviewQuestionUseCase {

    List<RecruitingRoundInterviewQuestionInfo> listActiveRoundQuestions(Long roundId, Long requesterMemberId);

    List<RecruitingApplicationInterviewQuestionInfo> listActiveApplicationQuestions(
        Long applicationId,
        Long requesterMemberId
    );
}
