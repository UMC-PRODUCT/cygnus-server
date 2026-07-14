package com.umc.product.project.application.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.authorization.domain.policy.PolicyResolvedOutcome;
import com.umc.product.authorization.domain.policy.PolicyValue;
import com.umc.product.project.application.access.ProjectAccessScope.Clauses;
import com.umc.product.project.application.access.ProjectAccessScope.None;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyOutcomes;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;

@ExtendWith(MockitoExtension.class)
class ProjectAccessScopeResolverTest {

    private static final long MEMBER_ID = 10L;
    private static final long GISU_ID = 1L;
    private static final long CHAPTER_ID = 5L;
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-14T00:00:00Z");
    private static final Set<ProjectStatus> PUBLIC_STATUSES = Set.of(
        ProjectStatus.IN_PROGRESS,
        ProjectStatus.COMPLETED
    );

    @Mock
    ProjectPolicyAuthorizationService policyAuthorizationService;
    @Mock
    LoadProjectPort loadProjectPort;

    @InjectMocks
    ProjectAccessScopeResolver sut;

    @Test
    @DisplayName("일반 사용자의 공개 목록은 공개 상태만 하나의 기수 clause로 제한한다")
    void publicSearch_일반_사용자는_공개_상태만_조회한다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_PUBLIC), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(booleanOutcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY, true)));

        ProjectAccessScope result = sut.resolveForPublicSearch(
            MEMBER_ID, GISU_ID, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(result).isEqualTo(new Clauses(List.of(
            ScopeClause.gisu(Set.of(GISU_ID), PUBLIC_STATUSES)
        )));
    }

    @Test
    @DisplayName("활성 SUPER_ADMIN의 공개 목록은 공개 clause와 요청 상태 전체 clause를 합성한다")
    void publicSearch_SUPER_ADMIN은_요청_상태_전체를_조회한다() {
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_PUBLIC), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                booleanOutcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY, true),
                booleanOutcome(ProjectPolicyOutcomes.PROJECT_ALL, true)
            ));

        ProjectAccessScope result = sut.resolveForPublicSearch(MEMBER_ID, GISU_ID, requested);

        assertThat(result).isEqualTo(new Clauses(List.of(
            ScopeClause.gisu(Set.of(GISU_ID), PUBLIC_STATUSES),
            ScopeClause.gisu(Set.of(GISU_ID), requested)
        )));
    }

    @Test
    @DisplayName("활성 중앙 운영진의 공개 목록은 관리 기수만 별도 clause로 추가한다")
    void publicSearch_중앙_운영진은_관리_기수만_조회한다() {
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_PUBLIC), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                booleanOutcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY, true),
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_GISU_IDS, Set.of(GISU_ID))
            ));

        ProjectAccessScope result = sut.resolveForPublicSearch(MEMBER_ID, GISU_ID, requested);

        assertThat(result).isEqualTo(new Clauses(List.of(
            ScopeClause.gisu(Set.of(GISU_ID), PUBLIC_STATUSES),
            ScopeClause.gisu(Set.of(GISU_ID), requested)
        )));
    }

    @Test
    @DisplayName("활성 지부장의 공개 목록은 본인 지부만 별도 clause로 추가한다")
    void publicSearch_지부장은_본인_지부만_조회한다() {
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.ABORTED);
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_PUBLIC), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                booleanOutcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY, true),
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS, Set.of(CHAPTER_ID))
            ));

        ProjectAccessScope result = sut.resolveForPublicSearch(MEMBER_ID, GISU_ID, requested);

        assertThat(result).isEqualTo(new Clauses(List.of(
            ScopeClause.gisu(Set.of(GISU_ID), PUBLIC_STATUSES),
            ScopeClause.gisu(Set.of(GISU_ID), requested).andChapterIds(Set.of(CHAPTER_ID))
        )));
    }

    @Test
    @DisplayName("학교 운영진의 관리 목록은 정책이 산출한 소속 지부 clause만 사용한다")
    void management_학교_운영진은_소속_지부만_조회한다() {
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_MANAGED), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS, Set.of(CHAPTER_ID))
            ));

        ProjectAccessScope result = sut.resolveForManagement(MEMBER_ID, GISU_ID, requested);

        assertThat(result).isEqualTo(new Clauses(List.of(
            ScopeClause.gisu(Set.of(GISU_ID), requested).andChapterIds(Set.of(CHAPTER_ID))
        )));
    }

    @Test
    @DisplayName("활성 SUPER_ADMIN의 관리 목록은 요청 기수 전체 clause로 변환한다")
    void management_SUPER_ADMIN은_요청_기수_전체를_조회한다() {
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_MANAGED), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(booleanOutcome(ProjectPolicyOutcomes.PROJECT_ALL, true)));

        ProjectAccessScope result = sut.resolveForManagement(MEMBER_ID, GISU_ID, requested);

        assertThat(result).isEqualTo(new Clauses(List.of(
            ScopeClause.gisu(Set.of(GISU_ID), requested)
        )));
    }

    @Test
    @DisplayName("활성 중앙 운영진의 관리 목록은 정책이 산출한 관리 기수 clause로 변환한다")
    void management_중앙_운영진은_관리_기수만_조회한다() {
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_MANAGED), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_GISU_IDS, Set.of(GISU_ID))
            ));

        ProjectAccessScope result = sut.resolveForManagement(MEMBER_ID, GISU_ID, requested);

        assertThat(result).isEqualTo(new Clauses(List.of(
            ScopeClause.gisu(Set.of(GISU_ID), requested)
        )));
    }

    @Test
    @DisplayName("활성 지부장의 관리 목록은 정책이 산출한 본인 지부 clause로 변환한다")
    void management_지부장은_본인_지부만_조회한다() {
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_MANAGED), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS, Set.of(CHAPTER_ID))
            ));

        ProjectAccessScope result = sut.resolveForManagement(MEMBER_ID, GISU_ID, requested);

        assertThat(result).isEqualTo(new Clauses(List.of(
            ScopeClause.gisu(Set.of(GISU_ID), requested).andChapterIds(Set.of(CHAPTER_ID))
        )));
    }

    @Test
    @DisplayName("관리 지부 범위와 본인 PO DRAFT는 서로 다른 OR clause로 유지한다")
    void management_지부_scope와_PO_DRAFT를_분리한다() {
        Set<ProjectStatus> requested = Set.of(ProjectStatus.PENDING_REVIEW, ProjectStatus.IN_PROGRESS);
        Set<ProjectStatus> ownerStatuses = Set.of(
            ProjectStatus.DRAFT,
            ProjectStatus.PENDING_REVIEW,
            ProjectStatus.IN_PROGRESS
        );
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(loadProjectPort.existsByOwnerAndGisu(MEMBER_ID, GISU_ID)).willReturn(true);
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_MANAGED), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS, Set.of(CHAPTER_ID)),
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS, Set.of(MEMBER_ID)),
                booleanOutcome(ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS, true)
            ));

        ProjectAccessScope result = sut.resolveForManagement(MEMBER_ID, GISU_ID, requested);

        assertThat(result).isEqualTo(new Clauses(List.of(
            ScopeClause.gisu(Set.of(GISU_ID), requested).andChapterIds(Set.of(CHAPTER_ID)),
            ScopeClause.gisu(Set.of(GISU_ID), ownerStatuses).andOwnerMemberIds(Set.of(MEMBER_ID))
        )));
    }

    @Test
    @DisplayName("공개 목록은 소유 프로젝트 조회나 DRAFT owner clause를 자동 합성하지 않는다")
    void publicSearch_본인_PO_DRAFT를_자동_포함하지_않는다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_PUBLIC), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(booleanOutcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY, true)));

        ProjectAccessScope result = sut.resolveForPublicSearch(
            MEMBER_ID, GISU_ID, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(((Clauses) result).values())
            .allSatisfy(clause -> {
                assertThat(clause.statuses()).doesNotContain(ProjectStatus.DRAFT);
                assertThat(clause.ownerMemberIds()).isEmpty();
            });
        verifyNoInteractions(loadProjectPort);
    }

    @Test
    @DisplayName("본인 DRAFT 정책 outcome은 요청자를 소유자로 제한한 DRAFT scope로 변환한다")
    void ownDraft_본인_owner_outcome을_DRAFT_scope로_변환한다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS),
            any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS, Set.of(MEMBER_ID)),
                booleanOutcome(ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS, true)
            ));

        ProjectAccessScope result = sut.resolveForOwnDraft(MEMBER_ID, GISU_ID);

        assertThat(result).isEqualTo(
            new ProjectAccessScope.OwnerOnly(MEMBER_ID, Set.of(ProjectStatus.DRAFT))
        );
    }

    @Test
    @DisplayName("본인 DRAFT 정책이 허용되어도 owner outcome이 다른 회원이면 scope가 없다")
    void ownDraft_owner_outcome이_요청자와_다르면_None이다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS),
            any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS, Set.of(999L)),
                booleanOutcome(ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS, true)
            ));

        assertThat(sut.resolveForOwnDraft(MEMBER_ID, GISU_ID)).isEqualTo(new None());
    }

    @Test
    @DisplayName("관리 목록 정책이 DENY면 조회 범위는 NONE이다")
    void management_정책_DENY는_None이다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_MANAGED), any(ProjectPolicyResourceContext.class)))
            .willReturn(deny());

        ProjectAccessScope result = sut.resolveForManagement(
            MEMBER_ID, GISU_ID, Set.of(ProjectStatus.IN_PROGRESS));

        assertThat(result).isEqualTo(new None());
    }

    @Test
    @DisplayName("일반 사용자가 비공개 상태를 요청하면 접근을 거부한다")
    void publicSearch_비특권_사용자의_비공개_상태_요청을_거부한다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_PUBLIC), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(booleanOutcome(ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY, true)));

        assertThatThrownBy(() -> sut.resolveForPublicSearch(
            MEMBER_ID, GISU_ID, Set.of(ProjectStatus.PENDING_REVIEW)))
            .isInstanceOf(ProjectDomainException.class);
    }

    @Test
    @DisplayName("DRAFT 공개 요청은 subject snapshot을 만들기 전에 거부한다")
    void publicSearch_DRAFT_요청은_snapshot_전에_거부한다() {
        assertThatThrownBy(() -> sut.resolveForPublicSearch(
            MEMBER_ID, GISU_ID, Set.of(ProjectStatus.DRAFT, ProjectStatus.IN_PROGRESS)))
            .isInstanceOf(ProjectDomainException.class);

        verify(policyAuthorizationService, never()).snapshot(MEMBER_ID);
        verify(policyAuthorizationService, never()).evaluate(
            any(ProjectPolicySubjectSnapshot.class),
            any(ProjectPolicyAction.class),
            any(ProjectPolicyResourceContext.class)
        );
    }

    @Test
    @DisplayName("한 scope 판정은 subject snapshot을 한 번만 만들고 같은 snapshot으로 평가한다")
    void management_동일한_snapshot으로_한_번만_평가한다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(loadProjectPort.existsByOwnerAndGisu(MEMBER_ID, GISU_ID)).willReturn(true);
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);
        given(policyAuthorizationService.evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_MANAGED), any(ProjectPolicyResourceContext.class)))
            .willReturn(allow(
                longSetOutcome(ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS, Set.of(MEMBER_ID)),
                booleanOutcome(ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS, true)
            ));

        sut.resolveForManagement(MEMBER_ID, GISU_ID, Set.of(ProjectStatus.IN_PROGRESS));

        ArgumentCaptor<ProjectPolicyResourceContext> contextCaptor =
            ArgumentCaptor.forClass(ProjectPolicyResourceContext.class);
        verify(policyAuthorizationService).snapshot(MEMBER_ID);
        verify(policyAuthorizationService).evaluate(
            same(snapshot), eq(ProjectPolicyAction.PROJECT_LIST_MANAGED), contextCaptor.capture());
        assertThat(contextCaptor.getValue().gisuId()).contains(GISU_ID);
        assertThat(contextCaptor.getValue().requesterHasOwnedProjectInResourceGisu()).isTrue();
    }

    private static ProjectPolicySubjectSnapshot snapshot() {
        return new ProjectPolicySubjectSnapshot(
            new ProjectPolicyPrincipal.Member(MEMBER_ID),
            EVALUATED_AT,
            List.of(),
            List.of(),
            Map.of()
        );
    }

    private static PolicyDecision allow(PolicyResolvedOutcome... outcomes) {
        return decision(PolicyEffect.ALLOW, List.of(outcomes));
    }

    private static PolicyDecision deny() {
        return decision(PolicyEffect.DENY, List.of());
    }

    private static PolicyDecision decision(PolicyEffect effect, List<PolicyResolvedOutcome> outcomes) {
        return new PolicyDecision(
            effect,
            effect == PolicyEffect.ALLOW ? List.of("test.allow") : List.of(),
            effect == PolicyEffect.DENY ? List.of("test.deny") : List.of(),
            outcomes,
            EVALUATED_AT,
            "1.0",
            "project-1.0",
            "1.0.0",
            "test-fingerprint"
        );
    }

    private static PolicyResolvedOutcome booleanOutcome(String key, boolean value) {
        return new PolicyResolvedOutcome(key, new PolicyValue.BooleanValue(value));
    }

    private static PolicyResolvedOutcome longSetOutcome(String key, Set<Long> values) {
        return new PolicyResolvedOutcome(key, new PolicyValue.LongSetValue(values));
    }
}
