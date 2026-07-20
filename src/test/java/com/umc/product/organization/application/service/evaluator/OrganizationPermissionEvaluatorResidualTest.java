package com.umc.product.organization.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;

@DisplayName("Organization 권한 evaluator 잔여 분기")
class OrganizationPermissionEvaluatorResidualTest {

    private final SubjectAttributes superAdmin = SubjectAttributes.builder()
        .memberId(1L)
        .schoolId(10L)
        .systemRoles(Set.of(SystemRoleType.SUPER_ADMIN))
        .build();

    @Test
    @DisplayName("각 evaluator는 담당 리소스 타입과 지원 권한을 정확히 판정한다")
    void 지원_리소스와_권한을_판정한다() {
        var studyGroup = new StudyGroupPermissionEvaluator();
        var school = new SchoolPermissionEvaluator();
        var chapter = new ChapterPermissionEvaluator();
        var gisu = new GisuPermissionEvaluator();

        assertThat(studyGroup.supportedResourceType()).isEqualTo(ResourceType.STUDY_GROUP);
        assertThat(studyGroup.evaluate(superAdmin, permission(ResourceType.STUDY_GROUP, PermissionType.READ))).isTrue();
        assertThat(studyGroup.evaluate(superAdmin, permission(ResourceType.STUDY_GROUP, PermissionType.WRITE))).isTrue();
        assertThat(studyGroup.evaluate(superAdmin, permission(ResourceType.STUDY_GROUP, PermissionType.EDIT))).isTrue();
        assertThat(studyGroup.evaluate(superAdmin, permission(ResourceType.STUDY_GROUP, PermissionType.DELETE))).isTrue();

        assertThat(school.supportedResourceType()).isEqualTo(ResourceType.SCHOOL);
        assertThat(school.evaluate(superAdmin, permission(ResourceType.SCHOOL, PermissionType.WRITE))).isTrue();
        assertThat(school.evaluate(superAdmin, permission(ResourceType.SCHOOL, PermissionType.EDIT))).isTrue();
        assertThat(school.evaluate(superAdmin, permission(ResourceType.SCHOOL, PermissionType.DELETE))).isTrue();

        assertThat(chapter.supportedResourceType()).isEqualTo(ResourceType.CHAPTER);
        assertThat(chapter.evaluate(superAdmin, permission(ResourceType.CHAPTER, PermissionType.WRITE))).isTrue();
        assertThat(chapter.evaluate(superAdmin, permission(ResourceType.CHAPTER, PermissionType.DELETE))).isTrue();

        assertThat(gisu.supportedResourceType()).isEqualTo(ResourceType.GISU);
        assertThat(gisu.evaluate(superAdmin, permission(ResourceType.GISU, PermissionType.WRITE))).isTrue();
        assertThat(gisu.evaluate(superAdmin, permission(ResourceType.GISU, PermissionType.EDIT))).isTrue();
        assertThat(gisu.evaluate(superAdmin, permission(ResourceType.GISU, PermissionType.DELETE))).isTrue();
    }

    @Test
    @DisplayName("구현하지 않은 권한 타입은 모든 evaluator에서 fail-closed 처리한다")
    void 미구현_권한은_거부한다() {
        ResourcePermission unsupported = mock(ResourcePermission.class);
        given(unsupported.permission()).willReturn(PermissionType.MANAGE);

        assertUnsupported(new StudyGroupPermissionEvaluator(), unsupported);
        assertUnsupported(new SchoolPermissionEvaluator(), unsupported);
        assertUnsupported(new ChapterPermissionEvaluator(), unsupported);
        assertUnsupported(new GisuPermissionEvaluator(), unsupported);
    }

    private ResourcePermission permission(ResourceType type, PermissionType permission) {
        return ResourcePermission.ofType(type, permission);
    }

    private void assertUnsupported(ResourcePermissionEvaluator evaluator, ResourcePermission unsupported) {
        assertThatThrownBy(() -> evaluator.evaluate(superAdmin, unsupported))
            .isInstanceOf(AuthorizationDomainException.class);
    }
}
