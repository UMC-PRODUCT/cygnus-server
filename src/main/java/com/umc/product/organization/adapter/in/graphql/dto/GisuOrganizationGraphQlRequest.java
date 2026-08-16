package com.umc.product.organization.adapter.in.graphql.dto;

import java.util.List;
import java.util.Objects;

import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationQuery;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

public record GisuOrganizationGraphQlRequest(
    List<Long> ids,
    List<Long> generations,
    Boolean active
) {

    public GisuOrganizationQuery toQuery() {
        List<Long> uniqueIds = unique(ids);
        List<Long> uniqueGenerations = unique(generations);

        if (Boolean.FALSE.equals(active)) {
            throw invalidCondition();
        }

        boolean hasIds = !uniqueIds.isEmpty();
        boolean hasGenerations = !uniqueGenerations.isEmpty();
        boolean hasActive = Boolean.TRUE.equals(active);

        if (countSelected(hasIds, hasGenerations, hasActive) != 1) {
            throw invalidCondition();
        }

        if (hasIds) {
            return GisuOrganizationQuery.byIds(uniqueIds, false, false);
        }
        if (hasGenerations) {
            return GisuOrganizationQuery.byGenerations(uniqueGenerations, false, false);
        }
        return GisuOrganizationQuery.active(false, false);
    }

    private List<Long> unique(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private int countSelected(boolean hasIds, boolean hasGenerations, boolean hasActive) {
        int count = 0;
        if (hasIds) {
            count++;
        }
        if (hasGenerations) {
            count++;
        }
        if (hasActive) {
            count++;
        }
        return count;
    }

    private OrganizationDomainException invalidCondition() {
        return new OrganizationDomainException(OrganizationErrorCode.GISU_QUERY_CONDITION_INVALID);
    }
}
