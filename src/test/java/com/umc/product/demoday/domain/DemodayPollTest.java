package com.umc.product.demoday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.demoday.domain.enums.DemodayPollStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@DisplayName("DemodayPoll")
class DemodayPollTest {

    private static final Long GISU_ID = 8L;
    private static final Long POLL_ID = 1L;
    private static final Integer BOOTH_CODE = 11;
    private static final Long PROJECT_ID = 101L;
    private static final String NAME = "8기 데모데이 현장 투표";
    private static final Instant OPENS_AT = Instant.parse("2026-08-01T05:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-08-01T08:00:00Z");

    @Test
    @DisplayName("생성된 투표는 창을 그대로 보관하고 준비 상태로 시작한다")
    void initializePollAsReady() {
        // when
        DemodayPoll poll = DemodayPoll.create(GISU_ID, NAME, OPENS_AT, CLOSES_AT);

        // then
        assertThat(poll.getGisuId()).isEqualTo(GISU_ID);
        assertThat(poll.getName()).isEqualTo(NAME);
        assertThat(poll.getOpensAt()).isEqualTo(OPENS_AT);
        assertThat(poll.getClosesAt()).isEqualTo(CLOSES_AT);
        assertThat(poll.getStatus()).isEqualTo(DemodayPollStatus.READY);
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

    @Test
    @DisplayName("닫힌 투표는 등록된 프로젝트로 부스를 만든다")
    void registerProjectBoothWhileClosed() {
        // given
        DemodayPoll poll = persistedPoll();

        // when
        DemodayBooth booth = poll.registerProjectBooth(BOOTH_CODE, PROJECT_ID);

        // then
        assertThat(booth.getPollId()).isEqualTo(POLL_ID);
        assertThat(booth.getBoothCode()).isEqualTo(BOOTH_CODE);
        assertThat(booth.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(booth.getDisplayName()).isNull();
    }

    @Test
    @DisplayName("닫힌 투표는 표시 이름으로 외부 부스를 만든다")
    void registerExternalBoothWhileClosed() {
        // given
        DemodayPoll poll = persistedPoll();

        // when
        DemodayBooth booth = poll.registerExternalBooth(BOOTH_CODE, "외부 참가팀 A");

        // then
        assertThat(booth.getPollId()).isEqualTo(POLL_ID);
        assertThat(booth.getBoothCode()).isEqualTo(BOOTH_CODE);
        assertThat(booth.getProjectId()).isNull();
        assertThat(booth.getDisplayName()).isEqualTo("외부 참가팀 A");
    }

    @Test
    @DisplayName("등록한 부스는 저장 전이므로 조회 전용 부스 목록에 나타나지 않는다")
    void doNotAddRegisteredBoothToReadOnlyCollection() {
        // given
        DemodayPoll poll = persistedPoll();

        // when
        poll.registerProjectBooth(BOOTH_CODE, PROJECT_ID);

        // then
        assertThat(poll.getBooths()).isEmpty();
    }

    @Test
    @DisplayName("투표가 열리면 두 경로 모두 부스를 추가할 수 없다")
    void rejectBoothRegistrationWhenPollIsOpen() {
        // given
        DemodayPoll poll = persistedPoll();
        poll.open();

        // when & then
        assertThat(poll.isBoothRegistrable()).isFalse();

        assertThatThrownBy(() -> poll.registerProjectBooth(BOOTH_CODE, PROJECT_ID))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_BOOTH_LOCKED);

        assertThatThrownBy(() -> poll.registerExternalBooth(BOOTH_CODE, "외부 참가팀 A"))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_BOOTH_LOCKED);
    }

    @Test
    @DisplayName("열었던 투표를 다시 닫으면 부스를 추가할 수 있다")
    void allowBoothRegistrationAfterReclosingPoll() {
        // given
        DemodayPoll poll = persistedPoll();
        poll.open();
        poll.close();

        // when & then
        assertThat(poll.isBoothRegistrable()).isTrue();
        assertThatCode(() -> poll.registerProjectBooth(BOOTH_CODE, PROJECT_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("저장되지 않은 투표에는 부스를 등록할 수 없다")
    void rejectBoothRegistrationForUnsavedPoll() {
        // given
        DemodayPoll unsavedPoll = DemodayPoll.create(GISU_ID, NAME, OPENS_AT, CLOSES_AT);

        // when & then
        assertThatThrownBy(() -> unsavedPoll.registerProjectBooth(BOOTH_CODE, PROJECT_ID))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("부스 이름 규칙은 투표를 거쳐 등록해도 그대로 적용된다")
    void keepBoothNameRuleWhenRegisteringThroughPoll() {
        // given
        DemodayPoll poll = persistedPoll();

        // when & then
        assertThatThrownBy(() -> poll.registerExternalBooth(BOOTH_CODE, "  "))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_BOOTH_INVALID_NAME);
    }

    private DemodayPoll persistedPoll() {
        DemodayPoll poll = DemodayPoll.create(GISU_ID, NAME, OPENS_AT, CLOSES_AT);
        ReflectionTestUtils.setField(poll, "id", POLL_ID);
        return poll;
    }

    @Test
    @DisplayName("OPEN 상태이고 투표 기간 안이면 INFO QR을 조회할 수 있다")
    void allowVoteQrWhenOpenAndWithinWindow() {
        // given
        DemodayPoll poll = persistedPoll();
        poll.open();

        // when & then
        assertThatCode(() -> poll.validVoteQrAvailable(OPENS_AT.plusSeconds(60)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("READY 상태에서는 INFO QR을 조회할 수 없다")
    void rejectVoteQrWhenNotOpen() {
        // given
        DemodayPoll poll = persistedPoll();

        // when & then
        assertThatThrownBy(() -> poll.validVoteQrAvailable(OPENS_AT.plusSeconds(60)))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_OPEN);
    }

    @Test
    @DisplayName("OPEN 상태여도 투표 기간을 벗어나면 INFO QR을 조회할 수 없다")
    void rejectVoteQrWhenOutsideWindow() {
        // given
        DemodayPoll poll = persistedPoll();
        poll.open();

        // when & then
        assertThatThrownBy(() -> poll.validVoteQrAvailable(CLOSES_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_OPEN);
    }

    @Test
    @DisplayName("OPEN 상태이고 투표 기간 안이면 최종 투표가 가능하다")
    void allowVotingWhenOpenAndWithinWindow() {
        // given
        DemodayPoll poll = persistedPoll();
        poll.open();

        // when & then
        assertThatCode(() -> poll.validateVotingAvailable(OPENS_AT.plusSeconds(60)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시작 전에는 DEMODAY-0400, 종료 시각부터는 DEMODAY-0401로 구분한다")
    void distinguishVotingWindowErrors() {
        // given
        DemodayPoll poll = persistedPoll();
        poll.open();

        // when & then
        assertThatThrownBy(() -> poll.validateVotingAvailable(OPENS_AT.minusSeconds(1)))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_NOT_OPENED_YET);

        assertThatThrownBy(() -> poll.validateVotingAvailable(CLOSES_AT))
            .isInstanceOf(DemodayDomainException.class)
            .extracting("baseCode")
            .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_CLOSED);
    }
}
