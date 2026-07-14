package com.umc.product.project.application.authorization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.umc.product.authorization.application.port.in.policy.PolicyBundleCompilationRequest;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;

final class ProjectPolicyArtifactGenerator {

    static final String EXPECTED_POLICY_FINGERPRINT =
        "7cb5dd0453429ae766788952ae1972c9e24ae2c8f137dfa983a8bbe28a3f5e2e";
    static final String BUNDLE_FILE = ProjectPolicyResourceManifest.BUNDLE.logicalFilename();
    static final List<String> MODULE_FILES = ProjectPolicyResourceManifest.MODULES.stream()
        .map(ProjectPolicyResourceManifest.PolicyResource::logicalFilename)
        .sorted()
        .toList();
    static final List<String> POLICY_FILES = ProjectPolicyResourceManifest.POLICIES.stream()
        .map(ProjectPolicyResourceManifest.PolicyResource::logicalFilename)
        .sorted()
        .toList();

    private ProjectPolicyArtifactGenerator() {
    }

    static Artifact fromSources(
        Path projectRoot,
        Set<ProjectPolicySurfaceIdentity> discovered,
        List<ProjectPolicySurfaceDescriptor> catalog
    ) throws IOException {
        return generate(sourcePolicies(projectRoot), discovered, catalog);
    }

    static Map<String, byte[]> sourcePolicies(Path projectRoot) throws IOException {
        Path resources = projectRoot.resolve("src/main/resources");
        Map<String, byte[]> rawPolicies = new LinkedHashMap<>();
        List<ProjectPolicyResourceManifest.PolicyResource> policies = ProjectPolicyResourceManifest.POLICIES.stream()
            .sorted(Comparator.comparing(ProjectPolicyResourceManifest.PolicyResource::logicalFilename))
            .toList();
        for (ProjectPolicyResourceManifest.PolicyResource resource : policies) {
            rawPolicies.put(resource.logicalFilename(), Files.readAllBytes(resources.resolve(resource.classpathPath())));
        }
        return rawPolicies;
    }

    static Artifact generate(
        Map<String, byte[]> rawPolicies,
        Set<ProjectPolicySurfaceIdentity> discovered,
        List<ProjectPolicySurfaceDescriptor> catalog
    ) {
        requireExactPolicyFiles(rawPolicies.keySet());
        Map<String, byte[]> modules = MODULE_FILES.stream().collect(Collectors.toMap(
            Function.identity(),
            filename -> rawPolicies.get(filename).clone(),
            (first, second) -> first,
            LinkedHashMap::new));
        CompiledPolicyBundle bundle = new PolicySemanticCompiler().compile(new PolicyBundleCompilationRequest(
            rawPolicies.get(BUNDLE_FILE), modules, ProjectPolicyDomainSchema.create()));
        new ProjectPolicyCompiledContractValidator().validate(bundle);
        if (!EXPECTED_POLICY_FINGERPRINT.equals(bundle.policyFingerprint())) {
            throw new IllegalStateException(
                "Project policy fingerprint가 승인값과 다릅니다: " + bundle.policyFingerprint());
        }

        List<ProjectPolicySurfaceDescriptor> joined = joinRuntimeSurfaces(discovered, catalog);
        int statementCount = bundle.modules().stream()
            .mapToInt(module -> module.statements().size())
            .sum();
        if (statementCount != 89) {
            throw new IllegalStateException(
                "Project compiled policy statement 수가 일치하지 않습니다: " + statementCount);
        }
        long restCount = count(discovered, "rest:");
        long graphQlCount = count(discovered, "graphql:");
        long schedulerCount = count(discovered, "scheduler:");
        if (restCount != 37 || graphQlCount != 8 || schedulerCount != 1 || discovered.size() != 46) {
            throw new IllegalStateException(
                "Project runtime 호출면 수가 일치하지 않습니다: rest=" + restCount
                    + ", graphql=" + graphQlCount
                    + ", scheduler=" + schedulerCount
                    + ", total=" + discovered.size());
        }

        byte[] bytes = render(bundle, rawPolicies, joined, restCount, graphQlCount, schedulerCount)
            .getBytes(StandardCharsets.UTF_8);
        return new Artifact(
            bytes,
            bundle.policyFingerprint(),
            statementCount,
            joined.size(),
            Math.toIntExact(restCount),
            Math.toIntExact(graphQlCount),
            Math.toIntExact(schedulerCount));
    }

    static void verifyTracked(Path tracked, Artifact expected) throws IOException {
        if (!Files.isRegularFile(tracked)) {
            throw new IllegalStateException("tracked Project policy artifact가 없습니다: " + tracked);
        }
        byte[] actual = Files.readAllBytes(tracked);
        if (!Arrays.equals(actual, expected.bytes())) {
            throw new IllegalStateException(
                "tracked Project policy artifact가 stale입니다. "
                    + "./gradlew generateProjectPolicyArtifacts 실행 후 변경 내용을 검토하세요.");
        }
    }

    static void updateTracked(Path tracked, Artifact artifact) throws IOException {
        Files.createDirectories(tracked.getParent());
        Files.write(tracked, artifact.bytes());
    }

