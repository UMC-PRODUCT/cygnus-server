package com.umc.product.organization.adapter.in.graphql;

import org.springframework.stereotype.Component;

import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeFetcher;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * {@code node(id:)}의 Gisu 타입 재조회. {@code gisu(id:)} 쿼리와 동일하게 권한 검증 없이 조회한다.
 */
@Component
@RequiredArgsConstructor
public class GisuNodeFetcher implements NodeFetcher {

    private final GetGisuUseCase getGisuUseCase;

    @Override
    public String typeName() {
        return GlobalIdTypes.GISU;
    }

    @Override
    public RelayNode fetchOrNull(long rawId) {
        try {
            return GisuGraphQlResponse.from(getGisuUseCase.getById(rawId));
        } catch (OrganizationDomainException exception) {
            if (exception.getBaseCode() == OrganizationErrorCode.GISU_NOT_FOUND) {
                return null;
            }
            throw exception;
        }
    }
}
