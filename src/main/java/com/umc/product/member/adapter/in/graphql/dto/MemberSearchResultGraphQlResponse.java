package com.umc.product.member.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.util.EmailMasker;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;

/**
 * 스키마 {@code MemberSearchResult} 타입 응답. Node가 아니며 memberId는 Member 전역 ID로 인코딩한다.
 * rawMemberId/schoolId는 배치 로딩용 raw ID로 스키마에 노출하지 않는다.
 */
public record MemberSearchResultGraphQlResponse(
    Long rawMemberId,
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

    public String memberId() {
        return GlobalId.encode(GlobalIdTypes.MEMBER, rawMemberId);
    }

    private static String maskEmail(String email) {
        if (email == null) {
            return null;
        }

        String maskedEmail = EmailMasker.mask(email);
        return !email.isBlank() && email.equals(maskedEmail) ? "[masked-email]" : maskedEmail;
    }
}
