package com.umc.product.audit.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.audit.application.port.in.query.GetAuditLogUseCase;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;

@WebMvcTest(controllers = AuditLogController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("관리자 감사 로그 controller 입력 검증 계약")
class AuditLogControllerValidationContractTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetAuditLogUseCase getAuditLogUseCase;

    @Test
    @DisplayName("잘못된 enum 값은 400을 반환한다")
    void invalidEnumReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("outcome", "BROKEN"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 Instant 값은 400을 반환한다")
    void invalidInstantReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("from", "2026-02-10"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 actorMemberId 숫자 값은 400을 반환한다")
    void invalidActorIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/audit/admin/audit-logs").param("actorMemberId", "not-a-number"))
            .andExpect(status().isBadRequest());
    }
}
