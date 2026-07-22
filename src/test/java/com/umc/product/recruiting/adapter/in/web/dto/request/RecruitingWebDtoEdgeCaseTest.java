package com.umc.product.recruiting.adapter.in.web.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

@DisplayName("Recruiting Web DTO edge case")
class RecruitingWebDtoEdgeCaseTest {

    private static final Instant START = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-08-08T00:00:00Z");
    private static final Instant DOCUMENT_RESULT = Instant.parse("2026-08-10T00:00:00Z");
    private static final Instant FINAL_RESULT = Instant.parse("2026-08-16T00:00:00Z");

    @Test
    @DisplayName("nullable quota와 decision은 각각 빈 목록과 Bean Validation 위임 값으로 처리한다")
    void handleNullableQuotaAndDecision() {
        assertThat(new CreateRecruitingSeasonRequest(1L, 2L, null).toCommand(3L).quotas()).isEmpty();
        assertThat(new RecruitingDecisionRequest(null, null, null).isAcceptedTrackValid()).isTrue();
    }

    @Test
    @DisplayName("Round 생성·수정의 면접 설정은 활성·비활성 조합을 검증한다")
    void validateRoundInterviewConfigurations() {
        UpdateRecruitingRoundRequest update = new UpdateRecruitingRoundRequest(
            "본모집",
            List.of(ChallengerTrack.PLAN),
            false,
            START,
            END,
            DOCUMENT_RESULT,
            true,
            DOCUMENT_RESULT,
            FINAL_RESULT,
            FINAL_RESULT,
            100L,
            null,
            null
        );
        CreateRecruitingRoundRequest create = new CreateRecruitingRoundRequest(
            "본모집",
            RecruitingRoundType.REGULAR,
            1,
            List.of(ChallengerTrack.PLAN),
            false,
            START,
            END,
            DOCUMENT_RESULT,
            false,
            null,
            null,
            FINAL_RESULT,
            null,
            null,
            null
        );

        assertThat(update.isInterviewConfigurationValid()).isTrue();
        assertThat(create.isInterviewConfigurationValid()).isTrue();
        assertThat(update.toCommand(1L, 2L, 3L).configuration()).isNotNull();
        assertThat(create.toCommand(1L).configuration()).isNotNull();
    }

    @Test
    @DisplayName("면접 일정 기간 검증은 Bean Validation의 null 필드 검증과 충돌하지 않는다")
    void allowNullPeriodForSeparateNotNullValidation() {
        assertThat(new ConfirmRecruitingInterviewScheduleRequest(
            null, END, "온라인", "문의"
        ).isPeriodValid()).isTrue();
    }
}
