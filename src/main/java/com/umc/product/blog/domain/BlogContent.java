package com.umc.product.blog.domain;

import java.time.Instant;
import java.util.regex.Pattern;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "blog_content",
    uniqueConstraints = @UniqueConstraint(name = "uk_blog_content_type_slug", columnNames = {"content_type", "slug"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlogContent extends BaseEntity {

    private static final int MAX_SLUG_LENGTH = 200;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SUMMARY_LENGTH = 500;
    private static final int MAX_URL_LENGTH = 1000;
    private static final int MAX_CONTENT_LENGTH = 100_000;
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private BlogContentType contentType;

    @Column(nullable = false, length = 200)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlogContentStatus status;

    @Column(name = "author_member_id", nullable = false)
    private Long authorMemberId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by_member_id")
    private Long deletedByMemberId;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @Column(name = "og_image_url", length = 1000)
    private String ogImageUrl;

    private BlogContent(
        BlogContentType contentType,
        String slug,
        String title,
        String summary,
        String thumbnailUrl,
        String content,
        BlogContentStatus status,
        Long authorMemberId,
        String seoTitle,
        String seoDescription,
        String ogImageUrl
    ) {
        this.contentType = contentType;
        this.slug = slug.trim();
        this.title = title.trim();
        this.summary = normalizeOptional(summary);
        this.thumbnailUrl = normalizeOptional(thumbnailUrl);
        this.content = content.trim();
        this.status = status == null ? BlogContentStatus.DRAFT : status;
        this.authorMemberId = authorMemberId;
        this.seoTitle = normalizeOptional(seoTitle);
        this.seoDescription = normalizeOptional(seoDescription);
        this.ogImageUrl = normalizeOptional(ogImageUrl);
        if (this.status.isPublished()) {
            this.publishedAt = Instant.now();
        }
    }

    public static BlogContent create(
        BlogContentType type,
        String slug,
        String title,
        String summary,
        String thumbnailUrl,
        String content,
        BlogContentStatus status,
        Long authorMemberId,
        String seoTitle,
        String seoDescription,
        String ogImageUrl
    ) {
        validate(type, slug, title, summary, thumbnailUrl, content, status, authorMemberId, seoTitle, seoDescription,
            ogImageUrl);
        return new BlogContent(type, slug, title, summary, thumbnailUrl, content, status, authorMemberId, seoTitle,
            seoDescription, ogImageUrl);
    }

    public BlogContentType getType() {
        return contentType;
    }

    public void update(
        String slug,
        String title,
        String summary,
        String thumbnailUrl,
        String content,
        BlogContentStatus status,
        String seoTitle,
        String seoDescription,
        String ogImageUrl
    ) {
        ensureNotDeleted();
        validate(contentType, slug, title, summary, thumbnailUrl, content, status, authorMemberId, seoTitle,
            seoDescription, ogImageUrl);
        this.slug = slug.trim();
        this.title = title.trim();
        this.summary = normalizeOptional(summary);
        this.thumbnailUrl = normalizeOptional(thumbnailUrl);
        this.content = content.trim();
        this.seoTitle = normalizeOptional(seoTitle);
        this.seoDescription = normalizeOptional(seoDescription);
        this.ogImageUrl = normalizeOptional(ogImageUrl);
        if (status != null) {
            changeStatus(status);
        }
    }

    public void softDelete(Long deletedByMemberId) {
        ensureMemberId(deletedByMemberId);
        if (isDeleted()) {
            return;
        }
        this.status = BlogContentStatus.DELETED;
        this.deletedAt = Instant.now();
        this.deletedByMemberId = deletedByMemberId;
        this.publishedAt = null;
    }

    public boolean isAuthor(Long memberId) {
        return memberId != null && memberId.equals(authorMemberId);
    }

    public boolean isPublished() {
        return status != null && status.isPublished() && deletedAt == null;
    }

    public boolean isDeleted() {
        return status != null && status.isDeleted();
    }

    public void ensurePublished() {
        if (!isPublished()) {
            throw new BlogDomainException(BlogErrorCode.CONTENT_NOT_PUBLISHED);
        }
    }

    public String canonicalPath() {
        return "/" + contentType.pathValue() + "/" + slug;
    }

    public String resolvedSeoTitle() {
        return seoTitle == null ? title : seoTitle;
    }

    public String resolvedSeoDescription() {
        return seoDescription == null ? summary : seoDescription;
    }

    public String resolvedOgImageUrl() {
        return ogImageUrl == null ? thumbnailUrl : ogImageUrl;
    }

    private void changeStatus(BlogContentStatus nextStatus) {
        if (this.status == nextStatus) {
            return;
        }
        this.status = nextStatus;
        this.publishedAt = nextStatus.isPublished() ? Instant.now() : null;
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new BlogDomainException(BlogErrorCode.CONTENT_ALREADY_DELETED);
        }
    }

    private static void validate(
        BlogContentType type,
        String slug,
        String title,
        String summary,
        String thumbnailUrl,
        String content,
        BlogContentStatus status,
        Long authorMemberId,
        String seoTitle,
        String seoDescription,
        String ogImageUrl
    ) {
        if (type == null) {
            throw new BlogDomainException(BlogErrorCode.INVALID_CONTENT_TYPE);
        }
        if (isInvalidSlug(slug)) {
            throw new BlogDomainException(BlogErrorCode.INVALID_SLUG);
        }
        if (title == null || title.isBlank() || title.trim().length() > MAX_TITLE_LENGTH) {
            throw new BlogDomainException(BlogErrorCode.INVALID_CONTENT_TITLE);
        }
        if (summary != null && summary.trim().length() > MAX_SUMMARY_LENGTH) {
            throw new BlogDomainException(BlogErrorCode.INVALID_CONTENT_SUMMARY);
        }
        if (thumbnailUrl != null && thumbnailUrl.trim().length() > MAX_URL_LENGTH) {
            throw new BlogDomainException(BlogErrorCode.INVALID_THUMBNAIL_URL);
        }
        if (content == null || content.isBlank() || content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new BlogDomainException(BlogErrorCode.INVALID_CONTENT_BODY);
        }
        if (status == BlogContentStatus.DELETED) {
            throw new BlogDomainException(BlogErrorCode.INVALID_CONTENT_STATUS);
        }
        ensureMemberId(authorMemberId);
        if (seoTitle != null && seoTitle.trim().length() > MAX_TITLE_LENGTH) {
            throw new BlogDomainException(BlogErrorCode.INVALID_CONTENT_TITLE);
        }
        if (seoDescription != null && seoDescription.trim().length() > MAX_SUMMARY_LENGTH) {
            throw new BlogDomainException(BlogErrorCode.INVALID_CONTENT_SUMMARY);
        }
        if (ogImageUrl != null && ogImageUrl.trim().length() > MAX_URL_LENGTH) {
            throw new BlogDomainException(BlogErrorCode.INVALID_THUMBNAIL_URL);
        }
    }

    private static void ensureMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new BlogDomainException(BlogErrorCode.INVALID_MEMBER_ID);
        }
    }

    private static boolean isInvalidSlug(String slug) {
        if (slug == null) {
            return true;
        }
        String normalized = slug.trim();
        return normalized.isBlank()
            || normalized.length() > MAX_SLUG_LENGTH
            || !SLUG_PATTERN.matcher(normalized).matches();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
