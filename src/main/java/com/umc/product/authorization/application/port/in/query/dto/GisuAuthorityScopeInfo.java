package com.umc.product.authorization.application.port.in.query.dto;

import java.util.Set;

public record GisuAuthorityScopeInfo(
    boolean allSchoolsAccessible,
    Set<Long> chapterPresidentChapterIds,
    Set<Long> schoolAdminSchoolIds
) {

    public GisuAuthorityScopeInfo {
        chapterPresidentChapterIds = chapterPresidentChapterIds == null
            ? Set.of()
            : Set.copyOf(chapterPresidentChapterIds);
        schoolAdminSchoolIds = schoolAdminSchoolIds == null
            ? Set.of()
            : Set.copyOf(schoolAdminSchoolIds);
    }

    public boolean canAccess(Long chapterId, Long schoolId) {
        return allSchoolsAccessible
            || chapterPresidentChapterIds.contains(chapterId)
            || schoolAdminSchoolIds.contains(schoolId);
    }
}
