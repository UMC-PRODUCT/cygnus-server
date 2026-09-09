package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.command.PrepareRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingDecisionHistoryCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingEvaluationStatisticsUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleBoardUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingDecisionHistoryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingAdminFormStructureInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingChapterEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryPageInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistorySearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPartStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingTrackEvaluationCountInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionHistorySortOrder;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationProgressStatus;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

@WebMvcTest(controllers = {RecruitingAdminController.class, RecruitingAdminInterviewController.class})
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RecruitingAdminController")
class RecruitingAdminControllerTest {

    private static final Long MEMBER_ID = 99L;
    private static final Long SEASON_ID = 10L;
    private static final Long ROUND_ID = 20L;
    private static final Long APPLICATION_FORM_ID = 30L;
    private static final Long APPLICATION_ID = 40L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    PublishRecruitingApplicationFormUseCase publishFormUseCase;
    @MockitoBean
    CloseRecruitingApplicationFormUseCase closeFormUseCase;
    @MockitoBean
    DecideRecruitingFinalUseCase decideFinalUseCase;
    @MockitoBean
    DecideRecruitingDocumentUseCase decideDocumentUseCase;
    @MockitoBean
    SkipRecruitingInterviewUseCase skipInterviewUseCase;
    @MockitoBean
    PrepareRecruitingRegistrationUseCase prepareRegistrationUseCase;
    @MockitoBean
    CancelRecruitingRegistrationUseCase cancelRegistrationUseCase;
    @MockitoBean
    ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;
    @MockitoBean
    ManageRecruitingInterviewScheduleUseCase manageScheduleUseCase;
    @MockitoBean
    ManageRecruitingInterviewSessionUseCase manageInterviewSessionUseCase;
    @MockitoBean
    ConfirmRecruitingInterviewSchedulesUseCase confirmInterviewSchedulesUseCase;
    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    @MockitoBean
    ExportRecruitingCsvUseCase exportRecruitingCsvUseCase;
    @MockitoBean
    GetRecruitingEvaluationStatisticsUseCase getEvaluationStatisticsUseCase;
    @MockitoBean
    GetRecruitingInterviewSessionUseCase getInterviewSessionUseCase;
    @MockitoBean
    GetRecruitingInterviewScheduleBoardUseCase getInterviewScheduleBoardUseCase;
    @MockitoBean
    SearchRecruitingDecisionHistoryUseCase searchDecisionHistoryUseCase;
    @MockitoBean
    ExportRecruitingDecisionHistoryCsvUseCase exportDecisionHistoryCsvUseCase;
    @MockitoBean
    UpsertRecruitingApplicationFormUseCase upsertFormUseCase;
    @MockitoBean
    GetRecruitingFormQueryUseCase getRecruitingFormQueryUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("모집 폼 upsert API는 전체 구조와 요청자를 전달한다")
    void 모집_폼_upsert_API는_전체_구조와_요청자를_전달한다() throws Exception {
        given(upsertFormUseCase.upsert(any())).willReturn(APPLICATION_FORM_ID);

        mockMvc.perform(put("/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}/form", SEASON_ID, ROUND_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "지원서",
                      "sections": [{
                        "clientKey": "common",
                        "title": "공통",
                        "type": "COMMON",
                        "questions": []
                      }]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(APPLICATION_FORM_ID));

        ArgumentCaptor<UpsertRecruitingApplicationFormCommand> captor =
            ArgumentCaptor.forClass(UpsertRecruitingApplicationFormCommand.class);
        then(upsertFormUseCase).should().upsert(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(SEASON_ID);
        assertThat(captor.getValue().roundId()).isEqualTo(ROUND_ID);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("모집 폼 조회 API는 section 정책과 question 구조를 함께 반환한다")
    void 모집_폼_조회_API는_section_정책과_question_구조를_함께_반환한다() throws Exception {
        given(getRecruitingFormQueryUseCase.getAdminFormStructure(SEASON_ID, ROUND_ID))
            .willReturn(RecruitingAdminFormStructureInfo.builder()
                .exists(true)
                .applicationFormId(APPLICATION_FORM_ID)
                .formId(100L)
                .title("15기 본모집")
                .description("지원서")
                .status(RecruitingApplicationFormStatus.DRAFT)
                .sections(List.of(RecruitingAdminFormStructureInfo.SectionInfo.builder()
                    .sectionId(300L)
                    .clientKey("section-300")
                    .title("PLAN 파트")
                    .orderNo(1L)
                    .type(RecruitingFormSectionType.TRACK)
                    .track(ChallengerTrack.PLAN)
                    .questions(List.of(RecruitingAdminFormStructureInfo.QuestionInfo.builder()
                        .questionId(400L)
                        .title("지원 동기")
                        .type(QuestionType.LONG_TEXT)
                        .required(true)
                        .orderNo(1L)
                        .options(List.of(RecruitingAdminFormStructureInfo.OptionInfo.builder()
                            .optionId(500L)
                            .content("다음")
                            .orderNo(1L)
                            .nextSectionId(300L)
                            .nextSectionKey("section-300")
                            .build()))
                        .build()))
                    .build()))
                .build());

        mockMvc.perform(get("/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}/form", SEASON_ID, ROUND_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.exists").value(true))
            .andExpect(jsonPath("$.result.applicationFormId").value(APPLICATION_FORM_ID))
            .andExpect(jsonPath("$.result.status").value("DRAFT"))
            .andExpect(jsonPath("$.result.sections[0].type").value("TRACK"))
            .andExpect(jsonPath("$.result.sections[0].track").value("PLAN"))
            .andExpect(jsonPath("$.result.sections[0].clientKey").value("section-300"))
            .andExpect(jsonPath("$.result.sections[0].questions[0].required").value(true))
            .andExpect(jsonPath("$.result.sections[0].questions[0].orderNo").value(1))
            .andExpect(jsonPath("$.result.sections[0].questions[0].options[0].nextSectionId").value(300))
            .andExpect(jsonPath("$.result.sections[0].questions[0].options[0].nextSectionKey").value("section-300"));
    }

    @Test
    @DisplayName("모집 폼 조회 API는 Form을 만들지 않은 차수에 빈 구조를 반환한다")
    void 모집_폼_조회_API는_Form을_만들지_않은_차수에_빈_구조를_반환한다() throws Exception {
        given(getRecruitingFormQueryUseCase.getAdminFormStructure(SEASON_ID, ROUND_ID))
            .willReturn(RecruitingAdminFormStructureInfo.empty());

        mockMvc.perform(get("/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}/form", SEASON_ID, ROUND_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.exists").value(false))
            .andExpect(jsonPath("$.result.applicationFormId").doesNotExist())
            .andExpect(jsonPath("$.result.sections").isEmpty());
    }

    @Test
    @DisplayName("별도 모집 폼 게시 API는 제거한다")
    void publishFormEndpointIsRemoved() throws Exception {
        mockMvc.perform(post(
                "/api/v1/recruiting/admin/seasons/{seasonId}/forms/{applicationFormId}/publish",
                SEASON_ID,
                APPLICATION_FORM_ID
            ))
            .andExpect(status().isNotFound());
        then(publishFormUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("별도 모집 폼 마감 API는 제거한다")
    void closeFormEndpointIsRemoved() throws Exception {
        mockMvc.perform(post(
                "/api/v1/recruiting/admin/seasons/{seasonId}/forms/{applicationFormId}/close",
                SEASON_ID,
                APPLICATION_FORM_ID
            ))
            .andExpect(status().isNotFound());
        then(closeFormUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("최종 결정 API는 결정자 memberId를 command로 전달한다")
    void 최종_결정_API는_결정자_memberId를_command로_전달한다() throws Exception {
        mockMvc.perform(patch(
                    "/api/v1/recruiting/admin/applications/{applicationId}/final-decision",
                    APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"PASS\",\"acceptedTrack\":\"PLAN\",\"reason\":\"적합\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<DecideRecruitingFinalCommand> captor =
            ArgumentCaptor.forClass(DecideRecruitingFinalCommand.class);
        then(decideFinalUseCase).should().decideFinal(captor.capture());
        assertThat(captor.getValue().decision()).isEqualTo(com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus.PASS);
        assertThat(captor.getValue().acceptedTrack()).isEqualTo(com.umc.product.common.domain.enums.ChallengerTrack.PLAN);
        assertThat(captor.getValue().decidedByMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("서류 결정 API는 acceptedTrack 없이 결정자와 합불을 전달한다")
    void 서류_결정_API는_결정자와_합불을_전달한다() throws Exception {
        mockMvc.perform(patch(
                    "/api/v1/recruiting/admin/applications/{applicationId}/document-decision",
                    APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"PASS\",\"reason\":\"적합\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<DecideRecruitingDocumentCommand> captor =
            ArgumentCaptor.forClass(DecideRecruitingDocumentCommand.class);
        then(decideDocumentUseCase).should().decideDocument(captor.capture());
        assertThat(captor.getValue().decision())
            .isEqualTo(com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus.PASS);
        assertThat(captor.getValue().decidedByMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("서류 결정 API는 DB 제한을 넘는 사유를 거부한다")
    void 서류_결정_API는_긴_사유를_거부한다() throws Exception {
        mockMvc.perform(patch(
                    "/api/v1/recruiting/admin/applications/{applicationId}/document-decision",
                    APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"PASS\",\"reason\":\"%s\"}".formatted("a".repeat(256))))
            .andExpect(status().isBadRequest());

        then(decideDocumentUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 생략 API는 CurrentMember와 사유를 전달한다")
    void 면접_생략_API는_CurrentMember와_사유를_전달한다() throws Exception {
        mockMvc.perform(post(
                    "/api/v1/recruiting/admin/applications/{applicationId}/interview/skip",
                    APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"학교 정책상 면접 없음\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<SkipRecruitingInterviewCommand> captor =
            ArgumentCaptor.forClass(SkipRecruitingInterviewCommand.class);
        then(skipInterviewUseCase).should().skip(captor.capture());
        assertThat(captor.getValue().skippedByMemberId()).isEqualTo(MEMBER_ID);
        assertThat(captor.getValue().reason()).isEqualTo("학교 정책상 면접 없음");
    }

    @Test
    @DisplayName("면접 생략 API는 DB 제한을 넘는 사유를 거부한다")
    void 면접_생략_API는_긴_사유를_거부한다() throws Exception {
        mockMvc.perform(post(
                    "/api/v1/recruiting/admin/applications/{applicationId}/interview/skip",
                    APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"%s\"}".formatted("a".repeat(256))))
            .andExpect(status().isBadRequest());

        then(skipInterviewUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("READY API는 CurrentMember actor를 use case로 전달한다")
    void readyUsesCurrentMemberActor() throws Exception {
        mockMvc.perform(post("/api/v1/recruiting/admin/applications/{applicationId}/registration/ready", APPLICATION_ID))
            .andExpect(status().isOk());

        ArgumentCaptor<PrepareRecruitingRegistrationCommand> captor =
            ArgumentCaptor.forClass(PrepareRecruitingRegistrationCommand.class);
        then(prepareRegistrationUseCase).should().prepareRegistration(captor.capture());
        assertThat(captor.getValue().executorMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("CSV export API는 raw email과 지원서 본문 없이 attachment를 반환한다")
    void CSV_export_API는_raw_email과_지원서_본문_없이_attachment를_반환한다() throws Exception {
        given(exportRecruitingCsvUseCase.exportSummaryCsv(11L, 22L, MEMBER_ID))
            .willReturn("""
                gisuId,schoolId,roundType,roundNo,applicationId,maskedEmail,firstChoiceTrack,secondChoiceTrack,acceptedTrack,status,registrationStatus,submittedAt
                11,22,REGULAR,1,40,app****@example.org,WEB_PRODUCT_ENGINEER,,,SUBMITTED,NOT_READY,2026-07-01T00:00:00Z
                """.getBytes());

        mockMvc.perform(get("/api/v1/recruiting/admin/statistics.csv")
                .param("gisuId", "11")
                .param("schoolId", "22"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recruiting-statistics.csv\""))
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                assertThat(body.lines().findFirst()).contains(
                    "gisuId,schoolId,roundType,roundNo,applicationId,maskedEmail,firstChoiceTrack,"
                        + "secondChoiceTrack,acceptedTrack,status,registrationStatus,submittedAt"
                );
                assertThat(body).doesNotContain("applicantEmail");
                assertThat(body).doesNotContain("applicantName");
                assertThat(body).doesNotContain("applicationKey");
                assertThat(body).doesNotContain("answer");
            });
    }

    @Test
    @DisplayName("상태 요약 API는 status별 count를 반환한다")
    void 상태_요약_API는_status별_count를_반환한다() throws Exception {
        Map<RecruitingApplicationStatus, Long> counts = new EnumMap<>(RecruitingApplicationStatus.class);
        counts.put(RecruitingApplicationStatus.SUBMITTED, 3L);
        List<RecruitingPartStatusSummaryInfo> parts = List.of(
            new RecruitingPartStatusSummaryInfo(ChallengerTrack.PLAN, 3L, counts)
        );
        given(getApplicationQueryUseCase.getStatusSummary(any()))
            .willReturn(new RecruitingStatusSummaryInfo(3L, counts, parts, List.of(
                new RecruitingSchoolStatusSummaryInfo(22L, "테스트대학교", 7L, "중앙", 3L, counts, parts, List.of())
            )));

        mockMvc.perform(get("/api/v1/recruiting/admin/summary")
                .param("gisuId", "11")
                .param("schoolIds", "22", "23")
                .param("roundIds", "31", "32")
                .param("schoolName", "테스트"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.totalCount").value(3L))
            .andExpect(jsonPath("$.result.countByStatus.SUBMITTED").value(3L))
            .andExpect(jsonPath("$.result.parts[0].part").value("PLAN"))
            .andExpect(jsonPath("$.result.parts[0].totalCount").value(3L))
            .andExpect(jsonPath("$.result.parts[0].countByStatus.SUBMITTED").value(3L))
            .andExpect(jsonPath("$.result.schools[0].schoolName").value("테스트대학교"))
            .andExpect(jsonPath("$.result.schools[0].parts[0].part").value("PLAN"));

        ArgumentCaptor<RecruitingStatusSummaryQuery> captor =
            ArgumentCaptor.forClass(RecruitingStatusSummaryQuery.class);
        then(getApplicationQueryUseCase).should().getStatusSummary(captor.capture());
        assertThat(captor.getValue().schoolIds()).isEqualTo(Set.of(22L, 23L));
        assertThat(captor.getValue().roundIds()).isEqualTo(Set.of(31L, 32L));
        assertThat(captor.getValue().schoolName()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("평가 현황 집계 API는 지부·학교·파트별 카운트를 반환한다")
    void 평가_현황_집계_API는_지부_학교_파트별_카운트를_반환한다() throws Exception {
        List<RecruitingTrackEvaluationCountInfo> byTrack = List.of(
            new RecruitingTrackEvaluationCountInfo(ChallengerTrack.PLAN, 5L, 2L)
        );
        given(getEvaluationStatisticsUseCase.getEvaluationStatistics(any()))
            .willReturn(new RecruitingEvaluationStatisticsInfo(
                Instant.parse("2026-07-04T02:48:00Z"), 5L, 2L, byTrack,
                List.of(new RecruitingChapterEvaluationStatisticsInfo(7L, "중앙", 5L, 2L, byTrack,
                    List.of(new RecruitingSchoolEvaluationStatisticsInfo(22L, "테스트대학교", 5L, 2L, byTrack))
                ))
            ));

        mockMvc.perform(get("/api/v1/recruiting/admin/statistics/evaluations")
                .param("gisuId", "11"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.applicantCount").value(5L))
            .andExpect(jsonPath("$.result.evaluatedCount").value(2L))
            .andExpect(jsonPath("$.result.byTrack[0].track").value("PLAN"))
            .andExpect(jsonPath("$.result.chapters[0].chapterName").value("중앙"))
            .andExpect(jsonPath("$.result.chapters[0].schools[0].schoolName").value("테스트대학교"));

        ArgumentCaptor<RecruitingEvaluationStatisticsQuery> captor =
            ArgumentCaptor.forClass(RecruitingEvaluationStatisticsQuery.class);
        then(getEvaluationStatisticsUseCase).should().getEvaluationStatistics(captor.capture());
        assertThat(captor.getValue().gisuId()).isEqualTo(11L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("평가 이력 조회 API는 반복 지부·학교 필터와 정렬·페이징 조건을 query로 전달한다")
    void 평가_이력_조회_API는_조건을_query로_전달한다() throws Exception {
        given(searchDecisionHistoryUseCase.search(any())).willReturn(decisionHistoryPage());

        mockMvc.perform(get("/api/v1/recruiting/admin/decision-histories")
                .param("gisuId", "11")
                .param("chapterIds", "5", "6")
                .param("schoolIds", "22", "23")
                .param("tracks", "WEB_PRODUCT_ENGINEER")
                .param("results", "PASSED")
                .param("searchName", "박유엠")
                .param("sort", "OLDEST")
                .param("groupByDecider", "true")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.progressStatus").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.result.histories.totalElements").value(1L))
            .andExpect(jsonPath("$.result.histories.content[0].applicant.name").value("박유엠"))
            .andExpect(jsonPath("$.result.histories.content[0].decider.roleType").value("SCHOOL_PRESIDENT"));

        ArgumentCaptor<RecruitingDecisionHistorySearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingDecisionHistorySearchQuery.class);
        then(searchDecisionHistoryUseCase).should().search(captor.capture());
        RecruitingDecisionHistorySearchQuery query = captor.getValue();
        assertThat(query.gisuId()).isEqualTo(11L);
        assertThat(query.chapterIds()).containsExactlyInAnyOrder(5L, 6L);
        assertThat(query.schoolIds()).containsExactlyInAnyOrder(22L, 23L);
        assertThat(query.results()).containsExactly(RecruitingDecisionResult.PASSED);
        assertThat(query.searchName()).isEqualTo("박유엠");
        assertThat(query.sortOrder()).isEqualTo(RecruitingDecisionHistorySortOrder.OLDEST);
        assertThat(query.groupByDecider()).isTrue();
        assertThat(query.requesterMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("평가 이력 CSV API는 반복 지부·학교 필터와 함께 attachment를 반환한다")
    void 평가_이력_CSV_API는_attachment를_반환한다() throws Exception {
        given(exportDecisionHistoryCsvUseCase.exportCsv(any()))
            .willReturn("decidedAt,decisionStatus,result\n".getBytes());

        mockMvc.perform(get("/api/v1/recruiting/admin/decision-histories.csv")
                .param("gisuId", "11")
                .param("chapterIds", "5", "6")
                .param("schoolIds", "22", "23"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"recruiting-decision-histories.csv\""
            ));

        ArgumentCaptor<RecruitingDecisionHistorySearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingDecisionHistorySearchQuery.class);
        then(exportDecisionHistoryCsvUseCase).should().exportCsv(captor.capture());
        assertThat(captor.getValue().chapterIds()).containsExactlyInAnyOrder(5L, 6L);
        assertThat(captor.getValue().schoolIds()).containsExactlyInAnyOrder(22L, 23L);
    }

    @Test
    @DisplayName("평가 이력 조회 API는 빈 chapterIds 반복 값을 거부하고 UseCase를 호출하지 않는다")
    void 평가_이력_조회_API는_빈_chapterIds_반복_값을_거부한다() throws Exception {
        mockMvc.perform(get("/api/v1/recruiting/admin/decision-histories")
                .param("gisuId", "11")
                .param("chapterIds", ""))
            .andExpect(status().isBadRequest());

        then(searchDecisionHistoryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("평가 이력 조회 API는 빈 schoolIds 반복 값을 거부하고 UseCase를 호출하지 않는다")
    void 평가_이력_조회_API는_빈_schoolIds_반복_값을_거부한다() throws Exception {
        mockMvc.perform(get("/api/v1/recruiting/admin/decision-histories")
                .param("gisuId", "11")
                .param("schoolIds", ""))
            .andExpect(status().isBadRequest());

        then(searchDecisionHistoryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("평가 이력 CSV API는 빈 chapterIds 반복 값을 거부하고 UseCase를 호출하지 않는다")
    void 평가_이력_CSV_API는_빈_chapterIds_반복_값을_거부한다() throws Exception {
        mockMvc.perform(get("/api/v1/recruiting/admin/decision-histories.csv")
                .param("gisuId", "11")
                .param("chapterIds", ""))
            .andExpect(status().isBadRequest());

        then(exportDecisionHistoryCsvUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("평가 이력 CSV API는 빈 schoolIds 반복 값을 거부하고 UseCase를 호출하지 않는다")
    void 평가_이력_CSV_API는_빈_schoolIds_반복_값을_거부한다() throws Exception {
        mockMvc.perform(get("/api/v1/recruiting/admin/decision-histories.csv")
                .param("gisuId", "11")
                .param("schoolIds", ""))
            .andExpect(status().isBadRequest());

        then(exportDecisionHistoryCsvUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    @DisplayName("평가 이력 조회 API는 0 또는 음수 반복 ID를 거부하고 UseCase를 호출하지 않는다")
    void 평가_이력_조회_API는_유효하지_않은_반복_ID를_거부한다(String invalidId) throws Exception {
        mockMvc.perform(get("/api/v1/recruiting/admin/decision-histories")
                .param("gisuId", "11")
                .param("chapterIds", invalidId)
                .param("schoolIds", invalidId))
            .andExpect(status().isBadRequest());

        then(searchDecisionHistoryUseCase).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    @DisplayName("평가 이력 CSV API는 0 또는 음수 반복 ID를 거부하고 UseCase를 호출하지 않는다")
    void 평가_이력_CSV_API는_유효하지_않은_반복_ID를_거부한다(String invalidId) throws Exception {
        mockMvc.perform(get("/api/v1/recruiting/admin/decision-histories.csv")
                .param("gisuId", "11")
                .param("chapterIds", invalidId)
                .param("schoolIds", invalidId))
            .andExpect(status().isBadRequest());

        then(exportDecisionHistoryCsvUseCase).shouldHaveNoInteractions();
    }

    private RecruitingDecisionHistoryPageInfo decisionHistoryPage() {
        RecruitingDecisionHistoryInfo history = RecruitingDecisionHistoryInfo.builder()
            .decisionHistoryId(1L)
            .applicationId(900L)
            .decidedAt(java.time.Instant.parse("2026-07-04T02:48:00Z"))
            .decisionStatus(RecruitingApplicationStatus.FINAL_PASSED)
            .result(RecruitingDecisionResult.PASSED)
            .applicant(RecruitingDecisionHistoryInfo.ApplicantInfo.builder()
                .chapterId(5L)
                .chapterName("Selenium")
                .schoolId(22L)
                .schoolName("한양대 ERICA")
                .name("박유엠")
                .firstChoice(com.umc.product.common.domain.enums.ChallengerTrack.WEB_PRODUCT_ENGINEER)
                .build())
            .decider(RecruitingDecisionHistoryInfo.DeciderInfo.builder()
                .memberId(70L)
                .chapterId(5L)
                .chapterName("Selenium")
                .schoolId(22L)
                .schoolName("한양대 ERICA")
                .roleType(com.umc.product.common.domain.enums.ChallengerRoleType.SCHOOL_PRESIDENT)
                .name("이예원")
                .nickname("이방토")
                .build())
            .build();
        return new RecruitingDecisionHistoryPageInfo(
            java.time.Instant.parse("2026-07-04T02:48:00Z"),
            RecruitingEvaluationProgressStatus.IN_PROGRESS,
            new org.springframework.data.domain.PageImpl<>(
                List.of(history),
                org.springframework.data.domain.PageRequest.of(0, 20),
                1L
            )
        );
    }
}
