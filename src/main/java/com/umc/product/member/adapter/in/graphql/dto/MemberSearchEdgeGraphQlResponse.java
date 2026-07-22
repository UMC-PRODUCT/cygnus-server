package com.umc.product.member.adapter.in.graphql.dto;

import com.umc.product.global.util.EmailMasker;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;

public record MemberSearchEdgeGraphQlResponse(
    MemberPublicInfo member,
    String maskedEmail,
    Long currentChallengerId,
    boolean isAdminInActiveGisu
) {

    public static MemberSearchEdgeGraphQlResponse from(SearchMemberItemV2Info info) {
        return new MemberSearchEdgeGraphQlResponse(
            MemberPublicInfo.from(info),
            maskEmail(info.email()),
            info.primaryChallenger() == null ? null : info.primaryChallenger().challengerId(),
            info.isAdminInActiveGisu()
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
