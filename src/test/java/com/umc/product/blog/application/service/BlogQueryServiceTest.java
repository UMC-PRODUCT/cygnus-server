package com.umc.product.blog.application.service;

import static com.umc.product.support.fixture.BlogUnitFixture.content;
import static com.umc.product.support.fixture.BlogUnitFixture.series;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.blog.application.port.in.query.dto.BlogCommentCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogCommentListQuery;
import com.umc.product.blog.application.port.in.query.dto.BlogContentCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogContentInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogContentListQuery;
import com.umc.product.blog.application.port.in.query.dto.BlogContentSummaryInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesListQuery;
import com.umc.product.blog.application.port.out.LoadBlogCommentPort;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.application.port.out.LoadBlogHashtagPort;
import com.umc.product.blog.application.port.out.LoadBlogLikePort;
import com.umc.product.blog.application.port.out.LoadBlogSeoPort;
import com.umc.product.blog.application.port.out.LoadBlogSeriesPort;
import com.umc.product.blog.application.port.out.dto.BlogSeoPathRow;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentSort;
import com.umc.product.blog.domain.BlogContentStatus;
import com.umc.product.blog.domain.BlogContentType;
import com.umc.product.blog.domain.BlogDomainException;
import com.umc.product.blog.domain.BlogHashtag;
import com.umc.product.blog.domain.BlogHashtagSort;
import com.umc.product.blog.domain.BlogSeries;
import com.umc.product.blog.domain.BlogSeriesSort;

@ExtendWith(MockitoExtension.class)
@DisplayName("Blog query service 테스트")
class BlogQueryServiceTest {

    @Nested
    @DisplayName("콘텐츠 조회")
    class ContentQuery {

        @Mock
        LoadBlogContentPort loadBlogContentPort;

        @Mock
        LoadBlogSeoPort loadBlogSeoPort;

        @Mock
        BlogContentInfoAssembler contentInfoAssembler;

        @Mock
        GetChallengerRoleUseCase getChallengerRoleUseCase;

        BlogContentQueryService service;

        @BeforeEach
        void setUp() {
            service = new BlogContentQueryService(
                loadBlogContentPort, loadBlogSeoPort, contentInfoAssembler, getChallengerRoleUseCase
            );
        }

