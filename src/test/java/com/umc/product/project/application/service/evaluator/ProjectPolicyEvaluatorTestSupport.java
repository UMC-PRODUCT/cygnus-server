package com.umc.product.project.application.service.evaluator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyBundleLoader;
import com.umc.product.project.application.authorization.ProjectPolicyChallengerTuple;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyRoleTuple;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshotLoader;

final class ProjectPolicyEvaluatorTestSupport {

    private static final Instant EVALUATED_AT = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant START_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END_AT = Instant.parse("2027-01-01T00:00:00Z");

    private ProjectPolicyEvaluatorTestSupport() {
    }

    static ProjectPolicyAuthorizationService authorizationService() {
        ProjectPolicySubjectSnapshotLoader snapshotLoader = mock(ProjectPolicySubjectSnapshotLoader.class);
        lenient().when(snapshotLoader.load(any(SubjectAttributes.class)))
            .thenAnswer(invocation -> snapshot(invocation.getArgument(0)));
        return new ProjectPolicyAuthorizationService(
            snapshotLoader,
            new PolicyEvaluationService(),
            new ProjectPolicyBundleLoader(new PolicySemanticCompiler())
        );
    }

    static ProjectPolicyAuthorizationService authorizationService(ProjectPolicySubjectSnapshot snapshot) {
        ProjectPolicySubjectSnapshotLoader snapshotLoader = mock(ProjectPolicySubjectSnapshotLoader.class);
        lenient().when(snapshotLoader.load(any(SubjectAttributes.class))).thenReturn(snapshot);
        lenient().when(snapshotLoader.load(anyLong())).thenReturn(snapshot);
        return new ProjectPolicyAuthorizationService(
            snapshotLoader,
            new PolicyEvaluationService(),
            new ProjectPolicyBundleLoader(new PolicySemanticCompiler())
        );
    }

    private static ProjectPolicySubjectSnapshot snapshot(SubjectAttributes subject) {
        List<ProjectPolicyRoleTuple> roles = subject.roleAttributes().stream()
            .map(role -> new ProjectPolicyRoleTuple(
                role.roleType(),
                role.organizationType(),
                role.organizationId(),
                role.responsiblePart(),
                role.gisuId(),
                START_AT,
                END_AT
            ))
            .toList();
        List<ProjectPolicyChallengerTuple> challengers = subject.gisuChallengerInfos().stream()
            .map(challenger -> new ProjectPolicyChallengerTuple(
                challenger.challengerId(),
                challenger.gisuId(),
                challenger.chapterId(),
                challenger.part(),
                START_AT,
                END_AT
            ))
            .toList();
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(subject.memberId()),
            EVALUATED_AT,
            subject.systemRoles().contains(SystemRoleType.SUPER_ADMIN),
            roles,
            challengers,
            Map.of()
        );
    }
}
