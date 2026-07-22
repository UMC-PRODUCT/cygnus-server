package com.umc.product.project.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;

@ExtendWith(MockitoExtension.class)
class ProjectPermissionEvaluatorTest {

    @Mock
    LoadProjectPort loadProjectPort;

    ProjectPermissionEvaluator sut;

    @BeforeEach
    void setUp() {
        // 기본은 플래그 OFF. DRAFT 단건 노출 플래그를 켜야 하는 테스트는 sut = newSut(true) 로 재생성.
        sut = newSut(false);
    }

    private ProjectPermissionEvaluator newSut(boolean allowDraftRead) {
        return new ProjectPermissionEvaluator(loadProjectPort, new SuperAdminProperties(allowDraftRead));
    }

    @Test
    void supportedResourceType은_PROJECT를_반환한다() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.PROJECT);
    }

    @Test
    void 지원하지_않는_권한은_fail_closed한다() {
        SubjectAttributes subject = subjectWith(1L, List.of(), List.of());
        ResourcePermission permission = org.mockito.Mockito.mock(ResourcePermission.class);
        given(permission.permission()).willReturn(PermissionType.RELEASE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    // --- READ (목록 — resourceId 없음) ---

    @Test
    void READ는_resourceId_없으면_무조건_허용() {
        SubjectAttributes subject = subjectWith(1L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    // --- READ (단건) ---

    @Test
    void READ는_IN_PROGRESS_프로젝트를_누구나_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.IN_PROGRESS)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_COMPLETED_프로젝트를_누구나_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.COMPLETED)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_DRAFT_프로젝트를_작성자만_허용() {
        Long memberId = 10L;
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, memberId, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(memberId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_DRAFT_프로젝트를_작성자가_아니면_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void READ는_DRAFT_프로젝트를_중앙총괄이라도_작성자_아니면_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void READ는_DRAFT_프로젝트를_플래그_OFF면_SUPER_ADMIN도_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = superAdminSubject(20L);
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void READ는_DRAFT_프로젝트를_플래그_ON이면_SUPER_ADMIN_허용() {
        sut = newSut(true);
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = superAdminSubject(20L);
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_DRAFT_프로젝트를_플래그_ON이라도_비_SUPER_ADMIN_중앙총괄_거부() {
        sut = newSut(true);
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void READ는_DRAFT_프로젝트를_플래그_ON이라도_작성자는_플래그_무관_허용() {
        sut = newSut(true);
        Long memberId = 10L;
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, memberId, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(memberId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_PENDING_REVIEW_프로젝트를_작성자_허용() {
        Long memberId = 10L;
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, memberId, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(memberId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_PENDING_REVIEW_프로젝트를_중앙총괄_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_PENDING_REVIEW_프로젝트를_외부인_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void READ는_PENDING_REVIEW_프로젝트를_지부장_허용_scope_무관() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        // 본 프로젝트의 chapterId=1 인데 지부장은 다른 지부(2) — 일반 조회는 scope 무관 통과
        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(2L, 1L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_PENDING_REVIEW_프로젝트를_학교_회장단_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(schoolPresidentRole(1L, 1L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void READ는_ABORTED_프로젝트를_작성자_또는_중앙총괄_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.ABORTED)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_ABORTED_프로젝트를_외부인_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.ABORTED)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void READ는_ABORTED_프로젝트를_지부장_허용_scope_무관() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.ABORTED)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(2L, 1L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_PENDING_REVIEW_프로젝트를_이전_기수_지부장_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        // 프로젝트는 gisuId=1, 지원자의 지부장 role 은 과거 기수(gisuId=99)
        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(1L, 99L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void READ는_PENDING_REVIEW_프로젝트를_이전_기수_중앙총괄_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        // 프로젝트는 gisuId=1, 호출자의 총괄 role 은 과거 기수(gisuId=99)
        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRoleInGisu(99L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void READ는_PENDING_REVIEW_프로젝트를_SUPER_ADMIN_허용_기수_무관() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = superAdminSubject(20L);
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    // --- WRITE ---

    @Test
    void WRITE는_PLAN_파트_챌린저_허용() {
        SubjectAttributes subject = subjectWith(1L,
            List.of(gisuInfo(1L, 1L, ChallengerPart.PLAN, 1L)),
            List.of());
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.WRITE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void WRITE는_비PLAN_파트_거부() {
        SubjectAttributes subject = subjectWith(1L,
            List.of(gisuInfo(1L, 1L, ChallengerPart.SPRINGBOOT, 1L)),
            List.of());
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.WRITE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void WRITE는_챌린저_정보_없으면_거부() {
        SubjectAttributes subject = subjectWith(1L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.WRITE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void WRITE는_총괄단_허용() {
        SubjectAttributes subject = subjectWith(1L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.WRITE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void WRITE는_SUPER_ADMIN_허용() {
        SubjectAttributes subject = superAdminSubject(20L);
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.WRITE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void WRITE는_지부장_허용() {
        SubjectAttributes subject = subjectWith(1L, List.of(),
            List.of(chapterPresidentRole(1L, 1L)));
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.WRITE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void WRITE는_학교_회장단_허용() {
        SubjectAttributes subject = subjectWith(1L, List.of(),
            List.of(schoolPresidentRole(1L, 1L)));
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.WRITE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    // --- EDIT ---

    @Test
    void EDIT은_DRAFT에서_작성자만_허용() {
        Long memberId = 10L;
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, memberId, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(memberId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void EDIT은_DRAFT에서_작성자가_아니면_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_DRAFT에서_중앙총괄이라도_작성자가_아니면_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_PENDING_REVIEW에서_작성자_허용() {
        Long memberId = 10L;
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, memberId, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(memberId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void EDIT은_PENDING_REVIEW에서_중앙총괄_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void EDIT은_IN_PROGRESS에서_작성자_허용() {
        Long memberId = 10L;
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, memberId, ProjectStatus.IN_PROGRESS)));

        SubjectAttributes subject = subjectWith(memberId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void EDIT은_IN_PROGRESS에서_중앙총괄_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.IN_PROGRESS)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void EDIT은_PENDING_REVIEW에서_본인_지부장_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(1L, 1L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void EDIT은_DRAFT에서_creator_허용() {
        Long ownerId = 10L;
        Long creatorId = 20L;
        Long projectId = 100L;
        Project p = project(projectId, ownerId, ProjectStatus.DRAFT);
        ReflectionTestUtils.setField(p, "creatorMemberId", creatorId);
        given(loadProjectPort.findById(projectId)).willReturn(Optional.of(p));

        SubjectAttributes subject = subjectWith(creatorId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void EDIT은_PENDING_REVIEW에서_creator는_거부() {
        Long ownerId = 10L;
        Long creatorId = 20L;
        Long projectId = 100L;
        Project p = project(projectId, ownerId, ProjectStatus.PENDING_REVIEW);
        ReflectionTestUtils.setField(p, "creatorMemberId", creatorId);
        given(loadProjectPort.findById(projectId)).willReturn(Optional.of(p));

        SubjectAttributes subject = subjectWith(creatorId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_DRAFT에서는_creator_아니면_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(1L, 1L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_COMPLETED에서_작성자라도_거부() {
        Long memberId = 10L;
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, memberId, ProjectStatus.COMPLETED)));

        SubjectAttributes subject = subjectWith(memberId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_COMPLETED에서_중앙총괄도_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.COMPLETED)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_ABORTED에서_거부() {
        Long memberId = 10L;
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, memberId, ProjectStatus.ABORTED)));

        SubjectAttributes subject = subjectWith(memberId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_프로젝트가_없으면_예외() {
        Long projectId = 999L;
        given(loadProjectPort.findById(projectId)).willReturn(Optional.empty());

        SubjectAttributes subject = subjectWith(1L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThatThrownBy(() -> sut.evaluate(subject, permission))
            .isInstanceOf(ProjectDomainException.class);
    }

    @Test
    void EDIT은_DRAFT에서_PO라도_creator가_아니면_거부() {
        Long ownerId = 10L;
        Long creatorId = 20L;
        Long projectId = 100L;
        Project p = project(projectId, ownerId, ProjectStatus.DRAFT);
        ReflectionTestUtils.setField(p, "creatorMemberId", creatorId);
        given(loadProjectPort.findById(projectId)).willReturn(Optional.of(p));

        SubjectAttributes subject = subjectWith(ownerId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_IN_PROGRESS에서_본인_지부장_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.IN_PROGRESS)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(1L, 1L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void EDIT은_PENDING_REVIEW에서_다른_지부의_지부장_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(2L, 1L)));   // 본 프로젝트의 chapterId=1
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_PENDING_REVIEW에서_이전_기수_중앙총괄_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRoleInGisu(99L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_PENDING_REVIEW에서_이전_기수_지부장_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(1L, 99L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void EDIT은_ABORTED에서_운영진도_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.ABORTED)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.EDIT);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    // --- MANAGE (publish/abort/complete 등 운영진 전용 상태 전이) ---

    @Test
    void MANAGE는_PENDING_REVIEW에서_PM이라도_거부() {
        Long memberId = 10L;
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, memberId, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(memberId, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void MANAGE는_PENDING_REVIEW에서_중앙총괄_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void MANAGE는_PENDING_REVIEW에서_본인_지부장_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(1L, 1L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void MANAGE는_다른_지부의_지부장이면_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(2L, 1L)));   // 본 프로젝트의 chapterId=1
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void MANAGE는_이전_기수_중앙총괄_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        // 프로젝트는 gisuId=1, 호출자의 총괄 role 은 과거 기수(gisuId=99)
        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRoleInGisu(99L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void MANAGE는_이전_기수_지부장_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        // chapterId 는 일치하지만 gisuId 가 과거 기수
        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(1L, 99L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void MANAGE는_DRAFT에서는_누구도_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void MANAGE는_COMPLETED에서는_누구도_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.COMPLETED)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.MANAGE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    // --- DELETE ---

    @Test
    void DELETE는_DRAFT_프로젝트를_PO_본인이면_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(10L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.DELETE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void DELETE는_PENDING_REVIEW_프로젝트를_PO_본인이면_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.PENDING_REVIEW)));

        SubjectAttributes subject = subjectWith(10L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.DELETE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void DELETE는_DRAFT_프로젝트를_본인_지부장이면_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(1L, 1L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.DELETE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void DELETE는_DRAFT_프로젝트를_해당_기수_중앙총괄단이면_허용() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.DELETE);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void DELETE는_DRAFT_프로젝트를_다른_지부장이면_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(),
            List.of(chapterPresidentRole(999L, 1L)));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.DELETE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void DELETE는_DRAFT_프로젝트를_일반_사용자면_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.DRAFT)));

        SubjectAttributes subject = subjectWith(20L, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.DELETE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void DELETE는_IN_PROGRESS_프로젝트에는_누구도_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.IN_PROGRESS)));

        SubjectAttributes subject = subjectWith(10L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.DELETE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    @Test
    void DELETE는_COMPLETED_프로젝트에는_누구도_거부() {
        Long projectId = 100L;
        given(loadProjectPort.findById(projectId))
            .willReturn(Optional.of(project(projectId, 10L, ProjectStatus.COMPLETED)));

        SubjectAttributes subject = subjectWith(10L, List.of(), List.of(centralCoreRole()));
        ResourcePermission permission = ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.DELETE);

        assertThat(sut.evaluate(subject, permission)).isFalse();
    }

    // --- helpers ---

    private SubjectAttributes subjectWith(Long memberId,
                                          List<GisuChallengerInfo> gisuInfos,
                                          List<RoleAttribute> roles) {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(1L)
            .gisuChallengerInfos(gisuInfos)
            .roleAttributes(roles)
            .build();
    }

    private SubjectAttributes superAdminSubject(Long memberId) {
        return SubjectAttributes.builder()
            .memberId(memberId)
            .schoolId(1L)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of())
            .systemRoles(Set.of(SystemRoleType.SUPER_ADMIN))
            .build();
    }

    private GisuChallengerInfo gisuInfo(Long gisuId, Long chapterId,
                                        ChallengerPart part, Long challengerId) {
        return GisuChallengerInfo.builder()
            .gisuId(gisuId)
            .chapterId(chapterId)
            .part(part)
            .challengerId(challengerId)
            .build();
    }

    private RoleAttribute centralCoreRole() {
        return centralCoreRoleInGisu(1L);
    }

    private RoleAttribute centralCoreRoleInGisu(Long gisuId) {
        return new RoleAttribute(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            null, null, gisuId
        );
    }

    private RoleAttribute chapterPresidentRole(Long chapterId, Long gisuId) {
        return new RoleAttribute(
            ChallengerRoleType.CHAPTER_PRESIDENT,
            OrganizationType.CHAPTER,
            chapterId, null, gisuId
        );
    }

    private RoleAttribute schoolPresidentRole(Long schoolId, Long gisuId) {
        return new RoleAttribute(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            OrganizationType.SCHOOL,
            schoolId, null, gisuId
        );
    }

    private Project project(Long id, Long ownerMemberId, ProjectStatus status) {
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Project project = constructor.newInstance();
            ReflectionTestUtils.setField(project, "id", id);
            ReflectionTestUtils.setField(project, "productOwnerMemberId", ownerMemberId);
            ReflectionTestUtils.setField(project, "creatorMemberId", ownerMemberId);
            ReflectionTestUtils.setField(project, "status", status);
            ReflectionTestUtils.setField(project, "gisuId", 1L);
            ReflectionTestUtils.setField(project, "chapterId", 1L);
            return project;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
