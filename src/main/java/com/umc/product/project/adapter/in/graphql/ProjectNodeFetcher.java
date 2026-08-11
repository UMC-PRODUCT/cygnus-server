package com.umc.product.project.adapter.in.graphql;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeFetcher;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.project.adapter.in.graphql.dto.ProjectGraphQlResponse;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;

import lombok.RequiredArgsConstructor;

/**
 * Project 타입의 node(id:) 재조회. {@code project(id:)} 쿼리와 동일한 권한 검증을 수행하며,
 * 대상 프로젝트가 없으면 null을 반환한다.
 */
@Component
@RequiredArgsConstructor
public class ProjectNodeFetcher implements NodeFetcher {

    private final GetProjectUseCase getProjectUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;
    private final CurrentMemberProvider currentMemberProvider;

    @Override
    public String typeName() {
        return GlobalIdTypes.PROJECT;
    }

    @Override
    public RelayNode fetchOrNull(long rawId) {
        Long requesterMemberId = currentMemberProvider.getRequiredCurrentMemberId();
        checkPermissionUseCase.checkOrThrow(
            requesterMemberId,
            ResourcePermission.of(ResourceType.PROJECT, rawId, PermissionType.READ)
        );
        ProjectInfo info = getProjectUseCase.findAllByIds(List.of(rawId)).get(rawId);
        return info == null ? null : ProjectGraphQlResponse.from(info);
    }
}
