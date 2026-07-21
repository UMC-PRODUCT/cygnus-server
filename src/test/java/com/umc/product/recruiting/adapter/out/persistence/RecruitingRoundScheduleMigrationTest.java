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
class RecruitingRoundScheduleMigrationTest {

    private static final String DOCUMENT_START = "2026-08-01T00:00:00Z";
    private static final String DOCUMENT_END = "2026-08-08T00:00:00Z";
    private static final String DOCUMENT_RESULT = "2026-08-10T00:00:00Z";
    private static final String INTERVIEW_START = "2026-08-11T00:00:00Z";
    private static final String INTERVIEW_END = "2026-08-14T00:00:00Z";
    private static final String FINAL_RESULT = "2026-08-16T00:00:00Z";

    @Autowired
    TestEntityManager em;
    @Autowired
    RecruitingSeasonPersistenceAdapter seasonAdapter;

    @Test
    @DisplayName("데이터베이스는 순서가 역전된 모집 차수 일정을 거부한다")
    void databaseRejectsMalformedRoundSchedule() {
        RecruitingSeason season = persistSeason(15L, 150L);

        assertThatThrownBy(() -> insertRound(season, noInterviewSchedule(
            DOCUMENT_END, DOCUMENT_START, DOCUMENT_RESULT, FINAL_RESULT, null
        ))).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 null 필수 모집 일정을 거부한다")
    void databaseRejectsMissingRequiredRoundSchedule() {
        RecruitingSeason season = persistSeason(19L, 190L);

        assertThatThrownBy(() -> insertRound(season, noInterviewSchedule(
            null, DOCUMENT_END, DOCUMENT_RESULT, FINAL_RESULT, null
        ))).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 접수 종료보다 빠른 서류 결과 발표를 거부한다")
    void databaseRejectsDocumentResultBeforeDocumentEnd() {
        RecruitingSeason season = persistSeason(20L, 200L);

        assertThatThrownBy(() -> insertRound(season, noInterviewSchedule(
            DOCUMENT_START, DOCUMENT_END, DOCUMENT_START, FINAL_RESULT, null
        ))).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 서류 결과 발표보다 빠른 면접 시작을 거부한다")
    void databaseRejectsInterviewBeforeDocumentResult() {
        RecruitingSeason season = persistSeason(21L, 210L);

        assertThatThrownBy(() -> insertRound(season, interviewSchedule(
            DOCUMENT_START, DOCUMENT_END, DOCUMENT_RESULT, DOCUMENT_END, INTERVIEW_END, FINAL_RESULT
        ))).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 면접 종료보다 빠른 최종 결과 발표를 거부한다")
    void databaseRejectsFinalResultBeforeInterviewEnd() {
        RecruitingSeason season = persistSeason(22L, 220L);

        assertThatThrownBy(() -> insertRound(season, interviewSchedule(
            DOCUMENT_START, DOCUMENT_END, DOCUMENT_RESULT, INTERVIEW_START, INTERVIEW_END, INTERVIEW_START
        ))).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 면접이 없을 때 서류 결과보다 빠른 최종 발표를 거부한다")
    void databaseRejectsFinalResultBeforeDocumentResultWithoutInterview() {
        RecruitingSeason season = persistSeason(23L, 230L);

        assertThatThrownBy(() -> insertRound(season, noInterviewSchedule(
            DOCUMENT_START, DOCUMENT_END, DOCUMENT_RESULT, DOCUMENT_END, null
        ))).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 면접 없는 차수의 availability form을 거부한다")
    void databaseRejectsAvailabilityFormWithoutInterview() {
        RecruitingSeason season = persistSeason(16L, 160L);

        assertThatThrownBy(() -> insertRound(season, noInterviewSchedule(
            DOCUMENT_START, DOCUMENT_END, DOCUMENT_RESULT, FINAL_RESULT, 123L
        ))).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("데이터베이스는 필수 면접 기간이 없는 차수를 거부한다")
    void databaseRejectsMissingRequiredInterviewWindow() {
        RecruitingSeason season = persistSeason(17L, 170L);

        assertThatThrownBy(() -> insertRound(season, interviewSchedule(
            DOCUMENT_START, DOCUMENT_END, DOCUMENT_RESULT, null, null, FINAL_RESULT
        ))).isInstanceOf(PersistenceException.class);
    }

    private RecruitingSeason persistSeason(Long gisuId, Long schoolId) {
        RecruitingSeason season = seasonAdapter.save(RecruitingSeason.create(gisuId, schoolId));
        em.flush();
        return season;
    }

    private void insertRound(RecruitingSeason season, DatabaseSchedule schedule) {
        em.getEntityManager().createNativeQuery("""
            INSERT INTO recruiting_round (
                created_at, updated_at, recruiting_season_id, type, round_no, status,
                recruitable_tracks, second_choice_enabled, document_start_at, document_end_at,
                document_result_published_at, interview_required, interview_start_at,
                interview_end_at, final_result_published_at, availability_form_id
            ) VALUES (
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :seasonId, 'ADDITIONAL', 2, 'DRAFT',
                ARRAY['PLAN']::TEXT[], FALSE,
                CAST(:documentStartAt AS TIMESTAMPTZ), CAST(:documentEndAt AS TIMESTAMPTZ),
                CAST(:documentResultAt AS TIMESTAMPTZ), :interviewRequired,
                CAST(:interviewStartAt AS TIMESTAMPTZ), CAST(:interviewEndAt AS TIMESTAMPTZ),
                CAST(:finalResultAt AS TIMESTAMPTZ), :availabilityFormId
            )
            """)
            .setParameter("seasonId", season.getId())
            .setParameter("documentStartAt", schedule.documentStartAt())
            .setParameter("documentEndAt", schedule.documentEndAt())
            .setParameter("documentResultAt", schedule.documentResultPublishedAt())
            .setParameter("interviewRequired", schedule.interviewRequired())
            .setParameter("interviewStartAt", schedule.interviewStartAt())
            .setParameter("interviewEndAt", schedule.interviewEndAt())
            .setParameter("finalResultAt", schedule.finalResultPublishedAt())
            .setParameter("availabilityFormId", schedule.availabilityFormId())
            .executeUpdate();
    }

    private DatabaseSchedule noInterviewSchedule(
        String documentStartAt,
        String documentEndAt,
        String documentResultPublishedAt,
        String finalResultPublishedAt,
        Long availabilityFormId
    ) {
        return new DatabaseSchedule(
            documentStartAt,
            documentEndAt,
            documentResultPublishedAt,
            false,
            null,
            null,
            finalResultPublishedAt,
            availabilityFormId
        );
    }

    private DatabaseSchedule interviewSchedule(
        String documentStartAt,
        String documentEndAt,
        String documentResultPublishedAt,
        String interviewStartAt,
        String interviewEndAt,
        String finalResultPublishedAt
    ) {
        return new DatabaseSchedule(
            documentStartAt,
            documentEndAt,
            documentResultPublishedAt,
            true,
            interviewStartAt,
            interviewEndAt,
            finalResultPublishedAt,
            null
        );
    }

    private record DatabaseSchedule(
        String documentStartAt,
        String documentEndAt,
        String documentResultPublishedAt,
        boolean interviewRequired,
        String interviewStartAt,
        String interviewEndAt,
        String finalResultPublishedAt,
        Long availabilityFormId
    ) {
    }
}
