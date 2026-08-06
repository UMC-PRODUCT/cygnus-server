package com.umc.product.audit.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.audit.application.port.in.query.GetAuditLogUseCase;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;

@WebMvcTest(controllers = AuditLogController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuditLogController")
class AuditLogControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetAuditLogUseCase getAuditLogUseCase;

    @Test
    @DisplayName("신규 감사 로그 admin 경로로 검색한다")
    void 신규_감사_로그_admin_경로로_검색한다() throws Exception {
        given(getAuditLogUseCase.search(any(), any())).willReturn(Page.empty());

        mockMvc.perform(get("/api/v1/audit/admin/audit-logs")
                .param("domain", "SCHEDULE")
                .param("action", "CREATE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.content").isArray());
    }

    @Test
    @DisplayName("기존 감사 로그 admin 경로는 더 이상 지원하지 않는다")
    void 기존_감사_로그_admin_경로는_더_이상_지원하지_않는다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
            .andExpect(status().isNotFound());
    }
}
