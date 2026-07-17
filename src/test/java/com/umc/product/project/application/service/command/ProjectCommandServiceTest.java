package com.umc.product.project.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.project.application.port.in.command.dto.CreateDraftProjectCommand;
import com.umc.product.project.application.port.in.command.dto.SubmitProjectCommand;
import com.umc.product.project.application.port.in.command.dto.TransferProjectOwnershipCommand;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectCommand;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.SaveProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

@ExtendWith(MockitoExtension.class)
class ProjectCommandServiceTest {

    @Mock
    LoadProjectPort loadProjectPort;
    @Mock
    SaveProjectPort saveProjectPort;
    @Mock
    LoadProjectApplicationFormPort loadProjectApplicationFormPort;
    @Mock
    com.umc.product.project.application.port.out.LoadProjectPartQuotaPort loadProjectPartQuotaPort;
    @Mock
    com.umc.product.project.application.port.out.LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    com.umc.product.project.application.port.out.LoadProjectApplicationPort loadProjectApplicationPort;
    @Mock
    com.umc.product.project.application.port.out.SaveProjectMemberPort saveProjectMemberPort;
    @Mock
    com.umc.product.project.application.port.out.SaveProjectPartQuotaPort saveProjectPartQuotaPort;
    @Mock
    com.umc.product.project.application.port.out.SaveProjectApplicationFormPort saveProjectApplicationFormPort;
    @Mock
    com.umc.product.project.application.port.out.SaveProjectApplicationFormPolicyPort saveProjectApplicationFormPolicyPort;
    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    com.umc.product.form.application.port.in.command.ManageFormUseCase manageFormUseCase;

    @InjectMocks
    ProjectCommandService sut;

    private Project createProject(ProjectStatus status) {
        Project project = Project.createDraft(1L, 2L, 100L, 7L, 100L);
        ReflectionTestUtils.setField(project, "id", 1L);
        ReflectionTestUtils.setField(project, "status", status);
        return project;
    }

    private ChallengerInfo challengerInfo(Long memberId, ChallengerPart part) {
        return ChallengerInfo.builder()
            .challengerId(1L)
            .memberId(memberId)
            .gisuId(1L)
            .part(part)
            .build();
    }

    private MemberInfo memberInfo(Long schoolId) {
        return MemberInfo.builder()
            .id(100L)
            .schoolId(schoolId)
            .build();
    }

    private GisuInfo gisuInfo() {
        return new GisuInfo(1L, 9L, null, null, true);
    }

    private ChallengerRoleInfo roleInfo(ChallengerRoleType type, OrganizationType orgType, Long orgId, Long gisuId) {
        return ChallengerRoleInfo.builder()
            .id(1L)
            .challengerId(1L)
            .roleType(type)
            .organizationType(orgType)
            .organizationId(orgId)
            .responsiblePart(null)
            .gisuId(gisuId)
            .build();
    }

    // --- helpers ---

    @Nested
    class create {

        @Test
        void 프로젝트_생성_성공() {
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(100L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(any(), any())).willReturn(false);
            given(getMemberUseCase.getById(100L)).willReturn(memberInfo(10L));
            given(getChapterUseCase.byGisuAndSchool(1L, 10L)).willReturn(new ChapterInfo(5L, "서울"));
            given(saveProjectPort.save(any())).willAnswer(inv -> {
                Project p = inv.getArgument(0);
                ReflectionTestUtils.setField(p, "id", 99L);
                return p;
            });

            Long result = sut.create(command);

            assertThat(result).isEqualTo(99L);
            then(saveProjectPort).should().save(any(Project.class));
        }

