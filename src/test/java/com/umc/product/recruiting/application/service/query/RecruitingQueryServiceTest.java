package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetGisuAuthorityScopeUseCase;
import com.umc.product.authorization.application.port.in.query.dto.GisuAuthorityScopeInfo;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationQuestionScopeInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPartStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryQuery;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

@ExtendWith(MockitoExtension.class)
class RecruitingQueryServiceTest {

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    GetGisuAuthorityScopeUseCase getGisuAuthorityScopeUseCase;

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Mock
    LoadRecruitingRoundPort loadRoundPort;

    @Mock
    LoadRecruitingSeasonPort loadSeasonPort;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    GetFormUseCase getFormUseCase;

    @Mock
    GetRecruitingApplicationQuestionScopeUseCase getQuestionScopeUseCase;

    @InjectMocks
    RecruitingQueryService sut;

    private static final Set<RecruitingApplicationStatus> SUMMARY_STATUSES = EnumSet.complementOf(EnumSet.of(
        RecruitingApplicationStatus.DRAFT,
        RecruitingApplicationStatus.CANCELLED
    ));

    @Test
    @DisplayName("상태_요약은_summary_row의_지원서_상태를_집계한다")
    void summarizeApplicationStatuses() {
        // Given
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeForAllSchools());
        givenSummaryScope();
        given(loadApplicationPort.searchSummaryRows(1L, Set.of(10L), null, SUMMARY_STATUSES)).willReturn(List.of(
            row("지원자1", RecruitingApplicationStatus.SUBMITTED),
            row("지원자2", RecruitingApplicationStatus.SUBMITTED),
            row("지원자3", RecruitingApplicationStatus.FINAL_PASSED)
        ));

        // When
        RecruitingStatusSummaryInfo result = sut.getStatusSummary(summaryQuery(Set.of(10L), Set.of()));

