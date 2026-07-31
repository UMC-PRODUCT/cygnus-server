package com.umc.product.support.fixture;

import org.springframework.stereotype.Component;

import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.domain.FcmToken;

@Component
public class FcmTokenFixture extends FixtureSupport {

    private final SaveFcmPort saveFcmPort;

    public FcmTokenFixture(SaveFcmPort saveFcmPort) {
        this.saveFcmPort = saveFcmPort;
    }

    public FcmToken FCM_토큰(Long memberId, String installationId, String token) {
        FcmToken fcmToken = FcmToken.create(
            memberId,
            installationId,
            valueOrFixture(token, "fcm-token", 100)
        );
        saveFcmPort.save(fcmToken);
        return fcmToken;
    }
}
