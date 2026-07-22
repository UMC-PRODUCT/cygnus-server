package com.umc.product.community.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.community.application.port.in.command.comment.ToggleCommentLikeUseCase;
import com.umc.product.community.application.port.in.command.comment.dto.CreateCommentCommand;
import com.umc.product.community.application.port.in.command.post.TogglePostLikeUseCase;
import com.umc.product.community.application.port.in.command.post.dto.CreateLightningCommand;
import com.umc.product.community.application.port.in.command.post.dto.UpdateLightningCommand;
import com.umc.product.community.application.port.in.command.post.dto.UpdatePostCommand;
import com.umc.product.community.application.port.in.command.report.dto.ReportCommentCommand;
import com.umc.product.community.application.port.in.command.report.dto.ReportPostCommand;
import com.umc.product.community.application.port.out.comment.LoadCommentPort;
import com.umc.product.community.application.port.out.comment.SaveCommentPort;
import com.umc.product.community.application.port.out.dto.PostWithAuthor;
import com.umc.product.community.application.port.out.post.LoadPostPort;
import com.umc.product.community.application.port.out.post.SavePostPort;
import com.umc.product.community.application.port.out.report.LoadReportPort;
import com.umc.product.community.application.port.out.report.SaveReportPort;
import com.umc.product.community.application.port.out.scrap.LoadScrapPort;
import com.umc.product.community.application.port.out.scrap.SaveScrapPort;
import com.umc.product.community.application.service.AuthorInfoProvider;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.Report;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.community.domain.enums.ReportTargetType;
import com.umc.product.community.domain.exception.CommunityDomainException;

@ExtendWith(MockitoExtension.class)
class CommunityCommandServiceResidualTest {

    private static final Instant FUTURE = Instant.parse("2099-07-01T00:00:00Z");

    @Mock
    LoadPostPort loadPostPort;

    @Mock
    SavePostPort savePostPort;

    @Mock
    LoadCommentPort loadCommentPort;

    @Mock
    SaveCommentPort saveCommentPort;

    @Mock
    LoadScrapPort loadScrapPort;

    @Mock
    SaveScrapPort saveScrapPort;

    @Mock
    LoadReportPort loadReportPort;

    @Mock
    SaveReportPort saveReportPort;

    @Mock
    AuthorInfoProvider authorInfoProvider;

