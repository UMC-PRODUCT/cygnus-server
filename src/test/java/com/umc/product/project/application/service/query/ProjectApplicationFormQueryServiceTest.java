package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationFormQueryServiceTest {

    private static final Long PROJECT_ID = 42L;
    private static final Long APPLICATION_FORM_ID = 100L;
    private static final Long FORM_ID = 500L;
    private static final Long GISU_ID = 1L;
    private static final Long CHAPTER_ID = 7L;
    private static final Long PM_MEMBER_ID = 10L;
    private static final Long COMMON_SECTION_ID = 1000L;
    private static final Long PART_SECTION_ID = 1001L;

    @Mock
    LoadProjectApplicationFormPort loadApplicationFormPort;
    @Mock
    LoadProjectApplicationFormPolicyPort loadPolicyPort;
    @Mock
    GetFormUseCase getFormUseCase;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @InjectMocks
    ProjectApplicationFormQueryService sut;

    @Test
    void findByProjectId_폼이_없으면_empty_반환() {
        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.empty());

        Optional<ApplicationFormInfo> result = sut.findByProjectId(PROJECT_ID, PM_MEMBER_ID);

        assertThat(result).isEmpty();
        then(getFormUseCase).should(never()).getFormWithStructure(any(), any());
        then(loadPolicyPort).should(never()).listByApplicationFormId(any());
    }

    @Test
    void findByProjectId_PM_본인은_전체_섹션_노출() {
        // given
        Project project = createProject();
        ProjectApplicationForm applicationForm = createApplicationForm(project);

        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(buildFormStructure());
        given(loadPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID)).willReturn(List.of(
            ProjectApplicationFormPolicy.createCommon(applicationForm, COMMON_SECTION_ID),
            ProjectApplicationFormPolicy.createForParts(applicationForm, PART_SECTION_ID,
                Set.of(ChallengerPart.WEB, ChallengerPart.IOS))
        ));

        // when — 호출자가 PM 본인
        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, PM_MEMBER_ID).orElseThrow();

        // then — 전체 섹션 노출, Challenger 조회는 발생하지 않음
        assertThat(result.sections()).hasSize(2);
        var commonSection = result.sections().get(0);
        assertThat(commonSection.type()).isEqualTo(FormSectionType.COMMON);

        var partSection = result.sections().get(1);
        assertThat(partSection.type()).isEqualTo(FormSectionType.PART);
        assertThat(partSection.allowedParts())
            .containsExactlyInAnyOrder(ChallengerPart.WEB, ChallengerPart.IOS);

        then(getChallengerUseCase).should(never()).findByMemberIdAndGisuId(any(), any());
        then(getChallengerRoleUseCase).should(never()).isCentralCoreInGisu(any(), any());
    }

    @Test
    void findByProjectId_CentralCore는_전체_섹션_노출() {
        // given
        Long requesterMemberId = 999L;
        Project project = createProject();
        ProjectApplicationForm applicationForm = createApplicationForm(project);

        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(buildFormStructure());
        given(loadPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID)).willReturn(List.of(
            ProjectApplicationFormPolicy.createCommon(applicationForm, COMMON_SECTION_ID),
            ProjectApplicationFormPolicy.createForParts(applicationForm, PART_SECTION_ID,
                Set.of(ChallengerPart.WEB))
        ));
        given(getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, GISU_ID)).willReturn(true);

        // when
        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, requesterMemberId).orElseThrow();

        // then
        assertThat(result.sections()).hasSize(2);
        then(getChallengerUseCase).should(never()).findByMemberIdAndGisuId(any(), any());
    }

    @Test
    void findByProjectId_프로젝트_지부의_지부장은_전체_섹션_노출() {
        // given
        Long requesterMemberId = 888L;
        Project project = createProject();
        ProjectApplicationForm applicationForm = createApplicationForm(project);

        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(buildFormStructure());
        given(loadPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID)).willReturn(List.of(
            ProjectApplicationFormPolicy.createCommon(applicationForm, COMMON_SECTION_ID),
            ProjectApplicationFormPolicy.createForParts(applicationForm, PART_SECTION_ID,
                Set.of(ChallengerPart.WEB))
        ));
        given(getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, GISU_ID)).willReturn(false);
        given(getChallengerRoleUseCase.isChapterPresidentInGisu(requesterMemberId, GISU_ID, CHAPTER_ID))
            .willReturn(true);

        // when
        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, requesterMemberId).orElseThrow();

        // then
        assertThat(result.sections()).hasSize(2);
        then(getChallengerUseCase).should(never()).findByMemberIdAndGisuId(any(), any());
    }

    @Test
    void findByProjectId_챌린저_지원자_본인_파트가_매칭되지_않으면_COMMON만_노출() {
        // given — 지원자 파트(ANDROID) 가 PART 섹션의 allowedParts(WEB/IOS) 에 포함되지 않는 케이스
        Long requesterMemberId = 777L;
        Project project = createProject();
        ProjectApplicationForm applicationForm = createApplicationForm(project);

        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(buildFormStructure());
        given(loadPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID)).willReturn(List.of(
            ProjectApplicationFormPolicy.createCommon(applicationForm, COMMON_SECTION_ID),
            ProjectApplicationFormPolicy.createForParts(applicationForm, PART_SECTION_ID,
                Set.of(ChallengerPart.WEB, ChallengerPart.IOS))
        ));
        given(getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, GISU_ID)).willReturn(false);
        given(getChallengerRoleUseCase.isChapterPresidentInGisu(requesterMemberId, GISU_ID, CHAPTER_ID))
            .willReturn(false);
        given(getChallengerUseCase.findByMemberIdAndGisuId(requesterMemberId, GISU_ID))
            .willReturn(Optional.of(challengerInfoWithPart(ChallengerPart.ANDROID)));

        // when
        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, requesterMemberId).orElseThrow();

        // then — PART 섹션은 매칭 실패로 제외, COMMON 만 남음
        assertThat(result.sections()).hasSize(1);
        assertThat(result.sections().get(0).type()).isEqualTo(FormSectionType.COMMON);
        assertThat(result.sections().get(0).sectionId()).isEqualTo(COMMON_SECTION_ID);
    }

    @Test
    void findByProjectId_챌린저_지원자_본인_파트가_매칭되면_PART도_함께_노출() {
        // given
        Long requesterMemberId = 777L;
        Project project = createProject();
        ProjectApplicationForm applicationForm = createApplicationForm(project);

        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(buildFormStructure());
        given(loadPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID)).willReturn(List.of(
            ProjectApplicationFormPolicy.createCommon(applicationForm, COMMON_SECTION_ID),
            ProjectApplicationFormPolicy.createForParts(applicationForm, PART_SECTION_ID,
                Set.of(ChallengerPart.WEB, ChallengerPart.IOS))
        ));
        given(getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, GISU_ID)).willReturn(false);
        given(getChallengerRoleUseCase.isChapterPresidentInGisu(requesterMemberId, GISU_ID, CHAPTER_ID))
            .willReturn(false);
        given(getChallengerUseCase.findByMemberIdAndGisuId(requesterMemberId, GISU_ID))
            .willReturn(Optional.of(challengerInfoWithPart(ChallengerPart.WEB)));

        // when
        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, requesterMemberId).orElseThrow();

        // then
        assertThat(result.sections()).hasSize(2);
        assertThat(result.sections().get(0).sectionId()).isEqualTo(COMMON_SECTION_ID);
        assertThat(result.sections().get(1).sectionId()).isEqualTo(PART_SECTION_ID);
    }

    @Test
    void findByProjectId_프로젝트_기수_챌린저가_아닌_외부사용자는_403() {
        // given
        Long requesterMemberId = 666L;
        Project project = createProject();
        ProjectApplicationForm applicationForm = createApplicationForm(project);

        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(applicationForm));
        given(getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, GISU_ID)).willReturn(false);
        given(getChallengerRoleUseCase.isChapterPresidentInGisu(requesterMemberId, GISU_ID, CHAPTER_ID))
            .willReturn(false);
        given(getChallengerUseCase.findByMemberIdAndGisuId(requesterMemberId, GISU_ID))
            .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> sut.findByProjectId(PROJECT_ID, requesterMemberId))
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.APPLICATION_FORM_ACCESS_NOT_ALLOWED);

        // 외부 사용자는 권한 검증 단계에서 차단되어 폼/정책 조회는 발생하지 않음
        then(getFormUseCase).should(never()).getFormWithStructure(any(), any());
        then(loadPolicyPort).should(never()).listByApplicationFormId(any());
    }

    @Test
    void findByProjectId_지원자_시점에서_정책이_누락된_섹션은_노출하지_않음() {
        // given — Survey 단엔 섹션 2개 있지만 Project 정책은 1개만 (데이터 정합 깨진 시나리오)
        Long requesterMemberId = 777L;
        Project project = createProject();
        ProjectApplicationForm applicationForm = createApplicationForm(project);

        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(buildFormStructure());
        given(loadPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID)).willReturn(List.of(
            ProjectApplicationFormPolicy.createCommon(applicationForm, COMMON_SECTION_ID)
            // PART_SECTION_ID 정책 누락
        ));
        given(getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, GISU_ID)).willReturn(false);
        given(getChallengerRoleUseCase.isChapterPresidentInGisu(requesterMemberId, GISU_ID, CHAPTER_ID))
            .willReturn(false);
        given(getChallengerUseCase.findByMemberIdAndGisuId(requesterMemberId, GISU_ID))
            .willReturn(Optional.of(challengerInfoWithPart(ChallengerPart.WEB)));

        // when
        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, requesterMemberId).orElseThrow();

        // then — 정책 없는 섹션은 안전 차원에서 차단
        assertThat(result.sections()).hasSize(1);
        assertThat(result.sections().get(0).sectionId()).isEqualTo(COMMON_SECTION_ID);
    }

    @Test
    void findByProjectId_PM_시점에서는_정책이_누락된_섹션도_PART_빈_parts로_폴백() {
        // given — 운영진 우회 케이스에서는 기존의 of() 폴백 동작 유지
        Project project = createProject();
        ProjectApplicationForm applicationForm = createApplicationForm(project);

        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(any(), any())).willReturn(buildFormStructure());
        given(loadPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID)).willReturn(List.of(
            ProjectApplicationFormPolicy.createCommon(applicationForm, COMMON_SECTION_ID)
            // PART_SECTION_ID 정책 누락
        ));

        // when
        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, PM_MEMBER_ID).orElseThrow();

        // then
        var orphanSection = result.sections().get(1);
        assertThat(orphanSection.type()).isEqualTo(FormSectionType.PART);
        assertThat(orphanSection.allowedParts()).isEmpty();
    }

    @Test
    void findAllByProjectIds_지원폼을_batch_port로_조회하고_프로젝트_ID별로_매핑() {
        // given
        Project project = createProject();
        ProjectApplicationForm applicationForm = createApplicationForm(project);

        given(loadApplicationFormPort.findAllByProjectIds(List.of(PROJECT_ID, 99L))).willReturn(Map.of(
            PROJECT_ID, applicationForm
        ));
        given(getFormUseCase.batchGetFormsWithStructure(any(), any()))
            .willReturn(Map.of(FORM_ID, buildFormStructure()));
        given(loadPolicyPort.listByApplicationFormIds(Set.of(APPLICATION_FORM_ID))).willReturn(Map.of(
            APPLICATION_FORM_ID,
            List.of(
                ProjectApplicationFormPolicy.createCommon(applicationForm, COMMON_SECTION_ID),
                ProjectApplicationFormPolicy.createForParts(applicationForm, PART_SECTION_ID,
                    Set.of(ChallengerPart.WEB))
            )
        ));

        // when
        Map<Long, ApplicationFormInfo> result = sut.findAllByProjectIds(
            List.of(PROJECT_ID, PROJECT_ID, 99L),
            PM_MEMBER_ID
        );

        // then
        assertThat(result).containsOnlyKeys(PROJECT_ID);
        assertThat(result.get(PROJECT_ID).applicationFormId()).isEqualTo(APPLICATION_FORM_ID);
        assertThat(result.get(PROJECT_ID).sections()).hasSize(2);
        then(loadApplicationFormPort).should().findAllByProjectIds(List.of(PROJECT_ID, 99L));
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private Project createProject() {
        Project project;
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            project = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        ReflectionTestUtils.setField(project, "gisuId", GISU_ID);
        ReflectionTestUtils.setField(project, "chapterId", CHAPTER_ID);
        ReflectionTestUtils.setField(project, "status", ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(project, "name", "Triple");
        ReflectionTestUtils.setField(project, "productOwnerMemberId", PM_MEMBER_ID);
        ReflectionTestUtils.setField(project, "creatorMemberId", PM_MEMBER_ID);
        return project;
    }

    private ProjectApplicationForm createApplicationForm(Project project) {
        ProjectApplicationForm form = ProjectApplicationForm.create(project, FORM_ID);
        ReflectionTestUtils.setField(form, "id", APPLICATION_FORM_ID);
        return form;
    }

    private ChallengerInfo challengerInfoWithPart(ChallengerPart part) {
        return ChallengerInfo.builder()
            .challengerId(1L)
            .memberId(0L)
            .gisuId(GISU_ID)
            .part(part)
            .build();
    }

    private FormWithStructureInfo buildFormStructure() {
        return FormWithStructureInfo.builder()
            .formId(FORM_ID)
            .title("Triple 지원서")
            .description(null)
            .status(FormStatus.DRAFT)
            .isAnonymous(false)
            .sections(List.of(
                FormWithStructureInfo.SectionWithQuestions.builder()
                    .sectionId(COMMON_SECTION_ID)
                    .title("공통 문항")
                    .description(null)
                    .orderNo(1L)
                    .questions(List.of(
                        FormWithStructureInfo.QuestionWithOptions.builder()
                            .questionId(2000L)
                            .title("자기소개")
                            .description(null)
                            .type(QuestionType.LONG_TEXT)
                            .isRequired(true)
                            .orderNo(1L)
                            .options(List.of())
                            .build()
                    ))
                    .build(),
                FormWithStructureInfo.SectionWithQuestions.builder()
                    .sectionId(PART_SECTION_ID)
                    .title("프론트엔드")
                    .description(null)
                    .orderNo(2L)
                    .questions(List.of(
                        FormWithStructureInfo.QuestionWithOptions.builder()
                            .questionId(2001L)
                            .title("선호 프레임워크")
                            .description(null)
                            .type(QuestionType.RADIO)
                            .isRequired(true)
                            .orderNo(1L)
                            .options(List.of(
                                FormWithStructureInfo.Option.builder()
                                    .optionId(3000L)
                                    .content("React")
                                    .orderNo(1L)
                                    .isOther(false)
                                    .build(),
                                FormWithStructureInfo.Option.builder()
                                    .optionId(3001L)
                                    .content("Vue")
                                    .orderNo(2L)
                                    .isOther(false)
                                    .build()
                            ))
                            .build()
                    ))
                    .build()
            ))
            .build();
    }
}
