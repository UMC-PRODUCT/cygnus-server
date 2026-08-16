package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.demoday.domain.enums.DemodayPollStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@DisplayName("DemodayPoll")
class DemodayPollTest {

    private static final Long GISU_ID = 8L;
    private static final String NAME = "8기 데모데이 현장 투표";
    private static final Instant OPENS_AT = Instant.parse("2026-08-01T05:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-08-01T08:00:00Z");

    @Test
    @DisplayName("생성된 투표는 창을 그대로 보관하고 항상 닫힌 상태로 시작한다")
    void initializePollAsClosed() {
        // when
        DemodayPoll poll = DemodayPoll.create(GISU_ID, NAME, OPENS_AT, CLOSES_AT);

        // then
        assertThat(poll.getGisuId()).isEqualTo(GISU_ID);
        assertThat(poll.getName()).isEqualTo(NAME);
        assertThat(poll.getOpensAt()).isEqualTo(OPENS_AT);
        assertThat(poll.getClosesAt()).isEqualTo(CLOSES_AT);
        assertThat(poll.getStatus()).isEqualTo(DemodayPollStatus.CLOSED);
    }

    @Test
    @DisplayName("이름은 앞뒤 공백을 제거한 값으로 저장된다")
    void normalizePollName() {
        // when
        DemodayPoll poll = DemodayPoll.create(GISU_ID, "  8기 데모데이  ", OPENS_AT, CLOSES_AT);

        // then
        assertThat(poll.getName()).isEqualTo("8기 데모데이");
    }

    @Test
    @DisplayName("기수 없이는 투표를 만들 수 없다")
    void rejectNullGisu() {
        // when & then
        assertThatThrownBy(() -> DemodayPoll.create(null, NAME, OPENS_AT, CLOSES_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_GISU_REQUIRED);
    }

    @Test
    @DisplayName("투표 시작 시각 없이는 투표를 만들 수 없다")
    void rejectNullOpensAt() {
        // when & then
        assertThatThrownBy(() -> DemodayPoll.create(GISU_ID, NAME, null, CLOSES_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_OPEN_AT_REQUIRED);
    }

    @Test
    @DisplayName("투표 종료 시각 없이는 투표를 만들 수 없다")
    void rejectNullClosesAt() {
        // when & then
        assertThatThrownBy(() -> DemodayPoll.create(GISU_ID, NAME, OPENS_AT, null))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_CLOSE_AT_REQUIRED);
    }

    @Test
    @DisplayName("투표 창은 시작이 종료보다 앞서야 한다 - 같거나 뒤면 생성 실패")
    void rejectInvalidPollWindow() {
        // when & then
        assertThatThrownBy(() -> DemodayPoll.create(GISU_ID, NAME, CLOSES_AT, CLOSES_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_INVALID_WINDOW);

        assertThatThrownBy(() -> DemodayPoll.create(GISU_ID, NAME, CLOSES_AT, OPENS_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_INVALID_WINDOW);
    }

    @Test
    @DisplayName("이름이 비어 있으면 생성 실패 - null과 공백만 있는 값 모두")
    void rejectBlankPollName() {
        // when & then
        assertThatThrownBy(() -> DemodayPoll.create(GISU_ID, null, OPENS_AT, CLOSES_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_INVALID_NAME);

        assertThatThrownBy(() -> DemodayPoll.create(GISU_ID, "   ", OPENS_AT, CLOSES_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_INVALID_NAME);
    }

    @Test
    @DisplayName("이름은 100자까지 허용하고 101자부터 거부한다")
    void validatePollNameLengthBoundary() {
        // given
        String maxLength = "가".repeat(100);
        String tooLong = "가".repeat(101);

        // when & then
        assertThatCode(() -> DemodayPoll.create(GISU_ID, maxLength, OPENS_AT, CLOSES_AT))
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> DemodayPoll.create(GISU_ID, tooLong, OPENS_AT, CLOSES_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_INVALID_NAME);
    }

    @Test
    @DisplayName("이름 길이는 UTF-16 단위가 아니라 code point로 센다 - 이모지 100개는 허용된다")
    void countEmojiPollNameByCodePoint() {
        // given
        String hundredEmojis = "😀".repeat(100);
        String hundredOneEmojis = "😀".repeat(101);

        // when & then
        assertThat(hundredEmojis.length()).isEqualTo(200);
        assertThatCode(() -> DemodayPoll.create(GISU_ID, hundredEmojis, OPENS_AT, CLOSES_AT))
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> DemodayPoll.create(GISU_ID, hundredOneEmojis, OPENS_AT, CLOSES_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_INVALID_NAME);
    }
}
