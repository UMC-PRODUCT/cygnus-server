package com.umc.product.member.application.port.in.query.dto;

import org.springframework.lang.Nullable;

import com.umc.product.common.domain.enums.ChallengerPart;

/**
 * Community 초대 대상에 노출할 최소 회원 정보입니다.
 * Challenger 관련 필드는 이력이 없는 회원이면 {@code null}입니다.
 */
public record MemberInvitationInfo(
    Long memberId,
    @Nullable Long challengerId,
    String name,
    @Nullable ChallengerPart part,
    @Nullable Long generation
) {
}
