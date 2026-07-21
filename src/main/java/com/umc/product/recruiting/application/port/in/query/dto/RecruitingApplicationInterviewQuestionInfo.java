package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.recruiting.domain.RecruitingApplicationInterviewQuestion;

public record RecruitingApplicationInterviewQuestionInfo(
    Long id,
    Long applicationId,
    String content,
    Integer orderNo,
    boolean active
) {

    public static RecruitingApplicationInterviewQuestionInfo from(RecruitingApplicationInterviewQuestion question) {
        return new RecruitingApplicationInterviewQuestionInfo(
            question.getId(),
            question.getApplication().getId(),
            question.getContent(),
            question.getOrderNo(),
            question.isActive()
        );
    }
}
