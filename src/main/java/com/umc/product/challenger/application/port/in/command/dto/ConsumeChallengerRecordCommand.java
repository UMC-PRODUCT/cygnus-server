package com.umc.product.challenger.application.port.in.command.dto;

import lombok.Builder;

@Builder
public record ConsumeChallengerRecordCommand(
    Long targetMemberId,
    String code,
    // emailVerificationToken 으로 사전 검증된 이메일. 소유자 본인 확인에 사용한다.
    String verifiedEmail
) {
}
