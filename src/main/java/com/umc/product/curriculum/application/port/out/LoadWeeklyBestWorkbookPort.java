package com.umc.product.curriculum.application.port.out;

import java.util.Collection;
import java.util.List;

import com.umc.product.curriculum.domain.WeeklyBestWorkbook;

public interface LoadWeeklyBestWorkbookPort {

    WeeklyBestWorkbook getById(Long id);

    /**
     * 여러 (스터디 그룹, 주차) 조합의 베스트 선정자 batch 조회. 조합당 최대 1건.
     */
    List<BestWorkbookHolder> findHolders(
        Collection<Long> studyGroupIds,
        Collection<Long> weeklyCurriculumIds
    );

    /**
     * 어느 그룹의 어느 주차에서 누가 베스트로 뽑혔는지.
     */
    record BestWorkbookHolder(Long studyGroupId, Long weeklyCurriculumId, Long memberId) {
    }

    boolean existsByWeeklyCurriculumIdAndStudyGroupId(Long weeklyCurriculumId, Long studyGroupId);

    boolean existsByMemberIdAndWeeklyCurriculumIdAndStudyGroupId(
        Long memberId,
        Long weeklyCurriculumId,
        Long studyGroupId
    );
}
