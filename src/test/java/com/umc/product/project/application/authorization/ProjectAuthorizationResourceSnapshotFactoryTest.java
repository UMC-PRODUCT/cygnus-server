package com.umc.product.project.application.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationResourceSnapshot;

@ExtendWith(MockitoExtension.class)
class ProjectAuthorizationResourceSnapshotFactoryTest {

    private static final Instant GISU_START_AT = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant GISU_END_AT = Instant.parse("2025-07-01T00:00:00Z");
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-14T00:00:00Z");

    @Mock
    private GetChapterUseCase getChapterUseCase;

    @InjectMocks
    private ProjectAuthorizationResourceSnapshotFactory factory;

    @Test
    @DisplayName("100개 리소스와 2개 기수도 지부 학교 조회를 한 번만 수행한다")
    void createAllQueriesChaptersOnceForOneHundredResourcesAcrossTwoGisus() {
        // Given
        ProjectPolicySubjectSnapshot subject = subject(List.of(
            schoolRole(ChallengerRoleType.SCHOOL_PRESIDENT, 101L, 999L),
            schoolRole(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, 102L, 998L)
        ));
        Map<Integer, ProjectPolicyResourceContext> contexts = IntStream.range(0, 100)
            .boxed()
            .collect(Collectors.toMap(
                index -> index,
                index -> chapterScope(index % 2 == 0 ? 10L : 20L, index % 2 == 0 ? 1L : 2L),
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Map<Integer, Optional<Long>> targetSchoolIds = contexts.keySet().stream()
            .collect(Collectors.toMap(key -> key, key -> Optional.empty()));
        given(getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(
            Set.of(10L, 20L), Set.of(101L, 102L)))
            .willReturn(Map.of(
                10L, Map.of(101L, new ChapterInfo(1L, "A"), 102L, new ChapterInfo(9L, "B")),
                20L, Map.of(101L, new ChapterInfo(8L, "C"), 102L, new ChapterInfo(2L, "D"))
            ));

        // When
        Map<Integer, ProjectAuthorizationResourceSnapshot> result =
            factory.createAll(subject, contexts, targetSchoolIds);

        // Then
        assertThat(result).hasSize(100);
        assertThat(result.get(0).targetChapterSchoolIds()).containsExactly(101L);
        assertThat(result.get(1).targetChapterSchoolIds()).containsExactly(102L);
        verify(getChapterUseCase, times(1))
            .getChapterMapByGisuIdsAndSchoolIds(Set.of(10L, 20L), Set.of(101L, 102L));
    }

    @Test
    @DisplayName("빈 입력이나 학교 회장단 및 완전한 리소스가 없으면 지부 학교를 조회하지 않는다")
    void createAllSkipsQueryWhenTrustedIntersectionInputsAreEmpty() {
        // Given
        ProjectPolicySubjectSnapshot schoolLeader = subject(List.of(
            schoolRole(ChallengerRoleType.SCHOOL_PRESIDENT, 101L, 10L)));
        Map<String, ProjectPolicyResourceContext> incompleteContexts = Map.of(
            "no-gisu", ProjectPolicyResourceContext.builder().creatorMemberId(1L).build(),
            "no-chapter", ProjectPolicyResourceContext.builder().gisuScope(10L).build()
        );

        // When
        Map<String, ProjectAuthorizationResourceSnapshot> emptyResult =
            factory.createAll(schoolLeader, Map.of(), Map.of());
        Map<String, ProjectAuthorizationResourceSnapshot> noRoleResult = factory.createAll(
            subject(List.of()),
            Map.of("resource", chapterScope(10L, 1L)),
            Map.of("resource", Optional.empty())
        );
        Map<String, ProjectAuthorizationResourceSnapshot> incompleteResult = factory.createAll(
            schoolLeader,
            incompleteContexts,
            Map.of("no-gisu", Optional.empty(), "no-chapter", Optional.empty())
        );

        // Then
        assertThat(emptyResult).isEmpty();
        assertThat(noRoleResult.get("resource").targetChapterSchoolIds()).isEmpty();
        assertThat(incompleteResult.values()).allSatisfy(snapshot ->
            assertThat(snapshot.targetChapterSchoolIds()).isEmpty());
        assertThatThrownBy(() -> emptyResult.put(
            "resource", ProjectAuthorizationResourceSnapshot.of(chapterScope(10L, 1L))))
            .isInstanceOf(UnsupportedOperationException.class);
        verifyNoInteractions(getChapterUseCase);
    }

    @Test
    @DisplayName("단건 생성은 target member school과 원본 policy context를 보존한다")
    void createPreservesTargetMemberSchoolId() {
        // Given
        ProjectPolicyResourceContext context = ProjectPolicyResourceContext.builder()
            .creatorMemberId(1L)
            .build();

        // When
        ProjectAuthorizationResourceSnapshot result =
            factory.create(subject(List.of()), context, Optional.of(301L));

        // Then
        assertThat(result.policyContext()).isSameAs(context);
        assertThat(result.targetMemberSchoolId()).contains(301L);
        verifyNoInteractions(getChapterUseCase);
    }

    @Test
    @DisplayName("배치 context와 target member school map의 키 집합은 정확히 같아야 한다")
    void createAllRejectsDifferentTargetMemberSchoolKeySet() {
        // Given
        Map<String, ProjectPolicyResourceContext> contexts = Map.of("resource", chapterScope(10L, 1L));

        // When & Then
        assertThatThrownBy(() -> factory.createAll(
            subject(List.of()), contexts, Map.of("other", Optional.empty())))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(getChapterUseCase);
    }

    private ProjectPolicySubjectSnapshot subject(List<ProjectPolicyRoleTuple> roles) {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(1L), EVALUATED_AT, roles, List.of(), Map.of());
    }

    private ProjectPolicyRoleTuple schoolRole(
        ChallengerRoleType roleType,
        long schoolId,
        long roleGisuId
    ) {
        return new ProjectPolicyRoleTuple(
            roleType,
            OrganizationType.SCHOOL,
            schoolId,
            null,
            roleGisuId,
            GISU_START_AT,
            GISU_END_AT
        );
    }

    private ProjectPolicyResourceContext chapterScope(long gisuId, long chapterId) {
        return ProjectPolicyResourceContext.builder().chapterScope(gisuId, chapterId).build();
    }
}
