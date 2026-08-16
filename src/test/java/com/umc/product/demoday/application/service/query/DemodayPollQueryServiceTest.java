package com.umc.product.demoday.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("데모데이 Poll 조회 서비스")
class DemodayPollQueryServiceTest {

    private static final Long POLL_ID = 1L;

    @Mock
    private LoadDemodayPollPort loadDemodayPollPort;

    @Mock
    private LoadDemodayBoothPort loadDemodayBoothPort;

    @Mock
    private LoadDemodayStampPort loadDemodayStampPort;

    @Mock
    private LoadDemodayVotePort loadDemodayVotePort;

    @InjectMocks
    private DemodayPollQueryService demodayPollQueryService;

    @Test
    @DisplayName("회원이 아닌 참여자는 참여 정보를 조회할 수 없다")
    void getParticipationWithGuest() {
        // given
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(mock(DemodayPoll.class)));
        DemodayParticipant participant = mock(DemodayParticipant.class);
        given(participant.participantType()).willReturn(DemodayParticipantType.GUEST);

        // when & then
        assertThatThrownBy(() -> demodayPollQueryService.getParticipation(POLL_ID, participant))
                .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                        assertThat(exception.getBaseCode())
                                .isEqualTo(DemodayErrorCode.DEMODAY_PARTICIPATION_UNSUPPORTED));

        verifyNoInteractions(loadDemodayBoothPort, loadDemodayStampPort, loadDemodayVotePort);
    }

    @Test
    @DisplayName("존재하지 않는 Poll의 부스는 조회할 수 없다")
    void listBoothsWhenPollAbsent() {
        // given
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> demodayPollQueryService.listBooths(POLL_ID))
                .isInstanceOfSatisfying(DemodayDomainException.class, exception ->
                        assertThat(exception.getBaseCode())
                                .isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        verifyNoInteractions(loadDemodayBoothPort);
    }
}
