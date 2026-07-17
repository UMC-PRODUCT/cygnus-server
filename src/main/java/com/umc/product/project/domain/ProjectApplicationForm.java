package com.umc.product.project.domain;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트에 지원하기 위한 외부 Form ID를 navigation mirror로 보관하는 엔티티입니다.
 * <p>
 * Form 안에 속한 FormSection에 대한 접근 권한 제한은 {@link ProjectApplicationFormPolicy}에서 가져옵니다.
 * <p>
 * {@link Project}에 Array 타입으로 직접 지정해도 되나, 소속된 각 지원서만이 가지고 있는 필드가 존재해야 하는 확장성을 고려하여 엔티티로 분리합니다.
 */
@Entity
@Table(name = "project_application_form")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectApplicationForm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // title 및 description은 Form에 있는 것을 사용합니다.
    @Column(nullable = false)
    private Long formId;

    @Builder(access = AccessLevel.PRIVATE)
    private ProjectApplicationForm(
        Project project,
        Long formId
    ) {
        this.project = project;
        this.formId = formId;
    }

    /**
     * 프로젝트 지원 폼을 생성하는 정팩메 입니다.
     */
    public static ProjectApplicationForm create(Project project, Long formId) {
        return ProjectApplicationForm.builder()
            .project(project)
            .formId(formId)
            .build();
    }

    /**
     * 본 지원 폼이 인자로 전달된 프로젝트에 속하는지 검증한다.
     */
    public boolean belongsTo(Project project) {
        return this.project.getId().equals(project.getId());
    }
}
