package com.umc.product.notification.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.notification.application.port.in.ManageFcmTopicUseCase;
import com.umc.product.notification.application.port.in.ManageFcmUseCase;
import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;
import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;

@WebMvcTest(controllers = FcmController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FcmController")
class FcmControllerTest {

    private static final Long MEMBER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    ManageFcmUseCase manageFcmUseCase;

    @MockitoBean
    ManageFcmTopicUseCase manageFcmTopicUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("installation과 FCM 토큰을 등록한다")
    void installation_등록() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/fcm/installations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "installationId": "installation-1",
                      "fcmToken": "token-1",
                      "platform": "IOS",
                      "appVersion": "1.0.0"
                    }
                    """))
            .andExpect(status().isOk());

        ArgumentCaptor<RegisterFcmTokenCommand> captor = ArgumentCaptor.forClass(RegisterFcmTokenCommand.class);
        then(manageFcmUseCase).should().registerFcmToken(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
            RegisterFcmTokenCommand.of(MEMBER_ID, "installation-1", "token-1", "IOS", "1.0.0")
        );
    }

    @Test
    @DisplayName("installationId가 없으면 등록을 거부한다")
    void installationId_없는_등록_거부() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/fcm/installations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fcmToken": "token-1"
                    }
                    """))
            .andExpect(status().isBadRequest());

        then(manageFcmUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("현재 회원의 installation을 해제한다")
    void installation_해제() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/fcm/installations/{installationId}", "installation-1"))
            .andExpect(status().isOk());

        then(manageFcmUseCase).should().unregisterFcmToken(
            UnregisterFcmTokenCommand.of(MEMBER_ID, "installation-1")
        );
    }

    @Test
    @DisplayName("token-only 등록 API는 제공하지 않는다")
    void token_only_등록_API_비활성화() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/fcm/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fcmToken": "token-1"
                    }
                    """))
            .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/notification/fcm/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fcmToken": "token-1"
                    }
                    """))
            .andExpect(status().isNotFound());

        then(manageFcmUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("token-only 해제 API는 제공하지 않는다")
    void token_only_해제_API_비활성화() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/fcm/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fcmToken": "token-1"
                    }
                    """))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/notification/fcm/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fcmToken": "token-1"
                    }
                    """))
            .andExpect(status().isNotFound());

        then(manageFcmUseCase).shouldHaveNoInteractions();
    }
}
