package com.umc.product.member.application.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleBasicInfo;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.dto.MemberSearchAccessScope;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberSearchAccessScopeResolver {

    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetChallengerUseCase getChallengerUseCase;

    public MemberSearchAccessScope resolve(Long memberId) {
        List<ChallengerRoleBasicInfo> roles = getChallengerRoleUseCase.findAllBasicByMemberId(memberId);

        if (roles.stream().map(ChallengerRoleBasicInfo::roleType).anyMatch(this::grantsUnrestrictedAccess)) {
            return MemberSearchAccessScope.allowAll();
        }

        Set<Long> allowedSchoolIds = roles.stream()
            .filter(role -> grantsSchoolAccess(role.roleType()))
            .map(ChallengerRoleBasicInfo::organizationId)
            .filter(this::isPositiveId)
            .collect(Collectors.toUnmodifiableSet());

        Set<Long> allowedGisuIds = getChallengerUseCase.getAllBasicByMemberIds(Set.of(memberId))
            .getOrDefault(memberId, List.of()).stream()
            .map(ChallengerBasicInfo::gisuId)
            .filter(this::isPositiveId)
            .collect(Collectors.toUnmodifiableSet());

        if (allowedSchoolIds.isEmpty() && allowedGisuIds.isEmpty()) {
            return MemberSearchAccessScope.denyAll();
        }

        return MemberSearchAccessScope.restrictedTo(allowedSchoolIds, allowedGisuIds);
    }

    private boolean grantsUnrestrictedAccess(ChallengerRoleType roleType) {
        return roleType == ChallengerRoleType.SUPER_ADMIN
            || roleType == ChallengerRoleType.CENTRAL_PRESIDENT
            || roleType == ChallengerRoleType.CENTRAL_VICE_PRESIDENT;
    }

    private boolean grantsSchoolAccess(ChallengerRoleType roleType) {
        return roleType == ChallengerRoleType.SCHOOL_PRESIDENT
            || roleType == ChallengerRoleType.SCHOOL_VICE_PRESIDENT;
    }

    private boolean isPositiveId(Long id) {
        return id != null && id > 0;
    }
}
