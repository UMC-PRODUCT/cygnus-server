package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationCommand;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationInfo;
import com.umc.product.demoday.application.port.in.query.GetDemodayParticipationUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.in.query.participant.GuestDemodayParticipant;
import com.umc.product.demoday.application.port.out.HashDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.IssueDemodayParticipantTokenPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("게스트 참여 시작 커맨드 서비스")
class StartDemodayGuestParticipationCommandServiceTest {

    private static final Long POLL_ID = 1L;
    private static final Long ENTRY_CODE_ID = 42L;
    private static final Instant CLOSES_AT = Instant.parse("2100-08-15T12:00:00Z");
    private static final String ADMISSION_CODE = "GUEST-A1B2C3";
    private static final String CODE_HASH = "hashed-code";
    private static final String PARTICIPANT_TOKEN = "participant-token";

    @Mock
    private LoadDemodayPollPort loadDemodayPollPort;

    @Mock
    private LoadDemodayEntryCodePort loadDemodayEntryCodePort;

    @Mock
    private SaveDemodayEntryCodePort saveDemodayEntryCodePort;

    @Mock
    private HashDemodayEntryCodePort hashDemodayEntryCodePort;

    @Mock
    private IssueDemodayParticipantTokenPort issueDemodayParticipantTokenPort;

    @Mock
    private GetDemodayParticipationUseCase getDemodayParticipationUseCase;

    @InjectMocks
    private StartDemodayGuestParticipationCommandService startDemodayGuestParticipationCommandService;

    @Test
    @DisplayName("존재하지 않는 투표 행사면 거부한다")
    void startWhenPollNotFound() {
        // given
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.empty());
        StartDemodayGuestParticipationCommand command =
            new StartDemodayGuestParticipationCommand(POLL_ID, ADMISSION_CODE, null);

