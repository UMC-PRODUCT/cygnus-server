package com.umc.product.global.event.application.service;

import org.springframework.stereotype.Component;

import com.umc.product.global.event.domain.EventOutboxStatus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Component
public class EventOutboxRelayMetrics {

    private final Counter retryCounter;
    private final Counter failedCounter;

    public EventOutboxRelayMetrics(MeterRegistry meterRegistry) {
        retryCounter = Counter.builder("event.outbox.relay.retry")
            .description("Event outbox publish failures scheduled for retry")
            .register(meterRegistry);
        failedCounter = Counter.builder("event.outbox.relay.failed")
            .description("Event outbox publish failures exhausted retries")
            .register(meterRegistry);
    }

    static EventOutboxRelayMetrics noOp() {
        return new EventOutboxRelayMetrics(new SimpleMeterRegistry());
    }

    public void recordFailure(EventOutboxStatus status) {
        switch (status) {
            case PENDING -> retryCounter.increment();
            case FAILED -> failedCounter.increment();
            case PROCESSING, PUBLISHED -> {
            }
        }
    }
}
