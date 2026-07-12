package com.umc.product.recruiting.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
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
}
