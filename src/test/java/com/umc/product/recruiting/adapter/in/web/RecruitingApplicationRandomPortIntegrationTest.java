package com.umc.product.recruiting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.p6spy.engine.logging.P6LogFactory;
import com.p6spy.engine.spy.P6ModuleManager;
import com.p6spy.engine.spy.P6SpyFactory;
import com.p6spy.engine.spy.appender.Slf4JLogger;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResourceInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.application.service.command.RecruitingApplicationCommandService;
import com.umc.product.recruiting.application.service.query.RecruitingPublicApplicationQueryService;
import com.umc.product.recruiting.application.service.query.RecruitingSeasonQueryService;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.support.TestContainersConfig;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "decorator.datasource.p6spy.enable-logging=true",
        "logging.level.p6spy=WARN",
        "spring.main.banner-mode=off",
        "jwt.access-token-secret=task5-round4-access-secret-0000000001",
        "jwt.refresh-token-secret=task5-round4-refresh-secret-000000001",
        "jwt.oauth-verification-token-secret=task5-round4-oauth-secret-0000000001",
        "jwt.email-verification-token-secret=task5-round4-email-secret-0000000001",
        "jwt.sso-login-token-secret=task5-round4-sso-secret-000000000001"
    }
)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@ContextConfiguration(initializers = RecruitingApplicationRandomPortIntegrationTest.P6SpyLoggingInitializer.class)
@Testcontainers
@DisplayName("Recruiting application random-port HTTP")
class RecruitingApplicationRandomPortIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(RecruitingApplicationRandomPortIntegrationTest.class);
    private static final Long MEMBER_ID = 200L;
    private static final String APPLICATION_KEY = "R4SAFE";
    private static final String PROBE_EMAIL = "socket-probe@example.invalid";
    private static String previousP6SpyModuleList;
    private static String previousP6SpyAppender;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    RecruitingApplicationCommandService applicationCommandService;

    @MockitoBean
    RecruitingSeasonQueryService recruitingSeasonQueryService;

    @MockitoBean
    RecruitingPublicApplicationQueryService recruitingPublicApplicationQueryService;

    @MockitoBean
    GetRecruitingResourceUseCase getRecruitingResourceUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @MockitoBean
    ExportRecruitingCsvUseCase exportRecruitingCsvUseCase;

    @MockitoBean
    JavaMailSender mailSender;

    @MockitoBean
    FirebaseMessaging firebaseMessaging;

    @MockitoBean
    StoragePort storagePort;

    @Test
    @DisplayName("실제 REST socket에서 JWT actor로 지원서 초안을 생성한다")
    void 실제_REST_socket_JWT_actor_지원서_생성() throws JsonProcessingException {
        given(applicationCommandService.createDraft(argThat(command -> MEMBER_ID.equals(command.applicantMemberId()))))
            .willReturn(createdInfo());

        ResponseEntity<String> response = post(
            "/api/v1/recruiting/applications",
            RecruitingHttpTestPayloads.restCreate(objectMapper, PROBE_EMAIL),
            authorizationHeaders()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode result = objectMapper.readTree(response.getBody()).path("result");
        assertThat(result.path("applicationId").asLong()).isEqualTo(900L);
        assertThat(result.path("applicationKey").asText()).isEqualTo(APPLICATION_KEY);
        assertThat(result.path("status").asText()).isEqualTo("DRAFT");
        then(applicationCommandService).should().createDraft(
            argThat(command -> MEMBER_ID.equals(command.applicantMemberId()))
        );
        recordTranscript("rest-happy", response);
    }

    @Test
    @DisplayName("실제 REST socket에서 비로그인과 malformed email 요청을 거부한다")
    void 실제_REST_socket_비로그인과_malformed_email_거부() throws JsonProcessingException {
        ResponseEntity<String> unauthenticated = post(
            "/api/v1/recruiting/applications",
            RecruitingHttpTestPayloads.restCreate(objectMapper, PROBE_EMAIL),
            jsonHeaders()
        );
        ResponseEntity<String> malformed = post(
            "/api/v1/recruiting/applications",
            RecruitingHttpTestPayloads.restCreate(objectMapper, "invalid-email"),
            authorizationHeaders()
        );

        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(malformed.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        then(applicationCommandService).shouldHaveNoInteractions();
        recordTranscript("rest-unauthenticated", unauthenticated);
        recordTranscript("rest-malformed-email", malformed);
    }

    @Test
    @DisplayName("실제 GraphQL HTTP에서 query와 인증 mutation 결과를 관측한다")
    void 실제_GraphQL_HTTP_query와_인증_mutation_결과() throws JsonProcessingException {
        given(recruitingSeasonQueryService.searchPublicRounds(any())).willReturn(List.of());
        given(applicationCommandService.createDraft(argThat(command -> MEMBER_ID.equals(command.applicantMemberId()))))
            .willReturn(createdInfo());
        given(getRecruitingResourceUseCase.getApplication(900L, MEMBER_ID)).willReturn(applicationInfo());

        ResponseEntity<String> queryResponse = post(
            "/graphql",
            RecruitingHttpTestPayloads.graphQlQuery(objectMapper),
            jsonHeaders()
        );
        ResponseEntity<String> mutationResponse = post(
            "/graphql",
            RecruitingHttpTestPayloads.graphQlMutation(objectMapper, PROBE_EMAIL),
            authorizationHeaders()
        );

        assertThat(queryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(queryResponse.getBody()).path("data")
            .path("recruitingRounds").isArray()).isTrue();
        JsonNode created = objectMapper.readTree(mutationResponse.getBody()).path("data")
            .path("createRecruitingApplication");
        assertThat(created.path("application").path("id").asLong()).isEqualTo(900L);
        assertThat(created.path("application").path("status").asText()).isEqualTo("DRAFT");
        assertThat(created.path("credential").isNull()).isTrue();
        recordTranscript("graphql-query", queryResponse);
        recordTranscript("graphql-authenticated-mutation", mutationResponse);
    }

    @Test
    @DisplayName("실제 GraphQL HTTP에서 비로그인 mutation은 개인정보 동의가 없으면 거부한다")
    void 실제_GraphQL_HTTP_비로그인_mutation_개인정보_동의_거부() throws JsonProcessingException {
        ResponseEntity<String> response = post(
            "/graphql",
            RecruitingHttpTestPayloads.graphQlMutation(objectMapper, PROBE_EMAIL),
            jsonHeaders()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("errors").isArray()).isTrue();
        assertThat(body.path("data").hasNonNull("createRecruitingApplication")).isFalse();
        then(applicationCommandService).shouldHaveNoInteractions();
        recordTranscript("graphql-unauthenticated-mutation", response);
    }

    @Test
    @DisplayName("실제 Security chain에서 공개 Round 목록은 익명 요청을 허용한다")
    void 실제_Security_chain은_공개_Round_익명_요청을_허용한다() {
        given(recruitingSeasonQueryService.searchPublicRounds(any())).willReturn(List.of());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/recruiting/public/rounds?gisuId=11&schoolId=22",
            HttpMethod.GET,
            new HttpEntity<>(jsonHeaders()),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("실제 REST socket에서 익명 지원서를 생성하고 credential로 조회한다")
    void 실제_REST_socket_익명_지원서_생성_조회() throws JsonProcessingException {
        given(applicationCommandService.createAnonymousDraft(any()))
            .willReturn(createdInfo());
        given(recruitingPublicApplicationQueryService.getByCredential(PROBE_EMAIL, APPLICATION_KEY))
            .willReturn(publicApplicationInfo());

        ResponseEntity<String> created = post(
            "/api/v1/recruiting/public/applications",
            """
                {
                  "applicationFormId": 100,
                  "applicantName": "지원자",
                  "applicantEmail": "%s",
                  "firstChoice": "PLAN",
                  "privacyTermId": 3,
                  "privacyAgreed": true
                }
                """.formatted(PROBE_EMAIL),
            jsonHeaders()
        );
        ResponseEntity<String> found = post(
            "/api/v1/recruiting/public/applications/lookup",
            """
                {
                  "email": "%s",
                  "applicationKey": "%s"
                }
                """.formatted(PROBE_EMAIL, APPLICATION_KEY),
            jsonHeaders()
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(created.getBody()).path("result").path("applicationKey").asText())
            .isEqualTo(APPLICATION_KEY);
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode foundResult = objectMapper.readTree(found.getBody()).path("result");
        assertThat(foundResult.path("applicationId").asLong()).isEqualTo(900L);
        assertThat(foundResult.has("applicationKey")).isFalse();
        assertThat(foundResult.has("formResponseAccessKey")).isFalse();
        recordTranscript("rest-anonymous-created", created);
        recordTranscript("rest-anonymous-lookup", found);
    }

    @Test
    @DisplayName("실제 GraphQL HTTP에서 anonymous credential Query를 실행한다")
    void 실제_GraphQL_HTTP_anonymous_credential_Query() throws JsonProcessingException {
        given(recruitingPublicApplicationQueryService.getByCredential(PROBE_EMAIL, APPLICATION_KEY))
            .willReturn(publicApplicationInfo());
        String graphQlBody = objectMapper.writeValueAsString(Map.of("query", """
            query {
              recruitingApplication(access: {credential: {
                email: "%s", applicationKey: "%s"
              }}) {
                id
                private { documentResult finalResult }
              }
            }
            """.formatted(PROBE_EMAIL, APPLICATION_KEY)));

        ResponseEntity<String> response = post("/graphql", graphQlBody, jsonHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode result = objectMapper.readTree(response.getBody())
            .path("data")
            .path("recruitingApplication");
        assertThat(result.path("id").asText()).isEqualTo("900");
        assertThat(result.path("private").path("documentResult").asText()).isEqualTo("PENDING");
        assertThat(result.path("private").path("finalResult").asText()).isEqualTo("PENDING");
        recordTranscript("graphql-anonymous-credential", response);
    }

    @Test
    @DisplayName("실제 REST socket에서 CSV 요청자를 use case 경계에 전달한다")
    void 실제_REST_socket_CSV_요청자_결속() {
        byte[] csv = ("gisuId,schoolId,roundType,roundNo,applicationId,maskedEmail,firstChoiceTrack,"
            + "secondChoiceTrack,acceptedTrack,status,registrationStatus,submittedAt\n")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        given(exportRecruitingCsvUseCase.exportSummaryCsv(15L, null, MEMBER_ID)).willReturn(csv);
        ResponseEntity<byte[]> response = restTemplate.exchange(
            "/api/v1/recruiting/admin/statistics.csv?gisuId=15",
            HttpMethod.GET,
            new HttpEntity<>(authorizationHeaders()),
            byte[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8))
            .startsWith("gisuId,schoolId,roundType,roundNo,applicationId,maskedEmail")
            .doesNotContain("applicantName", "applicantEmail", "applicationKey");
        then(exportRecruitingCsvUseCase).should().exportSummaryCsv(15L, null, MEMBER_ID);
    }

    @Test
    @DisplayName("실제 PostgreSQL P6Spy 로그는 INSERT와 SELECT 바인딩 값을 노출하지 않는다")
    void 실제_PostgreSQL_P6Spy_INSERT_SELECT_redaction() {
        ch.qos.logback.classic.Logger p6spyLogger =
            (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("p6spy");
        Level previousLevel = p6spyLogger.getLevel();
        boolean previousAdditive = p6spyLogger.isAdditive();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        p6spyLogger.addAppender(appender);
        p6spyLogger.setLevel(Level.INFO);
        p6spyLogger.setAdditive(false);
        try {
            jdbcTemplate.execute("drop table if exists task5_sql_redaction_probe");
            jdbcTemplate.execute("create table task5_sql_redaction_probe (applicant_email text, application_key text)");
            jdbcTemplate.update(
                "insert into task5_sql_redaction_probe (applicant_email, application_key) values (?, ?)",
                PROBE_EMAIL,
                APPLICATION_KEY
            );
            Integer count = jdbcTemplate.queryForObject(
                "select count(*) from task5_sql_redaction_probe where applicant_email = ? and application_key = ?",
                Integer.class,
                PROBE_EMAIL,
                APPLICATION_KEY
            );

            assertThat(count).isEqualTo(1);
            String formattedSql = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
            assertThat(formattedSql)
                .contains("task5_sql_redaction_probe", "applicant_email", "application_key", "?")
                .doesNotContain(PROBE_EMAIL, APPLICATION_KEY);
            log.warn("task5_sql_redaction_qa operation=insert/select values=[REDACTED]");
        } finally {
            jdbcTemplate.execute("drop table if exists task5_sql_redaction_probe");
            p6spyLogger.detachAppender(appender);
            appender.stop();
            p6spyLogger.setLevel(previousLevel);
            p6spyLogger.setAdditive(previousAdditive);
        }
    }

    @AfterAll
    static void restoreP6SpyConfiguration() {
        restoreSystemProperty("p6spy.config.modulelist", previousP6SpyModuleList);
        restoreSystemProperty("p6spy.config.appender", previousP6SpyAppender);
        P6ModuleManager.getInstance().reload();
    }

    private static void restoreSystemProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
            return;
        }
        System.setProperty(name, value);
    }

    static final class P6SpyLoggingInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            previousP6SpyModuleList = System.getProperty("p6spy.config.modulelist");
            previousP6SpyAppender = System.getProperty("p6spy.config.appender");
            System.setProperty(
                "p6spy.config.modulelist",
                String.join(",", P6SpyFactory.class.getName(), P6LogFactory.class.getName())
            );
            System.setProperty("p6spy.config.appender", Slf4JLogger.class.getName());
            P6ModuleManager.getInstance().reload();
        }
    }

    private ResponseEntity<String> post(String path, String body, HttpHeaders headers) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private HttpHeaders authorizationHeaders() {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(jwtTokenProvider.createAccessToken(MEMBER_ID, List.of("MEMBER")));
        return headers;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void recordTranscript(String scenario, ResponseEntity<String> response) throws JsonProcessingException {
        log.warn(
            "task5_http_qa scenario={} status={} body={}",
            scenario,
            response.getStatusCode().value(),
            RecruitingHttpTestPayloads.redactResponse(objectMapper, response.getBody())
        );
    }

    private static RecruitingApplicationCreatedInfo createdInfo() {
        return RecruitingApplicationCreatedInfo.of(900L, APPLICATION_KEY, RecruitingApplicationStatus.DRAFT);
    }

    private static RecruitingPublicApplicationInfo publicApplicationInfo() {
        return RecruitingPublicApplicationInfo.builder()
            .applicationId(900L)
            .roundId(30L)
            .seasonId(10L)
            .status(RecruitingApplicationStatus.DRAFT)
            .registrationStatus(RecruitingApplicationRegistrationStatus.NOT_READY)
            .applicantName("지원자")
            .applicantEmail(PROBE_EMAIL)
            .firstChoice(ChallengerTrack.PLAN)
            .submitted(true)
            .editable(true)
            .documentResult(RecruitingPublicResultStatus.PENDING)
            .finalResult(RecruitingPublicResultStatus.PENDING)
            .answers(List.of())
            .build();
    }

    private static RecruitingApplicationResourceInfo applicationInfo() {
        return new RecruitingApplicationResourceInfo(
            900L,
            30L,
            10L,
            RecruitingApplicationStatus.DRAFT,
            RecruitingApplicationRegistrationStatus.NOT_READY,
            ChallengerTrack.PLAN,
            null,
            null,
            true,
            false
        );
    }

}
