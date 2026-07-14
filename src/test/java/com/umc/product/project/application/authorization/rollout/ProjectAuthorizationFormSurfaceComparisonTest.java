package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationAdapter;
import com.umc.product.project.domain.enums.ProjectStatus;

class ProjectAuthorizationFormSurfaceComparisonTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-02T00:00:00Z");

    @Test
    @DisplayName("동일 snapshot의 REST와 GraphQL Form surface는 legacy 차이와 shadow 분류를 보존한다")
    void preservesLegacyFormSurfaceDifferenceAndClassification() {
        ProjectPolicySubjectSnapshot snapshot = superAdminSnapshot();
        ProjectPolicyResourceContext resource = ProjectPolicyResourceContext.builder()
            .project(100L, 10L, 20L, ProjectStatus.IN_PROGRESS)
            .build();
        ProjectAuthorizationRolloutCoordinator coordinator = new ProjectAuthorizationRolloutCoordinator(
            new LegacyProjectAuthorizationAdapter(),
            request -> ProjectAuthorizationDecision.denied(),
            new ProjectAuthorizationClassifier(this::expectedGraphQlDifference),
            action -> ProjectAuthorizationRolloutMode.SHADOW
        );

        ProjectAuthorizationCoordinationResult rest = coordinator.coordinate(request(
            snapshot, resource, ProjectAuthorizationSurface.REST_FORM_READ));
        ProjectAuthorizationCoordinationResult graphQl = coordinator.coordinate(request(
            snapshot, resource, ProjectAuthorizationSurface.GRAPHQL_APPLICATION_FORM));

        assertThat(rest.legacy()).isEqualTo(ProjectAuthorizationDecision.denied());
        assertThat(rest.classification()).contains(ProjectAuthorizationClassification.MATCH);
        assertThat(graphQl.legacy()).isEqualTo(new ProjectAuthorizationDecision(
            com.umc.product.authorization.domain.policy.PolicyEffect.ALLOW,
            ProjectAuthorizationProjectScope.none(),
            ProjectAuthorizationApplicationScope.none(),
            ProjectAuthorizationFormView.FULL,
            false,
            true,
            List.of()
        ));
        assertThat(graphQl.classification())
            .contains(ProjectAuthorizationClassification.EXPECTED_DIFFERENCE);
        assertThat(rest.authoritative()).isEqualTo(rest.legacy());
        assertThat(graphQl.authoritative()).isEqualTo(graphQl.legacy());
    }

    private Optional<ProjectExpectedDifferenceId> expectedGraphQlDifference(
        ProjectAuthorizationComparisonRequest request,
        ProjectAuthorizationDecision legacy,
        ProjectAuthorizationDecision target
    ) {
        boolean graphQlForm = request.evaluationPoint() instanceof ProjectAuthorizationEvaluationPoint.Surface point
            && point.surface() == ProjectAuthorizationSurface.GRAPHQL_APPLICATION_FORM;
        return graphQlForm
            && legacy.formView() == ProjectAuthorizationFormView.FULL
            && target.equals(ProjectAuthorizationDecision.denied())
                ? Optional.of(ProjectExpectedDifferenceId.E001)
                : Optional.empty();
    }

    private ProjectAuthorizationComparisonRequest request(
        ProjectPolicySubjectSnapshot snapshot,
        ProjectPolicyResourceContext resource,
        ProjectAuthorizationSurface surface
    ) {
        return new ProjectAuthorizationComparisonRequest(
            snapshot,
            ProjectPolicyAction.FORM_READ,
            resource,
            ProjectAuthorizationEvaluationPoint.surface(surface)
        );
    }

    private ProjectPolicySubjectSnapshot superAdminSnapshot() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(50L),
            EVALUATED_AT,
            true,
            List.of(),
            List.of(),
            Map.of()
        );
    }
}
