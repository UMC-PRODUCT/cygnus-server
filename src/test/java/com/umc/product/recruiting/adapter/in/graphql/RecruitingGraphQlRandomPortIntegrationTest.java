package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.service.query.RecruitingQueryService;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.support.TestContainersConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.banner-mode=off",
        "spring.graphql.schema.introspection.enabled=true",
        "jwt.access-token-secret=task11-access-secret-000000000000000001",
        "jwt.refresh-token-secret=task11-refresh-secret-0000000000000001",
        "jwt.oauth-verification-token-secret=task11-oauth-secret-000000000000001",
        "jwt.email-verification-token-secret=task11-email-secret-000000000000001",
        "jwt.sso-login-token-secret=task11-sso-secret-000000000000000001"
    }
)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Testcontainers
class RecruitingGraphQlRandomPortIntegrationTest {

    private static final Long MEMBER_ID = 200L;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RecruitingQueryService recruitingQueryService;

    @MockitoBean
    JavaMailSender mailSender;

    @MockitoBean
    FirebaseMessaging firebaseMessaging;

    @MockitoBean
    StoragePort storagePort;

    @Test
    @DisplayName("실제 GraphQL HTTP는 제거된 anonymous credential Query를 거부한다")
    void 실제_GraphQL_HTTP는_제거된_anonymous_credential_Query를_거부한다() throws Exception {
        ResponseEntity<String> response = post("""
            query {
              recruitingApplicationResult(
                applicationNo: "legacy",
                applicantIdentityKey: "legacy"
              ) { status }
            }
            """, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.has("data")).isFalse();
        assertThat(body.path("errors").toString()).contains("recruitingApplicationResult");
    }

    @Test
    @DisplayName("실제 GraphQL HTTP는 JWT CurrentMember로 로그인 지원서를 조회한다")
    void 실제_GraphQL_HTTP는_JWT_CurrentMember로_로그인_지원서를_조회한다() throws Exception {
        given(recruitingQueryService.getById(20L, MEMBER_ID)).willReturn(applicationInfo());

        ResponseEntity<String> response = post("""
            query {
              recruitingApplication(applicationId: "%s") {
                id
                status
                registrationStatus
                acceptedTrack
              }
            }
            """.formatted(GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, 20L)), true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode application = objectMapper.readTree(response.getBody())
            .path("data")
            .path("recruitingApplication");
        assertThat(application.path("id").asText())
            .isEqualTo(GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, 20L));
        assertThat(application.path("registrationStatus").asText()).isEqualTo("READY");
        assertThat(application.path("acceptedTrack").asText()).isEqualTo("DESIGN");
    }

    @Test
    @DisplayName("실제 GraphQL HTTP는 비로그인 지원서 조회를 FORBIDDEN으로 거부한다")
    void 실제_GraphQL_HTTP는_비로그인_지원서_조회를_FORBIDDEN으로_거부한다() throws Exception {
        ResponseEntity<String> response = post("""
            query {
              recruitingApplication(applicationId: "%s") { id }
            }
            """.formatted(GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, 20L)), false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("errors").get(0).path("extensions").path("code").asText())
            .isEqualTo("COMMON-403");
        assertThat(body.path("data").isNull()).isTrue();
    }

    private ResponseEntity<String> post(String query, boolean authenticated) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authenticated) {
            headers.setBearerAuth(jwtTokenProvider.createAccessToken(MEMBER_ID, List.of("MEMBER")));
        }
        String body = objectMapper.writeValueAsString(Map.of("query", query));
        return restTemplate.exchange("/graphql", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private static RecruitingApplicationInfo applicationInfo() {
        return new RecruitingApplicationInfo(
            20L,
            RecruitingApplicationStatus.FINAL_PASSED,
            RecruitingApplicationRegistrationStatus.READY,
            ChallengerTrack.PLAN,
            ChallengerTrack.DESIGN,
            ChallengerTrack.DESIGN
        );
    }
}
