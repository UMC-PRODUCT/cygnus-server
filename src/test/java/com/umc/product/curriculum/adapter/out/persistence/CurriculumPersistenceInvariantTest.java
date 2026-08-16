package com.umc.product.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
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
import com.umc.product.organization.adapter.out.persistence.StudyGroupScheduleJpaRepository;
import com.umc.product.organization.domain.StudyGroupSchedule;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@Import({
    ChallengerWorkbookQueryRepository.class,
    WeeklyBestWorkbookQueryRepository.class
})
@DisplayName("커리큘럼 V2 영속성 불변식")
class CurriculumPersistenceInvariantTest {

    private static final Instant WITHDRAWN_AT = Instant.parse("2026-07-23T00:00:00Z");

    @Autowired private CurriculumJpaRepository curriculumJpaRepository;
    @Autowired private WeeklyCurriculumJpaRepository weeklyCurriculumJpaRepository;
    @Autowired private OriginalWorkbookJpaRepository originalWorkbookJpaRepository;
    @Autowired private OriginalWorkbookMissionJpaRepository originalWorkbookMissionJpaRepository;
    @Autowired private ChallengerWorkbookJpaRepository challengerWorkbookJpaRepository;
    @Autowired private ChallengerWorkbookQueryRepository challengerWorkbookQueryRepository;
    @Autowired private WeeklyBestWorkbookQueryRepository weeklyBestWorkbookQueryRepository;
    @Autowired private MissionSubmissionJpaRepository missionSubmissionJpaRepository;
    @Autowired private WeeklyBestWorkbookJpaRepository weeklyBestWorkbookJpaRepository;
    @Autowired private StudyGroupScheduleJpaRepository studyGroupScheduleJpaRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("철회된 제출 행은 유지·조회되지만 일반 목록과 재제출에서는 제외 또는 거부된다")
    void withdrawnSubmissionRemainsButCannotBeResubmitted() {
        Scenario scenario = persistScenario();
        MissionSubmissionPersistenceAdapter adapter = new MissionSubmissionPersistenceAdapter(
            missionSubmissionJpaRepository
        );
        MissionSubmission submission = adapter.save(MissionSubmission.create(
            scenario.mission(), scenario.workbook(), "첫 제출"
        ));

        submission.withdraw(WITHDRAWN_AT);
        adapter.save(submission);
        entityManager.clear();

        assertThat(adapter.getById(submission.getId()).getWithdrawnAt()).isEqualTo(WITHDRAWN_AT);
        assertThat(adapter.listActiveByChallengerWorkbookId(scenario.workbook().getId())).isEmpty();
        assertThatThrownBy(() -> adapter.save(MissionSubmission.create(
            scenario.mission(), scenario.workbook(), "재제출"
        )))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.WORKBOOK_SUBMISSION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("연결된 제출물이 있으면 FK race도 워크북 삭제 충돌로 변환한다")
    void workbookDeleteForeignKeyRaceMapsToDomainConflict() {
        Scenario scenario = persistScenario();
        missionSubmissionJpaRepository.saveAndFlush(MissionSubmission.create(
            scenario.mission(), scenario.workbook(), "제출"
        ));
        Long workbookId = scenario.workbook().getId();
        entityManager.clear();
        ChallengerWorkbookPersistenceAdapter adapter = new ChallengerWorkbookPersistenceAdapter(
            challengerWorkbookJpaRepository,
            null
        );
        ChallengerWorkbook persistedWorkbook = challengerWorkbookJpaRepository.findById(workbookId).orElseThrow();

        assertThatThrownBy(() -> adapter.delete(persistedWorkbook))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.WORKBOOK_HAS_SUBMISSIONS);
    }

