package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Scrap;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitUtil;

@PersistenceAdapterTest
@Import({
    CommentPersistenceAdapter.class,
    PostPersistenceAdapter.class,
    PostQueryRepository.class,
    ScrapPersistenceAdapter.class
})
@DisplayName("커뮤니티 JPA 관계 엣지 케이스")
class CommunityJpaRelationEdgeCaseTest {

    private static final long MISSING_POST_ID = Long.MAX_VALUE - 100;

    @Autowired
    TestEntityManager em;

    @Autowired
    PostPersistenceAdapter postAdapter;

    @Autowired
    CommentPersistenceAdapter commentAdapter;

    @Autowired
    ScrapPersistenceAdapter scrapAdapter;

    @Autowired
    PostRepository postRepository;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    ScrapRepository scrapRepository;

    @Test
    @DisplayName("동일 챌린저의 Post 좋아요는 게시글별로 격리된다")
    void sameChallengerPostLikesRemainIsolated() {
        // given
        Post first = em.persist(Post.createPost("첫 글", "본문", Category.FREE, 1L));
        Post second = em.persist(Post.createPost("둘째 글", "본문", Category.FREE, 2L));
        first.toggleLike(10L);
        second.toggleLike(10L);
        em.flush();
        Long firstId = first.getId();
        Long secondId = second.getId();
        em.clear();

        // when
        Post firstReloaded = em.find(Post.class, firstId);
        firstReloaded.toggleLike(10L);
        em.flush();
        em.clear();

        // then
        assertThat(em.find(Post.class, firstId).isLikedBy(10L)).isFalse();
        assertThat(em.find(Post.class, secondId).isLikedBy(10L)).isTrue();
    }

    @Test
    @DisplayName("detached Comment는 LAZY Post를 초기화하지 않고 postId를 반환한다")
    void detachedCommentReturnsPostIdWithoutLoadingPost() {
        // given
        Post post = em.persist(Post.createPost("댓글 글", "본문", Category.FREE, 20L));
        Comment comment = em.persist(Comment.create(post, 21L, "댓글", null));
        em.flush();
        Long postId = post.getId();
        Long commentId = comment.getId();
        em.clear();
        Comment reloaded = em.find(Comment.class, commentId);
        PersistenceUnitUtil persistenceUnitUtil = em.getEntityManager()
            .getEntityManagerFactory()
            .getPersistenceUnitUtil();
        assertThat(persistenceUnitUtil.isLoaded(reloaded, "post")).isFalse();
        em.detach(reloaded);

        // when
        Long reloadedPostId = reloaded.getPostId();

        // then
        assertThat(reloadedPostId).isEqualTo(postId);
        assertThat(persistenceUnitUtil.isLoaded(reloaded, "post")).isFalse();
    }

