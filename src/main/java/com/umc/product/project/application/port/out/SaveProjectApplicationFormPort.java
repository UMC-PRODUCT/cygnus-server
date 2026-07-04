package com.umc.product.project.application.port.out;

import com.umc.product.project.domain.ProjectApplicationForm;
import java.util.List;

/**
 * ProjectApplicationForm 쓰기 Port (Driven / Port Out).
 * <p>
 * 코드베이스 컨벤션: {@code save}, {@code saveAll}, {@code delete}는
 * 현재 사용 여부와 무관하게 함께 선언합니다.
 */
public interface SaveProjectApplicationFormPort {

    ProjectApplicationForm save(ProjectApplicationForm form);

    List<ProjectApplicationForm> saveAll(List<ProjectApplicationForm> forms);

    void delete(ProjectApplicationForm form);

    /**
     * 특정 프로젝트의 모든 지원 폼 매핑 row 를 일괄 삭제합니다. DRAFT/PENDING_REVIEW 단계 프로젝트 hard delete 시 자식 정리용.
     * form 도메인의 Form 자체는 호출 측에서 {@code ManageFormUseCase.deleteForm} 로 별도 정리해야 합니다.
     */
    void deleteAllByProjectId(Long projectId);
}
