package com.umc.product.community.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfoWithStatus;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.application.port.in.query.dto.PostSearchQuery;
import com.umc.product.community.application.port.out.comment.LoadCommentPort;
import com.umc.product.community.application.port.out.dto.PostWithAuthor;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.application.port.out.scrap.LoadScrapPort;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock
    LoadPostPort loadPostPort;

    @Mock
    LoadCommentPort loadCommentPort;

    @Mock
    LoadScrapPort loadScrapPort;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Test
    @DisplayName("게시글 상세는 작성자 challenger와 member 정보를 결합한다")
    void 게시글_상세는_작성자_challenger와_member_정보를_결합한다() {
        given(loadPostPort.findByIdWithAuthor(10L)).willReturn(Optional.of(new PostWithAuthor(post(10L), 1L)));
        given(getChallengerUseCase.getById(1L)).willReturn(challenger(1L, 100L));
        given(getMemberUseCase.getById(100L)).willReturn(member(100L));

        var result = sut().getPostDetail(10L);

        assertThat(result.authorName()).isEqualTo("회원100");
        assertThat(result.authorProfileImage()).isEqualTo("profile100");
    }

    @Test
    @DisplayName("없는 게시글 상세 조회는 not-found로 실패한다")
    void 없는_게시글_상세_조회는_not_found로_실패한다() {
        given(loadPostPort.findByIdWithAuthor(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut().getPostDetail(10L))
            .isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("viewer 게시글 상세는 좋아요·댓글·스크랩 interaction을 결합한다")
    void viewer_게시글_상세는_좋아요_댓글_스크랩_interaction을_결합한다() {
        given(loadPostPort.findByIdWithAuthor(10L, 2L))
            .willReturn(Optional.of(new PostWithAuthor(post(10L), 1L, true)));
        given(getChallengerUseCase.getById(1L)).willReturn(challenger(1L, 100L));
        given(getMemberUseCase.getById(100L)).willReturn(member(100L));
        given(loadCommentPort.countByPostId(10L)).willReturn(3);
        given(loadScrapPort.existsByPostIdAndChallengerId(10L, 2L)).willReturn(true);
        given(loadScrapPort.countByPostId(10L)).willReturn(4);

        var result = sut().getPostDetail(10L, 2L);

        assertThat(result.isLiked()).isTrue();
        assertThat(result.commentCount()).isEqualTo(3);
        assertThat(result.isScrapped()).isTrue();
        assertThat(result.scrapCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("없는 viewer 게시글 상세 조회는 not-found로 실패한다")
    void 없는_viewer_게시글_상세_조회는_not_found로_실패한다() {
        given(loadPostPort.findByIdWithAuthor(10L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut().getPostDetail(10L, 2L))
            .isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("게시글 목록은 빈 page를 동일 pageable의 빈 결과로 반환한다")
    void 게시글_목록은_빈_page를_동일_pageable의_빈_결과로_반환한다() {
        PageRequest pageable = PageRequest.of(1, 10);
        PostSearchQuery query = new PostSearchQuery(null);
        given(loadPostPort.findAllByQuery(query, pageable)).willReturn(Page.empty(pageable));

        var result = sut().getPostList(query, 100L, pageable);

        assertThat(result).isEmpty();
        assertThat(result.getPageable()).isEqualTo(pageable);
    }

    @Test
    @DisplayName("게시글 목록은 batch 작성자 데이터 누락과 댓글 기본값을 안전하게 처리한다")
    void 게시글_목록은_batch_작성자_데이터_누락과_댓글_기본값을_안전하게_처리한다() {
        PageRequest pageable = PageRequest.of(0, 10);
        Post first = post(10L);
        Post missingMember = post(20L);
        Post missingAuthor = post(30L);
        PostSearchQuery query = new PostSearchQuery(null);
        given(loadPostPort.findAllByQuery(query, pageable))
            .willReturn(new PageImpl<>(List.of(first, missingMember, missingAuthor), pageable, 3));
        given(loadPostPort.findAuthorIdsByPostIds(List.of(10L, 20L, 30L)))
            .willReturn(Map.of(10L, 1L, 20L, 2L));
        given(getChallengerUseCase.getAllByIdsAsMap(Set.of(1L, 2L))).willReturn(Map.of(
            1L, challenger(1L, 100L),
            2L, challenger(2L, 200L)
        ));
        given(getMemberUseCase.findAllByIds(Set.of(100L, 200L))).willReturn(Map.of(100L, member(100L)));
        given(loadCommentPort.countByPostIds(List.of(10L, 20L, 30L))).willReturn(Map.of(10L, 2));

        var result = sut().getPostList(query, 999L, pageable).getContent();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).authorName()).isEqualTo("회원100");
        assertThat(result.get(0).commentCount()).isEqualTo(2);
        assertThat(result.get(1).authorName()).isEqualTo("알 수 없음");
        assertThat(result.get(1).authorProfileImage()).isNull();
        assertThat(result.get(2).authorName()).isEqualTo("알 수 없음");
        assertThat(result.get(2).authorPart()).isNull();
    }

    @Test
    @DisplayName("검색 결과는 각 게시글 작성자 정보를 결합한다")
    void 검색_결과는_각_게시글_작성자_정보를_결합한다() {
        PageRequest pageable = PageRequest.of(0, 10);
        Post post = post(10L);
        given(loadPostPort.searchByKeyword("검색", pageable)).willReturn(new PageImpl<>(List.of(post), pageable, 1));
        given(getChallengerUseCase.getById(1L)).willReturn(challenger(1L, 100L));
        given(getMemberUseCase.getById(100L)).willReturn(member(100L));

        var result = sut().search("검색", pageable);

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.authorMemberId()).isEqualTo(100L);
            assertThat(response.authorName()).isEqualTo("회원100");
        });
    }

    @Test
    @DisplayName("내 작성·댓글·스크랩 목록은 최신 활성 challenger ID로 조회한다")
    void 내_작성_댓글_스크랩_목록은_최신_활성_challenger_ID로_조회한다() {
        PageRequest pageable = PageRequest.of(0, 10);
        given(getChallengerUseCase.getLatestActiveChallengerByMemberId(100L))
            .willReturn(ChallengerInfoWithStatus.builder()
                .challengerId(1L)
                .memberId(100L)
                .build());
        given(loadPostPort.findByAuthorChallengerId(1L, pageable)).willReturn(Page.empty(pageable));
        given(loadPostPort.findCommentedPostsByChallengerId(1L, pageable)).willReturn(Page.empty(pageable));
        given(loadPostPort.findScrappedPostsByChallengerId(1L, pageable)).willReturn(Page.empty(pageable));

        assertThat(sut().getMyPosts(100L, pageable)).isEmpty();
        assertThat(sut().getCommentedPosts(100L, pageable)).isEmpty();
        assertThat(sut().getScrappedPosts(100L, pageable)).isEmpty();
    }

    private PostQueryService sut() {
        return new PostQueryService(
            loadPostPort,
            loadCommentPort,
            loadScrapPort,
            getChallengerUseCase,
            getMemberUseCase
        );
    }

    private Post post(Long id) {
        Post post = Post.createPost("제목" + id, "내용", Category.FREE, 1L);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private ChallengerInfo challenger(Long challengerId, Long memberId) {
        return ChallengerInfo.builder()
            .challengerId(challengerId)
            .memberId(memberId)
            .part(ChallengerPart.WEB)
            .build();
    }

    private MemberInfo member(Long memberId) {
        return MemberInfo.builder()
            .id(memberId)
            .name("회원" + memberId)
            .profileImageLink("profile" + memberId)
            .build();
    }
}
