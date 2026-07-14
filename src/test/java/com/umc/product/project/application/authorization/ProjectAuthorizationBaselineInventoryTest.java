package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectAuthorizationBaselineInventoryTest {

    private static final Path PROJECT_SOURCE = Path.of("src/main/java/com/umc/product/project");
    private static final Pattern DIRECT_ROLE_USE_CASE = Pattern.compile(
        "private\\s+final\\s+Get(?:ChallengerRole|Challenger)UseCase\\s+"
    );
    private static final Pattern SUBJECT_ROLE_READ = Pattern.compile(
        "getChallengerRoleUseCase\\.findAllByMemberId\\s*\\("
    );
    private static final Pattern ROLE_POLICY_FACT_LOAD = Pattern.compile(
        "getChallengerRoleUseCase\\.[a-zA-Z0-9]+\\s*\\("
    );
    private static final Pattern CHALLENGER_POLICY_FACT_LOAD = Pattern.compile(
        "getChallengerUseCase\\.listPolicyFactsByMemberId\\s*\\("
    );
    private static final Pattern DIRECT_CHECK_OR_LOAD = Pattern.compile(
        "checkPermissionUseCase\\.(?:check|checkOrThrow|loadSubject)\\s*\\("
    );

    @Test
    @DisplayName("Project의 전환 후 승인된 role consumer와 subject/check seam inventory를 정확히 고정한다")
    void capturesDirectAuthorizationInventory() {
        Map<String, String> sources = readProjectSources();

        Set<String> directRoleConsumers = sources.entrySet().stream()
            .filter(entry -> DIRECT_ROLE_USE_CASE.matcher(entry.getValue()).find())
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<String, Long> subjectRoleReads = occurrenceDistribution(sources, SUBJECT_ROLE_READ);
        Map<String, Long> rolePolicyFactLoads = occurrenceDistribution(sources, ROLE_POLICY_FACT_LOAD);
        Map<String, Long> challengerPolicyFactLoads = occurrenceDistribution(
            sources, CHALLENGER_POLICY_FACT_LOAD);
        Map<String, Long> directChecksAndLoads = occurrenceDistribution(sources, DIRECT_CHECK_OR_LOAD);

        assertThat(directRoleConsumers).containsExactlyInAnyOrderElementsOf(expectedDirectRoleConsumers());
        assertThat(directRoleConsumers).hasSize(9);
        assertThat(subjectRoleReads).containsExactlyInAnyOrderEntriesOf(expectedSubjectRoleReads());
        assertThat(subjectRoleReads).isEmpty();
        assertThat(rolePolicyFactLoads).containsExactlyInAnyOrderEntriesOf(Map.of(
            "application/authorization/ProjectPolicySubjectSnapshotLoader.java", 4L
        ));
        assertThat(challengerPolicyFactLoads).containsExactlyInAnyOrderEntriesOf(Map.of(
            "application/authorization/ProjectPolicySubjectSnapshotLoader.java", 1L
        ));
        assertThat(directChecksAndLoads).containsExactlyInAnyOrderEntriesOf(expectedDirectChecksAndLoads());
        assertThat(directChecksAndLoads.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(8);

        System.out.println("approved-role-consumer[9]=" + directRoleConsumers.stream().sorted().toList());
        System.out.println("legacy-subject-role-read[0]=" + subjectRoleReads);
        System.out.println("approved-role-and-system-policy-fact-load[4]=" + rolePolicyFactLoads);
        System.out.println("approved-challenger-policy-fact-load[1]=" + challengerPolicyFactLoads);
        System.out.println("subject-check-load-seam[8]=" + directChecksAndLoads);
    }

    private Map<String, String> readProjectSources() {
        Map<String, String> sources = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.walk(PROJECT_SOURCE)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                .sorted()
                .forEach(path -> sources.put(relative(path), read(path)));
        } catch (IOException exception) {
            throw new UncheckedIOException("Project source inventory를 읽을 수 없습니다.", exception);
        }
        return Map.copyOf(sources);
    }

    private Map<String, Long> occurrenceDistribution(Map<String, String> sources, Pattern pattern) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        sources.forEach((path, source) -> {
            long count = pattern.matcher(source).results().count();
            if (count > 0) {
                distribution.put(path, count);
            }
        });
        return Map.copyOf(distribution);
    }

    private Set<String> expectedDirectRoleConsumers() {
        return Set.of(
            "adapter/in/web/assembler/ProjectApplicationResponseAssembler.java",
            "application/authorization/ProjectPolicySubjectSnapshotLoader.java",
            "application/service/command/ProjectApplicationCommandService.java",
            "application/service/command/ProjectCommandService.java",
            "application/service/command/ProjectMatchingRoundFinalizationCommandService.java",
            "application/service/query/ProjectApplicationQueryService.java",
            "application/service/query/ProjectMemberQueryService.java",
            "application/service/query/ProjectPermissionQueryService.java",
            "application/service/query/ProjectStatisticsQueryService.java"
        );
    }

    private Map<String, Long> expectedSubjectRoleReads() {
        return Map.of();
    }

    private Map<String, Long> expectedDirectChecksAndLoads() {
        return Map.of(
            "adapter/in/graphql/ProjectGraphQlController.java", 5L,
            "adapter/in/web/assembler/ProjectResponseAssembler.java", 2L,
            "application/service/query/ProjectPermissionQueryService.java", 1L
        );
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
