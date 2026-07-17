package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.community.application.port.in.query.dto.PostInfo;
import com.umc.product.community.application.port.in.query.dto.PostSearchQuery;
import com.umc.product.community.application.port.out.dto.PostWithAuthor;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Scrap;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManagerFactory;

@PersistenceAdapterTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({PostPersistenceAdapter.class, PostQueryRepository.class, ScrapPersistenceAdapter.class})
@DisplayName("커뮤니티 JPA 경계")
class CommunityJpaBoundaryTest {

    private static final Instant MEET_AT = Instant.parse("2026-07-18T12:34:56Z");

    @Autowired
    TestEntityManager em;

    @Autowired
    PostPersistenceAdapter postAdapter;

    @Autowired
    ScrapPersistenceAdapter scrapAdapter;

    @Autowired
    PostQueryRepository postQueryRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    ScrapRepository scrapRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUpStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @DisplayName("UTC 번개 시각 기본값 대댓글 parentId가 DB 왕복을 보존한다")
    void utc_번개_시각_기본값_대댓글_parent_id가_db_왕복을_보존한다() {
        // given
        Post post = em.persist(Post.createLightning(
            "번개",
            "본문",
            new Post.LightningInfo(MEET_AT, "역삼역", 5, "https://example.com/chat"),
            401L
        ));
        Comment parent = em.persist(Comment.create(post.getId(), 402L, "부모", null));
        Comment child = em.persist(Comment.create(post.getId(), 403L, "자식", parent.getId()));

        // when
        em.flush();
        Long postId = post.getId();
        Long childId = child.getId();
        em.clear();
        Post reloadedPost = em.find(Post.class, postId);
        Comment reloadedChild = em.find(Comment.class, childId);

        // then
        assertThat(reloadedPost.getRegion()).isEmpty();
        assertThat(reloadedPost.isAnonymous()).isFalse();
        assertThat(reloadedPost.getMeetAt()).isEqualTo(LocalDateTime.ofInstant(MEET_AT, ZoneOffset.UTC));
        assertThat(reloadedPost.getLightningInfoOrThrow().meetAt()).isEqualTo(MEET_AT);
        assertThat(reloadedChild.getParentId()).isEqualTo(parent.getId());
    }

    @Test
    @DisplayName("조회자 좋아요는 DTO 상태이며 Post 엔티티에 누출되지 않는다")
    void 조회자_좋아요는_dto_상태이며_post_엔티티에_누출되지_않는다() {
        // given
        Post post = em.persist(Post.createPost("조회자", "본문", Category.FREE, 411L));
        post.toggleLike(412L);
        em.flush();
        em.clear();

        // when
        PostWithAuthor liked = postAdapter.findByIdWithAuthor(post.getId(), 412L).orElseThrow();
        PostWithAuthor notLiked = postAdapter.findByIdWithAuthor(post.getId(), 413L).orElseThrow();
        PostInfo likedInfo = PostInfo.from(liked.post(), null, null, liked.liked());
        PostInfo notLikedInfo = PostInfo.from(notLiked.post(), null, null, notLiked.liked());

        // then
        assertThat(likedInfo.isLiked()).isTrue();
        assertThat(notLikedInfo.isLiked()).isFalse();
        assertThat(likedInfo.postId()).isEqualTo(notLikedInfo.postId());
        assertThat(Arrays.stream(Post.class.getDeclaredFields()).map(Field::getName))
            .doesNotContain("liked", "isLiked", "likeCount");
    }

    @Test
    @DisplayName("동일 Post-Challenger 스크랩 중복은 DB unique 제약으로 거절된다")
    void 동일_post_challenger_스크랩_중복은_db_unique_제약으로_거절된다() {
        // given
        Post post = em.persist(Post.createPost("중복", "본문", Category.FREE, 421L));
        scrapRepository.saveAndFlush(Scrap.create(post.getId(), 422L));
        em.clear();

        // when & then
        assertThatThrownBy(() -> scrapRepository.saveAndFlush(Scrap.create(post.getId(), 422L)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동시 스크랩 토글은 예외 없이 직렬화되어 최종 상태가 일관된다")
    void 동시_스크랩_토글은_예외_없이_직렬화되어_최종_상태가_일관된다() throws Exception {
        // given
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Long postId = transactionTemplate.execute(status -> postRepository.saveAndFlush(
            Post.createPost("동시 토글", "본문", Category.FREE, 431L)
        ).getId());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> toggleAfterSignal(postId, 432L, ready, start));
            Future<Boolean> second = executor.submit(() -> toggleAfterSignal(postId, 432L, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // when
            start.countDown();
            List<Boolean> results = List.of(
                first.get(5, TimeUnit.SECONDS),
                second.get(5, TimeUnit.SECONDS)
            );

            // then
            assertThat(results).containsExactlyInAnyOrder(true, false);
            assertThat(scrapRepository.countByPostId(postId)).isZero();
        } finally {
            start.countDown();
            executor.shutdownNow();
            transactionTemplate.executeWithoutResult(status -> {
                scrapRepository.deleteByPostIdAndChallengerId(postId, 432L);
                postRepository.deleteById(postId);
            });
        }
    }

    @Test
    @DisplayName("좋아요를 포함한 게시글 목록 조회 쿼리 수는 결과 건수에 비례해 증가하지 않는다")
    void 좋아요를_포함한_게시글_목록_조회_쿼리_수는_결과_건수에_비례해_증가하지_않는다() {
        // given
        persistLikedPosts(1, 1);
        long singleResultStatements = loadFreePostsAndCountStatements();
        persistLikedPosts(2, 19);

        // when
        long twentyResultStatements = loadFreePostsAndCountStatements();

        // then
        assertThat(twentyResultStatements).isEqualTo(singleResultStatements);
        assertThat(twentyResultStatements).isLessThanOrEqualTo(3L);
    }

    private Boolean toggleAfterSignal(
        Long postId,
        Long challengerId,
        CountDownLatch ready,
        CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("concurrent scrap toggle start timed out");
        }
        return scrapAdapter.toggleScrap(postId, challengerId);
    }

    private void persistLikedPosts(int startIndex, int count) {
        for (int index = startIndex; index < startIndex + count; index++) {
            Post post = Post.createPost("글 " + index, "본문 " + index, Category.FREE, 500L + index);
            post.toggleLike(600L + index);
            em.persist(post);
        }
        em.flush();
        em.clear();
    }

    private long loadFreePostsAndCountStatements() {
        statistics.clear();
        List<Post> posts = postQueryRepository.findAllByQuery(
            new PostSearchQuery(Category.FREE),
            PageRequest.of(0, 100)
        ).getContent();
        assertThat(posts).isNotEmpty();
        assertThat(posts).allSatisfy(post -> assertThat(post.getLikeCount()).isOne());
        long statementCount = statistics.getPrepareStatementCount();
        em.clear();
        return statementCount;
    }
}
