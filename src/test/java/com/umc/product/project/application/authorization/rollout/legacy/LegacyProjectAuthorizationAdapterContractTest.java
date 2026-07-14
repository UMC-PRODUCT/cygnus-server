package com.umc.product.project.application.authorization.rollout.legacy;

import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.CHAPTER_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.GISU_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.SCHOOL_ID;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.challenger;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.internal;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.member;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.richResource;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.role;
import static com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationFixture.superAdminMember;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySchoolChapterKey;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationComparisonRequest;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationDecision;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailure;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationEvaluationFailureCode;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;

class LegacyProjectAuthorizationAdapterContractTest {

    private static final Path LEGACY_SOURCE = Path.of(
        "src/main/java/com/umc/product/project/application/authorization/rollout/legacy");

    private final LegacyProjectAuthorizationAdapter adapter = new LegacyProjectAuthorizationAdapter();

    @Test
    @DisplayName("39개 Project action은 legacy adapter의 exhaustive switch에서 typed decision으로 평가된다")
    void evaluatesEveryProjectAction() {
        // Given
        var subject = superAdminMember(
            List.of(
                role(ChallengerRoleType.CHAPTER_PRESIDENT, CHAPTER_ID, GISU_ID),
                role(ChallengerRoleType.SCHOOL_PRESIDENT, SCHOOL_ID, GISU_ID)
            ),
            List.of(challenger(GISU_ID, ChallengerPart.PLAN)),
            Map.of(new ProjectPolicySchoolChapterKey(GISU_ID, SCHOOL_ID), CHAPTER_ID)
        );
        var resource = ProjectAuthorizationResourceSnapshot.withTargetMemberSchool(
            richResource(), SCHOOL_ID);

        // When
        Map<ProjectPolicyAction, ?> results = Arrays.stream(ProjectPolicyAction.values())
            .collect(java.util.stream.Collectors.toMap(
                action -> action,
                action -> adapter.evaluate(new ProjectAuthorizationComparisonRequest(
                    subject, action, resource, internal()))
            ));

        // Then
        assertThat(ProjectPolicyAction.values()).hasSize(39);
        assertThat(results.keySet()).containsExactlyInAnyOrder(ProjectPolicyAction.values());
        assertThat(results.values()).hasSize(39).allMatch(ProjectAuthorizationDecision.class::isInstance);
    }

    @Test
    @DisplayName("동일한 legacy request를 병렬 평가해도 결과가 결정적이다")
    void evaluatesConcurrentlyWithDeterministicResult() {
        // Given
        var subject = member(
            List.of(role(ChallengerRoleType.CHAPTER_PRESIDENT, 999L, GISU_ID)),
            List.of(),
            Map.of()
        );
        var request = new ProjectAuthorizationComparisonRequest(
            subject,
            ProjectPolicyAction.PROJECT_READ,
            ProjectPolicyResourceContext.builder()
                .project(100L, GISU_ID, CHAPTER_ID,
                    com.umc.product.project.domain.enums.ProjectStatus.PENDING_REVIEW)
                .productOwnerMemberId(999L)
                .build(),
            internal()
        );
        var expected = adapter.evaluate(request);

        // When
        List<?> results = IntStream.range(0, 128).parallel()
            .mapToObj(ignored -> adapter.evaluate(request))
            .toList();

        // Then
        assertThat(results).allMatch(expected::equals);
    }

    @Test
    @DisplayName("필수 resource fact가 없으면 예외 대신 typed legacy failure를 반환한다")
    void returnsTypedFailureForMissingResourceFact() {
        // Given
        var request = new ProjectAuthorizationComparisonRequest(
            member(List.of(), List.of(), Map.of()),
            ProjectPolicyAction.PROJECT_READ,
            ProjectPolicyResourceContext.builder().build(),
            internal()
        );

        // When
        Object result = adapter.evaluate(request);

        // Then
        assertThat(result).isEqualTo(new ProjectAuthorizationEvaluationFailure(
            ProjectAuthorizationEvaluationFailureCode.LEGACY_ADAPTER_FAILED,
            LegacyProjectAuthorizationFixture.EVALUATED_AT
        ));
    }

    @Test
    @DisplayName("legacy adapter는 DB, UseCase, mutator, target evaluator에 의존하지 않는다")
    void keepsLegacyAdapterPure() throws IOException {
        // Given
        List<String> forbidden = List.of(
            "application.port.in",
            "application.port.out",
            "Repository",
            "UseCase",
            "ProjectPolicyAuthorizationService",
            "PolicyEvaluationService",
            ".isActiveAt("
        );

        // When
        String source;
        try (var paths = Files.walk(LEGACY_SOURCE)) {
            source = paths.filter(path -> path.toString().endsWith(".java"))
                .sorted()
                .map(this::read)
                .collect(java.util.stream.Collectors.joining("\n"));
        }

        // Then
        assertThat(forbidden).allMatch(token -> !source.contains(token));
        assertThat(source).contains("catch (RuntimeException exception)");
        assertThat(source).doesNotContain("catch (Error", "catch (Throwable");
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
