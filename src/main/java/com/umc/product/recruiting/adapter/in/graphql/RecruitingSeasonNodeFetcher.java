package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeFetcher;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonGraphQlResponse;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * {@code node(id:)}에서 RecruitingSeason 노드를 재조회한다.
 * {@code recruitingSeason(id:)} 쿼리와 동일하게 시즌 READ 권한을 검증한다.
 */
@Component
@RequiredArgsConstructor
public class RecruitingSeasonNodeFetcher implements NodeFetcher {

    private final GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @Override
    public String typeName() {
        return GlobalIdTypes.RECRUITING_SEASON;
    }

    @Override
    public RelayNode fetchOrNull(long rawId) {
        permissionSupport.assertRecruitmentPermission(
            permissionSupport.currentMemberId(),
            rawId,
            PermissionType.READ
        );
        try {
            return RecruitingSeasonGraphQlResponse.from(getSeasonConfigurationUseCase.getBySeasonId(rawId));
        } catch (RecruitingDomainException exception) {
            if (exception.getBaseCode() == RecruitingErrorCode.RECRUITING_SEASON_NOT_FOUND) {
                return null;
            }
            throw exception;
        }
    }
}
