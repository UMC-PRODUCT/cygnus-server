package com.umc.product.project.application.port.in.query;

import java.util.List;
import java.util.Map;

import com.umc.product.project.application.port.in.query.dto.ProjectApplicationSummaryInfo;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsBatchQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsQuery;

/**
 * PM/운영진용 단일 프로젝트 지원자 목록 조회 UseCase.
 * <p>
 * 임시저장(DRAFT)을 제외한 SUBMITTED/APPROVED/REJECTED 지원서만 반환한다. 매칭 차수 / 상태는 Query 의 옵셔널 필드로 받아 동적 필터를 적용한다.
 * <p>
 * 본 UseCase 는 ProjectApplication 자원만 반환한다. 화면 카드 조립에 필요한 부가 정보 (지원자 챌린저 part / 매칭 라운드 정보 / 지원자 닉네임/실명/학교)는 Web
 * Assembler 가 cross-domain UseCase 를 통해 합성한다. 파트(part) 필터도 챌린저 도메인의 정보가 필요하므로 Assembler 단계에서 in-memory 로 적용한다.
 * <p>
 * 정렬: 매칭 차수(phase) ASC -> 지원시각(submittedAt) ASC. 같은 파트로 묶기는 클라이언트가 처리한다.
 */
public interface SearchProjectApplicationsUseCase {

    List<ProjectApplicationSummaryInfo> searchByProject(SearchProjectApplicationsQuery query);

    /**
     * PM/운영진용 복수 프로젝트 지원자 목록을 조회한다.
     * <p>
     * 요청한 projectId 는 권한 없음/미존재 여부와 무관하게 결과 Map 의 key 로 유지하며, 노출할 지원서가 없으면 빈 리스트를 반환한다.
     */
    Map<Long, List<ProjectApplicationSummaryInfo>> searchByProjects(SearchProjectApplicationsBatchQuery query);
}
