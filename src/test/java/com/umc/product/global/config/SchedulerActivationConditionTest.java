package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotatedElementUtils;

import com.umc.product.notification.adapter.in.scheduler.FcmTokenValidationScheduler;

@DisplayName("Scheduler activation condition")
class SchedulerActivationConditionTest {

    @Test
    @DisplayName("FCM 토큰 검증 scheduler는 FCM과 토큰 검증이 모두 활성화된 인스턴스에만 등록된다")
    void fcmTokenValidationScheduler는_fcm과_토큰검증_활성화시에만_등록된다() {
        assertConditionalOnProperty(
            FcmTokenValidationScheduler.class,
            "app.fcm.enabled",
            "app.fcm.token-validation-enabled"
        );
    }

    private static void assertConditionalOnProperty(Class<?> type, String... propertyNames) {
        ConditionalOnProperty annotation = AnnotatedElementUtils.findMergedAnnotation(
            type,
            ConditionalOnProperty.class
        );
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).containsExactly(propertyNames);
        assertThat(annotation.havingValue()).isEqualTo("true");
    }
}
