package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectAuthorizationPurityArchitectureTest {

    private static final Path AUTHORIZATION =
        Path.of("src/main/java/com/umc/product/project/application/authorization");
    private static final Set<Path> DECISION_BOUNDARIES = Set.of(
        AUTHORIZATION,
        Path.of("src/main/java/com/umc/product/project/application/access"),
        Path.of("src/main/java/com/umc/product/project/application/service/evaluator"),
        Path.of("src/main/java/com/umc/product/project/application/service/policy")
    );
    private static final Set<String> ROLE_FACT_BUILDERS = Set.of(
        "ProjectAuthorizationResourceSnapshotFactory.java",
        "ProjectExpectedDifferenceFacts.java",
        "ProjectPolicyRelationResolver.java",
        "ProjectPolicySubjectSnapshot.java",
        "ProjectPolicySubjectSnapshotLoader.java"
    );

    @Test
    @DisplayName("rollout evaluator와 coordinator는 command와 mutation 경계에 의존하지 않는다")
    void rolloutEvaluationBoundaryHasNoMutationDependency() {
        String sources = read(AUTHORIZATION.resolve("rollout/ProjectAuthorizationRolloutCoordinator.java"))
            + readTree(AUTHORIZATION.resolve("rollout/legacy"))
            + readTree(AUTHORIZATION.resolve("rollout/target"));

        assertThat(sources)
            .doesNotContain("application.port.in.command")
            .doesNotContain("application.service.command")
            .doesNotContain("application.port.out")
            .doesNotContain("adapter.out")
            .doesNotContain("SavePort")
            .doesNotContain("Repository");
        assertThat(Pattern.compile(
            "import com\\.umc\\.product\\.project\\.domain\\.(?!enums\\.)")
            .matcher(sources).find()).isFalse();
    }

    @Test
    @DisplayName("role과 challenger 직접 판정은 fact builder와 legacy 비교 경계 밖에 존재하지 않는다")
    void authorizationDecisionHasNoDirectRoleCheckOutsideFactBuildersAndLegacy() {
        String forbiddenSources = DECISION_BOUNDARIES.stream()
            .map(ProjectAuthorizationPurityArchitectureTest::readDecisionBoundary)
            .collect(Collectors.joining("\n"));

        assertThat(forbiddenSources)
            .doesNotContain(".roleAttributes()")
            .doesNotContain(".gisuChallengerInfos()")
            .doesNotContain(".roles().stream()")
            .doesNotContain(".challengers().stream()");
    }

    private static String readDecisionBoundary(Path root) {
        try (var paths = Files.walk(root)) {
            return paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.toString().contains("/rollout/legacy/"))
                .filter(path -> !ROLE_FACT_BUILDERS.contains(path.getFileName().toString()))
                .map(ProjectAuthorizationPurityArchitectureTest::read)
                .collect(Collectors.joining("\n"));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String readTree(Path root) {
        try (var paths = Files.walk(root)) {
            return paths
                .filter(path -> path.toString().endsWith(".java"))
                .map(ProjectAuthorizationPurityArchitectureTest::read)
                .collect(Collectors.joining("\n"));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
