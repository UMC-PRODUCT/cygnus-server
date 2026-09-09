package com.umc.product.analytics.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.analytics.application.port.in.query.GetAdminSchoolSummaryUseCase;
import com.umc.product.analytics.application.port.in.query.dto.AdminSchoolSummaryInfo;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = AdminSchoolAnalyticsController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminSchoolAnalyticsController")
class AdminSchoolAnalyticsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetAdminSchoolSummaryUseCase getAdminSchoolSummaryUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(1L).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("학교별 summary API 응답")
    void 학교별_summary_API_응답() throws Exception {
        AdminSchoolSummaryInfo info = AdminSchoolSummaryInfo.of(
            10L,
            "가천대학교",
            1L,
            "중앙",
            20L,
            AdminSchoolSummaryInfo.StaffInfo.of(100L, "회장"),
            null,
            AdminSchoolSummaryInfo.PartLeaderRatioInfo.of(3L, 6L),
            -3.5,
            2L,
            4L
        );
        given(getAdminSchoolSummaryUseCase.getSchoolSummaries(any()))
            .willReturn(new PageImpl<>(List.of(info), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/analytics/admin/schools/summary")
                .param("gisuId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].schoolName").value("가천대학교"))
            .andExpect(jsonPath("$.result.content[0].riskChallengerCount").value(2L));
    }
}
