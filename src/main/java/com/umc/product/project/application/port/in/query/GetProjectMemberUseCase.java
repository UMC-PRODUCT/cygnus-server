package com.umc.product.project.application.port.in.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;

/**
 * 프로젝트 멤버 조회 UseCase.
 */
public interface GetProjectMemberUseCase {

    /**
     * 특정 프로젝트의 활성 멤버 목록을 조회합니다.
     */
    List<ProjectMemberInfo> listByProjectId(Long projectId);

    /**
     * 여러 프로젝트의 활성 멤버 목록을 projectId 기준으로 조회합니다.
     */
    Map<Long, List<ProjectMemberInfo>> listByProjectIds(Collection<Long> projectIds);

    ProjectMemberInfo getByProjectIdAndMemberId(Long projectId, Long memberId);

    Optional<ProjectMemberInfo> findByProjectIdAndMemberId(Long projectId, Long memberId);
}
