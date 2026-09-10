package com.umc.product.maintenance.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.maintenance.application.port.in.query.GetMaintenanceStatusUseCase;
import com.umc.product.maintenance.application.port.in.query.dto.MaintenanceStatusInfo;
import com.umc.product.maintenance.application.port.in.query.dto.MaintenanceWindowInfo;
import com.umc.product.maintenance.domain.MaintenanceDomain;
import com.umc.product.maintenance.domain.MaintenanceScope;

@WebMvcTest(controllers = SystemStatusController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SystemStatusController")
class SystemStatusControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetMaintenanceStatusUseCase getMaintenanceStatusUseCase;

    @Test
    void 점검중이_아닐_때_inMaintenance_false() throws Exception {
        given(getMaintenanceStatusUseCase.getStatus())
            .willReturn(new MaintenanceStatusInfo(false, null, null));

        mockMvc.perform(get("/api/v1/system/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.inMaintenance").value(false))
            .andExpect(jsonPath("$.result.current").doesNotExist())
            .andExpect(jsonPath("$.result.upcoming").doesNotExist());
    }

    @Test
    void 점검중일_때_current_채워짐() throws Exception {
        Instant now = Instant.parse("2026-05-18T10:00:00Z");
        MaintenanceWindowInfo current = new MaintenanceWindowInfo(
            7L,
            MaintenanceScope.PER_DOMAIN,
            EnumSet.of(MaintenanceDomain.CHALLENGER),
            now.minusSeconds(60),
            now.plusSeconds(3600),
            "긴급 점검",
            "잠시만 기다려주세요",
            null,
            null,
            1L,
            now.minusSeconds(120)
        );
        given(getMaintenanceStatusUseCase.getStatus())
            .willReturn(new MaintenanceStatusInfo(true, current, null));

        mockMvc.perform(get("/api/v1/system/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.inMaintenance").value(true))
            .andExpect(jsonPath("$.result.current.id").value(7))
            .andExpect(jsonPath("$.result.current.scope").value("PER_DOMAIN"))
            .andExpect(jsonPath("$.result.current.targetDomains[0]").value("CHALLENGER"))
            .andExpect(jsonPath("$.result.current.title").value("긴급 점검"));
    }
}
