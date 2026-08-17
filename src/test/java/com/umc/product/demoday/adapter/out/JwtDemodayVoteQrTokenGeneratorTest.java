package com.umc.product.demoday.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.config.DemodayVoteQrProperties;

class JwtDemodayVoteQrTokenGeneratorTest {

    private static final String SIGNING_KEY = "unit-test-demoday-vote-qr-signing-key-needs-32-bytes-minimum";
    private static final Long POLL_ID = 1L;
    private static final Instant WINDOW_START = Instant.parse("2026-08-17T15:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-08-17T16:00:00Z");

    private final JwtDemodayVoteQrTokenGenerator generator =
        new JwtDemodayVoteQrTokenGenerator(new DemodayVoteQrProperties(SIGNING_KEY));

    @Test
    @DisplayName("같은 pollId·같은 구간이면 항상 같은 문자열을 생성한다")
    void generateIsDeterministicForSameWindow() {
        // when
        String first = generator.generate(POLL_ID, WINDOW_START, WINDOW_END);
        String second = generator.generate(POLL_ID, WINDOW_START, WINDOW_END);

        // then
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("구간이 다르면 다른 문자열을 생성한다")
    void generateDiffersForDifferentWindow() {
        // when
        String current = generator.generate(POLL_ID, WINDOW_START, WINDOW_END);
        String next = generator.generate(POLL_ID, WINDOW_END, WINDOW_END.plusSeconds(3600));

        // then
        assertThat(current).isNotEqualTo(next);
    }

    @Test
    @DisplayName("Poll이 다르면 다른 문자열을 생성한다")
    void generateDiffersForDifferentPoll() {
        // when
        String pollOne = generator.generate(POLL_ID, WINDOW_START, WINDOW_END);
        String pollTwo = generator.generate(POLL_ID + 1, WINDOW_START, WINDOW_END);

        // then
        assertThat(pollOne).isNotEqualTo(pollTwo);
    }
}
