package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.dto.request.CreateCommunityThreadMessageRequest;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.websocket.handler.WebSocketErrorEvent;

import jakarta.validation.ConstraintViolationException;

@DisplayName("Community STOMP 오류 매핑")
class CommunityStompErrorMapperTest {

    private static final UUID COMMAND_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CLIENT_MESSAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final CapturingPublisher publisher = new CapturingPublisher();
    private final CommunityStompErrorMapper mapper =
        new CommunityStompErrorMapper(publisher, new ObjectMapper());

    @Test
    @DisplayName("principal이 없으면 사용자별 오류를 발행하지 않고 즉시 거부한다")
    void rejectsMissingPrincipal() {
        assertThatThrownBy(() -> mapper.publish(null, rawMessage("{}"), new RuntimeException()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("principal must not be null");
        assertThat(publisher.events).isEmpty();
    }

    @Test
    @DisplayName("destination과 command ID가 유효하면 command 및 client message 상관관계를 보존한다")
    void preservesCommandCorrelationFromTypedPayload() {
        CreateCommunityThreadMessageRequest payload = new CreateCommunityThreadMessageRequest(
            CLIENT_MESSAGE_ID.toString(),
            CommunityThreadMessageType.TEXT,
            "메시지",
            List.of(),
            List.of(),
            null
        );

        mapper.publish(principal(), stompMessage(payload),
            new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED));

        WebSocketErrorEvent event = publisher.last();
        assertThat(event.userName()).isEqualTo("41");
        assertThat(event.commandId()).isEqualTo(COMMAND_ID);
        assertThat(event.clientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);
        assertThat(event.code()).isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED.getCode());
        assertThat(event.retryable()).isFalse();
    }

    @Test
    @DisplayName("헤더가 없으면 raw JSON의 canonical clientMessageId만 복원해 오류를 발행한다")
    void extractsClientMessageIdWithoutStompHeaders() {
        mapper.publish(principal(), rawMessage("{\"clientMessageId\":\"" + CLIENT_MESSAGE_ID + "\"}"),
            new IllegalArgumentException("invalid"));
        mapper.publish(principal(), rawBytes("{\"clientMessageId\":1}"), new IllegalArgumentException());
        mapper.publish(principal(), rawMessage("null"), new IllegalArgumentException());
        mapper.publish(principal(), rawMessage("{"), new IllegalArgumentException());
        mapper.publish(principal(), MessageBuilder.withPayload(new Object()).build(),
            new IllegalArgumentException());

        assertThat(publisher.events).hasSize(5);
        assertThat(publisher.events.getFirst().clientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);
        assertThat(publisher.events.subList(1, 5))
            .allSatisfy(event -> assertThat(event.clientMessageId()).isNull());
    }

    @Test
    @DisplayName("validation 계열 예외는 원인 체인을 따라 BAD_REQUEST로 정규화한다")
    void mapsValidationFailuresToBadRequest() {
        List<Throwable> failures = List.of(
            new IllegalArgumentException("invalid"),
            new ConstraintViolationException(Set.of()),
            new MessageConversionException("invalid"),
            new JsonParseException(null, "invalid")
        );

        failures.forEach(failure -> mapper.publish(correlation(), new RuntimeException(failure)));

        assertThat(publisher.events).hasSize(failures.size())
            .allSatisfy(event -> {
                assertThat(event.code()).isEqualTo(CommonErrorCode.BAD_REQUEST.getCode());
                assertThat(event.retryable()).isFalse();
            });
    }

    @Test
    @DisplayName("알 수 없는 예외와 순환 cause는 INTERNAL_SERVER_ERROR이며 재시도 가능하다")
    void mapsUnknownAndCyclicFailuresToInternalServerError() {
        RuntimeException cyclic = new RuntimeException("cyclic") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        mapper.publish(correlation(), new RuntimeException("unknown"));
        mapper.publish(correlation(), cyclic);

        assertThat(publisher.events).hasSize(2)
            .allSatisfy(event -> {
                assertThat(event.code()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
                assertThat(event.retryable()).isTrue();
            });
    }

    private CommunityStompCorrelation correlation() {
        return new CommunityStompCorrelation(
            "41",
            COMMAND_ID,
            CLIENT_MESSAGE_ID,
            CommunityStompCommandType.MESSAGE_CREATE
        );
    }

    private Principal principal() {
        return () -> "41";
    }

    private Message<Object> stompMessage(Object payload) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/community/threads/12/messages");
        accessor.setNativeHeader("x-command-id", COMMAND_ID.toString());
        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }

    private Message<String> rawMessage(String payload) {
        return MessageBuilder.withPayload(payload).build();
    }

    private Message<byte[]> rawBytes(String payload) {
        return MessageBuilder.withPayload(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)).build();
    }

    private static final class CapturingPublisher implements ApplicationEventPublisher {

        private final List<WebSocketErrorEvent> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            events.add((WebSocketErrorEvent) event);
        }

        private WebSocketErrorEvent last() {
            return events.getLast();
        }
    }
}
