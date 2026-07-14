package com.umc.product.project.application.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterScopeInfo;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyPrincipal;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.authorization.ProjectPolicySubjectSnapshot;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class ProjectStatisticsAccessPolicyTest {

    private static final long MEMBER_ID = 10L;
    private static final long OWNER_MEMBER_ID = 20L;
    private static final long PROJECT_ID = 100L;
    private static final long GISU_ID = 1L;
    private static final long OTHER_GISU_ID = 2L;
    private static final long CHAPTER_ID = 7L;
    private static final Instant EVALUATED_AT = Instant.parse("2026-07-14T00:00:00Z");

    @Mock
    ProjectPolicyAuthorizationService policyAuthorizationService;

    @InjectMocks
    ProjectStatisticsAccessPolicy sut;

    @Test
    @DisplayName("통계 정책용 subject snapshot 생성을 공용 정책 서비스에 위임한다")
    void 통계_정책용_subject_snapshot_생성을_공용_정책_서비스에_위임한다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        given(policyAuthorizationService.snapshot(MEMBER_ID)).willReturn(snapshot);

        ProjectPolicySubjectSnapshot result = sut.snapshot(MEMBER_ID);

        assertThat(result).isSameAs(snapshot);
        verify(policyAuthorizationService).snapshot(MEMBER_ID);
    }

    @Test
    @DisplayName("프로젝트 통계 정책이 허용하면 프로젝트 통계를 조회할 수 있다")
    void 프로젝트_통계_정책이_허용하면_프로젝트_통계를_조회할_수_있다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        Project project = project();
        ProjectPolicyResourceContext resource = projectResource(true);
        given(policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_PROJECT,
            resource
        )).willReturn(decision(PolicyEffect.ALLOW));

        boolean result = sut.canReadProjectStatistics(snapshot, project, true);

        assertThat(result).isTrue();
        verify(policyAuthorizationService).evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_PROJECT,
            resource
        );
    }

    @Test
    @DisplayName("프로젝트 통계 정책이 거부하면 프로젝트 통계를 조회할 수 없다")
    void 프로젝트_통계_정책이_거부하면_프로젝트_통계를_조회할_수_없다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        Project project = project();
        ProjectPolicyResourceContext resource = projectResource(false);
        given(policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_PROJECT,
            resource
        )).willReturn(decision(PolicyEffect.DENY));

        boolean result = sut.canReadProjectStatistics(snapshot, project, false);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("지부 통계 정책이 허용하면 지부 통계를 조회할 수 있다")
    void 지부_통계_정책이_허용하면_지부_통계를_조회할_수_있다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        ChapterScopeInfo chapter = new ChapterScopeInfo(CHAPTER_ID, GISU_ID);
        ProjectPolicyResourceContext resource = chapterResource(GISU_ID);
        given(policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_CHAPTER,
            resource
        )).willReturn(decision(PolicyEffect.ALLOW));

        boolean result = sut.canReadChapterStatistics(snapshot, chapter);

        assertThat(result).isTrue();
        verify(policyAuthorizationService).evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_CHAPTER,
            resource
        );
    }

    @Test
    @DisplayName("다른 기수의 운영진에 대한 target 거부 결정은 지부 통계 조회를 거부한다")
    void 다른_기수의_운영진에_대한_target_거부_결정은_지부_통계_조회를_거부한다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        ChapterScopeInfo chapter = new ChapterScopeInfo(CHAPTER_ID, OTHER_GISU_ID);
        ProjectPolicyResourceContext resource = chapterResource(OTHER_GISU_ID);
        given(policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_CHAPTER,
            resource
        )).willReturn(decision(PolicyEffect.DENY));

        boolean result = sut.canReadChapterStatistics(snapshot, chapter);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("공개 매칭 통계 정책이 허용하면 공개 매칭 통계를 조회할 수 있다")
    void 공개_매칭_통계_정책이_허용하면_공개_매칭_통계를_조회할_수_있다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        ChapterScopeInfo chapter = new ChapterScopeInfo(CHAPTER_ID, GISU_ID);
        ProjectPolicyResourceContext resource = chapterResource(GISU_ID);
        given(policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING,
            resource
        )).willReturn(decision(PolicyEffect.ALLOW));

        boolean result = sut.canReadPublicMatchingStatistics(snapshot, chapter);

        assertThat(result).isTrue();
        verify(policyAuthorizationService).evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING,
            resource
        );
    }

    @Test
    @DisplayName("공개 매칭 통계 정책이 거부하면 공개 매칭 통계를 조회할 수 없다")
    void 공개_매칭_통계_정책이_거부하면_공개_매칭_통계를_조회할_수_없다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        ChapterScopeInfo chapter = new ChapterScopeInfo(CHAPTER_ID, GISU_ID);
        ProjectPolicyResourceContext resource = chapterResource(GISU_ID);
        given(policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING,
            resource
        )).willReturn(decision(PolicyEffect.DENY));

        boolean result = sut.canReadPublicMatchingStatistics(snapshot, chapter);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("정책 평가 실패는 통계 정책에서 숨기지 않고 전파한다")
    void 정책_평가_실패는_통계_정책에서_숨기지_않고_전파한다() {
        ProjectPolicySubjectSnapshot snapshot = snapshot();
        Project project = project();
        ProjectPolicyResourceContext resource = projectResource(true);
        AuthorizationDomainException failure = new AuthorizationDomainException(
            AuthorizationErrorCode.POLICY_EVALUATION_FAILED
        );
        given(policyAuthorizationService.evaluate(
            snapshot,
            ProjectPolicyAction.STATISTICS_PROJECT,
            resource
        )).willThrow(failure);

        assertThatThrownBy(() -> sut.canReadProjectStatistics(snapshot, project, true))
            .isSameAs(failure);
    }

    private static Project project() {
        Project project = Project.createDraft(
            GISU_ID,
            CHAPTER_ID,
            OWNER_MEMBER_ID,
            3L,
            OWNER_MEMBER_ID
        );
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        return project;
    }

    private static ProjectPolicyResourceContext projectResource(boolean activePlanMember) {
        return ProjectPolicyResourceContext.builder()
            .project(PROJECT_ID, GISU_ID, CHAPTER_ID, ProjectStatus.DRAFT)
            .productOwnerMemberId(OWNER_MEMBER_ID)
            .activePlanMember(activePlanMember)
            .build();
    }

    private static ProjectPolicyResourceContext chapterResource(long gisuId) {
        return ProjectPolicyResourceContext.builder()
            .chapterScope(gisuId, CHAPTER_ID)
            .build();
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

    private static PolicyDecision decision(PolicyEffect effect) {
        return new PolicyDecision(
            effect,
            effect == PolicyEffect.ALLOW ? List.of("allow") : List.of(),
            effect == PolicyEffect.DENY ? List.of("deny") : List.of(),
            List.of(),
            EVALUATED_AT,
            "1.0",
            "project-1.0",
            "1.0.0",
            "fingerprint"
        );
    }
}
