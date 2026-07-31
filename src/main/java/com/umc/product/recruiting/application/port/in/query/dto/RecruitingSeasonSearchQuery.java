package com.umc.product.recruiting.application.port.in.query.dto;

import lombok.Builder;

@Builder
public record RecruitingSeasonSearchQuery(
    Long gisuId,
    Long chapterId,
    Long schoolId,
    Long requesterMemberId
) {
}
