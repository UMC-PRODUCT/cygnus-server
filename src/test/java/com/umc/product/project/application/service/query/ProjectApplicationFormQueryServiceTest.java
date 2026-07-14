package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
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

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyChallengerTuple;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationInternalOrigin;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
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

    private static final long PROJECT_ID = 42L;
    private static final long SECOND_PROJECT_ID = 99L;
    private static final long APPLICATION_FORM_ID = 100L;
    private static final long SECOND_APPLICATION_FORM_ID = 101L;
    private static final long FORM_ID = 500L;
    private static final long SECOND_FORM_ID = 501L;
    private static final long GISU_ID = 1L;
    private static final long CHAPTER_ID = 7L;
    private static final long PM_MEMBER_ID = 10L;
    private static final long APPLICANT_MEMBER_ID = 777L;
    private static final long COMMON_SECTION_ID = 1000L;
    private static final long PART_SECTION_ID = 1001L;
    private static final long OTHER_PART_SECTION_ID = 1002L;
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-14T00:00:00Z");
    private static final Instant GISU_START_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant GISU_END_AT = Instant.parse("2026-08-01T00:00:00Z");
    private static final ProjectAuthorizationEvaluationPoint REST_FORM_READ =
        ProjectAuthorizationEvaluationPoint.surface(ProjectAuthorizationSurface.REST_FORM_READ);
    private static final ProjectAuthorizationEvaluationPoint INTERNAL_FORM_READ =
        ProjectAuthorizationEvaluationPoint.internal(ProjectAuthorizationInternalOrigin.FORM_ACCESS_POLICY);
    private static final ProjectAuthorizationEvaluationPoint GRAPHQL_FORM_READ =
        ProjectAuthorizationEvaluationPoint.surface(ProjectAuthorizationSurface.GRAPHQL_APPLICATION_FORM);

    @Mock
    LoadProjectApplicationFormPort loadApplicationFormPort;
    @Mock
    LoadProjectApplicationFormPolicyPort loadPolicyPort;
    @Mock
    GetFormUseCase getFormUseCase;
    @Mock
    ProjectPolicyAuthorizationService policyAuthorizationService;

    @InjectMocks
    ProjectApplicationFormQueryService sut;

    @Test
    void findByProjectId_폼이_없으면_snapshot을_만들지_않고_empty를_반환한다() {
        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.empty());

        Optional<ApplicationFormInfo> result = sut.findByProjectId(PROJECT_ID, PM_MEMBER_ID);

        assertThat(result).isEmpty();
        then(policyAuthorizationService).should(never()).snapshot(PM_MEMBER_ID);
        then(getFormUseCase).should(never()).getFormWithStructure(any());
        then(loadPolicyPort).should(never()).listByApplicationFormId(any());
    }

    @Test
    void findByProjectId_FULL이면_전체_섹션을_노출한다() {
        Project project = createProject(PROJECT_ID);
        ProjectApplicationForm form = createApplicationForm(project, APPLICATION_FORM_ID, FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(PM_MEMBER_ID, List.of());
        givenSingleForm(form, snapshot, formViewDecision("FULL"), defaultPolicies(form));

        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, PM_MEMBER_ID).orElseThrow();

        assertThat(result.sections()).hasSize(2);
        assertThat(result.sections().get(0).type()).isEqualTo(FormSectionType.COMMON);
        assertThat(result.sections().get(1).type()).isEqualTo(FormSectionType.PART);
        assertThat(result.sections().get(1).allowedParts())
            .containsExactlyInAnyOrder(ChallengerPart.WEB, ChallengerPart.IOS);
        then(policyAuthorizationService).should().evaluate(
            snapshot,
            ProjectPolicyAction.FORM_READ,
            resource(project),
            REST_FORM_READ,
            Optional.empty()
        );
    }

    @Test
    void findByProjectId_FULL은_APPLICANT_사실보다_우선한다() {
        Project project = createProject(PROJECT_ID);
        ProjectApplicationForm form = createApplicationForm(project, APPLICATION_FORM_ID, FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(
            APPLICANT_MEMBER_ID,
            List.of(challenger(ChallengerPart.ANDROID))
        );
        givenSingleForm(form, snapshot, formViewDecision("FULL"), defaultPolicies(form));

        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, APPLICANT_MEMBER_ID).orElseThrow();

        assertThat(result.sections()).extracting(ApplicationFormInfo.SectionInfo::sectionId)
            .containsExactly(COMMON_SECTION_ID, PART_SECTION_ID);
    }

    @Test
    void findByProjectId_APPLICANT이면_본인_파트에_맞는_섹션만_노출한다() {
        Project project = createProject(PROJECT_ID);
        ProjectApplicationForm form = createApplicationForm(project, APPLICATION_FORM_ID, FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(
            APPLICANT_MEMBER_ID,
            List.of(challenger(ChallengerPart.WEB))
        );
        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(form));
        given(policyAuthorizationService.snapshot(APPLICANT_MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            snapshot, ProjectPolicyAction.FORM_READ, resource(project), REST_FORM_READ, Optional.empty()))
            .willReturn(formViewDecision("APPLICANT"));
        given(getFormUseCase.getFormWithStructure(FORM_ID)).willReturn(buildFormStructureWithOtherPart());
        given(loadPolicyPort.listByApplicationFormId(APPLICATION_FORM_ID)).willReturn(List.of(
            ProjectApplicationFormPolicy.createCommon(form, COMMON_SECTION_ID),
            ProjectApplicationFormPolicy.createForParts(form, PART_SECTION_ID, Set.of(ChallengerPart.WEB)),
            ProjectApplicationFormPolicy.createForParts(
                form,
                OTHER_PART_SECTION_ID,
                Set.of(ChallengerPart.ANDROID)
            )
        ));

        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, APPLICANT_MEMBER_ID).orElseThrow();

        assertThat(result.sections()).extracting(ApplicationFormInfo.SectionInfo::sectionId)
            .containsExactly(COMMON_SECTION_ID, PART_SECTION_ID);
    }

    @Test
    void findByProjectId_NONE이면_폼_구조를_조회하기_전에_접근을_거부한다() {
        Project project = createProject(PROJECT_ID);
        ProjectApplicationForm form = createApplicationForm(project, APPLICATION_FORM_ID, FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(APPLICANT_MEMBER_ID, List.of());
        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(form));
        given(policyAuthorizationService.snapshot(APPLICANT_MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            snapshot, ProjectPolicyAction.FORM_READ, resource(project), REST_FORM_READ, Optional.empty()))
            .willReturn(formViewDecision("NONE"));

        assertAccessDenied(() -> sut.findByProjectId(PROJECT_ID, APPLICANT_MEMBER_ID));
        then(getFormUseCase).should(never()).getFormWithStructure(any());
        then(loadPolicyPort).should(never()).listByApplicationFormId(any());
    }

    @Test
    void findByProjectId_DENY이면_폼_구조를_조회하기_전에_접근을_거부한다() {
        Project project = createProject(PROJECT_ID);
        ProjectApplicationForm form = createApplicationForm(project, APPLICATION_FORM_ID, FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(APPLICANT_MEMBER_ID, List.of());
        given(loadApplicationFormPort.findByProjectId(PROJECT_ID)).willReturn(Optional.of(form));
        given(policyAuthorizationService.snapshot(APPLICANT_MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            snapshot, ProjectPolicyAction.FORM_READ, resource(project), REST_FORM_READ, Optional.empty()))
            .willReturn(deniedDecision());

        assertAccessDenied(() -> sut.findByProjectId(PROJECT_ID, APPLICANT_MEMBER_ID));
        then(getFormUseCase).should(never()).getFormWithStructure(any());
        then(loadPolicyPort).should(never()).listByApplicationFormId(any());
    }

    @Test
    void findByProjectId_APPLICANT_snapshot에_해당_기수_파트가_없으면_접근을_거부한다() {
        Project project = createProject(PROJECT_ID);
        ProjectApplicationForm form = createApplicationForm(project, APPLICATION_FORM_ID, FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(APPLICANT_MEMBER_ID, List.of());
        givenSingleForm(form, snapshot, formViewDecision("APPLICANT"), defaultPolicies(form));

        assertAccessDenied(() -> sut.findByProjectId(PROJECT_ID, APPLICANT_MEMBER_ID));
    }

    @Test
    void findByProjectId_APPLICANT_파트_section이_매칭되지_않으면_접근을_거부한다() {
        Project project = createProject(PROJECT_ID);
        ProjectApplicationForm form = createApplicationForm(project, APPLICATION_FORM_ID, FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(
            APPLICANT_MEMBER_ID,
            List.of(challenger(ChallengerPart.ANDROID))
        );
        givenSingleForm(form, snapshot, formViewDecision("APPLICANT"), defaultPolicies(form));

        assertAccessDenied(() -> sut.findByProjectId(PROJECT_ID, APPLICANT_MEMBER_ID));
    }

    @Test
    void findByProjectId_FULL이면_정책이_누락된_섹션도_빈_parts로_노출한다() {
        Project project = createProject(PROJECT_ID);
        ProjectApplicationForm form = createApplicationForm(project, APPLICATION_FORM_ID, FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(PM_MEMBER_ID, List.of());
        givenSingleForm(
            form,
            snapshot,
            formViewDecision("FULL"),
            List.of(ProjectApplicationFormPolicy.createCommon(form, COMMON_SECTION_ID))
        );

        ApplicationFormInfo result = sut.findByProjectId(PROJECT_ID, PM_MEMBER_ID).orElseThrow();

        ApplicationFormInfo.SectionInfo orphanSection = result.sections().get(1);
        assertThat(orphanSection.type()).isEqualTo(FormSectionType.PART);
        assertThat(orphanSection.allowedParts()).isEmpty();
    }

    @Test
    void findAllByProjectIds_하나의_snapshot과_evaluatedAt으로_모든_정책을_평가한다() {
        Project firstProject = createProject(PROJECT_ID);
        Project secondProject = createProject(SECOND_PROJECT_ID);
        ProjectApplicationForm firstForm =
            createApplicationForm(firstProject, APPLICATION_FORM_ID, FORM_ID);
        ProjectApplicationForm secondForm =
            createApplicationForm(secondProject, SECOND_APPLICATION_FORM_ID, SECOND_FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(PM_MEMBER_ID, List.of());
        given(loadApplicationFormPort.findAllByProjectIds(List.of(PROJECT_ID, SECOND_PROJECT_ID)))
            .willReturn(Map.of(PROJECT_ID, firstForm, SECOND_PROJECT_ID, secondForm));
        given(policyAuthorizationService.snapshot(PM_MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            snapshot, ProjectPolicyAction.FORM_READ, resource(firstProject), INTERNAL_FORM_READ, Optional.empty()))
            .willReturn(formViewDecision("FULL"));
        given(policyAuthorizationService.evaluate(
            snapshot, ProjectPolicyAction.FORM_READ, resource(secondProject), INTERNAL_FORM_READ, Optional.empty()))
            .willReturn(formViewDecision("FULL"));
        given(getFormUseCase.batchGetFormsWithStructure(Set.of(FORM_ID, SECOND_FORM_ID))).willReturn(Map.of(
            FORM_ID, buildFormStructure(FORM_ID),
            SECOND_FORM_ID, buildFormStructure(SECOND_FORM_ID)
        ));
        given(loadPolicyPort.listByApplicationFormIds(Set.of(APPLICATION_FORM_ID, SECOND_APPLICATION_FORM_ID)))
            .willReturn(Map.of(
                APPLICATION_FORM_ID, defaultPolicies(firstForm),
                SECOND_APPLICATION_FORM_ID, defaultPolicies(secondForm)
            ));

        Map<Long, ApplicationFormInfo> result = sut.findAllByProjectIds(
            List.of(PROJECT_ID, PROJECT_ID, SECOND_PROJECT_ID),
            PM_MEMBER_ID
        );

        assertThat(result).containsOnlyKeys(PROJECT_ID, SECOND_PROJECT_ID);
        assertThat(snapshot.evaluatedAt()).isEqualTo(EVALUATED_AT);
        then(policyAuthorizationService).should(times(1)).snapshot(PM_MEMBER_ID);
        then(policyAuthorizationService).should(times(2)).evaluate(
            eq(snapshot),
            eq(ProjectPolicyAction.FORM_READ),
            any(ProjectPolicyResourceContext.class),
            eq(INTERNAL_FORM_READ),
            eq(Optional.empty())
        );
        then(loadApplicationFormPort).should().findAllByProjectIds(List.of(PROJECT_ID, SECOND_PROJECT_ID));
    }

    @Test
    void findAllByProjectIds_SubjectAttributes는_GraphQL_surface로_정책을_평가한다() {
        Project project = createProject(PROJECT_ID);
        ProjectApplicationForm form = createApplicationForm(project, APPLICATION_FORM_ID, FORM_ID);
        ProjectPolicySubjectSnapshot snapshot = memberSnapshot(PM_MEMBER_ID, List.of());
        SubjectAttributes subject = SubjectAttributes.builder().memberId(PM_MEMBER_ID).build();
        given(loadApplicationFormPort.findAllByProjectIds(List.of(PROJECT_ID)))
            .willReturn(Map.of(PROJECT_ID, form));
        given(policyAuthorizationService.snapshot(subject)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            snapshot, ProjectPolicyAction.FORM_READ, resource(project), GRAPHQL_FORM_READ, Optional.empty()))
            .willReturn(formViewDecision("FULL"));
        given(getFormUseCase.batchGetFormsWithStructure(Set.of(FORM_ID)))
            .willReturn(Map.of(FORM_ID, buildFormStructure(FORM_ID)));
        given(loadPolicyPort.listByApplicationFormIds(Set.of(APPLICATION_FORM_ID)))
            .willReturn(Map.of(APPLICATION_FORM_ID, defaultPolicies(form)));

        Map<Long, ApplicationFormInfo> result = sut.findAllByProjectIds(List.of(PROJECT_ID), subject);

        assertThat(result).containsOnlyKeys(PROJECT_ID);
        then(policyAuthorizationService).should().evaluate(
            snapshot,
            ProjectPolicyAction.FORM_READ,
            resource(project),
            GRAPHQL_FORM_READ,
            Optional.empty()
        );
    }

    private void givenSingleForm(
        ProjectApplicationForm form,
        ProjectPolicySubjectSnapshot snapshot,
        PolicyDecision decision,
        List<ProjectApplicationFormPolicy> policies
    ) {
        long requesterMemberId = ((ProjectPolicyPrincipal.Member) snapshot.principal()).memberId();
        given(loadApplicationFormPort.findByProjectId(form.getProject().getId())).willReturn(Optional.of(form));
        given(policyAuthorizationService.snapshot(requesterMemberId)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.FORM_READ,
            resource(form.getProject()),
            REST_FORM_READ,
            Optional.empty()
        )).willReturn(decision);
        given(getFormUseCase.getFormWithStructure(form.getFormId()))
            .willReturn(buildFormStructure(form.getFormId()));
        given(loadPolicyPort.listByApplicationFormId(form.getId())).willReturn(policies);
    }

    private void assertAccessDenied(Runnable invocation) {
        assertThatThrownBy(invocation::run)
            .isInstanceOf(ProjectDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", ProjectErrorCode.APPLICATION_FORM_ACCESS_NOT_ALLOWED);
    }

    private ProjectPolicySubjectSnapshot memberSnapshot(
        long memberId,
        List<ProjectPolicyChallengerTuple> challengers
    ) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(memberId),
            EVALUATED_AT,
            List.of(),
            challengers,
            Map.of()
        );
    }

    private ProjectPolicyChallengerTuple challenger(ChallengerPart part) {
        return new ProjectPolicyChallengerTuple(
            1L,
            GISU_ID,
            CHAPTER_ID,
            part,
            GISU_START_AT,
            GISU_END_AT
        );
    }

    private PolicyDecision formViewDecision(String view) {
        return decision(
            PolicyEffect.ALLOW,
            List.of(new PolicyResolvedOutcome(ProjectPolicyOutcomes.FORM_VIEW, new PolicyValue.EnumValue(view)))
        );
    }

    private PolicyDecision deniedDecision() {
        return decision(PolicyEffect.DENY, List.of());
    }

    private PolicyDecision decision(PolicyEffect effect, List<PolicyResolvedOutcome> outcomes) {
        return new PolicyDecision(
            effect,
            effect == PolicyEffect.ALLOW ? List.of("form.read.allow") : List.of(),
            effect == PolicyEffect.DENY ? List.of("form.read.deny") : List.of(),
            outcomes,
            EVALUATED_AT,
            "1.0",
            "project-1.0",
            "1.0.0",
            "a".repeat(64)
        );
    }

    private ProjectPolicyResourceContext resource(Project project) {
        return ProjectPolicyResourceContext.builder()
            .project(project.getId(), project.getGisuId(), project.getChapterId(), project.getStatus())
            .productOwnerMemberId(project.getProductOwnerMemberId())
            .build();
    }

    private Project createProject(long projectId) {
        Project project;
        try {
            var constructor = Project.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            project = constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        ReflectionTestUtils.setField(project, "id", projectId);
        ReflectionTestUtils.setField(project, "gisuId", GISU_ID);
        ReflectionTestUtils.setField(project, "chapterId", CHAPTER_ID);
        ReflectionTestUtils.setField(project, "status", ProjectStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(project, "name", "Triple");
        ReflectionTestUtils.setField(project, "productOwnerMemberId", PM_MEMBER_ID);
        ReflectionTestUtils.setField(project, "creatorMemberId", PM_MEMBER_ID);
        return project;
    }

    private ProjectApplicationForm createApplicationForm(
        Project project,
        long applicationFormId,
        long formId
    ) {
        ProjectApplicationForm form = ProjectApplicationForm.create(project, formId);
        ReflectionTestUtils.setField(form, "id", applicationFormId);
        return form;
    }

    private List<ProjectApplicationFormPolicy> defaultPolicies(ProjectApplicationForm form) {
        return List.of(
            ProjectApplicationFormPolicy.createCommon(form, COMMON_SECTION_ID),
            ProjectApplicationFormPolicy.createForParts(
                form,
                PART_SECTION_ID,
                Set.of(ChallengerPart.WEB, ChallengerPart.IOS)
            )
        );
    }

    private FormWithStructureInfo buildFormStructure(long formId) {
        return buildFormStructure(formId, List.of(
            section(COMMON_SECTION_ID, "공통 문항", 2000L, QuestionType.LONG_TEXT),
            section(PART_SECTION_ID, "프론트엔드", 2001L, QuestionType.RADIO)
        ));
    }

    private FormWithStructureInfo buildFormStructureWithOtherPart() {
        return buildFormStructure(FORM_ID, List.of(
            section(COMMON_SECTION_ID, "공통 문항", 2000L, QuestionType.LONG_TEXT),
            section(PART_SECTION_ID, "웹", 2001L, QuestionType.RADIO),
            section(OTHER_PART_SECTION_ID, "안드로이드", 2002L, QuestionType.RADIO)
        ));
    }

    private FormWithStructureInfo buildFormStructure(
        long formId,
        List<FormWithStructureInfo.SectionWithQuestions> sections
    ) {
        return FormWithStructureInfo.builder()
            .formId(formId)
            .title("Triple 지원서")
            .description(null)
            .status(FormStatus.DRAFT)
            .isAnonymous(false)
            .sections(sections)
            .build();
    }

    private FormWithStructureInfo.SectionWithQuestions section(
        long sectionId,
        String title,
        long questionId,
        QuestionType questionType
    ) {
        return FormWithStructureInfo.SectionWithQuestions.builder()
            .sectionId(sectionId)
            .title(title)
            .description(null)
            .orderNo(sectionId == COMMON_SECTION_ID ? 1L : 2L)
            .questions(List.of(
                FormWithStructureInfo.QuestionWithOptions.builder()
                    .questionId(questionId)
                    .title("질문")
                    .description(null)
                    .type(questionType)
                    .isRequired(true)
                    .orderNo(1L)
                    .options(List.of())
                    .build()
            ))
            .build();
    }
}
