package com.umc.product.project.application.authorization.rollout.target;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.policy.EvaluatePolicyUseCase;
import com.umc.product.authorization.application.port.in.policy.PolicyEvaluationRequest;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyEvaluationFailure;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.authorization.ProjectAuthorizationResourceSnapshotFactory;
import com.umc.product.project.application.authorization.ProjectCompiledPolicyBundle;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyBundleLoader;
import com.umc.product.project.application.authorization.ProjectPolicyChallengerTuple;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshotLoader;
import com.umc.product.project.application.authorization.rollout.InitialProjectExpectedDifferenceMatrix;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationClassification;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationClassifier;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailure;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailureCode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationPoint;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationInternalOrigin;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutCoordinator;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutMode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationRolloutObserver;
import com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationAdapter;
import com.umc.product.project.domain.enums.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class TargetProjectAuthorizationAdapterTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-13T00:00:00Z");

    @Mock
    EvaluatePolicyUseCase evaluatePolicyUseCase;

    @Mock
    ProjectPolicyBundleLoader bundleLoader;

    private CompiledPolicyBundle bundle;
    private TargetProjectAuthorizationAdapter adapter;

    @BeforeEach
    void setUp() {
        bundle = mock(CompiledPolicyBundle.class);
        given(bundleLoader.compiled()).willReturn(new ProjectCompiledPolicyBundle(bundle));
        adapter = new TargetProjectAuthorizationAdapter(evaluatePolicyUseCase, bundleLoader);
    }

    @Test
    @DisplayName("target adapter는 compiled bundle과 immutable request snapshot으로 ALLOW를 canonicalize한다")
    void canonicalizesAllowedPolicyDecision() {
        // Given
        PolicyDecision policyDecision = decision(PolicyEffect.ALLOW);
        given(evaluatePolicyUseCase.evaluate(org.mockito.ArgumentMatchers.any()))
            .willReturn(policyDecision);
        ProjectAuthorizationComparisonRequest request = request();

        // When
        var result = adapter.evaluate(request);

        // Then
        assertThat(result).isEqualTo(ProjectAuthorizationDecision.allowed());
        ArgumentCaptor<PolicyEvaluationRequest> captor =
            ArgumentCaptor.forClass(PolicyEvaluationRequest.class);
        then(evaluatePolicyUseCase).should().evaluate(captor.capture());
        assertThat(captor.getValue().bundle()).isSameAs(bundle);
        assertThat(captor.getValue().actionId()).isEqualTo(ProjectPolicyAction.MATCHING_LIST.id());
        assertThat(captor.getValue().evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(captor.getValue().attributes().entries())
            .extracting(entry -> entry.name())
            .containsExactlyInAnyOrder("subject.kind", "subject.memberId");
    }

    @Test
    @DisplayName("typed PolicyEvaluationFailure는 POLICY_EVALUATION_FAILED로 변환한다")
    void convertsTypedPolicyFailure() {
        // Given
        given(evaluatePolicyUseCase.evaluate(org.mockito.ArgumentMatchers.any()))
            .willReturn(new PolicyEvaluationFailure(
                com.umc.product.authorization.domain.policy.PolicyEvaluationFailureCode
                    .REQUIRED_ATTRIBUTE_MISSING,
                Optional.empty(),
                Optional.of("subject.memberId"),
                Optional.empty(),
                EVALUATED_AT,
                "1.0",
                "project-1.0",
                "1.0.0",
                "fingerprint"
            ));

        // When
        var result = adapter.evaluate(request());

        // Then
        assertThat(result).isEqualTo(failure());
    }

    @Test
    @DisplayName("RuntimeException은 POLICY_EVALUATION_FAILED로 변환한다")
    void convertsRuntimeException() {
        // Given
        given(evaluatePolicyUseCase.evaluate(org.mockito.ArgumentMatchers.any()))
            .willThrow(new IllegalStateException("evaluation failed"));

        // When
        var result = adapter.evaluate(request());

        // Then
        assertThat(result).isEqualTo(failure());
    }

    @Test
    @DisplayName("Error는 target failure로 삼키지 않고 호출자에게 전파한다")
    void propagatesError() {
        // Given
        AssertionError error = new AssertionError("fatal");
        given(evaluatePolicyUseCase.evaluate(org.mockito.ArgumentMatchers.any()))
            .willThrow(error);

        // When & Then
        assertThatThrownBy(() -> adapter.evaluate(request())).isSameAs(error);
    }

    @ParameterizedTest
    @EnumSource(
        value = ProjectPolicyAction.class,
        names = {"APPLICATION_CREATE", "APPLICATION_SUBMIT"}
    )
    @DisplayName("지원서 생성·제출의 trusted round scope mismatch는 정상 DENY로 닫는다")
    void deniesTrustedApplicationRoundScopeMismatch(ProjectPolicyAction action) {
        // Given
        ProjectAuthorizationComparisonRequest request = new ProjectAuthorizationComparisonRequest(
            subject(),
            action,
            ProjectAuthorizationResourceSnapshot.withApplicationRoundScopeMismatch(
                ProjectPolicyResourceContext.builder().build()),
            ProjectAuthorizationEvaluationPoint.internal(
                ProjectAuthorizationInternalOrigin.RESOURCE_PERMISSION_EVALUATOR)
        );

        // When
        var result = adapter.evaluate(request);

        // Then
        assertThat(result).isEqualTo(ProjectAuthorizationDecision.denied());
        then(evaluatePolicyUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("round scope mismatch fact는 지원서 생성·제출 이외 action을 차단하지 않는다")
    void doesNotApplyRoundScopeMismatchToOtherActions() {
        // Given
        given(evaluatePolicyUseCase.evaluate(org.mockito.ArgumentMatchers.any()))
            .willReturn(decision(PolicyEffect.ALLOW));
        ProjectAuthorizationComparisonRequest request = new ProjectAuthorizationComparisonRequest(
            subject(),
            ProjectPolicyAction.MATCHING_LIST,
            ProjectAuthorizationResourceSnapshot.withApplicationRoundScopeMismatch(
                ProjectPolicyResourceContext.builder().build()),
            ProjectAuthorizationEvaluationPoint.internal(
                ProjectAuthorizationInternalOrigin.PARENT_TRANSITIVE)
        );

        // When
        var result = adapter.evaluate(request);

        // Then
        assertThat(result).isEqualTo(ProjectAuthorizationDecision.allowed());
        then(evaluatePolicyUseCase).should().evaluate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("위임 프로젝트 생성에 대상 학교 fact가 없으면 target typed failure로 닫는다")
    void delegatedProjectCreateWithoutTargetSchoolFailsClosed() {
        ProjectAuthorizationComparisonRequest request = new ProjectAuthorizationComparisonRequest(
            subject(),
            ProjectPolicyAction.PROJECT_CREATE,
            ProjectAuthorizationResourceSnapshot.of(ProjectPolicyResourceContext.builder()
                .projectTarget(1L, 10L)
                .creatorMemberId(99L)
                .build()),
            ProjectAuthorizationEvaluationPoint.internal(
                ProjectAuthorizationInternalOrigin.RESOURCE_PERMISSION_EVALUATOR)
        );

        var result = adapter.evaluate(request);

        assertThat(result).isEqualTo(failure());
        then(evaluatePolicyUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SHADOW는 trusted create scope mismatch를 EXPECTED로 관측하고 legacy ALLOW를 유지한다")
    void observesExpectedDifferenceForTrustedCreateScopeMismatchInShadow() {
        // Given
        given(bundle.schemaVersion()).willReturn("1.0");
        given(bundle.contextSchemaVersion()).willReturn("project-1.0");
        given(bundle.policyVersion()).willReturn("1.0.0");
        given(bundle.policyFingerprint()).willReturn("a".repeat(64));
        ProjectAuthorizationRolloutObserver observer = mock(ProjectAuthorizationRolloutObserver.class);
        ProjectAuthorizationRolloutCoordinator coordinator = new ProjectAuthorizationRolloutCoordinator(
            new LegacyProjectAuthorizationAdapter(),
            adapter,
            new ProjectAuthorizationClassifier(InitialProjectExpectedDifferenceMatrix.create()),
            action -> ProjectAuthorizationRolloutMode.SHADOW
        );
        ProjectPolicyAuthorizationService service = new ProjectPolicyAuthorizationService(
            mock(ProjectPolicySubjectSnapshotLoader.class),
            coordinator,
            bundleLoader,
            mock(ProjectAuthorizationResourceSnapshotFactory.class),
            observer
        );
        ProjectAuthorizationEvaluationPoint evaluationPoint =
            ProjectAuthorizationEvaluationPoint.internal(
                ProjectAuthorizationInternalOrigin.RESOURCE_PERMISSION_EVALUATOR);
        ProjectAuthorizationResourceSnapshot resource =
            ProjectAuthorizationResourceSnapshot.withApplicationRoundScopeMismatch(
                ProjectPolicyResourceContext.builder()
                    .project(100L, 1L, 2L, ProjectStatus.IN_PROGRESS)
                    .build());

        // When
        PolicyDecision decision = service.evaluate(
            activeChallengerSubject(),
            ProjectPolicyAction.APPLICATION_CREATE,
            resource,
            evaluationPoint
        );

        // Then
        assertThat(decision.effect()).isEqualTo(PolicyEffect.ALLOW);
        ArgumentCaptor<ProjectAuthorizationRolloutObserver.Observation> observationCaptor =
            ArgumentCaptor.forClass(ProjectAuthorizationRolloutObserver.Observation.class);
        then(observer).should().observe(observationCaptor.capture());
        assertThat(observationCaptor.getValue().classification())
            .contains(ProjectAuthorizationClassification.EXPECTED_DIFFERENCE);
        assertThat(observationCaptor.getValue().mode()).isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);
        assertThat(observationCaptor.getValue().evaluationPoint()).isEqualTo(evaluationPoint);
        then(evaluatePolicyUseCase).shouldHaveNoInteractions();
    }

    private ProjectAuthorizationComparisonRequest request() {
        return new ProjectAuthorizationComparisonRequest(
            subject(),
            ProjectPolicyAction.MATCHING_LIST,
            ProjectPolicyResourceContext.builder().build(),
            ProjectAuthorizationEvaluationPoint.internal(ProjectAuthorizationInternalOrigin.PARENT_TRANSITIVE)
        );
    }

    private ProjectPolicySubjectSnapshot subject() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(1L), EVALUATED_AT, List.of(), List.of(), Map.of());
    }

    private ProjectPolicySubjectSnapshot activeChallengerSubject() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(1L),
            EVALUATED_AT,
            List.of(),
            List.of(new ProjectPolicyChallengerTuple(
                10L,
                1L,
                2L,
                ChallengerPart.WEB,
                EVALUATED_AT.minusSeconds(60),
                EVALUATED_AT.plusSeconds(60)
            )),
            Map.of()
        );
    }

    private PolicyDecision decision(PolicyEffect effect) {
        return new PolicyDecision(
            effect,
            effect == PolicyEffect.ALLOW ? List.of("allow") : List.of(),
            effect == PolicyEffect.DENY ? List.of("deny") : List.of(),
            List.of(),
            EVALUATED_AT,
            "1.0",
            "project-1.0",
            "1.0.0",
            "fingerprint"
        );
    }

    private ProjectAuthorizationEvaluationFailure failure() {
        return new ProjectAuthorizationEvaluationFailure(
            ProjectAuthorizationEvaluationFailureCode.POLICY_EVALUATION_FAILED,
            EVALUATED_AT
        );
    }
}
