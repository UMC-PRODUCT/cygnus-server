package com.umc.product.global.websocket.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;

@DisplayName("WebSocketErrorPublisher")
class WebSocketErrorPublisherTest {

    private final SimpMessagingTemplate messagingTemplate =
        org.mockito.Mockito.mock(SimpMessagingTemplate.class);
    private final WebSocketErrorPublisher sut = new WebSocketErrorPublisher(messagingTemplate);

    @Test
    @DisplayName("사용자의 에러 큐로 command correlation을 포함한 typed 오류를 전송한다")
    void sendCorrelatedErrorToUser() {
        UUID commandId = UUID.fromString("b108f0c7-e244-4c9d-a57a-6e5bb8e94e89");
        UUID clientMessageId = UUID.fromString("0dce06f4-11bc-4dc2-b9fd-4f9cb88ea9cd");
        WebSocketErrorEvent event = WebSocketErrorEvent.from(
            "member-1",
            commandId,
            clientMessageId,
            AuthorizationErrorCode.RESOURCE_ACCESS_DENIED,
            false
        );

        sut.sendErrorToUser(event);

        ArgumentCaptor<WebSocketErrorPayload> responseCaptor =
            ArgumentCaptor.forClass(WebSocketErrorPayload.class);
        verify(messagingTemplate).convertAndSendToUser(
            org.mockito.ArgumentMatchers.eq("member-1"),
            org.mockito.ArgumentMatchers.eq("/queue/errors"),
            responseCaptor.capture()
        );

        WebSocketErrorPayload response = responseCaptor.getValue();
        assertThat(response.commandId()).isEqualTo(commandId);
        assertThat(response.clientMessageId()).isEqualTo(clientMessageId);
        assertThat(response.status()).isEqualTo(403);
        assertThat(response.code()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED.getCode());
        assertThat(response.message()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED.getMessage());
        assertThat(response.retryable()).isFalse();
    }

    @Test
    @DisplayName("전역 숫자 문자열 설정과 무관하게 typed 오류 status는 JSON number로 직렬화된다")
    void statusSerializesAsJsonNumber() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
            .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.mappedFeature());
        WebSocketErrorPayload payload = new WebSocketErrorPayload(
            null,
            null,
            429,
            "COMMON-429",
            "rate limited",
            true
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(payload));

        assertThat(json.path("status").isNumber()).isTrue();
        assertThat(json.path("status").asInt()).isEqualTo(429);
        assertThat(json.path("commandId").isNull()).isTrue();
        assertThat(json.path("clientMessageId").isNull()).isTrue();
    }
}
