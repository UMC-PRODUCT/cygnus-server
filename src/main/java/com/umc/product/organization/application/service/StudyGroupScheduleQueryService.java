package com.umc.product.organization.application.service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.organization.application.port.in.query.GetStudyGroupScheduleUseCase;
import com.umc.product.organization.application.port.in.query.GetStudyGroupUseCase;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupSchedulePort;

import lombok.RequiredArgsConstructor;

/**
 * StudyGroupSchedule (StudyGroup ↔ Schedule 매핑) 조회 Service.
 * <p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyGroupScheduleQueryService implements GetStudyGroupScheduleUseCase {

    private final LoadStudyGroupSchedulePort loadStudyGroupSchedulePort;

    private final GetStudyGroupUseCase getStudyGroupUseCase;

    @Override
    public Set<Long> findScheduleIdsByStudyGroupIds(Collection<Long> studyGroupIds) {
        if (studyGroupIds == null || studyGroupIds.isEmpty()) {
            return Set.of();
        }
        return loadStudyGroupSchedulePort.findScheduleIdsByStudyGroupIds(studyGroupIds);
    }

    @Override
    public Optional<Long> findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(
        Long studyGroupId,
        Long weeklyCurriculumId
    ) {
        return loadStudyGroupSchedulePort.findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(
            studyGroupId,
            weeklyCurriculumId
        );
    }

    /**
     * 사용자에게 보이는 스터디 그룹을 먼저 구한 뒤, 그 그룹들에 매핑된 scheduleId 를 반환한다.
     * <p>
     * 보이는 그룹이 없으면 findScheduleIdsByStudyGroupIds 가 빈 입력을 즉시 걸러 내므로 불필요한 IN() 쿼리는 나가지 않는다.
     */
    @Override
    public Set<Long> findVisibleScheduleIdsByMemberId(Long memberId) {
        Set<Long> visibleStudyGroupIds = getStudyGroupUseCase.findVisibleStudyGroupIds(memberId);

        return findScheduleIdsByStudyGroupIds(visibleStudyGroupIds);
    }
}
