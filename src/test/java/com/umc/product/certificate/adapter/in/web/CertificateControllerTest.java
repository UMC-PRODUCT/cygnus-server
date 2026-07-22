package com.umc.product.certificate.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.certificate.application.port.in.command.AdminIssueCertificateUseCase;
import com.umc.product.certificate.application.port.in.command.IssueCertificateUseCase;
import com.umc.product.certificate.application.port.in.command.RevokeCertificateUseCase;
import com.umc.product.certificate.application.port.in.command.dto.AdminIssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.CertificateIssueInfo;
import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.RevokeCertificateCommand;
import com.umc.product.certificate.application.port.in.query.GetCertificateUseCase;
import com.umc.product.certificate.application.port.in.query.dto.CertificateDownloadInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificateInfo;
import com.umc.product.certificate.application.port.in.query.dto.CertificateVerificationInfo;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateStatus;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@WebMvcTest(controllers = {CertificateController.class, AdminCertificateController.class})
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CertificateController")
class CertificateControllerTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2027-07-01T00:00:00Z");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    IssueCertificateUseCase issueCertificateUseCase;

    @MockitoBean
    AdminIssueCertificateUseCase adminIssueCertificateUseCase;

    @MockitoBean
    RevokeCertificateUseCase revokeCertificateUseCase;

    @MockitoBean
    GetCertificateUseCase getCertificateUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(99L)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("본인 인증서를 셀프 발급한다")
    void 본인_인증서를_셀프_발급한다() throws Exception {
        // given
        given(issueCertificateUseCase.issue(any())).willReturn(issueInfo(
            "UMC-CMP-20260701-ABCDEFGH",
            CertificateTemplate.UMC_COURSE_COMPLETION,
            CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE
        ));

        // when & then
        mockMvc.perform(post("/api/v1/certificates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new SelfIssueRequestBody(
                    CertificateTemplate.UMC_COURSE_COMPLETION,
                    7L
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.serialNumber").value("UMC-CMP-20260701-ABCDEFGH"))
            .andExpect(jsonPath("$.result.template").value("UMC_COURSE_COMPLETION"))
            .andExpect(jsonPath("$.result.issuer").value("UNIVERSITY_MAKEUS_CHALLENGE"));

        verify(issueCertificateUseCase).issue(IssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_COURSE_COMPLETION)
            .requesterMemberId(99L)
            .gisuId(7L)
            .build());
    }

    @Test
    @DisplayName("셀프 발급은 template이 필수다")
    void 셀프_발급은_template이_필수다() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/certificates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "gisuId": 7
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(issueCertificateUseCase, never()).issue(any());
    }

    @Test
    @DisplayName("운영진 인증서는 template 없이 발급할 수 없다")
    void 운영진_인증서는_template_없이_발급할_수_없다() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/certificates/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "recipientMemberId": 1,
                      "gisuId": 7,
                      "meritTitle": "대상"
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(adminIssueCertificateUseCase, never()).issueByAdmin(any());
    }

    @Test
    @DisplayName("운영진 템플릿 발급 요청은 template 중심 command로 변환한다")
    void 운영진_템플릿_발급_요청은_template_중심_command로_변환한다() throws Exception {
        // given
        given(adminIssueCertificateUseCase.issueByAdmin(any())).willReturn(issueInfo(
            "UMC-MRT-20260701-ABCDEFGH",
            CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE,
            CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE
        ));

        // when
        mockMvc.perform(post("/api/v1/certificates/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "template": "UMC_DEMO_DAY_FIRST_PRIZE",
                      "recipientMemberId": 1,
                      "gisuId": 7,
                      "meritDescription": "탁월한 기여",
                      "reissue": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.template").value("UMC_DEMO_DAY_FIRST_PRIZE"))
            .andExpect(jsonPath("$.result.issuer").value("UNIVERSITY_MAKEUS_CHALLENGE"));

        // then
        verify(adminIssueCertificateUseCase).issueByAdmin(AdminIssueCertificateCommand.builder()
            .template(CertificateTemplate.UMC_DEMO_DAY_FIRST_PRIZE)
            .requesterMemberId(99L)
            .recipientMemberId(1L)
            .gisuId(7L)
            .meritDescription("탁월한 기여")
            .reissue(true)
            .build());
    }

    @Test
    @DisplayName("공개 검증 API는 유효 여부와 마스킹된 이름을 반환한다")
    void 공개_검증_API는_유효_여부와_마스킹된_이름을_반환한다() throws Exception {
        // given
        given(getCertificateUseCase.verifyBySerialNumber("UMC-CMP-20260701-ABCDEFGH"))
            .willReturn(new CertificateVerificationInfo(
                true,
                "ISSUED",
                CertificateTemplate.UMC_COURSE_COMPLETION,
                CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
                7L,
                "김*엠",
                ISSUED_AT,
                EXPIRES_AT
            ));

        // when & then
        mockMvc.perform(get("/api/v1/certificates/verify/UMC-CMP-20260701-ABCDEFGH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.valid").value(true))
            .andExpect(jsonPath("$.result.status").value("ISSUED"))
            .andExpect(jsonPath("$.result.template").value("UMC_COURSE_COMPLETION"))
            .andExpect(jsonPath("$.result.issuer").value("UNIVERSITY_MAKEUS_CHALLENGE"))
            .andExpect(jsonPath("$.result.gisuGeneration").value(7))
            .andExpect(jsonPath("$.result.recipientName").value("김*엠"));
    }

    @Test
    @DisplayName("본인 인증서 목록을 응답 DTO로 변환한다")
    void 본인_인증서_목록을_응답_DTO로_변환한다() throws Exception {
        given(getCertificateUseCase.listByMemberId(99L)).willReturn(List.of(new CertificateInfo(
            10L,
            "UMC-CMP-20260701-ABCDEFGH",
            CertificateTemplate.UMC_COURSE_COMPLETION,
            CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE,
            CertificateStatus.ISSUED,
            "김유엠",
            7L,
            7L,
            null,
            ISSUED_AT,
            EXPIRES_AT
        )));

        mockMvc.perform(get("/api/v1/certificates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[0].certificateId").value(10L))
            .andExpect(jsonPath("$.result[0].serialNumber").value("UMC-CMP-20260701-ABCDEFGH"));
    }

    @Test
    @DisplayName("본인 인증서 다운로드 정보를 응답 DTO로 변환한다")
    void 본인_인증서_다운로드_정보를_응답_DTO로_변환한다() throws Exception {
        given(getCertificateUseCase.getDownloadInfo(10L, 99L)).willReturn(CertificateDownloadInfo.of(
            10L,
            "UMC-CMP-20260701-ABCDEFGH",
            "https://cdn.example.com/certificate.pdf",
            EXPIRES_AT
        ));

        mockMvc.perform(get("/api/v1/certificates/{certificateId}/download", 10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.downloadUrl").value("https://cdn.example.com/certificate.pdf"))
            .andExpect(jsonPath("$.result.expiresAt").exists());
    }

    @Test
    @DisplayName("운영진 인증서 폐기 요청을 command로 변환한다")
    void 운영진_인증서_폐기_요청을_command로_변환한다() throws Exception {
        // when
        mockMvc.perform(patch("/api/v1/certificates/admin/{certificateId}/revoke", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new RevokeRequestBody("오발급"))))
            .andExpect(status().isOk());

        // then
        verify(revokeCertificateUseCase).revoke(RevokeCertificateCommand.builder()
            .certificateId(10L)
            .requesterMemberId(99L)
            .reason("오발급")
            .build());
    }

    private CertificateIssueInfo issueInfo(
        String serialNumber,
        CertificateTemplate template,
        CertificateIssuer issuer
    ) {
        return new CertificateIssueInfo(
            1L,
            serialNumber,
            template,
            issuer,
            CertificateStatus.ISSUED,
            ISSUED_AT,
            EXPIRES_AT
        );
    }

    private record SelfIssueRequestBody(
        CertificateTemplate template,
        Long gisuId
    ) {
    }

    private record RevokeRequestBody(String reason) {
    }
}
