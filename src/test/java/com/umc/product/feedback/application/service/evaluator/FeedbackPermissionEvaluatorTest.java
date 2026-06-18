package com.umc.product.feedback.application.service.evaluator;

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
import com.umc.product.common.domain.enums.OrganizationType;

@DisplayName("FeedbackPermissionEvaluator")
class FeedbackPermissionEvaluatorTest {

    private final FeedbackPermissionEvaluator sut = new FeedbackPermissionEvaluator();

    @Test
    @DisplayName("supportedResourceType은 FEEDBACK을 반환한다")
    void supportedResourceType은_FEEDBACK을_반환한다() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.FEEDBACK);
    }

    @Test
    @DisplayName("SUPER_ADMIN은 READ 권한을 통과한다")
    void SUPER_ADMIN_READ_허용() {
        SubjectAttributes subject = subjectWithSystemRoles(SystemRoleType.SUPER_ADMIN);
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.FEEDBACK, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 MANAGE 권한을 통과한다")
    void SUPER_ADMIN_MANAGE_허용() {
        SubjectAttributes subject = subjectWithSystemRoles(SystemRoleType.SUPER_ADMIN);
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.FEEDBACK, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    @DisplayName("중앙운영진은 READ 권한을 거부한다")
    void 중앙운영진_READ_거부() {
        SubjectAttributes subject = subjectWithRoles(roleOf(ChallengerRoleType.CENTRAL_PRESIDENT));
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.FEEDBACK, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    @DisplayName("역할이 없는 사용자는 MANAGE 권한을 거부한다")
    void 역할_없음_MANAGE_거부() {
        SubjectAttributes subject = subjectWithRoles();
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.FEEDBACK, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    private SubjectAttributes subjectWithRoles(RoleAttribute... roles) {
        return SubjectAttributes.builder()
            .memberId(1L)
            .schoolId(1L)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of(roles))
            .build();
    }

    private SubjectAttributes subjectWithSystemRoles(SystemRoleType... roles) {
        return SubjectAttributes.builder()
            .memberId(1L)
            .schoolId(1L)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of())
            .systemRoles(Set.of(roles))
            .build();
    }

    private RoleAttribute roleOf(ChallengerRoleType roleType) {
        return new RoleAttribute(roleType, OrganizationType.CENTRAL, null, null, 1L);
    }
}
