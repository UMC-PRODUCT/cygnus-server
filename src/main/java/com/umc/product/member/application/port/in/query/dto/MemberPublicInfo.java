package com.umc.product.member.application.port.in.query.dto;

public record MemberPublicInfo(
    Long memberId,
    String name,
    String nickname,
    Long schoolId,
    String schoolName,
    String profileImageLink
) {

    public static MemberPublicInfo from(MemberInfo info) {
        return new MemberPublicInfo(
            info.id(),
            info.name(),
            info.nickname(),
            info.schoolId(),
            info.schoolName(),
            info.profileImageLink()
        );
    }

    public static MemberPublicInfo from(SearchMemberItemV2Info info) {
        return new MemberPublicInfo(
            info.memberId(),
            info.name(),
            info.nickname(),
            info.schoolId(),
            info.schoolName(),
            info.profileImageLink()
        );
    }
}
