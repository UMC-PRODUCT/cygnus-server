package com.umc.product.authorization.application.service.query;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.application.port.in.query.GetGisuAuthorityScopeUseCase;
import com.umc.product.authorization.application.port.in.query.dto.GisuAuthorityScopeInfo;
import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.CheckMemberExistenceUseCase;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GisuAuthorityScopeQueryService implements GetGisuAuthorityScopeUseCase {

    private final CheckPermissionUseCase checkPermissionUseCase;
    private final CheckMemberExistenceUseCase checkMemberExistenceUseCase;

    @Override
    public GisuAuthorityScopeInfo getByMemberIdAndGisuId(Long memberId, Long gisuId) {
        if (!checkMemberExistenceUseCase.existsById(memberId)) {
            return new GisuAuthorityScopeInfo(false, Set.of(), Set.of(), false);
        }

        AuthoritySnapshot authority = checkPermissionUseCase.loadSubject(memberId).toAuthoritySnapshot();
        if (authority.isSuperAdmin() || authority.isCentralCoreInGisu(gisuId)) {
            return new GisuAuthorityScopeInfo(true, Set.of(), Set.of(), true);
        }

        Set<RoleAttribute> roles = authority.challengerRoles().stream()
            .filter(role -> gisuId.equals(role.gisuId()))
            .collect(Collectors.toSet());

        Set<Long> chapterPresidentChapterIds = roles.stream()
            .filter(role -> role.organizationType() == OrganizationType.CHAPTER)
            .filter(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT)
            .map(RoleAttribute::organizationId)
            .collect(Collectors.toUnmodifiableSet());

        Set<Long> schoolAdminSchoolIds = roles.stream()
            .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
            .filter(role -> role.roleType().isAtLeastSchoolAdmin())
            .map(RoleAttribute::organizationId)
            .collect(Collectors.toUnmodifiableSet());

        boolean detailedStatisticsAccessible = !chapterPresidentChapterIds.isEmpty()
            || roles.stream()
                .filter(role -> role.organizationType() == OrganizationType.SCHOOL)
                .map(RoleAttribute::roleType)
                .anyMatch(roleType -> roleType.isAtLeastSchoolAdmin()
                    && roleType != ChallengerRoleType.SCHOOL_ETC_ADMIN);

        return new GisuAuthorityScopeInfo(
            false,
            chapterPresidentChapterIds,
            schoolAdminSchoolIds,
            detailedStatisticsAccessible
        );
    }
}
