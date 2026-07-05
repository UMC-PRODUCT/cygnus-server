package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotatedElementUtils;

import com.umc.product.notification.adapter.in.scheduler.FcmOutboxScheduler;

@DisplayName("Scheduler activation condition")
class SchedulerActivationConditionTest {

    @Test
    @DisplayName("FCM outbox scheduler는 FCM 활성화 시에만 등록된다")
    void fcmOutboxScheduler는_fcm_활성화시에만_등록된다() {
        assertConditionalOnProperty(FcmOutboxScheduler.class, "app.fcm.enabled");
    }

    private static void assertConditionalOnProperty(Class<?> type, String propertyName) {
        ConditionalOnProperty annotation = AnnotatedElementUtils.findMergedAnnotation(
            type,
            ConditionalOnProperty.class
        );
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).containsExactly(propertyName);
        assertThat(annotation.havingValue()).isEqualTo("true");
    }
}
