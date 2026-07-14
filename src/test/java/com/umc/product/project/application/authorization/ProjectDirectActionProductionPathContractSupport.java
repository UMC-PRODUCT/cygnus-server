package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.mockito.Answers;

import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.project.domain.exception.ProjectDomainException;

abstract class ProjectDirectActionProductionPathContractSupport {

    protected static final long MEMBER_ID = 99L;
    protected static final long PROJECT_ID = 42L;
    protected static final long GISU_ID = 5L;
    protected static final long CHAPTER_ID = 10L;
    protected static final long ROUND_ID = 500L;
    protected static final Instant EVALUATED_AT = Instant.parse("2026-07-13T00:00:00Z");

    protected final ProjectPolicySubjectSnapshot snapshot = new ProjectPolicySubjectSnapshot(
        new ProjectPolicyPrincipal.Member(MEMBER_ID), EVALUATED_AT, List.of(), List.of(), Map.of());
    protected final PolicyDecision deniedDecision = new PolicyDecision(
        PolicyEffect.DENY,
        List.of(),
        List.of("direct-contract-deny"),
        List.of(),
        EVALUATED_AT,
        "1.0",
        "project-1.0",
        "1.1.0",
        "direct-contract"
    );
    protected final ProjectPolicyAuthorizationService policyService = mock(
        ProjectPolicyAuthorizationService.class,
        invocation -> {
            if (invocation.getMethod().getReturnType() == ProjectPolicySubjectSnapshot.class) {
                return snapshot;
            }
            if (invocation.getMethod().getReturnType() == PolicyDecision.class) {
                return deniedDecision;
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        }
    );

    protected void assertExactPolicyAction(ProjectPolicyAction expectedAction, ActionInvocation invocation) {
        try {
            invocation.invoke();
        } catch (ProjectDomainException | AuthorizationDomainException expectedDenial) {
            // DENY decision으로 authorization boundary 직후 종료하는 caller도 sink capture 대상이다.
        }

        List<ProjectPolicyAction> actualActions = mockingDetails(policyService).getInvocations().stream()
            .flatMap(policyInvocation -> Arrays.stream(policyInvocation.getArguments()))
            .filter(ProjectPolicyAction.class::isInstance)
            .map(ProjectPolicyAction.class::cast)
            .toList();
        assertThat(actualActions)
            .as(expectedAction.id() + " production caller가 전달한 exact policy action")
            .containsExactly(expectedAction);
    }

    @FunctionalInterface
    protected interface ActionInvocation {
        void invoke();
    }
}
