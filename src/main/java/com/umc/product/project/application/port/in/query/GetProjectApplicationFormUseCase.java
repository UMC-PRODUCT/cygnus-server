package com.umc.product.project.application.port.in.query;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;

/**
 * 프로젝트 지원 폼 조회 UseCase (PROJECT-106-GET).
 * <p>
 * 폼이 없으면 {@link Optional#empty()} 를 반환하며, Controller 단에서 {@code ApiResponse.result = null} 로 매핑된다.
 * <p>
 * 호출자 역할에 따라 응답이 다음과 같이 차등 적용된다.
 * <ul>
 *   <li>프로젝트 PM(owner) / Central Core / 프로젝트 지부의 지부장 → 전체 섹션 노출</li>
 *   <li>그 외 챌린저(지원자, 프로젝트 기수 소속) → 본인 파트가 매칭된 {@code PART} 섹션 + 모든 {@code COMMON} 섹션만 노출</li>
 *   <li>해당 기수에 챌린저 레코드가 없는 외부 사용자 → 도메인 예외(403) 로 차단</li>
 * </ul>
 */
public interface GetProjectApplicationFormUseCase {

    /**
     * 특정 프로젝트의 지원 폼 구조를 조회합니다.
     *
     * @param projectId         조회 대상 프로젝트 ID
     * @param requesterMemberId 호출자 Member ID. 마스킹/접근 판단 기준
     * @return 폼이 있으면 호출자 역할에 따라 (마스킹된) {@link ApplicationFormInfo}, 없으면 {@link Optional#empty()}
     */
    Optional<ApplicationFormInfo> findByProjectId(Long projectId, Long requesterMemberId);

    /**
     * 여러 프로젝트의 지원 폼 구조를 projectId 기준으로 조회합니다.
     *
     * @param projectIds         조회 대상 프로젝트 ID 목록
     * @param requesterMemberId 호출자 Member ID. 마스킹/접근 판단 기준
     * @return 폼이 존재하는 프로젝트만 포함한 projectId -> {@link ApplicationFormInfo} 맵
     */
    Map<Long, ApplicationFormInfo> findAllByProjectIds(Collection<Long> projectIds, Long requesterMemberId);
}
