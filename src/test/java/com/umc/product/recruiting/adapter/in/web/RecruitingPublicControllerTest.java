package com.umc.product.recruiting.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import com.umc.product.recruiting.application.port.in.command.CreateAnonymousRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.support.RestDocsConfig;

@WebMvcTest(controllers = RecruitingPublicController.class)
@Import({JacksonConfig.class, RestDocsConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@DisplayName("RecruitingPublicController")
class RecruitingPublicControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetRecruitingFormQueryUseCase getRecruitingFormQueryUseCase;

    @MockitoBean
    GetAnonymousRecruitingApplicationUseCase getAnonymousApplicationUseCase;

    @MockitoBean
    CreateAnonymousRecruitingApplicationDraftUseCase createAnonymousDraftUseCase;

    @MockitoBean
    UpdateAnonymousRecruitingApplicationUseCase updateAnonymousApplicationUseCase;

    @MockitoBean
    SubmitAnonymousRecruitingApplicationUseCase submitAnonymousApplicationUseCase;

    @Test
    @DisplayName("public forms API는 학교별 게시된 모집 폼을 반환한다")
    void public_forms_API는_학교별_게시된_모집_폼을_반환한다() throws Exception {
        given(getRecruitingFormQueryUseCase.listPublicForms(11L, 22L))
            .willReturn(List.of(new RecruitingApplicationFormInfo(
                1L,
                100L,
                RecruitingRoundType.REGULAR,
                1,
                200L,
                RecruitingApplicationFormStatus.PUBLISHED
            )));

        mockMvc.perform(get("/api/v1/recruiting/public/forms")
                .param("gisuId", "11")
                .param("schoolId", "22"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].applicationFormId").value(1L))
            .andExpect(jsonPath("$.result[0].status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("익명 지원서 생성 API는 지원 키를 최초 응답에서 반환한다")
    void 익명_지원서_생성_API는_지원_키를_최초_응답에서_반환한다() throws Exception {
        given(createAnonymousDraftUseCase.createAnonymousDraft(org.mockito.ArgumentMatchers.any()))
            .willReturn(RecruitingApplicationCreatedInfo.of(100L, "A1B2C3", RecruitingApplicationStatus.DRAFT));

        mockMvc.perform(post("/api/v1/recruiting/public/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationFormId": 10,
                      "applicantName": "홍길동",
                      "applicantEmail": "Applicant@Example.COM",
                      "firstChoice": "PLAN",
                      "privacyTermId": 3,
                      "privacyAgreed": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.applicationId").value(100L))
            .andExpect(jsonPath("$.result.applicationKey").value("A1B2C3"));
    }

    @Test
    @DisplayName("익명 지원서 조회 API는 내부 Form access key와 application key를 노출하지 않는다")
    void 익명_지원서_조회_API는_내부_key를_노출하지_않는다() throws Exception {
        given(getAnonymousApplicationUseCase.getByCredential("applicant@example.com", "A1B2C3"))
            .willReturn(RecruitingPublicApplicationInfo.builder()
                .applicationId(100L)
                .applicantName("홍길동")
                .applicantEmail("applicant@example.com")
                .firstChoice(com.umc.product.common.domain.enums.ChallengerTrack.PLAN)
                .submitted(true)
                .editable(true)
                .documentResult(RecruitingPublicResultStatus.PENDING)
                .finalResult(RecruitingPublicResultStatus.PENDING)
                .answers(List.of())
                .build());

        mockMvc.perform(post("/api/v1/recruiting/public/applications/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "Applicant@Example.COM",
                      "applicationKey": "A1B2C3"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.applicationId").value(100L))
            .andExpect(jsonPath("$.result.documentResult").value("PENDING"))
            .andExpect(jsonPath("$.result.applicationKey").doesNotExist())
            .andExpect(jsonPath("$.result.formResponseAccessKey").doesNotExist());
    }
}
