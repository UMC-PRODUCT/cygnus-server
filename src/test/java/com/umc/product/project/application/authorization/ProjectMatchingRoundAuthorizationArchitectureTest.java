package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.adapter.in.scheduler.MatchingRoundDeadlineHandler;
import com.umc.product.project.adapter.in.web.ProjectMatchingRoundController;
import com.umc.product.project.application.port.in.command.AutoDecideProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.AutoDecisionActor;
import com.umc.product.project.application.service.command.ProjectMatchingRoundCommandService;
import com.umc.product.project.application.service.command.ProjectMatchingRoundFinalizationCommandService;
import com.umc.product.project.application.service.query.ProjectMatchingRoundQueryService;

class ProjectMatchingRoundAuthorizationArchitectureTest {

    private static final Set<ProjectPolicyAction> MATCHING_ACTIONS = Set.of(
        ProjectPolicyAction.MATCHING_LIST,
        ProjectPolicyAction.MATCHING_CREATE,
        ProjectPolicyAction.MATCHING_UPDATE,
        ProjectPolicyAction.MATCHING_DELETE,
        ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE,
        ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE
    );

    @Test
    @DisplayName("auto decide UseCase는 nullable 회원 ID 대신 sealed actor만 받는다")
    void autoDecideUseCaseRequiresActor() {
        Method method = Arrays.stream(AutoDecideProjectMatchingRoundUseCase.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals("autoDecide"))
            .findFirst()
            .orElseThrow();

        assertThat(method.getParameterTypes()).containsExactly(Long.class, AutoDecisionActor.class);
        assertThat(read(ProjectMatchingRoundFinalizationCommandService.class))
            .doesNotContain("autoDecide(Long matchingRoundId, Long")
            .doesNotContain("GetChallengerRoleUseCase");
    }

    @Test
    @DisplayName("REST는 MEMBER actor만 생성하고 deadline handler는 고정 SYSTEM actor만 생성한다")
    void drivingAdaptersConstructOnlyTheirActorKind() {
        String controller = read(ProjectMatchingRoundController.class);
        String handler = read(MatchingRoundDeadlineHandler.class);

        assertThat(controller)
            .contains("new AutoDecisionActor.Member(memberPrincipal.getMemberId())")
            .doesNotContain("AutoDecisionActor.SystemActor")
            .doesNotContain("matchingRoundScheduler()");
        assertThat(handler)
            .contains("AutoDecisionActor.matchingRoundScheduler()")
            .doesNotContain("new AutoDecisionActor.Member")
            .doesNotContain("autoDecide(matchingRoundId, null)");
    }

    @Test
    @DisplayName("매칭 호출면 여섯 개는 각각 정확한 target action으로 연결된다")
    void matchingSurfacesMapToSixExactActions() {
        Map<ProjectPolicyAction, Long> surfaceCounts = ProjectPolicySurfaceCatalog.surfaces().stream()
            .filter(surface -> MATCHING_ACTIONS.contains(surface.action()))
            .collect(Collectors.groupingBy(
                ProjectPolicySurfaceDescriptor::action,
                Collectors.counting()
            ));

        assertThat(surfaceCounts).containsOnly(
            Map.entry(ProjectPolicyAction.MATCHING_LIST, 1L),
            Map.entry(ProjectPolicyAction.MATCHING_CREATE, 1L),
            Map.entry(ProjectPolicyAction.MATCHING_UPDATE, 1L),
            Map.entry(ProjectPolicyAction.MATCHING_DELETE, 1L),
            Map.entry(ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE, 1L),
            Map.entry(ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE, 1L)
        );
        assertThat(read(ProjectMatchingRoundQueryService.class)).contains("ProjectPolicyAction.MATCHING_LIST");
        assertThat(read(ProjectMatchingRoundCommandService.class))
            .contains("ProjectPolicyAction.MATCHING_CREATE")
            .contains("ProjectPolicyAction.MATCHING_UPDATE")
            .contains("ProjectPolicyAction.MATCHING_DELETE");
        assertThat(read(ProjectMatchingRoundFinalizationCommandService.class))
            .contains("ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE")
            .contains("ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE");
    }

    @Test
    @DisplayName("Project policy authorization 경계는 mutation 의존성을 가지지 않는다")
    void projectPolicyAuthorizationBoundaryHasNoMutationDependencies() {
        Path root = Path.of("src/main/java/com/umc/product/project/application/authorization");
        String sources;
        try (var paths = Files.walk(root)) {
            sources = paths
                .filter(path -> path.toString().endsWith(".java"))
                .map(ProjectMatchingRoundAuthorizationArchitectureTest::read)
                .collect(Collectors.joining("\n"));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        assertThat(sources)
            .doesNotContain("com.umc.product.project.application.port.out")
            .doesNotContain("com.umc.product.project.adapter.out")
            .doesNotContain("com.umc.product.project.application.service.command")
            .doesNotContain("SavePort")
            .doesNotContain("Repository");
    }

    private static String read(Class<?> type) {
        String relative = type.getName().replace('.', '/') + ".java";
        return read(Path.of("src/main/java").resolve(relative));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