    @Test
    @DisplayName("연결된 제출물이 없으면 챌린저 워크북을 삭제한다")
    void workbookWithoutSubmissionCanBeDeleted() {
        Scenario scenario = persistScenario();
        ChallengerWorkbookPersistenceAdapter adapter = new ChallengerWorkbookPersistenceAdapter(
            challengerWorkbookJpaRepository,
            null
        );

        adapter.delete(scenario.workbook());

        assertThat(challengerWorkbookJpaRepository.findById(scenario.workbook().getId())).isEmpty();
    }

    @Test
    @DisplayName("동일 그룹과 주차의 베스트 중복은 도메인 충돌로 변환한다")
    void duplicateGroupWeekBestMapsToDomainConflict() {
        Scenario scenario = persistScenario();
        WeeklyBestWorkbookPersistenceAdapter adapter = new WeeklyBestWorkbookPersistenceAdapter(
            weeklyBestWorkbookJpaRepository,
            null
        );
        adapter.save(WeeklyBestWorkbook.create(
            scenario.weekly(), 100L, 10L, "첫 선정", 900L
        ));

        assertThatThrownBy(() -> adapter.save(WeeklyBestWorkbook.create(
            scenario.weekly(), 101L, 10L, "중복 선정", 901L
        )))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode")
            .isEqualTo(CurriculumErrorCode.WEEKLY_BEST_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("베스트 batch 조회는 요청한 그룹과 주차 조합의 선정자만 돌려준다")
    void findHoldersReturnsOnlyRequestedCombinations() {
        Scenario scenario = persistScenario();
        Long weeklyCurriculumId = scenario.weekly().getId();
        weeklyBestWorkbookJpaRepository.saveAndFlush(
            WeeklyBestWorkbook.create(scenario.weekly(), 100L, 10L, "10번 그룹 베스트", 900L)
        );
        weeklyBestWorkbookJpaRepository.saveAndFlush(
            WeeklyBestWorkbook.create(scenario.weekly(), 200L, 20L, "20번 그룹 베스트", 900L)
        );

        assertThat(weeklyBestWorkbookQueryRepository.findHolders(Set.of(10L), List.of(weeklyCurriculumId)))
            .singleElement()
            .satisfies(holder -> {
                assertThat(holder.studyGroupId()).isEqualTo(10L);
                assertThat(holder.weeklyCurriculumId()).isEqualTo(weeklyCurriculumId);
                assertThat(holder.memberId()).isEqualTo(100L);
            });

        assertThat(weeklyBestWorkbookQueryRepository.findHolders(Set.of(10L, 20L), List.of(weeklyCurriculumId)))
            .hasSize(2);
        assertThat(weeklyBestWorkbookQueryRepository.findHolders(Set.of(99L), List.of(weeklyCurriculumId)))
            .isEmpty();
        assertThat(weeklyBestWorkbookQueryRepository.findHolders(Set.of(), List.of(weeklyCurriculumId)))
            .isEmpty();
        assertThat(weeklyBestWorkbookQueryRepository.findHolders(Set.of(10L), List.of()))
            .isEmpty();
    }

    @Test
    @DisplayName("같은 그룹과 주차에는 스터디 일정을 하나만 연결할 수 있다")
    void duplicateStudyGroupWeekScheduleRejected() {
        Scenario scenario = persistScenario();
        studyGroupScheduleJpaRepository.saveAndFlush(StudyGroupSchedule.builder()
            .studyGroupId(10L)
            .scheduleId(100L)
            .weeklyCurriculumId(scenario.weekly().getId())
            .build());

        assertThatThrownBy(() -> studyGroupScheduleJpaRepository.saveAndFlush(
            StudyGroupSchedule.builder()
                .studyGroupId(10L)
                .scheduleId(101L)
                .weeklyCurriculumId(scenario.weekly().getId())
                .build()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("베스트 상세용 워크북 조회는 회원·주차·그룹의 정확한 tuple만 반환한다")
    void bestWorkbookLookupUsesExactTuplesWithoutCartesianProduct() {
        Scenario first = persistScenario();
        Curriculum secondCurriculum = curriculumJpaRepository.save(Curriculum.create(
            9002L,
            ChallengerPart.SPRINGBOOT,
            "두 번째 커리큘럼"
        ));
        WeeklyCurriculum secondWeekly = weeklyCurriculumJpaRepository.save(WeeklyCurriculum.create(
            secondCurriculum,
            2L,
            false,
            "2주차",
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-31T00:00:00Z")
        ));
        OriginalWorkbook secondOriginal = originalWorkbookJpaRepository.save(OriginalWorkbook.createAsDraft(
            secondWeekly,
            "두 번째 워크북",
            null,
            null,
            null,
            OriginalWorkbookType.MAIN
        ));
        ChallengerWorkbook second = challengerWorkbookJpaRepository.save(
            ChallengerWorkbook.create(secondOriginal, 200L, 20L)
        );
        ChallengerWorkbook cartesianOnly = challengerWorkbookJpaRepository.saveAndFlush(
            ChallengerWorkbook.create(secondOriginal, first.workbook().getMemberId(), 20L)
        );
        entityManager.clear();

        List<ChallengerWorkbook> result = challengerWorkbookQueryRepository.findByLookupKeys(List.of(
            new com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort
                .ChallengerWorkbookLookupKey(
                    first.workbook().getMemberId(),
                    first.weekly().getId(),
                    first.workbook().getStudyGroupId()
                ),
            new com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort
                .ChallengerWorkbookLookupKey(
                    second.getMemberId(),
                    secondWeekly.getId(),
                    second.getStudyGroupId()
                )
        ));

        assertThat(result).extracting(ChallengerWorkbook::getId)
            .containsExactlyInAnyOrder(first.workbook().getId(), second.getId())
            .doesNotContain(cartesianOnly.getId());
    }

    @Test
    @DisplayName("학교 필터 회원이 500명을 넘어도 베스트 조회의 전체 개수와 페이지 순서를 유지한다")
    void bestWorkbookSearchPreservesPaginationAcrossMemberIdChunks() {
        Scenario scenario = persistScenario();
        List<WeeklyBestWorkbook> saved = weeklyBestWorkbookJpaRepository.saveAllAndFlush(
            LongStream.rangeClosed(1, 501)
                .mapToObj(id -> WeeklyBestWorkbook.create(
                    scenario.weekly(),
                    id,
                    id,
                    "선정 사유 " + id,
                    10_000L + id
                ))
                .toList()
        );
        entityManager.clear();
        Set<Long> memberIds = LongStream.rangeClosed(1, 501)
            .boxed()
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> expectedIds = saved.stream()
            .map(WeeklyBestWorkbook::getId)
            .sorted(Comparator.reverseOrder())
            .skip(20)
            .limit(20)
            .toList();

        var page = weeklyBestWorkbookQueryRepository.searchBestWorkbooks(
            GetBestWorkbooksQuery.of(9001L, null, null, null, null, 1, 20)
                .withMemberIds(memberIds)
        );

        assertThat(page.getTotalElements()).isEqualTo(501);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getContent()).extracting(WeeklyBestWorkbook::getId)
            .containsExactlyElementsOf(expectedIds);
    }

    private Scenario persistScenario() {
        Curriculum curriculum = curriculumJpaRepository.save(Curriculum.create(
            9001L, ChallengerPart.SPRINGBOOT, "커리큘럼"
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
            weekly, "워크북", null, null, null, OriginalWorkbookType.MAIN
        ));
        OriginalWorkbookMission mission = originalWorkbookMissionJpaRepository.save(
            OriginalWorkbookMission.create(original, "미션", null, MissionType.MEMO, true)
        );
        ChallengerWorkbook workbook = challengerWorkbookJpaRepository.saveAndFlush(
            ChallengerWorkbook.create(original, 100L, 10L)
        );
        return new Scenario(weekly, mission, workbook);
    }

    private record Scenario(
        WeeklyCurriculum weekly,
        OriginalWorkbookMission mission,
        ChallengerWorkbook workbook
    ) {
    }
}
