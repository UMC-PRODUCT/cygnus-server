package com.umc.product.blog.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.blog.adapter.in.web.dto.request.BlogContentRequest;
import com.umc.product.blog.adapter.in.web.dto.request.BlogSeriesRequest;
import com.umc.product.blog.application.port.in.command.CreateBlogCommentUseCase;
import com.umc.product.blog.application.port.in.command.CreateBlogContentUseCase;
import com.umc.product.blog.application.port.in.command.CreateBlogSeriesUseCase;
import com.umc.product.blog.application.port.in.command.DeleteBlogCommentUseCase;
import com.umc.product.blog.application.port.in.command.DeleteBlogContentUseCase;
import com.umc.product.blog.application.port.in.command.DeleteBlogSeriesUseCase;
import com.umc.product.blog.application.port.in.command.ReplaceBlogSeriesContentsUseCase;
import com.umc.product.blog.application.port.in.command.ToggleBlogCommentLikeUseCase;
import com.umc.product.blog.application.port.in.command.ToggleBlogContentLikeUseCase;
import com.umc.product.blog.application.port.in.command.UpdateBlogCommentUseCase;
import com.umc.product.blog.application.port.in.command.UpdateBlogContentUseCase;
import com.umc.product.blog.application.port.in.command.UpdateBlogSeriesUseCase;
import com.umc.product.blog.application.port.in.command.dto.DeleteBlogContentCommand;
import com.umc.product.blog.application.port.in.command.dto.DeleteBlogSeriesCommand;
import com.umc.product.blog.application.port.in.query.GetBlogCommentListUseCase;
import com.umc.product.blog.application.port.in.query.GetBlogContentLikeUseCase;
import com.umc.product.blog.application.port.in.query.GetBlogContentUseCase;
import com.umc.product.blog.application.port.in.query.GetBlogSeriesUseCase;
import com.umc.product.blog.application.port.in.query.dto.BlogContentCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogContentInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogContentSummaryInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesCursorInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesInfo;
import com.umc.product.blog.application.port.in.query.dto.BlogSeriesSummaryInfo;
import com.umc.product.blog.domain.BlogContentStatus;
import com.umc.product.blog.domain.BlogDomainException;
import com.umc.product.global.security.MemberPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("Blog controller 단위 테스트")
class BlogControllerUnitTest {

    @Nested
    @DisplayName("콘텐츠 controller")
    class ContentController {

        @Mock
        GetBlogContentUseCase getBlogContentUseCase;

        @Mock
        CreateBlogContentUseCase createBlogContentUseCase;

        @Mock
        UpdateBlogContentUseCase updateBlogContentUseCase;

        @Mock
        DeleteBlogContentUseCase deleteBlogContentUseCase;

        @Test
        @DisplayName("목록은 비회원과 회원 식별자를 구분해 cursor response로 변환한다")
        void 목록은_cursor_response로_변환한다() {
            BlogContentSummaryInfo summary = contentSummaryInfo();
            given(getBlogContentUseCase.getPublicContents(any()))
                .willReturn(new BlogContentCursorInfo(List.of(summary), 10L, true));
            BlogContentController controller = controller();

            var response = controller.getPublicContents(null, null, null, null, 20, null, null);

            assertThat(response.content()).hasSize(1);
            assertThat(response.nextCursor()).isEqualTo(10L);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("preview와 수정은 application info를 응답으로 변환한다")
        void preview와_수정은_info를_응답으로_변환한다() {
            BlogContentInfo info = contentInfo();
            given(getBlogContentUseCase.getPreview(10L, 1L)).willReturn(info);
            given(updateBlogContentUseCase.update(any())).willReturn(info);
            BlogContentController controller = controller();

            assertThat(controller.getPreview(10L, new MemberPrincipal(1L))).isNotNull();
            assertThat(controller.update(10L, contentRequest())).isNotNull();
        }

        @Test
        @DisplayName("삭제는 현재 회원 ID를 command에 담고 비인증 요청은 거부한다")
        void 삭제는_회원_ID를_요구한다() {
            BlogContentController controller = controller();

            controller.delete(10L, new MemberPrincipal(1L));
            verify(deleteBlogContentUseCase).delete(DeleteBlogContentCommand.of(10L, 1L));
            assertThatThrownBy(() -> controller.getPreview(10L, null)).isInstanceOf(BlogDomainException.class);
        }

        private BlogContentController controller() {
            return new BlogContentController(
                getBlogContentUseCase,
                createBlogContentUseCase,
                updateBlogContentUseCase,
                deleteBlogContentUseCase
            );
        }
    }

    @Nested
    @DisplayName("시리즈 controller")
    class SeriesController {

        @Mock
        GetBlogSeriesUseCase getBlogSeriesUseCase;

        @Mock
        CreateBlogSeriesUseCase createBlogSeriesUseCase;

        @Mock
        UpdateBlogSeriesUseCase updateBlogSeriesUseCase;

        @Mock
        DeleteBlogSeriesUseCase deleteBlogSeriesUseCase;

        @Mock
        ReplaceBlogSeriesContentsUseCase replaceBlogSeriesContentsUseCase;

