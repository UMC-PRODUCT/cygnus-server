package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;

@DisplayName("WebSocketOutboundMetricInterceptor")
class WebSocketOutboundMetricInterceptorTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final WebSocketOutboundMetricInterceptor sut = new WebSocketOutboundMetricInterceptor(meterRegistry);

    @Test
    @DisplayName("MESSAGE 프레임 전송 시 outbound 메트릭이 MESSAGE 태그로 집계된다")
    void message_frame_is_counted_with_message_tag() {
        sut.preSend(simpMessage(SimpMessageType.MESSAGE), mock());

        assertThat(countOf("MESSAGE")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("CONNECT_ACK 프레임 전송 시 CONNECT_ACK 태그로 집계된다")
    void connect_ack_frame_is_counted_with_connect_ack_tag() {
        sut.preSend(simpMessage(SimpMessageType.CONNECT_ACK), mock());

        assertThat(countOf("CONNECT_ACK")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("messageType이 null인 프레임은 UNKNOWN 태그로 집계된다")
    void null_message_type_is_counted_as_unknown() {
        sut.preSend(MessageBuilder.withPayload(new byte[0]).build(), mock());

        assertThat(countOf("UNKNOWN")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("동일 타입 프레임이 반복 전송되면 횟수만큼 누적 집계된다")
    void repeated_frames_accumulate_count() {
        for (int i = 0; i < 5; i++) {
            sut.preSend(simpMessage(SimpMessageType.MESSAGE), mock());
        }

        assertThat(countOf("MESSAGE")).isEqualTo(5.0);
    }

    @Test
    @DisplayName("서로 다른 타입의 프레임은 각 태그로 독립 집계된다")
    void different_frame_types_are_counted_independently() {
        sut.preSend(simpMessage(SimpMessageType.MESSAGE), mock());
        sut.preSend(simpMessage(SimpMessageType.CONNECT_ACK), mock());

        assertThat(countOf("MESSAGE")).isEqualTo(1.0);
        assertThat(countOf("CONNECT_ACK")).isEqualTo(1.0);
    }

    private Message<byte[]> simpMessage(SimpMessageType type) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(type);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private double countOf(String commandTag) {
        Counter counter = meterRegistry.find("websocket.outbound.frames").tag("command", commandTag).counter();
        return counter != null ? counter.count() : 0.0;
    }
}
