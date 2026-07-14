package com.umc.product.project.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.umc.product.project.domain.enums.MatchingPhase;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

class ProjectMatchingRoundTest {

    private static final Instant STARTS_AT = Instant.parse("2026-05-10T00:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-05-15T00:00:00Z");
    private static final Instant DECISION_DEADLINE = Instant.parse("2026-05-17T00:00:00Z");

    ProjectMatchingRound round;

    @BeforeEach
    void setUp() {
        round = ProjectMatchingRound.create(
            "기획-디자인 1차 매칭", null,
            MatchingType.PLAN_DESIGN, MatchingPhase.FIRST, 1L, 1L,
            STARTS_AT, ENDS_AT, DECISION_DEADLINE
        );
    }

    @Nested
    class validateIsMutableAt {

        @Test
        void endsAt_직후면_정상_통과() {
            assertThatCode(() -> round.validateIsMutableAt(ENDS_AT.plusSeconds(1)))
                .doesNotThrowAnyException();
        }

        @Test
        void 결정_마감_전이면_정상_통과() {
            Instant midPoint = Instant.parse("2026-05-16T00:00:00Z");

            assertThatCode(() -> round.validateIsMutableAt(midPoint))
                .doesNotThrowAnyException();
        }

        @Test
        void decisionDeadline_정각이면_정상_통과() {
            assertThatCode(() -> round.validateIsMutableAt(DECISION_DEADLINE))
                .doesNotThrowAnyException();
        }

        @Test
        void 지원_진행_중이면_PROJECT_MATCHING_ROUND_NOT_ENDED() {
            Instant duringApplication = Instant.parse("2026-05-12T12:00:00Z");

            assertThatThrownBy(() -> round.validateIsMutableAt(duringApplication))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_NOT_ENDED);
        }

        @Test
        void endsAt_정각이면_아직_지원_중이라_PROJECT_MATCHING_ROUND_NOT_ENDED() {
            assertThatThrownBy(() -> round.validateIsMutableAt(ENDS_AT))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_NOT_ENDED);
        }

        @Test
        void decisionDeadline_경과시_PROJECT_MATCHING_ROUND_LOCKED() {
            Instant afterDeadline = DECISION_DEADLINE.plusSeconds(1);

            assertThatThrownBy(() -> round.validateIsMutableAt(afterDeadline))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_LOCKED);
        }
    }

    @Nested
    class validateIsViewableAt {

        @Test
        void endsAt_직후면_정상_통과() {
            assertThatCode(() -> round.validateIsViewableAt(ENDS_AT.plusSeconds(1)))
                .doesNotThrowAnyException();
        }

        @Test
        void 결정_마감_경과_후에도_정상_통과() {
            assertThatCode(() -> round.validateIsViewableAt(DECISION_DEADLINE.plusSeconds(1)))
                .doesNotThrowAnyException();
        }

        @Test
        void 지원_진행_중이면_PROJECT_MATCHING_ROUND_APPLICANTS_NOT_VIEWABLE() {
            Instant duringApplication = Instant.parse("2026-05-12T12:00:00Z");

            assertThatThrownBy(() -> round.validateIsViewableAt(duringApplication))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_APPLICANTS_NOT_VIEWABLE);
        }

        @Test
        void endsAt_정각이면_아직_지원_중이라_PROJECT_MATCHING_ROUND_APPLICANTS_NOT_VIEWABLE() {
            assertThatThrownBy(() -> round.validateIsViewableAt(ENDS_AT))
                .isInstanceOf(ProjectDomainException.class)
                .extracting("baseCode")
                .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_APPLICANTS_NOT_VIEWABLE);
        }
    }
}
