package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyEvaluationResult;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectTargetPolicyTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-01T00:00:00Z");

    private static CompiledPolicyBundle bundle;

    private final ProjectPolicyContextBuilder contextBuilder = new ProjectPolicyContextBuilder();
    private final PolicyEvaluationService evaluator = new PolicyEvaluationService();

    @BeforeAll
    static void compileBundle() {
        bundle = new ProjectPolicyBundleLoader(new PolicySemanticCompiler()).compiled().value();
    }

    @Test
    @DisplayName("공개 프로젝트는 MEMBER에게 허용하고 비공개 프로젝트는 default deny한다")
    void projectReadPublicAndPrivateDefaultDeny() {
        ProjectPolicySubjectSnapshot member = member(START, List.of(), List.of(), Map.of());

        assertAllowed(ProjectPolicyAction.PROJECT_READ, member, project(ProjectStatus.IN_PROGRESS, 9L).build());
        assertDenied(ProjectPolicyAction.PROJECT_READ, member, project(ProjectStatus.DRAFT, 9L).build());
    }

    @Test
    @DisplayName("SystemRole SUPER_ADMIN은 Gisu와 평가 시점에 무관하게 전역 권한을 가진다")
    void systemSuperAdminIsGlobal() {
        assertAllowed(ProjectPolicyAction.PROJECT_READ,
            member(START, true, List.of(), List.of(), Map.of()),
            project(ProjectStatus.PENDING_REVIEW, 999L).build());
        assertAllowed(ProjectPolicyAction.PROJECT_READ,
            member(END, true, List.of(), List.of(), Map.of()),
            project(ProjectStatus.PENDING_REVIEW, 999L).build());
    }

    @Test
    @DisplayName("지부장은 같은 Gisu라도 다른 chapter 비공개 프로젝트를 읽을 수 없다")
    void chapterPresidentCannotReadAnotherChapterPrivateProject() {
        ProjectPolicyRoleTuple president = role(
            ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, 1L);
        ProjectPolicySubjectSnapshot subject = member(START, List.of(president), List.of(), Map.of());

        assertAllowed(ProjectPolicyAction.PROJECT_READ, subject,
            project(ProjectStatus.PENDING_REVIEW, 1L, 10L).build());
        assertDenied(ProjectPolicyAction.PROJECT_READ, subject,
            project(ProjectStatus.PENDING_REVIEW, 1L, 20L).build());
    }

    @Test
    @DisplayName("project create는 target Gisu 기간 안의 PLAN challenger만 허용한다")
    void projectCreateRequiresActivePlanChallengerInTargetGisu() {
        ProjectPolicyChallengerTuple challenger = challenger(1L, 10L, ChallengerPart.PLAN);
        ProjectPolicyResourceContext target = ProjectPolicyResourceContext.builder()
            .projectTarget(1L, 10L)
            .creatorMemberId(5L)
            .build();

        assertAllowed(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(), List.of(challenger), Map.of()), target);
        assertDenied(ProjectPolicyAction.PROJECT_CREATE,
            member(END, List.of(), List.of(challenger), Map.of()), target);
        assertDenied(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(), List.of(challenger), Map.of()),
            ProjectPolicyResourceContext.builder()
                .projectTarget(1L, 10L)
                .creatorMemberId(6L)
                .build());
        assertDenied(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(), List.of(challenger), Map.of()),
            ProjectPolicyResourceContext.builder()
                .projectTarget(2L, 10L)
                .creatorMemberId(5L)
                .build());
    }

    @Test
    @DisplayName("project create delegated target은 active SA/CC/CP/SC의 유효 scope만 허용한다")
    void projectCreateDelegationRequiresActiveScopedAdmin() {
        ProjectPolicyResourceContext delegatedTarget = ProjectPolicyResourceContext.builder()
            .projectTarget(1L, 10L)
            .creatorMemberId(99L)
            .build();

        assertAllowed(ProjectPolicyAction.PROJECT_CREATE,
            member(START, true, List.of(), List.of(), Map.of()),
            delegatedTarget);
        assertAllowed(ProjectPolicyAction.PROJECT_CREATE,
            member(END, true, List.of(), List.of(), Map.of()),
            delegatedTarget);

        assertAllowed(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(role(
                ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, 1L)), List.of(), Map.of()),
            delegatedTarget);
        assertDenied(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(role(
                ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, 2L)), List.of(), Map.of()),
            delegatedTarget);

        assertAllowed(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(role(
                ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, 1L)), List.of(), Map.of()),
            delegatedTarget);
        assertDenied(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(role(
                ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 20L, 1L)), List.of(), Map.of()),
            delegatedTarget);
        assertDenied(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(role(
                ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, 2L)), List.of(), Map.of()),
            delegatedTarget);

        ProjectPolicyRoleTuple schoolCore = role(
            ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 30L, 1L);
        assertAllowed(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(schoolCore), List.of(),
                Map.of(new ProjectPolicySchoolChapterKey(1L, 30L), 10L)),
            delegatedTarget);
        assertDenied(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(schoolCore), List.of(),
                Map.of(new ProjectPolicySchoolChapterKey(1L, 30L), 20L)),
            delegatedTarget);
        assertDenied(ProjectPolicyAction.PROJECT_CREATE,
            member(START, List.of(role(
                ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 30L, 2L)), List.of(),
                Map.of(new ProjectPolicySchoolChapterKey(2L, 30L), 10L)),
            delegatedTarget);
    }

    @Test
    @DisplayName("project 수정은 상태와 creator/PO 조건을 함께 만족해야 한다")
    void projectEditRequiresStatusAndRelationship() {
        ProjectPolicySubjectSnapshot subject = member(START, List.of(), List.of(), Map.of());

        assertAllowed(ProjectPolicyAction.PROJECT_UPDATE, subject,
            project(ProjectStatus.DRAFT, 1L).creatorMemberId(5L).build());
        assertDenied(ProjectPolicyAction.PROJECT_UPDATE, subject,
            project(ProjectStatus.DRAFT, 1L).creatorMemberId(6L).build());
        assertAllowed(ProjectPolicyAction.PROJECT_UPDATE, subject,
            project(ProjectStatus.IN_PROGRESS, 1L).productOwnerMemberId(5L).build());
        assertDenied(ProjectPolicyAction.PROJECT_UPDATE, subject,
            project(ProjectStatus.COMPLETED, 1L).productOwnerMemberId(5L).build());
    }

    @Test
    @DisplayName("지원서 decide의 active SUPER_ADMIN statement는 forceDecision outcome을 낸다")
    void superAdminDecisionEmitsForceDecision() {
        ProjectPolicySubjectSnapshot subject = member(START, true, List.of(), List.of(), Map.of());
        ProjectPolicyResourceContext resource = application(ProjectApplicationStatus.SUBMITTED, 999L, 55L);

        PolicyDecision decision = assertAllowed(ProjectPolicyAction.APPLICATION_DECIDE, subject, resource);

        assertThat(decision.outcome(ProjectPolicyOutcomes.APPLICATION_FORCE_DECISION))
            .contains(new PolicyValue.BooleanValue(true));
    }

    @Test
    @DisplayName("form FULL과 APPLICANT가 함께 매칭되면 FULL이 우선한다")
    void fullFormViewDominatesApplicantView() {
        ProjectPolicyChallengerTuple challenger = challenger(1L, 10L, ChallengerPart.DESIGN);
        ProjectPolicySubjectSnapshot subject = member(START, List.of(), List.of(challenger), Map.of());
        ProjectPolicyResourceContext resource = project(ProjectStatus.IN_PROGRESS, 1L, 10L)
            .productOwnerMemberId(5L)
            .build();

        PolicyDecision decision = assertAllowed(ProjectPolicyAction.FORM_READ, subject, resource);

        assertThat(decision.outcome(ProjectPolicyOutcomes.FORM_VIEW))
            .contains(new PolicyValue.EnumValue("FULL"));
    }

    @Test
    @DisplayName("DRAFT form은 challenger·중앙·지부장에게 비공개이고 PO에게만 FULL이다")
    void draftFormRequiresProjectReadVisibility() {
        ProjectPolicyResourceContext draft = project(ProjectStatus.DRAFT, 1L, 10L)
            .productOwnerMemberId(5L)
            .build();
        ProjectPolicyChallengerTuple challenger = challenger(1L, 10L, ChallengerPart.DESIGN);
        ProjectPolicyRoleTuple central = role(
            ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, 1L);
        ProjectPolicyRoleTuple chapter = role(
            ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, 1L);

        assertDenied(ProjectPolicyAction.FORM_READ,
            member(START, List.of(), List.of(challenger), Map.of()),
            project(ProjectStatus.DRAFT, 1L, 10L).productOwnerMemberId(99L).build());
        assertDenied(ProjectPolicyAction.FORM_READ,
            member(START, List.of(central), List.of(), Map.of()),
            project(ProjectStatus.DRAFT, 1L, 10L).productOwnerMemberId(99L).build());
        assertDenied(ProjectPolicyAction.FORM_READ,
            member(START, List.of(chapter), List.of(), Map.of()),
            project(ProjectStatus.DRAFT, 1L, 10L).productOwnerMemberId(99L).build());
        assertAllowed(ProjectPolicyAction.FORM_READ,
            member(START, List.of(), List.of(), Map.of()), draft);
    }

    @Test
    @DisplayName("DRAFT form은 flag가 켜진 active SUPER_ADMIN만 FULL이다")
    void draftFormRequiresFlaggedActiveSuperAdmin() {
        ProjectPolicySubjectSnapshot active = member(START, true, List.of(), List.of(), Map.of());
        ProjectPolicyResourceContext unflagged = project(ProjectStatus.DRAFT, 99L, 10L)
            .productOwnerMemberId(99L)
            .build();
        ProjectPolicyResourceContext flagged = project(ProjectStatus.DRAFT, 99L, 10L)
            .productOwnerMemberId(99L)
            .superAdminAllowDraftRead(true)
            .build();

        assertDenied(ProjectPolicyAction.FORM_READ, active, unflagged);
        assertAllowed(ProjectPolicyAction.FORM_READ, active, flagged);
        assertAllowed(ProjectPolicyAction.FORM_READ,
            member(END, true, List.of(), List.of(), Map.of()), flagged);
    }

    @Test
    @DisplayName("public project scope는 지부장 chapter만 방출하고 학교 회장단 chapter를 섞지 않는다")
    void publicScopeDoesNotMixSchoolCoreChapters() {
        List<ProjectPolicyRoleTuple> roles = List.of(
            role(ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, 1L),
            role(ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 200L, 1L)
        );
        ProjectPolicySubjectSnapshot subject = member(START, roles, List.of(),
            Map.of(new ProjectPolicySchoolChapterKey(1L, 200L), 20L));

        PolicyDecision decision = assertAllowed(ProjectPolicyAction.PROJECT_LIST_PUBLIC, subject,
            ProjectPolicyResourceContext.builder().gisuScope(1L).build());

        assertThat(decision.outcome(ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS))
            .contains(new PolicyValue.LongSetValue(Set.of(10L)));
    }

    @Test
    @DisplayName("학교 회장단은 application project list scope를 얻지 못한다")
    void schoolCoreCannotListProjectApplications() {
        ProjectPolicyRoleTuple schoolCore = role(
            ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 200L, 1L);
        ProjectPolicySubjectSnapshot subject = member(START, List.of(schoolCore), List.of(),
            Map.of(new ProjectPolicySchoolChapterKey(1L, 200L), 10L));

        assertDenied(ProjectPolicyAction.APPLICATION_LIST_PROJECT, subject,
            project(ProjectStatus.IN_PROGRESS, 1L, 10L).build());
    }

    @Test
    @DisplayName("matching human 관리는 target Gisu와 chapter가 같은 active 지부장만 허용한다")
    void matchingHumanManagementRequiresExactActiveRoleTuple() {
        ProjectPolicyRoleTuple chapterPresident = role(
            ChallengerRoleType.CHAPTER_PRESIDENT, OrganizationType.CHAPTER, 10L, 1L);
        ProjectPolicySubjectSnapshot subject = member(START, List.of(chapterPresident), List.of(), Map.of());

        assertAllowed(ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE, subject,
            ProjectPolicyResourceContext.builder().matchingRound(77L, 1L, 10L).build());
        assertDenied(ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE, subject,
            ProjectPolicyResourceContext.builder().matchingRound(77L, 1L, 20L).build());
    }

    @Test
    @DisplayName("scheduler action은 고정 SYSTEM principal만 허용한다")
    void schedulerRequiresExactSystemPrincipal() {
        ProjectPolicyResourceContext round = ProjectPolicyResourceContext.builder()
            .matchingRound(77L, 1L, 10L)
            .build();

        assertAllowed(ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE,
            ProjectPolicySubjectSnapshot.system("matching-round-scheduler", START), round);
        assertDenied(ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE,
            ProjectPolicySubjectSnapshot.system("forged-scheduler", START), round);
    }

    private PolicyDecision assertAllowed(
        ProjectPolicyAction action,
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        PolicyEvaluationResult result = evaluate(action, subject, resource);
        assertThat(result).isInstanceOf(PolicyDecision.class);
        PolicyDecision decision = (PolicyDecision) result;
        assertThat(decision.effect()).as(action.id()).isEqualTo(PolicyEffect.ALLOW);
        return decision;
    }

    private void assertDenied(
        ProjectPolicyAction action,
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        PolicyEvaluationResult result = evaluate(action, subject, resource);
        assertThat(result).isInstanceOf(PolicyDecision.class);
        assertThat(((PolicyDecision) result).effect()).as(action.id()).isEqualTo(PolicyEffect.DENY);
    }

    private PolicyEvaluationResult evaluate(
        ProjectPolicyAction action,
        ProjectPolicySubjectSnapshot subject,
        ProjectPolicyResourceContext resource
    ) {
        return evaluator.evaluate(new PolicyEvaluationRequest(
            bundle, action.id(), contextBuilder.build(action, subject, resource), subject.evaluatedAt()));
    }

    private ProjectPolicySubjectSnapshot member(
        Instant evaluatedAt,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers,
        Map<ProjectPolicySchoolChapterKey, Long> schoolChapters
    ) {
        return member(evaluatedAt, false, roles, challengers, schoolChapters);
    }

    private ProjectPolicySubjectSnapshot member(
        Instant evaluatedAt,
        boolean superAdmin,
        List<ProjectPolicyRoleTuple> roles,
        List<ProjectPolicyChallengerTuple> challengers,
        Map<ProjectPolicySchoolChapterKey, Long> schoolChapters
    ) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(5L), evaluatedAt, superAdmin, roles, challengers, schoolChapters);
    }

    private ProjectPolicyRoleTuple role(
        ChallengerRoleType type,
        OrganizationType organizationType,
        Long organizationId,
        long gisuId
    ) {
        return new ProjectPolicyRoleTuple(type, organizationType, organizationId, null, gisuId, START, END);
    }

    private ProjectPolicyChallengerTuple challenger(long gisuId, long chapterId, ChallengerPart part) {
        return new ProjectPolicyChallengerTuple(99L, gisuId, chapterId, part, START, END);
    }

    private ProjectPolicyResourceContext.Builder project(ProjectStatus status, long gisuId) {
        return project(status, gisuId, 10L);
    }

    private ProjectPolicyResourceContext.Builder project(ProjectStatus status, long gisuId, long chapterId) {
        return ProjectPolicyResourceContext.builder().project(100L, gisuId, chapterId, status);
    }

    private ProjectPolicyResourceContext application(
        ProjectApplicationStatus status,
        long gisuId,
        long applicantId
    ) {
        return project(ProjectStatus.IN_PROGRESS, gisuId)
            .application(200L, status, applicantId)
            .build();
    }
}