        @Test
        @DisplayName("목록은 size+1 조회 후 다음 cursor와 최대 크기를 계산한다")
        void 목록은_cursor와_최대_크기를_계산한다() {
            BlogContent first = identified(content("first", BlogContentStatus.PUBLISHED, 1L), 1L);
            BlogContent second = identified(content("second", BlogContentStatus.PUBLISHED, 1L), 2L);
            BlogContentSummaryInfo summary = org.mockito.Mockito.mock(BlogContentSummaryInfo.class);
            given(loadBlogContentPort.listPublicContents(
                BlogContentType.ENGINEERING, null, null, BlogContentSort.PUBLISHED_AT_DESC, null, 51
            )).willReturn(List.of(first, second));
            given(contentInfoAssembler.assembleSummaries(List.of(first, second), null, false))
                .willReturn(List.of(summary));

            BlogContentCursorInfo result = service.getPublicContents(
                BlogContentListQuery.of("engineering", null, null, null, 100, null, null)
            );

            assertThat(result.content()).containsExactly(summary);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("목록은 기본 size를 적용하고 size+1행이면 마지막 노출 ID를 cursor로 반환한다")
        void 목록은_기본_size와_다음_cursor를_계산한다() {
            List<BlogContent> rows = java.util.stream.LongStream.rangeClosed(1, 21)
                .mapToObj(id -> identified(content("content-" + id, BlogContentStatus.PUBLISHED, 1L), id))
                .toList();
            given(loadBlogContentPort.listPublicContents(
                null, "series", "tag", BlogContentSort.PUBLISHED_AT_ASC, 5L, 21
            )).willReturn(rows);
            given(contentInfoAssembler.assembleSummaries(rows.subList(0, 20), 2L, true)).willReturn(List.of());
            given(getChallengerRoleUseCase.isSuperAdmin(2L)).willReturn(true);

            BlogContentCursorInfo result = service.getPublicContents(
                BlogContentListQuery.of(" ", "series", "tag", 5L, 0, "publishedAt,asc", 2L)
            );

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(20L);
        }

        @Test
        @DisplayName("preview는 미존재 또는 삭제 콘텐츠를 숨기고 정상 콘텐츠를 조립한다")
        void preview는_미존재와_삭제_콘텐츠를_숨긴다() {
            given(loadBlogContentPort.findContentById(1L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> service.getPreview(1L, null)).isInstanceOf(BlogDomainException.class);

            BlogContent deleted = content();
            deleted.softDelete(1L);
            given(loadBlogContentPort.findContentById(2L)).willReturn(Optional.of(deleted));
            assertThatThrownBy(() -> service.getPreview(2L, null)).isInstanceOf(BlogDomainException.class);

            BlogContent target = content();
            BlogContentInfo expected = org.mockito.Mockito.mock(BlogContentInfo.class);
            given(loadBlogContentPort.findContentById(3L)).willReturn(Optional.of(target));
            given(contentInfoAssembler.assemble(target, 2L, true)).willReturn(expected);
            given(getChallengerRoleUseCase.isSuperAdmin(2L)).willReturn(true);
            assertThat(service.getPreview(3L, 2L)).isSameAs(expected);
        }

        @Test
        @DisplayName("SEO path는 persistence row를 application info로 변환한다")
        void SEO_path를_info로_변환한다() {
            Instant updatedAt = Instant.parse("2026-07-22T00:00:00Z");
            given(loadBlogSeoPort.listPublicSeoPaths())
                .willReturn(List.of(new BlogSeoPathRow("content", "/engineering/slug", updatedAt)));

            assertThat(service.getSeoPaths().paths()).singleElement().satisfies(path -> {
                assertThat(path.type()).isEqualTo("content");
                assertThat(path.path()).isEqualTo("/engineering/slug");
                assertThat(path.updatedAt()).isEqualTo(updatedAt);
            });
        }
    }

    @Nested
    @DisplayName("시리즈 조회")
    class SeriesQuery {

        @Mock
        LoadBlogSeriesPort loadBlogSeriesPort;

        @Mock
        LoadBlogContentPort loadBlogContentPort;

        @Mock
        BlogSeriesInfoAssembler seriesInfoAssembler;

        @Mock
        BlogContentInfoAssembler contentInfoAssembler;

        @Mock
        GetChallengerRoleUseCase getChallengerRoleUseCase;

        BlogSeriesQueryService service;

        @BeforeEach
        void setUp() {
            service = new BlogSeriesQueryService(
                loadBlogSeriesPort,
                loadBlogContentPort,
                seriesInfoAssembler,
                contentInfoAssembler,
                getChallengerRoleUseCase
            );
        }

        @Test
        @DisplayName("시리즈 목록은 optional type과 size+1 cursor 규칙을 적용한다")
        void 시리즈_목록은_cursor를_계산한다() {
            List<BlogSeries> rows = java.util.stream.LongStream.rangeClosed(1, 21)
                .mapToObj(id -> identified(series("series-" + id, 1L), id))
                .toList();
            given(loadBlogSeriesPort.listPublicSeries(null, BlogSeriesSort.CREATED_AT_DESC, null, 21))
                .willReturn(rows);
            given(seriesInfoAssembler.assembleSummaries(rows.subList(0, 20), null, false)).willReturn(List.of());

            var result = service.getPublicSeries(BlogSeriesListQuery.of(" ", null, 0, null, null));

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(20L);

            given(loadBlogSeriesPort.listPublicSeries(
                BlogContentType.ENGINEERING, BlogSeriesSort.CREATED_AT_ASC, null, 2
            )).willReturn(List.of());
            given(seriesInfoAssembler.assembleSummaries(List.of(), null, false)).willReturn(List.of());
            assertThat(service.getPublicSeries(
                BlogSeriesListQuery.of("engineering", null, 1, "createdAt,asc", null)
            ).content()).isEmpty();
        }

        @Test
        @DisplayName("공개 시리즈는 삭제되지 않고 공개 콘텐츠가 있을 때만 조회한다")
        void 공개_시리즈는_공개_콘텐츠가_있어야_한다() {
            BlogSeries target = identified(series(), 10L);
            given(loadBlogSeriesPort.findSeriesByTypeAndSlug(BlogContentType.ENGINEERING, "series"))
                .willReturn(Optional.of(target));
            given(loadBlogSeriesPort.hasPublishedContent(10L)).willReturn(false, true);
            assertThatThrownBy(() -> service.getPublicSeries("engineering", "series", null))
                .isInstanceOf(BlogDomainException.class);

            BlogSeriesInfo expected = org.mockito.Mockito.mock(BlogSeriesInfo.class);
            given(seriesInfoAssembler.assemble(target, null, false)).willReturn(expected);
            assertThat(service.getPublicSeries("engineering", "series", null)).isSameAs(expected);
        }

        @Test
        @DisplayName("preview는 미존재와 삭제 시리즈를 숨긴다")
        void preview는_미존재와_삭제_시리즈를_숨긴다() {
            given(loadBlogSeriesPort.findSeriesById(1L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> service.getPreview(1L, null)).isInstanceOf(BlogDomainException.class);
            BlogSeries deleted = series();
            deleted.softDelete(1L);
            given(loadBlogSeriesPort.findSeriesById(2L)).willReturn(Optional.of(deleted));
            assertThatThrownBy(() -> service.getPreview(2L, null)).isInstanceOf(BlogDomainException.class);

            BlogSeries target = series();
            BlogSeriesInfo expected = org.mockito.Mockito.mock(BlogSeriesInfo.class);
            given(loadBlogSeriesPort.findSeriesById(3L)).willReturn(Optional.of(target));
            given(seriesInfoAssembler.assemble(target, null, false)).willReturn(expected);
            assertThat(service.getPreview(3L, null)).isSameAs(expected);
        }

        @Test
        @DisplayName("시리즈 콘텐츠 목록은 정렬을 검증하고 최대 size와 다음 cursor를 적용한다")
        void 시리즈_콘텐츠_목록은_cursor를_계산한다() {
            BlogSeries target = identified(series(), 10L);
            given(loadBlogSeriesPort.findSeriesByTypeAndSlug(BlogContentType.ENGINEERING, "series"))
                .willReturn(Optional.of(target));
            given(loadBlogSeriesPort.hasPublishedContent(10L)).willReturn(true);
            List<BlogContent> rows = java.util.stream.LongStream.rangeClosed(1, 51)
                .mapToObj(id -> identified(content("content-" + id, BlogContentStatus.PUBLISHED, 1L), id))
                .toList();
            given(loadBlogContentPort.listPublicSeriesContents(10L, 3L, 51)).willReturn(rows);
            given(contentInfoAssembler.assembleSummaries(rows.subList(0, 50), 2L, true)).willReturn(List.of());
            given(getChallengerRoleUseCase.isSuperAdmin(2L)).willReturn(true);

            BlogContentCursorInfo result = service.getPublicSeriesContents(
                "engineering", "series", 3L, 100, null, 2L
            );

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("해시태그와 댓글 조회")
    class HashtagAndCommentQuery {

        @Mock
        LoadBlogHashtagPort loadBlogHashtagPort;

        @Mock
        LoadBlogContentPort loadBlogContentPort;

        @Mock
        LoadBlogCommentPort loadBlogCommentPort;

        @Mock
        LoadBlogLikePort loadBlogLikePort;

        @Mock
        BlogContentInfoAssembler contentInfoAssembler;

        @Mock
        BlogCommentInfoAssembler commentInfoAssembler;

        @Mock
        GetChallengerRoleUseCase getChallengerRoleUseCase;

        @Test
        @DisplayName("해시태그 목록은 기본 size와 누락 count 0을 적용한다")
        void 해시태그_목록은_기본_size와_count를_적용한다() {
            BlogHashtag hashtag = BlogHashtag.create("Spring");
            ReflectionTestUtils.setField(hashtag, "id", 10L);
            given(loadBlogHashtagPort.listPublicHashtags(
                null, "spring", BlogHashtagSort.CONTENT_COUNT_DESC, null, 21
            )).willReturn(List.of(hashtag));
            given(loadBlogHashtagPort.countPublishedContentsByHashtagIds(List.of(10L), null)).willReturn(Map.of());
            BlogHashtagQueryService service = new BlogHashtagQueryService(
                loadBlogHashtagPort, loadBlogContentPort, contentInfoAssembler, getChallengerRoleUseCase
            );

            var result = service.getPublicHashtags(" ", "spring", null, 0, null);

            assertThat(result.content()).singleElement().satisfies(info -> assertThat(info.contentCount()).isZero());
        }

        @Test
        @DisplayName("댓글 목록은 콘텐츠가 없거나 댓글 page가 비면 빈 cursor를 반환한다")
        void 댓글_목록은_빈_경로를_단축한다() {
            BlogCommentQueryService service = new BlogCommentQueryService(
                loadBlogContentPort,
                loadBlogCommentPort,
                loadBlogLikePort,
                commentInfoAssembler,
                getChallengerRoleUseCase
            );
            BlogCommentListQuery query = BlogCommentListQuery.of("engineering", "slug", null, 0, null, null);
            given(loadBlogContentPort.findPublishedByTypeAndSlug(BlogContentType.ENGINEERING, "slug"))
                .willReturn(Optional.empty());
            assertThat(service.getComments(query).content()).isEmpty();

            BlogContent target = identified(content("slug", BlogContentStatus.PUBLISHED, 1L), 10L);
            given(loadBlogContentPort.findPublishedByTypeAndSlug(BlogContentType.ENGINEERING, "slug"))
                .willReturn(Optional.of(target));
            given(loadBlogCommentPort.listTopLevel(any(), any(), any(), any(Integer.class))).willReturn(List.of());
            BlogCommentCursorInfo emptyPage = service.getComments(query);
            assertThat(emptyPage.content()).isEmpty();
            verify(loadBlogCommentPort).listTopLevel(10L, com.umc.product.blog.domain.BlogCommentSort.CREATED_AT_DESC,
                null, 21);
        }
    }

    private static BlogContent identified(BlogContent target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        return target;
    }

    private static BlogSeries identified(BlogSeries target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        return target;
    }
}
