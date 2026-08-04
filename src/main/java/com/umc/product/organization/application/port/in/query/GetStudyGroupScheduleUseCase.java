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
     * <p>
     * 볼 수 있는 스터디 그룹을 호출자가 이미 알고 있을 때 쓴다. memberId 밖에 없다면 {@link #findVisibleScheduleIdsByMemberId} 를 쓴다.
     */
    Set<Long> findScheduleIdsByStudyGroupIds(Collection<Long> studyGroupIds);

    Optional<Long> findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(
        Long studyGroupId,
        Long weeklyCurriculumId
    );

    /**
     * 사용자에게 보이는 스터디 그룹 일정의 scheduleId 집합을 반환한다.
     * <p>
     * 호출자는 memberId 만 넘기면 된다. "보이는 스터디 그룹" 판단은 Organization 이 하고, 그 그룹들에 매핑된 scheduleId 까지 여기서 채워 준다.
     * <p>
     * Schedule 도메인이 운영진 일정 조회 범위에 스터디 그룹 일정을 더할 때 사용한다. 권한 없는 일반 챌린저는 빈 Set.
     *
     * @param memberId 요청 주체 memberId
     * @return 매핑된 scheduleId 집합 (보이는 그룹이 없거나 매핑이 없으면 빈 Set)
     */
    Set<Long> findVisibleScheduleIdsByMemberId(Long memberId);
}
