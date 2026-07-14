package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.policy.ActionSchema;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
class ProjectMatchingPolicySnapshotSelectionTest {

    private static final long MEMBER_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");

    @Mock
    GetMemberUseCase getMemberUseCase;
    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    GetChapterUseCase getChapterUseCase;
    @Mock
    Clock clock;
    @Mock
    ProjectPolicySubjectSnapshotLoader snapshotLoader;
    @Mock
    EvaluatePolicyUseCase evaluatePolicyUseCase;
    @Test
    @DisplayName("matching list snapshot은 인증 속성 외의 도메인 조회를 수행하지 않는다")
    void authenticatedSnapshotPerformsNoDomainQueries() {
        ProjectPolicySubjectSnapshotLoader loader = realLoader();
        given(clock.instant()).willReturn(NOW);

        ProjectPolicySubjectSnapshot snapshot = loader.loadAuthenticatedMember(MEMBER_ID);

        assertThat(snapshot.principal()).isEqualTo(new ProjectPolicyPrincipal.Member(MEMBER_ID));
        assertThat(snapshot.evaluatedAt()).isEqualTo(NOW);
        assertThat(snapshot.roles()).isEmpty();
        verifyNoInteractions(
            getMemberUseCase,
            getChallengerRoleUseCase,
            getChallengerUseCase,
            getGisuUseCase,
            getChapterUseCase
        );
    }

    @Test
    @DisplayName("matching manager snapshot은 raw role과 Gisu만 한 번씩 조회한다")
    void managerSnapshotLoadsOnlyRoleAndGisu() {
        ProjectPolicySubjectSnapshotLoader loader = realLoader();
        ChallengerRolePolicyInfo role = new ChallengerRolePolicyInfo(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            OrganizationType.CENTRAL,
            100L,
            null,
            5L
        );
        GisuInfo gisu = new GisuInfo(
            5L,
            10L,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2027-01-01T00:00:00Z"),
            false
        );
        given(clock.instant()).willReturn(NOW);
        given(getChallengerRoleUseCase.listPolicyFactsByMemberId(MEMBER_ID)).willReturn(List.of(role));
        given(getGisuUseCase.getByIds(Set.of(5L))).willReturn(List.of(gisu));

        ProjectPolicySubjectSnapshot snapshot = loader.loadMatchingManager(MEMBER_ID);

        assertThat(snapshot.roles()).singleElement().satisfies(tuple -> {
            assertThat(tuple.roleType()).isEqualTo(ChallengerRoleType.CENTRAL_PRESIDENT);
            assertThat(tuple.gisuId()).isEqualTo(5L);
        });
        then(getChallengerRoleUseCase).should(times(1)).listPolicyFactsByMemberId(MEMBER_ID);
        then(getGisuUseCase).should(times(1)).getByIds(Set.of(5L));
        verifyNoInteractions(getMemberUseCase, getChallengerUseCase, getChapterUseCase);
    }

    @Test
    @DisplayName("matching action별 snapshot은 정책 schema의 required attribute만 정확히 조립한다")
    void matchingActionsBuildExactRequiredAttributes() {
        ProjectPolicySubjectSnapshot authenticated = snapshot(List.of());
        ProjectPolicySubjectSnapshot manager = snapshot(List.of());
        given(snapshotLoader.loadAuthenticatedMember(MEMBER_ID)).willReturn(authenticated);
        given(snapshotLoader.loadMatchingManager(MEMBER_ID)).willReturn(manager);
        given(evaluatePolicyUseCase.evaluate(any(PolicyEvaluationRequest.class))).willReturn(allowed());
        ProjectPolicyBundleLoader bundleLoader = new ProjectPolicyBundleLoader(new PolicySemanticCompiler());
        ProjectPolicyAuthorizationService service = new ProjectPolicyAuthorizationService(
            snapshotLoader,
            evaluatePolicyUseCase,
            bundleLoader
        );
        ProjectPolicyResourceContext list = ProjectPolicyResourceContext.builder().build();
        ProjectPolicyResourceContext create = ProjectPolicyResourceContext.builder()
            .matchingRound(null, 5L, 10L)
            .build();
        ProjectPolicyResourceContext existing = ProjectPolicyResourceContext.builder()
            .matchingRound(77L, 5L, 10L)
            .build();

        service.evaluate(MEMBER_ID, ProjectPolicyAction.MATCHING_LIST, list);
        service.evaluate(MEMBER_ID, ProjectPolicyAction.MATCHING_CREATE, create);
        service.evaluate(MEMBER_ID, ProjectPolicyAction.MATCHING_UPDATE, existing);
        service.evaluate(MEMBER_ID, ProjectPolicyAction.MATCHING_DELETE, existing);
        service.evaluate(MEMBER_ID, ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE, existing);

        ArgumentCaptor<PolicyEvaluationRequest> requests =
            ArgumentCaptor.forClass(PolicyEvaluationRequest.class);
        then(evaluatePolicyUseCase).should(times(5)).evaluate(requests.capture());
        requests.getAllValues().forEach(this::assertExactRequiredAttributes);
        then(snapshotLoader).should(times(1)).loadAuthenticatedMember(MEMBER_ID);
        then(snapshotLoader).should(times(4)).loadMatchingManager(MEMBER_ID);
        then(snapshotLoader).should(never()).load(MEMBER_ID);
    }

    private void assertExactRequiredAttributes(PolicyEvaluationRequest request) {
        ActionSchema schema = ProjectPolicyDomainSchema.create().action(request.actionId()).orElseThrow();
        Set<String> actual = request.attributes().entries().stream()
            .map(attribute -> attribute.name())
            .collect(Collectors.toSet());
        assertThat(actual).isEqualTo(schema.requiredAttributes());
    }

    private ProjectPolicySubjectSnapshotLoader realLoader() {
        return new ProjectPolicySubjectSnapshotLoader(
            getMemberUseCase,
            getChallengerRoleUseCase,
            getChallengerUseCase,
            getGisuUseCase,
            getChapterUseCase,
            clock
        );
    }

    private ProjectPolicySubjectSnapshot snapshot(List<ProjectPolicyRoleTuple> roles) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(MEMBER_ID),
            NOW,
            roles,
            List.of(),
            Map.of()
        );
    }

    private PolicyDecision allowed() {
        return new PolicyDecision(
            PolicyEffect.ALLOW,
            List.of("test.allow"),
            List.of(),
            List.of(),
            NOW,
            "1.0",
            "project-1.0",
            "1.0.0",
            "fingerprint"
        );
    }
}
