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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.support.RestDocsConfig;

@WebMvcTest(controllers = RecruitingApplicationController.class)
@Import({JacksonConfig.class, RestDocsConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@DisplayName("RecruitingApplicationController")
class RecruitingApplicationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    CreateRecruitingApplicationDraftUseCase createDraftUseCase;

    @MockitoBean
    UpdateRecruitingApplicationDraftUseCase updateDraftUseCase;

    @MockitoBean
    SubmitRecruitingApplicationUseCase submitUseCase;

    @MockitoBean
    CancelRecruitingApplicationUseCase cancelUseCase;

    @Test
    @DisplayName("지원서 draft 생성 API는 identity metadata를 command로 전달한다")
    void 지원서_draft_생성_API는_identity_metadata를_command로_전달한다() throws Exception {
        given(createDraftUseCase.createDraft(any()))
            .willReturn(RecruitingApplicationInfo.from(100L, "REC-001", RecruitingApplicationStatus.DRAFT));

        String body = """
            {
              "applicationFormId": 10,
              "applicantIdentityKey": "identity-key",
              "maskedEmail": "h***@example.com"
            }
            """;

        mockMvc.perform(post("/api/v1/recruiting/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.applicationId").value(100L))
            .andExpect(jsonPath("$.result.applicationNo").value("REC-001"));

        ArgumentCaptor<CreateRecruitingApplicationDraftCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingApplicationDraftCommand.class);
        then(createDraftUseCase).should().createDraft(captor.capture());
        assertThat(captor.getValue().applicationFormId()).isEqualTo(10L);
        assertThat(captor.getValue().applicantIdentityKey()).isEqualTo("identity-key");
        assertThat(captor.getValue().maskedEmail()).isEqualTo("h***@example.com");
    }

    @Test
    @DisplayName("지원서 draft 수정 API는 answers를 command로 전달한다")
    void 지원서_draft_수정_API는_answers를_command로_전달한다() throws Exception {
        given(updateDraftUseCase.updateDraft(any()))
            .willReturn(RecruitingApplicationInfo.from(100L, "REC-001", RecruitingApplicationStatus.DRAFT));

        String body = """
            {
              "answers": [
                {
                  "questionId": 7,
                  "textValue": "답변"
                }
              ]
            }
            """;

        mockMvc.perform(put("/api/v1/recruiting/applications/{applicationId}", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("DRAFT"));

        ArgumentCaptor<UpdateRecruitingApplicationDraftCommand> captor =
            ArgumentCaptor.forClass(UpdateRecruitingApplicationDraftCommand.class);
        then(updateDraftUseCase).should().updateDraft(captor.capture());
        assertThat(captor.getValue().applicationId()).isEqualTo(100L);
        assertThat(captor.getValue().answers()).hasSize(1);
        assertThat(captor.getValue().answers().get(0).questionId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("지원서 제출 API는 submittedIp를 command로 전달한다")
    void 지원서_제출_API는_submittedIp를_command로_전달한다() throws Exception {
        given(submitUseCase.submit(any()))
            .willReturn(RecruitingApplicationInfo.from(100L, "REC-001", RecruitingApplicationStatus.SUBMITTED));

        mockMvc.perform(post("/api/v1/recruiting/applications/{applicationId}/submit", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"submittedIp\":\"127.0.0.1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("SUBMITTED"));

        ArgumentCaptor<SubmitRecruitingApplicationCommand> captor =
            ArgumentCaptor.forClass(SubmitRecruitingApplicationCommand.class);
        then(submitUseCase).should().submit(captor.capture());
        assertThat(captor.getValue().applicationId()).isEqualTo(100L);
        assertThat(captor.getValue().submittedIp()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("지원서 철회 API는 cancel usecase를 호출한다")
    void 지원서_철회_API는_cancel_usecase를_호출한다() throws Exception {
        given(cancelUseCase.cancel(any()))
            .willReturn(RecruitingApplicationInfo.from(100L, "REC-001", RecruitingApplicationStatus.CANCELLED));

        mockMvc.perform(patch("/api/v1/recruiting/applications/{applicationId}/cancel", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"개인 사정\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.status").value("CANCELLED"));
    }
}
