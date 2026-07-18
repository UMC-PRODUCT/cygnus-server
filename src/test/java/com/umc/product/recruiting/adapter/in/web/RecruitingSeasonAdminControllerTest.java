package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

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
    @MockitoBean
    SearchRecruitingSeasonUseCase searchSeasonUseCase;
    @MockitoBean
    SearchRecruitingRoundUseCase searchRoundUseCase;

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

    @Test
    @DisplayName("시즌 목록 조회는 기수와 지부 필터를 CurrentMember와 함께 전달한다")
    void searchSeasons() throws Exception {
        given(searchSeasonUseCase.searchSeasons(any())).willReturn(List.of(
            new RecruitingSeasonSummaryInfo(
                10L,
                11L,
                33L,
                "A 지부",
                22L,
                "A 학교",
                RecruitingSeasonStatus.ACTIVE,
                List.of(roundConfiguration())
            )
        ));

        mockMvc.perform(get("/api/v1/recruiting/admin/seasons")
                .param("gisuId", "11")
                .param("chapterId", "33"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].seasonId").value(10L))
            .andExpect(jsonPath("$.result[0].chapterName").value("A 지부"))
            .andExpect(jsonPath("$.result[0].rounds[0].id").value(20L));

        ArgumentCaptor<RecruitingSeasonSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingSeasonSearchQuery.class);
        then(searchSeasonUseCase).should().searchSeasons(captor.capture());
        assertThat(captor.getValue().gisuId()).isEqualTo(11L);
        assertThat(captor.getValue().chapterId()).isEqualTo(33L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("차수 목록 조회는 학교와 시즌 필터를 CurrentMember와 함께 전달한다")
    void searchRounds() throws Exception {
        given(searchRoundUseCase.searchRounds(any())).willReturn(List.of(
            new RecruitingRoundSummaryInfo(
                10L,
                11L,
                33L,
                "A 지부",
                22L,
                "A 학교",
                roundConfiguration()
            )
        ));

        mockMvc.perform(get("/api/v1/recruiting/admin/rounds")
                .param("gisuId", "11")
                .param("schoolId", "22")
                .param("seasonId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].seasonId").value(10L))
            .andExpect(jsonPath("$.result[0].schoolName").value("A 학교"))
            .andExpect(jsonPath("$.result[0].id").value(20L));

        ArgumentCaptor<RecruitingRoundSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingRoundSearchQuery.class);
        then(searchRoundUseCase).should().searchRounds(captor.capture());
        assertThat(captor.getValue().schoolId()).isEqualTo(22L);
        assertThat(captor.getValue().seasonId()).isEqualTo(10L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("시즌 목록 조회에서 기수 ID가 없으면 400을 반환한다")
    void searchSeasonsRequiresGisuId() throws Exception {
        mockMvc.perform(get("/api/v1/recruiting/admin/seasons"))
            .andExpect(status().isBadRequest());

        then(searchSeasonUseCase).should(never()).searchSeasons(any());
    }

    private RecruitingRoundConfigurationInfo roundConfiguration() {
        return new RecruitingRoundConfigurationInfo(
            20L,
            RecruitingRoundType.REGULAR,
            1,
            RecruitingRoundStatus.OPEN,
            List.of(ChallengerTrack.PLAN),
            false,
            java.time.Instant.parse("2026-08-01T00:00:00Z"),
            java.time.Instant.parse("2026-08-08T00:00:00Z"),
            java.time.Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            java.time.Instant.parse("2026-08-16T00:00:00Z"),
            null,
            "공고",
            "연락처"
        );
    }
}
