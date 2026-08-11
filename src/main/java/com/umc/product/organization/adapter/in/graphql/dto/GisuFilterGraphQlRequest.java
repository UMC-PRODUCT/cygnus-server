package com.umc.product.organization.adapter.in.graphql.dto;

import java.util.List;
import java.util.Objects;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationQuery;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

/**
 * {@code gisus} 쿼리의 {@code GisuFilterInput}. ids / generations / active 중 정확히 하나만 허용한다.
 */
public record GisuFilterGraphQlRequest(
    List<String> ids,
    List<Integer> generations,
    Boolean active
) {

    public static GisuFilterGraphQlRequest empty() {
        return new GisuFilterGraphQlRequest(null, null, null);
    }

    public GisuOrganizationQuery toQuery() {
        List<Long> uniqueIds = decodeIds();
        List<Long> uniqueGenerations = uniqueGenerations();

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

    private List<Long> decodeIds() {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return GlobalId.decodeLongs(ids, GlobalIdTypes.GISU);
    }

    private List<Long> uniqueGenerations() {
        if (generations == null) {
            return List.of();
        }
        return generations.stream()
            .filter(Objects::nonNull)
            .map(Integer::longValue)
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
