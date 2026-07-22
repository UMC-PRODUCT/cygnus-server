package com.umc.product.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.adapter.in.web.dto.response.PostDetailResponse;
import com.umc.product.community.adapter.in.web.dto.response.PostResponse;
import com.umc.product.community.adapter.in.web.dto.response.PostSearchResponse;
import com.umc.product.community.application.port.in.command.comment.dto.CreateCommentCommand;
import com.umc.product.community.application.port.in.command.report.dto.ReportCommentCommand;
import com.umc.product.community.application.port.in.command.report.dto.ReportPostCommand;
import com.umc.product.community.application.port.in.query.dto.CommentInfo;
import com.umc.product.community.application.port.in.query.dto.PostDetailInfo;
import com.umc.product.community.application.port.in.query.dto.PostInfo;
import com.umc.product.community.application.port.in.query.dto.PostSearchResult;
import com.umc.product.community.application.port.in.query.dto.PostSearchResult.MatchType;
import com.umc.product.community.application.port.out.dto.PostSearchData;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

class CommunityDtoResidualTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    @DisplayName("PostInfo는 일반글 작성자 정보와 null 작성자 정보를 모두 변환한다")
    void PostInfo는_일반글_작성자_정보와_null_작성자_정보를_모두_변환한다() {
        Post post = post(Category.FREE);

        PostInfo populated = PostInfo.from(post, member(), challenger());
        PostInfo missing = PostInfo.from(post, null, null, true);

        assertThat(populated.authorMemberId()).isEqualTo(100L);
        assertThat(populated.authorNickname()).isEqualTo("닉네임");
        assertThat(populated.authorPart()).isEqualTo(ChallengerPart.WEB);
        assertThat(missing.authorMemberId()).isNull();
        assertThat(missing.authorName()).isNull();
        assertThat(missing.authorNickname()).isNull();
        assertThat(missing.authorProfileImage()).isNull();
        assertThat(missing.authorPart()).isNull();
        assertThat(missing.isLiked()).isTrue();
    }

    @Test
    @DisplayName("PostInfo의 현재·legacy 변환은 번개 정보를 보존한다")
    void PostInfo의_현재_legacy_변환은_번개_정보를_보존한다() {
        Post post = lightning();

        PostInfo current = PostInfo.from(post, member(), challenger(), true);
        PostInfo legacySimple = PostInfo.from(post, 1L, "작성자");
        PostInfo legacyProfile = PostInfo.from(post, 1L, "작성자", "profile", ChallengerPart.WEB, 3);
        PostInfo legacyLiked = PostInfo.from(post, 1L, "작성자", "profile", ChallengerPart.WEB, 3, true);

        assertThat(current.location()).isEqualTo("강남");
        assertThat(legacySimple.meetAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(legacyProfile.maxParticipants()).isEqualTo(5);
        assertThat(legacyLiked.isLiked()).isTrue();
    }

    @Test
    @DisplayName("CommentInfo는 현재·legacy 작성자 변환과 null 교차 도메인 정보를 처리한다")
    void CommentInfo는_현재_legacy_작성자_변환과_null_교차_도메인_정보를_처리한다() {
        Comment comment = comment();

        CommentInfo current = CommentInfo.of(comment, member(), challenger());
        CommentInfo missing = CommentInfo.of(comment, null, null);
        CommentInfo nameOnly = CommentInfo.from(comment, "작성자");
        CommentInfo profile = CommentInfo.from(comment, "작성자", "profile");
        CommentInfo part = CommentInfo.from(comment, "작성자", "profile", ChallengerPart.WEB);

        assertThat(current.challengerNickname()).isEqualTo("닉네임");
        assertThat(missing.challengerId()).isNull();
        assertThat(missing.challengerName()).isNull();
        assertThat(missing.challengerNickname()).isNull();
        assertThat(missing.challengerProfileImage()).isNull();
        assertThat(missing.challengerPart()).isNull();
        assertThat(nameOnly.challengerName()).isEqualTo("작성자");
        assertThat(profile.challengerProfileImage()).isEqualTo("profile");
        assertThat(part.challengerPart()).isEqualTo(ChallengerPart.WEB);
    }

    @Test
    @DisplayName("검색 DTO는 relevance와 match type을 보존하고 긴 본문만 100자로 자른다")
    void 검색_DTO는_relevance와_match_type을_보존하고_긴_본문만_100자로_자른다() {
        Post post = post(Category.FREE);
        ReflectionTestUtils.setField(post, "content", "가".repeat(101));

        PostSearchData data = PostSearchData.from(post, MatchType.TITLE_START, 30);
        PostSearchResult longResult = data.toResult();
        PostSearchResult shortResult = PostSearchResult.of(2L, "제목", "짧음", Category.FREE, 0, NOW, MatchType.CONTENT);
        PostSearchResult nullResult = PostSearchResult.of(3L, "제목", null, Category.FREE, 0, NOW, MatchType.TITLE_CONTAIN);

        assertThat(data.relevanceScore()).isEqualTo(30);
        assertThat(longResult.contentPreview()).hasSize(103).endsWith("...");
        assertThat(shortResult.contentPreview()).isEqualTo("짧음");
        assertThat(nullResult.contentPreview()).isNull();
        assertThat(PostSearchResponse.from(longResult).matchType()).isEqualTo(MatchType.TITLE_START);
    }

    @Test
    @DisplayName("PostResponse는 현재 검색·legacy 번개 응답의 작성자와 번개 정보를 변환한다")
    void PostResponse는_현재_검색_legacy_번개_응답의_작성자와_번개_정보를_변환한다() {
        PostInfo normal = PostInfo.from(post(Category.FREE), member(), challenger());
        PostInfo lightning = PostInfo.from(lightning(), member(), challenger(), true);

        PostResponse populated = PostResponse.from(normal, member(), challenger());
        PostResponse missing = PostResponse.from(normal, null, null);
        PostResponse legacy = PostResponse.from(lightning);

        assertThat(populated.authorMemberId()).isEqualTo(100L);
        assertThat(populated.authorId()).isEqualTo(1L);
        assertThat(missing.authorId()).isNull();
        assertThat(missing.authorMemberId()).isNull();
        assertThat(missing.authorName()).isNull();
        assertThat(missing.authorNickname()).isNull();
        assertThat(missing.authorProfileImage()).isNull();
        assertThat(missing.authorPart()).isNull();
        assertThat(legacy.lightningInfo().location()).isEqualTo("강남");
        assertThat(legacy.isLiked()).isTrue();
    }

    @Test
    @DisplayName("PostDetailResponse는 번개 상세 정보와 interaction 집계를 변환한다")
    void PostDetailResponse는_번개_상세_정보와_interaction_집계를_변환한다() {
        PostInfo info = PostInfo.from(lightning(), member(), challenger(), true);
        PostDetailInfo detail = PostDetailInfo.of(info, 3, ChallengerPart.WEB, true, 4);

        PostDetailResponse response = PostDetailResponse.from(detail);

        assertThat(response.lightningInfo().meetAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(response.commentCount()).isEqualTo(3);
        assertThat(response.scrapCount()).isEqualTo(4);
        assertThat(response.isScrapped()).isTrue();
    }

    @Test
    @DisplayName("community command는 null 필수값과 blank 댓글을 거부한다")
    void community_command는_null_필수값과_blank_댓글을_거부한다() {
        assertThat(new ReportPostCommand(1L, 2L).reporterId()).isEqualTo(2L);
        assertThat(new ReportCommentCommand(1L, 2L).commentId()).isEqualTo(1L);
        assertThatThrownBy(() -> new ReportPostCommand(null, 2L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ReportCommentCommand(1L, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateCommentCommand(1L, 2L, " ", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private Post post(Category category) {
        Post post = Post.createPost("제목", "내용", category, 1L);
        setEntityFields(post, 10L);
        return post;
    }

    private Post lightning() {
        Post post = Post.createLightning(
            "번개",
            "내용",
            new Post.LightningInfo(NOW.plusSeconds(3600), "강남", 5, "https://chat"),
            1L
        );
        setEntityFields(post, 11L);
        return post;
    }

    private Comment comment() {
        Comment comment = Comment.create(post(Category.FREE), 1L, "댓글", null);
        setEntityFields(comment, 20L);
        return comment;
    }

    private void setEntityFields(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "createdAt", NOW);
    }

    private MemberInfo member() {
        return MemberInfo.builder()
            .id(100L)
            .name("작성자")
            .nickname("닉네임")
            .profileImageLink("profile")
            .build();
    }

    private ChallengerInfo challenger() {
        return ChallengerInfo.builder()
            .challengerId(1L)
            .memberId(100L)
            .gisuId(10L)
            .part(ChallengerPart.WEB)
            .build();
    }
}
