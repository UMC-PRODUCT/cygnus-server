package com.umc.product.organization.application.port.out.query;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface LoadStudyGroupSchedulePort {

    /**
     * 주어진 studyGroupIds 에 매핑된 scheduleId 집합을 반환한다.
     * <p>
     * Schedule 도메인이 "사용자에게 보이는 스터디 그룹" 의 scheduleId 들을 알아내는 데 사용.
     *
     * @param studyGroupIds 조회 대상 스터디 그룹 ID 집합. 비어있으면 빈 Set.
     * @return 매핑된 scheduleId 집합. 매핑이 없으면 빈 Set.
     */
    Set<Long> findScheduleIdsByStudyGroupIds(Collection<Long> studyGroupIds);

    Optional<Long> findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(
        Long studyGroupId,
        Long weeklyCurriculumId
    );
}
