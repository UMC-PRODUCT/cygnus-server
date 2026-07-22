package com.umc.product.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.application.port.in.query.dto.GetWorkbookSubmissionsQuery;
import com.umc.product.curriculum.application.port.in.query.dto.WorkbookSubmissionInfo;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.MissionFeedback;
import com.umc.product.curriculum.domain.MissionSubmission;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.WorkbookMission;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

@DisplayName("Curriculum persistence adapter 단위 계약")
class CurriculumPersistenceAdapterUnitTest {

    @Test
    @DisplayName("Curriculum adapter는 Optional/get·존재·저장·삭제 계약을 위임한다")
    void curriculum_adapter_contract() {
        CurriculumJpaRepository jpa = mock(CurriculumJpaRepository.class);
        CurriculumQueryRepository query = mock(CurriculumQueryRepository.class);
        CurriculumPersistenceAdapter sut = new CurriculumPersistenceAdapter(jpa, query);
        Curriculum entity = mock(Curriculum.class);
        CurriculumProjection projection = new CurriculumProjection(1L, ChallengerPart.SPRINGBOOT, "제목");
        given(jpa.findById(1L)).willReturn(Optional.of(entity));
        given(jpa.findById(404L)).willReturn(Optional.empty());
        given(query.findByGisuIdAndPart(9L, ChallengerPart.SPRINGBOOT)).willReturn(Optional.of(projection));
        given(query.findByGisuIdAndPart(10L, ChallengerPart.SPRINGBOOT)).willReturn(Optional.empty());
        given(jpa.existsByGisuIdAndPart(9L, ChallengerPart.SPRINGBOOT)).willReturn(true);
        given(jpa.save(entity)).willReturn(entity);

        assertThat(sut.findById(1L)).contains(entity);
        assertThat(sut.findByGisuIdAndPart(9L, ChallengerPart.SPRINGBOOT)).contains(projection);
        assertThat(sut.getByGisuIdAndPart(9L, ChallengerPart.SPRINGBOOT)).isSameAs(projection);
        assertError(() -> sut.getByGisuIdAndPart(10L, ChallengerPart.SPRINGBOOT),
            CurriculumErrorCode.CURRICULUM_NOT_FOUND);
        assertThat(sut.existsByGisuIdAndPart(9L, ChallengerPart.SPRINGBOOT)).isTrue();
        assertThat(sut.save(entity)).isSameAs(entity);
        sut.delete(entity);
        then(jpa).should().delete(entity);
    }

    @Test
    @DisplayName("ChallengerWorkbook adapter는 not-found와 빈 IN을 fail-safe로 처리한다")
    void challenger_workbook_adapter_contract() {
        ChallengerWorkbookJpaRepository jpa = mock(ChallengerWorkbookJpaRepository.class);
        ChallengerWorkbookPersistenceAdapter sut = new ChallengerWorkbookPersistenceAdapter(jpa);
        ChallengerWorkbook entity = mock(ChallengerWorkbook.class);
        given(jpa.findById(1L)).willReturn(Optional.of(entity));
        given(jpa.findById(404L)).willReturn(Optional.empty());
        given(jpa.findByMemberIdAndOriginalWorkbookId(2L, 3L)).willReturn(Optional.of(entity));
        given(jpa.existsByOriginalWorkbookId(3L)).willReturn(true);
        given(jpa.findByMemberIdAndOriginalWorkbookIdIn(2L, List.of(3L))).willReturn(List.of(entity));
        given(jpa.save(entity)).willReturn(entity);

        assertThat(sut.findById(1L)).isSameAs(entity);
        assertError(() -> sut.findById(404L), CurriculumErrorCode.CHALLENGER_WORKBOOK_NOT_FOUND);
        assertThat(sut.findByMemberIdAndOriginalWorkbookId(2L, 3L)).contains(entity);
        assertThat(sut.existsByOriginalWorkbookId(3L)).isTrue();
        assertThat(sut.findByMemberIdAndOriginalWorkbookIdIn(2L, List.of())).isEmpty();
        assertThat(sut.findByMemberIdAndOriginalWorkbookIdIn(2L, List.of(3L))).containsExactly(entity);
        assertThat(sut.save(entity)).isSameAs(entity);
    }

