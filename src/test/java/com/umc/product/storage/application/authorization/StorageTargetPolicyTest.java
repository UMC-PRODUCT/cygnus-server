package com.umc.product.storage.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.application.service.policy.RegisteredPolicyEvaluationService;
import com.umc.product.authorization.domain.AuthorizationSubjectSnapshot;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.authorization.domain.policy.rollout.PolicyRolloutEvaluation;

class StorageTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private TargetStorageAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new StoragePolicyBundleContributor()));
        target = new TargetStorageAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("업로더는 system role 조회 없이 파일을 삭제한다")
    void allowsUploaderWithoutSubject() {
        assertThat(target.evaluate(context(true, Optional.empty())))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("SUPER_ADMIN은 자신이 업로드하지 않은 파일도 삭제한다")
    void allowsSuperAdmin() {
        assertThat(target.evaluate(context(false, Optional.of(subject(true)))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
    }

    @Test
    @DisplayName("업로더도 SUPER_ADMIN도 아니면 파일 삭제를 거부한다")
    void deniesUnrelatedMember() {
        assertThat(target.evaluate(context(false, Optional.of(subject(false)))))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    private StorageAuthorizationContext context(
        boolean uploader,
        Optional<AuthorizationSubjectSnapshot> subject
    ) {
        return new StorageAuthorizationContext(uploader, subject, EVALUATED_AT);
    }

    private AuthorizationSubjectSnapshot subject(boolean superAdmin) {
        return AuthorizationSubjectSnapshot.member(
            1L,
            null,
            EVALUATED_AT,
            superAdmin ? Set.of(SystemRoleType.SUPER_ADMIN) : Set.of(),
            List.of(),
            List.of(),
            Map.of());
    }
}