        @Test
        @DisplayName("시리즈 목록과 콘텐츠 목록은 cursor metadata를 보존한다")
        void 목록은_cursor_metadata를_보존한다() {
            BlogSeriesSummaryInfo seriesSummary = seriesSummaryInfo();
            BlogContentSummaryInfo contentSummary = contentSummaryInfo();
            given(getBlogSeriesUseCase.getPublicSeries(any()))
                .willReturn(new BlogSeriesCursorInfo(List.of(seriesSummary), 10L, true));
            given(getBlogSeriesUseCase.getPublicSeriesContents(any(), any(), any(), any(Integer.class), any(), any()))
                .willReturn(new BlogContentCursorInfo(List.of(contentSummary), 20L, true));
            BlogSeriesController controller = controller();

            var seriesResponse = controller.getPublicSeries(null, null, 20, null, null);
            var contentsResponse = controller.getPublicSeriesContents(
                "engineering", "series", null, 20, null, new MemberPrincipal(1L)
            );

            assertThat(seriesResponse.content()).hasSize(1);
            assertThat(seriesResponse.nextCursor()).isEqualTo(10L);
            assertThat(contentsResponse.content()).hasSize(1);
            assertThat(contentsResponse.nextCursor()).isEqualTo(20L);
        }

        @Test
        @DisplayName("공개 상세, preview, 수정은 application info를 응답으로 변환한다")
        void 상세와_수정은_info를_응답으로_변환한다() {
            BlogSeriesInfo info = seriesInfo();
            given(getBlogSeriesUseCase.getPublicSeries("engineering", "series", null)).willReturn(info);
            given(getBlogSeriesUseCase.getPreview(10L, 1L)).willReturn(info);
            given(updateBlogSeriesUseCase.update(any())).willReturn(info);
            BlogSeriesController controller = controller();

            assertThat(controller.getPublicSeries("engineering", "series", null)).isNotNull();
            assertThat(controller.getPreview(10L, new MemberPrincipal(1L))).isNotNull();
            assertThat(controller.update(10L, seriesRequest())).isNotNull();
        }

        @Test
        @DisplayName("삭제는 현재 회원 ID를 command에 담고 비인증 요청은 거부한다")
        void 삭제는_회원_ID를_요구한다() {
            BlogSeriesController controller = controller();

            controller.delete(10L, new MemberPrincipal(1L));
            verify(deleteBlogSeriesUseCase).delete(DeleteBlogSeriesCommand.of(10L, 1L));
            assertThatThrownBy(() -> controller.getPreview(10L, null)).isInstanceOf(BlogDomainException.class);
        }

        private BlogSeriesController controller() {
            return new BlogSeriesController(
                getBlogSeriesUseCase,
                createBlogSeriesUseCase,
                updateBlogSeriesUseCase,
                deleteBlogSeriesUseCase,
                replaceBlogSeriesContentsUseCase
            );
        }
    }

    @Nested
    @DisplayName("상호작용 controller")
    class InteractionController {

        @Mock
        GetBlogContentLikeUseCase getBlogContentLikeUseCase;

        @Mock
        ToggleBlogContentLikeUseCase toggleBlogContentLikeUseCase;

        @Mock
        GetBlogCommentListUseCase getBlogCommentListUseCase;

        @Mock
        CreateBlogCommentUseCase createBlogCommentUseCase;

        @Mock
        UpdateBlogCommentUseCase updateBlogCommentUseCase;

        @Mock
        DeleteBlogCommentUseCase deleteBlogCommentUseCase;

        @Mock
        ToggleBlogCommentLikeUseCase toggleBlogCommentLikeUseCase;

        @Test
        @DisplayName("댓글 좋아요 토글은 인증 회원을 요구한다")
        void 댓글_좋아요는_인증_회원을_요구한다() {
            BlogInteractionController controller = new BlogInteractionController(
                getBlogContentLikeUseCase,
                toggleBlogContentLikeUseCase,
                getBlogCommentListUseCase,
                createBlogCommentUseCase,
                updateBlogCommentUseCase,
                deleteBlogCommentUseCase,
                toggleBlogCommentLikeUseCase
            );

            assertThatThrownBy(() -> controller.toggleCommentLike("engineering", "slug", 10L, null))
                .isInstanceOf(BlogDomainException.class);
        }
    }

    private static BlogContentRequest contentRequest() {
        return new BlogContentRequest(
            "engineering", "content", "제목", null, null, "본문", BlogContentStatus.DRAFT,
            null, null, null, List.of()
        );
    }

    private static BlogSeriesRequest seriesRequest() {
        return new BlogSeriesRequest("engineering", "series", "제목", null, null, null, null, null);
    }

    private static BlogContentInfo contentInfo() {
        BlogContentInfo info = org.mockito.Mockito.mock(BlogContentInfo.class);
        given(info.series()).willReturn(List.of());
        given(info.hashtags()).willReturn(List.of());
        return info;
    }

    private static BlogContentSummaryInfo contentSummaryInfo() {
        BlogContentSummaryInfo info = org.mockito.Mockito.mock(BlogContentSummaryInfo.class);
        given(info.series()).willReturn(List.of());
        given(info.hashtags()).willReturn(List.of());
        return info;
    }

    private static BlogSeriesInfo seriesInfo() {
        return org.mockito.Mockito.mock(BlogSeriesInfo.class);
    }

    private static BlogSeriesSummaryInfo seriesSummaryInfo() {
        return org.mockito.Mockito.mock(BlogSeriesSummaryInfo.class);
    }
}
