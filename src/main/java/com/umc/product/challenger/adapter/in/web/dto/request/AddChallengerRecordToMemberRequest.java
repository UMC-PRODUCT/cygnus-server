package com.umc.product.challenger.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddChallengerRecordToMemberRequest(
    @NotBlank(message = "챌린저 기록 코드는 필수입니다") @Size(min = 6, max = 6, message = "챌린저 기록 코드는 6자리여야 합니다") String code
) {
}
