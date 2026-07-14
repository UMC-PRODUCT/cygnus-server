package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

class ProjectPolicyArtifactTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path TRACKED_ARTIFACT = PROJECT_ROOT.resolve(
        "src/main/resources/policies/project/generated/project-policy-artifacts.md");

    @Test
    @DisplayName("실제 policy source와 runtime 호출면에서 canonical review artifact를 검증한다")
    void verifiesCanonicalArtifactFromPolicySourcesAndRuntimeSurfaces() throws Exception {
        ProjectPolicyArtifactGenerator.Artifact artifact = ProjectPolicyArtifactGenerator.fromSources(
            PROJECT_ROOT,
            ProjectPolicyRuntimeSurfaceDiscovery.discoverAnnotatedSurfaces(new StandardEnvironment()),
            ProjectPolicySurfaceCatalog.surfaces());

        String mode = System.getProperty("project.policy.artifact.mode", "verify");
        if ("generate".equals(mode)) {
            ProjectPolicyArtifactGenerator.updateTracked(TRACKED_ARTIFACT, artifact);
        } else if (!"verify".equals(mode)) {
            throw new IllegalStateException("지원하지 않는 Project policy artifact mode입니다: " + mode);
        }

        ProjectPolicyArtifactGenerator.verifyTracked(TRACKED_ARTIFACT, artifact);
        assertThat(artifact.fingerprint())
            .isEqualTo(ProjectPolicyArtifactGenerator.EXPECTED_POLICY_FINGERPRINT);
        assertThat(artifact.statementCount()).isEqualTo(89);
        assertThat(artifact.surfaceCount()).isEqualTo(46);
        assertThat(artifact.restCount()).isEqualTo(37);
        assertThat(artifact.graphQlCount()).isEqualTo(8);
        assertThat(artifact.schedulerCount()).isOne();
        assertThat(Files.readAllBytes(TRACKED_ARTIFACT)).isEqualTo(artifact.bytes());
    }
}
