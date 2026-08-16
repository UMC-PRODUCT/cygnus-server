package com.umc.product.member.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.global.util.EmailMasker;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;

public record MemberSearchResultGraphQlResponse(
    Long memberId,
    String name,
    String nickname,
    String email,
    Long schoolId,
    String profileImageLink,
    MemberSearchChallengerGraphQlResponse currentChallenger,
    boolean isAdminInActiveGisu,
    List<MemberSearchChallengerGraphQlResponse> challengerRecords
) {

    public static MemberSearchResultGraphQlResponse from(SearchMemberItemV2Info info) {
        return new MemberSearchResultGraphQlResponse(
            info.memberId(),
            info.name(),
            info.nickname(),
            maskEmail(info.email()),
            info.schoolId(),
            info.profileImageLink(),
            info.primaryChallenger() == null
                ? null
                : MemberSearchChallengerGraphQlResponse.from(info.primaryChallenger()),
            info.isAdminInActiveGisu(),
            info.participations().stream()
                .map(MemberSearchChallengerGraphQlResponse::from)
                .toList()
        );
    }

    private static String maskEmail(String email) {
        if (email == null) {
            return null;
        }

        String maskedEmail = EmailMasker.mask(email);
        return !email.isBlank() && email.equals(maskedEmail) ? "[masked-email]" : maskedEmail;
    }
}
