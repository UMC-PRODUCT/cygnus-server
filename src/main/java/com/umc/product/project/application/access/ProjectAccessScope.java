package com.umc.product.project.application.access;

import java.util.List;
import java.util.Set;

import com.umc.product.project.domain.enums.ProjectStatus;

/**
 * 프로젝트 조회 시 적용되는 가시 범위(scope) typed value.
 * <p>
 * 권한 판정(L2 {@code ProjectPermissionEvaluator})이 binary("진입 가능한가")만 다룬다면, scope 는
 * "어떤 부분집합을 보여줄지"를 표현한다. 같은 사용자라도 호출 의도(공개 검색 vs 관리 화면)에 따라
 * 다른 scope 가 적용될 수 있다.
 */
public sealed interface ProjectAccessScope {

    record Clauses(List<ScopeClause> values) implements ProjectAccessScope {
        public Clauses {
            values = List.copyOf(values);
        }
    }

    /** 모든 프로젝트 노출 (Central Core). 상태 필터는 호출자가 요청한 그대로 통과. */
    record All(Set<ProjectStatus> visibleStatuses) implements ProjectAccessScope { }

    /** 특정 지부의 프로젝트만 노출 (지부장 / 학교 회장단 — 본인 학교가 속한 지부 전체). */
    record ChapterScoped(Long chapterId, Set<ProjectStatus> visibleStatuses) implements ProjectAccessScope { }

    /** 본인이 PM 인 프로젝트만 노출 (PM 챌린저, 관리 화면). */
    record OwnerOnly(Long memberId, Set<ProjectStatus> visibleStatuses) implements ProjectAccessScope { }

    /**
     * 상위 권한 scope 결과에 본인 PO 프로젝트를 추가 포함한다.
     * <p>
     * 운영진/지부장/학교 회장단 scope 에서 DRAFT 를 전체 공개하지 않으면서, 본인이 PO 인 프로젝트만 별도 OR 조건으로 노출하기 위한 값 객체.
     */
    record WithOwnerIncluded(
        ProjectAccessScope baseScope,
        Long ownerMemberId,
        Set<ProjectStatus> ownerVisibleStatuses
    ) implements ProjectAccessScope { }

    /** 일반 챌린저용 공개 목록 ({@link ProjectStatus#IN_PROGRESS} / {@link ProjectStatus#COMPLETED}). */
    record PublicOnly() implements ProjectAccessScope { }

    /** 관리 대상 0건 (일반 챌린저가 관리 화면 호출 시 등). */
    record None() implements ProjectAccessScope { }
}
