package com.umc.product.analytics.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@DisplayName("AdminAnalyticsPermissionEvaluator")
class AdminAnalyticsPermissionEvaluatorTest {

    private final AdminAnalyticsPermissionEvaluator sut = new AdminAnalyticsPermissionEvaluator();

    @Test
    @DisplayName("ANALYTICS READ만 지원하고 SUPER_ADMIN은 항상 허용한다")
    void supports_read_and_super_admin() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.ANALYTICS);
        SubjectAttributes superAdmin = subject(List.of(), Set.of(SystemRoleType.SUPER_ADMIN));

        assertThat(sut.evaluate(superAdmin,
            ResourcePermission.ofType(ResourceType.ANALYTICS, PermissionType.READ))).isTrue();
        assertThat(sut.evaluate(superAdmin,
            ResourcePermission.ofType(ResourceType.SCHEDULE, PermissionType.WRITE))).isFalse();
    }

    @Test
    @DisplayName("중앙·지부·학교 운영진만 허용하고 일반 역할은 fail-closed 한다")
    void permits_only_admin_roles() {
        for (ChallengerRoleType roleType : List.of(
            ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER,
            ChallengerRoleType.CHAPTER_PRESIDENT,
            ChallengerRoleType.SCHOOL_PART_LEADER
        )) {
            assertThat(sut.evaluate(subject(List.of(role(roleType)), Set.of()), read())).isTrue();
        }
        assertThat(sut.evaluate(subject(List.of(), Set.of()), read())).isFalse();
    }

    private ResourcePermission read() {
        return ResourcePermission.ofType(ResourceType.ANALYTICS, PermissionType.READ);
    }

    private SubjectAttributes subject(List<RoleAttribute> roles, Set<SystemRoleType> systemRoles) {
        return SubjectAttributes.builder().roleAttributes(roles).systemRoles(systemRoles).build();
    }

    private RoleAttribute role(ChallengerRoleType roleType) {
        return new RoleAttribute(roleType, roleType.organizationType(), null, null, 1L);
    }
}
