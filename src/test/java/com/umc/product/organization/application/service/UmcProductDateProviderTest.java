package com.umc.product.organization.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UMC Product 기준 날짜 제공자")
class UmcProductDateProviderTest {

    @Test
    void KST_자정_직전에는_이전_날짜를_반환한다() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-12T14:59:59Z"), ZoneOffset.UTC);

        LocalDate result = new UmcProductDateProvider(clock).today();

        assertThat(result).isEqualTo(LocalDate.of(2026, 7, 12));
    }

    @Test
    void KST_자정부터는_다음_날짜를_반환한다() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-12T15:00:00Z"), ZoneOffset.UTC);

        LocalDate result = new UmcProductDateProvider(clock).today();

        assertThat(result).isEqualTo(LocalDate.of(2026, 7, 13));
    }
}
