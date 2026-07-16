package com.umc.product.authorization.adapter.in.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@WebMvcTest(controllers = ChallengerRoleController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChallengerRoleController")
class ChallengerRoleControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    ManageChallengerRoleUseCase manageChallengerRoleUseCase;

    @MockitoBean
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @MockitoBean
    GetGisuUseCase getGisuUseCase;

    @Test
    @DisplayName("운영진 역할 생성 요청에 SUPER_ADMIN을 입력하면 400")
    void 운영진_역할_생성_요청에_super_admin을_입력하면_400() throws Exception {
        mockMvc.perform(post("/api/v1/authorization/challenger-role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "challengerId": 1,
                      "roleType": "SUPER_ADMIN",
                      "organizationId": 2,
                      "responsiblePart": "SPRINGBOOT",
                      "gisuId": 3
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(manageChallengerRoleUseCase);
    }
}