        // when & then
        assertThatThrownBy(() -> startDemodayGuestParticipationCommandService.start(command))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        then(loadDemodayEntryCodePort).shouldHaveNoInteractions();
        then(saveDemodayEntryCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 입장 코드면 거부한다")
    void startWhenEntryCodeNotFound() {
        // given
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(mock(DemodayPoll.class)));
        given(hashDemodayEntryCodePort.hash(ADMISSION_CODE)).willReturn(CODE_HASH);
        given(loadDemodayEntryCodePort.findByCodeHash(CODE_HASH)).willReturn(Optional.empty());
        StartDemodayGuestParticipationCommand command =
            new StartDemodayGuestParticipationCommand(POLL_ID, ADMISSION_CODE, null);

        // when & then
        assertThatThrownBy(() -> startDemodayGuestParticipationCommandService.start(command))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_ENTRY_CODE_NOT_FOUND));

        then(saveDemodayEntryCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 투표 행사의 입장 코드면 거부한다")
    void startWhenEntryCodePollMismatch() {
        // given
        DemodayPoll poll = mock(DemodayPoll.class);
        given(poll.getId()).willReturn(POLL_ID);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));
        given(hashDemodayEntryCodePort.hash(ADMISSION_CODE)).willReturn(CODE_HASH);

        DemodayEntryCode entryCode = mock(DemodayEntryCode.class);
        given(entryCode.getPollId()).willReturn(999L);
        given(loadDemodayEntryCodePort.findByCodeHash(CODE_HASH)).willReturn(Optional.of(entryCode));

        StartDemodayGuestParticipationCommand command =
            new StartDemodayGuestParticipationCommand(POLL_ID, ADMISSION_CODE, null);

        // when & then
        assertThatThrownBy(() -> startDemodayGuestParticipationCommandService.start(command))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_ENTRY_CODE_POLL_MISMATCH));

        then(entryCode).should(never()).redeem(any());
        then(saveDemodayEntryCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 브라우저가 이미 사용한 코드를 제출하면 거부한다")
    void startWhenAlreadyRedeemedByDifferentBrowser() {
        // given
        DemodayPoll poll = mock(DemodayPoll.class);
        given(poll.getId()).willReturn(POLL_ID);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));
        given(hashDemodayEntryCodePort.hash(ADMISSION_CODE)).willReturn(CODE_HASH);

        DemodayEntryCode entryCode = mock(DemodayEntryCode.class);
        given(entryCode.getPollId()).willReturn(POLL_ID);
        given(loadDemodayEntryCodePort.findByCodeHash(CODE_HASH)).willReturn(Optional.of(entryCode));
        willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_ENTRY_CODE_ALREADY_REDEEMED))
            .given(entryCode).redeem(any());

        // 다른 브라우저이므로 existingEntryCodeId가 없다(없거나 이 코드와 다른 값)
        StartDemodayGuestParticipationCommand command =
            new StartDemodayGuestParticipationCommand(POLL_ID, ADMISSION_CODE, null);

        // when & then
        assertThatThrownBy(() -> startDemodayGuestParticipationCommandService.start(command))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_ENTRY_CODE_ALREADY_REDEEMED));

        then(saveDemodayEntryCodePort).shouldHaveNoInteractions();
        then(issueDemodayParticipantTokenPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("정상 코드를 제출하면 사용 처리하고 토큰을 발급한다")
    void startWhenNewRedemption() {
        // given
        DemodayPoll poll = mock(DemodayPoll.class);
        given(poll.getId()).willReturn(POLL_ID);
        given(poll.getClosesAt()).willReturn(CLOSES_AT);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));
        given(hashDemodayEntryCodePort.hash(ADMISSION_CODE)).willReturn(CODE_HASH);

        DemodayEntryCode entryCode = mock(DemodayEntryCode.class);
        given(entryCode.getPollId()).willReturn(POLL_ID);
        given(entryCode.getId()).willReturn(ENTRY_CODE_ID);
        given(loadDemodayEntryCodePort.findByCodeHash(CODE_HASH)).willReturn(Optional.of(entryCode));
        given(issueDemodayParticipantTokenPort.issue(ENTRY_CODE_ID, CLOSES_AT)).willReturn(PARTICIPANT_TOKEN);

        DemodayParticipationInfo participationInfo = new DemodayParticipationInfo(
            POLL_ID, DemodayParticipantType.GUEST, 0, 6, List.of(),
            null, false, false, false, null);

        given(getDemodayParticipationUseCase.getParticipation(
            eq(POLL_ID), eq(new GuestDemodayParticipant(ENTRY_CODE_ID))))
            .willReturn(participationInfo);

        StartDemodayGuestParticipationCommand command =
            new StartDemodayGuestParticipationCommand(POLL_ID, ADMISSION_CODE, null);

        // when
        StartDemodayGuestParticipationInfo result = startDemodayGuestParticipationCommandService.start(command);

        // then
        assertThat(result.participantToken()).isEqualTo(PARTICIPANT_TOKEN);
        assertThat(result.expiresAt()).isEqualTo(CLOSES_AT);
        assertThat(result.participation()).isEqualTo(participationInfo);
        then(entryCode).should().redeem(any());
        then(saveDemodayEntryCodePort).should().save(entryCode);
    }

    @Test
    @DisplayName("이미 유효한 Cookie를 가진 같은 브라우저가 같은 코드를 다시 제출하면 재사용 처리 없이 성공한다")
    void startWhenResubmittedByExistingHolder() {
        // given
        DemodayPoll poll = mock(DemodayPoll.class);
        given(poll.getId()).willReturn(POLL_ID);
        given(poll.getClosesAt()).willReturn(CLOSES_AT);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));
        given(hashDemodayEntryCodePort.hash(ADMISSION_CODE)).willReturn(CODE_HASH);

        DemodayEntryCode entryCode = mock(DemodayEntryCode.class);
        given(entryCode.getPollId()).willReturn(POLL_ID);
        given(entryCode.getId()).willReturn(ENTRY_CODE_ID);
        given(loadDemodayEntryCodePort.findByCodeHash(CODE_HASH)).willReturn(Optional.of(entryCode));

        given(issueDemodayParticipantTokenPort.issue(ENTRY_CODE_ID, CLOSES_AT)).willReturn(PARTICIPANT_TOKEN);

        DemodayParticipationInfo participationInfo = new DemodayParticipationInfo(
            POLL_ID, DemodayParticipantType.GUEST, 2, 6, List.of(), null, false, false, false, null);
        given(getDemodayParticipationUseCase.getParticipation(
            eq(POLL_ID), eq(new GuestDemodayParticipant(ENTRY_CODE_ID))))
            .willReturn(participationInfo);

        StartDemodayGuestParticipationCommand command =
            new StartDemodayGuestParticipationCommand(POLL_ID, ADMISSION_CODE, ENTRY_CODE_ID);

        // when
        StartDemodayGuestParticipationInfo result = startDemodayGuestParticipationCommandService.start(command);

        // then
        assertThat(result.participantToken()).isEqualTo(PARTICIPANT_TOKEN);
        then(entryCode).should(never()).redeem(any());
        then(saveDemodayEntryCodePort).shouldHaveNoInteractions();
    }
}
