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
        return switch (roleType) {
            case CENTRAL_PRESIDENT, CENTRAL_VICE_PRESIDENT,
                CENTRAL_OPERATING_TEAM_MEMBER, CENTRAL_EDUCATION_TEAM_MEMBER -> AdminAnalyticsScope.of(
                    AdminAnalyticsScopeType.CENTRAL,
                    gisuId,
                    requestedChapterId,
                    requestedSchoolId,
                    requestedPart,
                    roleType
                );
            case CHAPTER_PRESIDENT -> {
                Long chapterId = role.organizationId();
                if (requestedChapterId != null && !Objects.equals(requestedChapterId, chapterId)) {
                    throw accessDenied();
                }
                yield AdminAnalyticsScope.of(
                    AdminAnalyticsScopeType.CHAPTER,
                    gisuId,
                    chapterId,
                    requestedSchoolId,
                    requestedPart,
                    roleType
                );
            }
            case SCHOOL_PART_LEADER -> {
                validateSchool(role.organizationId(), requestedSchoolId);
                validatePart(role.responsiblePart(), requestedPart);
                yield AdminAnalyticsScope.of(
                    AdminAnalyticsScopeType.SCHOOL_PART,
                    gisuId,
                    null,
                    role.organizationId(),
                    role.responsiblePart(),
                    roleType
                );
            }
            case SCHOOL_PRESIDENT, SCHOOL_VICE_PRESIDENT, SCHOOL_ETC_ADMIN -> {
                validateSchool(role.organizationId(), requestedSchoolId);
                yield AdminAnalyticsScope.of(
                    AdminAnalyticsScopeType.SCHOOL,
                    gisuId,
                    null,
                    role.organizationId(),
                    requestedPart,
                    roleType
                );
            }
        };
    }

    public AdminAnalyticsScope resolve(Long memberId, Long requestedGisuId) {
        return resolve(memberId, requestedGisuId, null, null, null);
    }

    private ChallengerRoleInfo highestRole(Long memberId, Long gisuId) {
        List<ChallengerRoleInfo> roles = getGisuChallengerRoleUseCase.findAllByMemberId(memberId).stream()
            .filter(role -> Objects.equals(role.gisuId(), gisuId))
            .sorted(Comparator.comparingInt(role -> priority(role.roleType())))
            .toList();

        if (roles.isEmpty()) {
            throw accessDenied();
        }

        return roles.getFirst();
    }

    private void validateSchool(Long roleSchoolId, Long requestedSchoolId) {
        if (requestedSchoolId != null && !Objects.equals(requestedSchoolId, roleSchoolId)) {
            throw accessDenied();
        }
    }

    private void validatePart(ChallengerPart responsiblePart, ChallengerPart requestedPart) {
        if (requestedPart != null && requestedPart != responsiblePart) {
            throw accessDenied();
        }
    }

    private int priority(ChallengerRoleType roleType) {
        return switch (roleType) {
            case CENTRAL_PRESIDENT, CENTRAL_VICE_PRESIDENT,
                CENTRAL_OPERATING_TEAM_MEMBER, CENTRAL_EDUCATION_TEAM_MEMBER -> 1;
            case CHAPTER_PRESIDENT -> 2;
            case SCHOOL_PRESIDENT, SCHOOL_VICE_PRESIDENT, SCHOOL_ETC_ADMIN -> 3;
            case SCHOOL_PART_LEADER -> 4;
        };
    }

    private AnalyticsDomainException accessDenied() {
        return new AnalyticsDomainException(AnalyticsErrorCode.RESOURCE_ACCESS_DENIED);
    }
}
