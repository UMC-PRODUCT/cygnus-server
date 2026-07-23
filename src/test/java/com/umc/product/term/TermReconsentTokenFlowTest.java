package com.umc.product.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.IssueAuthenticationTokensCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.support.TestContainersConfig;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.SchoolFixture;
import com.umc.product.support.isolation.DatabaseIsolation;
import com.umc.product.term.application.port.in.command.ManageTermUseCase;
import com.umc.product.term.application.port.in.command.dto.CreateTermCommand;
import com.umc.product.term.domain.enums.TermType;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Testcontainers
@DatabaseIsolation
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.terms.reconsent.enabled=true",
    "jwt.access-token-secret=terms-e2e-access-token-secret-000000000000000000000001",
    "jwt.refresh-token-secret=terms-e2e-refresh-token-secret-00000000000000000000002",
    "jwt.oauth-verification-token-secret=terms-e2e-oauth-token-secret-000000000000000000003",
    "jwt.email-verification-token-secret=terms-e2e-email-token-secret-000000000000000000004",
    "jwt.sso-login-token-secret=terms-e2e-sso-login-secret-00000000000000000000005"
})
@DisplayName("약관 재동의와 AccessToken 갱신 실제 E2E")
class TermReconsentTokenFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private ManageTermUseCase manageTermUseCase;

    @Autowired
    private ManageAuthenticationUseCase manageAuthenticationUseCase;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private ChapterFixture chapterFixture;

    @Autowired
    private SchoolFixture schoolFixture;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private FirebaseMessaging firebaseMessaging;

    @MockitoBean
    private StoragePort storagePort;

    private Long memberId;
    private Long requiredTermId;
    private NewTokens initialTokens;

    @BeforeEach
    void setUp() {
        Gisu gisu = gisuFixture.비활성_기수(9923L);
        Chapter chapter = chapterFixture.지부(gisu, "약관-E2E-지부");
        School school = schoolFixture.지부에_소속된_학교("약관-E2E-학교", chapter);
        Member member = saveMemberPort.save(Member.create(
            "약관회원",
            "약관회원",
            "terms-e2e@test.com",
            school.getId(),
            null
        ));
        memberId = member.getId();
        requiredTermId = manageTermUseCase.createTerms(CreateTermCommand.builder()
            .link("https://example.com/terms/service")
            .required(true)
            .type(TermType.SERVICE)
            .build());
        initialTokens = manageAuthenticationUseCase.issueTokens(
            IssueAuthenticationTokensCommand.of(memberId)
        );
    }

    @Test
    @DisplayName("미동의 토큰은 동의 후에도 차단되고 renew로 받은 새 토큰부터 일반 API를 사용할 수 있다")
    void recoverOnlyAfterTokenRenew() throws Exception {
        mockMvc.perform(get("/api/v1/member/me")
                .header("Authorization", bearer(initialTokens.accessToken())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("TERMS-0012"));

        MvcResult consentStatusResult = mockMvc.perform(get("/api/v1/terms/consent-status/me")
                .header("Authorization", bearer(initialTokens.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.needsReconsent").value(true))
            .andReturn();
        JsonNode missingRequiredTerms = objectMapper
            .readTree(consentStatusResult.getResponse().getContentAsByteArray())
            .path("result")
            .path("missingRequiredTerms");
        List<Long> missingRequiredTermIds = new java.util.ArrayList<>();
        missingRequiredTerms.forEach(term -> missingRequiredTermIds.add(term.path("id").asLong()));
        assertThat(missingRequiredTermIds).contains(requiredTermId);
        for (Long missingRequiredTermId : missingRequiredTermIds) {
            submitAgreement(missingRequiredTermId).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/member/me")
                .header("Authorization", bearer(initialTokens.accessToken())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("TERMS-0012"));

        MvcResult renewResult = mockMvc.perform(post("/api/v1/auth/token/renew")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(initialTokens.refreshToken())))
            .andExpect(status().isOk())
            .andReturn();
        String renewedAccessToken = objectMapper.readTree(renewResult.getResponse().getContentAsByteArray())
            .path("result")
            .path("accessToken")
            .asText();

        mockMvc.perform(get("/api/v1/member/me")
                .header("Authorization", bearer(renewedAccessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(memberId));
    }

    @Test
    @DisplayName("재동의 API는 비활성 약관과 선택 약관을 TERMS-0013으로 거부한다")
    void rejectInactiveAndOptionalTerms() throws Exception {
        Long optionalTermId = manageTermUseCase.createTerms(CreateTermCommand.builder()
            .link("https://example.com/terms/marketing")
            .required(false)
            .type(TermType.MARKETING)
            .build());

        submitAgreement(optionalTermId)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TERMS-0013"));

        manageTermUseCase.createTerms(CreateTermCommand.builder()
            .link("https://example.com/terms/service-v2")
            .required(true)
            .type(TermType.SERVICE)
            .build());

        submitAgreement(requiredTermId)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TERMS-0013"));
    }

    private org.springframework.test.web.servlet.ResultActions submitAgreement(Long termId) throws Exception {
        return mockMvc.perform(post("/api/v1/terms/agreements")
            .header("Authorization", bearer(initialTokens.accessToken()))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "termsId": %d,
                  "isAgreed": true
                }
                """.formatted(termId)));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
