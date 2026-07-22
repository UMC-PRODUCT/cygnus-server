package com.umc.product.curriculum.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.umc.product.common.domain.exception.CommonException;

@DisplayName("Curriculum permission evaluator")
class CurriculumPermissionEvaluatorTest {

    @Test
    @DisplayName("원본 워크북 RELEASE·MANAGE는 중앙 운영진만 허용한다")
    void original_workbook_requires_central_member() {
        OriginalWorkbookPermissionEvaluator sut = new OriginalWorkbookPermissionEvaluator();
        SubjectAttributes central = subject(
            List.of(role(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER)), Set.of());
        SubjectAttributes regular = subject(List.of(), Set.of());

        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.ORIGINAL_WORKBOOK);
        assertThat(sut.evaluate(central,
            ResourcePermission.ofType(ResourceType.ORIGINAL_WORKBOOK, PermissionType.RELEASE))).isTrue();
        assertThat(sut.evaluate(central,
            ResourcePermission.ofType(ResourceType.ORIGINAL_WORKBOOK, PermissionType.MANAGE))).isTrue();
        assertThat(sut.evaluate(regular,
            ResourcePermission.ofType(ResourceType.ORIGINAL_WORKBOOK, PermissionType.MANAGE))).isFalse();
        assertThatThrownBy(() -> sut.evaluate(regular,
            ResourcePermission.ofType(ResourceType.SCHEDULE, PermissionType.WRITE)))
            .isInstanceOf(CommonException.class);
    }

    @Test
    @DisplayName("워크북 제출 READ는 SUPER_ADMIN 또는 학교 운영진만 허용하고 그 외 권한은 fail-closed한다")
    void submission_read_requires_school_admin_or_super_admin() {
        WorkbookSubmissionPermissionEvaluator sut = new WorkbookSubmissionPermissionEvaluator();
        var read = ResourcePermission.ofType(ResourceType.WORKBOOK_SUBMISSION, PermissionType.READ);

        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.WORKBOOK_SUBMISSION);
        assertThat(sut.evaluate(subject(List.of(), Set.of(SystemRoleType.SUPER_ADMIN)), read)).isTrue();
        assertThat(sut.evaluate(subject(
            List.of(role(ChallengerRoleType.SCHOOL_PART_LEADER)), Set.of()), read)).isTrue();
        assertThat(sut.evaluate(subject(List.of(), Set.of()), read)).isFalse();
        assertThatThrownBy(() -> sut.evaluate(subject(List.of(), Set.of()),
            ResourcePermission.ofType(ResourceType.SCHEDULE, PermissionType.WRITE)))
            .isInstanceOf(CommonException.class)
            .hasMessageContaining("지원하지 않는 권한");
    }

    private SubjectAttributes subject(List<RoleAttribute> roles, Set<SystemRoleType> systemRoles) {
        return SubjectAttributes.builder().roleAttributes(roles).systemRoles(systemRoles).build();
    }

    private RoleAttribute role(ChallengerRoleType roleType) {
        return new RoleAttribute(roleType, roleType.organizationType(), null, null, 9L);
    }
}
