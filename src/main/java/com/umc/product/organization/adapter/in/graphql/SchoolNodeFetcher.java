package com.umc.product.organization.adapter.in.graphql;

import org.springframework.stereotype.Component;

import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeFetcher;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.organization.adapter.in.graphql.dto.SchoolGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * {@code node(id:)}의 School 타입 재조회. {@code school(id:)} 쿼리와 동일하게 권한 검증 없이 조회한다.
 */
@Component
@RequiredArgsConstructor
public class SchoolNodeFetcher implements NodeFetcher {

    private final GetSchoolUseCase getSchoolUseCase;

    @Override
    public String typeName() {
        return GlobalIdTypes.SCHOOL;
    }

    @Override
    public RelayNode fetchOrNull(long rawId) {
        try {
            return SchoolGraphQlResponse.from(getSchoolUseCase.getSchoolDetail(rawId));
        } catch (OrganizationDomainException exception) {
            if (exception.getBaseCode() == OrganizationErrorCode.SCHOOL_NOT_FOUND) {
                return null;
            }
            throw exception;
        }
    }
}
