package com.umc.product.term.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.term.application.port.in.command.ManageTermUseCase;
import com.umc.product.term.application.port.in.command.SubmitRequiredTermReconsentUseCase;
import com.umc.product.term.application.port.in.command.dto.SubmitRequiredTermReconsentCommand;
import com.umc.product.term.application.port.in.query.GetRequiredTermConsentStatusUseCase;
import com.umc.product.term.application.port.in.query.GetTermUseCase;
import com.umc.product.term.application.port.in.query.dto.ActiveTermInfo;
import com.umc.product.term.domain.enums.TermType;

@WebMvcTest(controllers = TermController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TermController")
class TermControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetTermUseCase getTermUseCase;

    @MockitoBean
    GetRequiredTermConsentStatusUseCase getRequiredTermConsentStatusUseCase;

    @MockitoBean
    ManageTermUseCase manageTermUseCase;

    @MockitoBean
    SubmitRequiredTermReconsentUseCase submitRequiredTermReconsentUseCase;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/terms 활성 약관 전체 목록을 반환한다")
    void listActiveTerms() throws Exception {
        // given
        given(getTermUseCase.listActiveTerms()).willReturn(List.of(
            new ActiveTermInfo(
                1L,
                TermType.SERVICE,
                "서비스 이용약관",
                "https://example.com/terms/service",
                true,
                1L,
                Instant.parse("2026-05-26T00:00:00Z"),
                Instant.parse("2026-05-27T00:00:00Z")
            ),
            new ActiveTermInfo(
                2L,
                TermType.PRIVACY,
                "개인정보 처리방침",
                "https://example.com/terms/privacy",
                true,
                2L,
                Instant.parse("2026-05-28T00:00:00Z"),
                Instant.parse("2026-05-29T00:00:00Z")
            )
        ));

        // when & then
        mockMvc.perform(get("/api/v1/terms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.result.terms").isArray())
            .andExpect(jsonPath("$.result.terms[0].id").value("1"))
            .andExpect(jsonPath("$.result.terms[0].type").value("SERVICE"))
            .andExpect(jsonPath("$.result.terms[0].typeDescription").value("서비스 이용약관"))
            .andExpect(jsonPath("$.result.terms[0].link").value("https://example.com/terms/service"))
            .andExpect(jsonPath("$.result.terms[0].isMandatory").value(true))
            .andExpect(jsonPath("$.result.terms[0].version").value("1"))
            .andExpect(jsonPath("$.result.terms[0].createdAt").value("2026-05-26T00:00:00Z"))
            .andExpect(jsonPath("$.result.terms[0].updatedAt").value("2026-05-27T00:00:00Z"))
            .andExpect(jsonPath("$.result.terms[1].type").value("PRIVACY"));
    }

    @Test
    @DisplayName("POST /api/v1/terms/agreements 활성 필수 약관 재동의를 저장한다")
    void submitRequiredTermReconsent() throws Exception {
        authenticate(100L);
        mockMvc.perform(post("/api/v1/terms/agreements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"termsId": 10, "isAgreed": true}
                    """))
            .andExpect(status().isOk());

        then(submitRequiredTermReconsentUseCase).should()
            .submitRequiredTermReconsent(SubmitRequiredTermReconsentCommand.of(100L, 10L));
    }

    @Test
    @DisplayName("POST /api/v1/terms/agreements 약관 ID가 누락되면 400을 반환한다")
    void rejectMissingTermId() throws Exception {
        authenticate(100L);
        mockMvc.perform(post("/api/v1/terms/agreements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"isAgreed": true}
                    """))
            .andExpect(status().isBadRequest());

        then(submitRequiredTermReconsentUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /api/v1/terms/agreements 동의 값이 false면 400을 반환한다")
    void rejectFalseAgreement() throws Exception {
        authenticate(100L);
        mockMvc.perform(post("/api/v1/terms/agreements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"termsId": 10, "isAgreed": false}
                    """))
            .andExpect(status().isBadRequest());

        then(submitRequiredTermReconsentUseCase).shouldHaveNoInteractions();
    }

    private void authenticate(Long memberId) {
        MemberPrincipal principal = new MemberPrincipal(memberId);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
