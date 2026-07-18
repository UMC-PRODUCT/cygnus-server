package com.umc.product.community.adapter.out.persistence;

import static java.time.temporal.ChronoUnit.MICROS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.umc.product.common.BaseEntity;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Scrap;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@PersistenceAdapterTest
@DisplayName("커뮤니티 도메인 직접 JPA 매핑")
class CommunityDomainJpaMappingTest {

    private static final Instant MEET_AT = Instant.parse("2026-07-18T12:34:56Z");

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("Post Comment Scrap은 Long ID와 BaseEntity를 가진 직접 JPA 엔티티다")
    void post_comment_scrap은_long_id와_base_entity를_가진_직접_jpa_엔티티다() {
        assertDirectEntity(Post.class, "post");
        assertDirectEntity(Comment.class, "comment");
        assertDirectEntity(Scrap.class, "scrap");
    }

    @Test
    @DisplayName("Comment와 Scrap은 Post를 단방향 LAZY ManyToOne으로 참조한다")
    void commentAndScrapReferencePostWithLazyManyToOne() throws NoSuchFieldException {
        assertPostRelation(Comment.class);
        assertPostRelation(Scrap.class);
    }

    @Test
    @DisplayName("번개글 ID 감사 기본값 UTC 시각 좋아요가 flush-clear 후 보존된다")
    void 번개글_id_감사_기본값_utc_시각_좋아요가_flush_clear_후_보존된다() {
        // given
        Post post = Post.createLightning(
            "한강 번개",
            "저녁에 만나요",
            new Post.LightningInfo(MEET_AT, "여의나루역", 8, "https://open.kakao.com/o/example"),
            11L
        );
        post.toggleLike(22L);

        // when
        em.persistAndFlush(post);
        Long postId = post.getId();
        Instant createdAt = post.getCreatedAt();
        Instant initialUpdatedAt = post.getUpdatedAt();
        em.clear();
        Post reloaded = em.find(Post.class, postId);

        // then
        assertThat(postId).isPositive();
        assertThat(createdAt).isNotNull();
        assertThat(initialUpdatedAt).isNotNull();
        assertThat(reloaded.getCreatedAt()).isCloseTo(createdAt, within(1, MICROS));
        assertThat(reloaded.getUpdatedAt()).isCloseTo(initialUpdatedAt, within(1, MICROS));
        assertThat(reloaded.getRegion()).isEmpty();
        assertThat(reloaded.isAnonymous()).isFalse();
        assertThat(reloaded.getMeetAt()).isEqualTo(LocalDateTime.ofInstant(MEET_AT, ZoneOffset.UTC));
        assertThat(reloaded.getLightningInfoOrThrow().meetAt()).isEqualTo(MEET_AT);
        assertThat(reloaded.getLikeCount()).isOne();
        assertThat(reloaded.isLikedBy(22L)).isTrue();

        // when: 실제 영속 엔티티를 변경하고 다시 flush-clear 한다.
        reloaded.updateLightning(
            "한강 번개 수정",
            "수정된 본문",
            new Post.LightningInfo(MEET_AT.plusSeconds(3600), "국회의사당역", 10,
                "https://open.kakao.com/o/updated")
        );
        em.flush();
        Instant changedUpdatedAt = reloaded.getUpdatedAt();
        em.clear();
        Post updated = em.find(Post.class, postId);

        // then: 생성 시각은 유지되고 수정 시각만 실제로 전진한다.
        assertThat(changedUpdatedAt).isAfter(initialUpdatedAt);
        assertThat(updated.getCreatedAt()).isCloseTo(createdAt, within(1, MICROS));
        assertThat(updated.getUpdatedAt()).isCloseTo(changedUpdatedAt, within(1, MICROS));
        assertThat(updated.getTitle()).isEqualTo("한강 번개 수정");
    }

    @Test
    @DisplayName("대댓글 parentId와 좋아요가 flush-clear 후 보존된다")
    void 대댓글_parent_id와_좋아요가_flush_clear_후_보존된다() {
        // given
        Post post = em.persist(Post.createPost("글", "본문", Category.FREE, 31L));
        Comment parent = em.persist(Comment.create(post, 32L, "부모 댓글", null));
        Comment child = Comment.create(post, 33L, "대댓글", parent.getId());
        child.toggleLike(34L);

        // when
        em.persistAndFlush(child);
        Long childId = child.getId();
        Instant createdAt = child.getCreatedAt();
        Instant updatedAt = child.getUpdatedAt();
        em.clear();
        Comment reloaded = em.find(Comment.class, childId);

        // then
        assertThat(reloaded.getPostId()).isEqualTo(post.getId());
        assertThat(reloaded.getParentId()).isEqualTo(parent.getId());
        assertThat(reloaded.getCreatedAt()).isCloseTo(createdAt, within(1, MICROS));
        assertThat(updatedAt).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isCloseTo(updatedAt, within(1, MICROS));
        assertThat(reloaded.getLikeCount()).isOne();
        assertThat(reloaded.isLikedBy(34L)).isTrue();
    }

    @Test
    @DisplayName("스크랩 ID 감사 및 Post 관계가 flush-clear 후 보존된다")
    void 스크랩_id_감사_및_post_관계가_flush_clear_후_보존된다() {
        // given
        Post post = em.persist(Post.createPost("스크랩 글", "본문", Category.FREE, 41L));
        Scrap scrap = Scrap.create(post, 42L);

        // when
        em.persistAndFlush(scrap);
        Long scrapId = scrap.getId();
        Instant createdAt = scrap.getCreatedAt();
        Instant updatedAt = scrap.getUpdatedAt();
        em.clear();
        Scrap reloaded = em.find(Scrap.class, scrapId);

        // then
        assertThat(scrapId).isPositive();
        assertThat(reloaded.getPostId()).isEqualTo(post.getId());
        assertThat(reloaded.getChallengerId()).isEqualTo(42L);
        assertThat(reloaded.getCreatedAt()).isCloseTo(createdAt, within(1, MICROS));
        assertThat(updatedAt).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isCloseTo(updatedAt, within(1, MICROS));
    }

    private void assertDirectEntity(Class<?> entityType, String tableName) {
        assertThat(entityType.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(entityType.getSuperclass()).isEqualTo(BaseEntity.class);
        assertThat(entityType.getAnnotation(Table.class).name()).isEqualTo(tableName);

        Field idField = Arrays.stream(entityType.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(Id.class))
            .findFirst()
            .orElseThrow();
        assertThat(idField.getType()).isEqualTo(Long.class);
    }

    private void assertPostRelation(Class<?> entityType) throws NoSuchFieldException {
        Field postField = entityType.getDeclaredField("post");
        ManyToOne manyToOne = postField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = postField.getAnnotation(JoinColumn.class);

        assertThat(postField.getType()).isEqualTo(Post.class);
        assertThat(manyToOne).isNotNull();
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.name()).isEqualTo("post_id");
        assertThat(joinColumn.nullable()).isFalse();
        assertThat(joinColumn.updatable()).isFalse();
    }
}
