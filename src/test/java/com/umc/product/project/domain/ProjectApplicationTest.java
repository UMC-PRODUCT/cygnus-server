package com.umc.product.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

class ProjectApplicationTest {

    private static final Long APPLICANT_MEMBER_ID = 100L;
    private static final Long DECIDER_MEMBER_ID = 200L;
    private static final Long FORM_RESPONSE_ID = 300L;

    /**
     * 합/불 토글이 허용되는 구간(지원 종료 후 ~ 결정 마감 전)을 가정한다.
     * 지원 시작/종료는 과거, 결정 마감은 미래로 설정한다.
     */
    private static final Instant NOW = Instant.now();
    private static final Instant ROUND_STARTS_AT = NOW.minusSeconds(172_800);
    private static final Instant ROUND_ENDS_AT = NOW.minusSeconds(43_200);
    private static final Instant ROUND_DECISION_DEADLINE = NOW.plusSeconds(86_400);

    ProjectApplication application;
    ProjectMatchingRound round;

    @BeforeEach
    void setUp() {
        round = openRound();
        application = ProjectApplication.create(applicationForm(), FORM_RESPONSE_ID, APPLICANT_MEMBER_ID, round);
        setStatus(application, ProjectApplicationStatus.SUBMITTED);
    }

    @Nested
    class approve {

        @Test
        void SUBMITTED를_APPROVED로_전이한다() {
            application.approve(DECIDER_MEMBER_ID, "역량 우수");

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
            assertThat(application.getStatusChangedMemberId()).isEqualTo(DECIDER_MEMBER_ID);
            assertThat(application.getStatusChangeReason()).isEqualTo("역량 우수");
        }

        @Test
        void REJECTED를_APPROVED로_재토글한다() {
            setStatus(application, ProjectApplicationStatus.REJECTED);

            application.approve(DECIDER_MEMBER_ID, null);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
        }

        @Test
        void APPROVED에서_같은_status로_호출해도_예외_없이_갱신된다() {
            setStatus(application, ProjectApplicationStatus.APPROVED);

            assertThatCode(() -> application.approve(DECIDER_MEMBER_ID, "재확인"))
                .doesNotThrowAnyException();
            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
        }

        @Test
        void DRAFT_상태에서는_PROJECT_APPLICATION_DECISION_INVALID_TRANSITION() {
            setStatus(application, ProjectApplicationStatus.DRAFT);

            assertThatThrownBy(() -> application.approve(DECIDER_MEMBER_ID, null))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_DECISION_INVALID_TRANSITION);
        }

