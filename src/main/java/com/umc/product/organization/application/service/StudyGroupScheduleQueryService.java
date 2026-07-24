package com.umc.product.organization.application.service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.organization.application.port.in.query.GetStudyGroupScheduleUseCase;
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
}
