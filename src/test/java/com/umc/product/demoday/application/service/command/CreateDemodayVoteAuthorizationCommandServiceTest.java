package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayVoteAuthorizationCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteAuthorizationInfo;
import com.umc.product.demoday.application.port.in.query.participant.GuestDemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.demoday.application.port.out.GenerateDemodayVoteAuthorizationPort;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.service.DemodayVoteQrCredentialValidator;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("데모데이 투표 권한 발급 서비스")
class CreateDemodayVoteAuthorizationCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");
    private static final Long POLL_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final Long BOOTH_ID = 30L;
    private static final String QR_TOKEN = "info-qr-token";

    @Mock private LoadDemodayPollPort loadDemodayPollPort;
    @Mock private LoadDemodayBoothPort loadDemodayBoothPort;
    @Mock private LoadDemodayStampPort loadDemodayStampPort;
    @Mock private LoadDemodayVotePort loadDemodayVotePort;
    @Mock private DemodayVoteQrCredentialValidator demodayVoteQrCredentialValidator;
    @Mock private GenerateDemodayVoteAuthorizationPort generateDemodayVoteAuthorizationPort;

    @Mock private Clock clock;

    @InjectMocks
    private CreateDemodayVoteAuthorizationCommandService service;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(NOW);
    }

    @Test
    @DisplayName("스탬프 6개와 미사용 투표 슬롯을 확인하고 선택 부스에 결합된 5분 권한을 발급한다")
    void createAuthorization() {
        // given
        CreateDemodayVoteAuthorizationCommand command = command();
        DemodayBooth selectedBooth = booth(BOOTH_ID);
        List<DemodayBooth> booths = booths();
        List<DemodayStamp> stamps = stamps(6);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(selectedBooth));
        given(loadDemodayVotePort.findMemberVote(POLL_ID, MEMBER_ID)).willReturn(Optional.empty());
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(booths);
        given(loadDemodayStampPort.listMemberStamps(MEMBER_ID)).willReturn(stamps);
        given(generateDemodayVoteAuthorizationPort.generate(
            any(), any(), any(), any(), any())).willReturn("authorization-token");

        // when
        DemodayVoteAuthorizationInfo result = service.create(command);

        // then
        assertThat(result.voteAuthorizationToken()).isEqualTo("authorization-token");
        assertThat(result.selectedBooth().boothId()).isEqualTo(BOOTH_ID);
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        then(demodayVoteQrCredentialValidator).should().validate(POLL_ID, QR_TOKEN);
    }

    @Test
    @DisplayName("투표 전에는 같은 참여자가 권한을 여러 번 발급받을 수 있다")
    void allowReissueBeforeVoting() {
        // given
        List<DemodayStamp> stamps = stamps(6);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(booth(BOOTH_ID)));
        given(loadDemodayVotePort.findMemberVote(POLL_ID, MEMBER_ID)).willReturn(Optional.empty());
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(booths());
        given(loadDemodayStampPort.listMemberStamps(MEMBER_ID)).willReturn(stamps);
        given(generateDemodayVoteAuthorizationPort.generate(any(), any(), any(), any(), any()))
            .willReturn("first-token", "second-token");

        // when
        DemodayVoteAuthorizationInfo first = service.create(command());
        DemodayVoteAuthorizationInfo second = service.create(command());

        // then
        assertThat(first.voteAuthorizationToken()).isEqualTo("first-token");
        assertThat(second.voteAuthorizationToken()).isEqualTo("second-token");
        then(generateDemodayVoteAuthorizationPort).should(times(2))
            .generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("게스트는 entryCodeId 기준의 스탬프와 투표 슬롯으로 권한을 발급받는다")
    void createGuestAuthorization() {
        // given
        Long entryCodeId = 21L;
        List<DemodayStamp> stamps = stamps(6);
        GuestDemodayParticipant participant = new GuestDemodayParticipant(entryCodeId);
        CreateDemodayVoteAuthorizationCommand command =
            new CreateDemodayVoteAuthorizationCommand(POLL_ID, BOOTH_ID, QR_TOKEN, participant);

        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(booth(BOOTH_ID)));
        given(loadDemodayVotePort.findVisitorVote(entryCodeId)).willReturn(Optional.empty());
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(booths());
        given(loadDemodayStampPort.listVisitorStamps(entryCodeId)).willReturn(stamps);
        given(generateDemodayVoteAuthorizationPort.generate(any(), any(), any(), any(), any()))
            .willReturn("guest-authorization-token");

        // when
        DemodayVoteAuthorizationInfo result = service.create(command);

        // then
        assertThat(result.voteAuthorizationToken()).isEqualTo("guest-authorization-token");
        then(loadDemodayStampPort).should().listVisitorStamps(entryCodeId);
        then(loadDemodayVotePort).should().findVisitorVote(entryCodeId);
    }

    @Test
    @DisplayName("취소된 표라도 투표 슬롯을 사용했으면 권한을 다시 발급하지 않는다")
    void rejectWhenRevokedVoteAlreadyUsedSlot() {
        // given
        DemodayVote revokedVote = mock(DemodayVote.class);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(booth(BOOTH_ID)));
        given(loadDemodayVotePort.findMemberVote(POLL_ID, MEMBER_ID)).willReturn(Optional.of(revokedVote));

        // when & then
        assertThatThrownBy(() -> service.create(command()))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST));

        then(generateDemodayVoteAuthorizationPort).should(never())
            .generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("현재 Poll의 유효 스탬프가 6개 미만이면 권한을 발급하지 않는다")
    void rejectWhenInsufficientStamps() {
        // given
        List<DemodayStamp> stamps = stamps(5);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(booth(BOOTH_ID)));
        given(loadDemodayVotePort.findMemberVote(POLL_ID, MEMBER_ID)).willReturn(Optional.empty());
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(booths());
        given(loadDemodayStampPort.listMemberStamps(MEMBER_ID)).willReturn(stamps);

        // when & then
        assertThatThrownBy(() -> service.create(command()))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_INSUFFICIENT_STAMPS));
    }

    @Test
    @DisplayName("Poll이 종료되면 INFO QR과 관계없이 권한을 발급하지 않는다")
    void rejectWhenPollClosed() {
        // given
        DemodayPoll poll = openPoll();
        poll.close();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));

        // when & then
        assertThatThrownBy(() -> service.create(command()))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_CLOSED));
        then(demodayVoteQrCredentialValidator).shouldHaveNoInteractions();
    }

    private CreateDemodayVoteAuthorizationCommand command() {
        return new CreateDemodayVoteAuthorizationCommand(
            POLL_ID, BOOTH_ID, QR_TOKEN, new MemberDemodayParticipant(MEMBER_ID));
    }

    private DemodayPoll openPoll() {
        DemodayPoll poll = DemodayPoll.create(1L, "데모데이", NOW.minusSeconds(60), NOW.plusSeconds(3600));
        ReflectionTestUtils.setField(poll, "id", POLL_ID);
        poll.open();
        return poll;
    }

    private List<DemodayBooth> booths() {
        return LongStream.range(0, 6).mapToObj(index -> booth(BOOTH_ID + index)).toList();
    }

    private DemodayBooth booth(Long boothId) {
        int boothCode = Math.toIntExact(boothId - BOOTH_ID + 1);
        DemodayBooth booth = DemodayBooth.forExternal(POLL_ID, boothCode, "부스 " + boothId);
        ReflectionTestUtils.setField(booth, "id", boothId);
        return booth;
    }

    private List<DemodayStamp> stamps(int count) {
        return LongStream.range(0, count).mapToObj(index -> {
            DemodayStamp stamp = mock(DemodayStamp.class);
            given(stamp.getBoothId()).willReturn(BOOTH_ID + index);
            given(stamp.isRevoked()).willReturn(false);
            return stamp;
        }).toList();
    }
}
