package com.umc.product.blog.application.service.evaluator;

import static com.umc.product.support.fixture.BlogUnitFixture.comment;
import static com.umc.product.support.fixture.BlogUnitFixture.content;
import static com.umc.product.support.fixture.BlogUnitFixture.series;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.blog.application.port.out.LoadBlogCommentPort;
import com.umc.product.blog.application.port.out.LoadBlogContentPort;
import com.umc.product.blog.application.port.out.LoadBlogSeriesPort;
import com.umc.product.blog.domain.BlogComment;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogSeries;

@ExtendWith(MockitoExtension.class)
@DisplayName("Blog 리소스 권한 평가 테스트")
class BlogPermissionEvaluatorTest {

    @Mock
    LoadBlogContentPort loadBlogContentPort;

    @Mock
    LoadBlogSeriesPort loadBlogSeriesPort;

    @Mock
    LoadBlogCommentPort loadBlogCommentPort;

    BlogContentPermissionEvaluator contentEvaluator;
    BlogSeriesPermissionEvaluator seriesEvaluator;
    BlogCommentPermissionEvaluator commentEvaluator;

    @BeforeEach
    void setUp() {
        contentEvaluator = new BlogContentPermissionEvaluator(loadBlogContentPort);
        seriesEvaluator = new BlogSeriesPermissionEvaluator(loadBlogSeriesPort);
        commentEvaluator = new BlogCommentPermissionEvaluator(loadBlogCommentPort);
    }

    @Test
    @DisplayName("각 evaluator는 자신의 blog resource type만 지원한다")
    void 각_evaluator는_자신의_resource_type을_지원한다() {
        assertThat(contentEvaluator.supportedResourceType()).isEqualTo(ResourceType.BLOG_CONTENT);
        assertThat(seriesEvaluator.supportedResourceType()).isEqualTo(ResourceType.BLOG_SERIES);
        assertThat(commentEvaluator.supportedResourceType()).isEqualTo(ResourceType.BLOG_COMMENT);
    }

