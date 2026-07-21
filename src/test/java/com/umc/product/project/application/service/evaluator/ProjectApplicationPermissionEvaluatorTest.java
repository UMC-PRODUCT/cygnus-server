package com.umc.product.project.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationPermissionEvaluatorTest {

    private static final Long PROJECT_GISU_ID = 1L;
    private static final Long PROJECT_CHAPTER_ID = 1L;
    private static final Long PROJECT_ID = 100L;
    private static final Long PO_MEMBER_ID = 10L;
    private static final Long APPLICANT_MEMBER_ID = 20L;
    private static final Long APPLICATION_ID = 500L;

    @Mock
    LoadProjectPort loadProjectPort;
    @Mock
    LoadProjectApplicationPort loadProjectApplicationPort;
    @Mock
    LoadProjectMemberPort loadProjectMemberPort;

    ProjectApplicationPermissionEvaluator sut;

    @BeforeEach
    void setUp() {
        // 기본은 플래그 OFF. DRAFT 단건 노출 플래그를 켜야 하는 테스트는 sutWithDraftRead() 로 별도 생성.
        sut = newSut(false);
    }

    private ProjectApplicationPermissionEvaluator newSut(boolean allowDraftRead) {
        return new ProjectApplicationPermissionEvaluator(
            loadProjectPort, loadProjectApplicationPort, loadProjectMemberPort,
            new SuperAdminProperties(allowDraftRead));
    }

    @Test
    void supportedResourceType은_PROJECT_APPLICATION을_반환한다() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.PROJECT_APPLICATION);
    }

    // --- WRITE (resourceId = projectId) ---

    @Test
    void WRITE는_같은_기수_챌린저_허용() {
        givenProject(ProjectStatus.IN_PROGRESS);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID,
            List.of(gisuInfo(PROJECT_GISU_ID, PROJECT_CHAPTER_ID, ChallengerPart.SPRINGBOOT, 99L)),
            List.of());

        assertThat(sut.evaluate(subject, writePermission())).isTrue();
    }

    @Test
    void WRITE는_PO_본인이라도_기수_매칭하면_evaluator는_통과_자기지원_차단은_도메인에서() {
        givenProject(ProjectStatus.IN_PROGRESS);
        SubjectAttributes subject = subjectWith(PO_MEMBER_ID,
            List.of(gisuInfo(PROJECT_GISU_ID, PROJECT_CHAPTER_ID, ChallengerPart.PLAN, 99L)),
            List.of());

        assertThat(sut.evaluate(subject, writePermission())).isTrue();
    }

    @Test
    void WRITE는_다른_기수_챌린저_거부() {
        givenProject(ProjectStatus.IN_PROGRESS);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID,
            List.of(gisuInfo(99L, PROJECT_CHAPTER_ID, ChallengerPart.SPRINGBOOT, 99L)),
            List.of());

        assertThat(sut.evaluate(subject, writePermission())).isFalse();
    }

    @Test
    void WRITE는_챌린저_정보_없으면_거부() {
        givenProject(ProjectStatus.IN_PROGRESS);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, writePermission())).isFalse();
    }

    // --- READ (resourceId 없으면 통과 / 있으면 단건 검증) ---

    @Test
    void READ는_resourceId_없으면_무조건_허용() {
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());
        ResourcePermission permission = ResourcePermission.ofType(ResourceType.PROJECT_APPLICATION,
            PermissionType.READ);

        assertThat(sut.evaluate(subject, permission)).isTrue();
    }

    @Test
    void READ는_DRAFT_지원서를_본인_허용() {
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    void READ는_DRAFT_지원서를_PO여도_거부() {
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = subjectWith(PO_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, readPermission())).isFalse();
    }

    @Test
    void READ는_DRAFT_지원서를_플래그_OFF면_SUPER_ADMIN도_거부() {
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = superAdminSubject(30L);

        assertThat(sut.evaluate(subject, readPermission())).isFalse();
    }

    @Test
    void READ는_DRAFT_지원서를_플래그_ON이면_SUPER_ADMIN_허용() {
        sut = newSut(true);
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = superAdminSubject(30L);

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    void READ는_DRAFT_지원서를_플래그_ON이라도_비_SUPER_ADMIN_총괄단_거부() {
        sut = newSut(true);
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = subjectWith(30L, List.of(),
            List.of(centralCoreRoleInGisu(PROJECT_GISU_ID)));

        assertThat(sut.evaluate(subject, readPermission())).isFalse();
    }

    @Test
    void READ는_DRAFT_지원서를_플래그_ON이라도_본인은_플래그_무관_허용() {
        sut = newSut(true);
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    void READ는_SUBMITTED_지원서를_본인_허용() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    void READ는_SUBMITTED_지원서를_부모_PO_허용() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        SubjectAttributes subject = subjectWith(PO_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    void READ는_SUBMITTED_지원서를_보조PM_허용() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        Long subPmId = 30L;
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, subPmId)).willReturn(true);
        SubjectAttributes subject = subjectWith(subPmId, List.of(), List.of());

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    void READ는_SUBMITTED_지원서를_외부인_거부() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        Long outsiderId = 30L;
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, outsiderId)).willReturn(false);
        SubjectAttributes subject = subjectWith(outsiderId, List.of(), List.of());

        assertThat(sut.evaluate(subject, readPermission())).isFalse();
    }

    @Test
    void READ는_SUBMITTED_지원서를_해당_기수_총괄단_허용() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        Long outsiderId = 30L;
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, outsiderId)).willReturn(false);
        SubjectAttributes subject = subjectWith(outsiderId, List.of(),
            List.of(centralCoreRoleInGisu(PROJECT_GISU_ID)));

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    void READ는_SUBMITTED_지원서를_다른_기수_총괄단_거부() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        Long outsiderId = 30L;
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, outsiderId)).willReturn(false);
        SubjectAttributes subject = subjectWith(outsiderId, List.of(),
            List.of(centralCoreRoleInGisu(99L)));

        assertThat(sut.evaluate(subject, readPermission())).isFalse();
    }

    @Test
    void READ는_SUBMITTED_지원서를_SUPER_ADMIN_허용_기수_무관() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        Long outsiderId = 30L;
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, outsiderId)).willReturn(false);
        SubjectAttributes subject = superAdminSubject(outsiderId);

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    void READ는_SUBMITTED_지원서를_해당_기수_해당_지부_지부장_허용() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        Long outsiderId = 30L;
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, outsiderId)).willReturn(false);
        SubjectAttributes subject = subjectWith(outsiderId, List.of(),
            List.of(chapterPresidentRole(PROJECT_CHAPTER_ID, PROJECT_GISU_ID)));

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    @Test
    void READ는_SUBMITTED_지원서를_다른_지부_지부장_거부() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        Long outsiderId = 30L;
        given(loadProjectMemberPort.isActivePlanMember(PROJECT_ID, outsiderId)).willReturn(false);
        SubjectAttributes subject = subjectWith(outsiderId, List.of(),
            List.of(chapterPresidentRole(2L, PROJECT_GISU_ID)));

        assertThat(sut.evaluate(subject, readPermission())).isFalse();
    }

    @Test
    void READ는_APPROVED_지원서를_본인_허용() {
        givenApplication(ProjectApplicationStatus.APPROVED);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    // --- EDIT ---

    @Test
    void EDIT는_DRAFT_지원서를_본인_허용() {
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, editPermission())).isTrue();
    }

    @Test
    void EDIT는_SUBMITTED_지원서를_본인이라도_거부() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, editPermission())).isFalse();
    }

    @Test
    void EDIT는_DRAFT_지원서를_타인_거부() {
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = subjectWith(PO_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, editPermission())).isFalse();
    }

    // --- DELETE ---

    @Test
    void DELETE는_본인_DRAFT_허용() {
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, deletePermission())).isTrue();
    }

    @Test
    void DELETE는_본인_SUBMITTED_허용() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, deletePermission())).isTrue();
    }

    @Test
    void DELETE는_본인_APPROVED_거부() {
        givenApplication(ProjectApplicationStatus.APPROVED);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, deletePermission())).isFalse();
    }

    @Test
    void DELETE는_본인_REJECTED_거부() {
        givenApplication(ProjectApplicationStatus.REJECTED);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, deletePermission())).isFalse();
    }

    @Test
    void DELETE는_타인_거부() {
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = subjectWith(PO_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, deletePermission())).isFalse();
    }

    @Test
    void DELETE는_본인이라도_CANCELLED는_거부_이중취소_차단() {
        givenApplication(ProjectApplicationStatus.CANCELLED);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, deletePermission())).isFalse();
    }

    @Test
    void EDIT는_본인이라도_CANCELLED는_거부() {
        givenApplication(ProjectApplicationStatus.CANCELLED);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, editPermission())).isFalse();
    }

    @Test
    void READ는_CANCELLED_지원서를_본인_허용() {
        givenApplication(ProjectApplicationStatus.CANCELLED);
        SubjectAttributes subject = subjectWith(APPLICANT_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, readPermission())).isTrue();
    }

    // --- APPROVE ---

    @Test
    void APPROVE는_부모_PO_SUBMITTED_허용() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        SubjectAttributes subject = subjectWith(PO_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, approvePermission())).isTrue();
    }

    @Test
    void APPROVE는_부모_PO_DRAFT_거부() {
        givenApplication(ProjectApplicationStatus.DRAFT);
        SubjectAttributes subject = subjectWith(PO_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, approvePermission())).isFalse();
    }

    @Test
    void APPROVE는_부모_PO_APPROVED_허용_재토글_가능() {
        givenApplication(ProjectApplicationStatus.APPROVED);
        SubjectAttributes subject = subjectWith(PO_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, approvePermission())).isTrue();
    }

    @Test
    void APPROVE는_부모_PO_REJECTED_허용_재토글_가능() {
        givenApplication(ProjectApplicationStatus.REJECTED);
        SubjectAttributes subject = subjectWith(PO_MEMBER_ID, List.of(), List.of());

        assertThat(sut.evaluate(subject, approvePermission())).isTrue();
    }

    @Test
    void APPROVE는_보조PM_거부_PO만_가능() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        Long subPmId = 30L;
        SubjectAttributes subject = subjectWith(subPmId, List.of(), List.of());

        assertThat(sut.evaluate(subject, approvePermission())).isFalse();
    }

    @Test
    void APPROVE는_총괄단도_거부_자동매칭은_권한_외() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        SubjectAttributes subject = subjectWith(30L, List.of(),
            List.of(centralCoreRoleInGisu(PROJECT_GISU_ID)));

        assertThat(sut.evaluate(subject, approvePermission())).isFalse();
    }

    @Test
    void APPROVE는_SUPER_ADMIN_허용_기수_무관() {
        givenApplication(ProjectApplicationStatus.SUBMITTED);
        SubjectAttributes subject = superAdminSubject(30L);

        assertThat(sut.evaluate(subject, approvePermission())).isTrue();
    }

    // --- helpers ---

    private void givenProject(ProjectStatus status) {
        given(loadProjectPort.findById(PROJECT_ID))
            .willReturn(Optional.of(project(PROJECT_ID, PO_MEMBER_ID, status)));
    }

    private void givenApplication(ProjectApplicationStatus status) {
        Project project = project(PROJECT_ID, PO_MEMBER_ID, ProjectStatus.IN_PROGRESS);
        ProjectApplicationForm form = applicationForm(project);
        ProjectApplication application = application(APPLICATION_ID, APPLICANT_MEMBER_ID, status, form);
        given(loadProjectApplicationPort.findById(APPLICATION_ID)).willReturn(Optional.of(application));
    }

    private ResourcePermission writePermission() {
        return ResourcePermission.of(ResourceType.PROJECT_APPLICATION, PROJECT_ID, PermissionType.WRITE);
    }

    private ResourcePermission readPermission() {
        return ResourcePermission.of(ResourceType.PROJECT_APPLICATION, APPLICATION_ID, PermissionType.READ);
    }

    private ResourcePermission editPermission() {
        return ResourcePermission.of(ResourceType.PROJECT_APPLICATION, APPLICATION_ID, PermissionType.EDIT);
    }

    private ResourcePermission deletePermission() {
        return ResourcePermission.of(ResourceType.PROJECT_APPLICATION, APPLICATION_ID, PermissionType.DELETE);
    }

    private ResourcePermission approvePermission() {
        return ResourcePermission.of(ResourceType.PROJECT_APPLICATION, APPLICATION_ID, PermissionType.APPROVE);
    }

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

    private Project project(Long id, Long ownerMemberId, ProjectStatus status) {
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Project project = constructor.newInstance();
            ReflectionTestUtils.setField(project, "id", id);
            ReflectionTestUtils.setField(project, "productOwnerMemberId", ownerMemberId);
            ReflectionTestUtils.setField(project, "creatorMemberId", ownerMemberId);
            ReflectionTestUtils.setField(project, "status", status);
            ReflectionTestUtils.setField(project, "gisuId", PROJECT_GISU_ID);
            ReflectionTestUtils.setField(project, "chapterId", PROJECT_CHAPTER_ID);
            return project;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProjectApplicationForm applicationForm(Project project) {
        try {
            var constructor = ProjectApplicationForm.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ProjectApplicationForm form = constructor.newInstance();
            ReflectionTestUtils.setField(form, "id", 200L);
            ReflectionTestUtils.setField(form, "project", project);
            ReflectionTestUtils.setField(form, "formId", 300L);
            return form;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProjectApplication application(Long id, Long applicantMemberId,
                                           ProjectApplicationStatus status,
                                           ProjectApplicationForm form) {
        try {
            var constructor = ProjectApplication.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ProjectApplication application = constructor.newInstance();
            ReflectionTestUtils.setField(application, "id", id);
            ReflectionTestUtils.setField(application, "applicantMemberId", applicantMemberId);
            ReflectionTestUtils.setField(application, "status", status);
            ReflectionTestUtils.setField(application, "applicationForm", form);
            return application;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
