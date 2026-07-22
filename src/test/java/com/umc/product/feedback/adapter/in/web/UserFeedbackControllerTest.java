package com.umc.product.feedback.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.feedback.application.port.in.command.SubmitUserFeedbackResponseUseCase;
import com.umc.product.feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand;
import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateUseCase;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(UserFeedbackController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserFeedbackController")
class UserFeedbackControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetUserFeedbackTemplateUseCase getUserFeedbackTemplateUseCase;

    @MockitoBean
    SubmitUserFeedbackResponseUseCase submitUserFeedbackResponseUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(10L).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("분류된 피드백 템플릿을 현재 회원에게 반환한다")
    void 분류된_피드백_템플릿을_현재_회원에게_반환한다() throws Exception {
        given(getUserFeedbackTemplateUseCase.findTemplate(10L, UserFeedbackContext.APPLICATION_SUBMITTED))
            .willReturn(Optional.of(UserFeedbackTemplateInfo.builder()
                .templateId(100L)
                .context(UserFeedbackContext.APPLICATION_SUBMITTED)
                .targetType(UserFeedbackTargetType.NEW_CHALLENGER)
                .form(FormWithStructureInfo.builder().formId(200L).sections(List.of()).build())
                .build()));

        mockMvc.perform(get("/api/v1/user-feedbacks/templates")
                .queryParam("context", "APPLICATION_SUBMITTED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.templateId").value(100L))
            .andExpect(jsonPath("$.result.form.formId").value(200L));
    }

    @Test
    @DisplayName("활성 피드백 템플릿이 없으면 null result를 반환한다")
    void 활성_피드백_템플릿이_없으면_null_result를_반환한다() throws Exception {
        given(getUserFeedbackTemplateUseCase.findTemplate(10L, UserFeedbackContext.MATCHING_COMPLETED))
            .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/user-feedbacks/templates")
                .queryParam("context", "MATCHING_COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("피드백 답변을 현재 회원의 제출 command로 변환한다")
    void 피드백_답변을_현재_회원의_제출_command로_변환한다() throws Exception {
        given(submitUserFeedbackResponseUseCase.submit(any())).willReturn(300L);

        mockMvc.perform(post("/api/v1/user-feedbacks/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateId": 100,
                      "answers": [
                        {
                          "questionId": 1,
                          "textValue": "좋아요",
                          "selectedOptionIds": [2],
                          "fileIds": ["file-id"]
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.formResponseId").value(300L));

        verify(submitUserFeedbackResponseUseCase).submit(SubmitUserFeedbackResponseCommand.builder()
            .templateId(100L)
            .respondentMemberId(10L)
            .answers(List.of(AnswerCommand.builder()
                .questionId(1L)
                .textValue("좋아요")
                .selectedOptionIds(List.of(2L))
                .fileIds(List.of("file-id"))
                .build()))
            .build());
    }

    @Test
    @DisplayName("피드백 answers가 비어 있으면 제출하지 않는다")
    void 피드백_answers가_비어_있으면_제출하지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/user-feedbacks/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateId": 100,
                      "answers": []
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(submitUserFeedbackResponseUseCase, never()).submit(any());
    }
}
