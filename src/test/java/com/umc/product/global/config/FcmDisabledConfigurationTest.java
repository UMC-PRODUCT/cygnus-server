package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.product.notification.adapter.out.external.fcm.FirebaseFcmMessageAdapter;
import com.umc.product.notification.adapter.out.external.fcm.FirebaseFcmTokenValidationAdapter;
import com.umc.product.notification.adapter.out.external.fcm.NoopFcmMessageAdapter;
import com.umc.product.notification.adapter.out.external.fcm.NoopFcmTokenValidationAdapter;
import com.umc.product.notification.application.port.out.SendFcmMessagePort;
import com.umc.product.notification.application.port.out.ValidateFcmTokenPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("FCM disabled configuration")
class FcmDisabledConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withPropertyValues(
            "app.fcm.enabled=false",
            "app.fcm.firebase-configuration="
        )
        .withUserConfiguration(
            FcmConfig.class,
            NoopFcmMessageAdapter.class,
            FirebaseFcmMessageAdapter.class,
            NoopFcmTokenValidationAdapter.class,
            FirebaseFcmTokenValidationAdapter.class
        );

    @Test
    @DisplayName("FCM 비활성화와 빈 Firebase credential에서도 Firebase 빈 없이 부팅된다")
    void disabled_fcm_boots_without_firebase_credentials() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SendFcmMessagePort.class);
            assertThat(context).hasSingleBean(ValidateFcmTokenPort.class);
            assertThat(context).hasSingleBean(NoopFcmMessageAdapter.class);
            assertThat(context).hasSingleBean(NoopFcmTokenValidationAdapter.class);
            assertThat(context).doesNotHaveBean(FirebaseMessaging.class);
            assertThat(context).doesNotHaveBean(FirebaseFcmMessageAdapter.class);
            assertThat(context).doesNotHaveBean(FirebaseFcmTokenValidationAdapter.class);
        });
    }
}
