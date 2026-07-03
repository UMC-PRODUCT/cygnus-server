package com.umc.product.challenger.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddChallengerRecordToMemberRequest(
    @NotBlank(message = "챌린저 기록 코드는 필수입니다") @Size(min = 6, max = 6, message = "챌린저 기록 코드는 6자리여야 합니다") String code,

    // 운영진 코드는 이메일 검증을 생략하므로 토큰은 선택적 필드다.
    // 일반 코드의 경우 verifiedEmail null 검증은 consumeCode 내부에서 수행된다.
    String emailVerificationToken
) {
}
