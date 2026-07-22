package com.umc.product.community.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.application.port.out.comment.LoadCommentPort;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

    @Mock
    LoadPostPort loadPostPort;

    @Mock
    LoadCommentPort loadCommentPort;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Test
    @DisplayName("없는 게시글의 댓글 목록 조회를 거부한다")
    void 없는_게시글의_댓글_목록_조회를_거부한다() {
        given(loadPostPort.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut().getComments(10L))
            .isInstanceOf(CommunityDomainException.class);
        verify(loadCommentPort, never()).findByPostId(10L, Pageable.unpaged());
    }

    @Test
    @DisplayName("댓글이 없으면 작성자 조회 없이 빈 목록을 반환한다")
    void 댓글이_없으면_작성자_조회_없이_빈_목록을_반환한다() {
        given(loadPostPort.findById(10L)).willReturn(Optional.of(post()));
        given(loadCommentPort.findByPostId(10L, Pageable.unpaged()))
            .willReturn(new PageImpl<>(List.of()));

        assertThat(sut().getComments(10L)).isEmpty();
        verify(getChallengerUseCase, never()).findByIdOrNull(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("댓글 목록은 부분 작성자 데이터를 허용하고 현재 challenger의 isAuthor를 표시한다")
    void 댓글_목록은_부분_작성자_데이터를_허용하고_현재_challenger의_isAuthor를_표시한다() {
        Comment mine = comment(20L, 1L);
        Comment orphan = comment(21L, 9L);
        given(loadPostPort.findById(10L)).willReturn(Optional.of(post()));
        given(loadCommentPort.findByPostId(10L, Pageable.unpaged()))
            .willReturn(new PageImpl<>(List.of(mine, orphan)));
        given(getChallengerUseCase.findByIdOrNull(1L)).willReturn(challenger());
        given(getMemberUseCase.findByIdOrNull(100L)).willReturn(member());
        given(getChallengerUseCase.findByIdOrNull(9L)).willReturn(null);

        var result = sut().getComments(10L, 1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isAuthor()).isTrue();
        assertThat(result.get(0).challengerNickname()).isEqualTo("닉네임");
        assertThat(result.get(1).challengerId()).isNull();
        assertThat(result.get(1).challengerName()).isNull();
    }

    @Test
    @DisplayName("단일 댓글은 challenger와 member 작성자 정보를 결합한다")
    void 단일_댓글은_challenger와_member_작성자_정보를_결합한다() {
        given(loadCommentPort.findById(20L)).willReturn(Optional.of(comment(20L, 1L)));
        given(getChallengerUseCase.getById(1L)).willReturn(challenger());
        given(getMemberUseCase.getById(100L)).willReturn(member());

        var result = sut().getComment(20L);

        assertThat(result.challengerName()).isEqualTo("작성자");
        assertThat(result.challengerProfileImage()).isEqualTo("profile");
        assertThat(result.challengerPart()).isEqualTo(ChallengerPart.WEB);
    }

    @Test
    @DisplayName("단일 댓글의 member 정보가 없으면 안전한 기본 작성자 정보를 사용한다")
    void 단일_댓글의_member_정보가_없으면_안전한_기본_작성자_정보를_사용한다() {
        given(loadCommentPort.findById(20L)).willReturn(Optional.of(comment(20L, 1L)));
        given(getChallengerUseCase.getById(1L)).willReturn(challenger());
        given(getMemberUseCase.getById(100L)).willReturn(null);

        var result = sut().getComment(20L);

        assertThat(result.challengerName()).isEqualTo("알 수 없음");
        assertThat(result.challengerProfileImage()).isNull();
    }

    @Test
    @DisplayName("없는 단일 댓글 조회는 not-found로 실패한다")
    void 없는_단일_댓글_조회는_not_found로_실패한다() {
        given(loadCommentPort.findById(20L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut().getComment(20L))
            .isInstanceOf(CommunityDomainException.class);
    }

    private CommentQueryService sut() {
        return new CommentQueryService(loadPostPort, loadCommentPort, getChallengerUseCase, getMemberUseCase);
    }

    private Post post() {
        Post post = Post.createPost("제목", "내용", Category.FREE, 1L);
        ReflectionTestUtils.setField(post, "id", 10L);
        return post;
    }

    private Comment comment(Long id, Long challengerId) {
        Comment comment = Comment.create(post(), challengerId, "댓글", null);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private ChallengerInfo challenger() {
        return ChallengerInfo.builder()
            .challengerId(1L)
            .memberId(100L)
            .part(ChallengerPart.WEB)
            .build();
    }

    private MemberInfo member() {
        return MemberInfo.builder()
            .id(100L)
            .name("작성자")
            .nickname("닉네임")
            .profileImageLink("profile")
            .build();
    }
}
