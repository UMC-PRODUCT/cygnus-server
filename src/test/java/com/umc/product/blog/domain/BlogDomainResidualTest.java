package com.umc.product.blog.domain;

import static com.umc.product.support.fixture.BlogUnitFixture.content;
import static com.umc.product.support.fixture.BlogUnitFixture.series;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Blog 도메인 경계 조건 테스트")
class BlogDomainResidualTest {

    @Test
    @DisplayName("콘텐츠 수정은 선택값을 정규화하고 공개 전환과 동일 상태 요청을 멱등 처리한다")
    void 콘텐츠_수정은_선택값을_정규화하고_상태를_멱등_처리한다() {
        BlogContent target = content();

        target.update("updated", " 제목 ", " ", " ", " 본문 ", BlogContentStatus.PUBLISHED, " ", " ", " ");
        var publishedAt = target.getPublishedAt();
        target.update("updated", "제목", null, null, "본문", BlogContentStatus.PUBLISHED, null, null, null);

        assertThat(target.getSummary()).isNull();
        assertThat(target.getThumbnailUrl()).isNull();
        assertThat(target.getSeoTitle()).isNull();
        assertThat(target.getSeoDescription()).isNull();
        assertThat(target.getOgImageUrl()).isNull();
        assertThat(target.getPublishedAt()).isEqualTo(publishedAt);
        target.ensurePublished();
        assertThat(target.getType()).isEqualTo(BlogContentType.ENGINEERING);
        assertThat(target.isAuthor(null)).isFalse();
        assertThat(target.canonicalPath()).isEqualTo("/engineering/updated");
        assertThat(target.resolvedSeoTitle()).isEqualTo("제목");
        assertThat(target.resolvedSeoDescription()).isNull();
        assertThat(target.resolvedOgImageUrl()).isNull();
    }

    @Test
    @DisplayName("콘텐츠는 공개되지 않은 상태와 직접 삭제 상태 전환을 거부한다")
    void 콘텐츠는_잘못된_상태_전이를_거부한다() {
        BlogContent target = content();

        assertThatThrownBy(target::ensurePublished).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> target.update(
            "slug", "제목", null, null, "본문", BlogContentStatus.DELETED, null, null, null
        )).isInstanceOf(BlogDomainException.class);

        target.softDelete(1L);
        target.softDelete(1L);

