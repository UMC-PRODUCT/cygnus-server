package com.umc.product.global.websocket.relay;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("StompBrokerRelayMonitor")
class StompBrokerRelayMonitorTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final StompBrokerRelayMonitor sut = new StompBrokerRelayMonitor(meterRegistry);

    @Test
    @DisplayName("broker 가용 상태를 health와 저카디널리티 gauge로 노출한다")
    void availabilityUpdatesHealthAndGauge() throws InterruptedException {
        sut.onBrokerAvailability(new BrokerAvailabilityEvent(true, this));

        Gauge gauge = meterRegistry.get("websocket.broker.relay.available").gauge();
        assertThat(gauge.value()).isEqualTo(1.0);
        assertThat(gauge.getId().getTags()).isEmpty();
        assertThat(sut.health().getStatus()).isEqualTo(Status.UP);
        assertThat(sut.awaitAvailable(Duration.ofMillis(1))).isTrue();
    }

    @Test
    @DisplayName("초기 연결 이후 broker가 복구되면 reconnect counter를 한 번 증가시킨다")
    void reconnectIncrementsCounterAfterDisconnect() {
        sut.onBrokerAvailability(new BrokerAvailabilityEvent(true, this));
        sut.onBrokerAvailability(new BrokerAvailabilityEvent(false, this));
        sut.onBrokerAvailability(new BrokerAvailabilityEvent(true, this));

        Counter counter = meterRegistry.get("websocket.broker.relay.reconnects").counter();
        assertThat(counter.count()).isEqualTo(1.0);
        assertThat(counter.getId().getTags()).isEmpty();
    }

    @Test
    @DisplayName("broker가 연결되지 않으면 health는 DOWN이고 startup 대기는 실패한다")
    void unavailableBrokerIsDown() throws InterruptedException {
        assertThat(sut.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(sut.awaitAvailable(Duration.ofMillis(1))).isFalse();
    }
}
