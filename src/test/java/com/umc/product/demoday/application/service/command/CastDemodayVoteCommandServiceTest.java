package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.demoday.application.port.in.command.dto.CastDemodayVoteCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.GuestDemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.MemberDemodayParticipant;
import com.umc.product.demoday.application.port.out.DemodayVoteAuthorizationTokenClaims;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayVotePort;
import com.umc.product.demoday.application.service.DemodayVoteAuthorizationValidator;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.enums.DemodayVoteAuthorizationPurpose;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("데모데이 최종 투표 서비스")
class CastDemodayVoteCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");
    private static final Long POLL_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final Long ENTRY_CODE_ID = 21L;
    private static final Long BOOTH_ID = 30L;
    private static final String TOKEN = "vote-authorization-token";

    @Mock private LoadDemodayPollPort loadDemodayPollPort;
    @Mock private LoadDemodayBoothPort loadDemodayBoothPort;
    @Mock private LoadDemodayEntryCodePort loadDemodayEntryCodePort;
    @Mock private LoadDemodayVotePort loadDemodayVotePort;
    @Mock private SaveDemodayVotePort saveDemodayVotePort;
    @Mock private DemodayVoteAuthorizationValidator demodayVoteAuthorizationValidator;

    @Mock private Clock clock;

    @InjectMocks
    private CastDemodayVoteCommandService service;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(NOW);
    }

    @Test
    @DisplayName("회원 권한의 참여자·Poll·부스를 검증하고 표를 저장한다")
    void castMemberVote() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        givenSuccessContext(participant);
        given(loadDemodayVotePort.findMemberVote(POLL_ID, MEMBER_ID)).willReturn(Optional.empty());
        given(saveDemodayVotePort.save(any(DemodayVote.class))).willAnswer(invocation ->
            persisted(invocation.getArgument(0)));

        // when
        DemodayVoteInfo result = service.cast(new CastDemodayVoteCommand(POLL_ID, TOKEN, participant));

        // then
        assertThat(result.voteId()).isEqualTo(100L);
        assertThat(result.pollId()).isEqualTo(POLL_ID);
        assertThat(result.selectedBooth().boothId()).isEqualTo(BOOTH_ID);
        assertThat(result.votedAt()).isEqualTo(NOW);
        then(saveDemodayVotePort).should().save(any(DemodayVote.class));
    }

    @Test
    @DisplayName("게스트는 권한의 entryCodeId로 입장 코드를 로드해 표를 저장한다")
    void castGuestVote() {
        // given
        DemodayParticipant participant = new GuestDemodayParticipant(ENTRY_CODE_ID);
        DemodayEntryCode entryCode = DemodayEntryCode.create(POLL_ID, "code-hash");
        ReflectionTestUtils.setField(entryCode, "id", ENTRY_CODE_ID);
        givenSuccessContext(participant);
        given(loadDemodayVotePort.findVisitorVote(ENTRY_CODE_ID)).willReturn(Optional.empty());
        given(loadDemodayEntryCodePort.findById(ENTRY_CODE_ID)).willReturn(Optional.of(entryCode));
        given(saveDemodayVotePort.save(any(DemodayVote.class))).willAnswer(invocation ->
            persisted(invocation.getArgument(0)));

        // when
        DemodayVoteInfo result = service.cast(new CastDemodayVoteCommand(POLL_ID, TOKEN, participant));

        // then
        assertThat(result.voteId()).isEqualTo(100L);
        then(loadDemodayEntryCodePort).should().findById(ENTRY_CODE_ID);
    }

    @Test
    @DisplayName("Poll이 종료되면 아직 권한이 남아 있어도 최종 제출을 거부한다")
    void rejectWhenPollClosedBeforeAuthorizationValidation() {
        // given
        DemodayPoll poll = openPoll();
        poll.close();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));

        // when & then
        assertThatThrownBy(() -> service.cast(command()))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_CLOSED));
        then(demodayVoteAuthorizationValidator).shouldHaveNoInteractions();
        then(saveDemodayVotePort).should(never()).save(any());
    }

    @Test
    @DisplayName("기존 표가 있으면 취소 여부와 무관하게 최종 제출을 거부한다")
    void rejectWhenVoteSlotAlreadyUsed() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        givenSuccessContext(participant);
        given(loadDemodayVotePort.findMemberVote(POLL_ID, MEMBER_ID))
            .willReturn(Optional.of(mock(DemodayVote.class)));

        // when & then
        assertThatThrownBy(() -> service.cast(new CastDemodayVoteCommand(POLL_ID, TOKEN, participant)))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST));
        then(saveDemodayVotePort).should(never()).save(any());
    }

    @Test
    @DisplayName("외부 부스에 결합된 권한은 최종 투표를 저장하지 않는다")
    void rejectExternalBoothAtFinalSubmission() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(demodayVoteAuthorizationValidator.validate(POLL_ID, participant, TOKEN))
            .willReturn(claims(participant));
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(externalBooth()));

        // when & then
        assertThatThrownBy(() -> service.cast(new CastDemodayVoteCommand(POLL_ID, TOKEN, participant)))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_VOTE_EXTERNAL_BOOTH_NOT_ALLOWED));

        then(loadDemodayVotePort).shouldHaveNoInteractions();
        then(saveDemodayVotePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("사전 조회 뒤 경합에서 저장이 늦으면 DEMODAY-0402를 그대로 반환한다")
    void propagateAlreadyCastWhenLosingDatabaseRace() {
        // given
        DemodayParticipant participant = new MemberDemodayParticipant(MEMBER_ID);
        givenSuccessContext(participant);
        given(loadDemodayVotePort.findMemberVote(POLL_ID, MEMBER_ID)).willReturn(Optional.empty());
        given(saveDemodayVotePort.save(any(DemodayVote.class)))
            .willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST));

        // when & then
        assertThatThrownBy(() -> service.cast(new CastDemodayVoteCommand(POLL_ID, TOKEN, participant)))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST));
    }

    private void givenSuccessContext(DemodayParticipant participant) {
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(openPoll()));
        given(demodayVoteAuthorizationValidator.validate(POLL_ID, participant, TOKEN))
            .willReturn(claims(participant));
        given(loadDemodayBoothPort.findById(BOOTH_ID)).willReturn(Optional.of(booth()));
    }

    private CastDemodayVoteCommand command() {
        return new CastDemodayVoteCommand(POLL_ID, TOKEN, new MemberDemodayParticipant(MEMBER_ID));
    }

    private DemodayVoteAuthorizationTokenClaims claims(DemodayParticipant participant) {
        return new DemodayVoteAuthorizationTokenClaims(
            DemodayVoteAuthorizationPurpose.CAST_VOTE.name(),
            participant.participantType(),
            participant.participantId(),
            POLL_ID,
            BOOTH_ID,
            NOW.minusSeconds(60),
            NOW.plusSeconds(240)
        );
    }

    private DemodayPoll openPoll() {
        DemodayPoll poll = DemodayPoll.create(1L, "데모데이", NOW.minusSeconds(60), NOW.plusSeconds(3600));
        ReflectionTestUtils.setField(poll, "id", POLL_ID);
        poll.open();
        return poll;
    }

    private DemodayBooth booth() {
        DemodayBooth booth = DemodayBooth.forProject(POLL_ID, 11, 100L);
        ReflectionTestUtils.setField(booth, "id", BOOTH_ID);
        return booth;
    }

    private DemodayBooth externalBooth() {
        DemodayBooth booth = DemodayBooth.forExternal(POLL_ID, 11, "외부 부스");
        ReflectionTestUtils.setField(booth, "id", BOOTH_ID);
        return booth;
    }

    private DemodayVote persisted(DemodayVote vote) {
        ReflectionTestUtils.setField(vote, "id", 100L);
        ReflectionTestUtils.setField(vote, "createdAt", NOW);
        return vote;
    }
}
