package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.umc.product.authentication.application.event.SsoAuthorizationCodeIssuedEvent;
import com.umc.product.authentication.application.port.in.command.dto.AuthorizeSsoCommand;
import com.umc.product.authentication.application.port.in.command.dto.SsoAuthorizationRedirectInfo;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;
import com.umc.product.authentication.application.port.in.query.GetSsoBrowserLoginUseCase;
import com.umc.product.authentication.application.port.out.LoadSsoClientPort;
import com.umc.product.authentication.application.port.out.SaveSsoAuthorizationCodePort;
import com.umc.product.authentication.config.SsoProperties;
import com.umc.product.authentication.domain.PkceChallengeMethod;
import com.umc.product.authentication.domain.SsoAuthorizationCode;
import com.umc.product.authentication.domain.SsoClient;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("SsoAuthorizationCommandService")
class SsoAuthorizationCommandServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String CLIENT_ID = "backoffice";
    private static final String REDIRECT_URI = "https://backoffice.university.neordinary.com/auth/callback";
    private static final String RAW_LOGIN_TOKEN = "sso-login-token";
    private static final String CODE_VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";
    private static final String CODE_CHALLENGE = base64UrlSha256(CODE_VERIFIER);
    private static final String STATE = "state with space+symbols";

    @Mock
    LoadSsoClientPort loadSsoClientPort;

    @Mock
    SaveSsoAuthorizationCodePort saveSsoAuthorizationCodePort;

    @Mock
    GetSsoBrowserLoginUseCase getSsoBrowserLoginUseCase;

    @Mock
    DomainEventPublisher eventPublisher;

    SsoAuthorizationCommandService service;

    @BeforeEach
    void setUp() {
        SsoProperties properties = new SsoProperties(
            URI.create("https://api.university.neordinary.com"),
            Duration.ofMinutes(3),
            Duration.ofHours(12),
            new SsoProperties.Cookie("UMC_SSO_LOGIN", "", true, "Lax"),
            Map.of()
        );
        service = new SsoAuthorizationCommandService(
            loadSsoClientPort,
            saveSsoAuthorizationCodePort,
            getSsoBrowserLoginUseCase,
            properties,
            new SecureTokenGenerator(),
            new PkceVerifier(),
            eventPublisher
        );
    }

    @Test
    @DisplayName("authorize 성공 시 raw code는 redirect에만 노출하고 SHA-256 hash만 저장한다")
    void authorize_성공_raw_code_redirect_hash_저장() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));
        given(getSsoBrowserLoginUseCase.getLogin(RAW_LOGIN_TOKEN))
            .willReturn(SsoBrowserLoginInfo.of(MEMBER_ID, RAW_LOGIN_TOKEN, Instant.now().plusSeconds(3600)));
        given(saveSsoAuthorizationCodePort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        SsoAuthorizationRedirectInfo result = service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            "https://backoffice.university.neordinary.com"
        ));

        // then
        String redirected = result.redirectUri();
        String rawCode = UriComponentsBuilder.fromUriString(redirected)
            .build()
            .getQueryParams()
            .getFirst("code");
        String returnedState = UriComponentsBuilder.fromUriString(redirected)
            .build()
            .getQueryParams()
            .getFirst("state");

        assertThat(rawCode).isNotBlank();
        assertThat(UriUtils.decode(returnedState, StandardCharsets.UTF_8)).isEqualTo(STATE);
        assertThat(redirected).contains("state=state%20with%20space%2Bsymbols");
        assertThat(redirected).startsWith(REDIRECT_URI);

        ArgumentCaptor<SsoAuthorizationCode> codeCaptor = ArgumentCaptor.forClass(SsoAuthorizationCode.class);
        then(saveSsoAuthorizationCodePort).should().save(codeCaptor.capture());
        then(getSsoBrowserLoginUseCase).should().getLogin(RAW_LOGIN_TOKEN);

        SsoAuthorizationCode savedCode = codeCaptor.getValue();
        assertThat(savedCode.getCodeHash()).isEqualTo(sha256Hex(rawCode));
        assertThat(savedCode.getCodeHash()).isNotEqualTo(rawCode);
        assertThat(savedCode.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(savedCode.getRedirectUri()).isEqualTo(REDIRECT_URI);
        assertThat(savedCode.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(savedCode.getCodeChallenge()).isEqualTo(CODE_CHALLENGE);
        assertThat(savedCode.getCodeChallengeMethod()).isEqualTo(PkceChallengeMethod.S256);
        assertThat(savedCode.getExpiresAt()).isAfter(Instant.now().plusSeconds(170));

        ArgumentCaptor<SsoAuthorizationCodeIssuedEvent> eventCaptor =
            ArgumentCaptor.forClass(SsoAuthorizationCodeIssuedEvent.class);
        then(eventPublisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().memberId()).isEqualTo(MEMBER_ID);
        assertThat(eventCaptor.getValue().clientId()).isEqualTo(CLIENT_ID);
        assertThat(eventCaptor.getValue().redirectUri()).isEqualTo(REDIRECT_URI);
        assertThat(eventCaptor.getValue().eventType()).isEqualTo("authentication.sso.authorization-code.issued");
    }

    @Test
    @DisplayName("등록되지 않은 redirect URI면 authorization code를 저장하지 않고 예외를 던진다")
    void 등록되지_않은_redirect_uri_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            "https://evil.example.com/auth/callback",
            "code",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            "https://evil.example.com"
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_REDIRECT_URI);

        then(saveSsoAuthorizationCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("client의 requirePkce가 true이면 client 로드 후 PKCE S256 정책을 적용한다")
    void client_requirePkce_true_로드_후_pkce_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            " ",
            "S256",
            RAW_LOGIN_TOKEN,
            "https://backoffice.university.neordinary.com"
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);

        InOrder inOrder = inOrder(loadSsoClientPort, getSsoBrowserLoginUseCase);
        inOrder.verify(loadSsoClientPort).getByClientId(CLIENT_ID);
        then(getSsoBrowserLoginUseCase).shouldHaveNoInteractions();
        then(saveSsoAuthorizationCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("client의 requirePkce가 false이면 지원하지 않는 SSO client 정책으로 거부한다")
    void client_requirePkce_false_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI, false));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            "https://backoffice.university.neordinary.com"
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_CLIENT);

        then(getSsoBrowserLoginUseCase).shouldHaveNoInteractions();
        then(saveSsoAuthorizationCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("registered web client는 허용되지 않은 browser Origin을 거부한다")
    void 허용되지_않은_browser_origin_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            "https://evil.example.com"
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_REQUEST);

        then(getSsoBrowserLoginUseCase).shouldHaveNoInteractions();
        then(saveSsoAuthorizationCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("registered web client는 Origin이 허용되어도 Referer origin이 허용되지 않으면 거부한다")
    void 허용되지_않은_referer_origin_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.withRequestOrigins(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            List.of(
                "https://backoffice.university.neordinary.com",
                "https://evil.example.com"
            )
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_REQUEST);

        then(getSsoBrowserLoginUseCase).shouldHaveNoInteractions();
        then(saveSsoAuthorizationCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("registered web client는 Origin과 Referer가 모두 없어도 거부하지 않는다")
    void web_client_origin_referer_없음_허용() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));
        given(getSsoBrowserLoginUseCase.getLogin(RAW_LOGIN_TOKEN))
            .willReturn(SsoBrowserLoginInfo.of(MEMBER_ID, RAW_LOGIN_TOKEN, Instant.now().plusSeconds(3600)));
        given(saveSsoAuthorizationCodePort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        SsoAuthorizationRedirectInfo result = service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            null
        ));

        // then
        assertThat(result.redirectUri()).startsWith(REDIRECT_URI);
        then(saveSsoAuthorizationCodePort).should().save(any());
    }

    @Test
    @DisplayName("allowed origin이 비어 있는 app client는 Origin이 없어도 browser origin 검증으로 거부하지 않는다")
    void app_client_origin_없음_허용() {
        // given
        String appRedirectUri = "umc-ios://auth/callback";
        given(loadSsoClientPort.getByClientId("ios-app")).willReturn(SsoClient.of(
            "ios-app",
            "UMC iOS App",
            ClientServiceType.IOS_APP,
            ClientEnvironment.PROD,
            true,
            Duration.ofHours(1),
            List.of(appRedirectUri),
            List.of()
        ));
        given(getSsoBrowserLoginUseCase.getLogin(RAW_LOGIN_TOKEN))
            .willReturn(SsoBrowserLoginInfo.of(MEMBER_ID, RAW_LOGIN_TOKEN, Instant.now().plusSeconds(3600)));
        given(saveSsoAuthorizationCodePort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        SsoAuthorizationRedirectInfo result = service.authorize(AuthorizeSsoCommand.of(
            "ios-app",
            appRedirectUri,
            "code",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            null
        ));

        // then
        assertThat(result.redirectUri()).startsWith(appRedirectUri);
        then(saveSsoAuthorizationCodePort).should().save(any());
    }

    @Test
    @DisplayName("allowed origin이 비어 있는 app client는 browser origin이 있어도 거부하지 않는다")
    void app_client_origin_있어도_allowedOrigins_비어있으면_허용() {
        // given
        String appRedirectUri = "umc-ios://auth/callback";
        given(loadSsoClientPort.getByClientId("ios-app")).willReturn(SsoClient.of(
            "ios-app",
            "UMC iOS App",
            ClientServiceType.IOS_APP,
            ClientEnvironment.PROD,
            true,
            Duration.ofHours(1),
            List.of(appRedirectUri),
            List.of()
        ));
        given(getSsoBrowserLoginUseCase.getLogin(RAW_LOGIN_TOKEN))
            .willReturn(SsoBrowserLoginInfo.of(MEMBER_ID, RAW_LOGIN_TOKEN, Instant.now().plusSeconds(3600)));
        given(saveSsoAuthorizationCodePort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        SsoAuthorizationRedirectInfo result = service.authorize(AuthorizeSsoCommand.of(
            "ios-app",
            appRedirectUri,
            "code",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            "https://unexpected.example.com"
        ));

        // then
        assertThat(result.redirectUri()).startsWith(appRedirectUri);
        then(saveSsoAuthorizationCodePort).should().save(any());
    }

    @Test
    @DisplayName("allowed origin이 비어 있는 web client는 지원하지 않는 SSO client 정책으로 거부한다")
    void web_client_allowedOrigins_비어있으면_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(SsoClient.of(
            CLIENT_ID,
            "UMC Backoffice",
            ClientServiceType.UMC_BACKOFFICE,
            ClientEnvironment.PROD,
            true,
            Duration.ofHours(1),
            List.of(REDIRECT_URI),
            List.of()
        ));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            "https://backoffice.university.neordinary.com"
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_CLIENT);

        then(getSsoBrowserLoginUseCase).shouldHaveNoInteractions();
        then(saveSsoAuthorizationCodePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("response_type이 code가 아니면 authorize 요청을 거부한다")
    void response_type_code_아님_거부() {
        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "token",
            STATE,
            CODE_CHALLENGE,
            "S256",
            RAW_LOGIN_TOKEN,
            null
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_REQUEST);
    }

    @Test
    @DisplayName("PKCE challenge method가 S256이 아니면 요청을 거부한다")
    void pkce_method_S256_아님_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            CODE_CHALLENGE,
            "plain",
            RAW_LOGIN_TOKEN,
            null
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);
    }

    @Test
    @DisplayName("PKCE challenge가 blank이면 INVALID_SSO_PKCE 예외를 던진다")
    void pkce_challenge_blank_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            " ",
            "S256",
            RAW_LOGIN_TOKEN,
            null
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);
    }

    @Test
    @DisplayName("PKCE challenge가 43자 미만이면 INVALID_SSO_PKCE 예외를 던진다")
    void pkce_challenge_짧으면_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            "short",
            "S256",
            RAW_LOGIN_TOKEN,
            null
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);
    }

    @Test
    @DisplayName("PKCE challenge에 공백이 포함되면 INVALID_SSO_PKCE 예외를 던진다")
    void pkce_challenge_공백_포함_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            "abcdefghijklmnopqrstuvwxyzABCDEFGHI JKLMNOPQRSTUVWXYZ0123456789",
            "S256",
            RAW_LOGIN_TOKEN,
            null
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);
    }

    @Test
    @DisplayName("PKCE challenge가 128자를 초과하면 INVALID_SSO_PKCE 예외를 던진다")
    void pkce_challenge_너무_길면_거부() {
        // given
        String tooLongChallenge = "a".repeat(129);
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            tooLongChallenge,
            "S256",
            RAW_LOGIN_TOKEN,
            null
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);
    }

    @Test
    @DisplayName("PKCE challenge method가 blank이면 INVALID_SSO_PKCE 예외를 던진다")
    void pkce_method_blank_거부() {
        // given
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(ssoClient(REDIRECT_URI));

        // when & then
        assertThatThrownBy(() -> service.authorize(AuthorizeSsoCommand.of(
            CLIENT_ID,
            REDIRECT_URI,
            "code",
            STATE,
            CODE_CHALLENGE,
            " ",
            RAW_LOGIN_TOKEN,
            null
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);
    }

    @Test
    @DisplayName("PKCE verifier는 code verifier로 S256 challenge를 검증한다")
    void pkce_verifier_성공() {
        // given
        PkceVerifier verifier = new PkceVerifier();

        // when
        verifier.verify(CODE_VERIFIER, CODE_CHALLENGE);

        // then
        assertThat(verifier.requireS256("S256")).isEqualTo(PkceChallengeMethod.S256);
    }

    @Test
    @DisplayName("PKCE verifier는 verifier 형식 오류와 challenge 불일치를 INVALID_SSO_PKCE로 거부한다")
    void pkce_verifier_실패() {
        // given
        PkceVerifier verifier = new PkceVerifier();

        // when & then
        assertThatThrownBy(() -> verifier.verify("short", CODE_CHALLENGE))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);

        assertThatThrownBy(() -> verifier.verify(CODE_VERIFIER, "mismatch"))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);
    }

    @Test
    @DisplayName("SecureTokenGenerator는 SHA-256 hash 대상 값이 null이면 명시적인 예외 메시지로 실패한다")
    void secure_token_generator_sha256_null_거부() {
        // given
        SecureTokenGenerator generator = new SecureTokenGenerator();

        // when & then
        assertThatThrownBy(() -> generator.sha256Hex(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("value must not be null");
    }

    private SsoClient ssoClient(String redirectUri) {
        return ssoClient(redirectUri, true);
    }

    private SsoClient ssoClient(String redirectUri, boolean requirePkce) {
        return SsoClient.of(
            CLIENT_ID,
            "UMC Backoffice",
            ClientServiceType.UMC_BACKOFFICE,
            ClientEnvironment.PROD,
            requirePkce,
            Duration.ofHours(1),
            List.of(redirectUri),
            List.of("https://backoffice.university.neordinary.com")
        );
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64UrlSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
