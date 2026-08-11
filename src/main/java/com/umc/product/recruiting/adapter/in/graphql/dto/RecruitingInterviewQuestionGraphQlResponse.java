package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundInterviewQuestionInfo;

public final class RecruitingInterviewQuestionGraphQlResponse {

    private RecruitingInterviewQuestionGraphQlResponse() {
    }

    public record RoundQuestion(
        String questionId,
        String roundId,
        String content,
        Integer orderNo,
        boolean active
    ) {

        public static RoundQuestion from(RecruitingRoundInterviewQuestionInfo info) {
            return new RoundQuestion(
                GlobalId.encode(GlobalIdTypes.RECRUITING_INTERVIEW_QUESTION, info.id()),
                GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND, info.roundId()),
                info.content(),
                info.orderNo(),
                info.active()
            );
        }
    }

    public record ApplicationQuestion(
        String questionId,
        String applicationId,
        String content,
        Integer orderNo,
        boolean active
    ) {

        public static ApplicationQuestion from(RecruitingApplicationInterviewQuestionInfo info) {
            return new ApplicationQuestion(
                GlobalId.encode(GlobalIdTypes.RECRUITING_INTERVIEW_QUESTION, info.id()),
                GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, info.applicationId()),
                info.content(),
                info.orderNo(),
                info.active()
            );
        }
    }
}