        @Test
        void PLAN_파트가_아니면_예외() {
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(100L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.SPRINGBOOT));

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_OWNER_NOT_PLAN_CHALLENGER);
        }

        @Test
        void creator가_같은_기수에_DRAFT_보유시_예외() {
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(100L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(100L, 1L)).willReturn(true);

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_DRAFT_ALREADY_IN_PROGRESS);
        }

        @Test
        void admin이_creator일_때_admin_기준으로_DRAFT_보유_검증() {
            // 운영진(requester=200)이 다른 PM(productOwner=100)을 임명해 생성 시,
            // creator-DRAFT 검증은 productOwnerMemberId 가 아닌 requesterMemberId 로 수행되어야 한다.
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(200L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(200L, 1L)).willReturn(true);

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_DRAFT_ALREADY_IN_PROGRESS);
        }

        @Test
        void status_무관_PO_중복은_검사하지_않고_DRAFT_한정으로만_검사한다() {
            // PM 이 같은 기수에 PENDING_REVIEW / IN_PROGRESS / COMPLETED 등 DRAFT 가 아닌 상태의
            // 다른 프로젝트를 보유한 채 새 DRAFT 를 시작해도 차단되지 않는다.
            // status 무관 existsByOwnerAndGisu 는 호출되지 않고, DRAFT 한정 existsDraftByOwnerAndGisu 만 호출된다.
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(100L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(any(), any())).willReturn(false);
            given(loadProjectPort.existsDraftByOwnerAndGisu(100L, 1L)).willReturn(false);
            given(getMemberUseCase.getById(100L)).willReturn(memberInfo(10L));
            given(getChapterUseCase.byGisuAndSchool(1L, 10L)).willReturn(new ChapterInfo(5L, "서울"));
            given(saveProjectPort.save(any())).willAnswer(inv -> {
                Project p = inv.getArgument(0);
                ReflectionTestUtils.setField(p, "id", 99L);
                return p;
            });

            sut.create(command);

            then(loadProjectPort).should(never()).existsByOwnerAndGisu(any(), any());
            then(loadProjectPort).should().existsDraftByOwnerAndGisu(100L, 1L);
        }

        @Test
        void PO가_같은_기수에_DRAFT_보유시_예외() {
            // 운영진(requester=200)이 PM(productOwner=100)을 임명할 때, PM 이 이미 같은 기수에
            // DRAFT 프로젝트를 보유하고 있으면 creator 검증을 통과해도 owner 검증에서 막힌다.
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(200L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(200L, 1L)).willReturn(false);
            given(loadProjectPort.existsDraftByOwnerAndGisu(100L, 1L)).willReturn(true);

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_DRAFT_ALREADY_IN_PROGRESS);
        }

        @Test
        void 총괄단이_다른_PLAN_챌린저를_PO로_지정하면_성공() {
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(200L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(any(), any())).willReturn(false);
            given(getMemberUseCase.getById(100L)).willReturn(memberInfo(10L));
            given(getChapterUseCase.byGisuAndSchool(1L, 10L)).willReturn(new ChapterInfo(5L, "서울"));
            given(getChallengerRoleUseCase.findAllByMemberId(200L)).willReturn(java.util.List.of(
                roleInfo(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, 1L)
            ));
            given(saveProjectPort.save(any())).willAnswer(inv -> {
                Project p = inv.getArgument(0);
                ReflectionTestUtils.setField(p, "id", 99L);
                return p;
            });

            Long result = sut.create(command);

            assertThat(result).isEqualTo(99L);
        }

        @Test
        void 지부장이_본인_지부_PLAN_챌린저를_PO로_지정하면_성공() {
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(200L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(any(), any())).willReturn(false);
            given(getMemberUseCase.getById(100L)).willReturn(memberInfo(10L));
            given(getChapterUseCase.byGisuAndSchool(1L, 10L)).willReturn(new ChapterInfo(5L, "서울"));
            given(getChallengerRoleUseCase.findAllByMemberId(200L)).willReturn(java.util.List.of(
                roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 5L, 1L)
            ));
            given(saveProjectPort.save(any())).willAnswer(inv -> {
                Project p = inv.getArgument(0);
                ReflectionTestUtils.setField(p, "id", 99L);
                return p;
            });

            Long result = sut.create(command);

            assertThat(result).isEqualTo(99L);
        }

        @Test
        void 지부장이_다른_지부의_PLAN_챌린저를_PO로_지정하면_거부() {
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(200L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(any(), any())).willReturn(false);
            given(getMemberUseCase.getById(100L)).willReturn(memberInfo(10L));
            given(getChapterUseCase.byGisuAndSchool(1L, 10L)).willReturn(new ChapterInfo(5L, "서울"));
            given(getChallengerRoleUseCase.findAllByMemberId(200L)).willReturn(java.util.List.of(
                roleInfo(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 99L, 1L)
            ));

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }

        @Test
        void 학교_회장이_본인_학교_PLAN_챌린저를_PO로_지정하면_성공() {
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(200L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(any(), any())).willReturn(false);
            given(getMemberUseCase.getById(100L)).willReturn(memberInfo(10L));
            given(getChapterUseCase.byGisuAndSchool(1L, 10L)).willReturn(new ChapterInfo(5L, "서울"));
            given(getChallengerRoleUseCase.findAllByMemberId(200L)).willReturn(java.util.List.of(
                roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 10L, 1L)
            ));
            given(saveProjectPort.save(any())).willAnswer(inv -> {
                Project p = inv.getArgument(0);
                ReflectionTestUtils.setField(p, "id", 99L);
                return p;
            });

            Long result = sut.create(command);

            assertThat(result).isEqualTo(99L);
        }

        @Test
        void 학교_회장이_다른_학교의_PLAN_챌린저를_PO로_지정하면_거부() {
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(200L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(any(), any())).willReturn(false);
            given(getMemberUseCase.getById(100L)).willReturn(memberInfo(10L));
            given(getChapterUseCase.byGisuAndSchool(1L, 10L)).willReturn(new ChapterInfo(5L, "서울"));
            given(getChallengerRoleUseCase.findAllByMemberId(200L)).willReturn(java.util.List.of(
                roleInfo(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 99L, 1L)
            ));

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }

        @Test
        void 일반_PLAN_챌린저가_다른_PLAN_챌린저를_PO로_지정하면_거부() {
            var command = CreateDraftProjectCommand.builder()
                .gisuId(1L)
                .productOwnerMemberId(100L)
                .requesterMemberId(200L)
                .build();

            given(getGisuUseCase.getById(1L)).willReturn(gisuInfo());
            given(getChallengerUseCase.getByMemberIdAndGisuId(100L, 1L))
                .willReturn(challengerInfo(100L, ChallengerPart.PLAN));
            given(loadProjectPort.existsDraftByCreatorAndGisu(any(), any())).willReturn(false);
            given(getMemberUseCase.getById(100L)).willReturn(memberInfo(10L));
            given(getChapterUseCase.byGisuAndSchool(1L, 10L)).willReturn(new ChapterInfo(5L, "서울"));
            given(getChallengerRoleUseCase.findAllByMemberId(200L)).willReturn(java.util.List.of());

            assertThatThrownBy(() -> sut.create(command))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }
    }

    @Nested
    class update {

        @Test
        void DRAFT_상태_수정_성공() {
            Project project = createProject(ProjectStatus.DRAFT);
            given(loadProjectPort.getById(1L)).willReturn(project);

            sut.update(updateCommand("새이름"));

            assertThat(project.getName()).isEqualTo("새이름");
        }

        @Test
        void PENDING_REVIEW_상태_수정_성공() {
            Project project = createProject(ProjectStatus.PENDING_REVIEW);
            given(loadProjectPort.getById(1L)).willReturn(project);

            sut.update(updateCommand("새이름"));

            assertThat(project.getName()).isEqualTo("새이름");
        }

        @Test
        void IN_PROGRESS_상태_수정_성공() {
            Project project = createProject(ProjectStatus.IN_PROGRESS);
            given(loadProjectPort.getById(1L)).willReturn(project);

            sut.update(updateCommand("새이름"));

            assertThat(project.getName()).isEqualTo("새이름");
        }

        @Test
        void COMPLETED_상태는_수정_불가() {
            Project project = createProject(ProjectStatus.COMPLETED);
            given(loadProjectPort.getById(1L)).willReturn(project);

            assertThatThrownBy(() -> sut.update(updateCommand("새이름")))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_STATE);
        }

        @Test
        void ABORTED_상태는_수정_불가() {
            Project project = createProject(ProjectStatus.ABORTED);
            given(loadProjectPort.getById(1L)).willReturn(project);

            assertThatThrownBy(() -> sut.update(updateCommand("새이름")))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_STATE);
        }

        private UpdateProjectCommand updateCommand(String name) {
            return UpdateProjectCommand.builder()
                .projectId(1L)
                .requesterMemberId(100L)
                .name(name)
                .build();
        }
    }

    @Nested
    class submit {

        @Test
        void 프로젝트_제출_성공() {
            Project project = createProject(ProjectStatus.DRAFT);
            project.updateBasicInfo("프로젝트명", null, null, null, null);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(loadProjectApplicationFormPort.existsByProjectId(1L)).willReturn(true);

            sut.submit(SubmitProjectCommand.builder()
                .projectId(1L)
                .build());

            assertThat(project.getStatus()).isEqualTo(ProjectStatus.PENDING_REVIEW);
        }

        @Test
        void 지원폼_미연결이면_SUBMIT_VALIDATION_FAILED() {
            Project project = createProject(ProjectStatus.DRAFT);
            project.updateBasicInfo("프로젝트명", null, null, null, null);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(loadProjectApplicationFormPort.existsByProjectId(1L)).willReturn(false);

            assertThatThrownBy(() -> sut.submit(SubmitProjectCommand.builder()
                .projectId(1L)
                .build()))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_SUBMIT_VALIDATION_FAILED);
        }
    }

    @Nested
    class transferOwnership {

        @Test
        void 양도_성공() {
            Project project = createProject(ProjectStatus.DRAFT);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(getChallengerUseCase.getByMemberIdAndGisuId(200L, 1L))
                .willReturn(challengerInfo(200L, ChallengerPart.PLAN));
            given(getMemberUseCase.getById(200L)).willReturn(memberInfo(8L));
            given(getChapterUseCase.byGisuAndSchool(1L, 8L)).willReturn(new ChapterInfo(3L, "인천"));

            sut.transfer(transferCommand(200L));

            assertThat(project.getProductOwnerMemberId()).isEqualTo(200L);
            assertThat(project.getProductOwnerSchoolId()).isEqualTo(8L);
            assertThat(project.getChapterId()).isEqualTo(3L);
        }

        @Test
        void 새_PM이_PLAN이_아니면_예외() {
            Project project = createProject(ProjectStatus.DRAFT);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(getChallengerUseCase.getByMemberIdAndGisuId(200L, 1L))
                .willReturn(challengerInfo(200L, ChallengerPart.WEB));

            assertThatThrownBy(() -> sut.transfer(transferCommand(200L)))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_OWNER_NOT_PLAN_CHALLENGER);
        }

        @Test
        void COMPLETED_상태는_양도_불가() {
            Project project = createProject(ProjectStatus.COMPLETED);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(getChallengerUseCase.getByMemberIdAndGisuId(200L, 1L))
                .willReturn(challengerInfo(200L, ChallengerPart.PLAN));

            assertThatThrownBy(() -> sut.transfer(transferCommand(200L)))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_STATE);
        }

        @Test
        void 양도_시_새_PM의_PO_기수_중복은_검증하지_않고_성공() {
            // PO 룰 제거 검증: 기존엔 새 PM 이 같은 기수에 다른 프로젝트의 PO 면 차단했지만,
            // 이제는 호출 자체가 사라져 새 PM 이 PO 인 프로젝트가 있어도 양도가 허용된다.
            Project project = createProject(ProjectStatus.DRAFT);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(getChallengerUseCase.getByMemberIdAndGisuId(200L, 1L))
                .willReturn(challengerInfo(200L, ChallengerPart.PLAN));
            given(getMemberUseCase.getById(200L)).willReturn(memberInfo(8L));
            given(getChapterUseCase.byGisuAndSchool(1L, 8L)).willReturn(new ChapterInfo(3L, "인천"));

            sut.transfer(transferCommand(200L));

            assertThat(project.getProductOwnerMemberId()).isEqualTo(200L);
            then(loadProjectPort).should(never()).existsByOwnerAndGisu(any(), any());
        }

        private TransferProjectOwnershipCommand transferCommand(Long newOwner) {
            return TransferProjectOwnershipCommand.builder()
                .projectId(1L)
                .newOwnerMemberId(newOwner)
                .build();
        }
    }

    @Nested
    class publish {

        @Test
        void PENDING_REVIEW에서_IN_PROGRESS로_전이_및_Form_동반_publish() {
            Project project = createProject(ProjectStatus.PENDING_REVIEW);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(loadProjectPartQuotaPort.listByProjectId(1L)).willReturn(java.util.List.of(
                com.umc.product.project.domain.ProjectPartQuota.create(
                    project, ChallengerPart.WEB, 3L, 99L)
            ));
            com.umc.product.project.domain.ProjectApplicationForm form =
                com.umc.product.project.domain.ProjectApplicationForm.create(project, 500L);
            given(loadProjectApplicationFormPort.findByProjectId(1L))
                .willReturn(java.util.Optional.of(form));

            ProjectStatus status = sut.publish(
                com.umc.product.project.application.port.in.command.dto.PublishProjectCommand.builder()
                    .projectId(1L).requesterMemberId(99L).build());

            assertThat(status).isEqualTo(ProjectStatus.IN_PROGRESS);
            org.mockito.BDDMockito.then(manageFormUseCase).should().publishForm(any(), any(), any());
        }

        @Test
        void DRAFT_상태는_publish_불가() {
            Project project = createProject(ProjectStatus.DRAFT);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(loadProjectPartQuotaPort.listByProjectId(1L)).willReturn(java.util.List.of(
                com.umc.product.project.domain.ProjectPartQuota.create(
                    project, ChallengerPart.WEB, 3L, 99L)
            ));
            com.umc.product.project.domain.ProjectApplicationForm form =
                com.umc.product.project.domain.ProjectApplicationForm.create(project, 500L);
            given(loadProjectApplicationFormPort.findByProjectId(1L))
                .willReturn(java.util.Optional.of(form));

            assertThatThrownBy(() -> sut.publish(
                com.umc.product.project.application.port.in.command.dto.PublishProjectCommand.builder()
                    .projectId(1L).requesterMemberId(99L).build()))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_STATE);
            then(manageFormUseCase).should(never()).publishForm(any(), any(), any());
        }

        @Test
        void 파트_quota가_없으면_거부() {
            Project project = createProject(ProjectStatus.PENDING_REVIEW);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(loadProjectPartQuotaPort.listByProjectId(1L)).willReturn(java.util.List.of());

            assertThatThrownBy(() -> sut.publish(
                com.umc.product.project.application.port.in.command.dto.PublishProjectCommand.builder()
                    .projectId(1L).requesterMemberId(99L).build()))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_PART_QUOTA_REQUIRED);
        }

        @Test
        void 지원_폼이_없으면_거부() {
            Project project = createProject(ProjectStatus.PENDING_REVIEW);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(loadProjectPartQuotaPort.listByProjectId(1L)).willReturn(java.util.List.of(
                com.umc.product.project.domain.ProjectPartQuota.create(
                    project, ChallengerPart.WEB, 3L, 99L)
            ));
            given(loadProjectApplicationFormPort.findByProjectId(1L))
                .willReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> sut.publish(
                com.umc.product.project.application.port.in.command.dto.PublishProjectCommand.builder()
                    .projectId(1L).requesterMemberId(99L).build()))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.APPLICATION_FORM_NOT_FOUND);
        }
    }

    @Nested
    class delete {

        @Test
        void DRAFT_상태에서_form이_없으면_자식만_정리하고_삭제() {
            Project project = createProject(ProjectStatus.DRAFT);
            given(loadProjectPort.getById(1L)).willReturn(project);
            given(loadProjectApplicationFormPort.findByProjectId(1L))
                .willReturn(java.util.Optional.empty());

            sut.delete(com.umc.product.project.application.port.in.command.dto.DeleteProjectCommand.builder()
                .projectId(1L).requesterMemberId(99L).build());

            then(saveProjectApplicationFormPolicyPort).should(never()).deleteAllByApplicationFormId(any());
            then(saveProjectApplicationFormPort).should(never()).deleteAllByProjectId(any());
            then(manageFormUseCase).should(never()).deleteForm(any(), any(), any());
            then(saveProjectPartQuotaPort).should().deleteAllByProjectId(1L);
            then(saveProjectMemberPort).should().deleteAllByProjectId(1L);
            then(saveProjectPort).should().delete(project);
        }

        @Test
        void PENDING_REVIEW_상태에서_form이_있으면_Form까지_cascade_삭제() {
            Project project = createProject(ProjectStatus.PENDING_REVIEW);
            com.umc.product.project.domain.ProjectApplicationForm form =
                com.umc.product.project.domain.ProjectApplicationForm.create(project, 777L);
            ReflectionTestUtils.setField(form, "id", 55L);

            given(loadProjectPort.getById(1L)).willReturn(project);
            given(loadProjectApplicationFormPort.findByProjectId(1L))
                .willReturn(java.util.Optional.of(form));

            sut.delete(com.umc.product.project.application.port.in.command.dto.DeleteProjectCommand.builder()
                .projectId(1L).requesterMemberId(99L).build());

            then(saveProjectApplicationFormPolicyPort).should().deleteAllByApplicationFormId(55L);
            then(saveProjectApplicationFormPort).should().deleteAllByProjectId(1L);
            then(manageFormUseCase).should().deleteForm(any(), any(), any());
            then(saveProjectPartQuotaPort).should().deleteAllByProjectId(1L);
            then(saveProjectMemberPort).should().deleteAllByProjectId(1L);
            then(saveProjectPort).should().delete(project);
        }

        @Test
        void IN_PROGRESS_상태이면_PROJECT_DELETE_NOT_ALLOWED_IN_STATUS() {
            Project project = createProject(ProjectStatus.IN_PROGRESS);
            given(loadProjectPort.getById(1L)).willReturn(project);

            assertThatThrownBy(() -> sut.delete(
                com.umc.product.project.application.port.in.command.dto.DeleteProjectCommand.builder()
                    .projectId(1L).requesterMemberId(99L).build()))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_DELETE_NOT_ALLOWED_IN_STATUS);

            then(saveProjectPort).should(never()).delete(any());
        }
    }

    @Nested
    class abort {

        @Test
        void IN_PROGRESS_상태에서_멤버_WITHDRAWN_application_CANCELLED() {
            Project project = createProject(ProjectStatus.IN_PROGRESS);
            com.umc.product.project.domain.ProjectMember activeMember =
                com.umc.product.project.domain.ProjectMember.create(project, 500L, ChallengerPart.WEB, 100L);

            given(loadProjectPort.getById(1L)).willReturn(project);
            given(loadProjectMemberPort.listByProjectId(1L)).willReturn(java.util.List.of(activeMember));
            given(loadProjectApplicationPort.listInProgressByProjectId(1L))
                .willReturn(java.util.List.of());

            sut.abort(com.umc.product.project.application.port.in.command.dto.AbortProjectCommand.builder()
                .projectId(1L).requesterMemberId(99L).reason("팀 와해").build());

            assertThat(project.getStatus()).isEqualTo(ProjectStatus.ABORTED);
            assertThat(project.getStatusChangedReason()).isEqualTo("팀 와해");
            assertThat(activeMember.getStatus())
                .isEqualTo(com.umc.product.project.domain.enums.ProjectMemberStatus.WITHDRAWN);
            assertThat(activeMember.getStatusChangeReason()).isEqualTo("팀 와해");
        }

        @Test
        void COMPLETED_상태이면_PROJECT_ABORT_UNAVAILABLE() {
            Project project = createProject(ProjectStatus.COMPLETED);
            given(loadProjectPort.getById(1L)).willReturn(project);

            assertThatThrownBy(() -> sut.abort(
                com.umc.product.project.application.port.in.command.dto.AbortProjectCommand.builder()
                    .projectId(1L).requesterMemberId(99L).reason("사유").build()))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ABORT_UNAVAILABLE);
        }

        @Test
        void 사유가_blank이면_Command_생성_단계에서_PROJECT_ABORT_REASON_REQUIRED() {
            assertThatThrownBy(() ->
                com.umc.product.project.application.port.in.command.dto.AbortProjectCommand.builder()
                    .projectId(1L).requesterMemberId(99L).reason("  ").build())
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_ABORT_REASON_REQUIRED);
        }
    }
}
