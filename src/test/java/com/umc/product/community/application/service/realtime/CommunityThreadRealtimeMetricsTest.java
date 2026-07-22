package com.umc.product.community.application.service.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Reason;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("Community thread realtime metrics")
class CommunityThreadRealtimeMetricsTest {

    @Test
    @DisplayName("모든 realtime metric tag key와 value는 허용된 유한 enum 집합에 속한다")
    void recordsOnlyBoundedBucketTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CommunityThreadRealtimeMetrics sut = new CommunityThreadRealtimeMetrics(registry);
        Map<String, List<String>> allowedTagValues = Map.of(
            "operation", Arrays.stream(Operation.values()).map(Operation::value).toList(),
            "outcome", Arrays.stream(Outcome.values()).map(Outcome::value).toList(),
            "reason", Arrays.stream(Reason.values()).map(Reason::value).toList()
        );

        sut.recordSend(Operation.MESSAGE_CREATE, Outcome.SUCCESS);
        sut.recordReject(Operation.MESSAGE_EDIT, Reason.VALIDATION);
        sut.recordRateLimit(Operation.REACTION_ADD);
        sut.recordFanOut(Operation.MESSAGE_CREATED, Outcome.FAILURE, 3);
        sut.recordBroadcastFailure(Operation.MESSAGE_CREATED, Reason.BROKER_UNAVAILABLE);
        sut.recordBackfill(Operation.MESSAGE_HISTORY, Outcome.SUCCESS);

        List<Tag> tags = registry.getMeters().stream()
            .flatMap(meter -> meter.getId().getTags().stream())
            .toList();
        assertThat(registry.getMeters()).hasSize(7);
        assertThat(tags)
            .allSatisfy(tag -> {
                assertThat(allowedTagValues).containsKey(tag.getKey());
                assertThat(allowedTagValues.get(tag.getKey())).contains(tag.getValue());
            });
        assertThat(registry.get("community.thread.realtime.fanout.events").counter().count())
            .isEqualTo(1.0);
        assertThat(registry.get("community.thread.realtime.fanout.recipients").summary().totalAmount())
            .isEqualTo(3.0);
        assertThat(registry.get("community.thread.realtime.broadcast.failures").counter().count())
            .isEqualTo(1.0);
    }

    @Test
    @DisplayName("음수 fan-out recipient 수는 metric을 기록하지 않고 거절한다")
    void negativeFanOutRecipientCountRejected() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CommunityThreadRealtimeMetrics sut = new CommunityThreadRealtimeMetrics(registry);

        assertThatThrownBy(() -> sut.recordFanOut(Operation.MESSAGE_CREATED, Outcome.FAILURE, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("recipientCount must not be negative");
        assertThat(registry.getMeters()).isEmpty();
    }
}