        // Then
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.countByStatus().get(RecruitingApplicationStatus.SUBMITTED)).isEqualTo(2L);
        assertThat(result.countByStatus().get(RecruitingApplicationStatus.FINAL_PASSED)).isEqualTo(1L);
        assertThat(result.schools()).singleElement().satisfies(school -> {
            assertThat(school.schoolName()).isEqualTo("테스트대학교");
            assertThat(school.rounds()).singleElement().satisfies(round -> {
                assertThat(round.roundId()).isEqualTo(20L);
                assertThat(round.totalCount()).isEqualTo(3L);
            });
        });
    }

    @Test
    @DisplayName("상태 요약은 DRAFT·CANCELLED를 제외한 상태만 집계 대상으로 조회한다")
    void statusSummaryExcludesDraftAndCancelledFromQuery() {
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeForAllSchools());
        givenSummaryScope();
        given(loadApplicationPort.searchSummaryRows(1L, Set.of(10L), null, SUMMARY_STATUSES))
            .willReturn(List.of(row("지원자", RecruitingApplicationStatus.SUBMITTED)));

        sut.getStatusSummary(summaryQuery(Set.of(10L), Set.of()));

        then(loadApplicationPort).should().searchSummaryRows(1L, Set.of(10L), null, SUMMARY_STATUSES);
        assertThat(SUMMARY_STATUSES)
            .doesNotContain(RecruitingApplicationStatus.DRAFT, RecruitingApplicationStatus.CANCELLED);
    }

    @Test
    @DisplayName("상태 요약은 1지망 파트별로 상태를 교차집계하고 sortOrder 순으로 정렬한다")
    void statusSummaryAggregatesByFirstChoicePart() {
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeForAllSchools());
        givenSummaryScope();
        given(loadApplicationPort.searchSummaryRows(1L, Set.of(10L), null, SUMMARY_STATUSES)).willReturn(List.of(
            row("웹1", RecruitingApplicationStatus.SUBMITTED, ChallengerTrack.WEB_PRODUCT_ENGINEER),
            row("웹2", RecruitingApplicationStatus.FINAL_PASSED, ChallengerTrack.WEB_PRODUCT_ENGINEER),
            row("기획1", RecruitingApplicationStatus.SUBMITTED, ChallengerTrack.PLAN)
        ));

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(summaryQuery(Set.of(10L), Set.of()));

        // 지원자가 없는 파트(DESIGN, MOBILE)도 0건 슬롯으로 포함되고 sortOrder 순으로 정렬된다.
        assertThat(result.parts())
            .extracting(part -> part.part())
            .containsExactly(
                ChallengerTrack.PLAN,
                ChallengerTrack.DESIGN,
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.MOBILE_PRODUCT_ENGINEER
            );
        assertThat(result.parts())
            .filteredOn(part -> part.part() == ChallengerTrack.WEB_PRODUCT_ENGINEER)
            .singleElement()
            .satisfies(part -> {
                assertThat(part.totalCount()).isEqualTo(2L);
                assertThat(part.countByStatus().get(RecruitingApplicationStatus.SUBMITTED)).isEqualTo(1L);
                assertThat(part.countByStatus().get(RecruitingApplicationStatus.FINAL_PASSED)).isEqualTo(1L);
            });
        assertThat(result.parts())
            .filteredOn(part -> part.part() == ChallengerTrack.DESIGN)
            .singleElement()
            .satisfies(part -> {
                assertThat(part.totalCount()).isZero();
                assertThat(part.countByStatus()).isEmpty();
            });
        assertThat(result.parts().stream().mapToLong(part -> part.totalCount()).sum())
            .isEqualTo(result.totalCount());
        assertThat(result.schools()).singleElement()
            .satisfies(school -> assertThat(school.rounds()).singleElement()
                .satisfies(round -> assertThat(round.parts()).hasSize(4)));
    }

    @Test
    @DisplayName("상태 요약은 지원서가 전혀 없어도 전체·학교·Round 모두에서 4개 파트 슬롯을 0건으로 반환한다")
    void statusSummaryReturnsAllPartSlotsWhenNoApplications() {
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeForAllSchools());
        givenSummaryScope();
        given(loadApplicationPort.searchSummaryRows(1L, Set.of(10L), null, SUMMARY_STATUSES))
            .willReturn(List.of());

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(summaryQuery(Set.of(10L), Set.of()));

        assertThat(result.totalCount()).isZero();
        assertAllPartSlotsZero(result.parts());
        assertThat(result.schools()).singleElement().satisfies(school -> {
            assertAllPartSlotsZero(school.parts());
            assertThat(school.rounds()).singleElement()
                .satisfies(round -> assertAllPartSlotsZero(round.parts()));
        });
    }

    private void assertAllPartSlotsZero(List<RecruitingPartStatusSummaryInfo> parts) {
        assertThat(parts)
            .extracting(part -> part.part())
            .containsExactly(
                ChallengerTrack.PLAN,
                ChallengerTrack.DESIGN,
                ChallengerTrack.WEB_PRODUCT_ENGINEER,
                ChallengerTrack.MOBILE_PRODUCT_ENGINEER
            );
        assertThat(parts).allSatisfy(part -> {
            assertThat(part.totalCount()).isZero();
            assertThat(part.countByStatus()).isEmpty();
        });
    }

    @Test
    @DisplayName("상태 요약은 학교·Round가 여러 개일 때도 각 계층의 parts를 해당 그룹 rows로만 집계한다")
    void statusSummaryAggregatesPartsPerSchoolAndRound() {
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeForAllSchools());
        RecruitingSeason season10 = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season10, "id", 5L);
        RecruitingSeason season11 = RecruitingSeason.create(1L, 11L);
        ReflectionTestUtils.setField(season11, "id", 6L);
        RecruitingRound round20 = summaryRound(season10, 20L, "A대 1차");
        RecruitingRound round22 = summaryRound(season10, 22L, "A대 2차");
        RecruitingRound round21 = summaryRound(season11, 21L, "B대 1차");
        given(getSchoolUseCase.getSchoolListByGisuId(1L))
            .willReturn(List.of(school(10L, "A대학교"), school(11L, "B대학교")));
        given(loadSeasonPort.listByGisuId(1L)).willReturn(List.of(season10, season11));
        given(loadRoundPort.listBySeasonIds(List.of(5L, 6L))).willReturn(List.of(round20, round22, round21));
        given(loadApplicationPort.searchSummaryRows(1L, Set.of(10L, 11L), null, SUMMARY_STATUSES)).willReturn(List.of(
            row("A웹서류", RecruitingApplicationStatus.SUBMITTED, ChallengerTrack.WEB_PRODUCT_ENGINEER, 10L, 20L),
            row("A기획서류", RecruitingApplicationStatus.SUBMITTED, ChallengerTrack.PLAN, 10L, 20L),
            row("A웹최종", RecruitingApplicationStatus.FINAL_PASSED, ChallengerTrack.WEB_PRODUCT_ENGINEER, 10L, 22L),
            row("B기획", RecruitingApplicationStatus.SUBMITTED, ChallengerTrack.PLAN, 11L, 21L),
            row("B디자인", RecruitingApplicationStatus.DOCUMENT_FAILED, ChallengerTrack.DESIGN, 11L, 21L)
        ));

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(summaryQuery(Set.of(10L, 11L), Set.of()));

        // 전체: PLAN 2, DESIGN 1, WEB 2, MOBILE 0
        assertThat(partTotal(result.parts(), ChallengerTrack.PLAN)).isEqualTo(2L);
        assertThat(partTotal(result.parts(), ChallengerTrack.DESIGN)).isEqualTo(1L);
        assertThat(partTotal(result.parts(), ChallengerTrack.WEB_PRODUCT_ENGINEER)).isEqualTo(2L);
        assertThat(partTotal(result.parts(), ChallengerTrack.MOBILE_PRODUCT_ENGINEER)).isZero();
        assertPartSumMatchesTotal(result.parts(), result.totalCount());

        RecruitingSchoolStatusSummaryInfo schoolA = schoolOf(result, 10L);
        assertThat(partTotal(schoolA.parts(), ChallengerTrack.WEB_PRODUCT_ENGINEER)).isEqualTo(2L);
        assertThat(partStatus(schoolA.parts(), ChallengerTrack.WEB_PRODUCT_ENGINEER,
            RecruitingApplicationStatus.SUBMITTED)).isEqualTo(1L);
        assertThat(partStatus(schoolA.parts(), ChallengerTrack.WEB_PRODUCT_ENGINEER,
            RecruitingApplicationStatus.FINAL_PASSED)).isEqualTo(1L);
        assertThat(partTotal(schoolA.parts(), ChallengerTrack.PLAN)).isEqualTo(1L);
        assertThat(partTotal(schoolA.parts(), ChallengerTrack.DESIGN)).isZero();
        assertPartSumMatchesTotal(schoolA.parts(), schoolA.totalCount());

        RecruitingSchoolStatusSummaryInfo schoolB = schoolOf(result, 11L);
        assertThat(partTotal(schoolB.parts(), ChallengerTrack.PLAN)).isEqualTo(1L);
        assertThat(partTotal(schoolB.parts(), ChallengerTrack.DESIGN)).isEqualTo(1L);
        assertThat(partTotal(schoolB.parts(), ChallengerTrack.WEB_PRODUCT_ENGINEER)).isZero();
        assertPartSumMatchesTotal(schoolB.parts(), schoolB.totalCount());

        RecruitingRoundStatusSummaryInfo round20Info = roundOf(schoolA, 20L);
        assertThat(partTotal(round20Info.parts(), ChallengerTrack.WEB_PRODUCT_ENGINEER)).isEqualTo(1L);
        assertThat(partTotal(round20Info.parts(), ChallengerTrack.PLAN)).isEqualTo(1L);
        RecruitingRoundStatusSummaryInfo round22Info = roundOf(schoolA, 22L);
        assertThat(partTotal(round22Info.parts(), ChallengerTrack.WEB_PRODUCT_ENGINEER)).isEqualTo(1L);
        assertThat(partStatus(round22Info.parts(), ChallengerTrack.WEB_PRODUCT_ENGINEER,
            RecruitingApplicationStatus.FINAL_PASSED)).isEqualTo(1L);
        assertThat(partTotal(round22Info.parts(), ChallengerTrack.PLAN)).isZero();
        RecruitingRoundStatusSummaryInfo round21Info = roundOf(schoolB, 21L);
        assertThat(partTotal(round21Info.parts(), ChallengerTrack.PLAN)).isEqualTo(1L);
        assertThat(partStatus(round21Info.parts(), ChallengerTrack.DESIGN,
            RecruitingApplicationStatus.DOCUMENT_FAILED)).isEqualTo(1L);
    }

    private RecruitingSchoolStatusSummaryInfo schoolOf(RecruitingStatusSummaryInfo result, Long schoolId) {
        return result.schools().stream()
            .filter(school -> school.schoolId().equals(schoolId))
            .findFirst()
            .orElseThrow();
    }

    private RecruitingRoundStatusSummaryInfo roundOf(RecruitingSchoolStatusSummaryInfo school, Long roundId) {
        return school.rounds().stream()
            .filter(round -> round.roundId().equals(roundId))
            .findFirst()
            .orElseThrow();
    }

    private long partTotal(List<RecruitingPartStatusSummaryInfo> parts, ChallengerTrack track) {
        return parts.stream()
            .filter(part -> part.part() == track)
            .findFirst()
            .orElseThrow()
            .totalCount();
    }

    private long partStatus(
        List<RecruitingPartStatusSummaryInfo> parts,
        ChallengerTrack track,
        RecruitingApplicationStatus status
    ) {
        return parts.stream()
            .filter(part -> part.part() == track)
            .findFirst()
            .orElseThrow()
            .countByStatus()
            .getOrDefault(status, 0L);
    }

    private void assertPartSumMatchesTotal(List<RecruitingPartStatusSummaryInfo> parts, Long totalCount) {
        assertThat(parts.stream().mapToLong(part -> part.totalCount()).sum()).isEqualTo(totalCount);
    }

    @Test
    @DisplayName("다른 기수의 중앙 총괄단은 상태 요약을 조회할 수 없다")
    void rejectStatusSummaryForCentralCoreFromDifferentGisu() {
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeWithoutAccess());

        assertThatThrownBy(() -> sut.getStatusSummary(summaryQuery(Set.of(10L), Set.of())))
            .isInstanceOf(com.umc.product.recruiting.domain.exception.RecruitingDomainException.class);
        then(loadApplicationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 기수와 무관하게 상태 요약을 조회할 수 있다")
    void superAdminReadsStatusSummaryAcrossGisu() {
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeForAllSchools());

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(summaryQuery(Set.of(10L), Set.of()));

        assertThat(result.totalCount()).isZero();
    }

    @Test
    @DisplayName("상태 요약은 선택한 Round ID를 persistence 조회에 전달한다")
    void filterStatusSummaryByRound() {
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeForAllSchools());
        givenSummaryScope();
        given(loadApplicationPort.searchSummaryRows(1L, Set.of(10L), Set.of(20L), SUMMARY_STATUSES))
            .willReturn(List.of(row("지원자", RecruitingApplicationStatus.SUBMITTED)));

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(summaryQuery(Set.of(10L), Set.of(20L)));

        assertThat(result.totalCount()).isEqualTo(1L);
        then(loadApplicationPort).should().searchSummaryRows(1L, Set.of(10L), Set.of(20L), SUMMARY_STATUSES);
    }

    @Test
    @DisplayName("상태 요약은 학교명으로 검색하고 지원서가 없는 학교와 Round도 0건으로 반환한다")
    void statusSummaryIncludesZeroCountGroupsAfterSchoolNameFilter() {
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeForAllSchools());
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 5L);
        RecruitingRound firstRound = summaryRound(season, 20L, "15기 본모집");
        RecruitingRound emptyRound = summaryRound(season, 21L, "15기 추가모집");
        given(getSchoolUseCase.getSchoolListByGisuId(1L)).willReturn(List.of(
            school(10L, "Alpha 대학교"),
            school(11L, "Beta 대학교")
        ));
        given(loadSeasonPort.listByGisuId(1L)).willReturn(List.of(season));
        given(loadRoundPort.listBySeasonIds(List.of(5L))).willReturn(List.of(firstRound, emptyRound));
        given(loadApplicationPort.searchSummaryRows(1L, Set.of(10L), null, SUMMARY_STATUSES))
            .willReturn(List.of(row("지원자", RecruitingApplicationStatus.SUBMITTED)));

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(RecruitingStatusSummaryQuery.builder()
            .gisuId(1L)
            .schoolName("  alpha  ")
            .requesterMemberId(99L)
            .build());

        assertThat(result.schools()).singleElement().satisfies(foundSchool -> {
            assertThat(foundSchool.schoolName()).isEqualTo("Alpha 대학교");
            assertThat(foundSchool.rounds()).hasSize(2);
            assertThat(foundSchool.rounds())
                .filteredOn(round -> round.roundId().equals(21L))
                .singleElement()
                .satisfies(round -> {
                    assertThat(round.totalCount()).isZero();
                    assertThat(round.countByStatus()).isEmpty();
                });
        });
    }

    @Test
    @DisplayName("상태 요약은 시즌이 없는 선택 학교도 0건 그룹으로 반환한다")
    void statusSummaryIncludesSchoolWithoutSeason() {
        given(getGisuAuthorityScopeUseCase.getByMemberIdAndGisuId(99L, 1L)).willReturn(scopeForAllSchools());
        given(getSchoolUseCase.getSchoolListByGisuId(1L)).willReturn(List.of(school(10L, "빈 학교")));
        given(loadSeasonPort.listByGisuId(1L)).willReturn(List.of());

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(summaryQuery(Set.of(10L), Set.of()));

        assertThat(result.totalCount()).isZero();
        assertThat(result.schools()).singleElement().satisfies(foundSchool -> {
            assertThat(foundSchool.schoolId()).isEqualTo(10L);
            assertThat(foundSchool.totalCount()).isZero();
            assertThat(foundSchool.rounds()).isEmpty();
        });
        then(loadApplicationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원 Form 구조는 선택한 트랙 범위 밖 section으로 이동하는 option을 제외한다")
    void publicFormStructureExcludesConditionalDestinationOutsideSelectedTracks() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        RecruitingRound round = RecruitingRound.createRegular(season, RecruitingRoundConfiguration.of(
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
            null,
            null
        ));
        RecruitingApplicationForm applicationForm = RecruitingApplicationForm.create(round, 500L);
        ReflectionTestUtils.setField(applicationForm, "id", 100L);
        applicationForm.publish(round.getRecruitableTracks());
        given(loadApplicationFormPort.getById(100L)).willReturn(applicationForm);
        given(getQuestionScopeUseCase.getQuestionScope(100L, ChallengerTrack.PLAN, null))
            .willReturn(new RecruitingApplicationQuestionScopeInfo(java.util.Set.of(11L), java.util.Set.of()));
        given(getFormUseCase.getFormWithStructureByQuestionIds(500L, java.util.Set.of(11L)))
            .willReturn(FormWithStructureInfo.builder()
                .formId(500L)
                .sections(List.of(
                    section(1L, 11L, List.of(
                        option(101L, "계속", null),
                        option(102L, "선택하지 않은 트랙", 2L)
                    )),
                    section(2L, 22L, List.of())
                ))
                .build());

        FormWithStructureInfo result = sut.getPublicFormStructure(100L, ChallengerTrack.PLAN, null);

        assertThat(result.sections()).singleElement().satisfies(section ->
            assertThat(section.questions().getFirst().options())
                .extracting(FormWithStructureInfo.Option::optionId)
                .containsExactly(101L)
        );
    }

    private FormWithStructureInfo.SectionWithQuestions section(
        Long sectionId,
        Long questionId,
        List<FormWithStructureInfo.Option> options
    ) {
        return FormWithStructureInfo.SectionWithQuestions.builder()
            .sectionId(sectionId)
            .title("section-" + sectionId)
            .orderNo(sectionId)
            .questions(questionId == 22L ? List.of() : List.of(FormWithStructureInfo.QuestionWithOptions.builder()
                .questionId(questionId)
                .title("question")
                .type(QuestionType.RADIO)
                .orderNo(1L)
                .options(options)
                .build()))
            .build();
    }

    private FormWithStructureInfo.Option option(Long optionId, String content, Long nextSectionId) {
        return FormWithStructureInfo.Option.builder()
            .optionId(optionId)
            .content(content)
            .orderNo(optionId)
            .nextSectionId(nextSectionId)
            .build();
    }

    private RecruitingApplicationSummaryRow row(String applicantName, RecruitingApplicationStatus status) {
        return row(applicantName, status, ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    private RecruitingApplicationSummaryRow row(
        String applicantName,
        RecruitingApplicationStatus status,
        ChallengerTrack firstChoice
    ) {
        return row(applicantName, status, firstChoice, 10L, 20L);
    }

    private RecruitingApplicationSummaryRow row(
        String applicantName,
        RecruitingApplicationStatus status,
        ChallengerTrack firstChoice,
        Long schoolId,
        Long roundId
    ) {
        return new RecruitingApplicationSummaryRow(
            1L,
            1L,
            schoolId,
            roundId,
            "15기 본모집",
            RecruitingRoundType.REGULAR,
            1,
            100L,
            500L,
            900L,
            applicantName,
            "masked-source@umc.test",
            firstChoice,
            null,
            null,
            status,
            RecruitingApplicationRegistrationStatus.NOT_READY,
            Instant.parse("2026-07-02T01:00:00Z")
        );
    }

    private void givenSummaryScope() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 5L);
        RecruitingRound round = summaryRound(season, 20L, "15기 본모집");
        given(getSchoolUseCase.getSchoolListByGisuId(1L)).willReturn(List.of(school(10L, "테스트대학교")));
        given(loadSeasonPort.listByGisuId(1L)).willReturn(List.of(season));
        given(loadRoundPort.listBySeasonIds(List.of(5L))).willReturn(List.of(round));
    }

    private RecruitingRound summaryRound(RecruitingSeason season, Long roundId, String title) {
        RecruitingRound round = RecruitingRound.createRegular(season, title, RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER),
            false,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null
        ));
        ReflectionTestUtils.setField(round, "id", roundId);
        return round;
    }

    private SchoolDetailInfo school(Long schoolId, String schoolName) {
        return new SchoolDetailInfo(
            3L, "중앙", schoolName, null, schoolId, null, null, List.of(), true, null, null
        );
    }

    private RecruitingStatusSummaryQuery summaryQuery(Set<Long> schoolIds, Set<Long> roundIds) {
        return RecruitingStatusSummaryQuery.builder()
            .gisuId(1L)
            .schoolIds(schoolIds)
            .roundIds(roundIds)
            .requesterMemberId(99L)
            .build();
    }

    private GisuAuthorityScopeInfo scopeForAllSchools() {
        return new GisuAuthorityScopeInfo(true, Set.of(), Set.of());
    }

    private GisuAuthorityScopeInfo scopeWithoutAccess() {
        return new GisuAuthorityScopeInfo(false, Set.of(), Set.of());
    }
}
