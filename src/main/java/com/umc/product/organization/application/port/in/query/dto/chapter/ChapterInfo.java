package com.umc.product.organization.application.port.in.query.dto.chapter;

import com.umc.product.organization.domain.Chapter;

public record ChapterInfo(Long id, Long gisuId, String name) {

    public ChapterInfo(Long id, String name) {
        this(id, null, name);
    }

    public static ChapterInfo from(Chapter chapter) {
        return new ChapterInfo(chapter.getId(), chapter.getGisu().getId(), chapter.getName());
    }
}
