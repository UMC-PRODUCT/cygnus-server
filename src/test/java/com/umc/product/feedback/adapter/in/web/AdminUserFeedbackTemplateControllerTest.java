package com.umc.product.feedback.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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

import com.umc.product.feedback.application.port.in.command.ManageUserFeedbackTemplateUseCase;
import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateAdminUseCase;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateDetailInfo;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateSummaryInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = AdminUserFeedbackTemplateController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserFeedbackTemplateControllerTest {

    private static final Long MEMBER_ID = 99L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetUserFeedbackTemplateAdminUseCase getAdminUseCase;

    @MockitoBean
    ManageUserFeedbackTemplateUseCase manageUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(MEMBER_ID)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("GET 관리자 템플릿 목록은 필터 결과를 반환한다")
    void GET_관리자_템플릿_목록() throws Exception {
        given(getAdminUseCase.listTemplates(
            UserFeedbackContext.APPLICATION_SUBMITTED,
            UserFeedbackTargetType.NEW_CHALLENGER,
            true
        )).willReturn(List.of(UserFeedbackTemplateSummaryInfo.builder()
            .templateId(1L)
            .context(UserFeedbackContext.APPLICATION_SUBMITTED)
            .targetType(UserFeedbackTargetType.NEW_CHALLENGER)
            .isActive(true)
            .formId(10L)
            .title("지원 경험")
            .build()));

        mockMvc.perform(get("/api/v1/admin/user-feedbacks/templates")
                .param("context", "APPLICATION_SUBMITTED")
                .param("targetType", "NEW_CHALLENGER")
                .param("active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].templateId").value(1))
            .andExpect(jsonPath("$.result[0].title").value("지원 경험"));
    }

    @Test
    @DisplayName("GET 관리자 템플릿 단건은 폼 구조를 반환한다")
    void GET_관리자_템플릿_단건() throws Exception {
        given(getAdminUseCase.getTemplate(1L)).willReturn(detailInfo());

        mockMvc.perform(get("/api/v1/admin/user-feedbacks/templates/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.templateId").value(1))
            .andExpect(jsonPath("$.result.form.formId").value(10));
    }

    @Test
    @DisplayName("POST 관리자 템플릿 생성은 생성된 상세를 반환한다")
    void POST_관리자_템플릿_생성() throws Exception {
        given(manageUseCase.create(any())).willReturn(detailInfo());

        mockMvc.perform(post("/api/v1/admin/user-feedbacks/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(templateBody(null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.templateId").value(1));
    }

    @Test
    @DisplayName("PUT 관리자 템플릿 수정은 수정된 상세를 반환한다")
    void PUT_관리자_템플릿_수정() throws Exception {
        given(manageUseCase.update(any())).willReturn(detailInfo());

        mockMvc.perform(put("/api/v1/admin/user-feedbacks/templates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(templateBody(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.templateId").value(1));
    }

    @Test
    @DisplayName("DELETE 관리자 템플릿은 204를 반환한다")
    void DELETE_관리자_템플릿() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/user-feedbacks/templates/1"))
            .andExpect(status().isNoContent());
    }

    private UserFeedbackTemplateDetailInfo detailInfo() {
        return UserFeedbackTemplateDetailInfo.builder()
            .templateId(1L)
            .context(UserFeedbackContext.APPLICATION_SUBMITTED)
            .targetType(UserFeedbackTargetType.NEW_CHALLENGER)
            .isActive(true)
            .form(FormWithStructureInfo.builder()
                .formId(10L)
                .title("지원 경험")
                .status(FormStatus.PUBLISHED)
                .sections(List.of())
                .build())
            .build();
    }

    private String templateBody(Long sectionId) {
        String sectionIdField = sectionId == null ? "" : "\"sectionId\": " + sectionId + ",";
        return """
            {
              "context": "APPLICATION_SUBMITTED",
              "targetType": "NEW_CHALLENGER",
              "title": "지원 경험",
              "description": "설명",
              "isAnonymous": false,
              "allowDuplicateResponses": false,
              "sections": [
                {
                  %s
                  "title": "섹션",
                  "description": null,
                  "orderNo": 1,
                  "questions": []
                }
              ]
            }
            """.formatted(sectionIdField);
    }
}