        @Test
        void 결정_마감_후에는_PROJECT_MATCHING_ROUND_LOCKED() {
            setRoundDeadline(round, NOW.minusSeconds(60));

            assertThatThrownBy(() -> application.approve(DECIDER_MEMBER_ID, null))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_LOCKED);
        }

        @Test
        void 지원_진행_중에는_PROJECT_MATCHING_ROUND_NOT_ENDED() {
            setRoundEndsAt(round, NOW.plusSeconds(43_200));

            assertThatThrownBy(() -> application.approve(DECIDER_MEMBER_ID, null))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_NOT_ENDED);
        }
    }

    @Nested
    class reject {

        @Test
        void SUBMITTED를_REJECTED로_전이한다() {
            application.reject(DECIDER_MEMBER_ID, "면접 결과 부적합");

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.REJECTED);
            assertThat(application.getStatusChangedMemberId()).isEqualTo(DECIDER_MEMBER_ID);
            assertThat(application.getStatusChangeReason()).isEqualTo("면접 결과 부적합");
        }

        @Test
        void APPROVED를_REJECTED로_재토글한다() {
            setStatus(application, ProjectApplicationStatus.APPROVED);

            application.reject(DECIDER_MEMBER_ID, null);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.REJECTED);
        }

        @Test
        void DRAFT_상태에서는_PROJECT_APPLICATION_DECISION_INVALID_TRANSITION() {
            setStatus(application, ProjectApplicationStatus.DRAFT);

            assertThatThrownBy(() -> application.reject(DECIDER_MEMBER_ID, null))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_DECISION_INVALID_TRANSITION);
        }

        @Test
        void 결정_마감_후에는_PROJECT_MATCHING_ROUND_LOCKED() {
            setRoundDeadline(round, NOW.minusSeconds(60));

            assertThatThrownBy(() -> application.reject(DECIDER_MEMBER_ID, null))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_LOCKED);
        }

        @Test
        void 지원_진행_중에는_PROJECT_MATCHING_ROUND_NOT_ENDED() {
            setRoundEndsAt(round, NOW.plusSeconds(43_200));

            assertThatThrownBy(() -> application.reject(DECIDER_MEMBER_ID, null))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_NOT_ENDED);
        }
    }

    @Nested
    class forceDecision {

        @Test
        void forceApprove는_결정_마감_후에도_APPROVED로_전이한다() {
            setRoundDeadline(round, NOW.minusSeconds(60));

            application.forceApprove(DECIDER_MEMBER_ID, "슈퍼어드민 수정");

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
            assertThat(application.getStatusChangedMemberId()).isEqualTo(DECIDER_MEMBER_ID);
            assertThat(application.getStatusChangeReason()).isEqualTo("슈퍼어드민 수정");
        }

        @Test
        void forceReject는_지원_진행_중에도_REJECTED로_전이한다() {
            setRoundEndsAt(round, NOW.plusSeconds(43_200));

            application.forceReject(DECIDER_MEMBER_ID, "슈퍼어드민 수정");

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.REJECTED);
            assertThat(application.getStatusChangedMemberId()).isEqualTo(DECIDER_MEMBER_ID);
            assertThat(application.getStatusChangeReason()).isEqualTo("슈퍼어드민 수정");
        }

        @Test
        void forceApprove도_DRAFT_상태에서는_PROJECT_APPLICATION_DECISION_INVALID_TRANSITION() {
            setStatus(application, ProjectApplicationStatus.DRAFT);

            assertThatThrownBy(() -> application.forceApprove(DECIDER_MEMBER_ID, null))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_DECISION_INVALID_TRANSITION);
        }
    }

    @Nested
    class applyAutoDecision {

        @Test
        void SUBMITTED를_APPROVED로_확정한다() {
            application.applyAutoDecision(ProjectApplicationStatus.APPROVED, DECIDER_MEMBER_ID);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
            assertThat(application.getStatusChangedMemberId()).isEqualTo(DECIDER_MEMBER_ID);
            assertThat(application.getStatusChangeReason()).isEqualTo("auto-decide");
        }

        @Test
        void SUBMITTED를_REJECTED로_확정한다() {
            application.applyAutoDecision(ProjectApplicationStatus.REJECTED, DECIDER_MEMBER_ID);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.REJECTED);
        }

        @Test
        void executedByMemberId가_null이어도_정상_처리된다_스케줄러_호출_케이스() {
            application.applyAutoDecision(ProjectApplicationStatus.APPROVED, null);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
            assertThat(application.getStatusChangedMemberId()).isNull();
        }

        @Test
        void REJECTED를_APPROVED로_override한다_PM_결정_무시() {
            setStatus(application, ProjectApplicationStatus.REJECTED);

            application.applyAutoDecision(ProjectApplicationStatus.APPROVED, DECIDER_MEMBER_ID);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.APPROVED);
        }

        @Test
        void 결정_마감_후에도_차수_검증_없이_정상_적용된다() {
            setRoundDeadline(round, NOW.minusSeconds(60));

            assertThatCode(() -> application.applyAutoDecision(
                ProjectApplicationStatus.APPROVED, DECIDER_MEMBER_ID
            )).doesNotThrowAnyException();
        }

        @Test
        void DRAFT_상태에서는_PROJECT_APPLICATION_DECISION_INVALID_TRANSITION() {
            setStatus(application, ProjectApplicationStatus.DRAFT);

            assertThatThrownBy(() -> application.applyAutoDecision(
                ProjectApplicationStatus.APPROVED, DECIDER_MEMBER_ID
            ))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_DECISION_INVALID_TRANSITION);
        }

        @Test
        void targetStatus가_SUBMITTED면_PROJECT_APPLICATION_DECISION_INVALID_TRANSITION() {
            assertThatThrownBy(() -> application.applyAutoDecision(
                ProjectApplicationStatus.SUBMITTED, DECIDER_MEMBER_ID
            ))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_DECISION_INVALID_TRANSITION);
        }
    }

    @Nested
    class cancel {

        @Test
        void DRAFT를_CANCELLED로_전이한다() {
            setStatus(application, ProjectApplicationStatus.DRAFT);

            application.cancel(DECIDER_MEMBER_ID, "임시저장본 더 안 쓸래요");

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.CANCELLED);
            assertThat(application.isCancelled()).isTrue();
            assertThat(application.getStatusChangedMemberId()).isEqualTo(DECIDER_MEMBER_ID);
            assertThat(application.getStatusChangeReason()).isEqualTo("임시저장본 더 안 쓸래요");
        }

        @Test
        void SUBMITTED를_CANCELLED로_전이한다() {
            application.cancel(DECIDER_MEMBER_ID, "다른 프로젝트로 갈래요");

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.CANCELLED);
            assertThat(application.getStatusChangeReason()).isEqualTo("다른 프로젝트로 갈래요");
        }

        @Test
        void reason은_null이어도_허용된다() {
            setStatus(application, ProjectApplicationStatus.DRAFT);

            application.cancel(DECIDER_MEMBER_ID, null);

            assertThat(application.getStatus()).isEqualTo(ProjectApplicationStatus.CANCELLED);
            assertThat(application.getStatusChangeReason()).isNull();
        }

        @Test
        void APPROVED_상태에서는_CANCEL_NOT_ALLOWED() {
            setStatus(application, ProjectApplicationStatus.APPROVED);

            assertThatThrownBy(() -> application.cancel(DECIDER_MEMBER_ID, "사유"))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_CANCEL_NOT_ALLOWED);
        }

        @Test
        void REJECTED_상태에서는_CANCEL_NOT_ALLOWED() {
            setStatus(application, ProjectApplicationStatus.REJECTED);

            assertThatThrownBy(() -> application.cancel(DECIDER_MEMBER_ID, "사유"))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_CANCEL_NOT_ALLOWED);
        }

        @Test
        void 이미_CANCELLED_상태에서는_CANCEL_NOT_ALLOWED() {
            setStatus(application, ProjectApplicationStatus.CANCELLED);

            assertThatThrownBy(() -> application.cancel(DECIDER_MEMBER_ID, "사유"))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_APPLICATION_CANCEL_NOT_ALLOWED);
        }
    }

    private void setStatus(ProjectApplication application, ProjectApplicationStatus status) {
        ReflectionTestUtils.setField(application, "status", status);
    }

    private void setRoundDeadline(ProjectMatchingRound round, Instant deadline) {
        ReflectionTestUtils.setField(round, "decisionDeadline", deadline);
    }

    private void setRoundEndsAt(ProjectMatchingRound round, Instant endsAt) {
        ReflectionTestUtils.setField(round, "endsAt", endsAt);
    }

    private ProjectMatchingRound openRound() {
        return ProjectMatchingRound.create(
            "기획-디자인 1차 매칭", null,
            MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, 1L, 2L,
            ROUND_STARTS_AT, ROUND_ENDS_AT, ROUND_DECISION_DEADLINE
        );
    }

    private ProjectApplicationForm applicationForm() {
        Project project = Project.createDraft(1L, 2L, 999L, 7L, 999L);
        return ProjectApplicationForm.create(project, 500L);
    }
}
