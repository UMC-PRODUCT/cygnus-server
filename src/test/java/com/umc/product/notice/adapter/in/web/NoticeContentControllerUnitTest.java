package com.umc.product.notice.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.notice.adapter.in.web.dto.request.AddNoticeImagesRequest;
import com.umc.product.notice.adapter.in.web.dto.request.AddNoticeLinksRequest;
import com.umc.product.notice.adapter.in.web.dto.request.AddNoticeVoteRequest;
import com.umc.product.notice.adapter.in.web.dto.request.ReplaceNoticeImagesRequest;
import com.umc.product.notice.adapter.in.web.dto.request.ReplaceNoticeLinksRequest;
import com.umc.product.notice.application.port.in.command.ManageNoticeContentUseCase;
import com.umc.product.notice.application.port.in.command.dto.AddNoticeVoteResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeContentController 단위 테스트")
class NoticeContentControllerUnitTest {

    @Mock
    ManageNoticeContentUseCase manageNoticeContentUseCase;

    @Test
    @DisplayName("이미지·링크·투표의 추가·교체·삭제를 현재 회원 ID와 함께 위임한다")
    void delegates_all_content_commands() {
        NoticeContentController sut = new NoticeContentController(manageNoticeContentUseCase);
        MemberPrincipal principal = new MemberPrincipal(10L);
        given(manageNoticeContentUseCase.addImages(any(), eq(1L), eq(10L))).willReturn(List.of(11L));
        given(manageNoticeContentUseCase.addLinks(any(), eq(1L), eq(10L))).willReturn(List.of(12L));
        given(manageNoticeContentUseCase.addVote(any(), eq(1L))).willReturn(new AddNoticeVoteResult(13L, 14L));

        var images = sut.addNoticeImages(1L, new AddNoticeImagesRequest(List.of("image")), principal);
        var links = sut.addNoticeLinks(1L, new AddNoticeLinksRequest(List.of("https://example.com")), principal);
        var vote = sut.addNoticeVote(1L, new AddNoticeVoteRequest(
            "투표", false, true, Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z"), List.of("A", "B")
        ), principal);
        sut.replaceNoticeImages(1L, new ReplaceNoticeImagesRequest(List.of()), principal);
        sut.replaceNoticeLinks(1L, new ReplaceNoticeLinksRequest(List.of()), principal);
        sut.deleteNoticeVote(1L, principal);

        assertThat(images.imageIds()).containsExactly(11L);
        assertThat(links.linkIds()).containsExactly(12L);
        assertThat(vote.noticeVoteId()).isEqualTo(13L);
        assertThat(vote.voteId()).isEqualTo(14L);
        verify(manageNoticeContentUseCase).replaceImages(any(), eq(1L), eq(10L));
        verify(manageNoticeContentUseCase).replaceLinks(any(), eq(1L), eq(10L));
        verify(manageNoticeContentUseCase).deleteVote(1L, 10L);
    }
}
