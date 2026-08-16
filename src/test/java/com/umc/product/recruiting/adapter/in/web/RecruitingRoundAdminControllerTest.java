package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CloneRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.DeleteRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.RestoreRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.application.port.in.query.CheckRecruitingRoundTitleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundGroupUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingSeasonUseCase;

@WebMvcTest(RecruitingSeasonAdminController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class RecruitingRoundAdminControllerTest {

    private static final String INTERVIEW_ROUND_JSON = """
        {
          "title": "15기 본모집",
          "type": "REGULAR",
          "recruitableTracks": ["PLAN", "DESIGN"],
          "secondChoiceEnabled": true,
          "documentStartAt": "2026-08-01T00:00:00Z",
          "documentEndAt": "2026-08-08T00:00:00Z",
          "documentResultPublishedAt": "2026-08-10T00:00:00Z",
          "interviewRequired": true,
          "interviewStartAt": "2026-08-11T00:00:00Z",
          "interviewEndAt": "2026-08-14T00:00:00Z",
          "finalResultPublishedAt": "2026-08-16T00:00:00Z",
          "availabilityFormId": 100,
          "availabilityScheduleQuestionId": 200,
          "announcement": "안내",
          "contactText": "문의"
        }
        """;

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    CreateRecruitingSeasonUseCase createSeasonUseCase;
    @MockitoBean
    UpdateRecruitingSeasonUseCase updateSeasonUseCase;
    @MockitoBean
    ReplaceRecruitingSeasonTrackQuotasUseCase replaceQuotasUseCase;
    @MockitoBean
    CreateRecruitingRoundUseCase createRoundUseCase;
    @MockitoBean
    UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    @MockitoBean
    UpdateRecruitingRoundUseCase updateRoundUseCase;
    @MockitoBean
    GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;
    @MockitoBean
    SearchRecruitingSeasonUseCase searchSeasonUseCase;
    @MockitoBean
    SearchRecruitingRoundUseCase searchRoundUseCase;
    @MockitoBean
    SearchRecruitingRoundGroupUseCase searchRoundGroupUseCase;
    @MockitoBean
    CheckRecruitingRoundTitleUseCase checkRoundTitleUseCase;
    @MockitoBean
    CloneRecruitingRoundUseCase cloneRoundUseCase;
    @MockitoBean
    DeleteRecruitingRoundUseCase deleteRoundUseCase;

    @MockitoBean
    RestoreRecruitingRoundUseCase restoreRoundUseCase;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(99L), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("면접 차수 생성 요청의 전체 설정을 command로 전달한다")
    void createInterviewRound() throws Exception {
        given(createRoundUseCase.createRound(any())).willReturn(20L);

        mockMvc.perform(post("/api/v1/recruiting/admin/seasons/{seasonId}/rounds", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(INTERVIEW_ROUND_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(20L));

        ArgumentCaptor<CreateRecruitingRoundCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingRoundCommand.class);
        then(createRoundUseCase).should().createRound(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(10L);
        assertThat(captor.getValue().configuration().recruitableTracks())
            .containsExactly(ChallengerTrack.PLAN, ChallengerTrack.DESIGN);
        assertThat(captor.getValue().configuration().availabilityFormId()).isEqualTo(100L);
        assertThat(captor.getValue().configuration().availabilityScheduleQuestionId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("면접 없는 차수에 availability form이 있으면 400을 반환한다")
    void updateNoInterviewRoundRejectsAvailabilityForm() throws Exception {
        String invalid = INTERVIEW_ROUND_JSON
            .replace("\"interviewRequired\": true", "\"interviewRequired\": false")
            .replace("\"interviewStartAt\": \"2026-08-11T00:00:00Z\",", "")
            .replace("\"interviewEndAt\": \"2026-08-14T00:00:00Z\",", "");

        mockMvc.perform(put("/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}", 10L, 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
            .andExpect(status().isBadRequest());
        then(updateRoundUseCase).should(never()).updateRound(any());
    }

    @Test
    @DisplayName("면접 차수 변경 요청은 availability Form과 SCHEDULE 질문 ID를 함께 전달한다")
    void updateInterviewRound() throws Exception {
        mockMvc.perform(put("/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}", 10L, 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(INTERVIEW_ROUND_JSON))
            .andExpect(status().isOk());

        ArgumentCaptor<UpdateRecruitingRoundCommand> captor =
            ArgumentCaptor.forClass(UpdateRecruitingRoundCommand.class);
        then(updateRoundUseCase).should().updateRound(captor.capture());
        assertThat(captor.getValue().configuration().interviewRequired()).isTrue();
        assertThat(captor.getValue().configuration().availabilityFormId()).isEqualTo(100L);
        assertThat(captor.getValue().configuration().availabilityScheduleQuestionId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("면접 없는 차수 변경 요청은 null 면접 설정을 전달한다")
    void updateNoInterviewRound() throws Exception {
        String valid = INTERVIEW_ROUND_JSON
            .replace("\"interviewRequired\": true", "\"interviewRequired\": false")
            .replace("\"interviewStartAt\": \"2026-08-11T00:00:00Z\",", "")
            .replace("\"interviewEndAt\": \"2026-08-14T00:00:00Z\",", "")
            .replace("\"availabilityFormId\": 100,", "");
        valid = valid.replace("\"availabilityScheduleQuestionId\": 200,", "");

        mockMvc.perform(put("/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}", 10L, 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valid))
            .andExpect(status().isOk());

        ArgumentCaptor<UpdateRecruitingRoundCommand> captor =
            ArgumentCaptor.forClass(UpdateRecruitingRoundCommand.class);
        then(updateRoundUseCase).should().updateRound(captor.capture());
        assertThat(captor.getValue().configuration().interviewStartAt()).isNull();
        assertThat(captor.getValue().configuration().availabilityFormId()).isNull();
        assertThat(captor.getValue().configuration().availabilityScheduleQuestionId()).isNull();
    }

    @Test
    @DisplayName("차수 상태 변경은 path의 seasonId와 roundId를 모두 전달한다")
    void updateRoundStatusIncludesSeasonId() throws Exception {
        mockMvc.perform(patch("/api/v1/recruiting/admin/seasons/{seasonId}/rounds/{roundId}/status", 10L, 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"OPEN\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<UpdateRecruitingRoundStatusCommand> captor =
            ArgumentCaptor.forClass(UpdateRecruitingRoundStatusCommand.class);
        then(updateRoundStatusUseCase).should().updateRoundStatus(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(10L);
        assertThat(captor.getValue().roundId()).isEqualTo(20L);
    }
}
