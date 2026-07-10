package com.umc.product.notification.adapter.out.external.fcm;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.umc.product.notification.application.port.out.SendFcmMessagePort;
import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;

@Component
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "false", matchIfMissing = true)
public class NoopFcmMessageAdapter implements SendFcmMessagePort {

    @Override
    public FcmSendResult send(FcmSendRequest request) {
        return FcmSendResult.of(0, 0, List.of());
    }
}
