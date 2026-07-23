package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.community.application.port.in.command.report.ReportCommentUseCase;
import com.umc.product.community.application.port.in.command.report.ReportPostUseCase;
import com.umc.product.community.application.port.in.command.report.dto.ReportCommentCommand;
import com.umc.product.community.application.port.in.command.report.dto.ReportPostCommand;
import com.umc.product.global.security.MemberPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("게시글/댓글 신고 REST adapter")
class ReportControllerTest {

    private static final Long MEMBER_ID = 41L;

    @Mock
    ReportPostUseCase reportPostUseCase;

    @Mock
    ReportCommentUseCase reportCommentUseCase;

    ReportController sut;

    @BeforeEach
    void setUp() {
        sut = new ReportController(reportPostUseCase, reportCommentUseCase);
    }

    @Test
    @DisplayName("Challenger 기록 조회 없이 CurrentMember의 member ID로 게시글을 신고한다")
    void reportPost_member_ID를_신고자_ID로_전달한다() {
        // when
        sut.reportPost(100L, new MemberPrincipal(MEMBER_ID));

        // then
        ArgumentCaptor<ReportPostCommand> captor = ArgumentCaptor.forClass(ReportPostCommand.class);
        then(reportPostUseCase).should().report(captor.capture());
        assertThat(captor.getValue().postId()).isEqualTo(100L);
        assertThat(captor.getValue().reporterId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("Challenger 기록 조회 없이 CurrentMember의 member ID로 댓글을 신고한다")
    void reportComment_member_ID를_신고자_ID로_전달한다() {
        // when
        sut.reportComment(200L, new MemberPrincipal(MEMBER_ID));

        // then
        ArgumentCaptor<ReportCommentCommand> captor = ArgumentCaptor.forClass(ReportCommentCommand.class);
        then(reportCommentUseCase).should().report(captor.capture());
        assertThat(captor.getValue().commentId()).isEqualTo(200L);
        assertThat(captor.getValue().reporterId()).isEqualTo(MEMBER_ID);
    }
}
