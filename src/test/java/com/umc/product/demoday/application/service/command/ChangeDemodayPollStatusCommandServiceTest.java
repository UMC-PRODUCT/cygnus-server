package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayPollStatusCommand;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
class ChangeDemodayPollStatusCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 1L;
    private static final Long GISU_ID = 8L;
    private static final String POLL_NAME = "8기 데모데이 투표";
    private static final Instant OPENS_AT = Instant.parse("2026-08-15T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-08-15T12:00:00Z");

    @Mock
    private DemodayAdminAccessChecker adminAccessChecker;

    @Mock
    private LoadDemodayPollPort loadDemodayPollPort;

    @Mock
    private SaveDemodayPollPort saveDemodayPollPort;

    @InjectMocks
    private ChangeDemodayPollStatusCommandService service;

    @Test
    @DisplayName("준비 상태의 투표를 열고 저장한다")
    void openReadyPoll() {
        // given
        DemodayPoll poll = createReadyPoll();
        ChangeDemodayPollStatusCommand command = commandOf(DemodayPollStatus.OPEN);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));

        // when
        service.changeStatus(command);

        // then
        assertThat(poll.getStatus()).isEqualTo(DemodayPollStatus.OPEN);
        then(adminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
        then(saveDemodayPollPort).should().save(poll);
    }

    @Test
    @DisplayName("열린 투표를 닫고 저장한다")
    void closeOpenPoll() {
        // given
        DemodayPoll poll = createReadyPoll();
        poll.open();
        ChangeDemodayPollStatusCommand command = commandOf(DemodayPollStatus.CLOSED);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));

        // when
        service.changeStatus(command);

        // then
        assertThat(poll.getStatus()).isEqualTo(DemodayPollStatus.CLOSED);
        then(adminAccessChecker).should().validateAdminAccess(MEMBER_ID, GISU_ID);
        then(saveDemodayPollPort).should().save(poll);
    }

    @Test
    @DisplayName("투표가 존재하지 않으면 예외가 발생하고 권한 검사와 저장을 수행하지 않는다")
    void throwExceptionWhenPollDoesNotExist() {
        // given
        ChangeDemodayPollStatusCommand command = commandOf(DemodayPollStatus.OPEN);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.changeStatus(command))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));
        then(adminAccessChecker).shouldHaveNoInteractions();
        then(saveDemodayPollPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관리자 권한이 없으면 상태를 변경하거나 저장하지 않는다")
    void doNotChangeOrSavePollWhenAdminAccessIsDenied() {
        // given
        DemodayPoll poll = createReadyPoll();
        ChangeDemodayPollStatusCommand command = commandOf(DemodayPollStatus.OPEN);
        DemodayDomainException expectedException =
                new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll));
        willThrow(expectedException)
                .given(adminAccessChecker)
                .validateAdminAccess(MEMBER_ID, GISU_ID);

        // when & then
        assertThatThrownBy(() -> service.changeStatus(command)).isSameAs(expectedException);
        assertThat(poll.getStatus()).isEqualTo(DemodayPollStatus.READY);
        then(saveDemodayPollPort).shouldHaveNoInteractions();
    }

    private DemodayPoll createReadyPoll() {
        return DemodayPoll.create(GISU_ID, POLL_NAME, OPENS_AT, CLOSES_AT);
    }

    private ChangeDemodayPollStatusCommand commandOf(DemodayPollStatus status) {
        return new ChangeDemodayPollStatusCommand(MEMBER_ID, POLL_ID, status);
    }
}
