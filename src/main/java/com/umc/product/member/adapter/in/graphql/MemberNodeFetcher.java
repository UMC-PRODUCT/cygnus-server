package com.umc.product.member.adapter.in.graphql;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeFetcher;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.member.adapter.in.graphql.dto.MemberGraphQlResponse;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * {@code node(id:)}의 Member 타입 재조회. {@code member(id:)} 쿼리와 동일하게
 * MEMBER READ 권한을 검증하고, 요청자 자신은 비공개 필드가 포함된 동일한 표현을 반환한다.
 */
@Component
@RequiredArgsConstructor
public class MemberNodeFetcher implements NodeFetcher {

    private final GetMemberUseCase getMemberUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;
    private final CurrentMemberProvider currentMemberProvider;

    @Override
    public String typeName() {
        return GlobalIdTypes.MEMBER;
    }

    @Override
    public RelayNode fetchOrNull(long rawId) {
        Long requesterMemberId = currentMemberProvider.getRequiredCurrentMemberId();
        checkPermissionUseCase.checkOrThrow(
            requesterMemberId,
            ResourcePermission.of(ResourceType.MEMBER, rawId, PermissionType.READ)
        );
        try {
            if (requesterMemberId.equals(rawId)) {
                return MemberGraphQlResponse.privateFrom(getMemberUseCase.getById(rawId));
            }
            return MemberGraphQlResponse.publicFrom(getMemberUseCase.getById(rawId));
        } catch (MemberDomainException exception) {
            if (exception.getBaseCode() == MemberErrorCode.MEMBER_NOT_FOUND) {
                return null;
            }
            throw exception;
        }
    }
}
