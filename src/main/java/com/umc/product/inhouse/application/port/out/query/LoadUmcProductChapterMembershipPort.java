package com.umc.product.inhouse.application.port.out.query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.umc.product.inhouse.domain.UmcProductChapterMembership;

public interface LoadUmcProductChapterMembershipPort {

    UmcProductChapterMembership getById(Long chapterMembershipId);

    List<UmcProductChapterMembership> listByUmcProductMemberId(Long umcProductMemberId);

    List<UmcProductChapterMembership> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds);

    boolean existsByChapterId(Long chapterId);

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    boolean existsOverlappingChapterMembership(
        Long umcProductMemberId,
        Long chapterId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedChapterMembershipId
    );

}
