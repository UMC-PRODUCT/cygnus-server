package com.umc.product.term.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.term.application.port.in.query.dto.RequiredTermConsentStatusInfo;

public record RequiredTermConsentStatusGraphQlResponse(
    boolean needsReconsent,
    List<TermGraphQlResponse> missingRequiredTerms
) {

    public static RequiredTermConsentStatusGraphQlResponse from(RequiredTermConsentStatusInfo info) {
        return new RequiredTermConsentStatusGraphQlResponse(
            info.needsReconsent(),
            info.missingRequiredTerms().stream().map(TermGraphQlResponse::from).toList()
        );
    }
}
