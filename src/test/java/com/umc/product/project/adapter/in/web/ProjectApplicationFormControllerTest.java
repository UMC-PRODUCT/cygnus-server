package com.umc.product.project.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.project.adapter.in.web.dto.common.ApplicationFormSection;
import com.umc.product.project.adapter.in.web.dto.common.ApplicationQuestionItem;
import com.umc.product.project.adapter.in.web.dto.common.ApplicationQuestionOptionItem;
import com.umc.product.project.adapter.in.web.dto.request.UpsertApplicationFormRequest;
import com.umc.product.project.application.port.in.command.UpsertProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.form.domain.enums.QuestionType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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

@WebMvcTest(controllers = ProjectApplicationFormController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectApplicationFormControllerTest {

    private static final Long TEST_MEMBER_ID = 99L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    UpsertProjectApplicationFormUseCase upsertProjectApplicationFormUseCase;

    @MockitoBean
    GetProjectApplicationFormUseCase getProjectApplicationFormUseCase;

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
    void PUT_지원_폼_저장() throws Exception {
        UpsertApplicationFormRequest request = new UpsertApplicationFormRequest(
            "Triple 지원서", "팀 소개",
            List.of(
                ApplicationFormSection.builder()
                    .sectionId(null).type(FormSectionType.PART)
                    .allowedParts(Set.of(ChallengerPart.WEB))
                    .title("프론트엔드").description(null).orderNo(1)
                    .questions(List.of(
                        ApplicationQuestionItem.builder()
                            .questionId(null).type(QuestionType.RADIO).title("선호 프레임워크")
                            .description(null).isRequired(true).orderNo(1)
                            .options(List.of(
                                ApplicationQuestionOptionItem.builder()
                                    .optionId(null).content("React").orderNo(1).isOther(false).build()
                            ))
                            .build()
                    ))
                    .build()
            )
        );

        given(upsertProjectApplicationFormUseCase.upsert(any())).willReturn(
            ApplicationFormInfo.builder()
                .projectId(42L).applicationFormId(100L)
                .title("Triple 지원서").description("팀 소개")
                .sections(List.of())
                .build()
        );

        mockMvc.perform(put("/api/v1/projects/42/application-form")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.projectId").value(42))
            .andExpect(jsonPath("$.result.applicationFormId").value(100));
    }

    @Test
    void PUT_sections_필드_누락시_400() throws Exception {
        String body = """
            { "title": "Triple 지원서" }
            """;

        mockMvc.perform(put("/api/v1/projects/42/application-form")
                .content(body).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void PUT_section_title이_빈_문자열이면_400() throws Exception {
        UpsertApplicationFormRequest request = new UpsertApplicationFormRequest(
            null, null,
            List.of(
                ApplicationFormSection.builder()
                    .sectionId(null).type(FormSectionType.COMMON).allowedParts(Set.of())
                    .title("").description(null).orderNo(1).questions(List.of())
                    .build()
            )
        );

        mockMvc.perform(put("/api/v1/projects/42/application-form")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void GET_폼이_있으면_구조_반환() throws Exception {
        given(getProjectApplicationFormUseCase.findByProjectId(42L, TEST_MEMBER_ID)).willReturn(Optional.of(
            ApplicationFormInfo.builder()
                .projectId(42L).applicationFormId(100L)
                .title("Triple 지원서").description(null)
                .sections(List.of())
                .build()
        ));

        mockMvc.perform(get("/api/v1/projects/42/application-form"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.applicationFormId").value(100));
    }

    @Test
    void GET_폼이_없으면_result_null() throws Exception {
        given(getProjectApplicationFormUseCase.findByProjectId(42L, TEST_MEMBER_ID)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/projects/42/application-form"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").doesNotExist());
    }
}
