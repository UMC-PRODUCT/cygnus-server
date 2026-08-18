package com.umc.product.demoday.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.demoday.application.port.in.command.ChangeDemodayPollStatusUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayEntryCodeUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.query.GetDemodayVoteQrUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayVoteQrInfo;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = DemodayPollAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig.class)
@DisplayName("DemodayPollAdminController")
class DemodayPollAdminControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLL_ID = 10L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private CreateDemodayPollUseCase createDemodayPollUseCase;

    @MockitoBean private ChangeDemodayPollStatusUseCase changeDemodayPollStatusUseCase;

    @MockitoBean private CreateDemodayEntryCodeUseCase createDemodayEntryCodeUseCase;

    @MockitoBean private GetDemodayVoteQrUseCase getDemodayVoteQrUseCase;

    @BeforeEach
    void setUp() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("현재 구간의 INFO QR을 조회하면 200과 QR 정보를 반환한다")
    void getVoteQr() throws Exception {
        // given
        Instant generatedAt = Instant.parse("2026-08-17T15:00:00Z");
        Instant expiresAt = Instant.parse("2026-08-17T16:00:00Z");
        DemodayVoteQrInfo info = new DemodayVoteQrInfo(
            POLL_ID,
            "https://vote.umc.it.kr/demoday/polls/10/vote-authorization#token=eyJ",
            generatedAt,
            expiresAt);
        given(getDemodayVoteQrUseCase.get(POLL_ID, MEMBER_ID)).willReturn(info);

        // when & then
        mockMvc.perform(get("/api/v1/demoday/admin/polls/{pollId}/vote-qr", POLL_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.pollId").value(POLL_ID))
            .andExpect(jsonPath("$.result.qrValue").value(info.qrValue()))
            .andExpect(jsonPath("$.result.generatedAt").exists())
            .andExpect(jsonPath("$.result.expiresAt").exists());

        then(getDemodayVoteQrUseCase).should().get(POLL_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("운영 중이 아닌 Poll을 조회하면 오류 코드를 반환한다")
    void getVoteQrWhenPollIsNotOpen() throws Exception {
        // given
        willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_OPEN))
            .given(getDemodayVoteQrUseCase)
            .get(POLL_ID, MEMBER_ID);

        // when & then
        mockMvc.perform(get("/api/v1/demoday/admin/polls/{pollId}/vote-qr", POLL_ID))
            .andExpect(jsonPath("$.code").value(DemodayErrorCode.DEMODAY_POLL_NOT_OPEN.getCode()));
    }

    @Test
    @DisplayName("관리자 권한이 없으면 접근 오류 코드를 반환한다")
    void getVoteQrWhenNotAdmin() throws Exception {
        // given
        willThrow(new DemodayDomainException(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED))
            .given(getDemodayVoteQrUseCase)
            .get(POLL_ID, MEMBER_ID);

        // when & then
        mockMvc.perform(get("/api/v1/demoday/admin/polls/{pollId}/vote-qr", POLL_ID))
            .andExpect(jsonPath("$.code").value(DemodayErrorCode.DEMODAY_ADMIN_ACCESS_DENIED.getCode()));
    }
}
