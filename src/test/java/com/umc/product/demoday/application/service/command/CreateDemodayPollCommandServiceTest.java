package com.umc.product.demoday.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayPollCommand;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
class CreateDemodayPollCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 8L;
    private static final Long POLL_ID = 1L;
    private static final String NAME = "8기 데모데이 현장 투표";
    private static final Instant OPENS_AT = Instant.parse("2026-08-01T05:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-08-01T08:00:00Z");

    @Mock
    private SaveDemodayPollPort saveDemodayPollPort;

    @Mock
    private DemodayAdminAccessChecker adminAccessChecker;

    @InjectMocks
    private CreateDemodayPollCommandService service;



    @Test
    @DisplayName("데모데이 투표 이벤트가 정상적으로 생성된다")
    void createPoll() {
        // given
        CreateDemodayPollCommand command = createCommand();

        DemodayPoll savedPoll = mock(DemodayPoll.class);
        given(savedPoll.getId()).willReturn(POLL_ID);
        given(saveDemodayPollPort.save(any(DemodayPoll.class)))
            .willReturn(savedPoll);

        // when
        Long pollId = service.create(command);

        // then
        assertThat(pollId).isEqualTo(POLL_ID);

        ArgumentCaptor<DemodayPoll> captor =
            ArgumentCaptor.forClass(DemodayPoll.class);

        then(adminAccessChecker)
            .should()
            .validateAdminAccess(MEMBER_ID, GISU_ID);
        then(saveDemodayPollPort)
            .should()
            .save(captor.capture());

        DemodayPoll createdPoll = captor.getValue();
        assertThat(createdPoll.getGisuId()).isEqualTo(GISU_ID);
        assertThat(createdPoll.getName()).isEqualTo(NAME);
        assertThat(createdPoll.getOpensAt()).isEqualTo(OPENS_AT);
        assertThat(createdPoll.getClosesAt()).isEqualTo(CLOSES_AT);
    }

    @Test
    @DisplayName("데모데이 관리자 권한이 없으면 투표 이벤트를 생성할 수 없다.")
    void createPollWithoutAdminAuthority() {
        //given
        CreateDemodayPollCommand command = createCommand();

        DemodayDomainException expectedException =
            new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED);

        willThrow(expectedException)
            .given(adminAccessChecker)
            .validateAdminAccess(MEMBER_ID, GISU_ID);

        //when & then
        assertThatThrownBy(() -> service.create(command))
            .isInstanceOf(DemodayDomainException.class);

        then(saveDemodayPollPort)
            .should(never())
            .save(any(DemodayPoll.class));
    }

    private static CreateDemodayPollCommand createCommand() {
        return new CreateDemodayPollCommand(MEMBER_ID, GISU_ID, NAME, OPENS_AT, CLOSES_AT);
    }
}
