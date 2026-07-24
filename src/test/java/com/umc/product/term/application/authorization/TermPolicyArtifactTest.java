package com.umc.product.term.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.service.policy.CompiledPolicyRegistry;
import com.umc.product.authorization.application.service.policy.PolicyReviewArtifactRenderer;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;

class TermPolicyArtifactTest {

    private static final Path TRACKED = Path.of(
        "src/main/resources/policies/term/generated/term-policy-artifacts.md");

    @Test
    @DisplayName("Term source policy와 runtime surface에서 생성한 review artifact가 최신이다")
    void verifiesTrackedArtifact() throws Exception {
        CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
            new PolicySemanticCompiler(),
            List.of(new TermPolicyBundleContributor()));
        String artifact = PolicyReviewArtifactRenderer.render(
            registry.require(TermPolicyDomainSchema.BUNDLE_KEY),
            registry.surfaces());
        if ("true".equals(System.getenv("TERM_POLICY_ARTIFACT_PRINT"))) {
            System.out.print(artifact);
            return;
        }

        assertThat(Files.readString(TRACKED, StandardCharsets.UTF_8)).isEqualTo(artifact);
    }
}
