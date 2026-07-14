package com.umc.product.authorization.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.policy.EvaluatePolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRolePolicyInfo;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.authorization.domain.SubjectPolicyFacts.ChallengerPolicyFact;
import com.umc.product.authorization.domain.SubjectPolicyFacts.RolePolicyFact;
import com.umc.product.authorization.domain.SubjectPolicyFacts.SchoolChapterKey;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyCombiningAlgorithm;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerPolicyInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.project.application.authorization.ProjectCompiledPolicyBundle;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyBundleLoader;
import com.umc.product.project.application.authorization.ProjectPolicyDomainSchema;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshotLoader;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.service.evaluator.ProjectPermissionEvaluator;
import com.umc.product.project.application.service.evaluator.SuperAdminProperties;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.ProjectStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("주체 정책 사실 스냅샷 조회")
class SubjectPolicyFactsSnapshotQueryTest {

    private static final long MEMBER_ID = 7L;
    private static final long MEMBER_SCHOOL_ID = 100L;
    private static final long OTHER_SCHOOL_ID = 200L;
    private static final long FIRST_GISU_ID = 10L;
    private static final long SECOND_GISU_ID = 20L;
    private static final long PROJECT_ID = 900L;
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-14T00:00:00Z");
    private static final Instant FIRST_GISU_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FIRST_GISU_END = Instant.parse("2026-12-31T00:00:00Z");
    private static final Instant SECOND_GISU_START = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant SECOND_GISU_END = Instant.parse("2026-11-30T00:00:00Z");

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetChapterUseCase getChapterUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    OperationalMetrics operationalMetrics;

    @Mock
    Clock clock;

    @Mock
    LoadProjectPort loadProjectPort;

    @Mock
    EvaluatePolicyUseCase evaluatePolicyUseCase;

    @Mock
    ProjectPolicyBundleLoader bundleLoader;

    @Test
    @DisplayName(
        "loadSubject는 role, challenger, Gisu, 학교-지부 fact를 각각 한 번의 batch 조회로 조립한다")
    void 주체_로드는_정책_사실을_각각_한_번의_배치_조회로_조립한다() {
        given(getMemberUseCase.getById(MEMBER_ID)).willReturn(member());
        given(clock.instant()).willReturn(EVALUATED_AT);
        given(getChallengerRoleUseCase.listPolicyFactsByMemberId(MEMBER_ID)).willReturn(roleInfos());
        given(getChallengerUseCase.listPolicyFactsByMemberId(MEMBER_ID)).willReturn(challengerInfos());
        given(getGisuUseCase.getByIds(Set.of(FIRST_GISU_ID, SECOND_GISU_ID))).willReturn(gisus());
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(
            Set.of(FIRST_GISU_ID, SECOND_GISU_ID), Set.of(MEMBER_SCHOOL_ID, OTHER_SCHOOL_ID)))
            .willReturn(chapterMap());
        AuthorizationService service = authorizationService();

        SubjectAttributes subject = service.loadSubject(MEMBER_ID);

