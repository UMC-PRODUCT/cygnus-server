package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
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
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@WebMvcTest(RecruitingInterviewScheduleController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RecruitingInterviewScheduleController")
class RecruitingInterviewScheduleControllerTest {

    private static final Long ACTOR_ID = 99L;
    private static final Long APPLICATION_ID = 40L;
    private static final String SUBMIT_AVAILABILITY_PATH =
        "/api/v1/recruiting/applications/{applicationId}/interview-schedule/availability";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    ManageRecruitingInterviewScheduleUseCase manageScheduleUseCase;
    @MockitoBean
    GetRecruitingInterviewScheduleUseCase getScheduleUseCase;

    @BeforeEach
    void authenticate() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(ACTOR_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("면접 가능 일정 제출은 times와 CurrentMember를 command로 전달한다")
    void submitAvailabilityUsesTimesAndCurrentMember() throws Exception {
        List<Instant> expectedTimes = List.of(
            Instant.parse("2026-08-12T10:00:00Z"),
            Instant.parse("2026-08-12T10:15:00Z")
        );

        mockMvc.perform(put(SUBMIT_AVAILABILITY_PATH, APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"times\":[\"2026-08-12T10:00:00Z\",\"2026-08-12T10:15:00Z\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("COMMON200"));

        ArgumentCaptor<SubmitRecruitingInterviewAvailabilityCommand> captor =
            ArgumentCaptor.forClass(SubmitRecruitingInterviewAvailabilityCommand.class);
        then(manageScheduleUseCase).should().submitAvailability(captor.capture());
        assertThat(captor.getValue().applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(ACTOR_ID);
        assertThat(captor.getValue().times()).containsExactlyElementsOf(expectedTimes);
    }

    @Test
    @DisplayName("면접 가능 일정 제출은 request body가 없으면 거부한다")
    void rejectMissingRequestBody() throws Exception {
        mockMvc.perform(put(SUBMIT_AVAILABILITY_PATH, APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        then(manageScheduleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 가능 일정 제출은 times가 없으면 거부한다")
    void rejectMissingTimes() throws Exception {
        mockMvc.perform(put(SUBMIT_AVAILABILITY_PATH, APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        then(manageScheduleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 가능 일정 제출은 빈 times를 거부한다")
    void rejectEmptyTimes() throws Exception {
        mockMvc.perform(put(SUBMIT_AVAILABILITY_PATH, APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"times\":[]}"))
            .andExpect(status().isBadRequest());

        then(manageScheduleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 가능 일정 제출은 null times 요소를 거부한다")
    void rejectNullTimeElement() throws Exception {
        mockMvc.perform(put(SUBMIT_AVAILABILITY_PATH, APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"times\":[null]}"))
            .andExpect(status().isBadRequest());

        then(manageScheduleUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 가능 시간 범위 오류는 표준 error envelope로 반환한다")
    void exposeInvalidPeriodError() throws Exception {
        willThrow(new RecruitingDomainException(
            RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD
        )).given(manageScheduleUseCase).submitAvailability(any());

        mockMvc.perform(put(SUBMIT_AVAILABILITY_PATH, APPLICATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"times\":[\"2026-08-12T10:00:00Z\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("RECRUITING-0413"));
    }
}
