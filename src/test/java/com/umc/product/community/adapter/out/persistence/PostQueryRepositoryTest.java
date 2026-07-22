package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.umc.product.community.application.port.in.query.dto.PostSearchQuery;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Scrap;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManagerFactory;

@PersistenceAdapterTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(PostQueryRepository.class)
@DisplayName("PostQueryRepository 직접 JPA 조회")
class PostQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    PostQueryRepository sut;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUpStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @DisplayName("카테고리 페이지는 직접 JPA 게시글과 정확한 total을 두 쿼리로 반환한다")
    void 카테고리_페이지는_직접_JPA_게시글과_정확한_total을_두_쿼리로_반환한다() {
        // given
        Post first = persistPost("자유글 1", "첫 본문", Category.FREE, 101L);
        Post second = persistPost("자유글 2", "둘째 본문", Category.FREE, 102L);
        persistPost("질문글", "질문 본문", Category.QUESTION, 103L);
        flushAndClear();
        statistics.clear();

        // when
        Page<Post> result = sut.findAllByQuery(
            new PostSearchQuery(Category.FREE),
            PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent())
            .extracting(Post::getId)
            .containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("작성한 게시글 피드는 작성자만 페이지 크기와 최신순을 유지한다")
    void 작성한_게시글_피드는_작성자만_페이지_크기와_최신순을_유지한다() {
        // given
        Post oldest = persistPost("작성 글 1", "첫 본문", Category.FREE, 111L);
        Post middle = persistPost("작성 글 2", "둘째 본문", Category.FREE, 111L);
        Post newest = persistPost("작성 글 3", "셋째 본문", Category.FREE, 111L);
        persistPost("다른 작성자 글", "제외 본문", Category.FREE, 222L);
        flushAndClear();
        statistics.clear();

        // when
        Page<Post> firstPage = sut.findByAuthorChallengerId(111L, PageRequest.of(0, 2));
        Page<Post> secondPage = sut.findByAuthorChallengerId(111L, PageRequest.of(1, 2));

        // then
        assertThat(firstPage.getTotalElements()).isEqualTo(3L);
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getContent())
            .extracting(Post::getId)
            .containsExactly(newest.getId(), middle.getId());
        assertThat(secondPage.getTotalElements()).isEqualTo(3L);
        assertThat(secondPage.getSize()).isEqualTo(2);
        assertThat(secondPage.getNumberOfElements()).isEqualTo(1);
        assertThat(secondPage.getContent())
            .extracting(Post::getId)
            .containsExactly(oldest.getId());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("댓글 단 글 페이지는 중복을 제거하고 댓글 최신순 total을 두 쿼리로 계산한다")
    void 댓글_단_글_페이지는_중복을_제거하고_댓글_최신순_total을_두_쿼리로_계산한다() {
        // given
        Post first = persistPost("첫 글", "첫 본문", Category.FREE, 201L);
        Post second = persistPost("둘째 글", "둘째 본문", Category.FREE, 202L);
        persistComment(first, 301L, "첫 댓글");
        persistComment(first, 301L, "두 번째 댓글");
        persistComment(first, 999L, "다른 사용자 댓글");
        persistComment(second, 301L, "셋째 댓글");
        flushAndClear();
        statistics.clear();

        // when
        Page<Post> result = sut.findCommentedPostsByChallengerId(301L, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent())
            .extracting(Post::getId)
            .containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("스크랩 페이지는 스크랩 순서와 total을 유지하고 게시글별 추가 조회를 만들지 않는다")
    void 스크랩_페이지는_스크랩_순서와_total을_유지하고_게시글별_추가_조회를_만들지_않는다() {
        // given
        Post first = persistPost("첫 스크랩", "첫 본문", Category.FREE, 401L);
        Post second = persistPost("둘째 스크랩", "둘째 본문", Category.FREE, 402L);
        persistScrap(first, 501L);
        persistScrap(second, 501L);
        flushAndClear();
        statistics.clear();

        // when
        Page<Post> result = sut.findScrappedPostsByChallengerId(501L, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent())
            .extracting(Post::getId)
            .containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("키워드 검색은 제목 우선순위를 유지하고 결과별 N+1을 만들지 않는다")
    void 키워드_검색은_제목_우선순위를_유지하고_결과별_N_plus_1을_만들지_않는다() {
        // given
        Post titleStart = persistPost("Spring 스터디", "다른 내용", Category.FREE, 601L);
        Post titleContains = persistPost("즐거운 Spring 이야기", "다른 내용", Category.FREE, 602L);
        persistPost("제목 없음", "Spring 본문", Category.FREE, 603L);
        flushAndClear();
        statistics.clear();

        // when
        Page<Post> result = sut.searchByKeyword("spring", PageRequest.of(0, 10));

        // then
        assertThat(result.getContent())
            .extracting(Post::getId)
            .containsSubsequence(titleStart.getId(), titleContains.getId());
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("필터·키워드·댓글·스크랩이 비어 있으면 동일 pageable의 빈 결과를 반환한다")
    void 필터_키워드_댓글_스크랩이_비어_있으면_동일_pageable의_빈_결과를_반환한다() {
        PageRequest pageable = PageRequest.of(1, 10);

        assertThat(sut.findAllByQuery(new PostSearchQuery(null), pageable)).isEmpty();
        assertThat(sut.searchByKeyword(" ", pageable)).isEmpty();
        assertThat(sut.findCommentedPostsByChallengerId(Long.MAX_VALUE, pageable)).isEmpty();
        assertThat(sut.findScrappedPostsByChallengerId(Long.MAX_VALUE, pageable)).isEmpty();
    }

    private Post persistPost(String title, String content, Category category, Long challengerId) {
        return em.persist(Post.createPost(title, content, category, challengerId));
    }

    private void persistComment(Post post, Long challengerId, String content) {
        em.persist(Comment.create(post, challengerId, content, null));
    }

    private void persistScrap(Post post, Long challengerId) {
        em.persist(Scrap.create(post, challengerId));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
