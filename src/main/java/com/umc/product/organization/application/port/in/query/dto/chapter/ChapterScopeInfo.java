package com.umc.product.organization.application.port.in.query.dto.chapter;

import java.util.Objects;

import com.umc.product.organization.domain.Chapter;

public record ChapterScopeInfo(Long chapterId, Long gisuId) {

    public ChapterScopeInfo {
        Objects.requireNonNull(chapterId);
        Objects.requireNonNull(gisuId);
    }

    public static ChapterScopeInfo from(Chapter chapter) {
        return new ChapterScopeInfo(chapter.getId(), chapter.getGisu().getId());
    }
}
