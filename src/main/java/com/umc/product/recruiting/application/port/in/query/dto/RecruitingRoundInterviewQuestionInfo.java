package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.recruiting.domain.RecruitingRoundInterviewQuestion;

public record RecruitingRoundInterviewQuestionInfo(
    Long id,
    Long roundId,
    String content,
    Integer orderNo,
    boolean active
) {

    public static RecruitingRoundInterviewQuestionInfo from(RecruitingRoundInterviewQuestion question) {
        return new RecruitingRoundInterviewQuestionInfo(
            question.getId(),
            question.getRound().getId(),
            question.getContent(),
            question.getOrderNo(),
            question.isActive()
        );
    }
}
