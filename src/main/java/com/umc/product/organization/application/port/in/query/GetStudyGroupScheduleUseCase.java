package com.umc.product.organization.application.port.in.query;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * StudyGroup ↔ Schedule 매핑(StudyGroupSchedule) 조회 UseCase.
 * <p>
 * Schedule 도메인이 "특정 스터디 그룹들의 scheduleId" 만 알면 될 때 사용. cross-domain 합성은 호출자 측에서 (도메인 경계 보존).
 */
public interface GetStudyGroupScheduleUseCase {

    /**
     * 주어진 studyGroupIds 에 매핑된 scheduleId 집합을 반환한다.
     */
    Set<Long> findScheduleIdsByStudyGroupIds(Collection<Long> studyGroupIds);

    Optional<Long> findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(
        Long studyGroupId,
        Long weeklyCurriculumId
    );
}