    @Test
    @DisplayName("콘텐츠와 시리즈 생성은 SUPER_ADMIN만 허용하고 ID가 없는 나머지 요청은 거부한다")
    void 생성은_SUPER_ADMIN만_허용한다() {
        assertThat(contentEvaluator.evaluate(subject(1L), typePermission(ResourceType.BLOG_CONTENT, PermissionType.WRITE)))
            .isFalse();
        assertThat(contentEvaluator.evaluate(admin(1L), typePermission(ResourceType.BLOG_CONTENT, PermissionType.WRITE)))
            .isTrue();
        assertThat(contentEvaluator.evaluate(subject(1L), typePermission(ResourceType.BLOG_CONTENT, PermissionType.READ)))
            .isFalse();
        assertThat(seriesEvaluator.evaluate(admin(1L), typePermission(ResourceType.BLOG_SERIES, PermissionType.WRITE)))
            .isTrue();
        assertThat(seriesEvaluator.evaluate(subject(1L), typePermission(ResourceType.BLOG_SERIES, PermissionType.READ)))
            .isFalse();
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 콘텐츠와 시리즈는 fail-closed로 거부한다")
    void 존재하지_않거나_삭제된_리소스는_거부한다() {
        given(loadBlogContentPort.findContentById(10L)).willReturn(Optional.empty());
        given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.empty());
        assertThat(contentEvaluator.evaluate(admin(1L), permission(ResourceType.BLOG_CONTENT, PermissionType.READ)))
            .isFalse();
        assertThat(seriesEvaluator.evaluate(admin(1L), permission(ResourceType.BLOG_SERIES, PermissionType.READ)))
            .isFalse();

        BlogContent deletedContent = content();
        deletedContent.softDelete(1L);
        BlogSeries deletedSeries = series();
        deletedSeries.softDelete(1L);
        given(loadBlogContentPort.findContentById(10L)).willReturn(Optional.of(deletedContent));
        given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.of(deletedSeries));
        assertThat(contentEvaluator.evaluate(admin(1L), permission(ResourceType.BLOG_CONTENT, PermissionType.DELETE)))
            .isFalse();
        assertThat(seriesEvaluator.evaluate(admin(1L), permission(ResourceType.BLOG_SERIES, PermissionType.DELETE)))
            .isFalse();
    }

    @Test
    @DisplayName("콘텐츠와 시리즈의 READ DELETE는 작성자 또는 관리자, EDIT는 작성자만 허용한다")
    void 콘텐츠와_시리즈는_작성자_권한과_관리자_호환성을_보장한다() {
        BlogContent blogContent = content();
        BlogSeries blogSeries = series();
        given(loadBlogContentPort.findContentById(10L)).willReturn(Optional.of(blogContent));
        given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.of(blogSeries));

        assertThat(contentEvaluator.evaluate(subject(1L), permission(ResourceType.BLOG_CONTENT, PermissionType.READ)))
            .isTrue();
        assertThat(contentEvaluator.evaluate(admin(2L), permission(ResourceType.BLOG_CONTENT, PermissionType.READ)))
            .isTrue();
        assertThat(contentEvaluator.evaluate(subject(2L), permission(ResourceType.BLOG_CONTENT, PermissionType.EDIT)))
            .isFalse();
        assertThat(contentEvaluator.evaluate(admin(2L), permission(ResourceType.BLOG_CONTENT, PermissionType.DELETE)))
            .isTrue();
        assertThat(seriesEvaluator.evaluate(subject(1L), permission(ResourceType.BLOG_SERIES, PermissionType.EDIT)))
            .isTrue();
        assertThat(seriesEvaluator.evaluate(subject(1L), permission(ResourceType.BLOG_SERIES, PermissionType.READ)))
            .isTrue();
        assertThat(seriesEvaluator.evaluate(admin(2L), permission(ResourceType.BLOG_SERIES, PermissionType.EDIT)))
            .isFalse();
        assertThat(seriesEvaluator.evaluate(admin(2L), permission(ResourceType.BLOG_SERIES, PermissionType.DELETE)))
            .isTrue();
    }

    @Test
    @DisplayName("댓글은 작성자만 수정하고 작성자 또는 관리자만 삭제한다")
    void 댓글은_작성자와_관리자_규칙을_따른다() {
        BlogComment blogComment = comment();
        given(loadBlogCommentPort.findById(10L)).willReturn(Optional.of(blogComment));

        assertThat(commentEvaluator.evaluate(subject(1L), permission(ResourceType.BLOG_COMMENT, PermissionType.EDIT)))
            .isTrue();
        assertThat(commentEvaluator.evaluate(subject(2L), permission(ResourceType.BLOG_COMMENT, PermissionType.EDIT)))
            .isFalse();
        assertThat(commentEvaluator.evaluate(admin(2L), permission(ResourceType.BLOG_COMMENT, PermissionType.DELETE)))
            .isTrue();

        given(loadBlogCommentPort.findById(10L)).willReturn(Optional.empty());
        assertThat(commentEvaluator.evaluate(admin(2L), permission(ResourceType.BLOG_COMMENT, PermissionType.DELETE)))
            .isFalse();
        assertThat(commentEvaluator.evaluate(subject(1L), typePermission(ResourceType.BLOG_COMMENT, PermissionType.EDIT)))
            .isFalse();
    }

    @Test
    @DisplayName("지원하지 않는 permission은 모든 evaluator에서 fail-closed로 거부한다")
    void 지원하지_않는_permission은_거부한다() {
        given(loadBlogContentPort.findContentById(10L)).willReturn(Optional.of(content()));
        given(loadBlogSeriesPort.findSeriesById(10L)).willReturn(Optional.of(series()));
        given(loadBlogCommentPort.findById(10L)).willReturn(Optional.of(comment()));

        assertThat(contentEvaluator.evaluate(subject(1L), unsupported())).isFalse();
        assertThat(seriesEvaluator.evaluate(subject(1L), unsupported())).isFalse();
        assertThat(commentEvaluator.evaluate(subject(1L), unsupported())).isFalse();
    }

    private static SubjectAttributes subject(Long memberId) {
        return SubjectAttributes.builder().memberId(memberId).build();
    }

    private static SubjectAttributes admin(Long memberId) {
        return SubjectAttributes.builder().memberId(memberId).systemRoles(Set.of(SystemRoleType.SUPER_ADMIN)).build();
    }

    private static ResourcePermission permission(ResourceType type, PermissionType permission) {
        return ResourcePermission.of(type, 10L, permission);
    }

    private static ResourcePermission typePermission(ResourceType type, PermissionType permission) {
        ResourcePermission resourcePermission = mock(ResourcePermission.class);
        given(resourcePermission.permission()).willReturn(permission);
        given(resourcePermission.getResourceIdAsLong()).willReturn(null);
        return resourcePermission;
    }

    private static ResourcePermission unsupported() {
        ResourcePermission resourcePermission = mock(ResourcePermission.class);
        given(resourcePermission.permission()).willReturn(PermissionType.APPROVE);
        given(resourcePermission.getResourceIdAsLong()).willReturn(10L);
        return resourcePermission;
    }
}
