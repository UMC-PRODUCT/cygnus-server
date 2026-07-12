package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingGraphQlPermissionSupport {

    private final CheckPermissionUseCase checkPermissionUseCase;
    private final CurrentMemberProvider currentMemberProvider;

    public void assertRecruitmentTypePermission(PermissionType permission) {
        checkPermissionUseCase.checkOrThrow(
            currentMemberId(),
            ResourcePermission.ofType(ResourceType.RECRUITMENT, permission)
        );
    }

    public void assertRecruitmentTypePermission(Long memberId, PermissionType permission) {
        checkPermissionUseCase.checkOrThrow(
            memberId,
            ResourcePermission.ofType(ResourceType.RECRUITMENT, permission)
        );
    }

    public void assertRecruitmentPermission(Long seasonId, PermissionType permission) {
        checkPermissionUseCase.checkOrThrow(currentMemberId(), recruitmentPermission(seasonId, permission));
    }

    public void assertRecruitmentPermission(Long memberId, Long seasonId, PermissionType permission) {
        checkPermissionUseCase.checkOrThrow(memberId, recruitmentPermission(seasonId, permission));
    }

    public boolean hasRecruitmentPermission(Long memberId, Long seasonId, PermissionType permission) {
        return checkPermissionUseCase.check(memberId, recruitmentPermission(seasonId, permission));
    }

    public void assertResourceBelongsToSeason(boolean belongsToSeason) {
        if (!belongsToSeason) {
            throw new AccessDeniedException("해당 모집 리소스에 접근할 권한이 없어요.");
        }
    }

    public Long currentMemberId() {
        return currentMemberProvider.getRequiredCurrentMemberId();
    }

    public Long currentMemberId(MemberPrincipal memberPrincipal) {
        if (memberPrincipal == null) {
            throw new AccessDeniedException("로그인이 필요해요. 로그인 후 다시 시도해주세요.");
        }
        return memberPrincipal.getMemberId();
    }

    public Long nullableCurrentMemberId() {
        return currentMemberProvider.getNullableCurrentMemberId();
    }

    public Long nullableCurrentMemberId(MemberPrincipal memberPrincipal) {
        return memberPrincipal == null ? null : memberPrincipal.getMemberId();
    }

    private ResourcePermission recruitmentPermission(Long seasonId, PermissionType permission) {
        return ResourcePermission.of(ResourceType.RECRUITMENT, seasonId, permission);
    }
}
