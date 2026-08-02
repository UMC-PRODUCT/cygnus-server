package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingApplicationInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingInterviewSessionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleBoardUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleBoardInfo;

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
    ManageRecruitingInterviewSessionUseCase manageSessionUseCase;
    @MockitoBean
    ConfirmRecruitingInterviewSchedulesUseCase confirmSchedulesUseCase;
    @MockitoBean
    GetRecruitingInterviewScheduleUseCase getScheduleUseCase;
    @MockitoBean
    GetRecruitingInterviewSessionUseCase getSessionUseCase;
    @MockitoBean
    GetRecruitingInterviewScheduleBoardUseCase getScheduleBoardUseCase;
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

    @Test
    @DisplayName("면접 세션 생성은 body와 CurrentMember를 command로 변환한다")
    void createInterviewSessionUsesBodyAndCurrentMember() throws Exception {
        given(manageSessionUseCase.createSession(any())).willReturn(7L);

        mockMvc.perform(post("/api/v1/recruiting/admin/rounds/{roundId}/interview-sessions", 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"온라인 면접","startsAt":"2026-08-11T00:00:00Z",
                    "endsAt":"2026-08-11T00:30:00Z","mode":"ONLINE","location":"https://meet.example.com"}
                    """))
            .andExpect(status().isOk());

        ArgumentCaptor<CreateRecruitingInterviewSessionCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingInterviewSessionCommand.class);
        then(manageSessionUseCase).should().createSession(captor.capture());
        assertThat(captor.getValue().roundId()).isEqualTo(20L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(ACTOR_ID);
        assertThat(captor.getValue().name()).isEqualTo("온라인 면접");
    }

    @Test
    @DisplayName("보드 조회와 batch 확정은 날짜와 배정 목록을 CurrentMember와 함께 전달한다")
    void boardAndBatchConfirmationUseDateAssignmentsAndCurrentMember() throws Exception {
        given(getScheduleBoardUseCase.getBoard(any(), any(), any())).willReturn(
            new RecruitingInterviewScheduleBoardInfo(20L, java.time.LocalDate.of(2026, 8, 11),
                java.util.List.of(), java.util.List.of(), java.util.List.of())
        );

        mockMvc.perform(get("/api/v1/recruiting/admin/rounds/{roundId}/interview-schedule-board", 20L)
                .param("date", "2026-08-11"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/recruiting/admin/rounds/{roundId}/interview-schedule/confirmations", 20L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assignments":[{"applicationId":40,"sessionId":7,
                    "startsAt":"2026-08-11T00:00:00Z","contactSnapshot":"문의 채널"}]}
                    """))
            .andExpect(status().isOk());

        then(getScheduleBoardUseCase).should().getBoard(20L, java.time.LocalDate.of(2026, 8, 11), ACTOR_ID);
        ArgumentCaptor<ConfirmRecruitingInterviewSchedulesCommand> captor =
            ArgumentCaptor.forClass(ConfirmRecruitingInterviewSchedulesCommand.class);
        then(confirmSchedulesUseCase).should().confirmAll(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(ACTOR_ID);
        assertThat(captor.getValue().assignments()).singleElement().satisfies(assignment -> {
            assertThat(assignment.applicationId()).isEqualTo(40L);
            assertThat(assignment.sessionId()).isEqualTo(7L);
        });
    }

    @Test
    @DisplayName("단건 확정에 sessionId가 없으면 command를 호출하지 않고 400으로 거부한다")
    void rejectSingleConfirmationWithoutSessionId() throws Exception {
        mockMvc.perform(put("/api/v1/recruiting/admin/applications/{applicationId}/interview-schedule/confirmation", 40L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"startsAt":"2026-08-11T00:00:00Z","endsAt":"2026-08-11T00:15:00Z",
                    "location":"온라인","contactSnapshot":"문의 채널"}
                    """))
            .andExpect(status().isBadRequest());

        then(manageScheduleUseCase).shouldHaveNoInteractions();
    }

}
