package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Scrap;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(ScrapPersistenceAdapter.class)
@DisplayName("ScrapPersistenceAdapter")
class ScrapPersistenceAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ScrapPersistenceAdapter sut;

    @Test
    @DisplayName("Scrap 도메인을 별도 매퍼 없이 저장 조회 삭제한다")
    void scrap_도메인을_별도_매퍼_없이_저장_조회_삭제한다() {
        // given
        Post post = em.persist(Post.createPost("스크랩", "본문", Category.FREE, 301L));
        Scrap saved = sut.save(Scrap.create(post, 302L));
        em.flush();
        Long scrapId = saved.getId();
        em.clear();

        // when
        Scrap reloaded = sut.findByPostIdAndChallengerId(post.getId(), 302L).orElseThrow();
        sut.delete(reloaded);
        em.flush();
        em.clear();

        // then
        assertThat(scrapId).isPositive();
        assertThat(sut.findByPostIdAndChallengerId(post.getId(), 302L)).isEmpty();
    }

    @Test
    @DisplayName("스크랩 토글은 동일 스카라 ID 조합을 추가하고 취소한다")
    void 스크랩_토글은_동일_스카라_id_조합을_추가하고_취소한다() {
        // given
        Post post = em.persist(Post.createPost("토글", "본문", Category.FREE, 311L));
        em.flush();

        // when
        boolean added = sut.toggleScrap(post.getId(), 312L);
        boolean removed = sut.toggleScrap(post.getId(), 312L);
        em.flush();

        // then
        assertThat(added).isTrue();
        assertThat(removed).isFalse();
        assertThat(sut.countByPostId(post.getId())).isZero();
    }
}
