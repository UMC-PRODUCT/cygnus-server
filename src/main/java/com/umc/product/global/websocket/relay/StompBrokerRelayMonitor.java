package com.umc.product.global.websocket.relay;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
@ConditionalOnProperty(prefix = "app.websocket.broker", name = "mode", havingValue = "relay")
public class StompBrokerRelayMonitor implements HealthIndicator {

    private final AtomicInteger available = new AtomicInteger();
    private final AtomicBoolean connectedBefore = new AtomicBoolean();
    private final CountDownLatch firstConnection = new CountDownLatch(1);
    private final Counter reconnectCounter;

    public StompBrokerRelayMonitor(MeterRegistry meterRegistry) {
        Gauge.builder("websocket.broker.relay.available", available, AtomicInteger::doubleValue)
            .description("STOMP broker relay system connection availability")
            .register(meterRegistry);
        reconnectCounter = Counter.builder("websocket.broker.relay.reconnects")
            .description("STOMP broker relay reconnections after an established connection")
            .register(meterRegistry);
    }

    @EventListener
    public void onBrokerAvailability(BrokerAvailabilityEvent event) {
        if (!event.isBrokerAvailable()) {
            available.set(0);
            return;
        }

        boolean stateChanged = available.compareAndSet(0, 1);
        if (stateChanged && connectedBefore.getAndSet(true)) {
            reconnectCounter.increment();
        }
        firstConnection.countDown();
    }

    public boolean awaitAvailable(Duration timeout) throws InterruptedException {
        if (available.get() == 1) {
            return true;
        }
        return firstConnection.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
            && available.get() == 1;
    }

    @Override
    public Health health() {
        return available.get() == 1 ? Health.up().build() : Health.down().build();
    }
}