        assertThat(target.isDeleted()).isTrue();
        assertThatThrownBy(() -> target.update(
            "slug", "제목", null, null, "본문", BlogContentStatus.DRAFT, null, null, null
        )).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> target.softDelete(0L)).isInstanceOf(BlogDomainException.class);
    }

    @Test
    @DisplayName("콘텐츠 생성은 모든 필수값과 길이 제한을 검증한다")
    void 콘텐츠_생성은_모든_경계를_검증한다() {
        assertContentInvalid(null, "slug", "제목", null, null, "본문", BlogContentStatus.DRAFT, 1L, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, null, "제목", null, null, "본문", BlogContentStatus.DRAFT, 1L, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "a".repeat(201), "제목", null, null, "본문", BlogContentStatus.DRAFT, 1L, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "a".repeat(201), null, null, "본문", BlogContentStatus.DRAFT, 1L, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "제목", "a".repeat(501), null, "본문", BlogContentStatus.DRAFT, 1L, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, "a".repeat(1001), "본문", BlogContentStatus.DRAFT, 1L, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, null, BlogContentStatus.DRAFT, 1L, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, "a".repeat(100_001), BlogContentStatus.DRAFT, 1L, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, "본문", BlogContentStatus.DELETED, 1L, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, "본문", BlogContentStatus.DRAFT, null, null, null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, "본문", BlogContentStatus.DRAFT, 1L, "a".repeat(201), null, null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, "본문", BlogContentStatus.DRAFT, 1L, null, "a".repeat(501), null);
        assertContentInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, "본문", BlogContentStatus.DRAFT, 1L, null, null, "a".repeat(1001));
    }

    @Test
    @DisplayName("시리즈 수정과 삭제는 정규화, 멱등성, 삭제 후 변경 금지를 보장한다")
    void 시리즈_수정과_삭제는_불변식을_보장한다() {
        BlogSeries target = series();

        target.update("updated", " 제목 ", " ", " ", " ", " ", " ");
        assertThat(target.getTitle()).isEqualTo("제목");
        assertThat(target.getDescription()).isNull();
        assertThat(target.resolvedSeoTitle()).isEqualTo("제목");
        assertThat(target.resolvedSeoDescription()).isNull();
        assertThat(target.resolvedOgImageUrl()).isNull();
        assertThat(target.isAuthor(null)).isFalse();

        target.softDelete(2L);
        var deletedAt = target.getDeletedAt();
        target.softDelete(3L);

        assertThat(target.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(target.getDeletedByMemberId()).isEqualTo(2L);
        assertThatThrownBy(() -> target.update("x", "x", null, null, null, null, null))
            .isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> target.softDelete(null)).isInstanceOf(BlogDomainException.class);
    }

    @Test
    @DisplayName("시리즈 생성은 모든 필수값과 길이 제한을 검증한다")
    void 시리즈_생성은_모든_경계를_검증한다() {
        assertSeriesInvalid(null, "slug", "제목", null, null, 1L, null, null, null);
        assertSeriesInvalid(BlogContentType.ENGINEERING, null, "제목", null, null, 1L, null, null, null);
        assertSeriesInvalid(BlogContentType.ENGINEERING, "a".repeat(201), "제목", null, null, 1L, null, null, null);
        assertSeriesInvalid(BlogContentType.ENGINEERING, "slug", "a".repeat(201), null, null, 1L, null, null, null);
        assertSeriesInvalid(BlogContentType.ENGINEERING, "slug", "제목", "a".repeat(1001), null, 1L, null, null, null);
        assertSeriesInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, "a".repeat(1001), 1L, null, null, null);
        assertSeriesInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, 0L, null, null, null);
        assertSeriesInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, 1L, "a".repeat(201), null, null);
        assertSeriesInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, 1L, null, "a".repeat(501), null);
        assertSeriesInvalid(BlogContentType.ENGINEERING, "slug", "제목", null, null, 1L, null, null, "a".repeat(1001));
    }

    @Test
    @DisplayName("댓글은 parent ID, 삭제 주체, 중복 삭제와 수정 경계를 검증한다")
    void 댓글은_식별자와_삭제_경계를_검증한다() {
        assertThatThrownBy(() -> BlogComment.create(1L, 0L, 1L, false, null, "댓글"))
            .isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogComment.create(0L, null, 1L, false, null, "댓글"))
            .isInstanceOf(BlogDomainException.class);

        BlogComment target = BlogComment.create(1L, null, 1L, false, null, "댓글");
        target.updateContent(" 수정 ");
        assertThat(target.getContent()).isEqualTo("수정");
        assertThat(target.displayContent()).isEqualTo("수정");
        assertThatThrownBy(() -> target.deleteByUser(null)).isInstanceOf(BlogDomainException.class);

        target.deleteByUser(1L);
        var deletedAt = target.getDeletedAt();
        target.deleteByAdmin(2L);
        assertThat(target.getDeletedAt()).isEqualTo(deletedAt);
        assertThatThrownBy(() -> target.updateContent("다시 수정")).isInstanceOf(BlogDomainException.class);
    }

    @Test
    @DisplayName("연결 엔티티와 hashtag는 유효하지 않은 ID와 이름 경계를 거부한다")
    void 연결_엔티티와_hashtag는_경계를_검증한다() {
        BlogContentHashtag relation = BlogContentHashtag.create(1L, 2L, 0);
        assertThat(relation.getDisplayOrder()).isZero();
        assertThatThrownBy(() -> BlogContentHashtag.create(null, 2L, 0)).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogContentHashtag.create(1L, 0L, 0)).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogContentHashtag.create(1L, 2L, -1)).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogSeriesContent.create(0L, 2L, 0)).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogSeriesContent.create(1L, null, 0)).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogHashtag.create(null)).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogHashtag.create("# #")).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogHashtag.create("a".repeat(31))).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogHashtag.create("a\u00a0b")).isInstanceOf(BlogDomainException.class);
    }

    @Test
    @DisplayName("정렬 값은 기본값과 방향을 제공하고 지원하지 않는 입력을 거부한다")
    void 정렬_값은_입력을_엄격히_파싱한다() {
        assertThat(BlogSeriesSort.fromSeriesList(null)).isEqualTo(BlogSeriesSort.CREATED_AT_DESC);
        assertThat(BlogSeriesSort.fromSeriesList(" createdAt,asc ").isAscending()).isTrue();
        assertThat(BlogSeriesSort.fromSeriesContents(null)).isEqualTo(BlogSeriesSort.DISPLAY_ORDER_ASC);
        assertThat(BlogContentSort.from(null).isAscending()).isFalse();
        assertThat(BlogContentSort.from("publishedAt,asc").isAscending()).isTrue();
        assertThat(BlogCommentSort.from(null)).isEqualTo(BlogCommentSort.CREATED_AT_DESC);
        assertThat(BlogHashtagSort.from(null)).isEqualTo(BlogHashtagSort.CONTENT_COUNT_DESC);
        assertThatThrownBy(() -> BlogSeriesSort.fromSeriesList("invalid")).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogSeriesSort.fromSeriesContents("invalid")).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogContentSort.from("invalid")).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogCommentSort.from("invalid")).isInstanceOf(BlogDomainException.class);
        assertThatThrownBy(() -> BlogHashtagSort.from("invalid")).isInstanceOf(BlogDomainException.class);
    }

    @Test
    @DisplayName("콘텐츠 타입 null과 사용자 메시지 domain 예외를 명시적으로 지원한다")
    void 콘텐츠_타입과_domain_예외를_검증한다() {
        assertThatThrownBy(() -> BlogContentType.fromPath(null)).isInstanceOf(BlogDomainException.class);
        BlogDomainException exception = new BlogDomainException(BlogErrorCode.INVALID_SLUG, "custom");
        assertThat(exception.getMessage()).isEqualTo("custom");
    }

    private static void assertContentInvalid(
        BlogContentType type,
        String slug,
        String title,
        String summary,
        String thumbnailUrl,
        String body,
        BlogContentStatus status,
        Long authorMemberId,
        String seoTitle,
        String seoDescription,
        String ogImageUrl
    ) {
        assertThatThrownBy(() -> BlogContent.create(
            type, slug, title, summary, thumbnailUrl, body, status, authorMemberId, seoTitle, seoDescription, ogImageUrl
        )).isInstanceOf(BlogDomainException.class);
    }

    private static void assertSeriesInvalid(
        BlogContentType type,
        String slug,
        String title,
        String description,
        String thumbnailUrl,
        Long authorMemberId,
        String seoTitle,
        String seoDescription,
        String ogImageUrl
    ) {
        assertThatThrownBy(() -> BlogSeries.create(
            type, slug, title, description, thumbnailUrl, authorMemberId, seoTitle, seoDescription, ogImageUrl
        )).isInstanceOf(BlogDomainException.class);
    }
}
