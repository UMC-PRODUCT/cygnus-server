package com.umc.product.member.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerPart;

public record SearchMemberQuery(
    String keyword,
    Long gisuId,
    ChallengerPart part,
    Long chapterId,
    Long schoolId
) {
}
