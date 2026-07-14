package com.umc.product.authorization.application.service.query;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.ListChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleBasicInfo;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRolePolicyInfo;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.CheckMemberExistenceUseCase;
import com.umc.product.member.application.port.in.query.ListMemberSystemRoleUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자의 역할 정보를 조회하는 Query Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChallengerRoleQueryService implements
    ListChallengerRoleUseCase,
    CheckChallengerAuthorityUseCase,
    GetChallengerRoleUseCase {

    private final LoadChallengerRolePort loadChallengerRolePort;

    private final GetGisuUseCase getGisuUseCase;

    private final ListMemberSystemRoleUseCase listMemberSystemRoleUseCase;

    private final CheckMemberExistenceUseCase checkMemberExistenceUseCase;

    private ChallengerRoleInfo getChallengerRoleInfoFromEntity(ChallengerRole role) {
        return ChallengerRoleInfo.from(
            role, getGisuUseCase.getById(role.getGisuId())
        );
    }

    @Override
    public ChallengerRoleInfo getById(Long challengerRoleId) {
        return getChallengerRoleInfoFromEntity(
            loadChallengerRolePort.getById(challengerRoleId)
        );
    }

    @Override
    public List<ChallengerRoleInfo> listByMemberId(Long memberId) {
        return loadChallengerRolePort.findByMemberId(memberId).stream()
            .map(this::getChallengerRoleInfoFromEntity)
            .toList();
    }

    @Override
    public List<ChallengerRoleBasicInfo> listBasicByMemberId(Long memberId) {
        return loadChallengerRolePort.findByMemberId(memberId).stream()
            .map(ChallengerRoleBasicInfo::from)
            .toList();
    }

    @Override
    public List<ChallengerRolePolicyInfo> listPolicyFactsByMemberId(Long memberId) {
        return loadChallengerRolePort.findByMemberId(memberId).stream()
            .map(ChallengerRolePolicyInfo::from)
            .toList();
    }

    @Override
    public List<ChallengerRoleInfo> listByMemberIdAndGisuId(Long memberId, Long gisuId) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
        }

        return loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId).stream()
            .map(this::getChallengerRoleInfoFromEntity)
            .toList();
    }

    @Override
    public boolean isSuperAdmin(Long memberId) {
        if (!memberExists(memberId)) {
            return false;
        }
        return hasSystemSuperAdmin(memberId);
    }

    private boolean memberExists(Long memberId) {
        return checkMemberExistenceUseCase.existsById(memberId);
    }

    private boolean hasSystemSuperAdmin(Long memberId) {
        return listMemberSystemRoleUseCase.listByMemberId(memberId).stream()
            .map(role -> SystemRoleType.from(role.roleType()))
            .anyMatch(roleType -> roleType == SystemRoleType.SUPER_ADMIN);
    }

    @Override
    public boolean isCentralCoreInAnyGisu(Long memberId) {
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }
        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralCore);
    }

    @Override
    public boolean isCentralMemberInAnyGisu(Long memberId) {
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }
        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralMember);
    }

    @Override
    public boolean isSchoolCoreInAnyGisu(Long memberId, Long schoolId) {
        if (schoolId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "schoolId는 null일 수 없습니다.");
        }
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.SCHOOL)
            .filter(role -> role.getOrganizationId().equals(schoolId))
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolCore);
    }

    @Override
    public boolean isSchoolAdminInAnyGisu(Long memberId, Long schoolId) {
        if (schoolId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "schoolId는 null일 수 없습니다.");
        }
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.SCHOOL)
            .filter(role -> role.getOrganizationId().equals(schoolId))
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolAdmin);
    }

    @Override
    public boolean isChapterPresidentInAnyGisu(Long memberId, Long chapterId) {
        if (chapterId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "chapterId는 null일 수 없습니다.");
        }
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.CHAPTER)
            .filter(role -> role.getOrganizationId().equals(chapterId))
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(roleType -> roleType == ChallengerRoleType.CHAPTER_PRESIDENT);
    }

    @Override
    public List<ChallengerRoleType> getAllRoleTypesByMemberIdAndGisuId(Long memberId, Long gisuId) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE, "권한을 확인하려면 기수를 선택해주세요.");
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return roles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .distinct()
            .toList();
    }

    @Override
    public List<ChallengerRoleType> getAllRoleTypesByMemberIdAndSchoolId(Long memberId, Long schoolId) {
        if (schoolId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "schoolId는 null일 수 없습니다.");
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.SCHOOL)
            .filter(role -> role.getOrganizationId().equals(schoolId))
            .map(ChallengerRole::getChallengerRoleType)
            .distinct()
            .toList();
    }

    @Override
    public boolean hasRoleTypeInGisu(Long memberId, Long gisuId, ChallengerRoleType role) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
        }
        if (role == null) {
            return false;
        }
        if (!memberExists(memberId)) {
            return false;
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return roles.stream()
            .anyMatch(r -> r.getChallengerRoleType() == role);
    }

    @Override
    public boolean hasAnyRoleTypeInGisu(Long memberId, Long gisuId, ChallengerRoleType... roles) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
        }
        if (roles == null || roles.length == 0) {
            return false;
        }
        if (!memberExists(memberId)) {
            return false;
        }

        Set<ChallengerRoleType> targetRoles = Arrays.stream(roles).collect(Collectors.toSet());
        List<ChallengerRole> memberRoles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return memberRoles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(targetRoles::contains);
    }

    @Override
    public boolean hasAllRoleTypeInGisu(Long memberId, Long gisuId, ChallengerRoleType... roles) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
        }
        if (!memberExists(memberId)) {
            return false;
        }
        if (roles == null || roles.length == 0) {
            return true;
        }

        Set<ChallengerRoleType> targetRoles = Arrays.stream(roles).collect(Collectors.toSet());
        List<ChallengerRole> memberRoles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        Set<ChallengerRoleType> memberRoleTypes = memberRoles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .collect(Collectors.toSet());

        return memberRoleTypes.containsAll(targetRoles);
    }

    @Override
    public boolean isCentralCoreInGisu(Long memberId, Long gisuId) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
        }
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return roles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralCore);
    }

    @Override
    public Map<Long, List<ChallengerRoleType>> mapRoleTypesByChallengerIds(Set<Long> challengerIds) {
        if (challengerIds == null || challengerIds.isEmpty()) {
            return Map.of();
        }

        return loadChallengerRolePort.findByChallengerIdIn(challengerIds).stream()
            .collect(Collectors.groupingBy(
                ChallengerRole::getChallengerId,
                Collectors.mapping(ChallengerRole::getChallengerRoleType, Collectors.toList())
            ));
    }

    @Override
    public Set<ChallengerPart> listResponsiblePartsByMemberIdAndGisuId(Long memberId, Long gisuId) {
        return loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId).stream()
            .map(ChallengerRole::getResponsiblePart)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isCentralMemberInGisu(Long memberId, Long gisuId) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
        }
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return roles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralMember);
    }

    @Override
    public boolean isSchoolCoreInGisu(Long memberId, Long gisuId, Long schoolId) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
        }
        if (schoolId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "schoolId는 null일 수 없습니다.");
        }
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.SCHOOL)
            .filter(role -> role.getOrganizationId().equals(schoolId))
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolCore);
    }

    @Override
    public boolean isSchoolAdminInGisu(Long memberId, Long gisuId, Long schoolId) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
        }
        if (schoolId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "schoolId는 null일 수 없습니다.");
        }
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.SCHOOL)
            .filter(role -> role.getOrganizationId().equals(schoolId))
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolAdmin);
    }

    @Override
    public boolean isChapterPresidentInGisu(Long memberId, Long gisuId, Long chapterId) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
        }
        if (chapterId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "chapterId는 null일 수 없습니다.");
        }
        if (!memberExists(memberId)) {
            return false;
        }
        if (hasSystemSuperAdmin(memberId)) {
            return true;
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.CHAPTER)
            .filter(role -> role.getOrganizationId().equals(chapterId))
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(roleType -> roleType == ChallengerRoleType.CHAPTER_PRESIDENT);
    }
}