    @Test
    @DisplayName("번개 게시글을 생성하고 작성자 정보를 결합한다")
    void 번개_게시글을_생성하고_작성자_정보를_결합한다() {
        CreateLightningCommand command = new CreateLightningCommand(
            "번개",
            "내용",
            FUTURE,
            "강남",
            5,
            "https://chat",
            1L
        );
        given(savePostPort.save(org.mockito.ArgumentMatchers.any(Post.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(authorInfoProvider.getAuthorName(1L)).willReturn("작성자");

        var result = postService().createLightningPost(command);

        assertThat(result.category()).isEqualTo(Category.LIGHTNING);
        assertThat(result.location()).isEqualTo("강남");
        assertThat(result.authorName()).isEqualTo("작성자");
    }

    @Test
    @DisplayName("일반 게시글을 수정하고 기존 작성자 정보를 유지한다")
    void 일반_게시글을_수정하고_기존_작성자_정보를_유지한다() {
        Post post = post(Category.FREE);
        UpdatePostCommand command = new UpdatePostCommand(10L, "수정", "수정 내용", Category.INFORMATION);
        given(loadPostPort.findByIdWithAuthor(10L)).willReturn(Optional.of(new PostWithAuthor(post, 1L)));
        given(savePostPort.save(post)).willReturn(post);
        given(authorInfoProvider.getAuthorName(1L)).willReturn("작성자");

        var result = postService().updatePost(command);

        assertThat(result.title()).isEqualTo("수정");
        assertThat(result.category()).isEqualTo(Category.INFORMATION);
    }

    @Test
    @DisplayName("없는 일반 게시글 수정은 not-found로 실패한다")
    void 없는_일반_게시글_수정은_not_found로_실패한다() {
        given(loadPostPort.findByIdWithAuthor(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService().updatePost(
            new UpdatePostCommand(10L, "수정", "내용", Category.FREE)
        )).isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("번개 게시글을 수정하고 새 번개 정보를 반환한다")
    void 번개_게시글을_수정하고_새_번개_정보를_반환한다() {
        Post post = lightning();
        UpdateLightningCommand command = new UpdateLightningCommand(
            10L,
            "수정 번개",
            "수정 내용",
            FUTURE.plusSeconds(1),
            "잠실",
            6,
            "http://chat"
        );
        given(loadPostPort.findByIdWithAuthor(10L)).willReturn(Optional.of(new PostWithAuthor(post, 1L)));
        given(savePostPort.save(post)).willReturn(post);
        given(authorInfoProvider.getAuthorName(1L)).willReturn("작성자");

        var result = postService().updateLightning(command);

        assertThat(result.title()).isEqualTo("수정 번개");
        assertThat(result.location()).isEqualTo("잠실");
        assertThat(result.maxParticipants()).isEqualTo(6);
    }

    @Test
    @DisplayName("없는 번개 게시글 수정은 not-found로 실패한다")
    void 없는_번개_게시글_수정은_not_found로_실패한다() {
        given(loadPostPort.findByIdWithAuthor(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService().updateLightning(new UpdateLightningCommand(
            10L,
            "수정",
            "내용",
            FUTURE,
            "강남",
            5,
            "https://chat"
        ))).isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("게시글 좋아요는 존재 확인 후 port 결과를 반환한다")
    void 게시글_좋아요는_존재_확인_후_port_결과를_반환한다() {
        Post post = post(Category.FREE);
        TogglePostLikeUseCase.LikeResult expected = new TogglePostLikeUseCase.LikeResult(true, 3);
        given(loadPostPort.findById(10L)).willReturn(Optional.of(post));
        given(savePostPort.toggleLike(10L, 1L)).willReturn(expected);

        assertThat(postService().toggleLike(10L, 1L)).isSameAs(expected);
    }

    @Test
    @DisplayName("없는 게시글은 좋아요를 변경하지 않는다")
    void 없는_게시글은_좋아요를_변경하지_않는다() {
        given(loadPostPort.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService().toggleLike(10L, 1L))
            .isInstanceOf(CommunityDomainException.class);
        verify(savePostPort, never()).toggleLike(10L, 1L);
    }

    @Test
    @DisplayName("댓글을 생성하고 작성자 이름을 결합한다")
    void 댓글을_생성하고_작성자_이름을_결합한다() {
        Post post = post(Category.FREE);
        given(loadPostPort.findById(10L)).willReturn(Optional.of(post));
        given(saveCommentPort.save(org.mockito.ArgumentMatchers.any(Comment.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(authorInfoProvider.getAuthorName(2L)).willReturn("댓글 작성자");

        var result = commentService().create(new CreateCommentCommand(10L, 2L, "댓글", null));

        assertThat(result.content()).isEqualTo("댓글");
        assertThat(result.challengerName()).isEqualTo("댓글 작성자");
    }

    @Test
    @DisplayName("없는 게시글에는 댓글을 생성하지 않는다")
    void 없는_게시글에는_댓글을_생성하지_않는다() {
        given(loadPostPort.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService().create(new CreateCommentCommand(10L, 2L, "댓글", null)))
            .isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("댓글은 작성자만 삭제할 수 있다")
    void 댓글은_작성자만_삭제할_수_있다() {
        Comment comment = comment(2L);
        given(loadCommentPort.findById(20L)).willReturn(Optional.of(comment));

        commentService().delete(20L, 2L);

        verify(saveCommentPort).delete(comment);
    }

    @Test
    @DisplayName("없는 댓글과 다른 작성자의 댓글 삭제를 거부한다")
    void 없는_댓글과_다른_작성자의_댓글_삭제를_거부한다() {
        given(loadCommentPort.findById(20L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> commentService().delete(20L, 2L))
            .isInstanceOf(CommunityDomainException.class);

        Comment comment = comment(2L);
        given(loadCommentPort.findById(21L)).willReturn(Optional.of(comment));
        assertThatThrownBy(() -> commentService().delete(21L, 3L))
            .isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("댓글 좋아요는 존재 확인 후 port 결과를 반환한다")
    void 댓글_좋아요는_존재_확인_후_port_결과를_반환한다() {
        Comment comment = comment(2L);
        ToggleCommentLikeUseCase.LikeResult expected = new ToggleCommentLikeUseCase.LikeResult(true, 2);
        given(loadCommentPort.findById(20L)).willReturn(Optional.of(comment));
        given(saveCommentPort.toggleLike(20L, 3L)).willReturn(expected);

        assertThat(commentService().toggle(20L, 3L)).isSameAs(expected);
    }

    @Test
    @DisplayName("없는 게시글의 스크랩 toggle을 거부한다")
    void 없는_게시글의_스크랩_toggle을_거부한다() {
        given(loadPostPort.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> scrapService().toggleScrap(10L, 1L))
            .isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("게시글 스크랩 toggle 결과와 최신 개수를 반환한다")
    void 게시글_스크랩_toggle_결과와_최신_개수를_반환한다() {
        given(loadPostPort.findById(10L)).willReturn(Optional.of(post(Category.FREE)));
        given(saveScrapPort.toggleScrap(10L, 1L)).willReturn(true);
        given(loadScrapPort.countByPostId(10L)).willReturn(4);

        var result = scrapService().toggleScrap(10L, 1L);

        assertThat(result.scrapped()).isTrue();
        assertThat(result.scrapCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("게시글과 댓글 신고를 생성한다")
    void 게시글과_댓글_신고를_생성한다() {
        given(loadPostPort.findById(10L)).willReturn(Optional.of(post(Category.FREE)));
        given(loadCommentPort.findById(20L)).willReturn(Optional.of(comment(2L)));

        reportService().report(new ReportPostCommand(10L, 1L));
        reportService().report(new ReportCommentCommand(20L, 1L));

        verify(saveReportPort).save(org.mockito.ArgumentMatchers.argThat(
            report -> report.getTargetType() == ReportTargetType.POST && report.getTargetId().equals(10L)
        ));
        verify(saveReportPort).save(org.mockito.ArgumentMatchers.argThat(
            report -> report.getTargetType() == ReportTargetType.COMMENT && report.getTargetId().equals(20L)
        ));
    }

    @Test
    @DisplayName("없는 신고 대상과 중복 신고를 거부한다")
    void 없는_신고_대상과_중복_신고를_거부한다() {
        given(loadPostPort.findById(10L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> reportService().report(new ReportPostCommand(10L, 1L)))
            .isInstanceOf(CommunityDomainException.class);

        given(loadCommentPort.findById(20L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> reportService().report(new ReportCommentCommand(20L, 1L)))
            .isInstanceOf(CommunityDomainException.class);

        given(loadPostPort.findById(11L)).willReturn(Optional.of(post(Category.FREE)));
        given(loadReportPort.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.POST, 11L))
            .willReturn(true);
        assertThatThrownBy(() -> reportService().report(new ReportPostCommand(11L, 1L)))
            .isInstanceOf(CommunityDomainException.class);
        verify(saveReportPort, never()).save(org.mockito.ArgumentMatchers.any(Report.class));
    }

    private PostCommandService postService() {
        return new PostCommandService(
            loadPostPort,
            savePostPort,
            saveCommentPort,
            saveScrapPort,
            authorInfoProvider
        );
    }

    private CommentCommandService commentService() {
        return new CommentCommandService(loadPostPort, loadCommentPort, saveCommentPort, authorInfoProvider);
    }

    private ScrapCommandService scrapService() {
        return new ScrapCommandService(loadPostPort, loadScrapPort, saveScrapPort);
    }

    private ReportCommandService reportService() {
        return new ReportCommandService(loadPostPort, loadCommentPort, loadReportPort, saveReportPort);
    }

    private Post post(Category category) {
        Post post = Post.createPost("제목", "내용", category, 1L);
        ReflectionTestUtils.setField(post, "id", 10L);
        return post;
    }

    private Post lightning() {
        Post post = Post.createLightning(
            "번개",
            "내용",
            new Post.LightningInfo(FUTURE, "강남", 5, "https://chat"),
            1L
        );
        ReflectionTestUtils.setField(post, "id", 10L);
        return post;
    }

    private Comment comment(Long challengerId) {
        Comment comment = Comment.create(post(Category.FREE), challengerId, "댓글", null);
        ReflectionTestUtils.setField(comment, "id", 20L);
        return comment;
    }
}
