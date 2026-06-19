package com.umc.product.notification.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.notification.application.port.in.RequestFcmNotificationUseCase;
import com.umc.product.notification.application.port.in.dto.FcmNotificationRequestInfo;
import com.umc.product.support.RestDocsConfig;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FcmAdminController.class)
@Import({JacksonConfig.class, RestDocsConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@DisplayName("FcmAdminController")
class FcmAdminControllerTest {

    private static final Long MEMBER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    RequestFcmNotificationUseCase requestFcmNotificationUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("관리자 FCM 알림 발송 요청은 202와 requestId를 반환한다")
    void 관리자_FCM_알림_발송_요청() throws Exception {
        UUID requestId = UUID.randomUUID();
        Instant queuedAt = Instant.parse("2026-06-20T00:00:00Z");
        given(requestFcmNotificationUseCase.request(any()))
            .willReturn(FcmNotificationRequestInfo.of(requestId, queuedAt));

        mockMvc.perform(post("/api/v1/notifications/admin/fcm/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "target": {
                        "memberIds": [1, 2],
                        "gisuId": 10,
                        "parts": ["SPRINGBOOT"]
                      },
                      "message": {
                        "title": "공지",
                        "body": "본문",
                        "data": {
                          "noticeId": "1"
                        },
                        "deepLink": "umc://notices/1"
                      }
                    }
                    """))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.requestId").value(requestId.toString()))
            .andExpect(jsonPath("$.result.queuedAt").value("2026-06-20T00:00:00Z"));
    }
}
