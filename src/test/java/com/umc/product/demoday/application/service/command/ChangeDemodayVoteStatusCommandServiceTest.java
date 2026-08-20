package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayVoteStatusCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteStatusInfo;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayVotePort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.enums.DemodayVoteStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeDemodayVoteStatusCommandService")
class ChangeDemodayVoteStatusCommandServiceTest {

    private static final Long POLL_ID = 10L;
    private static final Long GISU_ID = 8L;
    private static final Long MEMBER_ID = 1L;
    private static final Long VOTE_ID = 100L;
    private static final Long BOOTH_ID = 20L;
    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");

    @Mock private DemodayAdminAccessChecker adminAccessChecker;
    @Mock private LoadDemodayPollPort loadDemodayPollPort;
    @Mock private LoadDemodayVotePort loadDemodayVotePort;
    @Mock private SaveDemodayVotePort saveDemodayVotePort;

    private ChangeDemodayVoteStatusCommandService service;

    @BeforeEach
    void setUp() {
        service = new ChangeDemodayVoteStatusCommandService(
            adminAccessChecker,
            loadDemodayPollPort,
            loadDemodayVotePort,
            saveDemodayVotePort,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("관리자가 표를 무효화하면 현재 시각을 기록하고 같은 표를 저장한다")
    void revokeVote() {
        // given
        ChangeDemodayVoteStatusCommand command = command("부정 투표 확인");
        DemodayVote vote = vote();
        givenPoll();
        given(loadDemodayVotePort.findByIdInPollForUpdate(POLL_ID, VOTE_ID))
            .willReturn(Optional.of(vote));
        given(saveDemodayVotePort.save(vote)).willReturn(vote);

        // when
        DemodayVoteStatusInfo result = service.revoke(command);

        // then
        assertThat(result.voteId()).isEqualTo(VOTE_ID);
        assertThat(result.status()).isEqualTo(DemodayVoteStatus.REVOKED);
        assertThat(result.revokedAt()).isEqualTo(NOW);
        assertThat(vote.getTargetBoothId()).isEqualTo(BOOTH_ID);
        then(adminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
        then(saveDemodayVotePort).should().save(vote);
    }

    @Test
    @DisplayName("무효화한 표를 복원하면 기존 행의 revokedAt만 제거한다")
    void restoreVote() {
        // given
        DemodayVote vote = vote();
        vote.revoke(NOW.minusSeconds(60));
        givenPoll();
        given(loadDemodayVotePort.findByIdInPollForUpdate(POLL_ID, VOTE_ID))
            .willReturn(Optional.of(vote));
        given(saveDemodayVotePort.save(vote)).willReturn(vote);

        // when
        DemodayVoteStatusInfo result = service.restore(command("오탐 확인"));

        // then
        assertThat(result.status()).isEqualTo(DemodayVoteStatus.VALID);
        assertThat(result.revokedAt()).isNull();
        assertThat(vote.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(vote.getTargetBoothId()).isEqualTo(BOOTH_ID);
        then(saveDemodayVotePort).should().save(vote);
    }

    @Test
    @DisplayName("다른 Poll의 표 ID는 존재하지 않는 표로 거절한다")
    void rejectVoteOutsidePoll() {
        // given
        givenPoll();
        given(loadDemodayVotePort.findByIdInPollForUpdate(POLL_ID, VOTE_ID))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.revoke(command("Poll 불일치")))
            .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(DemodayErrorCode.DEMODAY_VOTE_NOT_FOUND));
        then(saveDemodayVotePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("무효화와 무효 해제는 서로 다른 감사 동작으로 기록한다")
    void auditActionsAreDistinct() throws NoSuchMethodException {
        Audited revokeAudit = ChangeDemodayVoteStatusCommandService.class
            .getMethod("revoke", ChangeDemodayVoteStatusCommand.class)
            .getAnnotation(Audited.class);
        Audited restoreAudit = ChangeDemodayVoteStatusCommandService.class
            .getMethod("restore", ChangeDemodayVoteStatusCommand.class)
            .getAnnotation(Audited.class);

        assertThat(revokeAudit.action()).isEqualTo(AuditAction.REVOKE);
        assertThat(restoreAudit.action()).isEqualTo(AuditAction.RESTORE);
        assertThat(revokeAudit.description()).contains("#command.pollId()", "#command.reason()");
        assertThat(restoreAudit.description()).contains("#command.pollId()", "#command.reason()");
    }

    private void givenPoll() {
        DemodayPoll poll = org.mockito.Mockito.mock(DemodayPoll.class);
        given(poll.getGisuId()).willReturn(GISU_ID);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));
    }

    private DemodayVote vote() {
        DemodayBooth booth = DemodayBooth.forProject(POLL_ID, 501L);
        ReflectionTestUtils.setField(booth, "id", BOOTH_ID);
        DemodayVote vote = DemodayVote.forMember(POLL_ID, MEMBER_ID, booth);
        ReflectionTestUtils.setField(vote, "id", VOTE_ID);
        return vote;
    }

    private ChangeDemodayVoteStatusCommand command(String reason) {
        return new ChangeDemodayVoteStatusCommand(POLL_ID, VOTE_ID, MEMBER_ID, reason);
    }
}
