package com.umc.product.notice.adapter.in.web;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.notice.adapter.in.web.dto.request.SubmitNoticeVoteResponseRequest;
import com.umc.product.notice.adapter.in.web.dto.request.UpdateNoticeVoteResponseRequest;
import com.umc.product.notice.application.port.in.command.ManageNoticeVoteResponseUseCase;
import com.umc.product.notice.application.port.in.command.dto.SubmitNoticeVoteResponseCommand;
import com.umc.product.notice.application.port.in.command.dto.UpdateNoticeVoteResponseCommand;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeVoteResponseController 단위 테스트")
class NoticeVoteResponseControllerUnitTest {

    @Mock
    ManageNoticeVoteResponseUseCase manageNoticeVoteResponseUseCase;

    @Test
    @DisplayName("투표 제출·수정·취소 요청에 공지와 현재 회원 ID를 결합한다")
    void delegates_submit_and_update_commands() {
        NoticeVoteResponseController sut = new NoticeVoteResponseController(manageNoticeVoteResponseUseCase);
        MemberPrincipal principal = new MemberPrincipal(10L);

        sut.submitVoteResponse(1L, new SubmitNoticeVoteResponseRequest(List.of(2L)), principal);
        sut.updateOrCancelVoteResponse(1L, new UpdateNoticeVoteResponseRequest(List.of()), principal);

        verify(manageNoticeVoteResponseUseCase).submit(SubmitNoticeVoteResponseCommand.builder()
            .noticeId(1L)
            .respondentMemberId(10L)
            .selectedOptionIds(List.of(2L))
            .build());
        verify(manageNoticeVoteResponseUseCase).updateOrCancel(UpdateNoticeVoteResponseCommand.builder()
            .noticeId(1L)
            .respondentMemberId(10L)
            .selectedOptionIds(List.of())
            .build());
    }
}