    @Test
    @DisplayName("detached Scrap은 LAZY Post를 초기화하지 않고 postId를 반환한다")
    void detachedScrapReturnsPostIdWithoutLoadingPost() {
        // given
        Post post = em.persist(Post.createPost("스크랩 글", "본문", Category.FREE, 30L));
        Scrap scrap = em.persist(Scrap.create(post, 31L));
        em.flush();
        Long postId = post.getId();
        Long scrapId = scrap.getId();
        em.clear();
        Scrap reloaded = em.find(Scrap.class, scrapId);
        PersistenceUnitUtil persistenceUnitUtil = em.getEntityManager()
            .getEntityManagerFactory()
            .getPersistenceUnitUtil();
        assertThat(persistenceUnitUtil.isLoaded(reloaded, "post")).isFalse();
        em.detach(reloaded);

        // when
        Long reloadedPostId = reloaded.getPostId();

        // then
        assertThat(reloadedPostId).isEqualTo(postId);
        assertThat(persistenceUnitUtil.isLoaded(reloaded, "post")).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 Post를 참조하는 Comment 저장은 FK가 거절한다")
    void commentForeignKeyRejectsMissingPost() {
        // given
        Post missingPost = em.getEntityManager().getReference(Post.class, MISSING_POST_ID);
        Comment orphan = Comment.create(missingPost, 41L, "고아 댓글", null);

        // when / then
        assertThatThrownBy(() -> em.persistAndFlush(orphan))
            .isInstanceOf(PersistenceException.class)
            .hasRootCauseInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("존재하지 않는 Post를 참조하는 Scrap 저장은 FK가 거절한다")
    void scrapForeignKeyRejectsMissingPost() {
        // given
        Post missingPost = em.getEntityManager().getReference(Post.class, MISSING_POST_ID);
        Scrap orphan = Scrap.create(missingPost, 51L);

        // when / then
        assertThatThrownBy(() -> em.persistAndFlush(orphan))
            .isInstanceOf(PersistenceException.class)
            .hasRootCauseInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("영속화되지 않은 Post는 Comment 관계의 부모가 될 수 없다")
    void transientPostCannotOwnComment() {
        // given
        Post transientPost = Post.createPost("미영속 글", "본문", Category.FREE, 60L);

        // when / then
        assertThatThrownBy(() -> Comment.create(transientPost, 61L, "댓글", null))
            .isInstanceOf(CommunityDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", CommunityErrorCode.INVALID_COMMENT_POST_ID);
    }

    @Test
    @DisplayName("영속화되지 않은 Post는 Scrap 관계의 부모가 될 수 없다")
    void transientPostCannotOwnScrap() {
        // given
        Post transientPost = Post.createPost("미영속 글", "본문", Category.FREE, 60L);

        // when / then
        assertThatThrownBy(() -> Scrap.create(transientPost, 62L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("게시글 ID는 필수이며 양수여야 합니다.");
    }

    @Test
    @DisplayName("동일 챌린저는 서로 다른 Post를 각각 스크랩할 수 있다")
    void sameChallengerCanScrapDifferentPosts() {
        // given
        Post first = em.persist(Post.createPost("첫 스크랩 글", "본문", Category.FREE, 70L));
        Post second = em.persist(Post.createPost("둘째 스크랩 글", "본문", Category.FREE, 71L));

        // when
        em.persist(Scrap.create(first, 72L));
        em.persist(Scrap.create(second, 72L));
        em.flush();
        em.clear();

        // then
        assertThat(scrapRepository.existsByPost_IdAndChallengerId(first.getId(), 72L)).isTrue();
        assertThat(scrapRepository.existsByPost_IdAndChallengerId(second.getId(), 72L)).isTrue();
    }

    @Test
    @DisplayName("한 Post 삭제는 다른 Post의 Comment Scrap 좋아요를 변경하지 않는다")
    void deletingOnePostKeepsOtherAggregateIntact() {
        // given
        Post target = em.persist(Post.createPost("삭제 대상", "본문", Category.FREE, 80L));
        target.toggleLike(81L);
        Comment targetComment = em.persist(Comment.create(target, 82L, "삭제 댓글", null));
        targetComment.toggleLike(83L);
        em.persist(Scrap.create(target, 84L));

        Post survivor = em.persist(Post.createPost("유지 대상", "본문", Category.FREE, 90L));
        survivor.toggleLike(91L);
        Comment survivorComment = em.persist(Comment.create(survivor, 92L, "유지 댓글", null));
        survivorComment.toggleLike(93L);
        em.persist(Scrap.create(survivor, 94L));
        em.flush();
        Long targetId = target.getId();
        Long survivorId = survivor.getId();
        Long survivorCommentId = survivorComment.getId();

        // when
        commentAdapter.deleteByPostId(targetId);
        scrapAdapter.deleteByPostId(targetId);
        postAdapter.delete(target);
        em.flush();
        em.clear();

        // then
        assertThat(postRepository.findById(targetId)).isEmpty();
        assertThat(commentRepository.countByPost_Id(targetId)).isZero();
        assertThat(scrapRepository.countByPost_Id(targetId)).isZero();
        assertThat(postRepository.findById(survivorId).orElseThrow().isLikedBy(91L)).isTrue();
        assertThat(commentRepository.findById(survivorCommentId).orElseThrow().isLikedBy(93L)).isTrue();
        assertThat(scrapRepository.existsByPost_IdAndChallengerId(survivorId, 94L)).isTrue();
    }
}
