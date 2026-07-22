package com.umc.product.recruiting.application.port.in.query;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundInterviewQuestionInfo;

public interface GetRecruitingInterviewQuestionUseCase {

    List<RecruitingRoundInterviewQuestionInfo> listActiveRoundQuestions(Long roundId, Long requesterMemberId);

    List<RecruitingApplicationInterviewQuestionInfo> listActiveApplicationQuestions(
        Long applicationId,
        Long requesterMemberId
    );

    Map<Long, List<RecruitingRoundInterviewQuestionInfo>> listActiveRoundQuestionsByRoundIds(
        Set<Long> roundIds,
        Long requesterMemberId
    );

    Map<Long, List<RecruitingApplicationInterviewQuestionInfo>> listActiveApplicationQuestionsByApplicationIds(
        Set<Long> applicationIds,
        Long requesterMemberId
    );
}
