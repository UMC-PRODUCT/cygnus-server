package com.umc.product.notice.application.service.command;

import static com.umc.product.support.fixture.NoticeUnitFixture.notice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfoWithStatus;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.DeleteFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormResponseCommand;
import com.umc.product.form.application.port.in.query.GetVoteUseCase;
import com.umc.product.notice.application.port.in.command.dto.SubmitNoticeVoteResponseCommand;
import com.umc.product.notice.application.port.in.command.dto.UpdateNoticeVoteResponseCommand;
import com.umc.product.notice.application.port.in.query.GetNoticeTargetUseCase;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.application.port.out.LoadNoticeReadPort;
import com.umc.product.notice.application.port.out.LoadNoticeVotePort;
import com.umc.product.notice.application.port.out.SaveNoticeReadPort;
import com.umc.product.notice.domain.NoticeRead;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.NoticeVote;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.exception.NoticeDomainException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notice 읽음 및 투표 응답 service 테스트")
class NoticeReadAndVoteResponseServiceTest {

    @Nested
    @DisplayName("읽음 기록")
    class ReadService {

        @Mock
        LoadNoticePort loadNoticePort;

        @Mock
        LoadNoticeReadPort loadNoticeReadPort;

        @Mock
        SaveNoticeReadPort saveNoticeReadPort;

        @Mock
        GetChallengerUseCase getChallengerUseCase;

        @Mock
        GetNoticeTargetUseCase getNoticeTargetUseCase;

        @InjectMocks
        NoticeReadService service;

        @Test
        @DisplayName("특정 기수 공지는 해당 기수 challenger로 읽음을 한 번만 기록한다")
        void 특정_기수_읽음은_멱등하게_기록한다() {
            given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(notice()));
            given(getNoticeTargetUseCase.findByNoticeId(1L))
                .willReturn(new NoticeTargetInfo(2L, null, null, null, NoticeTab.CHALLENGER));
            given(getChallengerUseCase.getActiveByMemberIdAndGisuId(10L, 2L))
                .willReturn(ChallengerInfo.builder().challengerId(20L).build());
            given(loadNoticeReadPort.existsRead(1L, 20L)).willReturn(true, false);

            service.recordRead(1L, 10L);
            verify(saveNoticeReadPort, never()).saveRead(any());

            service.recordRead(1L, 10L);
            ArgumentCaptor<NoticeRead> captor = ArgumentCaptor.forClass(NoticeRead.class);
            verify(saveNoticeReadPort).saveRead(captor.capture());
            assertThat(captor.getValue().getChallengerId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("전체 기수 공지는 최신 active challenger로 기록한다")
        void 전체_기수_읽음은_최신_challenger를_사용한다() {
            given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.of(notice()));
            given(getNoticeTargetUseCase.findByNoticeId(1L))
                .willReturn(new NoticeTargetInfo(null, null, null, null, NoticeTab.CHALLENGER));
            given(getChallengerUseCase.getLatestActiveChallengerByMemberId(10L)).willReturn(challenger(30L));
            given(loadNoticeReadPort.existsRead(1L, 30L)).willReturn(false);

            service.recordRead(1L, 10L);
            verify(saveNoticeReadPort).saveRead(any());
        }

        @Test
        @DisplayName("미존재 공지는 읽음 기록 전에 not-found로 거부한다")
        void 미존재_공지의_읽음을_거부한다() {
            given(loadNoticePort.findNoticeById(1L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> service.recordRead(1L, 10L)).isInstanceOf(NoticeDomainException.class);
        }

        private ChallengerInfoWithStatus challenger(Long id) {
            return ChallengerInfoWithStatus.builder().challengerId(id).build();
        }
    }

    @Nested
    @DisplayName("투표 응답")
    class VoteResponseService {

        @Mock
        LoadNoticeVotePort loadNoticeVotePort;

        @Mock
        GetVoteUseCase getVoteUseCase;

