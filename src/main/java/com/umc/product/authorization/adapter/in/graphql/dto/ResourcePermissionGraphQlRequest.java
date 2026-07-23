package com.umc.product.authorization.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.authorization.application.port.in.query.dto.ResourcePermissionQuery;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;

public record ResourcePermissionGraphQlRequest(
    ResourceType resourceType,
    List<Long> resourceIds,
    List<PermissionType> permissionTypes
) {

    public ResourcePermissionQuery toQuery() {
        return ResourcePermissionQuery.of(resourceType, resourceIds, permissionTypes);
    }
}
