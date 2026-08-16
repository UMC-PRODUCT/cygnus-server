package com.umc.product.authorization.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.application.port.in.query.dto.GisuAuthorityScopeInfo;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.CheckMemberExistenceUseCase;

@ExtendWith(MockitoExtension.class)
class GisuAuthorityScopeQueryServiceTest {

    private static final Long MEMBER_ID = 99L;
    private static final Long GISU_ID = 1L;

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;

    @Mock
    CheckMemberExistenceUseCase checkMemberExistenceUseCase;

    @Test
    @DisplayName("기수 역할을 한 번 로드해 지부장과 학교 관리자 범위를 계산한다")
    void resolveScopedAuthority() {
        given(checkMemberExistenceUseCase.existsById(MEMBER_ID)).willReturn(true);
        given(checkPermissionUseCase.loadSubject(MEMBER_ID)).willReturn(subject(
            role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, GISU_ID),
            role(ChallengerRoleType.SCHOOL_PART_LEADER, OrganizationType.SCHOOL, 20L, GISU_ID),
            role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 30L, 2L)
        ));

        GisuAuthorityScopeInfo scope = new GisuAuthorityScopeQueryService(
            checkPermissionUseCase,
            checkMemberExistenceUseCase
        ).getByMemberIdAndGisuId(MEMBER_ID, GISU_ID);

        assertThat(scope.allSchoolsAccessible()).isFalse();
        assertThat(scope.canAccess(10L, 999L)).isTrue();
        assertThat(scope.canAccess(999L, 20L)).isTrue();
        assertThat(scope.canAccess(999L, 30L)).isFalse();
        assertThat(scope.detailedStatisticsAccessible()).isTrue();
    }

    @Test
    @DisplayName("기타 교내 운영진만 보유하면 본인 학교 접근은 허용하고 상세 통계는 제한한다")
    void restrictDetailedStatisticsForSchoolEtcAdminOnly() {
        given(checkMemberExistenceUseCase.existsById(MEMBER_ID)).willReturn(true);
        given(checkPermissionUseCase.loadSubject(MEMBER_ID)).willReturn(subject(
            role(ChallengerRoleType.SCHOOL_ETC_ADMIN, OrganizationType.SCHOOL, 20L, GISU_ID)
        ));

        GisuAuthorityScopeInfo scope = new GisuAuthorityScopeQueryService(
            checkPermissionUseCase,
            checkMemberExistenceUseCase
        ).getByMemberIdAndGisuId(MEMBER_ID, GISU_ID);

        assertThat(scope.canAccess(999L, 20L)).isTrue();
        assertThat(scope.detailedStatisticsAccessible()).isFalse();
    }

    @Test
    @DisplayName("기타 교내 운영진과 상위 학교 운영진 역할을 함께 보유하면 상세 통계를 허용한다")
    void allowDetailedStatisticsWhenHigherSchoolRoleExists() {
        given(checkMemberExistenceUseCase.existsById(MEMBER_ID)).willReturn(true);
        given(checkPermissionUseCase.loadSubject(MEMBER_ID)).willReturn(subject(
            role(ChallengerRoleType.SCHOOL_ETC_ADMIN, OrganizationType.SCHOOL, 20L, GISU_ID),
            role(ChallengerRoleType.SCHOOL_PART_LEADER, OrganizationType.SCHOOL, 20L, GISU_ID)
        ));

        GisuAuthorityScopeInfo scope = new GisuAuthorityScopeQueryService(
            checkPermissionUseCase,
            checkMemberExistenceUseCase
        ).getByMemberIdAndGisuId(MEMBER_ID, GISU_ID);

        assertThat(scope.canAccess(999L, 20L)).isTrue();
        assertThat(scope.detailedStatisticsAccessible()).isTrue();
    }

    private SubjectAttributes subject(RoleAttribute... roles) {
        return SubjectAttributes.builder()
            .roleAttributes(List.of(roles))
            .build();
    }

    private RoleAttribute role(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId,
        Long gisuId
    ) {
        return new RoleAttribute(roleType, organizationType, organizationId, null, gisuId);
    }
}
