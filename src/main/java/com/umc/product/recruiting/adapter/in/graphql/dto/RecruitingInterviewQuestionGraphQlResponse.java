package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInterviewQuestionInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundInterviewQuestionInfo;

public final class RecruitingInterviewQuestionGraphQlResponse {

    private RecruitingInterviewQuestionGraphQlResponse() {
    }

    public record RoundQuestion(Long id, Long roundId, String content, Integer orderNo, boolean active) {

        public static RoundQuestion from(RecruitingRoundInterviewQuestionInfo info) {
            return new RoundQuestion(info.id(), info.roundId(), info.content(), info.orderNo(), info.active());
        }
    }

    public record ApplicationQuestion(
        Long id,
        Long applicationId,
        String content,
        Integer orderNo,
        boolean active
    ) {

        public static ApplicationQuestion from(RecruitingApplicationInterviewQuestionInfo info) {
            return new ApplicationQuestion(
                info.id(),
                info.applicationId(),
                info.content(),
                info.orderNo(),
                info.active()
            );
        }
    }
}
