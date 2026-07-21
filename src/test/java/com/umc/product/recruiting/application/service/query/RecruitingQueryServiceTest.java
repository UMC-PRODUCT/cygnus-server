package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationQuestionScopeInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
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
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Mock
    GetFormUseCase getFormUseCase;

    @Mock
    GetRecruitingApplicationQuestionScopeUseCase getQuestionScopeUseCase;

    @InjectMocks
    RecruitingQueryService sut;

    @Test
    @DisplayName("상태_요약은_summary_row의_지원서_상태를_집계한다")
    void summarizeApplicationStatuses() {
        // Given
        given(getChallengerRoleUseCase.isCentralCoreInGisu(99L, 1L)).willReturn(true);
        given(loadApplicationPort.searchSummaryRows(1L, 10L, null, null)).willReturn(List.of(
            row("지원자1", RecruitingApplicationStatus.SUBMITTED),
            row("지원자2", RecruitingApplicationStatus.SUBMITTED),
            row("지원자3", RecruitingApplicationStatus.FINAL_PASSED)
        ));

        // When
        RecruitingStatusSummaryInfo result = sut.getStatusSummary(1L, 10L, 99L);

        // Then
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.countByStatus().get(RecruitingApplicationStatus.SUBMITTED)).isEqualTo(2L);
        assertThat(result.countByStatus().get(RecruitingApplicationStatus.FINAL_PASSED)).isEqualTo(1L);
        assertThat(result.rounds()).singleElement().satisfies(round -> {
            assertThat(round.roundId()).isEqualTo(20L);
            assertThat(round.totalCount()).isEqualTo(3L);
        });
    }

    @Test
    @DisplayName("다른 기수의 중앙 총괄단은 상태 요약을 조회할 수 없다")
    void rejectStatusSummaryForCentralCoreFromDifferentGisu() {
        given(getChallengerRoleUseCase.isCentralCoreInGisu(99L, 1L)).willReturn(false);
        given(getChallengerRoleUseCase.isSuperAdmin(99L)).willReturn(false);

        assertThatThrownBy(() -> sut.getStatusSummary(1L, 10L, 99L))
            .isInstanceOf(com.umc.product.recruiting.domain.exception.RecruitingDomainException.class);
        then(loadApplicationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 기수와 무관하게 상태 요약을 조회할 수 있다")
    void superAdminReadsStatusSummaryAcrossGisu() {
        given(getChallengerRoleUseCase.isCentralCoreInGisu(99L, 1L)).willReturn(false);
        given(getChallengerRoleUseCase.isSuperAdmin(99L)).willReturn(true);
        given(loadApplicationPort.searchSummaryRows(1L, 10L, null, null)).willReturn(List.of());

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(1L, 10L, 99L);

        assertThat(result.totalCount()).isZero();
    }

    @Test
    @DisplayName("상태 요약은 선택한 Round ID를 persistence 조회에 전달한다")
    void filterStatusSummaryByRound() {
        given(getChallengerRoleUseCase.isCentralCoreInGisu(99L, 1L)).willReturn(true);
        given(loadApplicationPort.searchSummaryRows(1L, 10L, 20L, null))
            .willReturn(List.of(row("지원자", RecruitingApplicationStatus.SUBMITTED)));

        RecruitingStatusSummaryInfo result = sut.getStatusSummary(1L, 10L, 20L, 99L);

        assertThat(result.totalCount()).isEqualTo(1L);
        then(loadApplicationPort).should().searchSummaryRows(1L, 10L, 20L, null);
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
        return new RecruitingApplicationSummaryRow(
            1L,
            1L,
            10L,
            20L,
            RecruitingRoundType.REGULAR,
            1,
            100L,
            500L,
            900L,
            applicantName,
            "masked-source@umc.test",
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            null,
            null,
            status,
            RecruitingApplicationRegistrationStatus.NOT_READY,
            Instant.parse("2026-07-02T01:00:00Z")
        );
    }
}