    private static List<ProjectPolicySurfaceDescriptor> joinRuntimeSurfaces(
        Set<ProjectPolicySurfaceIdentity> discovered,
        List<ProjectPolicySurfaceDescriptor> catalog
    ) {
        Map<ProjectPolicySurfaceIdentity, ProjectPolicySurfaceDescriptor> catalogByIdentity = catalog.stream()
            .collect(Collectors.toMap(
                ProjectPolicySurfaceDescriptor::identity,
                Function.identity(),
                (first, second) -> {
                    throw new IllegalStateException("Project runtime catalog identity가 중복되었습니다: " + first.id());
                }));
        Set<ProjectPolicySurfaceIdentity> catalogIdentities = Set.copyOf(catalogByIdentity.keySet());
        if (!catalogIdentities.equals(discovered)) {
            Set<ProjectPolicySurfaceIdentity> missing = catalogIdentities.stream()
                .filter(identity -> !discovered.contains(identity))
                .collect(Collectors.toSet());
            Set<ProjectPolicySurfaceIdentity> unexpected = discovered.stream()
                .filter(identity -> !catalogIdentities.contains(identity))
                .collect(Collectors.toSet());
            throw new IllegalStateException(
                "Project runtime 호출면과 catalog가 일치하지 않습니다. missing=" + sortedIdentities(missing)
                    + ", unexpected=" + sortedIdentities(unexpected));
        }
        return discovered.stream()
            .sorted(Comparator.comparing(ProjectPolicySurfaceIdentity::id))
            .map(catalogByIdentity::get)
            .toList();
    }

    private static String render(
        CompiledPolicyBundle bundle,
        Map<String, byte[]> rawPolicies,
        List<ProjectPolicySurfaceDescriptor> surfaces,
        long restCount,
        long graphQlCount,
        long schedulerCount
    ) {
        StringBuilder result = new StringBuilder();
        result.append("# Project Policy Generated Review Artifact\n\n");
        result.append("> 이 파일은 생성 산출물입니다. 직접 수정하지 마세요.\n\n");
        result.append("## Contract\n\n");
        result.append("- schemaVersion: `").append(bundle.schemaVersion()).append("`\n");
        result.append("- contextSchemaVersion: `").append(bundle.contextSchemaVersion()).append("`\n");
        result.append("- policyVersion: `").append(bundle.policyVersion()).append("`\n");
        result.append("- policyFingerprint: `").append(bundle.policyFingerprint()).append("`\n");
        result.append("- compiled statements: `89`\n");
        result.append("- runtime surfaces: `").append(surfaces.size()).append("` (REST ")
            .append(restCount).append(", GraphQL ").append(graphQlCount)
            .append(", scheduler ").append(schedulerCount).append(")\n\n");
        appendSourceManifest(result, rawPolicies);
        appendRuntimeCatalog(result, surfaces);
        result.append("## Compiled Target Policy\n\n");
        result.append(ProjectCompiledPolicyMatrix.render(bundle)
            .replaceFirst("# Project Target Policy Matrix\\n\\n", ""));
        return result.toString();
    }

    private static void appendSourceManifest(StringBuilder result, Map<String, byte[]> rawPolicies) {
        result.append("## Raw Policy Source SHA-256\n\n");
        result.append("| Resource | SHA-256 |\n");
        result.append("|---|---|\n");
        rawPolicies.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> result.append("| ").append(entry.getKey()).append(" | `")
                .append(sha256(entry.getValue())).append("` |\n"));
        result.append('\n');
    }

    private static void appendRuntimeCatalog(
        StringBuilder result,
        List<ProjectPolicySurfaceDescriptor> surfaces
    ) {
        result.append("## Runtime Surface Catalog\n\n");
        result.append("| Surface | Handler | Type | Action | Module | Gate |\n");
        result.append("|---|---|---|---|---|---|\n");
        for (ProjectPolicySurfaceDescriptor surface : surfaces) {
            result.append("| ").append(surface.id()).append(" | ")
                .append(surface.handler()).append(" | ")
                .append(surface.type()).append(" | ")
                .append(surface.action().id()).append(" | ")
                .append(surface.module().id()).append(" | ")
                .append(surface.gate()).append(" |\n");
        }
        result.append('\n');
    }

    private static void requireExactPolicyFiles(Set<String> actual) {
        if (!actual.equals(Set.copyOf(POLICY_FILES))) {
            throw new IllegalStateException("Project policy raw resource manifest가 일치하지 않습니다: " + actual);
        }
    }

    private static long count(Set<ProjectPolicySurfaceIdentity> surfaces, String prefix) {
        return surfaces.stream().filter(surface -> surface.id().startsWith(prefix)).count();
    }

    private static List<String> sortedIdentities(Set<ProjectPolicySurfaceIdentity> identities) {
        return identities.stream()
            .map(identity -> identity.id() + " -> " + identity.handler())
            .sorted()
            .toList();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256를 사용할 수 없습니다.", exception);
        }
    }

    record Artifact(
        byte[] content,
        String fingerprint,
        int statementCount,
        int surfaceCount,
        int restCount,
        int graphQlCount,
        int schedulerCount
    ) {

        Artifact {
            content = Objects.requireNonNull(content).clone();
            Objects.requireNonNull(fingerprint);
        }

        byte[] bytes() {
            return content.clone();
        }
    }
}
