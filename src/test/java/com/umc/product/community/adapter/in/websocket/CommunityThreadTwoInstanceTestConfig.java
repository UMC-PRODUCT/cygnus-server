package com.umc.product.community.adapter.in.websocket;

import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;

import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.product.storage.application.port.out.StoragePort;

@TestConfiguration(proxyBeanMethods = false)
class CommunityThreadTwoInstanceTestConfig {

    @Bean(name = "firebaseMessaging")
    FirebaseMessaging firebaseMessaging() {
        return mock(FirebaseMessaging.class);
    }

    @Bean
    @Primary
    JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    StoragePort storagePort() {
        return mock(StoragePort.class);
    }

    @Bean
    RelayAvailabilityProbe relayAvailabilityProbe() {
        return new RelayAvailabilityProbe();
    }

    @Bean
    @Primary
    CommunityThreadE2EClock communityThreadE2EClock() {
        return new CommunityThreadE2EClock(Instant.parse("2026-07-18T00:00:00Z"));
    }
}

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

final class CommunityThreadE2EClock extends Clock {

    private final AtomicReference<Instant> current;
    private final ZoneId zone;

    CommunityThreadE2EClock(Instant initial) {
        this(new AtomicReference<>(initial), ZoneOffset.UTC);
    }

    private CommunityThreadE2EClock(AtomicReference<Instant> current, ZoneId zone) {
        this.current = current;
        this.zone = zone;
    }

    void advance(Duration duration) {
        current.updateAndGet(instant -> instant.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId requestedZone) {
        return zone.equals(requestedZone) ? this : new CommunityThreadE2EClock(current, requestedZone);
    }

    @Override
    public Instant instant() {
        return current.get();
    }
}
