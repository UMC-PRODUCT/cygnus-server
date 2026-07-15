package com.umc.product.project.application.port.in.command.dto;

import java.util.List;
import java.util.Objects;

import lombok.Builder;

/**
 * 프로젝트 완료(complete) Command.
 * <p>
 * 기수 종료 시 여러 IN_PROGRESS 프로젝트를 한 번에 COMPLETED 로 전이합니다. ACTIVE 멤버는 COMPLETED,
 * 진행 중(DRAFT/SUBMITTED) ProjectApplication 은 CANCELLED 로 동기화합니다.
 * <p>
 * 원자적으로 처리되며, 대상 중 하나라도 상태/권한 조건을 만족하지 못하면 전체 롤백됩니다.
 */
@Builder
public record CompleteProjectsCommand(
    List<Long> projectIds,
    Long requesterMemberId
) {
    public CompleteProjectsCommand {
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        if (projectIds == null || projectIds.isEmpty()) {
            throw new IllegalArgumentException("projectIds must not be empty");
        }
        if (projectIds.stream().distinct().count() != projectIds.size()) {
            throw new IllegalArgumentException("projectIds must not contain duplicates");
        }
    }
}
