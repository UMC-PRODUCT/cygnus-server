package com.umc.product.blog.application.authorization;

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

class BlogTargetPolicyTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private TargetBlogAuthorizationAdapter target;

    @BeforeEach
    void setUp() {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new BlogPolicyBundleContributor()));
        target = new TargetBlogAuthorizationAdapter(
            new RegisteredPolicyEvaluationService(registry, new PolicyEvaluationService()));
    }

    @Test
    @DisplayName("작성자는 content, series, comment 수정 action을 수행한다")
    void allowsAuthorUpdates() {
        for (BlogPolicyAction action : List.of(
            BlogPolicyAction.CONTENT_UPDATE,
            BlogPolicyAction.SERIES_UPDATE,
            BlogPolicyAction.COMMENT_UPDATE)) {
            assertThat(target.evaluate(context(action, true, false)))
                .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        }
    }

    @Test
    @DisplayName("SUPER_ADMIN은 생성·읽기·삭제를 수행하지만 작성자 전용 수정은 수행하지 않는다")
    void preservesSuperAdminActionBoundary() {
        assertThat(target.evaluate(context(BlogPolicyAction.CONTENT_CREATE, false, true)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(BlogPolicyAction.COMMENT_DELETE, false, true)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(true));
        assertThat(target.evaluate(context(BlogPolicyAction.CONTENT_UPDATE, false, true)))
            .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
    }

    @Test
    @DisplayName("작성자도 SUPER_ADMIN도 아닌 회원은 관리 action을 수행하지 못한다")
    void deniesUnrelatedMember() {
        for (BlogPolicyAction action : BlogPolicyAction.values()) {
            assertThat(target.evaluate(context(action, false, false)))
                .isEqualTo(new PolicyRolloutEvaluation.Success<>(false));
        }
    }

    private BlogAuthorizationContext context(
        BlogPolicyAction action,
        boolean author,
        boolean superAdmin
    ) {
        AuthorizationSubjectSnapshot subject = AuthorizationSubjectSnapshot.member(
            1L,
            null,
            EVALUATED_AT,
            superAdmin ? Set.of(SystemRoleType.SUPER_ADMIN) : Set.of(),
            List.of(),
            List.of(),
            Map.of());
        return new BlogAuthorizationContext(
            action,
            author,
            superAdmin,
            Optional.of(subject),
            EVALUATED_AT);
    }
}
