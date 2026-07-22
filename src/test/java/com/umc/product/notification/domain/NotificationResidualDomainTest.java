package com.umc.product.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.notification.application.event.FcmNotificationRequestedEvent;
import com.umc.product.notification.application.event.FcmSendBatchRequestedEvent;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.domain.exception.WebhookDomainException;
import com.umc.product.notification.domain.exception.WebhookErrorCode;

@DisplayName("Notification 도메인 경계")
class NotificationResidualDomainTest {

    @Test
    @DisplayName("FCM outbox는 처리 완료 시각과 최대 재시도 실패 상태를 관리한다")
    void fcm_outbox_state_transitions() {
        FcmOutbox processed = FcmOutbox.subscribeEvent(1L);
        FcmOutbox retried = FcmOutbox.unsubscribeEvent(2L, "old-token");

        processed.markProcessed();
        retried.incrementRetry();
        retried.incrementRetry();
        retried.incrementRetry();

        assertThat(processed.getStatus()).isEqualTo(FcmOutboxStatus.PROCESSED);
        assertThat(processed.getProcessedAt()).isNotNull();
        assertThat(retried.getStatus()).isEqualTo(FcmOutboxStatus.FAILED);
        assertThat(retried.getRetryCount()).isEqualTo(3);
        assertThat(retried.getPayload()).isEqualTo("old-token");
        assertThat(retried.getEventType()).isEqualTo(FcmOutboxEventType.FCM_UNSUBSCRIBE);
    }

    @Test
    @DisplayName("FCM 요청 command는 식별자를 정규화하고 제목·본문 필수값을 거부한다")
    void request_command_normalizes_and_validates() {
        RequestFcmNotificationCommand command = new RequestFcmNotificationCommand(
            1L,
            List.of(2L, 2L, 3L),
            null,
            null,
            null,
            null,
            "제목",
            "본문",
            null,
            null,
            null
        );

        assertThat(command.memberIds()).containsExactly(2L, 3L);
        assertThat(command.targetParts()).isEmpty();
        assertThat(command.data()).isEmpty();
        assertThat(new RequestFcmNotificationCommand(
            1L, null, null, null, null, Set.of(), "제목", "본문", Map.of(), null, null
        ).memberIds()).isEmpty();
        assertThatThrownBy(() -> command(null, "본문"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("FCM 알림 제목은 필수입니다.");
        assertThatThrownBy(() -> command("제목", " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("FCM 알림 본문은 필수입니다.");
    }

    @Test
    @DisplayName("FCM event는 null 식별자와 빈 대상 목록에 안전한 기본값을 부여한다")
    void fcm_events_assign_defaults() {
        FcmNotificationRequestedEvent requested = new FcmNotificationRequestedEvent(
            null, null, null, 1L, null, null, null, null, null,
            "제목", "본문", null, null, null
        );
        FcmSendBatchRequestedEvent batch = new FcmSendBatchRequestedEvent(
            null, null, null, null, "제목", "본문", null, null, null
        );

        assertThat(requested.eventId()).isNotNull();
        assertThat(requested.occurredAt()).isNotNull();
        assertThat(requested.requestId()).isNotNull();
        assertThat(requested.memberIds()).isEmpty();
        assertThat(requested.eventType()).isEqualTo("notification.fcm.requested");
        assertThat(batch.eventId()).isNotNull();
        assertThat(batch.occurredAt()).isNotNull();
        assertThat(batch.requestId()).isNotNull();
        assertThat(batch.tokenIds()).isEmpty();
        assertThat(batch.eventType()).isEqualTo("notification.fcm.batch.requested");
        assertThat(batch.outboxDispatchMode().name()).isEqualTo("NON_TRANSACTIONAL");
    }

    @Test
    @DisplayName("Webhook event는 blank event type을 표준값으로 바꾸고 컬렉션을 방어 복사한다")
    void webhook_event_normalizes_event_type() {
        List<WebhookPlatform> platforms = new ArrayList<>(List.of(WebhookPlatform.SLACK));
        WebhookAlarmEvent event = new WebhookAlarmEvent(
            UUID.randomUUID(), Instant.now(), " ", platforms, "제목", "본문"
        );
        platforms.clear();

        assertThat(event.eventType()).isEqualTo("notification.webhook.alarm.requested");
        assertThat(event.platforms()).containsExactly(WebhookPlatform.SLACK);
    }

    @Test
    @DisplayName("Webhook 명령과 event는 빈 플랫폼을 거부한다")
    void webhook_requires_platform() {
        assertThatThrownBy(() -> new SendWebhookAlarmCommand(List.of(), "제목", "본문"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WebhookAlarmEvent.of(List.of(), "제목", "본문"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Webhook 오류 코드는 상태·code·메시지를 보존하고 도메인 예외에 연결된다")
    void webhook_error_contract() {
        assertThat(WebhookErrorCode.values()).allSatisfy(code -> {
            assertThat(code.getHttpStatus()).isNotNull();
            assertThat(code.getCode()).startsWith("WEBHOOK-");
            assertThat(code.getMessage()).isNotBlank();
            assertThat(new WebhookDomainException(code).getBaseCode()).isEqualTo(code);
        });
    }

    @Test
    @DisplayName("SaveFcmPort 기본 batch 저장은 입력 순서대로 단건 저장한다")
    void save_port_default_batch_preserves_order() {
        List<FcmToken> saved = new ArrayList<>();
        SaveFcmPort port = saved::add;
        FcmToken first = FcmToken.create(1L, "installation-1", "token-1");
        FcmToken second = FcmToken.create(2L, "installation-2", "token-2");

        port.saveAll(List.of(first, second));

        assertThat(saved).containsExactly(first, second);
    }

    private RequestFcmNotificationCommand command(String title, String body) {
        return new RequestFcmNotificationCommand(
            1L, List.of(), null, null, null, Set.of(), title, body, Map.of(), null, null
        );
    }
}
