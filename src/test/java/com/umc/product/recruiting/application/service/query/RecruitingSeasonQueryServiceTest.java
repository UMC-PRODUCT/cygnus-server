package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;

@ExtendWith(MockitoExtension.class)
class RecruitingSeasonQueryServiceTest {

    @Mock
    LoadRecruitingSeasonPort loadSeasonPort;

    @Mock
    LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;

    @Mock
    LoadRecruitingRoundPort loadRoundPort;

    @InjectMocks
    RecruitingSeasonQueryService sut;

    @Test
    @DisplayName("시즌의 쿼터와 차수 설정을 조회한다")
    void getSeasonConfiguration() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 100L);
        RecruitingSeasonTrackQuota quota = RecruitingSeasonTrackQuota.create(
            season,
            ChallengerTrack.PLAN,
            3
        );
        RecruitingRound round = RecruitingRound.createRegular(season, RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.PLAN),
            true,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            "안내",
            "contact"
        ));
        ReflectionTestUtils.setField(round, "id", 200L);
        given(loadSeasonPort.getById(100L)).willReturn(season);
        given(loadQuotaPort.listBySeasonId(100L)).willReturn(List.of(quota));
        given(loadRoundPort.listBySeasonId(100L)).willReturn(List.of(round));

        RecruitingSeasonConfigurationInfo info = sut.getBySeasonId(100L);

        assertThat(info.quotas()).singleElement().satisfies(found -> {
            assertThat(found.track()).isEqualTo(ChallengerTrack.PLAN);
            assertThat(found.targetCount()).isEqualTo(3);
        });
        assertThat(info.rounds()).singleElement().satisfies(found -> {
            assertThat(found.recruitableTracks()).containsExactly(ChallengerTrack.PLAN);
            assertThat(found.secondChoiceEnabled()).isTrue();
            assertThat(found.availabilityFormId()).isNull();
        });
    }
}
