package com.umc.product.notification.application.port.in;

import com.umc.product.notification.application.port.in.dto.FcmTokenValidationInfo;

public interface ValidateFcmTokensUseCase {

    FcmTokenValidationInfo validateDueTokens();
}
