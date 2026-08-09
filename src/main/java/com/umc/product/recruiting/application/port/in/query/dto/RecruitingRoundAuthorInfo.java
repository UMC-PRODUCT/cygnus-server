package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.member.application.port.in.query.dto.MemberInfo;

/**
 * 모집 공고 작성자 표시용 정보
 */
public record RecruitingRoundAuthorInfo(
    Long memberId,
    String name,
    String nickname,
    String schoolName
) {

    public static RecruitingRoundAuthorInfo from(MemberInfo member) {
        return new RecruitingRoundAuthorInfo(
            member.id(),
            member.name(),
            member.nickname(),
            member.schoolName()
        );
    }
}
