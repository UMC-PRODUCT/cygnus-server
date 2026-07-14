package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.CompiledPolicyModule;
import com.umc.product.authorization.domain.policy.CompiledPolicyOutcome;
import com.umc.product.authorization.domain.policy.CompiledPolicyStatement;
import com.umc.product.authorization.domain.policy.PolicyOperand;
import com.umc.product.authorization.domain.policy.PolicyValue;

class ProjectPolicyBundleLoaderTest {

    @Test
    @DisplayName("고정 classpath의 일곱 module을 strict compile하고 exact Project 계약을 검증한다")
    void loadsAndCompilesExactBundle() {
        ProjectPolicyBundleLoader loader = new ProjectPolicyBundleLoader(new PolicySemanticCompiler());
        CompiledPolicyBundle bundle = loader.compiled().value();

        assertThat(bundle.schemaVersion()).isEqualTo("1.0");
        assertThat(bundle.contextSchemaVersion()).isEqualTo("project-1.0");
        assertThat(bundle.namespace()).isEqualTo("project");
        assertThat(bundle.policyVersion()).isEqualTo("1.1.0");
        assertThat(bundle.policyFingerprint()).matches("[0-9a-f]{64}");
        assertThat(bundle.modules()).hasSize(7);
        assertThat(bundle.modules()).extracting(module -> module.statements().size())
            .containsExactlyInAnyOrder(32, 10, 12, 9, 10, 11, 5);
    }

