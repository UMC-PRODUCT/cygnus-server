package com.umc.product.feedback.adapter.in.graphql.dto;

import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;

public record FeedbackFormOptionGraphQlResponse(
    Long optionId,
    String content,
    Long orderNo,
    boolean other
) {

    public static FeedbackFormOptionGraphQlResponse from(FormWithStructureInfo.Option option) {
        return new FeedbackFormOptionGraphQlResponse(
            option.optionId(),
            option.content(),
            option.orderNo(),
            option.isOther()
        );
    }
}