        assertLegacyProjection(subject);
        assertThat(subject.policyFacts()).isNotNull();
        assertThat(subject.policyFacts().evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(subject.policyFacts().roles())
            .extracting(RolePolicyFact::gisuStartAt, RolePolicyFact::gisuEndAt)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(FIRST_GISU_START, FIRST_GISU_END),
                org.assertj.core.groups.Tuple.tuple(SECOND_GISU_START, SECOND_GISU_END));
        assertThat(subject.policyFacts().challengers())
            .extracting(ChallengerPolicyFact::chapterId, ChallengerPolicyFact::gisuStartAt,
                ChallengerPolicyFact::gisuEndAt)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(11L, FIRST_GISU_START, FIRST_GISU_END),
                org.assertj.core.groups.Tuple.tuple(21L, SECOND_GISU_START, SECOND_GISU_END));

        then(getChallengerRoleUseCase).should(times(1)).listPolicyFactsByMemberId(MEMBER_ID);
        then(getChallengerUseCase).should(times(1)).listPolicyFactsByMemberId(MEMBER_ID);
        then(getGisuUseCase).should(times(1)).getByIds(Set.of(FIRST_GISU_ID, SECOND_GISU_ID));
        then(getChapterUseCase).should(times(1)).getChapterMapByGisuIdsAndSchoolIds(
            Set.of(FIRST_GISU_ID, SECOND_GISU_ID), Set.of(MEMBER_SCHOOL_ID, OTHER_SCHOOL_ID));
        then(clock).should(times(1)).instant();
        then(getChallengerRoleUseCase).should(never()).findAllByMemberId(anyLong());
        then(getChallengerRoleUseCase).should(never()).getById(anyLong());
        then(getChallengerUseCase).should(never()).getAllByMemberId(anyLong());
        then(getChallengerUseCase).should(never()).getById(anyLong());
        then(getGisuUseCase).should(never()).getById(anyLong());
        then(getChapterUseCase).should(never()).byGisuAndSchool(anyLong(), anyLong());
        then(getChapterUseCase).should(never()).getChapterById(anyLong());
    }

    @Test
    @DisplayName("Project evaluator는 preloaded policyFacts를 재조회 없이 같은 snapshot으로 사용한다")
    void 프로젝트_평가기는_미리_로드한_정책_사실을_재조회하지_않는다() {
        SubjectPolicyFacts facts = policyFacts();
        SubjectAttributes subject = facts.toSubjectAttributes(MEMBER_ID, MEMBER_SCHOOL_ID);
        ProjectPolicySubjectSnapshotLoader snapshotLoader = spy(new ProjectPolicySubjectSnapshotLoader(
            getMemberUseCase,
            getChallengerRoleUseCase,
            getChallengerUseCase,
            getGisuUseCase,
            getChapterUseCase,
            clock
        ));
        given(bundleLoader.compiled()).willReturn(compiledBundle());
        given(evaluatePolicyUseCase.evaluate(org.mockito.ArgumentMatchers.any(PolicyEvaluationRequest.class)))
            .willReturn(allowDecision());
        ProjectPolicyAuthorizationService authorizationService = new ProjectPolicyAuthorizationService(
            snapshotLoader, evaluatePolicyUseCase, bundleLoader);
        ProjectPermissionEvaluator evaluator = new ProjectPermissionEvaluator(
            loadProjectPort, authorizationService, new SuperAdminProperties(false));
        Project project = project();
        given(loadProjectPort.findById(PROJECT_ID)).willReturn(Optional.of(project));

        boolean result = evaluator.evaluate(
            subject, ResourcePermission.of(ResourceType.PROJECT, PROJECT_ID, PermissionType.READ));

        assertThat(result).isTrue();
        then(snapshotLoader).should(times(1)).load(subject);
        then(snapshotLoader).should(times(1)).fromFacts(MEMBER_ID, facts, false);
        ArgumentCaptor<PolicyEvaluationRequest> requestCaptor =
            ArgumentCaptor.forClass(PolicyEvaluationRequest.class);
        then(evaluatePolicyUseCase).should(times(1)).evaluate(requestCaptor.capture());
        PolicyEvaluationRequest request = requestCaptor.getValue();
        assertThat(request.evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(request.attributes().value("relation.activeCentralCoreInResourceGisu"))
            .contains(new PolicyValue.BooleanValue(true));
        assertThat(request.attributes().value("subject.memberId"))
            .contains(new PolicyValue.LongValue(MEMBER_ID));
        verifyNoInteractions(
            getMemberUseCase,
            getChallengerRoleUseCase,
            getChallengerUseCase,
            getGisuUseCase,
            getChapterUseCase,
            clock
        );
    }

    private AuthorizationService authorizationService() {
        return new AuthorizationService(
            getChallengerRoleUseCase,
            List.of(),
            getMemberUseCase,
            getChapterUseCase,
            getChallengerUseCase,
            getGisuUseCase,
            operationalMetrics,
            clock
        );
    }

    private MemberInfo member() {
        return MemberInfo.builder()
            .id(MEMBER_ID)
            .schoolId(MEMBER_SCHOOL_ID)
            .build();
    }

    private List<ChallengerRolePolicyInfo> roleInfos() {
        return List.of(
            new ChallengerRolePolicyInfo(
                ChallengerRoleType.CENTRAL_PRESIDENT,
                OrganizationType.CENTRAL,
                null,
                ChallengerPart.PLAN,
                FIRST_GISU_ID
            ),
            new ChallengerRolePolicyInfo(
                ChallengerRoleType.SCHOOL_PART_LEADER,
                OrganizationType.SCHOOL,
                OTHER_SCHOOL_ID,
                ChallengerPart.WEB,
                SECOND_GISU_ID
            )
        );
    }

    private List<ChallengerPolicyInfo> challengerInfos() {
        return List.of(
            new ChallengerPolicyInfo(501L, MEMBER_ID, FIRST_GISU_ID, ChallengerPart.PLAN),
            new ChallengerPolicyInfo(502L, MEMBER_ID, SECOND_GISU_ID, ChallengerPart.WEB)
        );
    }

    private List<GisuInfo> gisus() {
        return List.of(
            new GisuInfo(FIRST_GISU_ID, 9L, FIRST_GISU_START, FIRST_GISU_END, true),
            new GisuInfo(SECOND_GISU_ID, 10L, SECOND_GISU_START, SECOND_GISU_END, true)
        );
    }

    private Map<Long, Map<Long, ChapterInfo>> chapterMap() {
        return Map.of(
            FIRST_GISU_ID, Map.of(
                MEMBER_SCHOOL_ID, new ChapterInfo(11L, "첫 번째 지부"),
                OTHER_SCHOOL_ID, new ChapterInfo(12L, "첫 번째 타 학교 지부")
            ),
            SECOND_GISU_ID, Map.of(
                MEMBER_SCHOOL_ID, new ChapterInfo(21L, "두 번째 지부"),
                OTHER_SCHOOL_ID, new ChapterInfo(22L, "두 번째 타 학교 지부")
            )
        );
    }

    private SubjectPolicyFacts policyFacts() {
        return new SubjectPolicyFacts(
            EVALUATED_AT,
            List.of(
                new RolePolicyFact(
                    ChallengerRoleType.CENTRAL_PRESIDENT,
                    OrganizationType.CENTRAL,
                    null,
                    ChallengerPart.PLAN,
                    FIRST_GISU_ID,
                    FIRST_GISU_START,
                    FIRST_GISU_END
                ),
                new RolePolicyFact(
                    ChallengerRoleType.SCHOOL_PART_LEADER,
                    OrganizationType.SCHOOL,
                    OTHER_SCHOOL_ID,
                    ChallengerPart.WEB,
                    SECOND_GISU_ID,
                    SECOND_GISU_START,
                    SECOND_GISU_END
                )
            ),
            List.of(
                new ChallengerPolicyFact(
                    501L,
                    FIRST_GISU_ID,
                    11L,
                    ChallengerPart.PLAN,
                    FIRST_GISU_START,
                    FIRST_GISU_END
                ),
                new ChallengerPolicyFact(
                    502L,
                    SECOND_GISU_ID,
                    21L,
                    ChallengerPart.WEB,
                    SECOND_GISU_START,
                    SECOND_GISU_END
                )
            ),
            Map.of(
                new SchoolChapterKey(FIRST_GISU_ID, MEMBER_SCHOOL_ID), 11L,
                new SchoolChapterKey(FIRST_GISU_ID, OTHER_SCHOOL_ID), 12L,
                new SchoolChapterKey(SECOND_GISU_ID, MEMBER_SCHOOL_ID), 21L,
                new SchoolChapterKey(SECOND_GISU_ID, OTHER_SCHOOL_ID), 22L
            )
        );
    }

    private void assertLegacyProjection(SubjectAttributes subject) {
        assertThat(subject.memberId()).isEqualTo(MEMBER_ID);
        assertThat(subject.schoolId()).isEqualTo(MEMBER_SCHOOL_ID);
        assertThat(subject.roleAttributes()).containsExactly(
            new RoleAttribute(
                ChallengerRoleType.CENTRAL_PRESIDENT,
                OrganizationType.CENTRAL,
                null,
                ChallengerPart.PLAN,
                FIRST_GISU_ID
            ),
            new RoleAttribute(
                ChallengerRoleType.SCHOOL_PART_LEADER,
                OrganizationType.SCHOOL,
                OTHER_SCHOOL_ID,
                ChallengerPart.WEB,
                SECOND_GISU_ID
            )
        );
        assertThat(subject.gisuChallengerInfos()).containsExactly(
            new SubjectAttributes.GisuChallengerInfo(
                FIRST_GISU_ID, 11L, ChallengerPart.PLAN, 501L),
            new SubjectAttributes.GisuChallengerInfo(
                SECOND_GISU_ID, 21L, ChallengerPart.WEB, 502L)
        );
    }

    private ProjectCompiledPolicyBundle compiledBundle() {
        PolicyDomainSchema schema = ProjectPolicyDomainSchema.create();
        return new ProjectCompiledPolicyBundle(new CompiledPolicyBundle(
            "1.0",
            schema.contextSchemaVersion(),
            "project-test",
            "test",
            PolicyEffect.DENY,
            PolicyCombiningAlgorithm.DENY_OVERRIDES,
            schema,
            "a".repeat(64),
            List.of()
        ));
    }

    private PolicyDecision allowDecision() {
        return new PolicyDecision(
            PolicyEffect.ALLOW,
            List.of("project.read.allow"),
            List.of(),
            List.of(),
            EVALUATED_AT,
            "1.0",
            ProjectPolicyDomainSchema.create().contextSchemaVersion(),
            "test",
            "a".repeat(64)
        );
    }

    private Project project() {
        Project project = mock(Project.class);
        given(project.getId()).willReturn(PROJECT_ID);
        given(project.getGisuId()).willReturn(FIRST_GISU_ID);
        given(project.getChapterId()).willReturn(11L);
        given(project.getStatus()).willReturn(ProjectStatus.IN_PROGRESS);
        given(project.getCreatorMemberId()).willReturn(300L);
        given(project.getProductOwnerMemberId()).willReturn(301L);
        return project;
    }
}
