package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingGraphQlPermissionSupport {

    private final CheckPermissionUseCase checkPermissionUseCase;

    public void assertRecruitmentTypePermission(PermissionType permission) {
        checkPermissionUseCase.checkOrThrow(
            currentMemberId(),
            ResourcePermission.ofType(ResourceType.RECRUITMENT, permission)
        );
    }

    public void assertRecruitmentPermission(Long seasonId, PermissionType permission) {
        checkPermissionUseCase.checkOrThrow(currentMemberId(), recruitmentPermission(seasonId, permission));
    }

    public void assertRecruitmentPermission(Long memberId, Long seasonId, PermissionType permission) {
        checkPermissionUseCase.checkOrThrow(memberId, recruitmentPermission(seasonId, permission));
    }

    public void assertResourceBelongsToSeason(boolean belongsToSeason) {
        if (!belongsToSeason) {
            throw new AccessDeniedException("해당 모집 리소스에 접근할 권한이 없어요.");
        }
    }

    public Long currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요해요. 로그인 후 다시 시도해주세요.");
        }
        if (authentication.getPrincipal() instanceof MemberPrincipal principal) {
            return principal.getMemberId();
        }
        throw new AccessDeniedException("인증 정보가 올바르지 않아요. 다시 로그인해주세요.");
    }

    public Long nullableCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MemberPrincipal principal) {
            return principal.getMemberId();
        }
        return null;
    }

    private ResourcePermission recruitmentPermission(Long seasonId, PermissionType permission) {
        return ResourcePermission.of(ResourceType.RECRUITMENT, seasonId, permission);
    }
}
