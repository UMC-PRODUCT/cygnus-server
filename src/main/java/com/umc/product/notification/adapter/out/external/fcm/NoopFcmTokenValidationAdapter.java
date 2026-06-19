package com.umc.product.notification.adapter.out.external.fcm;

import com.umc.product.notification.application.port.out.ValidateFcmTokenPort;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationRequest;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationResult;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "false", matchIfMissing = true)
public class NoopFcmTokenValidationAdapter implements ValidateFcmTokenPort {

    @Override
    public FcmTokenValidationResult validate(FcmTokenValidationRequest request) {
        return FcmTokenValidationResult.of(
            request.targets().size(),
            0,
            request.targets().stream().map(FcmSendTarget::tokenId).toList(),
            List.of()
        );
    }
}
