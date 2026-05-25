package com.umc.product.term.application.port.in.query.dto;

import java.util.List;

import com.umc.product.term.domain.Term;

public record RequiredTermConsentStatusInfo(
    boolean needsReconsent,
    List<TermInfo> missingRequiredTerms,
    List<Long> agreedRequiredTermIds
) {

    public RequiredTermConsentStatusInfo {
        missingRequiredTerms = List.copyOf(missingRequiredTerms);
        agreedRequiredTermIds = List.copyOf(agreedRequiredTermIds);
    }

    public RequiredTermConsentStatusInfo(boolean needsReconsent, List<TermInfo> missingRequiredTerms) {
        this(needsReconsent, missingRequiredTerms, List.of());
    }

    public static RequiredTermConsentStatusInfo fromMissingTerms(List<Term> missingRequiredTerms) {
        List<TermInfo> missingTermInfos = missingRequiredTerms.stream()
            .map(TermInfo::from)
            .toList();

        return new RequiredTermConsentStatusInfo(
            !missingTermInfos.isEmpty(),
            missingTermInfos,
            List.of()
        );
    }

    public static RequiredTermConsentStatusInfo fromRequiredTerms(
        List<Term> requiredTerms,
        List<Long> agreedRequiredTermIds
    ) {
        List<TermInfo> missingTermInfos = requiredTerms.stream()
            .filter(term -> !agreedRequiredTermIds.contains(term.getId()))
            .map(TermInfo::from)
            .toList();

        return new RequiredTermConsentStatusInfo(
            !missingTermInfos.isEmpty(),
            missingTermInfos,
            agreedRequiredTermIds
        );
    }
}
