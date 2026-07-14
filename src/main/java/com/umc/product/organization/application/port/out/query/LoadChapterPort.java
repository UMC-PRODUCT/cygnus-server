package com.umc.product.organization.application.port.out.query;

import java.util.List;
import java.util.Set;

import com.umc.product.organization.domain.Chapter;

public interface LoadChapterPort {

    void validateExists(Long chapterId);

    Chapter findById(Long chapterId);

    List<Chapter> findAll();

    List<Chapter> findByGisuId(Long gisuId);

    List<Chapter> findByGisuIds(Set<Long> gisuIds);

    boolean existsByGisuId(Long gisuId);

    boolean existsByIdAndGisuId(Long chapterId, Long gisuId);
}
