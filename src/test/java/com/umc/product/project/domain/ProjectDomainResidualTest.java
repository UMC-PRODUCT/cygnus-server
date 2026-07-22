package com.umc.product.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

@DisplayName("Project 도메인 잔여 경계")
class ProjectDomainResidualTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant DEADLINE = Instant.parse("2026-01-03T00:00:00Z");

    @Test
    @DisplayName("매칭 기간 중첩은 null·다른 지부·비중첩을 허용하고 같은 지부 경계 접촉은 거부한다")
    void matching_round_overlap_boundaries() {
        ProjectMatchingRound base = round(1L, START, END, DEADLINE);

        assertThatCode(() -> ProjectMatchingRound.validateNoOverlap(null, base)).doesNotThrowAnyException();
        assertThatCode(() -> ProjectMatchingRound.validateNoOverlap(base, null)).doesNotThrowAnyException();
        assertThatCode(() -> ProjectMatchingRound.validateNoOverlap(
            round(2L, START, END, DEADLINE), base)).doesNotThrowAnyException();
        assertThatCode(() -> ProjectMatchingRound.validateNoOverlap(
            round(1L, DEADLINE.plusSeconds(1), DEADLINE.plusSeconds(2), DEADLINE.plusSeconds(3)), base))
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> ProjectMatchingRound.validateNoOverlap(
            round(1L, DEADLINE, DEADLINE.plusSeconds(1), DEADLINE.plusSeconds(2)), base))
            .isInstanceOfSatisfying(ProjectDomainException.class, exception ->
                assertThat(exception.getBaseCode())
                    .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_PERIOD_OVERLAPPED));
    }

    @Test
    @DisplayName("매칭 일정 재설정은 검증된 세 시각을 모두 교체한다")
    void reschedules_matching_round() {
        ProjectMatchingRound value = round(1L, START, END, DEADLINE);
        Instant nextStart = START.plusSeconds(10);
        Instant nextEnd = END.plusSeconds(10);
        Instant nextDeadline = DEADLINE.plusSeconds(10);

        value.reschedule(nextStart, nextEnd, nextDeadline);

        assertThat(value.getStartsAt()).isEqualTo(nextStart);
        assertThat(value.getEndsAt()).isEqualTo(nextEnd);
        assertThat(value.getDecisionDeadline()).isEqualTo(nextDeadline);
    }

    @Test
    @DisplayName("지원서는 DRAFT에서 한 번만 제출되고 제출 검증 message를 보존한다")
    void application_submission_and_validation_boundaries() {
        ProjectApplication application = ProjectApplication.create(
            applicationForm(project(1L)), 20L, 30L, round(1L, START, END, DEADLINE));

        assertThat(application.isSubmitted()).isFalse();
        assertThat(application.isCancelled()).isFalse();
        assertThatThrownBy(() -> application.validateIsSubmitted("아직 제출 전"))
            .isInstanceOf(ProjectDomainException.class)
            .hasMessageContaining("아직 제출 전");

        application.submit();

        assertThat(application.isSubmitted()).isTrue();
        assertThatCode(() -> application.validateIsSubmitted("제출 완료")).doesNotThrowAnyException();
        assertThatThrownBy(application::submit)
            .isInstanceOfSatisfying(ProjectDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(ProjectErrorCode.APPLICATION_NOT_SUBMITTED));
    }

    @Test
    @DisplayName("지원 폼 소속 비교는 프로젝트 ID 값 기준으로 판단한다")
    void application_form_belongs_to_project_by_id() {
        Project owner = project(1L);
        Project sameId = project(1L);
        Project other = project(2L);
        ProjectApplicationForm form = applicationForm(owner);

        assertThat(form.belongsTo(sameId)).isTrue();
        assertThat(form.belongsTo(other)).isFalse();
    }

    private ProjectMatchingRound round(Long chapterId, Instant start, Instant end, Instant deadline) {
        return ProjectMatchingRound.create(
            "매칭", null, MatchingType.PLAN_DESIGN, MatchingPhase.FIRST,
            chapterId, start, end, deadline
        );
    }

    private Project project(Long id) {
        Project project = Project.createDraft(1L, 1L, 10L, 20L, 10L);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private ProjectApplicationForm applicationForm(Project project) {
        return ProjectApplicationForm.create(project, 10L);
    }
}
