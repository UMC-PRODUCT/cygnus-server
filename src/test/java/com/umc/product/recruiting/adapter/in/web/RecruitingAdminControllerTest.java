package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
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
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.AssignRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.FindRecruitingInterviewScheduleCandidatesUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.SaveRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.SendRecruitingInterviewGuideUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewEvaluationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;
import com.umc.product.support.RestDocsConfig;

@WebMvcTest(controllers = {RecruitingAdminController.class, RecruitingAdminInterviewController.class})
@Import({JacksonConfig.class, RestDocsConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
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
    CreateRecruitingSeasonUseCase createSeasonUseCase;
    @MockitoBean
    UpdateRecruitingSeasonStatusUseCase updateSeasonStatusUseCase;
    @MockitoBean
    CreateRecruitingRoundUseCase createRoundUseCase;
    @MockitoBean
    UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    @MockitoBean
    LinkRecruitingApplicationFormUseCase linkFormUseCase;
    @MockitoBean
    PublishRecruitingApplicationFormUseCase publishFormUseCase;
    @MockitoBean
    CloseRecruitingApplicationFormUseCase closeFormUseCase;
    @MockitoBean
    DecideRecruitingDocumentUseCase decideDocumentUseCase;
    @MockitoBean
    DecideRecruitingFinalUseCase decideFinalUseCase;
    @MockitoBean
    ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;
    @MockitoBean
    AssignRecruitingInterviewUseCase assignInterviewUseCase;
    @MockitoBean
    SkipRecruitingInterviewUseCase skipInterviewUseCase;
    @MockitoBean
    FindRecruitingInterviewScheduleCandidatesUseCase findScheduleCandidatesUseCase;
    @MockitoBean
    SendRecruitingInterviewGuideUseCase sendInterviewGuideUseCase;
    @MockitoBean
    SaveRecruitingInterviewEvaluationUseCase saveEvaluationUseCase;
    @MockitoBean
    SubmitRecruitingInterviewEvaluationUseCase submitEvaluationUseCase;
    @MockitoBean
    GetRecruitingInterviewEvaluationUseCase getEvaluationUseCase;
    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    @MockitoBean
    ExportRecruitingCsvUseCase exportRecruitingCsvUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("모집 시즌 생성 API는 id를 반환한다")
    void 모집_시즌_생성_API는_id를_반환한다() throws Exception {
        given(createSeasonUseCase.createSeason(any())).willReturn(SEASON_ID);

        mockMvc.perform(post("/api/v1/admin/recruiting/seasons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gisuId\":11,\"schoolId\":22}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(SEASON_ID));
    }

    @Test
    @DisplayName("모집 폼 연결 API는 track 기반 command를 전달한다")
    void 모집_폼_연결_API는_track_기반_command를_전달한다() throws Exception {
        given(linkFormUseCase.link(any())).willReturn(APPLICATION_FORM_ID);

        mockMvc.perform(post("/api/v1/admin/recruiting/seasons/{seasonId}/rounds/{roundId}/forms", SEASON_ID, ROUND_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formId\":300,\"track\":\"WEB_PRODUCT_ENGINEER\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(APPLICATION_FORM_ID));

        ArgumentCaptor<LinkRecruitingApplicationFormCommand> captor =
            ArgumentCaptor.forClass(LinkRecruitingApplicationFormCommand.class);
        then(linkFormUseCase).should().link(captor.capture());
        assertThat(captor.getValue().roundId()).isEqualTo(ROUND_ID);
        assertThat(captor.getValue().track()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("서류 결정 API는 결정자 memberId를 command로 전달한다")
    void 서류_결정_API는_결정자_memberId를_command로_전달한다() throws Exception {
        mockMvc.perform(patch(
                    "/api/v1/admin/recruiting/seasons/{seasonId}/applications/{applicationId}/document-decision",
                    SEASON_ID, APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"PASS\",\"reason\":\"충분한 역량\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<DecideRecruitingDocumentCommand> captor =
            ArgumentCaptor.forClass(DecideRecruitingDocumentCommand.class);
        then(decideDocumentUseCase).should().decideDocument(captor.capture());
        assertThat(captor.getValue().applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(captor.getValue().decision()).isEqualTo(com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus.PASS);
        assertThat(captor.getValue().decidedByMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("최종 결정 API는 결정자 memberId를 command로 전달한다")
    void 최종_결정_API는_결정자_memberId를_command로_전달한다() throws Exception {
        mockMvc.perform(patch(
                    "/api/v1/admin/recruiting/seasons/{seasonId}/applications/{applicationId}/final-decision",
                    SEASON_ID, APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"FAIL\",\"reason\":\"정원 초과\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<DecideRecruitingFinalCommand> captor =
            ArgumentCaptor.forClass(DecideRecruitingFinalCommand.class);
        then(decideFinalUseCase).should().decideFinal(captor.capture());
        assertThat(captor.getValue().decision()).isEqualTo(com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus.FAIL);
        assertThat(captor.getValue().decidedByMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("면접 일정 후보 API는 survey overlap 결과를 반환한다")
    void 면접_일정_후보_API는_survey_overlap_결과를_반환한다() throws Exception {
        given(findScheduleCandidatesUseCase.findScheduleCandidates(any()))
            .willReturn(List.of(new RecruitingInterviewScheduleCandidate(
                Instant.parse("2026-07-01T01:00:00Z"),
                Instant.parse("2026-07-01T02:00:00Z"),
                3
            )));

        mockMvc.perform(post("/api/v1/admin/recruiting/seasons/{seasonId}/interviews/schedule-candidates", SEASON_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formId\":300,\"formResponseIds\":[1,2,3]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].availableApplicantCount").value(3));
    }

    @Test
    @DisplayName("평가 조회 API는 제출 전 visibility 정책 결과만 반환한다")
    void 평가_조회_API는_제출_전_visibility_정책_결과만_반환한다() throws Exception {
        given(getEvaluationUseCase.listVisibleEvaluations(APPLICATION_ID, MEMBER_ID))
            .willReturn(List.of(new RecruitingInterviewEvaluationInfo(
                77L,
                MEMBER_ID,
                RecruitingInterviewEvaluationStatus.DRAFT,
                4,
                "좋음",
                null
            )));

        mockMvc.perform(get(
                    "/api/v1/admin/recruiting/seasons/{seasonId}/applications/{applicationId}/interview-evaluations",
                    SEASON_ID, APPLICATION_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].evaluationId").value(77L))
            .andExpect(jsonPath("$.result[0].evaluatorMemberId").value(MEMBER_ID));
    }

    @Test
    @DisplayName("CSV export API는 raw email과 지원서 본문 없이 attachment를 반환한다")
    void CSV_export_API는_raw_email과_지원서_본문_없이_attachment를_반환한다() throws Exception {
        given(exportRecruitingCsvUseCase.exportSummaryCsv(11L, 22L))
            .willReturn("""
                gisuId,schoolId,roundType,roundNo,formId,track,applicationNo,maskedEmail,applicationStatus,registrationStatus,submittedAt
                11,22,REGULAR,1,300,WEB_PRODUCT_ENGINEER,REC-001,h***@example.com,SUBMITTED,NONE,2026-07-01T00:00:00Z
                """.getBytes());

        mockMvc.perform(get("/api/v1/admin/recruiting/statistics.csv")
                .param("gisuId", "11")
                .param("schoolId", "22"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recruiting-statistics.csv\""))
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                assertThat(body).contains("maskedEmail");
                assertThat(body).doesNotContain("rawEmail");
                assertThat(body).doesNotContain("answer");
            });
    }

    @Test
    @DisplayName("상태 요약 API는 status별 count를 반환한다")
    void 상태_요약_API는_status별_count를_반환한다() throws Exception {
        Map<RecruitingApplicationStatus, Long> counts = new EnumMap<>(RecruitingApplicationStatus.class);
        counts.put(RecruitingApplicationStatus.SUBMITTED, 3L);
        given(getApplicationQueryUseCase.getStatusSummary(11L, 22L))
            .willReturn(new RecruitingStatusSummaryInfo(3L, counts));

        mockMvc.perform(get("/api/v1/admin/recruiting/summary")
                .param("gisuId", "11")
                .param("schoolId", "22"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.totalCount").value(3L))
            .andExpect(jsonPath("$.result.countByStatus.SUBMITTED").value(3L));
    }
}
