package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingApplicationInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;

@WebMvcTest(controllers = {
    RecruitingAdminEvaluatorController.class,
    RecruitingAdminInterviewController.class,
    RecruitingAdminQuestionController.class,
    RecruitingInterviewScheduleController.class
})
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Recruiting 관리 REST controller")
class RecruitingManagementControllerTest {

    private static final Long ACTOR_ID = 99L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    ManageRecruitingRoundEvaluatorUseCase manageEvaluatorUseCase;
    @MockitoBean
    GetRecruitingRoundEvaluatorUseCase getEvaluatorUseCase;
    @MockitoBean
    ManageRecruitingInterviewScheduleUseCase manageScheduleUseCase;
    @MockitoBean
    GetRecruitingInterviewScheduleUseCase getScheduleUseCase;
    @MockitoBean
    ManageRecruitingRoundInterviewQuestionUseCase manageRoundQuestionUseCase;
    @MockitoBean
    ManageRecruitingApplicationInterviewQuestionUseCase manageApplicationQuestionUseCase;
    @MockitoBean
    GetRecruitingInterviewQuestionUseCase getQuestionUseCase;

    @BeforeEach
    void authenticate() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(ACTOR_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("평가자 추가는 target memberId를 path에서 받고 actor는 CurrentMember를 사용한다")
    void addEvaluatorUsesPathTargetAndCurrentMember() throws Exception {
        given(manageEvaluatorUseCase.addEvaluator(any())).willReturn(1L);

        mockMvc.perform(post("/api/v1/recruiting/admin/rounds/{roundId}/evaluators/{memberId}",
            20L, 300L))
            .andExpect(status().isOk());

        ArgumentCaptor<RecruitingRoundEvaluatorCommand> captor =
            ArgumentCaptor.forClass(RecruitingRoundEvaluatorCommand.class);
        then(manageEvaluatorUseCase).should().addEvaluator(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(ACTOR_ID);
        assertThat(captor.getValue().memberId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("공통 질문의 blank content는 400으로 거부한다")
    void rejectBlankRoundQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/recruiting/admin/rounds/{roundId}/questions", 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"   \",\"orderNo\":0}"))
            .andExpect(status().isBadRequest());

        then(manageRoundQuestionUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 가능 일정 요청은 CurrentMember actor를 전달한다")
    void requestScheduleUsesCurrentMember() throws Exception {
        given(manageScheduleUseCase.requestAvailability(any())).willReturn(5L);

        mockMvc.perform(post("/api/v1/recruiting/admin/applications/{applicationId}/interview-schedule/request", 40L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contactSnapshot\":\"운영진 연락처\"}"))
            .andExpect(status().isOk());

        ArgumentCaptor<RequestRecruitingInterviewScheduleCommand> captor =
            ArgumentCaptor.forClass(RequestRecruitingInterviewScheduleCommand.class);
        then(manageScheduleUseCase).should().requestAvailability(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(ACTOR_ID);
    }

}
