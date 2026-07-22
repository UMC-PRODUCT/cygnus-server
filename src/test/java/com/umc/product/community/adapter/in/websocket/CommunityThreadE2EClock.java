package com.umc.product.community.adapter.in.websocket;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

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