    @Test
    @DisplayName("원본 미션 adapter는 get·빈 batch·저장·삭제 계약을 보존한다")
    void original_workbook_mission_adapter_contract() {
        OriginalWorkbookMissionJpaRepository jpa = mock(OriginalWorkbookMissionJpaRepository.class);
        OriginalWorkbookMissionPersistenceAdapter sut = new OriginalWorkbookMissionPersistenceAdapter(jpa);
        OriginalWorkbookMission mission = mock(OriginalWorkbookMission.class);
        given(jpa.findById(1L)).willReturn(Optional.of(mission));
        given(jpa.findById(404L)).willReturn(Optional.empty());
        given(jpa.findByOriginalWorkbookId(2L)).willReturn(List.of(mission));
        given(jpa.findByOriginalWorkbookIdIn(List.of(2L))).willReturn(List.of(mission));
        given(jpa.save(mission)).willReturn(mission);

        assertThat(sut.getById(1L)).isSameAs(mission);
        assertError(() -> sut.getById(404L), CurriculumErrorCode.MISSION_NOT_FOUND);
        assertThat(sut.findByOriginalWorkbookId(2L)).containsExactly(mission);
        assertThat(sut.findByOriginalWorkbookIdIn(List.of())).isEmpty();
        assertThat(sut.findByOriginalWorkbookIdIn(List.of(2L))).containsExactly(mission);
        assertThat(sut.save(mission)).isSameAs(mission);
        sut.delete(mission);
        then(jpa).should().delete(mission);
    }

    @Test
    @DisplayName("제출·피드백 adapter는 빈 batch를 단축하고 각 repository를 구분해 위임한다")
    void mission_submission_adapter_contract() {
        MissionSubmissionJpaRepository submissionJpa = mock(MissionSubmissionJpaRepository.class);
        MissionFeedbackJpaRepository feedbackJpa = mock(MissionFeedbackJpaRepository.class);
        MissionSubmissionPersistenceAdapter sut = new MissionSubmissionPersistenceAdapter(submissionJpa, feedbackJpa);
        MissionSubmission submission = mock(MissionSubmission.class);
        MissionFeedback feedback = mock(MissionFeedback.class);
        given(submissionJpa.findByChallengerWorkbook_Id(1L)).willReturn(List.of(submission));
        given(submissionJpa.findByChallengerWorkbook_IdIn(List.of(1L))).willReturn(List.of(submission));
        given(feedbackJpa.findByMissionSubmission_IdIn(List.of(2L))).willReturn(List.of(feedback));
        given(submissionJpa.existsByOriginalWorkbookMission_Id(3L)).willReturn(true);

        assertThat(sut.findByChallengerWorkbookId(1L)).containsExactly(submission);
        assertThat(sut.findByChallengerWorkbookIdIn(List.of())).isEmpty();
        assertThat(sut.findByChallengerWorkbookIdIn(List.of(1L))).containsExactly(submission);
        assertThat(sut.findByMissionSubmissionIdIn(List.of())).isEmpty();
        assertThat(sut.findByMissionSubmissionIdIn(List.of(2L))).containsExactly(feedback);
        assertThat(sut.existsByOriginalWorkbookMissionId(3L)).isTrue();
    }

    @Test
    @DisplayName("OriginalWorkbook adapter는 전체 batch 존재를 검증하고 모든 상태 조회·write를 위임한다")
    void original_workbook_adapter_contract() {
        OriginalWorkbookJpaRepository jpa = mock(OriginalWorkbookJpaRepository.class);
        CurriculumQueryRepository query = mock(CurriculumQueryRepository.class);
        OriginalWorkbookPersistenceAdapter sut = new OriginalWorkbookPersistenceAdapter(jpa, query);
        OriginalWorkbook first = mock(OriginalWorkbook.class);
        OriginalWorkbook second = mock(OriginalWorkbook.class);
        Instant now = Instant.parse("2030-01-01T00:00:00Z");
        given(jpa.findById(1L)).willReturn(Optional.of(first));
        given(jpa.findById(404L)).willReturn(Optional.empty());
        given(jpa.findAllById(List.of(1L, 2L))).willReturn(List.of(first, second));
        given(jpa.findAllById(List.of(1L, 404L))).willReturn(List.of(first));
        given(jpa.findByWeeklyCurriculumIdAndOriginalWorkbookStatus(3L, OriginalWorkbookStatus.RELEASED))
            .willReturn(List.of(first));
        given(jpa.findByWeeklyCurriculumIdInAndOriginalWorkbookStatus(
            List.of(3L), OriginalWorkbookStatus.RELEASED)).willReturn(List.of(first));
        given(query.findUnreleasedWorkbookIdsWithStartDateBefore(now)).willReturn(List.of(second));
        given(jpa.save(first)).willReturn(first);
        given(jpa.saveAll(List.of(first, second))).willReturn(List.of(first, second));

        assertThat(sut.getById(1L)).isSameAs(first);
        assertError(() -> sut.getById(404L), CurriculumErrorCode.WORKBOOK_NOT_FOUND);
        assertThat(sut.batchGetByIds(List.of(1L, 2L))).containsExactly(first, second);
        assertError(() -> sut.batchGetByIds(List.of(1L, 404L)), CurriculumErrorCode.WORKBOOK_NOT_FOUND);
        assertThat(sut.findReleasedByWeeklyCurriculumId(3L)).containsExactly(first);
        assertThat(sut.findReleasedByWeeklyCurriculumIdIn(List.of())).isEmpty();
        assertThat(sut.findReleasedByWeeklyCurriculumIdIn(List.of(3L))).containsExactly(first);
        assertThat(sut.findUnreleasedWithStartDateBefore(now)).containsExactly(second);
        assertThat(sut.save(first)).isSameAs(first);
        assertThat(sut.saveAll(List.of(first, second))).containsExactly(first, second);
        sut.delete(first);
        then(jpa).should().delete(first);
    }

