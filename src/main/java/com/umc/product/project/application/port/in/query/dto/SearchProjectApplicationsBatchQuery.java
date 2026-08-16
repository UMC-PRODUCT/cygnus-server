package com.umc.product.project.application.port.in.query.dto;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.Builder;

/**
 * PM/운영진용 복수 프로젝트 지원자 목록 조회 Query.
 * <p>
 * 단건 조회와 동일하게 DRAFT 지원서는 응답과 상태 필터에서 제외한다.
 *
 * @param requesterMemberId 요청자 Member ID. 권한 scope 결정(L1)에 사용된다.
 * @param projectIds        대상 프로젝트 ID 목록. 입력 순서 기준으로 중복을 제거한다.
 * @param matchingRoundId   매칭 차수 필터 (선택). null 이면 전체 차수.
 * @param part              지원 파트 필터 (선택). null 이면 전체 파트.
 * @param status            지원 상태 필터 (선택). null 이면 SUBMITTED/APPROVED/REJECTED 전체.
 */
@Builder
public record SearchProjectApplicationsBatchQuery(
    Long requesterMemberId,
    List<Long> projectIds,
    Long matchingRoundId,
    ChallengerPart part,
    ProjectApplicationStatus status
) {
    public SearchProjectApplicationsBatchQuery {
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        Objects.requireNonNull(projectIds, "projectIds must not be null");
        if (projectIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("projectIds must not contain null");
        }
        projectIds = List.copyOf(new LinkedHashSet<>(projectIds));
        if (projectIds.isEmpty()) {
            throw new IllegalArgumentException("projectIds must not be empty");
        }
        if (status == ProjectApplicationStatus.DRAFT) {
            throw new ProjectDomainException(
                ProjectErrorCode.APPLICATION_DRAFT_FILTER_NOT_ALLOWED);
        }
    }
}
