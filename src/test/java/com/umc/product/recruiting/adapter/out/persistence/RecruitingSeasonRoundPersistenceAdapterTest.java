package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({
    RecruitingSeasonPersistenceAdapter.class,
    RecruitingSeasonTrackQuotaPersistenceAdapter.class,
    RecruitingRoundPersistenceAdapter.class
})
class RecruitingSeasonRoundPersistenceAdapterTest {

    @Autowired
    TestEntityManager em;
    @Autowired
    RecruitingSeasonPersistenceAdapter seasonAdapter;
    @Autowired
    RecruitingSeasonTrackQuotaPersistenceAdapter quotaAdapter;
    @Autowired
    RecruitingRoundPersistenceAdapter roundAdapter;

    @Test
    @DisplayName("시즌 쿼터와 면접 없는 차수 설정을 저장하고 조회한다")
    void saveAndLoadConfiguration() {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(11L, 110L));
        quotaAdapter.saveAll(List.of(
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.DESIGN, 2),
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.PLAN, 3)
        ));
        RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(
            season,
            noInterviewConfiguration()
        ));
        em.flush();
        em.clear();

        assertThat(quotaAdapter.listBySeasonId(season.getId()))
            .extracting(RecruitingSeasonTrackQuota::getTrack)
            .containsExactly(ChallengerTrack.PLAN, ChallengerTrack.DESIGN);
        RecruitingRound reloaded = roundAdapter.getById(round.getId());
        assertThat(reloaded.getRecruitableTracks())
            .containsExactly(ChallengerTrack.PLAN, ChallengerTrack.DESIGN);
        assertThat(reloaded.isInterviewRequired()).isFalse();
        assertThat(reloaded.getInterviewStartAt()).isNull();
        assertThat(reloaded.getAvailabilityFormId()).isNull();
        assertThat(reloaded.getAnnouncement()).isEqualTo("안내");
        assertThat(reloaded.getContactText()).isEqualTo("contact");
    }

    @Test
    @DisplayName("같은 시즌과 트랙의 쿼터는 중복 저장할 수 없다")
    void quotaTrackIsUniqueWithinSeason() {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(12L, 120L));

        assertThatThrownBy(() -> quotaAdapter.saveAll(List.of(
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.PLAN, 1),
            RecruitingSeasonTrackQuota.create(season, ChallengerTrack.PLAN, 2)
        )))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 트랙의 쿼터를 새 목표 인원으로 교체한다")
    void replaceQuotaForSameTrack() {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(13L, 130L));
        quotaAdapter.saveAll(List.of(RecruitingSeasonTrackQuota.create(
            season,
            ChallengerTrack.PLAN,
            1
        )));
        em.flush();

        quotaAdapter.deleteAllBySeasonId(season.getId());
        quotaAdapter.saveAll(List.of(RecruitingSeasonTrackQuota.create(
            season,
            ChallengerTrack.PLAN,
            5
        )));
        em.flush();
        em.clear();

        assertThat(quotaAdapter.listBySeasonId(season.getId()))
            .extracting(RecruitingSeasonTrackQuota::getTargetCount)
            .containsExactly(5);
    }

    @Test
    @DisplayName("여러 시즌의 차수를 한 번에 조회하면 시즌도 함께 로딩한다")
    void listRoundsBySeasonIds() {
        RecruitingSeason firstSeason = seasonAdapter.save(RecruitingSeason.create(14L, 140L));
        RecruitingSeason secondSeason = seasonAdapter.save(RecruitingSeason.create(14L, 150L));
        RecruitingRound firstRound = roundAdapter.save(RecruitingRound.createRegular(
            firstSeason,
            noInterviewConfiguration()
        ));
        RecruitingRound secondRound = roundAdapter.save(RecruitingRound.createRegular(
            secondSeason,
            noInterviewConfiguration()
        ));
        em.flush();
        em.clear();

        assertThat(roundAdapter.listBySeasonIds(List.of(firstSeason.getId(), secondSeason.getId())))
            .extracting(RecruitingRound::getId)
            .containsExactlyInAnyOrder(firstRound.getId(), secondRound.getId());
    }

    @Test
    @DisplayName("삭제된 차수는 목록, 단건, 제목 중복 검사, 추가모집 채번에서 제외된다")
    void deletedRoundIsExcludedFromQueries() {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(15L, 160L));
        RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(
            season,
            "본모집",
            noInterviewConfiguration()
        ));
        roundAdapter.save(RecruitingRound.createAdditional(season, 1, "추가모집 1차", noInterviewConfiguration()));
        em.flush();

        round.delete(Instant.parse("2026-08-16T00:00:00Z"));
        roundAdapter.save(round);
        em.flush();
        em.clear();

        assertThat(roundAdapter.findById(round.getId())).isEmpty();
        assertThat(roundAdapter.listBySeasonId(season.getId()))
            .extracting(RecruitingRound::getTitle)
            .containsExactly("추가모집 1차");
        assertThat(roundAdapter.listBySeasonIds(List.of(season.getId())))
            .hasSize(1);
        assertThat(roundAdapter.existsBySeasonIdAndTitleIgnoreCase(season.getId(), "본모집")).isFalse();
        assertThat(roundAdapter.existsBySeasonIdAndTypeAndRoundNo(
            season.getId(),
            RecruitingRoundType.REGULAR,
            1
        )).isFalse();
        assertThat(roundAdapter.getByIdForUpdateIncludingDeleted(round.getId()).isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제된 차수는 슬롯을 점유하지 않아 같은 유형과 번호로 다시 만들 수 있다")
    void deletedRoundReleasesUniqueSlot() {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(16L, 170L));
        RecruitingRound round = roundAdapter.save(RecruitingRound.createRegular(
            season,
            "본모집",
            noInterviewConfiguration()
        ));
        em.flush();

        round.delete(Instant.parse("2026-08-16T00:00:00Z"));
        roundAdapter.save(round);
        em.flush();

        RecruitingRound recreated = roundAdapter.save(RecruitingRound.createRegular(
            season,
            "새 본모집",
            noInterviewConfiguration()
        ));
        em.flush();
        em.clear();

        assertThat(recreated.getRoundNo()).isEqualTo(1);
        assertThat(roundAdapter.listBySeasonId(season.getId()))
            .extracting(RecruitingRound::getTitle)
            .containsExactly("새 본모집");
    }

    @Test
    @DisplayName("활성 차수끼리는 같은 유형과 번호를 가질 수 없다")
    void activeRoundsKeepUniqueSlot() {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(17L, 180L));
        roundAdapter.save(RecruitingRound.createRegular(season, "본모집", noInterviewConfiguration()));
        em.flush();

        assertThatThrownBy(() -> {
            roundAdapter.save(RecruitingRound.createRegular(season, "본모집 중복", noInterviewConfiguration()));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private RecruitingRoundConfiguration noInterviewConfiguration() {
        return RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.PLAN, ChallengerTrack.DESIGN),
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
        );
    }
}