    @Test
    @DisplayName("WeeklyCurriculum adapter는 week filter 유무와 workbook 상태별 존재 검사를 구분한다")
    void weekly_curriculum_adapter_contract() {
        WeeklyCurriculumJpaRepository jpa = mock(WeeklyCurriculumJpaRepository.class);
        WeeklyCurriculumQueryRepository query = mock(WeeklyCurriculumQueryRepository.class);
        WeeklyCurriculumPersistenceAdapter sut = new WeeklyCurriculumPersistenceAdapter(jpa, query);
        WeeklyCurriculum weekly = mock(WeeklyCurriculum.class);
        given(jpa.findById(1L)).willReturn(Optional.of(weekly));
        given(jpa.findById(404L)).willReturn(Optional.empty());
        given(jpa.findByCurriculumIdAndWeekNoOrderByIsExtraAsc(2L, 3L)).willReturn(List.of(weekly));
        given(jpa.findByCurriculumIdOrderByWeekNoAscIsExtraAsc(2L)).willReturn(List.of(weekly));
        given(jpa.existsByCurriculumId(2L)).willReturn(true);
        given(query.existsOriginalWorkbook(1L, null)).willReturn(true);
        given(query.existsOriginalWorkbook(1L, OriginalWorkbookStatus.RELEASED)).willReturn(false);
        given(jpa.existsByCurriculumIdAndWeekNoAndIsExtra(2L, 3L, false)).willReturn(true);
        given(jpa.existsByCurriculumIdAndWeekNoAndIsExtraAndIdNot(2L, 3L, false, 1L)).willReturn(false);
        given(jpa.save(weekly)).willReturn(weekly);

        assertThat(sut.findById(1L)).contains(weekly);
        assertThat(sut.getById(1L)).isSameAs(weekly);
        assertError(() -> sut.getById(404L), CurriculumErrorCode.WEEKLY_CURRICULUM_NOT_FOUND);
        assertThat(sut.findByCurriculumId(2L, 3L)).containsExactly(weekly);
        assertThat(sut.findByCurriculumId(2L, null)).containsExactly(weekly);
        assertThat(sut.existsByCurriculumId(2L)).isTrue();
        assertThat(sut.existsOriginalWorkbookByWeeklyCurriculumId(1L)).isTrue();
        assertThat(sut.existsReleasedOriginalWorkbookByWeeklyCurriculumId(1L)).isFalse();
        assertThat(sut.existsByCurriculumIdAndWeekNoAndIsExtra(2L, 3L, false)).isTrue();
        assertThat(sut.existsByCurriculumIdAndWeekNoAndIsExtraAndIdNot(2L, 3L, false, 1L)).isFalse();
        assertThat(sut.save(weekly)).isSameAs(weekly);
        sut.delete(weekly);
        then(jpa).should().delete(weekly);
    }

    @Test
    @DisplayName("legacy mission·submission adapter는 query 결과를 그대로 보존한다")
    void legacy_adapter_contract() {
        WorkbookMissionJpaRepository missionJpa = mock(WorkbookMissionJpaRepository.class);
        WorkbookMissionPersistenceAdapter missionSut = new WorkbookMissionPersistenceAdapter(missionJpa);
        WorkbookMission mission = mock(WorkbookMission.class);
        given(missionJpa.findById(1L)).willReturn(Optional.of(mission));
        given(missionJpa.findByOriginalWorkbookId(2L)).willReturn(List.of(mission));
        assertThat(missionSut.findById(1L)).contains(mission);
        assertThat(missionSut.findByOriginalWorkbookId(2L)).containsExactly(mission);

        WorkbookSubmissionQueryRepository query = mock(WorkbookSubmissionQueryRepository.class);
        WorkbookSubmissionPersistenceAdapter submissionSut = new WorkbookSubmissionPersistenceAdapter(query);
        GetWorkbookSubmissionsQuery criteria = new GetWorkbookSubmissionsQuery(null, 1, null, null, null, 1);
        WorkbookSubmissionInfo info = mock(WorkbookSubmissionInfo.class);
        given(query.findSubmissions(criteria)).willReturn(List.of(info));
        assertThat(submissionSut.findSubmissions(criteria)).containsExactly(info);
    }

    private void assertError(Runnable action, CurriculumErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(CurriculumDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code)
            );
    }
}
