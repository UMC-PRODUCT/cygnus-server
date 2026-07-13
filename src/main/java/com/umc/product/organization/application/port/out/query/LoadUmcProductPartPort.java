package com.umc.product.organization.application.port.out.query;

import java.util.Collection;
import java.util.List;

import com.umc.product.organization.domain.UmcProductPart;

public interface LoadUmcProductPartPort {

    UmcProductPart getById(Long partId);

    UmcProductPart getByIdWithLock(Long partId);

    List<UmcProductPart> listAll(Long chapterId, Boolean active);

    List<UmcProductPart> listByChapterIds(Collection<Long> chapterIds, Boolean active);

    List<UmcProductPart> listByIds(Collection<Long> partIds);

    boolean existsByChapterId(Long chapterId);

    boolean existsByChapterIdAndCode(Long chapterId, String code, Long excludedPartId);
}
