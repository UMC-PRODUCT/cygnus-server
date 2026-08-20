package com.umc.product.demoday.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.DemodayVote;
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
    @DisplayName("게스트 참여자는 entryCodeId 기준으로 방문자 전용 포트를 조회한다")
    void getParticipationWithGuest() {
        // given
        Long entryCodeId = 42L;
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(mock(DemodayPoll.class)));
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(List.of());
        DemodayParticipant participant = mock(DemodayParticipant.class);
        given(participant.participantType()).willReturn(DemodayParticipantType.GUEST);
        given(participant.participantId()).willReturn(entryCodeId);
        given(loadDemodayStampPort.listVisitorStamps(entryCodeId)).willReturn(List.of());
        given(loadDemodayVotePort.findVisitorVote(entryCodeId)).willReturn(Optional.empty());

        // when
        DemodayParticipationInfo info = demodayPollQueryService.getParticipation(POLL_ID, participant);

        // then
        assertThat(info.participantType()).isEqualTo(DemodayParticipantType.GUEST);
        assertThat(info.hasActiveVote()).isFalse();
        assertThat(info.hasUsedVoteSlot()).isFalse();
        assertThat(info.canRequestVoteAuthorization()).isFalse();
        verify(loadDemodayStampPort).listVisitorStamps(entryCodeId);
        verify(loadDemodayVotePort).findVisitorVote(entryCodeId);
    }

    @Test
    @DisplayName("취소된 표는 유효표가 아니지만 투표 기회를 이미 사용한 상태다")
    void revokedVoteUsesVoteSlotWithoutBeingActive() {
        // given
        Long entryCodeId = 42L;
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(mock(DemodayPoll.class)));
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(List.of());
        DemodayParticipant participant = mock(DemodayParticipant.class);
        given(participant.participantType()).willReturn(DemodayParticipantType.GUEST);
        given(participant.participantId()).willReturn(entryCodeId);
        given(loadDemodayStampPort.listVisitorStamps(entryCodeId)).willReturn(List.of());
        DemodayVote revokedVote = mock(DemodayVote.class);
        given(revokedVote.isRevoked()).willReturn(true);
        given(loadDemodayVotePort.findVisitorVote(entryCodeId)).willReturn(Optional.of(revokedVote));

        // when
        DemodayParticipationInfo info = demodayPollQueryService.getParticipation(POLL_ID, participant);

        // then
        assertThat(info.hasActiveVote()).isFalse();
        assertThat(info.hasUsedVoteSlot()).isTrue();
        assertThat(info.canRequestVoteAuthorization()).isFalse();
    }

    @Test
    @DisplayName("현재 Poll의 유효 스탬프가 6개이고 투표 슬롯이 비어 있으면 권한 요청이 가능하다")
    void allowVoteAuthorizationWithRequiredStampsAndUnusedSlot() {
        // given
        Long memberId = 10L;
        List<DemodayBooth> booths = LongStream.range(1, 7).mapToObj(boothId -> {
            DemodayBooth booth = mock(DemodayBooth.class);
            given(booth.getId()).willReturn(boothId);
            return booth;
        }).toList();
        List<DemodayStamp> stamps = LongStream.range(1, 7).mapToObj(boothId -> {
            DemodayStamp stamp = mock(DemodayStamp.class);
            given(stamp.getBoothId()).willReturn(boothId);
            given(stamp.isRevoked()).willReturn(false);
            return stamp;
        }).toList();
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(mock(DemodayPoll.class)));
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(booths);
        DemodayParticipant participant = mock(DemodayParticipant.class);
        given(participant.participantType()).willReturn(DemodayParticipantType.MEMBER);
        given(participant.participantId()).willReturn(memberId);
        given(loadDemodayStampPort.listMemberStamps(memberId)).willReturn(stamps);
        given(loadDemodayVotePort.findMemberVote(POLL_ID, memberId)).willReturn(Optional.empty());

        // when
        DemodayParticipationInfo info = demodayPollQueryService.getParticipation(POLL_ID, participant);

        // then
        assertThat(info.stampCount()).isEqualTo(6);
        assertThat(info.hasActiveVote()).isFalse();
        assertThat(info.hasUsedVoteSlot()).isFalse();
        assertThat(info.canRequestVoteAuthorization()).isTrue();
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

    @Test
    @DisplayName("투표 부스 목록에서는 외부 부스를 제외하고 프로젝트 부스만 반환한다")
    void listOnlyProjectBooths() {
        // given
        DemodayBooth projectBooth = DemodayBooth.forProject(POLL_ID, 11, 101L);
        ReflectionTestUtils.setField(projectBooth, "id", 10L);
        DemodayBooth externalBooth = DemodayBooth.forExternal(POLL_ID, 12, "외부 부스");
        ReflectionTestUtils.setField(externalBooth, "id", 11L);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(mock(DemodayPoll.class)));
        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(List.of(projectBooth, externalBooth));

        // when
        var booths = demodayPollQueryService.listBooths(POLL_ID);

        // then
        assertThat(booths)
            .extracting(booth -> booth.boothId())
            .containsExactly(10L);
    }
}
