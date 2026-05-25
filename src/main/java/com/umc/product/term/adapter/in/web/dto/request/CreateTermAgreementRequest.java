package com.umc.product.term.adapter.in.web.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CreateTermAgreementRequest(
    @NotNull(message = "약관 ID는 필수입니다") Long termsId,

    @NotNull(message = "약관 동의 여부는 필수입니다") @AssertTrue(message = "약관 동의는 true여야 합니다") Boolean isAgreed
) {
}
