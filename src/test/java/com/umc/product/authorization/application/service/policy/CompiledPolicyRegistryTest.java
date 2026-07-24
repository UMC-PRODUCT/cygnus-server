package com.umc.product.authorization.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.umc.product.authorization.application.port.out.policy.CompiledPolicyContractValidator;
import com.umc.product.authorization.application.port.out.policy.PolicyBundleContributor;
import com.umc.product.authorization.application.port.out.policy.PolicyClasspathResource;
import com.umc.product.authorization.application.port.out.policy.PolicyResourceManifest;
import com.umc.product.authorization.domain.policy.PolicyBundleKey;
import com.umc.product.authorization.domain.policy.PolicyDomainSchema;

class CompiledPolicyRegistryTest {

    @TempDir
    private Path tempDirectory;

    @Test
    @DisplayName("여러 도메인 bundle을 namespace 순서와 무관하게 한 번씩 compile한다")
    void compilesMultipleDomainBundlesOnce() throws Exception {
        writePolicy("zeta");
        writePolicy("alpha");
        AtomicBoolean alphaValidated = new AtomicBoolean();
        AtomicBoolean zetaValidated = new AtomicBoolean();

        try (URLClassLoader classLoader = classLoader()) {
            CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
                new PolicySemanticCompiler(),
                List.of(
                    contributor("zeta", zetaValidated),
                    contributor("alpha", alphaValidated)),
                classLoader);

            assertThat(registry.bundles().keySet()).containsExactly(
                new PolicyBundleKey("alpha", "test-1.0"),
                new PolicyBundleKey("zeta", "test-1.0"));
            assertThat(registry.require("alpha").namespace()).isEqualTo("alpha");
            assertThat(registry.require(new PolicyBundleKey("zeta", "test-1.0")).namespace())
                .isEqualTo("zeta");
            assertThat(alphaValidated).isTrue();
            assertThat(zetaValidated).isTrue();
        }
    }

    @Test
    @DisplayName("같은 namespace contributor가 둘이면 startup registry 생성을 거부한다")
    void rejectsDuplicateNamespace() throws Exception {
        writePolicy("duplicate");

        try (URLClassLoader classLoader = classLoader()) {
            PolicyBundleContributor contributor = contributor("duplicate", new AtomicBoolean());

            assertThatThrownBy(() -> new CompiledPolicyRegistry(
                new PolicySemanticCompiler(),
                List.of(contributor, contributor),
                classLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("namespace가 중복");
        }
    }

    @Test
    @DisplayName("한 contributor의 resource가 누락되면 일부 bundle만 가진 registry를 만들지 않는다")
    void rejectsMissingResourceAtomically() throws Exception {
        writePolicy("valid");

        try (URLClassLoader classLoader = classLoader()) {
            assertThatThrownBy(() -> new CompiledPolicyRegistry(
                new PolicySemanticCompiler(),
                List.of(
                    contributor("valid", new AtomicBoolean()),
                    contributor("missing", new AtomicBoolean())),
                classLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("resource가 없습니다");
        }
    }

    @Test
    @DisplayName("bundle envelope namespace와 contributor namespace가 다르면 startup을 거부한다")
    void rejectsNamespaceMismatch() throws Exception {
        writePolicy("source");

        try (URLClassLoader classLoader = classLoader()) {
            PolicyBundleContributor mismatched = new TestContributor(
                "target",
                manifest("source"),
                CompiledPolicyContractValidator.NO_OP);

            assertThatThrownBy(() -> new CompiledPolicyRegistry(
                new PolicySemanticCompiler(),
                List.of(mismatched),
                classLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("namespace가 일치하지 않습니다");
        }
    }

    @Test
    @DisplayName("compile된 bundle은 action별 statement index를 제공한다")
    void indexesCompiledStatementsByAction() throws Exception {
        writePolicy("indexed");

        try (URLClassLoader classLoader = classLoader()) {
            CompiledPolicyRegistry registry = new CompiledPolicyRegistry(
                new PolicySemanticCompiler(),
                List.of(contributor("indexed", new AtomicBoolean())),
                classLoader);

            assertThat(registry.require("indexed").statementsForAction("project:read"))
                .extracting(statement -> statement.id())
                .containsExactly("allow-active");
            assertThat(registry.require("indexed").statementsForAction("project:missing")).isEmpty();
        }
    }

    private PolicyBundleContributor contributor(String namespace, AtomicBoolean validated) {
        return new TestContributor(namespace, manifest(namespace), bundle -> validated.set(true));
    }

    private PolicyResourceManifest manifest(String namespace) {
        return new PolicyResourceManifest(
            new PolicyClasspathResource("bundle.json", "policies/" + namespace + "/bundle.json"),
            List.of(new PolicyClasspathResource(
                "demo.policy.json",
                "policies/" + namespace + "/demo.policy.json")));
    }

    private void writePolicy(String namespace) throws Exception {
        Path directory = tempDirectory.resolve("policies").resolve(namespace);
        Files.createDirectories(directory);
        Files.writeString(
            directory.resolve("bundle.json"),
            namespace(PolicyCompilerTestFixture.VALID_BUNDLE, namespace));
        Files.writeString(
            directory.resolve("demo.policy.json"),
            namespace(
                PolicyCompilerTestFixture.module(PolicyCompilerTestFixture.BOOLEAN_PREDICATE),
                namespace));
    }

    private String namespace(String source, String namespace) {
        return source.replace("\"namespace\": \"test\"", "\"namespace\": \"" + namespace + "\"");
    }

    private URLClassLoader classLoader() throws Exception {
        return new URLClassLoader(new URL[]{tempDirectory.toUri().toURL()}, null);
    }

    private record TestContributor(
        String namespace,
        PolicyResourceManifest resourceManifest,
        CompiledPolicyContractValidator compiledContractValidator
    ) implements PolicyBundleContributor {

        @Override
        public PolicyDomainSchema domainSchema() {
            return PolicyCompilerTestFixture.schema();
        }
    }
}
