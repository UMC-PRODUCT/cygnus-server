package com.umc.product.curriculum.application.port.out;

import com.umc.product.curriculum.domain.WeeklyBestWorkbook;

public interface LoadWeeklyBestWorkbookPort {

    WeeklyBestWorkbook getById(Long id);

    boolean existsByWeeklyCurriculumIdAndStudyGroupId(Long weeklyCurriculumId, Long studyGroupId);

    boolean existsByMemberIdAndWeeklyCurriculumIdAndStudyGroupId(
        Long memberId,
        Long weeklyCurriculumId,
        Long studyGroupId
    );
}
