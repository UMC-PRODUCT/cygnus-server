package com.umc.product.demoday.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.query.dto.DemodayVoteQrInfo;
import com.umc.product.demoday.application.port.out.GenerateDemodayVoteQrCredentialPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.config.DemodayQrProperties;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("데모데이 INFO QR 조회")
class DemodayVoteQrQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 10L;
    private static final Long GISU_ID = 8L;
    private static final String POLL_NAME = "8기 데모데이 투표";
    private static final Instant NOW = Instant.parse("2026-08-17T15:30:00Z");
    private static final Instant WINDOW_START = Instant.parse("2026-08-17T15:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-08-17T16:00:00Z");

    @Mock
    private LoadDemodayPollPort loadDemodayPollPort;

    @Mock
    private DemodayAdminAccessChecker adminAccessChecker;

    @Mock
    private GenerateDemodayVoteQrCredentialPort generateDemodayVoteQrCredentialPort;

    @Test
    @DisplayName("OPEN 상태이고 투표 기간 안이면 현재 구간의 QR을 조회한다")
    void getVoteQrWhenPollIsOpen() {
        // given
        DemodayVoteQrQueryService service = serviceWithClock(clockAt(NOW));
        DemodayPoll poll = openPoll();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));
        given(generateDemodayVoteQrCredentialPort.generate(POLL_ID, WINDOW_START, WINDOW_END))
            .willReturn("signed-token");

        // when
        DemodayVoteQrInfo info = service.get(POLL_ID, MEMBER_ID);

        // then
        assertThat(info.pollId()).isEqualTo(POLL_ID);
        assertThat(info.qrValue())
            .isEqualTo("https://vote.test.umc.it.kr/demoday/polls/10/vote-authorization#token=signed-token");
        assertThat(info.generatedAt()).isEqualTo(WINDOW_START);
        assertThat(info.expiresAt()).isEqualTo(WINDOW_END);
        then(adminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
    }

    @Test
    @DisplayName("존재하지 않는 Poll을 조회하면 404를 던진다")
    void getVoteQrWhenPollNotFound() {
        // given
        DemodayVoteQrQueryService service = serviceWithClock(clockAt(NOW));
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.get(POLL_ID, MEMBER_ID))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        then(generateDemodayVoteQrCredentialPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("READY 상태의 Poll은 조회할 수 없다")
    void getVoteQrWhenPollIsNotOpen() {
        // given
        DemodayVoteQrQueryService service = serviceWithClock(clockAt(NOW));
        DemodayPoll poll = createPoll();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));

        // when & then
        assertThatThrownBy(() -> service.get(POLL_ID, MEMBER_ID))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_OPEN));

        then(generateDemodayVoteQrCredentialPort).shouldHaveNoInteractions();
    }

    private DemodayVoteQrQueryService serviceWithClock(Clock clock) {
        return new DemodayVoteQrQueryService(
            loadDemodayPollPort,
            adminAccessChecker,
            generateDemodayVoteQrCredentialPort,
            new DemodayQrProperties("https://vote.test.umc.it.kr"),
            clock);
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static DemodayPoll createPoll() {
        return DemodayPoll.create(
            GISU_ID, POLL_NAME, Instant.parse("2026-08-17T09:00:00Z"), Instant.parse("2026-08-17T21:00:00Z"));
    }

    private static DemodayPoll openPoll() {
        DemodayPoll poll = createPoll();
        poll.open();
        return poll;
    }
}
