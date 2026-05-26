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

@DisplayName("WebSocketInboundMetricInterceptor")
class WebSocketInboundMetricInterceptorTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final WebSocketInboundMetricInterceptor sut = new WebSocketInboundMetricInterceptor(meterRegistry);

    @Test
    @DisplayName("CONNECT 프레임 수신 시 inbound 메트릭이 CONNECT 태그로 집계된다")
    void connect_frame_is_counted_with_connect_tag() {
        sut.preSend(simpMessage(SimpMessageType.CONNECT), mock());

        assertThat(countOf("CONNECT")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("MESSAGE 프레임 수신 시 inbound 메트릭이 MESSAGE 태그로 집계된다")
    void message_frame_is_counted_with_message_tag() {
        sut.preSend(simpMessage(SimpMessageType.MESSAGE), mock());

        assertThat(countOf("MESSAGE")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("SUBSCRIBE 프레임 수신 시 inbound 메트릭이 SUBSCRIBE 태그로 집계된다")
    void subscribe_frame_is_counted_with_subscribe_tag() {
        sut.preSend(simpMessage(SimpMessageType.SUBSCRIBE), mock());

        assertThat(countOf("SUBSCRIBE")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("messageType이 null인 프레임은 UNKNOWN 태그로 집계된다")
    void null_message_type_is_counted_as_unknown() {
        sut.preSend(MessageBuilder.withPayload(new byte[0]).build(), mock());

        assertThat(countOf("UNKNOWN")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("동일 타입 프레임이 반복 수신되면 횟수만큼 누적 집계된다")
    void repeated_frames_accumulate_count() {
        for (int i = 0; i < 3; i++) {
            sut.preSend(simpMessage(SimpMessageType.MESSAGE), mock());
        }

        assertThat(countOf("MESSAGE")).isEqualTo(3.0);
    }

    @Test
    @DisplayName("서로 다른 타입의 프레임은 각 태그로 독립 집계된다")
    void different_frame_types_are_counted_independently() {
        sut.preSend(simpMessage(SimpMessageType.CONNECT), mock());
        sut.preSend(simpMessage(SimpMessageType.MESSAGE), mock());
        sut.preSend(simpMessage(SimpMessageType.SUBSCRIBE), mock());

        assertThat(countOf("CONNECT")).isEqualTo(1.0);
        assertThat(countOf("MESSAGE")).isEqualTo(1.0);
        assertThat(countOf("SUBSCRIBE")).isEqualTo(1.0);
    }

    private Message<byte[]> simpMessage(SimpMessageType type) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(type);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private double countOf(String commandTag) {
        Counter counter = meterRegistry.find("websocket.inbound.frames").tag("command", commandTag).counter();
        return counter != null ? counter.count() : 0.0;
    }
}
