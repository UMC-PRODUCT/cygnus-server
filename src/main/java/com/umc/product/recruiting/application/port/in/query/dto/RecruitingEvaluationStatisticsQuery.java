package com.umc.product.recruiting.application.port.in.query.dto;

import lombok.Builder;

@Builder
public record RecruitingEvaluationStatisticsQuery(
    Long gisuId,
    Long requesterMemberId
) {
}
