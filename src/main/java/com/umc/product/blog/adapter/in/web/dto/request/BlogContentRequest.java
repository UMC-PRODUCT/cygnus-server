package com.umc.product.blog.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.blog.application.port.in.command.dto.CreateBlogContentCommand;
import com.umc.product.blog.application.port.in.command.dto.UpdateBlogContentCommand;
import com.umc.product.blog.domain.BlogContentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BlogContentRequest(
    @NotBlank(message = "카테고리를 선택해주세요.") String type,

    @NotBlank(message = "주소를 입력해주세요.") @Size(max = 200, message = "주소는 200자 이하로 입력해주세요.") @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "주소는 영문 소문자, 숫자, 하이픈만 사용할 수 있어요.")
    String slug,

    @NotBlank(message = "제목을 입력해주세요.") @Size(max = 200, message = "제목은 200자 이하로 입력해주세요.") String title,

    @Size(max = 500, message = "요약은 500자 이하로 입력해주세요.") String summary,

    @Size(max = 1000, message = "썸네일 URL은 1,000자 이하로 입력해주세요.") String thumbnailUrl,

    @NotBlank(message = "본문을 입력해주세요.") @Size(max = 100000, message = "본문은 100,000자 이하로 입력해주세요.") String content,

    BlogContentStatus status,

    @Size(max = 200, message = "SEO 제목은 200자 이하로 입력해주세요.") String seoTitle,

    @Size(max = 500, message = "SEO 설명은 500자 이하로 입력해주세요.") String seoDescription,

    @Size(max = 1000, message = "OG 이미지 URL은 1,000자 이하로 입력해주세요.") String ogImageUrl,

    List<String> hashtags
) {
    public BlogContentRequest {
        type = normalize(type);
        slug = normalize(slug);
        title = normalize(title);
        summary = normalize(summary);
        thumbnailUrl = normalize(thumbnailUrl);
        content = normalize(content);
        seoTitle = normalize(seoTitle);
        seoDescription = normalize(seoDescription);
        ogImageUrl = normalize(ogImageUrl);
        hashtags = hashtags == null
            ? List.of()
            : hashtags.stream().map(BlogContentRequest::normalize).filter(value -> value != null).toList();
    }

    public CreateBlogContentCommand toCreateCommand(Long authorMemberId) {
        return CreateBlogContentCommand.of(type, slug, title, summary, thumbnailUrl, content, status, authorMemberId,
            seoTitle, seoDescription, ogImageUrl, hashtags);
    }

    public UpdateBlogContentCommand toUpdateCommand(Long contentId) {
        return UpdateBlogContentCommand.of(contentId, slug, title, summary, thumbnailUrl, content, status, seoTitle,
            seoDescription, ogImageUrl, hashtags);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
