package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;

@DisplayName("RecruitingApplicationController 생성/인증")
class RecruitingApplicationControllerTest extends RecruitingApplicationControllerTestSupport {

    @Test
    @DisplayName("본인 지원 내역 조회 API는 인증 회원의 지원서를 비회원 조회와 같은 응답으로 반환한다")
    void getMyApplicationsUsesAuthenticatedMember() throws Exception {
        given(listMyApplicationsUseCase.listMyApplications(200L)).willReturn(List.of(
            RecruitingPublicApplicationInfo.builder()
                .applicationId(100L)
                .applicantName("홍길동")
                .applicantEmail("applicant@example.com")
                .firstChoice(ChallengerTrack.PLAN)
                .submitted(true)
                .documentResult(RecruitingPublicResultStatus.PENDING)
                .finalResult(RecruitingPublicResultStatus.PENDING)
                .answers(List.of())
                .build()
        ));

        mockMvc.perform(get("/api/v1/recruiting/applications").session(authenticatedSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].applicationId").value(100L))
            .andExpect(jsonPath("$.result[0].applicantName").value("홍길동"))
            .andExpect(jsonPath("$.result[0].submitted").value(true))
            .andExpect(jsonPath("$.result[0].documentResult").value("PENDING"))
            .andExpect(jsonPath("$.result[0].answers").isArray());

        then(listMyApplicationsUseCase).should().listMyApplications(200L);
    }

    @Test
    @DisplayName("비로그인 회원의 본인 지원 내역 조회 요청은 거부한다")
    void rejectUnauthenticatedGetMyApplications() throws Exception {
        mockMvc.perform(get("/api/v1/recruiting/applications"))
            .andExpect(status().isUnauthorized());

        then(listMyApplicationsUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서 draft 생성 API는 인증 actor와 지원 기본 정보를 command로 전달한다")
    void createDraftUsesAuthenticatedActor() throws Exception {
        given(createDraftUseCase.createDraft(any()))
            .willReturn(RecruitingApplicationCreatedInfo.of(100L, "A1B2C3", RecruitingApplicationStatus.DRAFT));

        mockMvc.perform(post("/api/v1/recruiting/applications")
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationFormId": 10,
                      "applicantMemberId": 999,
                      "applicantName": "홍길동",
                      "applicantEmail": " Applicant@Example.COM ",
                      "firstChoice": "PLAN",
                      "secondChoice": "DESIGN"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.applicationId").value(100L))
            .andExpect(jsonPath("$.result.applicationKey").value("A1B2C3"));

        ArgumentCaptor<CreateRecruitingApplicationDraftCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingApplicationDraftCommand.class);
        then(createDraftUseCase).should().createDraft(captor.capture());
        assertThat(captor.getValue().applicationFormId()).isEqualTo(10L);
        assertThat(captor.getValue().applicantMemberId()).isEqualTo(200L);
        assertThat(captor.getValue().applicantName()).isEqualTo("홍길동");
        assertThat(captor.getValue().applicantEmail()).isEqualTo("applicant@example.com");
        assertThat(captor.getValue().firstChoice()).isEqualTo(ChallengerTrack.PLAN);
    }

    @Test
    @DisplayName("비로그인 회원의 지원서 생성 요청은 거부한다")
    void rejectUnauthenticatedCreate() throws Exception {
        mockMvc.perform(post("/api/v1/recruiting/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateBody("applicant@example.com", 10L)))
            .andExpect(status().isUnauthorized());

        then(createDraftUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서 생성 API는 잘못된 이메일만 있는 요청을 거부한다")
    void rejectMalformedEmailWhenOtherCreateFieldsAreValid() throws Exception {
        mockMvc.perform(post("/api/v1/recruiting/applications")
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateBody("a@@b.com", 10L)))
            .andExpect(status().isBadRequest());

        then(createDraftUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서 생성 API는 비양수 Form ID만 있는 요청을 거부한다")
    void rejectNonPositiveFormIdWhenOtherCreateFieldsAreValid() throws Exception {
        mockMvc.perform(post("/api/v1/recruiting/applications")
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateBody("applicant@example.com", 0L)))
            .andExpect(status().isBadRequest());

        then(createDraftUseCase).shouldHaveNoInteractions();
    }

    private String validCreateBody(String email, Long applicationFormId) {
        return """
            {
              "applicationFormId": %d,
              "applicantName": "홍길동",
              "applicantEmail": "%s",
              "firstChoice": "PLAN"
            }
            """.formatted(applicationFormId, email);
    }
}
