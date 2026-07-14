package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicySurfaceCatalog;
import com.umc.product.project.application.authorization.ProjectPolicySurfaceDescriptor;

class ProjectAuthorizationCallerAuditTest {

    private static final Path PROJECT_SOURCE = Path.of("src/main/java/com/umc/product/project");

    @Test
    @DisplayName("39개 action은 하나의 rollout coordinator seam으로 수렴한다")
    void auditsAllActionsAndSingleCoordinatorSeam() {
        Map<String, String> sources = readProjectSources();
        var surfaceActions = ProjectPolicySurfaceCatalog.surfaces().stream()
            .map(ProjectPolicySurfaceDescriptor::action)
            .collect(Collectors.toUnmodifiableSet());
        Map<String, Long> coordinatorCalls = occurrences(sources, "coordinator.coordinate(");

        assertThat(ProjectPolicyAction.values()).hasSize(39);
        assertThat(surfaceActions).hasSize(38);
        assertThat(ProjectPolicyAction.nonSurfaceActions())
            .containsExactly(ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT);
        assertThat(Stream.concat(surfaceActions.stream(), ProjectPolicyAction.nonSurfaceActions().stream()))
            .containsExactlyInAnyOrder(ProjectPolicyAction.values());
        assertThat(coordinatorCalls).containsExactlyInAnyOrderEntriesOf(Map.of(
            "application/authorization/ProjectPolicyAuthorizationService.java", 1L
        ));
    }

    @Test
    @DisplayName("generic permission은 public project 목록 precheck 한 곳만 유지한다")
    void allowsOnlyPublicProjectListPrecheckWithoutResourceId() {
        Map<String, String> sources = readProjectSources();

        assertThat(occurrences(sources, "permission.resourceId() == null"))
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "application/service/evaluator/ProjectPermissionEvaluator.java", 1L
            ));
        assertThat(occurrences(sources, "allowsBroadProjectCreate")).isEmpty();
        assertThat(occurrences(sources, "case WRITE -> false"))
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "application/service/evaluator/ProjectPermissionEvaluator.java", 1L
            ));
    }

    private Map<String, String> readProjectSources() {
        try (Stream<Path> paths = Files.walk(PROJECT_SOURCE)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                .collect(Collectors.toUnmodifiableMap(this::relative, this::read));
        } catch (IOException exception) {
            throw new UncheckedIOException("Project authorization caller를 읽을 수 없습니다.", exception);
        }
    }

    private Map<String, Long> occurrences(Map<String, String> sources, String needle) {
        return sources.entrySet().stream()
            .filter(entry -> entry.getValue().contains(needle))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry ->
                entry.getValue().split(java.util.regex.Pattern.quote(needle), -1).length - 1L));
    }

    private String relative(Path path) {
        return PROJECT_SOURCE.relativize(path).toString();
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new UncheckedIOException("Project source를 읽을 수 없습니다: " + path, exception);
        }
    }
}