        @Mock
        ManageFormResponseUseCase manageFormResponseUseCase;

        @InjectMocks
        NoticeVoteResponseCommandService service;

        @Test
        @DisplayName("진행 중 투표 제출은 primary question에 선택지를 담아 즉시 제출한다")
        void 진행_중_투표를_제출한다() {
            givenOpenVote();
            given(getVoteUseCase.getPrimaryQuestionId(20L)).willReturn(30L);
            given(manageFormResponseUseCase.submitImmediately(any())).willReturn(40L);

            Long result = service.submit(SubmitNoticeVoteResponseCommand.builder()
                .noticeId(1L).respondentMemberId(10L).selectedOptionIds(List.of(100L)).build());

            assertThat(result).isEqualTo(40L);
            ArgumentCaptor<SubmitFormResponseCommand> captor = ArgumentCaptor.forClass(SubmitFormResponseCommand.class);
            verify(manageFormResponseUseCase).submitImmediately(captor.capture());
            assertThat(captor.getValue().formId()).isEqualTo(20L);
            assertThat(captor.getValue().answers().getFirst().questionId()).isEqualTo(30L);
            assertThat(captor.getValue().answers().getFirst().selectedOptionIds()).containsExactly(100L);
        }

        @Test
        @DisplayName("투표 응답 수정은 선택지를 갱신하고 빈 목록은 응답을 취소한다")
        void 투표_응답을_수정하거나_취소한다() {
            givenOpenVote();
            given(getVoteUseCase.getPrimaryQuestionId(20L)).willReturn(30L);

            service.updateOrCancel(UpdateNoticeVoteResponseCommand.builder()
                .noticeId(1L).respondentMemberId(10L).selectedOptionIds(List.of(100L)).build());
            ArgumentCaptor<UpdateFormResponseCommand> updateCaptor =
                ArgumentCaptor.forClass(UpdateFormResponseCommand.class);
            verify(manageFormResponseUseCase).updateResponse(updateCaptor.capture());
            assertThat(updateCaptor.getValue().answers().getFirst().selectedOptionIds()).containsExactly(100L);

            service.updateOrCancel(UpdateNoticeVoteResponseCommand.builder()
                .noticeId(1L).respondentMemberId(10L).selectedOptionIds(List.of()).build());
            ArgumentCaptor<DeleteFormResponseCommand> deleteCaptor =
                ArgumentCaptor.forClass(DeleteFormResponseCommand.class);
            verify(manageFormResponseUseCase).deleteResponse(deleteCaptor.capture());
            assertThat(deleteCaptor.getValue().formId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("null 선택지와 미존재·시작 전·종료 투표는 거부한다")
        void 잘못된_투표_상태를_거부한다() {
            assertThatThrownBy(() -> service.updateOrCancel(UpdateNoticeVoteResponseCommand.builder()
                .noticeId(1L).respondentMemberId(10L).selectedOptionIds(null).build()))
                .isInstanceOf(NoticeDomainException.class);

            given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.empty());
            assertSubmitRejected();

            given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.of(NoticeVote.create(
                20L, notice(), Instant.now().plusSeconds(60), Instant.now().plusSeconds(120)
            )));
            assertSubmitRejected();

            given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.of(NoticeVote.create(
                20L, notice(), Instant.now().minusSeconds(120), Instant.now().minusSeconds(60)
            )));
            assertSubmitRejected();
        }

        private void givenOpenVote() {
            given(loadNoticeVotePort.findVoteByNoticeId(1L)).willReturn(Optional.of(NoticeVote.create(
                20L, notice(), Instant.now().minusSeconds(60), Instant.now().plusSeconds(60)
            )));
        }

        private void assertSubmitRejected() {
            assertThatThrownBy(() -> service.submit(SubmitNoticeVoteResponseCommand.builder()
                .noticeId(1L).respondentMemberId(10L).selectedOptionIds(List.of(100L)).build()))
                .isInstanceOf(NoticeDomainException.class);
        }
    }
}
