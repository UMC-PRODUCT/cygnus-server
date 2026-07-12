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

import java.util.EnumMap;
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

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.PrepareRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
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
    LinkRecruitingApplicationFormUseCase linkFormUseCase;
    @MockitoBean
    PublishRecruitingApplicationFormUseCase publishFormUseCase;
    @MockitoBean
    CloseRecruitingApplicationFormUseCase closeFormUseCase;
    @MockitoBean
    DecideRecruitingFinalUseCase decideFinalUseCase;
    @MockitoBean
    PrepareRecruitingRegistrationUseCase prepareRegistrationUseCase;
    @MockitoBean
    CancelRecruitingRegistrationUseCase cancelRegistrationUseCase;
    @MockitoBean
    ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;
    @MockitoBean
    ManageRecruitingInterviewScheduleUseCase manageScheduleUseCase;
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
    @DisplayName("모집 폼 연결 API는 차수와 Form ID command를 전달한다")
    void 모집_폼_연결_API는_차수와_Form_ID를_전달한다() throws Exception {
        given(linkFormUseCase.link(any())).willReturn(APPLICATION_FORM_ID);

        mockMvc.perform(post("/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}/forms", SEASON_ID, ROUND_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formId\":300}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(APPLICATION_FORM_ID));

        ArgumentCaptor<LinkRecruitingApplicationFormCommand> captor =
            ArgumentCaptor.forClass(LinkRecruitingApplicationFormCommand.class);
        then(linkFormUseCase).should().link(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(SEASON_ID);
        assertThat(captor.getValue().roundId()).isEqualTo(ROUND_ID);
        assertThat(captor.getValue().formId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("모집 폼 게시 API는 시즌과 요청자 ID를 command로 전달한다")
    void publishFormBindsSeasonAndRequester() throws Exception {
        mockMvc.perform(post(
                "/api/v1/recruiting/admin/seasons/{seasonId}/forms/{applicationFormId}/publish",
                SEASON_ID,
                APPLICATION_FORM_ID
            ))
            .andExpect(status().isOk());

        ArgumentCaptor<PublishRecruitingApplicationFormCommand> captor =
            ArgumentCaptor.forClass(PublishRecruitingApplicationFormCommand.class);
        then(publishFormUseCase).should().publish(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(SEASON_ID);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("모집 폼 마감 API는 시즌 ID를 command로 전달한다")
    void closeFormBindsSeason() throws Exception {
        mockMvc.perform(post(
                "/api/v1/recruiting/admin/seasons/{seasonId}/forms/{applicationFormId}/close",
                SEASON_ID,
                APPLICATION_FORM_ID
            ))
            .andExpect(status().isOk());

        ArgumentCaptor<CloseRecruitingApplicationFormCommand> captor =
            ArgumentCaptor.forClass(CloseRecruitingApplicationFormCommand.class);
        then(closeFormUseCase).should().close(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(SEASON_ID);
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
        given(exportRecruitingCsvUseCase.exportSummaryCsv(11L, 22L))
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
        given(getApplicationQueryUseCase.getStatusSummary(11L, 22L))
            .willReturn(new RecruitingStatusSummaryInfo(3L, counts));

        mockMvc.perform(get("/api/v1/recruiting/admin/summary")
                .param("gisuId", "11")
                .param("schoolId", "22"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.totalCount").value(3L))
            .andExpect(jsonPath("$.result.countByStatus.SUBMITTED").value(3L));
    }
}
