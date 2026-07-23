package com.umc.product.notice.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notice.domain.NoticeClassification;
import com.umc.product.notice.domain.enums.NoticeTab;

public record NoticeFilterGraphQlRequest(
    Long gisuId,
    Long chapterId,
    Long schoolId,
    ChallengerPart part,
    NoticeTab tab,
    String keyword
) {

    public NoticeClassification toClassification() {
        return new NoticeClassification(gisuId, chapterId, schoolId, part, tab);
    }
}
