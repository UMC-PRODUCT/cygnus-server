package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.community.application.port.in.query.dto.PostSearchQuery;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.community.domain.enums.ReportTargetType;
import com.umc.product.community.domain.exception.CommunityDomainException;

import jakarta.persistence.EntityManager;

class CommunityPersistenceAdapterUnitTest {

    @Test
    @DisplayName("Post adapter는 조회·검색·목록 port를 각 repository에 위임한다")
    void Post_adapter는_조회_검색_목록_port를_각_repository에_위임한다() {
        PostRepository repository = mock(PostRepository.class);
        PostQueryRepository queryRepository = mock(PostQueryRepository.class);
        PostPersistenceAdapter sut = new PostPersistenceAdapter(repository, queryRepository);
        PageRequest pageable = PageRequest.of(0, 10);
        PostSearchQuery query = new PostSearchQuery(Category.FREE);
        Page<Post> page = Page.empty(pageable);
        given(queryRepository.findAllByQuery(query, pageable)).willReturn(page);
        given(queryRepository.findByAuthorChallengerId(1L, pageable)).willReturn(page);
        given(queryRepository.findCommentedPostsByChallengerId(1L, pageable)).willReturn(page);
        given(queryRepository.findScrappedPostsByChallengerId(1L, pageable)).willReturn(page);
        given(queryRepository.searchByKeyword("검색", pageable)).willReturn(page);
        given(repository.findByCategory(Category.FREE)).willReturn(List.of());
        given(repository.findAuthorIdsMapByPostIds(List.of())).willReturn(Map.of());

        assertThat(sut.findAllByQuery(query, pageable)).isSameAs(page);
        assertThat(sut.findByAuthorChallengerId(1L, pageable)).isSameAs(page);
        assertThat(sut.findCommentedPostsByChallengerId(1L, pageable)).isSameAs(page);
        assertThat(sut.findScrappedPostsByChallengerId(1L, pageable)).isSameAs(page);
        assertThat(sut.searchByKeyword("검색", pageable)).isSameAs(page);
        assertThat(sut.findByCategory(Category.FREE)).isEmpty();
        assertThat(sut.findAuthorIdsByPostIds(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Post adapter는 ID 조회·저장·삭제와 작성자 조회 계약을 보존한다")
    void Post_adapter는_ID_조회_저장_삭제와_작성자_조회_계약을_보존한다() {
        PostRepository repository = mock(PostRepository.class);
        PostPersistenceAdapter sut = new PostPersistenceAdapter(repository, mock(PostQueryRepository.class));
        Post post = post(10L);
        given(repository.save(post)).willReturn(post);
        given(repository.findById(10L)).willReturn(Optional.of(post));

        assertThat(sut.save(post)).isSameAs(post);
        assertThat(sut.findById(10L)).contains(post);
        assertThat(sut.findByIdWithAuthor(10L)).get().satisfies(result ->
            assertThat(result.authorChallengerId()).isEqualTo(1L));
        assertThat(sut.findByIdWithAuthor(10L, null)).get().satisfies(result ->
            assertThat(result.liked()).isFalse());
        post.toggleLike(2L);
        assertThat(sut.findByIdWithAuthor(10L, 2L)).get().satisfies(result ->
            assertThat(result.liked()).isTrue());
        assertThat(sut.findAuthorIdByPostId(10L)).isEqualTo(1L);

        sut.delete(post);
        sut.deleteById(11L);
        verify(repository).deleteById(10L);
        verify(repository).deleteById(11L);
    }

    @Test
    @DisplayName("Post adapter는 transient 삭제를 무시하고 없는 좋아요·작성자 조회를 거부한다")
    void Post_adapter는_transient_삭제를_무시하고_없는_좋아요_작성자_조회를_거부한다() {
        PostRepository repository = mock(PostRepository.class);
        PostPersistenceAdapter sut = new PostPersistenceAdapter(repository, mock(PostQueryRepository.class));
        Post transientPost = Post.createPost("제목", "내용", Category.FREE, 1L);
        sut.delete(transientPost);
        verify(repository, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
        given(repository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.toggleLike(10L, 1L)).isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> sut.findAuthorIdByPostId(10L)).isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("Comment adapter는 빈 batch와 count row를 map으로 변환한다")
    void Comment_adapter는_빈_batch와_count_row를_map으로_변환한다() {
        CommentRepository repository = mock(CommentRepository.class);
        CommentPersistenceAdapter sut = new CommentPersistenceAdapter(repository);
        given(repository.countByPostIdIn(List.of(10L, 20L)))
            .willReturn(List.<Object[]>of(new Object[] {10L, 3L}, new Object[] {20L, 2}));

        assertThat(sut.countByPostIds(null)).isEmpty();
        assertThat(sut.countByPostIds(List.of())).isEmpty();
        assertThat(sut.countByPostIds(List.of(10L, 20L))).containsEntry(10L, 3).containsEntry(20L, 2);
    }

    @Test
    @DisplayName("Comment adapter는 transient 삭제를 무시하고 없는 댓글 좋아요를 거부한다")
    void Comment_adapter는_transient_삭제를_무시하고_없는_댓글_좋아요를_거부한다() {
        CommentRepository repository = mock(CommentRepository.class);
        CommentPersistenceAdapter sut = new CommentPersistenceAdapter(repository);
        Comment transientComment = Comment.create(post(10L), 1L, "댓글", null);
        sut.delete(transientComment);
        verify(repository, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
        ReflectionTestUtils.setField(transientComment, "id", 20L);
        sut.delete(transientComment);
        verify(repository).deleteById(20L);
        given(repository.findById(20L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.toggleLike(20L, 1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Scrap adapter는 복합 key 삭제와 Post 전체 삭제를 위임한다")
    void Scrap_adapter는_복합_key_삭제와_Post_전체_삭제를_위임한다() {
        ScrapRepository repository = mock(ScrapRepository.class);
        ScrapPersistenceAdapter sut = new ScrapPersistenceAdapter(repository, mock(EntityManager.class));

        sut.deleteByPostIdAndChallengerId(10L, 1L);
        sut.deleteByPostId(10L);

        verify(repository).deleteByPost_IdAndChallengerId(10L, 1L);
        verify(repository).deleteAllByPost_Id(10L);
    }

    @Test
    @DisplayName("Report adapter는 중복 확인과 저장을 repository에 위임한다")
    void Report_adapter는_중복_확인과_저장을_repository에_위임한다() {
        ReportRepository repository = mock(ReportRepository.class);
        ReportPersistenceAdapter sut = new ReportPersistenceAdapter(repository);
        Report report = Report.create(1L, ReportTargetType.POST, 10L, null);
        given(repository.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.POST, 10L))
            .willReturn(true);
        given(repository.save(report)).willReturn(report);

        assertThat(sut.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.POST, 10L)).isTrue();
        assertThat(sut.save(report)).isSameAs(report);
    }

    @Test
    @DisplayName("PostRepository default map 변환은 projection을 ID map으로 바꾼다")
    void PostRepository_default_map_변환은_projection을_ID_map으로_바꾼다() {
        PostRepository repository = mock(PostRepository.class, CALLS_REAL_METHODS);
        PostRepository.PostAuthorProjection projection = mock(PostRepository.PostAuthorProjection.class);
        given(projection.getPostId()).willReturn(10L);
        given(projection.getAuthorId()).willReturn(1L);
        given(repository.findAuthorIdsByPostIds(List.of(10L))).willReturn(List.of(projection));

        assertThat(repository.findAuthorIdsMapByPostIds(List.of(10L))).containsEntry(10L, 1L);
    }

    private Post post(Long id) {
        Post post = Post.createPost("제목", "내용", Category.FREE, 1L);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }
}
