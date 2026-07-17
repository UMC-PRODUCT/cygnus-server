package com.umc.product.project.application.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.project.application.port.in.query.GetProjectPermissionsUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionCapabilityInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.ApplicationFormPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.ApplicationPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.MemberPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.PartQuotaPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.StatisticsPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo.StatusPermissions;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionReason;

@ExtendWith(MockitoExtension.class)
@DisplayName("Project 지원 폼 owner policy")
class ProjectApplicationFormOwnerPolicyTest {

    @Mock
    GetProjectPermissionsUseCase getProjectPermissionsUseCase;

    @Test
    @DisplayName("mapping 생성 전에도 프로젝트 생성 권한이 있으면 canonical owner 등록을 허용한다")
    void mapping_생성_전에도_등록을_허용한다() {
        ProjectApplicationFormOwnerPolicy sut = new ProjectApplicationFormOwnerPolicy(
            getProjectPermissionsUseCase
        );
        FormOwnerReference owner = ProjectApplicationFormOwnerReferenceFactory.forProject(42L).create(100L);
        FormActorContext actor = FormActorContext.authenticated(7L);
        given(getProjectPermissionsUseCase.listByProjectIds(7L, List.of(42L)))
            .willReturn(List.of(permissionsWithFormCreateAllowed(42L)));

        boolean allowed = sut.allows(owner, FormOperation.MANAGE_STRUCTURE, actor);

        assertThat(allowed).isTrue();
    }

    @ParameterizedTest
    @EnumSource(FormOperation.class)
    @DisplayName("Form operation을 Project permission capability에 일대일 매핑한다")
    void operation을_Project_capability에_매핑한다(FormOperation operation) {
        ProjectApplicationFormOwnerPolicy sut = new ProjectApplicationFormOwnerPolicy(
            getProjectPermissionsUseCase
        );
        FormOwnerReference owner = ProjectApplicationFormOwnerReferenceFactory.forProject(42L).create(100L);
        FormActorContext actor = FormActorContext.authenticated(7L);
        given(getProjectPermissionsUseCase.listByProjectIds(7L, List.of(42L)))
            .willReturn(List.of(permissionsAllowing(42L, operation)));

        boolean allowed = sut.allows(owner, operation, actor);

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("권한이 없는 actor는 canonical owner라도 거부한다")
    void 권한이_없는_actor를_거부한다() {
        ProjectApplicationFormOwnerPolicy sut = new ProjectApplicationFormOwnerPolicy(
            getProjectPermissionsUseCase
        );
        FormOwnerReference owner = ProjectApplicationFormOwnerReferenceFactory.forProject(42L).create(100L);
        FormActorContext actor = FormActorContext.authenticated(8L);
        given(getProjectPermissionsUseCase.listByProjectIds(8L, List.of(42L)))
            .willReturn(List.of(ProjectPermissionInfo.notFound(42L)));

        boolean allowed = sut.allows(owner, FormOperation.MANAGE_STRUCTURE, actor);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("잘못된 namespace나 slot은 Project 권한 조회 없이 거부한다")
    void canonical하지_않은_owner_좌표를_거부한다() {
        ProjectApplicationFormOwnerPolicy sut = new ProjectApplicationFormOwnerPolicy(
            getProjectPermissionsUseCase
        );
        FormActorContext actor = FormActorContext.authenticated(7L);
        FormOwnerReference wrongNamespace = FormOwnerReference.of(
            100L, "project.other", "42", "default"
        );
        FormOwnerReference wrongSlot = FormOwnerReference.of(
            100L, "project.application-form", "42", "secondary"
        );

        boolean namespaceAllowed = sut.allows(wrongNamespace, FormOperation.READ, actor);
        boolean slotAllowed = sut.allows(wrongSlot, FormOperation.READ, actor);

        assertThat(namespaceAllowed).isFalse();
        assertThat(slotAllowed).isFalse();
        verifyNoInteractions(getProjectPermissionsUseCase);
    }

    @Test
    @DisplayName("malformed project key와 인증되지 않은 actor는 fail-closed한다")
    void malformed_key와_익명_actor를_거부한다() {
        ProjectApplicationFormOwnerPolicy sut = new ProjectApplicationFormOwnerPolicy(
            getProjectPermissionsUseCase
        );
        FormOwnerReference malformed = FormOwnerReference.of(
            100L, "project.application-form", "not-a-project-id", "default"
        );
        FormOwnerReference canonical = ProjectApplicationFormOwnerReferenceFactory.forProject(42L).create(100L);

        boolean malformedAllowed = sut.allows(
            malformed, FormOperation.READ, FormActorContext.authenticated(7L)
        );
        boolean anonymousAllowed = sut.allows(
            canonical, FormOperation.READ, FormActorContext.anonymous()
        );

        assertThat(malformedAllowed).isFalse();
        assertThat(anonymousAllowed).isFalse();
        verifyNoInteractions(getProjectPermissionsUseCase);
    }

    private static ProjectPermissionInfo permissionsWithFormCreateAllowed(Long projectId) {
        return permissionsAllowing(projectId, FormOperation.MANAGE_STRUCTURE);
    }

    private static ProjectPermissionInfo permissionsAllowing(Long projectId, FormOperation operation) {
        ProjectPermissionCapabilityInfo allowed = ProjectPermissionCapabilityInfo.allow();
        ProjectPermissionCapabilityInfo denied = ProjectPermissionCapabilityInfo.denied(
            ProjectPermissionReason.PERMISSION_DENIED
        );
        return new ProjectPermissionInfo(
            projectId,
            true,
            denied,
            denied,
            operation == FormOperation.DELETE ? allowed : denied,
            new ApplicationFormPermissions(
                operation == FormOperation.READ ? allowed : denied,
                operation == FormOperation.MANAGE_STRUCTURE ? allowed : denied,
                denied,
                denied,
                denied
            ),
            new PartQuotaPermissions(denied),
            new StatusPermissions(
                denied,
                operation == FormOperation.PUBLISH ? allowed : denied,
                denied,
                denied
            ),
            new ApplicationPermissions(
                operation == FormOperation.RESPOND ? allowed : denied,
                operation == FormOperation.READ_RESPONSES ? allowed : denied,
                denied
            ),
            new MemberPermissions(denied, denied, denied),
            new StatisticsPermissions(denied)
        );
    }
}
