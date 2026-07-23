package com.umc.product.authorization.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.authorization.application.port.in.query.dto.ResourcePermissionInfo;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;

public record ResourcePermissionGraphQlResponse(
    ResourceType resourceType,
    Long resourceId,
    List<PermissionGrant> grants
) {

    public static ResourcePermissionGraphQlResponse from(ResourcePermissionInfo info) {
        List<PermissionGrant> grants = info.permissions().entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .map(entry -> new PermissionGrant(entry.getKey(), entry.getValue()))
            .toList();
        return new ResourcePermissionGraphQlResponse(info.resourceType(), info.resourceId(), grants);
    }

    public record PermissionGrant(
        PermissionType permission,
        boolean allowed
    ) {
    }
}
