package com.umc.product.recruiting.adapter.in.graphql.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;

@DisplayName("Recruiting GraphQL DTO edge case")
class RecruitingGraphQlDtoEdgeCaseTest {

    @Test
    @DisplayName("nullable collection 입력은 빈 collection command·query로 변환한다")
    void convertNullableCollectionsToEmptyCollections() {
        assertThat(new CreateRecruitingSeasonGraphQlRequest(1L, 2L, null)
            .toCommand(3L).quotas()).isEmpty();
        assertThat(new UpdateRecruitingApplicationDraftGraphQlRequest(
            "지원자", "applicant@example.com", ChallengerTrack.PLAN, null, null
        ).toCommand(1L, 2L).answers()).isEmpty();
        assertThat(new UpdateAnonymousRecruitingApplicationGraphQlRequest(
            "applicant@example.com",
            "A1B2C3",
            "지원자",
            "applicant@example.com",
            ChallengerTrack.PLAN,
            null,
            null
        ).toCommand().answers()).isEmpty();
        assertThat(RecruitingStatusSummaryGraphQlResponse.from(
            new RecruitingStatusSummaryInfo(0L, null, List.of())
        ).countByStatus()).isEmpty();
    }

    @Test
    @DisplayName("필수 ID와 양수 collection·page 경계를 fail-fast로 검증한다")
    void rejectInvalidIdentifiersAndPageBoundaries() {
        assertThatThrownBy(() -> new RecruitingStatusSummaryGraphQlRequest(null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecruitingStatusSummaryGraphQlRequest(1L, List.of(0L), null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecruitingSeasonSearchGraphQlRequest(0L, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecruitingApplicationSearchGraphQlRequest(null, null, -1, 20)
            .toQuery(1L, 2L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecruitingApplicationFormSearchGraphQlRequest(null, 1L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecruitingApplicationFormSearchGraphQlRequest(1L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
