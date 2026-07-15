package com.umc.product.authorization.application.service.query;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleBasicInfo;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
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
public class ChallengerRoleQueryService implements GetChallengerRoleUseCase {

    private final LoadChallengerRolePort loadChallengerRolePort;

    private final GetGisuUseCase getGisuUseCase;

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
    public List<ChallengerRoleInfo> findAllByMemberId(Long memberId) {
        return loadChallengerRolePort.findByMemberId(memberId).stream()
            .map(this::getChallengerRoleInfoFromEntity)
            .toList();
    }

    @Override
    public List<ChallengerRoleBasicInfo> findAllBasicByMemberId(Long memberId) {
        return loadChallengerRolePort.findByMemberId(memberId).stream()
            .map(ChallengerRoleBasicInfo::from)
            .toList();
    }

    @Override
    public boolean isSuperAdmin(Long memberId) {
        return loadChallengerRolePort.findByMemberId(memberId).stream()
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isSuperAdmin);
    }

    @Override
    public boolean isCentralCore(Long memberId) {
        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralCore);
    }

    @Override
    public boolean isCentralMember(Long memberId) {
        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralMember);
    }

    @Override
    public boolean isSchoolCore(Long memberId, Long schoolId) {
        if (schoolId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "schoolId는 null일 수 없습니다.");
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.SCHOOL)
            .filter(role -> role.getOrganizationId().equals(schoolId))
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolCore);
    }

    @Override
    public boolean isSchoolAdmin(Long memberId, Long schoolId) {
        if (schoolId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "schoolId는 null일 수 없습니다.");
        }

        List<ChallengerRole> roles = loadChallengerRolePort.findByMemberId(memberId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.SCHOOL)
            .filter(role -> role.getOrganizationId().equals(schoolId))
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastSchoolAdmin);
    }

    @Override
    public boolean isChapterPresident(Long memberId, Long chapterId) {
        if (chapterId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "chapterId는 null일 수 없습니다.");
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

        List<ChallengerRole> roles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return roles.stream()
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(ChallengerRoleType::isAtLeastCentralCore);
    }

    @Override
    public Map<Long, List<ChallengerRoleType>> getAllRoleTypesByChallengerIds(Set<Long> challengerIds) {
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
    public Set<ChallengerPart> getAllResponsiblePartByMemberIdAndGisuId(Long memberId, Long gisuId) {
        return loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId).stream()
            .map(ChallengerRole::getResponsiblePart)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean hasRoleInOrganization(
        Long challengerId, ChallengerRoleType roleType, Long organizationId, Long gisuId
    ) {
        if (challengerId == null || roleType == null || gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "역할 중복 확인에는 challengerId, roleType, gisuId가 모두 필요합니다.");
        }

        return loadChallengerRolePort.existsByChallengerRole(challengerId, roleType, organizationId, gisuId);
    }

    @Override
    public boolean isCentralMemberInGisu(Long memberId, Long gisuId) {
        if (gisuId == null) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_INPUT_VALUE,
                "gisuId는 null일 수 없습니다.");
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

        List<ChallengerRole> roles = loadChallengerRolePort.findRolesByMemberIdAndGisuId(memberId, gisuId);

        return roles.stream()
            .filter(role -> role.getOrganizationType() == OrganizationType.CHAPTER)
            .filter(role -> role.getOrganizationId().equals(chapterId))
            .map(ChallengerRole::getChallengerRoleType)
            .anyMatch(roleType -> roleType == ChallengerRoleType.CHAPTER_PRESIDENT);
    }
}
