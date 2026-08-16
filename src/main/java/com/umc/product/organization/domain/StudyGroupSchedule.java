package com.umc.product.organization.domain;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "study_group_schedule",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_study_group_schedule_group_week",
        columnNames = {"study_group_id", "weekly_curriculum_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyGroupSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studyGroupId;

    private Long scheduleId;

    private Long weeklyCurriculumId;

    @Builder
    private StudyGroupSchedule(
        Long studyGroupId, Long scheduleId,
        Long weeklyCurriculumId
    ) {
        this.studyGroupId = studyGroupId;
        this.scheduleId = scheduleId;
        this.weeklyCurriculumId = weeklyCurriculumId;
    }
}
