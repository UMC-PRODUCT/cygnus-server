package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

/**
 * 면접 질문 전역 ID를 담는 뮤테이션 payload.
 */
public record RecruitingInterviewQuestionIdGraphQlPayload(String questionId) {

    public static RecruitingInterviewQuestionIdGraphQlPayload of(Long rawQuestionId) {
        return new RecruitingInterviewQuestionIdGraphQlPayload(
            GlobalId.encode(GlobalIdTypes.RECRUITING_INTERVIEW_QUESTION, rawQuestionId)
        );
    }
}
