package com.umc.product.notification.application.port.out;

import com.umc.product.notification.application.port.out.dto.FcmTokenValidationRequest;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationResult;

public interface ValidateFcmTokenPort {

    FcmTokenValidationResult validate(FcmTokenValidationRequest request);
}
