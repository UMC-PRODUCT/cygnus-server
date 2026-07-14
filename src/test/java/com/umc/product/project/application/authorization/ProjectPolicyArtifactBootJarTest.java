package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;

import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEnforcementReceiptDocument;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEnforcementReceiptJsonParser;

class ProjectPolicyArtifactBootJarTest {

    private static final String POLICY_PREFIX = "BOOT-INF/classes/policies/project/";
    private static final String OLD_FORM_POLICY_ENTRY = POLICY_PREFIX + "form.policy.json";
    private static final String GENERATED_ARTIFACT_ENTRY =
        POLICY_PREFIX + "generated/project-policy-artifacts.md";
    private static final String RECEIPT_ENTRY =
        POLICY_PREFIX + "rollout/enforcement-receipts.json";
    private static final String APPLICATION_YML_ENTRY = "BOOT-INF/classes/application.yml";
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    @DisplayName("bootJar의 raw policy 8개로 재컴파일한 artifact가 tracked artifact와 일치한다")
    void verifiesBootJarPolicyResourcesAndTrackedArtifact() throws Exception {
        String configuredJar = System.getProperty("project.policy.artifact.bootJar");
        if (configuredJar == null || configuredJar.isBlank()) {
            throw new IllegalStateException("verifyPackagedProjectPolicyArtifacts task로 실행해야 합니다.");
        }

        Path bootJar = Path.of(configuredJar);
        Map<String, byte[]> packaged = readPackagedPolicies(bootJar);
        assertThat(packaged.keySet()).containsExactlyInAnyOrderElementsOf(ProjectPolicyArtifactGenerator.POLICY_FILES);
        assertEntryAbsent(bootJar, OLD_FORM_POLICY_ENTRY);
        assertPackagedBytesEqualSources(packaged);
        assertPackagedRolloutResources(bootJar);

        ProjectPolicyArtifactGenerator.Artifact artifact = ProjectPolicyArtifactGenerator.generate(
            packaged,
            ProjectPolicyRuntimeSurfaceDiscovery.discoverAnnotatedSurfaces(new StandardEnvironment()),
            ProjectPolicySurfaceCatalog.surfaces());
        ProjectPolicyArtifactGenerator.verifyTracked(
            PROJECT_ROOT.resolve("src/main/resources/policies/project/generated/project-policy-artifacts.md"),
            artifact);
    }

    private void assertPackagedRolloutResources(Path bootJar) throws IOException {
        byte[] generatedArtifact = readExactEntry(bootJar, GENERATED_ARTIFACT_ENTRY);
        byte[] receipt = readExactEntry(bootJar, RECEIPT_ENTRY);
        byte[] applicationYml = readExactEntry(bootJar, APPLICATION_YML_ENTRY);

        assertThat(generatedArtifact).isEqualTo(Files.readAllBytes(PROJECT_ROOT.resolve(
            "src/main/resources/policies/project/generated/project-policy-artifacts.md")));
        assertThat(receipt).isEqualTo(Files.readAllBytes(PROJECT_ROOT.resolve(
            "src/main/resources/policies/project/rollout/enforcement-receipts.json")));

        ProjectAuthorizationEnforcementReceiptDocument document =
            new ProjectAuthorizationEnforcementReceiptJsonParser().parse(receipt);
        assertThat(document.schemaVersion()).isEqualTo("1.0");
        assertThat(document.receipts()).isEmpty();

        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ByteArrayResource(applicationYml));
        Properties properties = factory.getObject();
        assertThat(properties).containsEntry(
            "app.project.authorization-rollout.default-mode",
            "${PROJECT_AUTHORIZATION_ROLLOUT_DEFAULT_MODE:SHADOW}"
        );
    }

    private byte[] readExactEntry(Path bootJar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(bootJar.toFile())) {
            java.util.List<? extends ZipEntry> matches = java.util.Collections.list(zip.entries()).stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().equals(entryName))
                .toList();
            assertThat(matches).as("bootJar entry: %s", entryName).hasSize(1);
            return zip.getInputStream(matches.getFirst()).readAllBytes();
        }
    }

    private Map<String, byte[]> readPackagedPolicies(Path bootJar) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (ProjectPolicyResourceManifest.PolicyResource resource : ProjectPolicyResourceManifest.POLICIES) {
            result.put(
                resource.logicalFilename(),
                readExactEntry(bootJar, "BOOT-INF/classes/" + resource.classpathPath())
            );
        }
        return result;
    }

    private void assertPackagedBytesEqualSources(Map<String, byte[]> packaged) throws IOException {
        Map<String, byte[]> sources = ProjectPolicyArtifactGenerator.sourcePolicies(PROJECT_ROOT);
        for (String filename : ProjectPolicyArtifactGenerator.POLICY_FILES) {
            assertThat(packaged.get(filename))
                .as("bootJar raw policy bytes: %s", filename)
                .isEqualTo(sources.get(filename));
        }
    }

    private void assertEntryAbsent(Path bootJar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(bootJar.toFile())) {
            assertThat(zip.getEntry(entryName)).as("bootJar stale entry: %s", entryName).isNull();
        }
    }
}
