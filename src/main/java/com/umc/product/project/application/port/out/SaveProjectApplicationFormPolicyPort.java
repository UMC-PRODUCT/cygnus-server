package com.umc.product.project.application.port.out;

import java.util.List;

import com.umc.product.project.domain.ProjectApplicationFormPolicy;

/**
 * ProjectApplicationFormPolicy 쓰기 Port (Driven / Port Out).
 * <p>
 * 코드베이스 컨벤션: {@code save}, {@code saveAll}, {@code delete}는
 * 현재 사용 여부와 무관하게 함께 선언합니다.
 */
public interface SaveProjectApplicationFormPolicyPort {

    ProjectApplicationFormPolicy save(ProjectApplicationFormPolicy policy);

    List<ProjectApplicationFormPolicy> saveAll(List<ProjectApplicationFormPolicy> policies);

    void delete(ProjectApplicationFormPolicy policy);

    /**
     * 특정 FormSection 에 매핑된 정책 row 를 삭제합니다.
     * <p>
     * Form 단의 {@code deleteSection} 호출 후 이 메서드로 매핑 row 를 정리합니다.
     */
    void deleteByFormSectionId(Long formSectionId);

    /**
     * 특정 지원 폼에 속한 모든 정책 row 를 일괄 삭제합니다. 프로젝트 hard delete 시
     * ProjectApplicationForm 정리 전에 호출해야 합니다.
     */
    void deleteAllByApplicationFormId(Long applicationFormId);
}
