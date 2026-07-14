package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.StandardEnvironment;

class ProjectPolicyArtifactMutationTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("raw JSON byte 변경은 compiled fingerprint가 같아도 stale artifact로 거부한다")
    void rejectsIndependentRawJsonMutation() throws Exception {
        ProjectPolicyArtifactGenerator.Artifact baseline = generate(sourcePolicies(), ProjectPolicySurfaceCatalog.surfaces());
        Map<String, byte[]> mutation = sourcePolicies();
        byte[] original = mutation.get("form.policy.json");
        mutation.put("form.policy.json", (new String(original, StandardCharsets.UTF_8) + "\n")
            .getBytes(StandardCharsets.UTF_8));
        ProjectPolicyArtifactGenerator.Artifact changed = generate(mutation, ProjectPolicySurfaceCatalog.surfaces());
        Path tracked = writeTracked(baseline);

        assertThat(changed.fingerprint()).isEqualTo(baseline.fingerprint());
        assertThatThrownBy(() -> ProjectPolicyArtifactGenerator.verifyTracked(tracked, changed))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("stale");
    }

    @Test
    @DisplayName("runtime catalog action 변경은 stale artifact로 거부한다")
    void rejectsIndependentCatalogMutation() throws Exception {
        List<ProjectPolicySurfaceDescriptor> mutation = new ArrayList<>(ProjectPolicySurfaceCatalog.surfaces());
        ProjectPolicySurfaceDescriptor original = mutation.getFirst();
        mutation.set(0, new ProjectPolicySurfaceDescriptor(
            original.identity(),
            original.type(),
            ProjectPolicyAction.PROJECT_READ,
            ProjectPolicyModule.PROJECT_RESOURCE,
            original.gate()));
        ProjectPolicyArtifactGenerator.Artifact baseline = generate(sourcePolicies(), ProjectPolicySurfaceCatalog.surfaces());
        ProjectPolicyArtifactGenerator.Artifact changed = generate(sourcePolicies(), List.copyOf(mutation));
        Path tracked = writeTracked(baseline);

        assertThatThrownBy(() -> ProjectPolicyArtifactGenerator.verifyTracked(tracked, changed))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("stale");
    }

    @Test
    @DisplayName("tracked artifact mismatch 검증은 파일을 자동 덮어쓰지 않는다")
    void verificationNeverOverwritesTrackedArtifact() throws Exception {
        ProjectPolicyArtifactGenerator.Artifact expected = generate(sourcePolicies(), ProjectPolicySurfaceCatalog.surfaces());
        Path tracked = tempDirectory.resolve("project-policy-artifacts.md");
        byte[] stale = "stale\n".getBytes(StandardCharsets.UTF_8);
        Files.write(tracked, stale);

        assertThatThrownBy(() -> ProjectPolicyArtifactGenerator.verifyTracked(tracked, expected))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("stale");
        assertThat(Files.readAllBytes(tracked)).isEqualTo(stale);
    }

    private ProjectPolicyArtifactGenerator.Artifact generate(
        Map<String, byte[]> policies,
        List<ProjectPolicySurfaceDescriptor> catalog
    ) {
        Set<ProjectPolicySurfaceIdentity> discovered =
            ProjectPolicyRuntimeSurfaceDiscovery.discoverAnnotatedSurfaces(new StandardEnvironment());
        return ProjectPolicyArtifactGenerator.generate(policies, discovered, catalog);
    }

    private Map<String, byte[]> sourcePolicies() throws Exception {
        return new LinkedHashMap<>(ProjectPolicyArtifactGenerator.sourcePolicies(PROJECT_ROOT));
    }

    private Path writeTracked(ProjectPolicyArtifactGenerator.Artifact artifact) throws Exception {
        Path tracked = tempDirectory.resolve("project-policy-artifacts.md");
        Files.write(tracked, artifact.bytes());
        return tracked;
    }
}
