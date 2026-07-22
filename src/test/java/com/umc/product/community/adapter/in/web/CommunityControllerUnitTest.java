package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfoWithStatus;
import com.umc.product.community.adapter.in.web.dto.request.CreateLightningRequest;
import com.umc.product.community.adapter.in.web.dto.request.UpdateLightningRequest;
import com.umc.product.community.application.port.in.command.comment.CreateCommentUseCase;
import com.umc.product.community.application.port.in.command.comment.DeleteCommentUseCase;
import com.umc.product.community.application.port.in.command.comment.ToggleCommentLikeUseCase;
import com.umc.product.community.application.port.in.command.post.CreatePostUseCase;
import com.umc.product.community.application.port.in.command.post.DeletePostUseCase;
import com.umc.product.community.application.port.in.command.post.TogglePostLikeUseCase;
import com.umc.product.community.application.port.in.command.post.ToggleScrapUseCase;
import com.umc.product.community.application.port.in.command.post.UpdateLightningUseCase;
import com.umc.product.community.application.port.in.command.post.UpdatePostUseCase;
import com.umc.product.community.application.port.in.command.report.ReportCommentUseCase;
import com.umc.product.community.application.port.in.command.report.ReportPostUseCase;
import com.umc.product.community.application.port.in.command.report.dto.ReportCommentCommand;
import com.umc.product.community.application.port.in.command.report.dto.ReportPostCommand;
import com.umc.product.community.application.port.in.query.GetCommentListUseCase;
import com.umc.product.community.application.port.in.query.GetCommentedPostsUseCase;
import com.umc.product.community.application.port.in.query.GetMyPostsUseCase;
import com.umc.product.community.application.port.in.query.GetPostDetailUseCase;
import com.umc.product.community.application.port.in.query.GetPostListUseCase;
import com.umc.product.community.application.port.in.query.GetScrappedPostsUseCase;
import com.umc.product.community.application.port.in.query.SearchPostUseCase;
import com.umc.product.community.application.port.in.query.dto.PostDetailInfo;
import com.umc.product.community.application.port.in.query.dto.PostInfo;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;

class CommunityControllerUnitTest {

    private static final Instant FUTURE = Instant.parse("2099-07-01T00:00:00Z");

    @Test
    @DisplayName("Post controller는 번개 생성·수정 요청을 command로 변환한다")
    void Post_controller는_번개_생성_수정_요청을_command로_변환한다() {
        CreatePostUseCase createUseCase = mock(CreatePostUseCase.class);
        UpdateLightningUseCase updateLightningUseCase = mock(UpdateLightningUseCase.class);
        GetMemberUseCase getMemberUseCase = mock(GetMemberUseCase.class);
        GetChallengerUseCase getChallengerUseCase = mock(GetChallengerUseCase.class);
        PostController sut = new PostController(
            createUseCase,
            mock(UpdatePostUseCase.class),
            updateLightningUseCase,
            mock(DeletePostUseCase.class),
            mock(TogglePostLikeUseCase.class),
            mock(ToggleScrapUseCase.class),
            getMemberUseCase,
            getChallengerUseCase
        );
        MemberPrincipal principal = MemberPrincipal.builder().memberId(100L).build();
        ChallengerInfoWithStatus current = ChallengerInfoWithStatus.builder().challengerId(1L).build();
        given(getChallengerUseCase.getLatestActiveChallengerByMemberId(100L)).willReturn(current);
        PostInfo info = postInfo();
        given(createUseCase.createLightningPost(org.mockito.ArgumentMatchers.any())).willReturn(info);
        given(updateLightningUseCase.updateLightning(org.mockito.ArgumentMatchers.any())).willReturn(info);
        given(getChallengerUseCase.findByIdOrNull(1L)).willReturn(null);
        CreateLightningRequest createRequest = new CreateLightningRequest(
            "번개",
            "내용",
            FUTURE,
            "강남",
            5,
            "https://chat"
        );
        UpdateLightningRequest updateRequest = new UpdateLightningRequest(
            "수정",
            "내용",
            FUTURE,
            "잠실",
            6,
            "http://chat"
        );

        assertThat(sut.createLightningPost(createRequest, principal).authorMemberId()).isNull();
        assertThat(sut.updateLightningPost(10L, updateRequest).postId()).isEqualTo(10L);
        verify(createUseCase).createLightningPost(createRequest.toCommand(1L));
        verify(updateLightningUseCase).updateLightning(updateRequest.toCommand(10L));
    }

    @Test
    @DisplayName("Comment controller는 현재 challenger ID로 댓글을 삭제한다")
    void Comment_controller는_현재_challenger_ID로_댓글을_삭제한다() {
        DeleteCommentUseCase deleteUseCase = mock(DeleteCommentUseCase.class);
        GetChallengerUseCase getChallengerUseCase = mock(GetChallengerUseCase.class);
        CommentController sut = new CommentController(
            mock(CreateCommentUseCase.class),
            deleteUseCase,
            mock(GetCommentListUseCase.class),
            mock(ToggleCommentLikeUseCase.class),
            getChallengerUseCase
        );
        MemberPrincipal principal = MemberPrincipal.builder().memberId(100L).build();
        given(getChallengerUseCase.getLatestActiveChallengerByMemberId(100L))
            .willReturn(ChallengerInfoWithStatus.builder().challengerId(1L).build());

        sut.deleteComment(10L, 20L, principal);

        verify(deleteUseCase).delete(20L, 1L);
    }

