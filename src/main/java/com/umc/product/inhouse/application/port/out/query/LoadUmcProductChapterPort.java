package com.umc.product.inhouse.application.port.out.query;

import java.util.Collection;
import java.util.List;

import com.umc.product.inhouse.domain.UmcProductChapter;

public interface LoadUmcProductChapterPort {

    UmcProductChapter getById(Long chapterId);

    UmcProductChapter getByIdWithLock(Long chapterId);

    List<UmcProductChapter> listAll(Boolean active);

    List<UmcProductChapter> listByIds(Collection<Long> chapterIds);

    boolean existsById(Long chapterId);

    boolean existsByCode(String code, Long excludedChapterId);
}
