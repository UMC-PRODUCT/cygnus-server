package com.umc.product.term.application.port.in.query.dto;

import java.util.List;

import com.umc.product.term.domain.Term;

public record RequiredTermConsentStatusInfo(
    boolean needsReconsent,
    List<TermInfo> missingRequiredTerms
) {

    public RequiredTermConsentStatusInfo {
        missingRequiredTerms = List.copyOf(missingRequiredTerms);
    }

    public static RequiredTermConsentStatusInfo fromMissingTerms(List<Term> missingRequiredTerms) {
        List<TermInfo> missingTermInfos = missingRequiredTerms.stream()
            .map(TermInfo::from)
            .toList();

        return new RequiredTermConsentStatusInfo(
            !missingTermInfos.isEmpty(),
            missingTermInfos
        );
    }
}
