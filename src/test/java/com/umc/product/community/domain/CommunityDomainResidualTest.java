package com.umc.product.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.community.domain.enums.Category;
import com.umc.product.community.domain.enums.ReportStatus;
import com.umc.product.community.domain.enums.ReportTargetType;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

class CommunityDomainResidualTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    @DisplayName("일반 게시글은 번개 카테고리와 빈 필수값을 거부한다")
    void 일반_게시글은_번개_카테고리와_빈_필수값을_거부한다() {
        assertThatThrownBy(() -> Post.createPost("제목", "내용", Category.LIGHTNING, 1L))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> Post.createPost(" ", "내용", Category.FREE, 1L))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> Post.createPost("제목", null, Category.FREE, 1L))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> Post.createPost("제목", "내용", Category.FREE, null))
            .isInstanceOf(CommunityDomainException.class);
        assertThat(Category.FREE.isLightning()).isFalse();
    }

    @Test
    @DisplayName("번개 게시글은 필수 번개 정보와 게시글 종류 전이를 강제한다")
    void 번개_게시글은_필수_번개_정보와_게시글_종류_전이를_강제한다() {
        Post normal = post();
        Post lightning = lightning();

        assertThatThrownBy(() -> Post.createLightning("제목", "내용", null, 1L))
            .isInstanceOf(CommunityDomainException.class);
        assertThat(normal.getLightningInfo()).isNull();
        assertThatThrownBy(normal::getLightningInfoOrThrow).isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> normal.update("제목", "내용", null))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> normal.update("제목", "내용", Category.LIGHTNING))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> lightning.update("제목", "내용", Category.FREE))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> normal.updateLightning("제목", "내용", lightningInfo()))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> lightning.updateLightning("제목", "내용", null))
            .isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("번개 정보는 시간·장소·인원·URL 불변식과 과거 시간 경계를 검증한다")
    void 번개_정보는_시간_장소_인원_URL_불변식과_과거_시간_경계를_검증한다() {
        assertThatThrownBy(() -> new Post.LightningInfo(null, "강남", 2, "https://open.chat"))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> new Post.LightningInfo(NOW, " ", 2, "https://open.chat"))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> new Post.LightningInfo(NOW, "강남", 0, "https://open.chat"))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> new Post.LightningInfo(NOW, "강남", 2, null))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> new Post.LightningInfo(NOW, "강남", 2, "ftp://open.chat"))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> lightningInfo().validateMeetAtIsFuture(NOW.plusSeconds(2)))
            .isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("댓글 수정과 좋아요 toggle은 빈 내용과 멱등 상태를 처리한다")
    void 댓글_수정과_좋아요_toggle은_빈_내용과_멱등_상태를_처리한다() {
        Comment comment = Comment.create(post(), 2L, "댓글", null);

        assertThatThrownBy(() -> comment.updateContent(" ")).isInstanceOf(CommunityDomainException.class);
        comment.updateContent("수정 댓글");
        assertThat(comment.getContent()).isEqualTo("수정 댓글");
        assertThat(comment.toggleLike(3L)).isTrue();
        assertThat(comment.toggleLike(3L)).isFalse();
        assertThat(comment.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("댓글은 양수 challenger ID와 non-blank 내용을 요구한다")
    void 댓글은_양수_challenger_ID와_non_blank_내용을_요구한다() {
        Post post = post();

        assertThatThrownBy(() -> Comment.create(post, 0L, "댓글", null))
            .isInstanceOf(CommunityDomainException.class);
        assertThatThrownBy(() -> Comment.create(post, 1L, null, null))
            .isInstanceOf(CommunityDomainException.class);
    }

    @Test
    @DisplayName("신고는 PENDING으로 생성되고 승인 또는 거절할 수 있다")
    void 신고는_PENDING으로_생성되고_승인_또는_거절할_수_있다() {
        Report approved = Report.create(1L, ReportTargetType.POST, 10L, "스팸");
        Report rejected = Report.create(2L, ReportTargetType.COMMENT, 20L, null);

        assertThat(approved.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(approved.getReason()).isEqualTo("스팸");
        approved.approve();
        rejected.reject();
        assertThat(approved.getStatus()).isEqualTo(ReportStatus.APPROVED);
        assertThat(rejected.getStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    @DisplayName("스크랩은 양수 challenger ID를 요구한다")
    void 스크랩은_양수_challenger_ID를_요구한다() {
        assertThatThrownBy(() -> Scrap.create(post(), 0L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("community 예외는 custom message를 보존한다")
    void community_예외는_custom_message를_보존한다() {
        assertThat(new CommunityDomainException(CommunityErrorCode.INVALID_POST_TITLE, "custom").getMessage())
            .isEqualTo("custom");
    }

    private Post post() {
        Post post = Post.createPost("제목", "내용", Category.FREE, 1L);
        ReflectionTestUtils.setField(post, "id", 10L);
        return post;
    }

    private Post lightning() {
        Post post = Post.createLightning("번개", "내용", lightningInfo(), 1L);
        ReflectionTestUtils.setField(post, "id", 11L);
        return post;
    }

    private Post.LightningInfo lightningInfo() {
        return new Post.LightningInfo(NOW.plusSeconds(1), "강남", 2, "https://open.chat");
    }
}