    @Test
    @DisplayName("Report controller는 현재 member ID로 게시글과 댓글을 신고한다")
    void Report_controller는_현재_member_ID로_게시글과_댓글을_신고한다() {
        ReportPostUseCase reportPostUseCase = mock(ReportPostUseCase.class);
        ReportCommentUseCase reportCommentUseCase = mock(ReportCommentUseCase.class);
        ReportController sut = new ReportController(reportPostUseCase, reportCommentUseCase);
        MemberPrincipal principal = MemberPrincipal.builder().memberId(100L).build();

        sut.reportPost(10L, principal);
        sut.reportComment(20L, principal);

        verify(reportPostUseCase).report(new ReportPostCommand(10L, 100L));
        verify(reportCommentUseCase).report(new ReportCommentCommand(20L, 100L));
    }

    @Test
    @DisplayName("Post query controller는 상세·목록·검색·내 활동 page를 응답으로 변환한다")
    void Post_query_controller는_상세_목록_검색_내_활동_page를_응답으로_변환한다() {
        GetPostDetailUseCase detailUseCase = mock(GetPostDetailUseCase.class);
        GetPostListUseCase listUseCase = mock(GetPostListUseCase.class);
        SearchPostUseCase searchUseCase = mock(SearchPostUseCase.class);
        GetMyPostsUseCase myPostsUseCase = mock(GetMyPostsUseCase.class);
        GetCommentedPostsUseCase commentedUseCase = mock(GetCommentedPostsUseCase.class);
        GetScrappedPostsUseCase scrappedUseCase = mock(GetScrappedPostsUseCase.class);
        GetChallengerUseCase challengerUseCase = mock(GetChallengerUseCase.class);
        GetMemberUseCase memberUseCase = mock(GetMemberUseCase.class);
        PostQueryController sut = new PostQueryController(
            detailUseCase,
            listUseCase,
            searchUseCase,
            myPostsUseCase,
            commentedUseCase,
            scrappedUseCase,
            memberUseCase,
            challengerUseCase
        );
        MemberPrincipal principal = MemberPrincipal.builder().memberId(100L).build();
        PageRequest pageable = PageRequest.of(0, 10);
        PostInfo postInfo = postInfo();
        given(challengerUseCase.getLatestActiveChallengerByMemberId(100L))
            .willReturn(ChallengerInfoWithStatus.builder().challengerId(1L).build());
        given(detailUseCase.getPostDetail(10L, 1L)).willReturn(PostDetailInfo.builder()
            .postId(10L)
            .category(Category.FREE)
            .build());
        given(listUseCase.getPostList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq(pageable))).willReturn(Page.empty(pageable));
        given(listUseCase.getPostList(new com.umc.product.community.application.port.in.query.dto.PostSearchQuery(
            Category.FREE
        ), 100L, pageable)).willReturn(new org.springframework.data.domain.PageImpl<>(
            java.util.List.of(postInfo),
            pageable,
            1
        ));
        given(searchUseCase.search("검색", pageable)).willReturn(Page.empty(pageable));
        given(myPostsUseCase.getMyPosts(100L, pageable)).willReturn(new org.springframework.data.domain.PageImpl<>(
            java.util.List.of(postInfo),
            pageable,
            1
        ));
        given(commentedUseCase.getCommentedPosts(100L, pageable)).willReturn(Page.empty(pageable));
        given(scrappedUseCase.getScrappedPosts(100L, pageable)).willReturn(Page.empty(pageable));
        given(challengerUseCase.findByIdOrNull(1L)).willReturn(ChallengerInfo.builder()
            .challengerId(1L)
            .memberId(100L)
            .build());
        given(memberUseCase.findByIdOrNull(100L)).willReturn(null);

        assertThat(sut.getPostDetail(10L, principal).postId()).isEqualTo(10L);
        assertThat(sut.getPostList(null, pageable, null).content()).isEmpty();
        assertThat(sut.getPostList(Category.FREE, pageable, principal).content()).hasSize(1);
        assertThat(sut.search("검색", pageable).content()).isEmpty();
        assertThat(sut.getMyPosts(principal, pageable).content()).hasSize(1);
        assertThat(sut.getCommentedPosts(principal, pageable).content()).isEmpty();
        assertThat(sut.getScrappedPosts(principal, pageable).content()).isEmpty();
    }

    private PostInfo postInfo() {
        return PostInfo.builder()
            .postId(10L)
            .title("제목")
            .content("내용")
            .category(Category.LIGHTNING)
            .authorChallengerId(1L)
            .meetAt(FUTURE)
            .location("강남")
            .maxParticipants(5)
            .openChatUrl("https://chat")
            .build();
    }
}
