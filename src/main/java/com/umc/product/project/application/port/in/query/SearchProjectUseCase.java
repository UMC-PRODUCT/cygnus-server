package com.umc.product.project.application.port.in.query;

import org.springframework.data.domain.Page;

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;

/**
 * 프로젝트 목록 검색 UseCase (PROJECT-001).
 * <p>
 * 호출자 역할에 따라 가시 범위(scope)가 자동 적용된다. 일반 챌린저/지부장/학교장은
 * IN_PROGRESS 만, Central Core 만 요청한 상태 전체를 조회한다.
 */
public interface SearchProjectUseCase {

    /**
     * 조건에 맞는 프로젝트 목록을 페이징하여 반환합니다.
     *
     * @param query    검색 조건 ({@code Pageable} 포함)
     * @param memberId 호출자 ID — scope 결정에 사용
     * @return ProjectInfo 페이지
     */
    Page<ProjectInfo> search(SearchProjectQuery query, Long memberId);

    Page<ProjectInfo> search(SearchProjectQuery query, SubjectAttributes subject);
}
