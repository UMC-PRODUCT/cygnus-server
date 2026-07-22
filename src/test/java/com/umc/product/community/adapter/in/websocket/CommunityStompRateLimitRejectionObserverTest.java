package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.global.websocket.application.port.in.WebSocketRateLimitRejectionObserver;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("Community STOMP rate-limit observer")
class CommunityStompRateLimitRejectionObserverTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("approvedDestinations")
    @DisplayName("승인된 여섯 SEND destination만 해당 operation rate-limit을 기록한다")
    void recordsRateLimitForApprovedSendDestinations(String destination, Operation operation) {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WebSocketRateLimitRejectionObserver observer =
            new CommunityStompRateLimitRejectionObserver(
                new com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics(
                    registry
                )
            );

        observer.observe(destination);

        assertThat(registry.get("community.thread.realtime.rate.limit.rejections")
            .tag("operation", operation.value())
            .tag("outcome", "rejected")
            .counter()
            .count()).isEqualTo(1.0);
        assertThat(registry.getMeters()).allSatisfy(meter -> meter.getId().getTags().forEach(tag ->
            assertThat(tag.getValue()).doesNotContain("12", "34")
        ));
    }

    @Test
    @DisplayName("malformed 또는 Community 외 destination은 계측하지 않는다")
    void ignoresMalformedAndForeignDestinations() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WebSocketRateLimitRejectionObserver observer =
            new CommunityStompRateLimitRejectionObserver(
                new com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics(
                    registry
                )
            );

        observer.observe("/app/community/threads/0/messages");
        observer.observe("/app/chat/threads/12/messages");
        observer.observe(null);

        assertThat(registry.getMeters()).isEmpty();
    }

    private static Stream<Arguments> approvedDestinations() {
        return Stream.of(
            Arguments.of("/app/community/threads/12/messages", Operation.MESSAGE_CREATE),
            Arguments.of("/app/community/threads/12/messages/34/edit", Operation.MESSAGE_EDIT),
            Arguments.of("/app/community/threads/12/messages/34/delete", Operation.MESSAGE_DELETE),
            Arguments.of("/app/community/threads/12/messages/34/reactions/add", Operation.REACTION_ADD),
            Arguments.of("/app/community/threads/12/messages/34/reactions/remove", Operation.REACTION_REMOVE),
            Arguments.of("/app/community/threads/12/read", Operation.READ_UPDATE)
        );
    }
}
