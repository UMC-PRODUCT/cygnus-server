package com.umc.product.community.adapter.in.websocket;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;

final class RelayAvailabilityProbe {

    private final BlockingQueue<Boolean> changes = new LinkedBlockingQueue<>();

    @EventListener
    void onBrokerAvailability(BrokerAvailabilityEvent event) {
        changes.offer(event.isBrokerAvailable());
    }

    boolean await(boolean expected, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return false;
            }
            Boolean state = changes.poll(remaining, TimeUnit.NANOSECONDS);
            if (state == null) {
                return false;
            }
            if (state == expected) {
                return true;
            }
        }
    }

    void discardPending() {
        changes.clear();
    }
}
