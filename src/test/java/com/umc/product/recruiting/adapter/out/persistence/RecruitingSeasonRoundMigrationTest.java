package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.PersistenceException;

@PersistenceAdapterTest
@Import(RecruitingSeasonPersistenceAdapter.class)
class RecruitingSeasonRoundMigrationTest {

    @Autowired
    TestEntityManager em;
    @Autowired
    RecruitingSeasonPersistenceAdapter seasonAdapter;

    @Test
    @DisplayName("데이터베이스는 음수 시즌 쿼터를 거부한다")
    void databaseRejectsNegativeQuota() {
        RecruitingSeason season = persistSeason(14L, 140L);

        assertThatThrownBy(() -> em.getEntityManager().createNativeQuery("""
            INSERT INTO recruiting_season_track_quota (
                created_at, updated_at, recruiting_season_id, track, target_count
            ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :seasonId, 'PLAN', -1)
            """)
            .setParameter("seasonId", season.getId())
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 음수 지부 전체 쿼터를 거부한다")
    void databaseRejectsNegativeChapterQuota() {
        assertThatThrownBy(() -> em.getEntityManager().createNativeQuery("""
            INSERT INTO recruiting_chapter_quota (
                created_at, updated_at, gisu_id, chapter_id, total_target_count
            ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, -1)
            """)
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 중복 모집 트랙 배열을 거부한다")
    void databaseRejectsDuplicateRecruitableTracks() {
        RecruitingSeason season = persistSeason(18L, 180L);

        assertThatThrownBy(() -> insertRoundWithSecondTrack(season, "PLAN"))
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 null 모집 트랙을 거부한다")
    void databaseRejectsNullRecruitableTrack() {
        RecruitingSeason season = persistSeason(24L, 240L);

        assertThatThrownBy(() -> insertRoundWithSecondTrack(season, null))
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 INFRA_PLUS 모집 트랙을 거부한다")
    void databaseRejectsInfraPlusRecruitableTrack() {
        RecruitingSeason season = persistSeason(25L, 250L);

        assertThatThrownBy(() -> insertRoundWithSecondTrack(season, "INFRA_PLUS"))
            .isInstanceOf(PersistenceException.class);
    }

    private RecruitingSeason persistSeason(Long gisuId, Long schoolId) {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(gisuId, schoolId));
        em.flush();
        return season;
    }

    private void insertRoundWithSecondTrack(RecruitingSeason season, String secondTrack) {
        em.getEntityManager().createNativeQuery("""
            INSERT INTO recruiting_round (
                created_at, updated_at, recruiting_season_id, type, round_no, status,
                recruitable_tracks, second_choice_enabled, document_start_at, document_end_at,
                document_result_published_at, interview_required, final_result_published_at
            ) VALUES (
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :seasonId, 'ADDITIONAL', 2, 'DRAFT',
                ARRAY['PLAN', CAST(:secondTrack AS TEXT)]::TEXT[], FALSE,
                TIMESTAMPTZ '2026-08-01T00:00:00Z', TIMESTAMPTZ '2026-08-08T00:00:00Z',
                TIMESTAMPTZ '2026-08-10T00:00:00Z', FALSE,
                TIMESTAMPTZ '2026-08-16T00:00:00Z'
            )
            """)
            .setParameter("seasonId", season.getId())
            .setParameter("secondTrack", secondTrack)
            .executeUpdate();
    }
}
