package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;

class RecruitingRoundApplicationWindowTest {

    private static final Instant DOCUMENT_START = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant DOCUMENT_END = Instant.parse("2026-08-08T00:00:00Z");

    @Test
    @DisplayName("DRAFT 차수는 접수할 수 없다")
    void draftRoundIsNotApplicationOpen() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        RecruitingRound round = configuredRound(season);
        boolean open = round.isLocalApplicationPeriodOpenAt(DOCUMENT_START, RecruitingApplicationFormStatus.PUBLISHED);

        assertThat(open).isFalse();
    }

    @Test
    @DisplayName("접수 시작 시각은 접수 가능하다")
    void documentStartIsInclusive() {
        RecruitingRound round = activeOpenRound();

        boolean open = round.isLocalApplicationPeriodOpenAt(DOCUMENT_START, RecruitingApplicationFormStatus.PUBLISHED);

        assertThat(open).isTrue();
    }

    @Test
    @DisplayName("접수 종료 시각은 exclusive이므로 접수할 수 없다")
    void documentEndIsExclusive() {
        RecruitingRound round = activeOpenRound();

        boolean open = round.isLocalApplicationPeriodOpenAt(DOCUMENT_END, RecruitingApplicationFormStatus.PUBLISHED);

        assertThat(open).isFalse();
    }

    @Test
    @DisplayName("게시되지 않은 Form은 접수할 수 없다")
    void unpublishedFormIsNotApplicationOpen() {
        RecruitingRound round = activeOpenRound();

        boolean open = round.isLocalApplicationPeriodOpenAt(DOCUMENT_START, RecruitingApplicationFormStatus.DRAFT);

        assertThat(open).isFalse();
    }

    @Test
    @DisplayName("접수 시작 전에는 접수할 수 없다")
    void beforeDocumentWindowIsNotApplicationOpen() {
        RecruitingRound round = activeOpenRound();

        boolean open = round.isLocalApplicationPeriodOpenAt(
            DOCUMENT_START.minusNanos(1),
            RecruitingApplicationFormStatus.PUBLISHED
        );

        assertThat(open).isFalse();
    }

    @Test
    @DisplayName("접수 종료 후에는 접수할 수 없다")
    void afterDocumentWindowIsNotApplicationOpen() {
        RecruitingRound round = activeOpenRound();

        boolean open = round.isLocalApplicationPeriodOpenAt(
            DOCUMENT_END.plusNanos(1),
            RecruitingApplicationFormStatus.PUBLISHED
        );

        assertThat(open).isFalse();
    }

    @Test
    @DisplayName("CLOSED 차수는 접수할 수 없다")
    void closedRoundIsNotApplicationOpen() {
        RecruitingRound round = activeOpenRound();
        round.close();

        boolean open = round.isLocalApplicationPeriodOpenAt(DOCUMENT_START, RecruitingApplicationFormStatus.PUBLISHED);

        assertThat(open).isFalse();
    }

    private RecruitingRound activeOpenRound() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        RecruitingRound round = configuredRound(season);
        round.open();
        return round;
    }

    private RecruitingRound configuredRound(RecruitingSeason season) {
        return RecruitingRound.createRegular(season, RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.PLAN),
            false,
            DOCUMENT_START,
            DOCUMENT_END,
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null
        ));
    }
}
