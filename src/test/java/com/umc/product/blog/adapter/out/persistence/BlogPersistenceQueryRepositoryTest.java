package com.umc.product.blog.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.umc.product.blog.domain.BlogComment;
import com.umc.product.blog.domain.BlogCommentLike;
import com.umc.product.blog.domain.BlogCommentSort;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentHashtag;
import com.umc.product.blog.domain.BlogContentLike;
import com.umc.product.blog.domain.BlogContentSort;
import com.umc.product.blog.domain.BlogContentStatus;
import com.umc.product.blog.domain.BlogContentType;
import com.umc.product.blog.domain.BlogDomainException;
import com.umc.product.blog.domain.BlogHashtag;
import com.umc.product.blog.domain.BlogHashtagSort;
import com.umc.product.blog.domain.BlogSeries;
import com.umc.product.blog.domain.BlogSeriesContent;
import com.umc.product.blog.domain.BlogSeriesSort;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@ActiveProfiles("test")
@Import({
    BlogPersistenceAdapter.class,
    BlogCommentQueryRepository.class,
    BlogCommentLikeQueryRepository.class,
    BlogContentQueryRepository.class,
    BlogSeriesQueryRepository.class,
    BlogHashtagQueryRepository.class,
    BlogSeoQueryRepository.class
})
@DisplayName("Blog persistence 및 QueryDSL 통합 테스트")
class BlogPersistenceQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    BlogPersistenceAdapter adapter;

    BlogContent first;
    BlogContent second;
    BlogContent design;
    BlogContent draft;
    BlogSeries engineeringSeries;
    BlogSeries designSeries;
    BlogHashtag spring;
    BlogHashtag javaHashtag;
    BlogComment firstComment;
    BlogComment secondComment;
    BlogComment reply;

    @BeforeEach
    void setUp() {
        first = persistContent(BlogContentType.ENGINEERING, "first", BlogContentStatus.PUBLISHED);
        second = persistContent(BlogContentType.ENGINEERING, "second", BlogContentStatus.PUBLISHED);
        design = persistContent(BlogContentType.DESIGN, "design", BlogContentStatus.PUBLISHED);
        draft = persistContent(BlogContentType.ENGINEERING, "draft", BlogContentStatus.DRAFT);

        BlogContent deleted = persistContent(BlogContentType.ENGINEERING, "deleted", BlogContentStatus.PUBLISHED);
        deleted.softDelete(1L);

        engineeringSeries = persistSeries(BlogContentType.ENGINEERING, "engineering-series");
        designSeries = persistSeries(BlogContentType.DESIGN, "design-series");
        BlogSeries deletedSeries = persistSeries(BlogContentType.ENGINEERING, "deleted-series");
        deletedSeries.softDelete(1L);
        em.persist(BlogSeriesContent.create(engineeringSeries.getId(), first.getId(), 0));
        em.persist(BlogSeriesContent.create(engineeringSeries.getId(), second.getId(), 1));
        em.persist(BlogSeriesContent.create(designSeries.getId(), design.getId(), 0));
        em.persist(BlogSeriesContent.create(deletedSeries.getId(), first.getId(), 0));

        spring = em.persist(BlogHashtag.create("Spring"));
        javaHashtag = em.persist(BlogHashtag.create("Java"));
        em.persist(BlogContentHashtag.create(first.getId(), spring.getId(), 0));
        em.persist(BlogContentHashtag.create(first.getId(), javaHashtag.getId(), 1));
        em.persist(BlogContentHashtag.create(second.getId(), spring.getId(), 0));
        em.persist(BlogContentHashtag.create(draft.getId(), spring.getId(), 0));

        firstComment = em.persist(BlogComment.create(first.getId(), null, 1L, false, null, "첫 댓글"));
        secondComment = em.persist(BlogComment.create(first.getId(), null, 2L, false, null, "둘째 댓글"));
        reply = em.persist(BlogComment.create(first.getId(), firstComment.getId(), 2L, false, null, "답글"));
        BlogComment deletedWithReply = em.persist(
            BlogComment.create(first.getId(), null, 3L, false, null, "삭제 예정")
        );
        em.persist(BlogComment.create(first.getId(), deletedWithReply.getId(), 4L, false, null, "남은 답글"));
        deletedWithReply.deleteByUser(3L);
        BlogComment deletedWithoutReply = em.persist(
            BlogComment.create(first.getId(), null, 5L, false, null, "완전 삭제")
        );
        deletedWithoutReply.deleteByUser(5L);

        em.persist(BlogContentLike.create(first.getId(), 1L));
        em.persist(BlogCommentLike.create(firstComment.getId(), 1L));
        em.persist(BlogCommentLike.create(firstComment.getId(), 2L));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("콘텐츠 목록은 공개 상태, type, 시리즈, hashtag, 양방향 cursor와 정렬을 검증한다")
    void 콘텐츠_목록은_필터와_cursor를_검증한다() {
        assertThat(adapter.listPublicContents(null, null, null, BlogContentSort.PUBLISHED_AT_DESC, null, 20))
            .extracting(BlogContent::getId)
            .containsExactlyInAnyOrder(first.getId(), second.getId(), design.getId());
        assertThat(adapter.listPublicContents(
            BlogContentType.ENGINEERING,
            engineeringSeries.getSlug(),
            spring.getSlug(),
            BlogContentSort.PUBLISHED_AT_ASC,
            null,
            20
        )).extracting(BlogContent::getId).containsExactly(first.getId(), second.getId());
        assertThat(adapter.listPublicContents(
            null,
            engineeringSeries.getSlug(),
            " ",
            BlogContentSort.PUBLISHED_AT_DESC,
            null,
            20
        )).extracting(BlogContent::getId).containsExactly(second.getId(), first.getId());

        assertThat(adapter.listPublicContents(
            BlogContentType.ENGINEERING,
            engineeringSeries.getSlug(),
            spring.getSlug(),
            BlogContentSort.PUBLISHED_AT_ASC,
            first.getId(),
            20
        )).extracting(BlogContent::getId).containsExactly(second.getId());
        assertThat(adapter.listPublicContents(
            BlogContentType.ENGINEERING,
            null,
            null,
            BlogContentSort.PUBLISHED_AT_DESC,
            second.getId(),
            20
        )).extracting(BlogContent::getId).containsExactly(first.getId());

        assertThatThrownBy(() -> adapter.listPublicContents(
            null, null, null, BlogContentSort.PUBLISHED_AT_DESC, Long.MAX_VALUE, 20
        )).isInstanceOf(BlogDomainException.class);
    }

    @Test
    @DisplayName("시리즈와 hashtag 콘텐츠 조회는 표시 순서·type·cursor를 보장하고 잘못된 cursor를 거부한다")
    void 연결_콘텐츠_조회는_cursor와_순서를_검증한다() {
        assertThat(adapter.listPublicSeriesContents(engineeringSeries.getId(), null, 20))
            .extracting(BlogContent::getId)
            .containsExactly(first.getId(), second.getId());
        assertThat(adapter.listPublicSeriesContents(engineeringSeries.getId(), first.getId(), 20))
            .extracting(BlogContent::getId)
            .containsExactly(second.getId());
        assertThatThrownBy(() -> adapter.listPublicSeriesContents(engineeringSeries.getId(), design.getId(), 20))
            .isInstanceOf(BlogDomainException.class);

        assertThat(adapter.listPublicHashtagContents(
            spring.getId(), null, BlogContentSort.PUBLISHED_AT_ASC, null, 20
        )).extracting(BlogContent::getId).containsExactly(first.getId(), second.getId());
        assertThat(adapter.listPublicHashtagContents(
            spring.getId(), BlogContentType.ENGINEERING, BlogContentSort.PUBLISHED_AT_ASC, first.getId(), 20
        )).extracting(BlogContent::getId).containsExactly(second.getId());
        assertThatThrownBy(() -> adapter.listPublicHashtagContents(
            spring.getId(), null, BlogContentSort.PUBLISHED_AT_DESC, design.getId(), 20
        )).isInstanceOf(BlogDomainException.class);
    }

    @Test
    @DisplayName("시리즈 조회는 공개 콘텐츠 존재, type, 양방향 cursor, 그룹·count의 빈 입력을 검증한다")
    void 시리즈_조회는_공개_관계와_빈_batch를_검증한다() {
        assertThat(adapter.listPublicSeries(null, BlogSeriesSort.CREATED_AT_DESC, null, 20))
            .extracting(BlogSeries::getId)
            .containsExactly(designSeries.getId(), engineeringSeries.getId());
        assertThat(adapter.listPublicSeries(
            BlogContentType.ENGINEERING, BlogSeriesSort.CREATED_AT_ASC, engineeringSeries.getId(), 20
        )).isEmpty();
        assertThat(adapter.listPublicSeries(
            null, BlogSeriesSort.CREATED_AT_DESC, designSeries.getId(), 20
        )).extracting(BlogSeries::getId).containsExactly(engineeringSeries.getId());
        assertThatThrownBy(() -> adapter.listPublicSeries(
            null, BlogSeriesSort.CREATED_AT_DESC, Long.MAX_VALUE, 20
        )).isInstanceOf(BlogDomainException.class);

        assertThat(adapter.listSeriesByContentIds(null)).isEmpty();
        assertThat(adapter.listSeriesByContentIds(List.of(first.getId())))
            .extracting(BlogSeries::getId)
            .containsExactly(engineeringSeries.getId());
        assertThat(adapter.listSeriesByContentIdsGrouped(List.of())).isEmpty();
        assertThat(adapter.listSeriesByContentIdsGrouped(List.of(first.getId(), design.getId())))
            .containsOnlyKeys(first.getId(), design.getId());
        assertThat(adapter.countPublishedContentsBySeriesIds(null)).isEmpty();
        assertThat(adapter.countPublishedContentsBySeriesIds(List.of(engineeringSeries.getId(), designSeries.getId())))
            .containsEntry(engineeringSeries.getId(), 2)
            .containsEntry(designSeries.getId(), 1);
        assertThat(adapter.hasPublishedContent(engineeringSeries.getId())).isTrue();
    }

    @Test
    @DisplayName("hashtag 조회는 검색어·count 정렬·cursor·빈 batch와 type count를 검증한다")
    void hashtag_조회는_검색_cursor_count를_검증한다() {
        assertThat(adapter.listPublicHashtags(null, null, BlogHashtagSort.CONTENT_COUNT_DESC, null, 20))
            .extracting(BlogHashtag::getId)
            .containsExactly(spring.getId(), javaHashtag.getId());
        assertThat(adapter.listPublicHashtags(
            BlogContentType.ENGINEERING, " spring ", BlogHashtagSort.CONTENT_COUNT_DESC, spring.getId(), 20
        )).isEmpty();
        assertThatThrownBy(() -> adapter.listPublicHashtags(
            null, null, BlogHashtagSort.CONTENT_COUNT_DESC, Long.MAX_VALUE, 20
        )).isInstanceOf(BlogDomainException.class);

        assertThat(adapter.listHashtagsByContentIds(null)).isEmpty();
        assertThat(adapter.listHashtagsByContentIds(List.of(first.getId())))
            .extracting(BlogHashtag::getId)
            .containsExactly(spring.getId(), javaHashtag.getId());
        assertThat(adapter.listHashtagsByContentIdsGrouped(List.of())).isEmpty();
        assertThat(adapter.listHashtagsByContentIdsGrouped(List.of(first.getId(), second.getId())))
            .containsOnlyKeys(first.getId(), second.getId());
        assertThat(adapter.countPublishedContentsByHashtagIds(null)).isEmpty();
        assertThat(adapter.countPublishedContentsByHashtagIds(List.of())).isEmpty();
        assertThat(adapter.countPublishedContentsByHashtagIds(List.of(spring.getId(), javaHashtag.getId())))
            .containsEntry(spring.getId(), 2)
            .containsEntry(javaHashtag.getId(), 1);
        assertThat(adapter.countPublishedContentsByHashtagIds(
            List.of(spring.getId()), BlogContentType.DESIGN
        )).isEmpty();
    }

    @Test
    @DisplayName("댓글 조회는 top-level 가시성, 양방향 cursor, reply 빈 batch와 좋아요 집계를 검증한다")
    void 댓글과_좋아요_조회는_가시성과_빈_batch를_검증한다() {
        List<BlogComment> descending = adapter.listTopLevel(
            first.getId(), BlogCommentSort.CREATED_AT_DESC, null, 20
        );
        assertThat(descending).extracting(BlogComment::getContent)
            .contains("첫 댓글", "둘째 댓글", "삭제 예정")
            .doesNotContain("완전 삭제");
        assertThat(adapter.listTopLevel(
            first.getId(), BlogCommentSort.CREATED_AT_ASC, firstComment.getId(), 20
        )).extracting(BlogComment::getId).contains(secondComment.getId());
        assertThat(adapter.listTopLevel(
            first.getId(), BlogCommentSort.CREATED_AT_DESC, secondComment.getId(), 20
        )).extracting(BlogComment::getId).contains(firstComment.getId());
        assertThatThrownBy(() -> adapter.listTopLevel(
            first.getId(), BlogCommentSort.CREATED_AT_DESC, reply.getId(), 20
        )).isInstanceOf(BlogDomainException.class);

        assertThat(adapter.listRepliesByParentIds(null)).isEmpty();
        assertThat(adapter.listRepliesByParentIds(List.of(firstComment.getId())))
            .extracting(BlogComment::getId)
            .containsExactly(reply.getId());
        assertThat(adapter.existsVisibleReply(firstComment.getId())).isTrue();
        assertThat(adapter.countCommentLikesByCommentIds(null)).isEmpty();
        assertThat(adapter.countCommentLikesByCommentIds(List.of(firstComment.getId())))
            .containsEntry(firstComment.getId(), 2);
        assertThat(adapter.findLikedCommentIds(List.of(firstComment.getId()), null)).isEmpty();
        assertThat(adapter.findLikedCommentIds(List.of(firstComment.getId()), 1L))
            .containsExactly(firstComment.getId());
    }

    @Test
    @DisplayName("adapter는 기본 조회·중복 제외·빈 ID와 SEO content/series/hashtag path를 보존한다")
    void adapter_계약과_SEO_path를_검증한다() {
        assertThat(adapter.findContentById(first.getId())).isPresent();
        assertThat(adapter.findByTypeAndSlug(BlogContentType.ENGINEERING, first.getSlug())).isPresent();
        assertThat(adapter.listByIds(null)).isEmpty();
        assertThat(adapter.listByIds(List.of(first.getId(), second.getId()))).hasSize(2);
        assertThat(adapter.existsContentByTypeAndSlug(
            BlogContentType.ENGINEERING, second.getSlug(), first.getId()
        )).isTrue();
        assertThat(adapter.findSeriesById(engineeringSeries.getId())).isPresent();
        assertThat(adapter.findSeriesByTypeAndSlug(
            BlogContentType.ENGINEERING, engineeringSeries.getSlug()
        )).isPresent();
        assertThat(adapter.existsSeriesByTypeAndSlug(
            BlogContentType.ENGINEERING, engineeringSeries.getSlug(), designSeries.getId()
        )).isTrue();
        assertThat(adapter.findBySlug(spring.getSlug())).isPresent();
        assertThat(adapter.findByNormalizedName(spring.getNormalizedName())).isPresent();

        Map<String, List<String>> pathsByType = adapter.listPublicSeoPaths().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                row -> row.type(),
                java.util.stream.Collectors.mapping(row -> row.path(), java.util.stream.Collectors.toList())
            ));
        assertThat(pathsByType.get("content")).contains("/engineering/first");
        assertThat(pathsByType.get("series")).contains("/series/engineering/engineering-series");
        assertThat(pathsByType.get("hashtag")).contains("/hashtags/spring");
    }

    private BlogContent persistContent(BlogContentType type, String slug, BlogContentStatus status) {
        return em.persist(BlogContent.create(
            type,
            slug,
            "제목 " + slug,
            "요약",
            null,
            "본문",
            status,
            1L,
            null,
            null,
            null
        ));
    }

    private BlogSeries persistSeries(BlogContentType type, String slug) {
        return em.persist(BlogSeries.create(
            type,
            slug,
            "시리즈 " + slug,
            null,
            null,
            1L,
            null,
            null,
            null
        ));
    }
}
