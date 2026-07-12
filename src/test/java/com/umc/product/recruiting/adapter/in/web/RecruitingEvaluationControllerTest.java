package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

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

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.SaveRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.ValidateRecruitingApplicationScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationEvaluationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

@WebMvcTest(RecruitingEvaluationController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RecruitingEvaluationController")
class RecruitingEvaluationControllerTest {

    private static final Long ACTOR_ID = 99L;
    private static final Long ROUND_ID = 20L;
    private static final Long APPLICATION_ID = 40L;
    private static final String PATH =
        "/api/v1/recruiting/rounds/{roundId}/applications/{applicationId}/evaluations/{stage}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    SaveRecruitingApplicationEvaluationUseCase saveEvaluationUseCase;
    @MockitoBean
    SubmitRecruitingApplicationEvaluationUseCase submitEvaluationUseCase;
    @MockitoBean
    GetRecruitingApplicationEvaluationUseCase getEvaluationUseCase;
    @MockitoBean
    ValidateRecruitingApplicationScopeUseCase validateApplicationScopeUseCase;

    @BeforeEach
    void authenticate() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(ACTOR_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("평가 초안 저장은 path stage와 CurrentMember actor를 command로 전달한다")
    void saveDraftUsesPathStageAndCurrentMember() throws Exception {
        given(saveEvaluationUseCase.saveDraft(any())).willReturn(7L);

        mockMvc.perform(put(PATH, ROUND_ID, APPLICATION_ID, "DOCUMENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"PASS\",\"comment\":\"검토 중\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(7L));

        ArgumentCaptor<SaveRecruitingApplicationEvaluationCommand> captor =
            ArgumentCaptor.forClass(SaveRecruitingApplicationEvaluationCommand.class);
        then(saveEvaluationUseCase).should().saveDraft(captor.capture());
        assertThat(captor.getValue().applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(ACTOR_ID);
        assertThat(captor.getValue().stage()).isEqualTo(RecruitingEvaluatorStage.DOCUMENT);
    }

    @Test
    @DisplayName("평가 제출은 body memberId와 무관하게 CurrentMember actor를 사용한다")
    void submitUsesCurrentMemberInsteadOfBodyMemberId() throws Exception {
        mockMvc.perform(post(PATH + "/submit", ROUND_ID, APPLICATION_ID, "INTERVIEW")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":1234,\"decision\":\"WAIT\",\"comment\":\"보류\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<SubmitRecruitingApplicationEvaluationCommand> captor =
            ArgumentCaptor.forClass(SubmitRecruitingApplicationEvaluationCommand.class);
        then(submitEvaluationUseCase).should().submit(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(ACTOR_ID);
    }

    @Test
    @DisplayName("평가 조회는 stage 범위의 가시 평가만 반환한다")
    void listVisibleEvaluationsByStage() throws Exception {
        given(getEvaluationUseCase.listVisibleEvaluations(
            APPLICATION_ID,
            ACTOR_ID,
            RecruitingEvaluatorStage.DOCUMENT
        )).willReturn(List.of(new RecruitingApplicationEvaluationInfo(
            7L,
            APPLICATION_ID,
            ACTOR_ID,
            RecruitingEvaluatorStage.DOCUMENT,
            RecruitingApplicationEvaluationStatus.SUBMITTED,
            RecruitingApplicationEvaluationDecision.PASS,
            "충분함",
            Instant.parse("2026-07-13T01:00:00Z")
        )));

        mockMvc.perform(get(PATH, ROUND_ID, APPLICATION_ID, "DOCUMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].stage").value("DOCUMENT"))
            .andExpect(jsonPath("$.result[0].decision").value("PASS"));
    }

    @Test
    @DisplayName("평가 요청의 comment가 2000자를 초과하면 거부한다")
    void rejectTooLongComment() throws Exception {
        mockMvc.perform(put(PATH, ROUND_ID, APPLICATION_ID, "DOCUMENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"PASS\",\"comment\":\"%s\"}".formatted("a".repeat(2001))))
            .andExpect(status().isBadRequest());

        then(saveEvaluationUseCase).shouldHaveNoInteractions();
    }
}
