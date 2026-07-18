package com.umc.product.registry.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

public final class MutableTestClock extends Clock {

    private Instant current;

    public MutableTestClock(Instant initial) {
        current = Objects.requireNonNull(initial, "초기 시각은 필수입니다.");
    }

    public void reset(Instant instant) {
        current = Objects.requireNonNull(instant, "reset 시각은 필수입니다.");
    }

    public void advance(Duration duration) {
        current = current.plus(Objects.requireNonNull(duration, "진행 기간은 필수입니다."));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (!ZoneOffset.UTC.equals(zone)) {
            throw new IllegalArgumentException("E2E clock은 UTC만 지원합니다.");
        }
        return this;
    }

    @Override
    public Instant instant() {
        return current;
    }
}
