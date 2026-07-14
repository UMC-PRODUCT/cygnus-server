package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectAuthorizationEnforcementReceiptLoaderTest {

    private static final String ARTIFACT_SHA =
        "2dd6403da68c43d3ccd8a864fe50f8d597a513f4c3f7080948ae29ac29205564";

    @Test
    @DisplayName("fixed receipt를 strict parse하고 generated artifact bytes의 SHA-256을 계산한다")
    void fixed_receipt와_generated_artifact를_load한다() {
        LoadedProjectAuthorizationEnforcementReceipts loaded =
            new ProjectAuthorizationEnforcementReceiptLoader().load();

        assertThat(loaded.document().receipts()).isEmpty();
        assertThat(loaded.artifactSha256()).isEqualTo(ARTIFACT_SHA);
    }

    @Test
    @DisplayName("receipt resource가 없으면 empty receipt로 SHADOW와 LEGACY rollback을 지원한다")
    void receipt_resource가_없으면_empty_receipt로_load한다() {
        ClassLoader loader = new MissingResourceClassLoader(
            getClass().getClassLoader(), ProjectAuthorizationEnforcementReceiptLoader.RECEIPT_PATH);

        LoadedProjectAuthorizationEnforcementReceipts loaded =
            new ProjectAuthorizationEnforcementReceiptLoader(loader).load();

        assertThat(loaded.document().receipts()).isEmpty();
        assertThat(loaded.artifactSha256()).isEqualTo(ARTIFACT_SHA);
    }

    @Test
    @DisplayName("receipt resource가 중복되면 stable code로 startup을 거부한다")
    void receipt_resource가_중복되면_거부한다() {
        ClassLoader loader = new DuplicateResourceClassLoader(
            getClass().getClassLoader(), ProjectAuthorizationEnforcementReceiptLoader.RECEIPT_PATH);

        assertFailure(loader, ProjectAuthorizationRolloutFailureCode.RECEIPT_RESOURCE_DUPLICATED);
    }

    @Test
    @DisplayName("generated artifact resource가 누락되면 stable code로 startup을 거부한다")
    void generated_artifact_resource가_누락되면_거부한다() {
        ClassLoader loader = new MissingResourceClassLoader(
            getClass().getClassLoader(), ProjectAuthorizationEnforcementReceiptLoader.ARTIFACT_PATH);

        assertFailure(loader, ProjectAuthorizationRolloutFailureCode.POLICY_ARTIFACT_RESOURCE_MISSING);
    }

    @Test
    @DisplayName("generated artifact resource가 중복되면 stable code로 startup을 거부한다")
    void generated_artifact_resource가_중복되면_거부한다() {
        ClassLoader loader = new DuplicateResourceClassLoader(
            getClass().getClassLoader(), ProjectAuthorizationEnforcementReceiptLoader.ARTIFACT_PATH);

        assertFailure(loader, ProjectAuthorizationRolloutFailureCode.POLICY_ARTIFACT_RESOURCE_DUPLICATED);
    }

    @Test
    @DisplayName("production loader는 외부 path나 URL을 받는 public API를 제공하지 않는다")
    void production_loader는_외부_resource_주입_api를_제공하지_않는다() {
        assertThat(ProjectAuthorizationEnforcementReceiptLoader.class.getConstructors())
            .singleElement()
            .satisfies(constructor -> assertThat(constructor.getParameterCount()).isZero());
    }

    private void assertFailure(ClassLoader classLoader, ProjectAuthorizationRolloutFailureCode code) {
        assertThatThrownBy(() -> new ProjectAuthorizationEnforcementReceiptLoader(classLoader).load())
            .isInstanceOfSatisfying(
                ProjectAuthorizationRolloutConfigurationException.class,
                exception -> assertThat(exception.code()).isEqualTo(code)
            );
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
