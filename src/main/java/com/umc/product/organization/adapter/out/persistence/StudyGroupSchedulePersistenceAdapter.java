package com.umc.product.organization.adapter.out.persistence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.global.persistence.ConstraintViolationInspector;
import com.umc.product.organization.application.port.out.command.SaveStudyGroupSchedulePort;
import com.umc.product.organization.application.port.out.query.LoadStudyGroupSchedulePort;
import com.umc.product.organization.domain.StudyGroupSchedule;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StudyGroupSchedulePersistenceAdapter
    implements SaveStudyGroupSchedulePort, LoadStudyGroupSchedulePort {

    private static final String UNIQUE_GROUP_WEEK_CONSTRAINT = "uk_study_group_schedule_group_week";

    private final StudyGroupScheduleJpaRepository jpaRepository;

    @Override
    public StudyGroupSchedule save(StudyGroupSchedule studyGroupSchedule) {
        try {
            return jpaRepository.saveAndFlush(studyGroupSchedule);
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolationInspector.matches(e, UNIQUE_GROUP_WEEK_CONSTRAINT)) {
                throw new OrganizationDomainException(OrganizationErrorCode.STUDY_GROUP_SCHEDULE_ALREADY_EXISTS, e);
            }
            throw e;
        }
    }

    @Override
    public Set<Long> findScheduleIdsByStudyGroupIds(Collection<Long> studyGroupIds) {
        if (studyGroupIds == null || studyGroupIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jpaRepository.findScheduleIdsByStudyGroupIdIn(studyGroupIds));
    }

    @Override
    public Optional<Long> findScheduleIdByStudyGroupIdAndWeeklyCurriculumId(
        Long studyGroupId,
        Long weeklyCurriculumId
    ) {
        return jpaRepository.findByStudyGroupIdAndWeeklyCurriculumId(studyGroupId, weeklyCurriculumId)
            .map(StudyGroupSchedule::getScheduleId);
    }
}
