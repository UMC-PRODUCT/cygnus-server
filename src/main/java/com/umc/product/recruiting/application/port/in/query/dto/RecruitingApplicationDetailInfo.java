package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.List;

import com.umc.product.form.application.port.in.query.dto.AnswerInfo;

import lombok.Builder;

@Builder
public record RecruitingApplicationDetailInfo(
    RecruitingApplicationSummaryInfo application,
    Long formResponseId,
    List<AnswerInfo> answers
) {
}
