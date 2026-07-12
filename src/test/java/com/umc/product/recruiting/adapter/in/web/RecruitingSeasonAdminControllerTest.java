package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
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
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;

@WebMvcTest(RecruitingSeasonAdminController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class RecruitingSeasonAdminControllerTest {

    private static final Long MEMBER_ID = 99L;

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    CreateRecruitingSeasonUseCase createSeasonUseCase;
    @MockitoBean
    UpdateRecruitingSeasonStatusUseCase updateSeasonStatusUseCase;
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

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(MEMBER_ID), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("시즌 생성 요청의 트랙 쿼터를 command로 전달한다")
    void createSeasonWithQuotas() throws Exception {
        given(createSeasonUseCase.createSeason(any())).willReturn(10L);

        mockMvc.perform(post("/api/v1/recruiting/admin/seasons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gisuId": 11,
                      "schoolId": 22,
                      "quotas": [
                        {"track": "PLAN", "targetCount": 0},
                        {"track": "DESIGN", "targetCount": 4}
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(10L));

        ArgumentCaptor<CreateRecruitingSeasonCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingSeasonCommand.class);
        then(createSeasonUseCase).should().createSeason(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(MEMBER_ID);
        assertThat(captor.getValue().quotas()).hasSize(2);
        assertThat(captor.getValue().quotas().get(0).targetCount()).isZero();
    }

    @Test
    @DisplayName("시즌 쿼터 교체 요청의 seasonId와 트랙을 command로 전달한다")
    void replaceSeasonQuotas() throws Exception {
        mockMvc.perform(put("/api/v1/recruiting/admin/seasons/{seasonId}/quotas", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"quotas": [{"track": "WEB_PRODUCT_ENGINEER", "targetCount": 3}]}
                    """))
            .andExpect(status().isOk());

        ArgumentCaptor<ReplaceRecruitingSeasonTrackQuotasCommand> captor =
            ArgumentCaptor.forClass(ReplaceRecruitingSeasonTrackQuotasCommand.class);
        then(replaceQuotasUseCase).should().replaceQuotas(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(10L);
        assertThat(captor.getValue().quotas().getFirst().track())
            .isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("음수 시즌 쿼터 요청은 400을 반환한다")
    void createSeasonRejectsNegativeQuota() throws Exception {
        mockMvc.perform(post("/api/v1/recruiting/admin/seasons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gisuId": 11,
                      "schoolId": 22,
                      "quotas": [{"track": "PLAN", "targetCount": -1}]
                    }
                    """))
            .andExpect(status().isBadRequest());

        then(createSeasonUseCase).should(never()).createSeason(any());
    }
}
