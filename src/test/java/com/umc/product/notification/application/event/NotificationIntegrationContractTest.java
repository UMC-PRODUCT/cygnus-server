package com.umc.product.notification.application.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.domain.IntegrationEventEnvelope;

@DisplayName("Notification integration event 계약")
class NotificationIntegrationContractTest {

    private static final String TRACEPARENT =
        "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("Java FCM envelope 직렬화 결과는 Node가 소비하는 공통 fixture와 같다")
    void FCM_envelope_fixture_호환() throws Exception {
        FcmNotificationRequestedIntegrationEvent event =
            new FcmNotificationRequestedIntegrationEvent(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                Instant.parse("2026-07-23T00:00:00Z"),
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                new FcmNotificationRequestedIntegrationEvent.Detail(
                    0,
                    1,
                    List.of(1L, 2L),
                    "공지",
                    "본문",
                    Map.of("noticeId", "10"),
                    null,
                    "umc://notices/10"
                )
            );

        JsonNode actual = objectMapper.valueToTree(IntegrationEventEnvelope.from(event, TRACEPARENT));
        JsonNode fixture = objectMapper.readTree(Path.of(
            "contracts/notification/fixtures/fcm-requested.v1.json"
        ).toFile());

        assertThat(actual.toString()).isEqualTo(fixture.toString());
    }
}
