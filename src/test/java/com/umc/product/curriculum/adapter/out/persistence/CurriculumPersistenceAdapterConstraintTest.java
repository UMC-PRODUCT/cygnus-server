package com.umc.product.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@ExtendWith(MockitoExtension.class)
class CurriculumPersistenceAdapterConstraintTest {

    @Mock
    private MissionSubmissionJpaRepository missionSubmissionJpaRepository;

    @Mock
    private WeeklyBestWorkbookJpaRepository weeklyBestWorkbookJpaRepository;

    @Mock
    private WeeklyBestWorkbookQueryRepository weeklyBestWorkbookQueryRepository;

    @Mock
    private ChallengerWorkbookJpaRepository challengerWorkbookJpaRepository;

    @Mock
    private ChallengerWorkbookQueryRepository challengerWorkbookQueryRepository;

    @Test
    void 제출_중복_UNIQUE만_도메인_충돌로_변환한다() {
        MissionSubmission submission = mock(MissionSubmission.class);
        DataIntegrityViolationException duplicate = constraintViolation(
            "uk_mission_submission_original_wb_mission_challenger_wb_id"
        );
        when(missionSubmissionJpaRepository.save(submission)).thenReturn(submission);
        doThrow(duplicate).when(missionSubmissionJpaRepository).flush();

        MissionSubmissionPersistenceAdapter sut =
            new MissionSubmissionPersistenceAdapter(missionSubmissionJpaRepository);

        assertThatThrownBy(() -> sut.save(submission))
            .isInstanceOfSatisfying(CurriculumDomainException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getBaseCode())
                    .isEqualTo(CurriculumErrorCode.WORKBOOK_SUBMISSION_ALREADY_EXISTS));
    }

    @Test
    void 제출의_다른_무결성_오류는_중복으로_오분류하지_않는다() {
        MissionSubmission submission = mock(MissionSubmission.class);
        DataIntegrityViolationException foreignKey = constraintViolation(
            "FK_MISSION_SUBMISSION_ON_CHALLENGER_WORKBOOK"
        );
        when(missionSubmissionJpaRepository.save(submission)).thenReturn(submission);
        doThrow(foreignKey).when(missionSubmissionJpaRepository).flush();

        MissionSubmissionPersistenceAdapter sut =
            new MissionSubmissionPersistenceAdapter(missionSubmissionJpaRepository);

        assertThatThrownBy(() -> sut.save(submission)).isSameAs(foreignKey);
    }

    @Test
    void 베스트_중복_UNIQUE만_도메인_충돌로_변환한다() {
        WeeklyBestWorkbook bestWorkbook = mock(WeeklyBestWorkbook.class);
        DataIntegrityViolationException duplicate = constraintViolation(
            "uk_weekly_best_workbook_study_group_week"
        );
        when(weeklyBestWorkbookJpaRepository.save(bestWorkbook)).thenReturn(bestWorkbook);
        doThrow(duplicate).when(weeklyBestWorkbookJpaRepository).flush();

        WeeklyBestWorkbookPersistenceAdapter sut = new WeeklyBestWorkbookPersistenceAdapter(
            weeklyBestWorkbookJpaRepository,
            weeklyBestWorkbookQueryRepository
        );

        assertThatThrownBy(() -> sut.save(bestWorkbook))
            .isInstanceOfSatisfying(CurriculumDomainException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getBaseCode())
                    .isEqualTo(CurriculumErrorCode.WEEKLY_BEST_ALREADY_EXISTS));
    }

    @Test
    void 베스트의_다른_무결성_오류는_중복으로_오분류하지_않는다() {
        WeeklyBestWorkbook bestWorkbook = mock(WeeklyBestWorkbook.class);
        DataIntegrityViolationException foreignKey = constraintViolation(
            "FK_WEEKLY_BEST_WORKBOOK_ON_WEEKLY_CURRICULUM"
        );
        when(weeklyBestWorkbookJpaRepository.save(bestWorkbook)).thenReturn(bestWorkbook);
        doThrow(foreignKey).when(weeklyBestWorkbookJpaRepository).flush();

        WeeklyBestWorkbookPersistenceAdapter sut = new WeeklyBestWorkbookPersistenceAdapter(
            weeklyBestWorkbookJpaRepository,
            weeklyBestWorkbookQueryRepository
        );

        assertThatThrownBy(() -> sut.save(bestWorkbook)).isSameAs(foreignKey);
    }

    @Test
    void 동시_자체_배포의_워크북_UNIQUE는_명시적인_409로_변환한다() {
        ChallengerWorkbook workbook = mock(ChallengerWorkbook.class);
        DataIntegrityViolationException duplicate = constraintViolation(
            "uk_challenger_workbook_member_id_original_workbook_id"
        );
        when(challengerWorkbookJpaRepository.saveAndFlush(workbook)).thenThrow(duplicate);

        ChallengerWorkbookPersistenceAdapter sut = new ChallengerWorkbookPersistenceAdapter(
            challengerWorkbookJpaRepository,
            challengerWorkbookQueryRepository
        );

        assertThatThrownBy(() -> sut.save(workbook))
            .isInstanceOfSatisfying(CurriculumDomainException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getBaseCode())
                    .isEqualTo(CurriculumErrorCode.CHALLENGER_WORKBOOK_ALREADY_EXISTS));
    }

    @Test
    void 워크북_삭제의_제출_FK만_연결_데이터_충돌로_변환한다() {
        ChallengerWorkbook workbook = mock(ChallengerWorkbook.class);
        DataIntegrityViolationException submissionForeignKey = constraintViolation(
            "FK_MISSION_SUBMISSION_ON_CHALLENGER_WORKBOOK"
        );
        doThrow(submissionForeignKey).when(challengerWorkbookJpaRepository).flush();

        ChallengerWorkbookPersistenceAdapter sut = new ChallengerWorkbookPersistenceAdapter(
            challengerWorkbookJpaRepository,
            challengerWorkbookQueryRepository
        );

        assertThatThrownBy(() -> sut.delete(workbook))
            .isInstanceOfSatisfying(CurriculumDomainException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getBaseCode())
                    .isEqualTo(CurriculumErrorCode.WORKBOOK_HAS_SUBMISSIONS));
    }

    @Test
    void 워크북_삭제의_다른_무결성_오류는_연결_제출로_오분류하지_않는다() {
        ChallengerWorkbook workbook = mock(ChallengerWorkbook.class);
        DataIntegrityViolationException unrelated = constraintViolation("some_other_constraint");
        doThrow(unrelated).when(challengerWorkbookJpaRepository).flush();

        ChallengerWorkbookPersistenceAdapter sut = new ChallengerWorkbookPersistenceAdapter(
            challengerWorkbookJpaRepository,
            challengerWorkbookQueryRepository
        );

        assertThatThrownBy(() -> sut.delete(workbook)).isSameAs(unrelated);
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
            "constraint violation",
            new SQLException(),
            constraintName
        );
        return new DataIntegrityViolationException("data integrity violation", cause);
    }
}
