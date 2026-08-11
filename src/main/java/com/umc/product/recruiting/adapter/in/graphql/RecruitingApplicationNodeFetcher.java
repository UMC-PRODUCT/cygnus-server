package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.stereotype.Component;

import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeFetcher;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * {@code node(id:)}에서 RecruitingApplication 노드를 재조회한다.
 * {@code recruitingApplication(applicationId:)} 쿼리와 동일하게 지원자 본인 검증을 수행한다.
 */
@Component
@RequiredArgsConstructor
public class RecruitingApplicationNodeFetcher implements NodeFetcher {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @Override
    public String typeName() {
        return GlobalIdTypes.RECRUITING_APPLICATION;
    }

    @Override
    public RelayNode fetchOrNull(long rawId) {
        Long requesterMemberId = permissionSupport.currentMemberId();
        try {
            return RecruitingApplicationGraphQlResponse.from(
                getApplicationQueryUseCase.getById(rawId, requesterMemberId)
            );
        } catch (RecruitingDomainException exception) {
            if (exception.getBaseCode() == RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND) {
                return null;
            }
            throw exception;
        }
    }
}
