package com.umc.product.analytics.application.service.query;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.analytics.domain.AnalyticsDomainException;
import com.umc.product.analytics.domain.AnalyticsErrorCode;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminAnalyticsScopeResolver {

    private final GetChallengerRoleUseCase getGisuChallengerRoleUseCase;
    private final GetGisuUseCase getGisuUseCase;

    public AdminAnalyticsScope resolve(
        Long memberId,
        Long requestedGisuId,
        Long requestedChapterId,
        Long requestedSchoolId,
        ChallengerPart requestedPart
    ) {
        Long gisuId = requestedGisuId != null ? requestedGisuId : getGisuUseCase.getActiveGisuId();
        if (getGisuChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return AdminAnalyticsScope.superAdmin(
                gisuId,
                requestedChapterId,
                requestedSchoolId,
                requestedPart
            );
        }

        ChallengerRoleInfo role = highestRole(memberId, gisuId);

        ChallengerRoleType roleType = role.roleType();
        if (roleType.isAtLeastCentralMember()) {
            return AdminAnalyticsScope.of(
                AdminAnalyticsScopeType.CENTRAL,
                gisuId,
                requestedChapterId,
                requestedSchoolId,
                requestedPart,
                roleType
            );
        }

        if (roleType == ChallengerRoleType.CHAPTER_PRESIDENT) {
            Long chapterId = role.organizationId();
            if (requestedChapterId != null && !Objects.equals(requestedChapterId, chapterId)) {
                throwAccessDenied();
            }
            return AdminAnalyticsScope.of(
                AdminAnalyticsScopeType.CHAPTER,
                gisuId,
                chapterId,
                requestedSchoolId,
                requestedPart,
                roleType
            );
        }

        if (roleType == ChallengerRoleType.SCHOOL_PART_LEADER) {
            validateSchool(role.organizationId(), requestedSchoolId);
            validatePart(role.responsiblePart(), requestedPart);
            return AdminAnalyticsScope.of(
                AdminAnalyticsScopeType.SCHOOL_PART,
                gisuId,
                null,
                role.organizationId(),
                role.responsiblePart(),
                roleType
            );
        }

        if (roleType.isAtLeastSchoolAdmin()) {
            validateSchool(role.organizationId(), requestedSchoolId);
            return AdminAnalyticsScope.of(
                AdminAnalyticsScopeType.SCHOOL,
                gisuId,
                null,
                role.organizationId(),
                requestedPart,
                roleType
            );
        }

        throwAccessDenied();
        throw new IllegalStateException("unreachable");
    }

    public AdminAnalyticsScope resolve(Long memberId, Long requestedGisuId) {
        return resolve(memberId, requestedGisuId, null, null, null);
    }

    private ChallengerRoleInfo highestRole(Long memberId, Long gisuId) {
        List<ChallengerRoleInfo> roles = getGisuChallengerRoleUseCase.findAllByMemberId(memberId).stream()
            .filter(role -> Objects.equals(role.gisuId(), gisuId))
            .filter(role -> priority(role.roleType()) < Integer.MAX_VALUE)
            .sorted(Comparator.comparingInt(role -> priority(role.roleType())))
            .toList();

        if (roles.isEmpty()) {
            throwAccessDenied();
        }

        return roles.getFirst();
    }

    private void validateSchool(Long roleSchoolId, Long requestedSchoolId) {
        if (requestedSchoolId != null && !Objects.equals(requestedSchoolId, roleSchoolId)) {
            throwAccessDenied();
        }
    }

    private void validatePart(ChallengerPart responsiblePart, ChallengerPart requestedPart) {
        if (requestedPart != null && requestedPart != responsiblePart) {
            throwAccessDenied();
        }
    }

    private int priority(ChallengerRoleType roleType) {
        if (roleType.isAtLeastCentralMember()) {
            return 1;
        }
        if (roleType == ChallengerRoleType.CHAPTER_PRESIDENT) {
            return 2;
        }
        if (roleType == ChallengerRoleType.SCHOOL_PRESIDENT
            || roleType == ChallengerRoleType.SCHOOL_VICE_PRESIDENT
            || roleType == ChallengerRoleType.SCHOOL_ETC_ADMIN) {
            return 3;
        }
        if (roleType == ChallengerRoleType.SCHOOL_PART_LEADER) {
            return 4;
        }
        return Integer.MAX_VALUE;
    }

    private void throwAccessDenied() {
        throw new AnalyticsDomainException(AnalyticsErrorCode.RESOURCE_ACCESS_DENIED);
    }
}