    @Test
    @DisplayName("고정 classpath module이 누락되거나 중복되면 startup compile을 거부한다")
    void rejectsMissingAndDuplicateClasspathResources() {
        ClassLoader delegate = getClass().getClassLoader();

        assertThatThrownBy(() -> new ProjectPolicyBundleLoader(
            new PolicySemanticCompiler(), new MissingResourceClassLoader(delegate, "policies/form/form.policy.json")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("resource가 없습니다");
        assertThatThrownBy(() -> new ProjectPolicyBundleLoader(
            new PolicySemanticCompiler(), new DuplicateResourceClassLoader(delegate, "policies/project/bundle.json")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("resource가 중복");
    }

    @Test
    @DisplayName("Form policy source는 Form 소유 경로에만 존재한다")
    void ownsFormPolicyUnderFormDirectoryOnly() {
        Path policyRoot = Path.of("src/main/resources/policies");

        assertThat(Files.isRegularFile(policyRoot.resolve("form/form.policy.json"))).isTrue();
        assertThat(Files.exists(policyRoot.resolve("project/form.policy.json"))).isFalse();
    }

    @Test
    @DisplayName("한 scope statement가 selector 두 개를 방출하면 Project contract validation이 실패한다")
    void rejectsMultipleSelectorDimensionsInOneStatement() {
        CompiledPolicyBundle original = new ProjectPolicyBundleLoader(new PolicySemanticCompiler()).compiled().value();
        List<CompiledPolicyModule> modules = new ArrayList<>(original.modules());
        int moduleIndex = java.util.stream.IntStream.range(0, modules.size())
            .filter(index -> modules.get(index).id().equals("project-scope"))
            .findFirst()
            .orElseThrow();
        CompiledPolicyModule scope = modules.get(moduleIndex);
        List<CompiledPolicyStatement> statements = new ArrayList<>(scope.statements());
        CompiledPolicyStatement source = statements.stream()
            .filter(statement -> statement.id().equals("project.list-public.member-public"))
            .findFirst()
            .orElseThrow();
        List<CompiledPolicyOutcome> outcomes = new ArrayList<>(source.outcomes());
        outcomes.add(new CompiledPolicyOutcome(
            ProjectPolicyOutcomes.PROJECT_GISU_IDS,
            new PolicyOperand.Literal(new PolicyValue.LongSetValue(java.util.Set.of(1L)))));
        statements.set(statements.indexOf(source), new CompiledPolicyStatement(
            source.id(), source.actions(), source.effect(), source.condition(), outcomes));
        modules.set(moduleIndex, new CompiledPolicyModule(scope.id(), scope.filename(), statements));
        CompiledPolicyBundle mutated = new CompiledPolicyBundle(
            original.schemaVersion(), original.contextSchemaVersion(), original.namespace(),
            original.policyVersion(), original.defaultEffect(), original.combiningAlgorithm(),
            original.domainSchema(), original.policyFingerprint(), modules);

        assertThatThrownBy(() -> new ProjectPolicyCompiledContractValidator().validate(mutated))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("selector 차원을 하나만");
    }

    @Test
    @DisplayName("Project scope statement가 selector를 방출하지 않으면 Project contract validation이 실패한다")
    void rejectsMissingScopeSelector() {
        CompiledPolicyBundle original = new ProjectPolicyBundleLoader(new PolicySemanticCompiler()).compiled().value();
        CompiledPolicyBundle mutated = replaceStatementOutcomes(
            original, "project-scope", "project.list-public.member-public", List.of());

        assertThatThrownBy(() -> new ProjectPolicyCompiledContractValidator().validate(mutated))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("selector를 정확히 하나");
    }

    @Test
    @DisplayName("Scope modifier만 있고 selector가 없으면 Project contract validation이 실패한다")
    void rejectsModifierOnlyScopeOutcome() {
        CompiledPolicyBundle original = new ProjectPolicyBundleLoader(new PolicySemanticCompiler()).compiled().value();
        CompiledPolicyBundle mutated = replaceStatementOutcomes(
            original,
            "project-scope",
            "project.list-managed.owner",
            List.of(new CompiledPolicyOutcome(
                ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS,
                new PolicyOperand.Literal(new PolicyValue.BooleanValue(true)))));

        assertThatThrownBy(() -> new ProjectPolicyCompiledContractValidator().validate(mutated))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("selector를 정확히 하나");
    }

    @Test
    @DisplayName("Project scope statement가 application selector를 방출하면 validation이 실패한다")
    void rejectsWrongSelectorFamily() {
        CompiledPolicyBundle original = new ProjectPolicyBundleLoader(new PolicySemanticCompiler()).compiled().value();
        CompiledPolicyBundle mutated = replaceStatementOutcomes(
            original,
            "project-scope",
            "project.list-public.member-public",
            List.of(new CompiledPolicyOutcome(
                ProjectPolicyOutcomes.APPLICATION_ALL,
                new PolicyOperand.Literal(new PolicyValue.BooleanValue(true)))));

        assertThatThrownBy(() -> new ProjectPolicyCompiledContractValidator().validate(mutated))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("project selector를 정확히 하나");
    }

    @Test
    @DisplayName("Non-scope statement가 scope selector를 방출하면 validation이 실패한다")
    void rejectsSelectorOnNonScopeAction() {
        CompiledPolicyBundle original = new ProjectPolicyBundleLoader(new PolicySemanticCompiler()).compiled().value();
        CompiledPolicyBundle mutated = replaceStatementOutcomes(
            original,
            "project-resource",
            "project.read.public",
            List.of(new CompiledPolicyOutcome(
                ProjectPolicyOutcomes.PROJECT_ALL,
                new PolicyOperand.Literal(new PolicyValue.BooleanValue(true)))));

        assertThatThrownBy(() -> new ProjectPolicyCompiledContractValidator().validate(mutated))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Non-scope statement는 selector outcome을");
    }

    @Test
    @DisplayName("compiled target matrix는 version과 fingerprint를 포함해 deterministic하다")
    void rendersDeterministicPolicyMatrix() {
        CompiledPolicyBundle bundle = new ProjectPolicyBundleLoader(new PolicySemanticCompiler()).compiled().value();

        String first = ProjectCompiledPolicyMatrix.render(bundle);
        String second = ProjectCompiledPolicyMatrix.render(bundle);

        assertThat(first).isEqualTo(second)
            .contains(bundle.policyFingerprint())
            .contains("project.read.chapter-review")
            .contains("project-application:list-management");
    }

    private static final class MissingResourceClassLoader extends ClassLoader {
        private final String missing;

        private MissingResourceClassLoader(ClassLoader parent, String missing) {
            super(parent);
            this.missing = missing;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return name.equals(missing) ? Collections.emptyEnumeration() : super.getResources(name);
        }
    }

    private CompiledPolicyBundle replaceStatementOutcomes(
        CompiledPolicyBundle original,
        String moduleId,
        String statementId,
        List<CompiledPolicyOutcome> replacement
    ) {
        List<CompiledPolicyModule> modules = new ArrayList<>(original.modules());
        int moduleIndex = java.util.stream.IntStream.range(0, modules.size())
            .filter(index -> modules.get(index).id().equals(moduleId))
            .findFirst()
            .orElseThrow();
        CompiledPolicyModule module = modules.get(moduleIndex);
        List<CompiledPolicyStatement> statements = new ArrayList<>(module.statements());
        CompiledPolicyStatement source = statements.stream()
            .filter(statement -> statement.id().equals(statementId))
            .findFirst()
            .orElseThrow();
        statements.set(statements.indexOf(source), new CompiledPolicyStatement(
            source.id(), source.actions(), source.effect(), source.condition(), replacement));
        modules.set(moduleIndex, new CompiledPolicyModule(module.id(), module.filename(), statements));
        return new CompiledPolicyBundle(
            original.schemaVersion(), original.contextSchemaVersion(), original.namespace(),
            original.policyVersion(), original.defaultEffect(), original.combiningAlgorithm(),
            original.domainSchema(), original.policyFingerprint(), modules);
    }

    private static final class DuplicateResourceClassLoader extends ClassLoader {
        private final String duplicated;

        private DuplicateResourceClassLoader(ClassLoader parent, String duplicated) {
            super(parent);
            this.duplicated = duplicated;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            Enumeration<URL> resources = super.getResources(name);
            if (!name.equals(duplicated) || !resources.hasMoreElements()) {
                return resources;
            }
            URL resource = resources.nextElement();
            return Collections.enumeration(List.of(resource, resource));
        }
    }
}
