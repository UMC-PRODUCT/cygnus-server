package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

@DisplayName("RecruitingApplicationController 수정/제출/철회")
class RecruitingApplicationMutationControllerTest extends RecruitingApplicationControllerTestSupport {

    @Test
    @DisplayName("지원서 수정 API는 잘못된 이메일만 있는 요청을 거부한다")
    void rejectMalformedEmailWhenOtherUpdateFieldsAreValid() throws Exception {
        mockMvc.perform(put("/api/v1/recruiting/applications/{applicationId}", 100L)
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateBody("a b@example.com", 7L)))
            .andExpect(status().isBadRequest());

        then(updateDraftUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서 수정 API는 비양수 질문 ID만 있는 요청을 거부한다")
    void rejectNonPositiveQuestionIdWhenOtherUpdateFieldsAreValid() throws Exception {
        mockMvc.perform(put("/api/v1/recruiting/applications/{applicationId}", 100L)
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateBody("applicant@example.com", 0L)))
            .andExpect(status().isBadRequest());

        then(updateDraftUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서 제출 API는 비양수 application ID를 거부한다")
    void rejectNonPositiveApplicationId() throws Exception {
        mockMvc.perform(post("/api/v1/recruiting/applications/{applicationId}/submit", 0L)
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        then(submitUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서 draft 수정 API는 인증 actor와 answers를 command로 전달한다")
    void updateDraftUsesAuthenticatedActor() throws Exception {
        given(updateDraftUseCase.updateDraft(any()))
            .willReturn(RecruitingApplicationInfo.of(100L, RecruitingApplicationStatus.DRAFT));

        mockMvc.perform(put("/api/v1/recruiting/applications/{applicationId}", 100L)
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUpdateBody("applicant@example.com", 7L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("DRAFT"));

        ArgumentCaptor<UpdateRecruitingApplicationDraftCommand> captor =
            ArgumentCaptor.forClass(UpdateRecruitingApplicationDraftCommand.class);
        then(updateDraftUseCase).should().updateDraft(captor.capture());
        assertThat(captor.getValue().applicationId()).isEqualTo(100L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(200L);
        assertThat(captor.getValue().answers()).hasSize(1);
        assertThat(captor.getValue().answers().get(0).questionId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("지원서 제출 API는 인증 actor와 submittedIp를 command로 전달한다")
    void submitUsesAuthenticatedActor() throws Exception {
        given(submitUseCase.submit(any()))
            .willReturn(RecruitingApplicationInfo.of(100L, RecruitingApplicationStatus.SUBMITTED));

        mockMvc.perform(post("/api/v1/recruiting/applications/{applicationId}/submit", 100L)
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"submittedIp\":\"127.0.0.1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("SUBMITTED"));

        ArgumentCaptor<SubmitRecruitingApplicationCommand> captor =
            ArgumentCaptor.forClass(SubmitRecruitingApplicationCommand.class);
        then(submitUseCase).should().submit(captor.capture());
        assertThat(captor.getValue().applicationId()).isEqualTo(100L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(200L);
        assertThat(captor.getValue().submittedIp()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("지원서 철회 API는 인증 actor를 command로 전달한다")
    void cancelUsesAuthenticatedActor() throws Exception {
        given(cancelUseCase.cancel(any()))
            .willReturn(RecruitingApplicationInfo.of(100L, RecruitingApplicationStatus.CANCELLED));

        mockMvc.perform(patch("/api/v1/recruiting/applications/{applicationId}/cancel", 100L)
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"개인 사정\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("CANCELLED"));

        ArgumentCaptor<CancelRecruitingApplicationCommand> captor =
            ArgumentCaptor.forClass(CancelRecruitingApplicationCommand.class);
        then(cancelUseCase).should().cancel(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(200L);
    }

    private String validUpdateBody(String email, Long questionId) {
        return """
            {
              "applicantName": "홍길동",
              "applicantEmail": "%s",
              "firstChoice": "PLAN",
              "answers": [{"questionId": %d, "textValue": "답변"}]
            }
            """.formatted(email, questionId);
    }
}
