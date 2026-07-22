package com.umc.product.blog.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.blog.adapter.in.web.dto.request.BlogContentRequest;
import com.umc.product.blog.adapter.in.web.dto.request.BlogSeriesRequest;
import com.umc.product.blog.adapter.in.web.dto.request.UpdateBlogCommentRequest;
import com.umc.product.blog.adapter.in.web.dto.response.BlogSeriesSummaryResponse;
import com.umc.product.blog.application.port.in.command.dto.DeleteBlogContentCommand;
import com.umc.product.blog.application.port.in.command.dto.DeleteBlogSeriesCommand;
import com.umc.product.blog.application.port.in.command.dto.UpdateBlogContentCommand;
import com.umc.product.blog.application.port.in.command.dto.UpdateBlogSeriesCommand;
import com.umc.product.blog.application.port.in.query.dto.BlogAuthorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesListQuery;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesSummaryInfo;
import com.umc.product.blog.domain.BlogContentStatus;
import com.umc.product.blog.domain.BlogContentType;

@DisplayName("Blog 잔여 DTO 테스트")
class BlogDtoResidualTest {

    @Test
    @DisplayName("시리즈 요약 응답은 작성자를 포함한 모든 조회 값을 보존한다")
    void 시리즈_요약_응답은_모든_조회_값을_보존한다() {
        Instant updatedAt = Instant.parse("2026-07-22T00:00:00Z");
        BlogAuthorInfo author = new BlogAuthorInfo(1L, "홍길동", "길동", "profile");
        BlogSeriesSummaryInfo info = new BlogSeriesSummaryInfo(
            2L,
            BlogContentType.ENGINEERING,
            "series",
            "제목",
            "설명",
            "thumbnail",
            author,
            3,
            updatedAt,
            "/series/engineering/series",
            "seo title",
            "seo description",
            "og-image",
            true,
            false
        );

        BlogSeriesSummaryResponse response = BlogSeriesSummaryResponse.from(info);
        BlogSeriesCursorInfo cursor = new BlogSeriesCursorInfo(List.of(info), 2L, true);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.type()).isEqualTo(BlogContentType.ENGINEERING);
        assertThat(response.slug()).isEqualTo("series");
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.description()).isEqualTo("설명");
        assertThat(response.thumbnailUrl()).isEqualTo("thumbnail");
        assertThat(response.author().id()).isEqualTo(1L);
        assertThat(response.contentCount()).isEqualTo(3);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        assertThat(response.canonicalPath()).isEqualTo("/series/engineering/series");
        assertThat(response.seoTitle()).isEqualTo("seo title");
        assertThat(response.seoDescription()).isEqualTo("seo description");
        assertThat(response.ogImageUrl()).isEqualTo("og-image");
        assertThat(response.canEdit()).isTrue();
        assertThat(response.canDelete()).isFalse();
        assertThat(cursor.content()).containsExactly(info);
        assertThat(cursor.nextCursor()).isEqualTo(2L);
        assertThat(cursor.hasNext()).isTrue();
    }

    @Test
    @DisplayName("목록과 삭제 command factory는 식별자와 조회 조건을 그대로 전달한다")
    void 목록과_삭제_command_factory는_값을_그대로_전달한다() {
        BlogSeriesListQuery query = BlogSeriesListQuery.of("engineering", 10L, 20, "createdAt,asc", 1L);

        assertThat(query).isEqualTo(new BlogSeriesListQuery("engineering", 10L, 20, "createdAt,asc", 1L));
        assertThat(DeleteBlogContentCommand.of(2L, 1L)).isEqualTo(new DeleteBlogContentCommand(2L, 1L));
        assertThat(DeleteBlogSeriesCommand.of(3L, 1L)).isEqualTo(new DeleteBlogSeriesCommand(3L, 1L));
    }

    @Test
    @DisplayName("콘텐츠 수정 command는 hashtag null을 빈 불변 목록으로 정규화한다")
    void 콘텐츠_수정_command는_hashtag를_안전하게_복사한다() {
        UpdateBlogContentCommand empty = updateContentCommand(null);
        ArrayList<String> mutable = new ArrayList<>(List.of("spring"));
        UpdateBlogContentCommand copied = updateContentCommand(mutable);
        mutable.add("java");

        assertThat(empty.hashtags()).isEmpty();
        assertThat(copied.hashtags()).containsExactly("spring");
    }

    @Test
    @DisplayName("웹 요청은 공백을 정규화하고 create 및 update command로 변환한다")
    void 웹_요청은_공백을_정규화하고_command로_변환한다() {
        BlogSeriesRequest seriesRequest = new BlogSeriesRequest(
            " engineering ", " series ", " 제목 ", " ", null, " seo ", " ", " og "
        );
        BlogContentRequest contentRequest = new BlogContentRequest(
            " engineering ", " content ", " 제목 ", " ", null, " 본문 ", BlogContentStatus.DRAFT,
            " seo ", " ", " og ", List.of(" spring ", " ")
        );
        UpdateBlogCommentRequest nullComment = new UpdateBlogCommentRequest(null);
        UpdateBlogCommentRequest blankComment = new UpdateBlogCommentRequest("   ");

        assertThat(seriesRequest.description()).isNull();
        assertThat(seriesRequest.toCreateCommand(1L).type()).isEqualTo("engineering");
        UpdateBlogSeriesCommand seriesCommand = seriesRequest.toUpdateCommand(2L);
        assertThat(seriesCommand).isEqualTo(UpdateBlogSeriesCommand.of(
            2L, "series", "제목", null, null, "seo", null, "og"
        ));
        assertThat(contentRequest.summary()).isNull();
        assertThat(contentRequest.toCreateCommand(1L).hashtags()).containsExactly("spring");
        assertThat(contentRequest.toUpdateCommand(2L).content()).isEqualTo("본문");
        assertThat(nullComment.content()).isNull();
        assertThat(blankComment.content()).isNull();
        assertThat(blankComment.toCommand("engineering", "slug", 3L, 1L).content()).isNull();

        BlogContentRequest nullHashtags = new BlogContentRequest(
            "engineering", "content", "제목", null, null, "본문", BlogContentStatus.DRAFT,
            null, null, null, null
        );
        assertThat(nullHashtags.hashtags()).isEmpty();
    }

    private static UpdateBlogContentCommand updateContentCommand(List<String> hashtags) {
        return UpdateBlogContentCommand.of(
            1L,
            "slug",
            "제목",
            "요약",
            "thumbnail",
            "본문",
            BlogContentStatus.DRAFT,
            "seo title",
            "seo description",
            "og image",
            hashtags
        );
    }
}
