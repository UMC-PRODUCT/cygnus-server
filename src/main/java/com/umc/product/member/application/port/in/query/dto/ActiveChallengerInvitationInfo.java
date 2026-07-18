package com.umc.product.member.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerPart;

/**
 * Community 초대 대상에 노출할 최소 회원/챌린저 정보입니다.
 */
public record ActiveChallengerInvitationInfo(
    Long memberId,
    Long challengerId,
    String name,
    ChallengerPart part,
    Long generation
) {
}
