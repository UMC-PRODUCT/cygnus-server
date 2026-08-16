package com.umc.product.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("커리큘럼 V2 동시성 불변식")
class CurriculumConcurrencyInvariantTest {

    @Autowired private CurriculumJpaRepository curriculumJpaRepository;
    @Autowired private WeeklyCurriculumJpaRepository weeklyCurriculumJpaRepository;
    @Autowired private OriginalWorkbookJpaRepository originalWorkbookJpaRepository;
    @Autowired private OriginalWorkbookMissionJpaRepository originalWorkbookMissionJpaRepository;
    @Autowired private ChallengerWorkbookJpaRepository challengerWorkbookJpaRepository;
    @Autowired private MissionSubmissionJpaRepository missionSubmissionJpaRepository;
    @Autowired private WeeklyBestWorkbookJpaRepository weeklyBestWorkbookJpaRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("철회가 행 잠금을 선점하면 동시 수정은 철회 상태를 확인하고 409로 실패한다")
    void withdrawAndEdit_areSerializedBySubmissionLock() throws Exception {
        Scenario scenario = transaction().execute(status -> persistScenario());
        Long submissionId = transaction().execute(status ->
            missionSubmissionJpaRepository.saveAndFlush(MissionSubmission.create(
                scenario.mission(),
                scenario.workbook(),
                "첫 제출"
            )).getId()
        );
        MissionSubmissionPersistenceAdapter adapter =
            new MissionSubmissionPersistenceAdapter(missionSubmissionJpaRepository);
        CountDownLatch withdrawLocked = new CountDownLatch(1);
        CountDownLatch editStarted = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Void> withdraw = executor.submit(() -> {
                transaction().executeWithoutResult(status -> {
                    MissionSubmission submission = adapter.getByIdForUpdate(submissionId);
                    withdrawLocked.countDown();
                    await(editStarted);
                    submission.withdraw(Instant.parse("2026-07-24T00:00:00Z"));
                    adapter.save(submission);
                });
                return null;
            });
            Future<Throwable> edit = executor.submit(() -> {
                await(withdrawLocked);
                editStarted.countDown();
                try {
                    transaction().executeWithoutResult(status -> {
                        MissionSubmission submission = adapter.getByIdForUpdate(submissionId);
                        submission.edit("동시 수정");
                        adapter.save(submission);
                    });
                    return null;
                } catch (Throwable throwable) {
                    return throwable;
                }
            });

            withdraw.get(10, TimeUnit.SECONDS);
            Throwable editFailure = edit.get(10, TimeUnit.SECONDS);

            assertThat(editFailure)
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.MISSION_SUBMISSION_ALREADY_WITHDRAWN);
        }

        MissionSubmission persisted = transaction().execute(status ->
            missionSubmissionJpaRepository.findById(submissionId).orElseThrow()
        );
        assertThat(persisted.isWithdrawn()).isTrue();
        assertThat(persisted.getContent()).isEqualTo("첫 제출");
    }

    @Test
    @DisplayName("동일 그룹·주차 베스트 동시 선정은 하나만 commit되고 나머지는 409다")
    void concurrentBestSelection_allowsOnlyOneCommit() throws Exception {
        Scenario scenario = transaction().execute(status -> persistScenario());
        WeeklyBestWorkbookPersistenceAdapter adapter = new WeeklyBestWorkbookPersistenceAdapter(
            weeklyBestWorkbookJpaRepository,
            null
        );
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Throwable>> attempts = List.of(100L, 101L).stream()
                .map(memberId -> executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    try {
                        transaction().executeWithoutResult(status -> {
                            WeeklyCurriculum weekly =
                                weeklyCurriculumJpaRepository.findById(scenario.weekly().getId()).orElseThrow();
                            adapter.save(WeeklyBestWorkbook.create(
                                weekly,
                                memberId,
                                10L,
                                "선정 사유",
                                900L + memberId
                            ));
                        });
                        return null;
                    } catch (Throwable throwable) {
                        return throwable;
                    }
                }))
                .toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Throwable> results = attempts.stream().map(this::get).toList();

            assertThat(results).filteredOn(result -> result == null).hasSize(1);
            assertThat(results).filteredOn(result -> result != null).singleElement()
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.WEEKLY_BEST_ALREADY_EXISTS);
        }

        long count = transaction().execute(status -> weeklyBestWorkbookJpaRepository.count());
        assertThat(count).isEqualTo(1L);
    }

    private Scenario persistScenario() {
        Curriculum curriculum = curriculumJpaRepository.save(Curriculum.create(
            9001L,
            ChallengerPart.SPRINGBOOT,
            "커리큘럼"
        ));
        WeeklyCurriculum weekly = weeklyCurriculumJpaRepository.save(WeeklyCurriculum.create(
            curriculum,
            1L,
            false,
            "1주차",
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-31T00:00:00Z")
        ));
        OriginalWorkbook original = originalWorkbookJpaRepository.save(OriginalWorkbook.createAsDraft(
            weekly,
            "워크북",
            null,
            null,
            null,
            OriginalWorkbookType.MAIN
        ));
        OriginalWorkbookMission mission = originalWorkbookMissionJpaRepository.save(
            OriginalWorkbookMission.create(original, "미션", null, MissionType.MEMO, true)
        );
        ChallengerWorkbook workbook = challengerWorkbookJpaRepository.saveAndFlush(
            ChallengerWorkbook.create(original, 100L, 10L)
        );
        return new Scenario(weekly, mission, workbook);
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 latch 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Throwable get(Future<Throwable> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record Scenario(
        WeeklyCurriculum weekly,
        OriginalWorkbookMission mission,
        ChallengerWorkbook workbook
    ) {
    }
}
