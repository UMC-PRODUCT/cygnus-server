package com.umc.product.project.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.project.adapter.in.web.dto.request.CreateProjectApplicationRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdateApplicationDecisionRequest;
import com.umc.product.project.application.port.in.command.CancelProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.DecideApplicationUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectApplicationDraftUseCase;
import com.umc.product.project.application.port.in.command.dto.ApplicationDecisionStatus;
import com.umc.product.project.application.port.in.command.dto.SubmitProjectApplicationCommand;
import com.umc.product.project.application.port.in.command.dto.UpdateProjectApplicationDraftCommand;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationInfo;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

@WebMvcTest(controllers = ProjectApplicationController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectApplicationControllerTest {

    private static final Long TEST_MEMBER_ID = 99L;
    private static final Long PROJECT_ID = 42L;
    private static final Long APPLICATION_ID = 500L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProjectApplicationController controller;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    CreateDraftProjectApplicationUseCase createDraftProjectApplicationUseCase;

    @MockitoBean
    UpdateProjectApplicationDraftUseCase updateProjectApplicationDraftUseCase;

    @MockitoBean
    SubmitProjectApplicationUseCase submitProjectApplicationUseCase;

    @MockitoBean
    DecideApplicationUseCase decideApplicationUseCase;

    @MockitoBean
    CancelProjectApplicationUseCase cancelProjectApplicationUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(TEST_MEMBER_ID)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    void 지원서_초안_생성과_철회는_요청자_정보를_전달한다() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(TEST_MEMBER_ID).build();
        given(createDraftProjectApplicationUseCase.create(any()))
            .willReturn(ProjectApplicationInfo.of(APPLICATION_ID, ProjectApplicationStatus.DRAFT));
        given(cancelProjectApplicationUseCase.cancel(any()))
            .willReturn(ProjectApplicationInfo.of(APPLICATION_ID, ProjectApplicationStatus.CANCELLED));

        assertThat(controller.createDraft(
            principal, PROJECT_ID, new CreateProjectApplicationRequest(7L)).status())
            .isEqualTo(ProjectApplicationStatus.DRAFT);
        assertThat(controller.cancel(principal, APPLICATION_ID, "개인 사정").status())
            .isEqualTo(ProjectApplicationStatus.CANCELLED);

        then(createDraftProjectApplicationUseCase).should().create(any());
        then(cancelProjectApplicationUseCase).should().cancel(any());
    }

    @Nested
    class updateAndSubmit {

        @Test
        void PUT_applicationId_경로는_command에_applicationId를_전달한다() throws Exception {
            given(updateProjectApplicationDraftUseCase.update(any()))
                .willReturn(ProjectApplicationInfo.of(APPLICATION_ID, ProjectApplicationStatus.DRAFT));
            String body = """
                {
                  "answers": [
                    {
                      "questionId": 10,
                      "textValue": "답변"
                    }
                  ]
                }
                """;

            mockMvc.perform(put("/api/v1/projects/{projectId}/applications/{applicationId}",
                    PROJECT_ID, APPLICATION_ID)
                    .content(body)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.applicationId").value(APPLICATION_ID))
                .andExpect(jsonPath("$.result.status").value("DRAFT"));

            ArgumentCaptor<UpdateProjectApplicationDraftCommand> captor =
                ArgumentCaptor.forClass(UpdateProjectApplicationDraftCommand.class);
            then(updateProjectApplicationDraftUseCase).should().update(captor.capture());
            assertThat(captor.getValue().projectId()).isEqualTo(PROJECT_ID);
            assertThat(captor.getValue().applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(captor.getValue().requesterMemberId()).isEqualTo(TEST_MEMBER_ID);
        }

        @Test
        void POST_applicationId_submit_경로는_command에_applicationId를_전달한다() throws Exception {
            given(submitProjectApplicationUseCase.submit(any()))
                .willReturn(ProjectApplicationInfo.of(APPLICATION_ID, ProjectApplicationStatus.SUBMITTED));

            mockMvc.perform(post("/api/v1/projects/{projectId}/applications/{applicationId}/submit",
                    PROJECT_ID, APPLICATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.applicationId").value(APPLICATION_ID))
                .andExpect(jsonPath("$.result.status").value("SUBMITTED"));

            ArgumentCaptor<SubmitProjectApplicationCommand> captor =
                ArgumentCaptor.forClass(SubmitProjectApplicationCommand.class);
            then(submitProjectApplicationUseCase).should().submit(captor.capture());
            assertThat(captor.getValue().projectId()).isEqualTo(PROJECT_ID);
            assertThat(captor.getValue().applicationId()).isEqualTo(APPLICATION_ID);
            assertThat(captor.getValue().requesterMemberId()).isEqualTo(TEST_MEMBER_ID);
        }

        @Test
        void 기존_applications_me_update_경로는_usecase를_호출하지_않는다() throws Exception {
            String body = """
                { "answers": [] }
                """;

            mockMvc.perform(put("/api/v1/projects/{projectId}/applications/me", PROJECT_ID)
                    .content(body)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

            then(updateProjectApplicationDraftUseCase).should(never()).update(any());
        }

        @Test
        void 기존_applications_me_submit_경로는_usecase를_호출하지_않는다() throws Exception {
            mockMvc.perform(post("/api/v1/projects/{projectId}/applications/me/submit", PROJECT_ID))
                .andExpect(status().is4xxClientError());

            then(submitProjectApplicationUseCase).should(never()).submit(any());
        }
    }

    @Nested
    class PATCH_decision {

        @Test
        void APPROVED_요청시_200_및_status_반환() throws Exception {
            UpdateApplicationDecisionRequest request = new UpdateApplicationDecisionRequest(
                ApplicationDecisionStatus.APPROVED, "역량 우수"
            );
            given(decideApplicationUseCase.decide(
                eq(APPLICATION_ID), eq(ApplicationDecisionStatus.APPROVED), eq("역량 우수"), eq(TEST_MEMBER_ID)
            )).willReturn(ProjectApplicationInfo.of(APPLICATION_ID, ProjectApplicationStatus.APPROVED));

            mockMvc.perform(patch("/api/v1/projects/{projectId}/applications/{applicationId}/decision",
                    PROJECT_ID, APPLICATION_ID)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.applicationId").value(APPLICATION_ID))
                .andExpect(jsonPath("$.result.status").value("APPROVED"));
        }

        @Test
        void REJECTED_요청시_200_및_status_반환() throws Exception {
            UpdateApplicationDecisionRequest request = new UpdateApplicationDecisionRequest(
                ApplicationDecisionStatus.REJECTED, null
            );
            given(decideApplicationUseCase.decide(
                eq(APPLICATION_ID), eq(ApplicationDecisionStatus.REJECTED), eq(null), eq(TEST_MEMBER_ID)
            )).willReturn(ProjectApplicationInfo.of(APPLICATION_ID, ProjectApplicationStatus.REJECTED));

            mockMvc.perform(patch("/api/v1/projects/{projectId}/applications/{applicationId}/decision",
                    PROJECT_ID, APPLICATION_ID)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("REJECTED"));
        }

        @Test
        void PENDING_요청시_400_및_UseCase를_호출하지_않는다() throws Exception {
            String body = """
                { "status": "PENDING" }
                """;

            mockMvc.perform(patch("/api/v1/projects/{projectId}/applications/{applicationId}/decision",
                    PROJECT_ID, APPLICATION_ID)
                    .content(body)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            then(decideApplicationUseCase).should(never()).decide(any(), any(), any(), any());
        }

        @Test
        void status_누락시_400() throws Exception {
            String body = """
                { "reason": "사유" }
                """;

            mockMvc.perform(patch("/api/v1/projects/{projectId}/applications/{applicationId}/decision",
                    PROJECT_ID, APPLICATION_ID)
                    .content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            then(decideApplicationUseCase).should(never()).decide(any(), any(), any(), any());
        }

        @Test
        void status가_허용된_enum이_아니면_400() throws Exception {
            String body = """
                { "status": "UNKNOWN" }
                """;

            mockMvc.perform(patch("/api/v1/projects/{projectId}/applications/{applicationId}/decision",
                    PROJECT_ID, APPLICATION_ID)
                    .content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            then(decideApplicationUseCase).should(never()).decide(any(), any(), any(), any());
        }

        @Test
        void reason이_500자를_초과하면_400() throws Exception {
            UpdateApplicationDecisionRequest request = new UpdateApplicationDecisionRequest(
                ApplicationDecisionStatus.APPROVED, "a".repeat(501)
            );

            mockMvc.perform(patch("/api/v1/projects/{projectId}/applications/{applicationId}/decision",
                    PROJECT_ID, APPLICATION_ID)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            then(decideApplicationUseCase).should(never()).decide(any(), any(), any(), any());
        }

        @Test
        void 차수_종료_후_도메인_예외시_400_PROJECT_MATCHING_ROUND_LOCKED() throws Exception {
            UpdateApplicationDecisionRequest request = new UpdateApplicationDecisionRequest(
                ApplicationDecisionStatus.APPROVED, null
            );
            given(decideApplicationUseCase.decide(any(), any(), any(), any()))
                .willThrow(new ProjectDomainException(ProjectErrorCode.PROJECT_MATCHING_ROUND_LOCKED));

            mockMvc.perform(patch("/api/v1/projects/{projectId}/applications/{applicationId}/decision",
                    PROJECT_ID, APPLICATION_ID)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT-0306"));
        }

        @Test
        void DRAFT_상태_도메인_예외시_400_PROJECT_APPLICATION_DECISION_INVALID_TRANSITION() throws Exception {
            UpdateApplicationDecisionRequest request = new UpdateApplicationDecisionRequest(
                ApplicationDecisionStatus.APPROVED, null
            );
            given(decideApplicationUseCase.decide(any(), any(), any(), any()))
                .willThrow(new ProjectDomainException(ProjectErrorCode.PROJECT_APPLICATION_DECISION_INVALID_TRANSITION));

            mockMvc.perform(patch("/api/v1/projects/{projectId}/applications/{applicationId}/decision",
                    PROJECT_ID, APPLICATION_ID)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT-0212"));
        }
    }
}
