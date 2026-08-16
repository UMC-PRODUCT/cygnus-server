package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.application.event.SsoAuthorizationCodeExchangedEvent;
import com.umc.product.authentication.application.event.SsoTokenIssuedEvent;
import com.umc.product.authentication.application.port.in.command.dto.ExchangeSsoAuthorizationCodeCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.in.command.dto.SsoTokenInfo;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authentication.application.port.in.query.dto.MemberOAuthInfo;
import com.umc.product.authentication.application.port.out.LoadSsoAuthorizationCodePort;
import com.umc.product.authentication.application.port.out.LoadSsoClientPort;
import com.umc.product.authentication.domain.PkceChallengeMethod;
import com.umc.product.authentication.domain.SsoAuthorizationCode;
import com.umc.product.authentication.domain.SsoClient;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("SsoTokenExchangeCommandService")
class SsoTokenExchangeCommandServiceTest {

    private static final Long MEMBER_ID = 10L;
    private static final String CLIENT_ID = "backoffice";
    private static final String REDIRECT_URI = "https://backoffice.university.neordinary.com/auth/callback";
    private static final String RAW_CODE = "raw-authorization-code";
    private static final String CODE_VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";
    private static final String CODE_CHALLENGE = base64UrlSha256(CODE_VERIFIER);

    @Mock
    LoadSsoAuthorizationCodePort loadSsoAuthorizationCodePort;

    @Mock
    LoadSsoClientPort loadSsoClientPort;

    @Mock
    AuthenticationTokenIssuer authenticationTokenIssuer;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetMemberOAuthUseCase getMemberOAuthUseCase;

    @Mock
    DomainEventPublisher eventPublisher;

    @Test
    @DisplayName("authorization_code 교환 성공 시 code를 1회 소비하고 client context가 포함된 토큰과 회원 정보를 반환한다")
    void authorization_code_교환_성공() {
        // given
        SsoTokenExchangeCommandService service = service();
        SsoAuthorizationCode authorizationCode = authorizationCode(Instant.now().plusSeconds(300));
        SsoClient client = ssoClient(REDIRECT_URI);
        ClientContextClaims expectedClientContext = ClientContextClaims.of(
            CLIENT_ID,
            ClientServiceType.UMC_BACKOFFICE,
            ClientEnvironment.PROD
        );

        given(loadSsoAuthorizationCodePort.findByCodeHashForUpdate(sha256Hex(RAW_CODE)))
            .willReturn(Optional.of(authorizationCode));
        given(loadSsoClientPort.getByClientId(CLIENT_ID)).willReturn(client);
        given(authenticationTokenIssuer.issue(MEMBER_ID, ClientType.WEB, expectedClientContext, Duration.ofHours(1)))
            .willReturn(NewTokens.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .clientContextClaims(expectedClientContext)
                .build());
        given(getMemberUseCase.getById(MEMBER_ID)).willReturn(memberInfo());
        given(getMemberOAuthUseCase.getOAuthList(MEMBER_ID)).willReturn(List.of(
            MemberOAuthInfo.builder()
                .memberOAuthId(1L)
                .memberId(MEMBER_ID)
                .provider(OAuthProvider.GOOGLE)
                .build()
        ));

        // when
        SsoTokenInfo result = service.exchange(command("authorization_code", REDIRECT_URI, CODE_VERIFIER));

        // then
        assertThat(authorizationCode.getUsedAt()).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.expiresIn()).isEqualTo(3600L);
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.member().id()).isEqualTo(MEMBER_ID);
        assertThat(result.member().name()).isEqualTo("홍길동");
        assertThat(result.member().nickname()).isEqualTo("길동");
        assertThat(result.member().email()).isEqualTo("gildong@example.com");
        assertThat(result.linkedOAuthProviders()).containsExactly(OAuthProvider.GOOGLE);

        ArgumentCaptor<ClientContextClaims> contextCaptor = ArgumentCaptor.forClass(ClientContextClaims.class);
        then(authenticationTokenIssuer).should()
            .issue(eq(MEMBER_ID), eq(ClientType.WEB), contextCaptor.capture(), eq(Duration.ofHours(1)));
        assertThat(contextCaptor.getValue()).isEqualTo(expectedClientContext);

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        then(eventPublisher).should(times(2)).publish(eventCaptor.capture());

        assertThat(eventCaptor.getAllValues())
            .hasSize(2)
            .satisfiesExactly(
                event -> {
                    assertThat(event).isInstanceOf(SsoAuthorizationCodeExchangedEvent.class);
                    SsoAuthorizationCodeExchangedEvent exchangedEvent =
                        (SsoAuthorizationCodeExchangedEvent) event;
                    assertThat(exchangedEvent.memberId()).isEqualTo(MEMBER_ID);
                    assertThat(exchangedEvent.clientId()).isEqualTo(CLIENT_ID);
                    assertThat(exchangedEvent.redirectUri()).isEqualTo(REDIRECT_URI);
                },
                event -> {
                    assertThat(event).isInstanceOf(SsoTokenIssuedEvent.class);
                    SsoTokenIssuedEvent issuedEvent = (SsoTokenIssuedEvent) event;
                    assertThat(issuedEvent.memberId()).isEqualTo(MEMBER_ID);
                    assertThat(issuedEvent.clientId()).isEqualTo(CLIENT_ID);
                    assertThat(issuedEvent.grantType()).isEqualTo("authorization_code");
                }
            );
    }

    @Test
    @DisplayName("iOS SSO client는 ClientType.IOS로 토큰을 발급한다")
    void ios_client_type_토큰_발급() {
        // given
        SsoTokenExchangeCommandService service = service();
        String clientId = "ios-app";
        String redirectUri = "umc-ios://auth/callback";
        SsoAuthorizationCode authorizationCode = authorizationCode(
            clientId,
            redirectUri,
            Instant.now().plusSeconds(300)
        );
        SsoClient client = SsoClient.of(
            clientId,
            "UMC iOS App",
            ClientServiceType.IOS_APP,
            ClientEnvironment.PROD,
            true,
            Duration.ofMinutes(30),
            List.of(redirectUri),
            List.of()
        );
        ClientContextClaims expectedClientContext = ClientContextClaims.of(
            clientId,
            ClientServiceType.IOS_APP,
            ClientEnvironment.PROD
        );

        given(loadSsoAuthorizationCodePort.findByCodeHashForUpdate(sha256Hex(RAW_CODE)))
            .willReturn(Optional.of(authorizationCode));
        given(loadSsoClientPort.getByClientId(clientId)).willReturn(client);
        given(authenticationTokenIssuer.issue(MEMBER_ID, ClientType.IOS, expectedClientContext, Duration.ofMinutes(30)))
            .willReturn(NewTokens.builder()
                .accessToken("ios-access-token")
                .refreshToken("ios-refresh-token")
                .expiresIn(1800L)
                .clientContextClaims(expectedClientContext)
                .build());
        given(getMemberUseCase.getById(MEMBER_ID)).willReturn(memberInfo());
        given(getMemberOAuthUseCase.getOAuthList(MEMBER_ID)).willReturn(List.of());

        // when
        SsoTokenInfo result = service.exchange(command(
            "authorization_code",
            clientId,
            redirectUri,
            CODE_VERIFIER
        ));

        // then
        assertThat(result.accessToken()).isEqualTo("ios-access-token");
        then(authenticationTokenIssuer).should()
            .issue(MEMBER_ID, ClientType.IOS, expectedClientContext, Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("grant_type이 authorization_code가 아니면 UNSUPPORTED_SSO_GRANT_TYPE 예외를 던진다")
    void grant_type_거부() {
        // given
        SsoTokenExchangeCommandService service = service();

        // when / then
        assertThatThrownBy(() -> service.exchange(command("refresh_token", REDIRECT_URI, CODE_VERIFIER)))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.UNSUPPORTED_SSO_GRANT_TYPE);

        then(loadSsoAuthorizationCodePort).shouldHaveNoInteractions();
        then(authenticationTokenIssuer).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("authorization code hash로 조회되지 않으면 INVALID_SSO_AUTHORIZATION_CODE 예외를 던진다")
    void authorization_code_없음_거부() {
        // given
        SsoTokenExchangeCommandService service = service();
        given(loadSsoAuthorizationCodePort.findByCodeHashForUpdate(sha256Hex(RAW_CODE)))
            .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.exchange(command("authorization_code", REDIRECT_URI, CODE_VERIFIER)))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_CODE);

        then(authenticationTokenIssuer).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("등록되지 않은 redirect_uri면 code를 소비하지 않고 INVALID_SSO_AUTHORIZATION_CODE 예외를 던진다")
    void redirect_uri_불일치_거부() {
        // given
        SsoTokenExchangeCommandService service = service();
        SsoAuthorizationCode authorizationCode = authorizationCode(Instant.now().plusSeconds(300));
        given(loadSsoAuthorizationCodePort.findByCodeHashForUpdate(sha256Hex(RAW_CODE)))
            .willReturn(Optional.of(authorizationCode));

        // when / then
        assertThatThrownBy(() -> service.exchange(command(
            "authorization_code",
            "https://evil.example.com/callback",
            CODE_VERIFIER
        )))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_CODE);

        assertThat(authorizationCode.getUsedAt()).isNull();
        then(authenticationTokenIssuer).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("만료된 authorization code면 EXPIRED_SSO_AUTHORIZATION_CODE 예외를 던진다")
    void authorization_code_만료_거부() {
        // given
        SsoTokenExchangeCommandService service = service();
        given(loadSsoAuthorizationCodePort.findByCodeHashForUpdate(sha256Hex(RAW_CODE)))
            .willReturn(Optional.of(authorizationCode(Instant.now().minusSeconds(1))));

        // when / then
        assertThatThrownBy(() -> service.exchange(command("authorization_code", REDIRECT_URI, CODE_VERIFIER)))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.EXPIRED_SSO_AUTHORIZATION_CODE);

        then(authenticationTokenIssuer).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PKCE 검증 실패 시 code를 소비하지 않고 토큰도 발급하지 않는다")
    void pkce_검증_실패_code_소비하지_않음() {
        // given
        SsoTokenExchangeCommandService service = service();
        SsoAuthorizationCode authorizationCode = authorizationCode(Instant.now().plusSeconds(300));
        given(loadSsoAuthorizationCodePort.findByCodeHashForUpdate(sha256Hex(RAW_CODE)))
            .willReturn(Optional.of(authorizationCode));

        // when / then
        assertThatThrownBy(() -> service.exchange(command("authorization_code", REDIRECT_URI, "invalid-verifier")))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_SSO_PKCE);

        assertThat(authorizationCode.getUsedAt()).isNull();
        then(authenticationTokenIssuer).should(never()).issue(any(), any(), any(), any());
        then(eventPublisher).shouldHaveNoInteractions();
    }

    private SsoTokenExchangeCommandService service() {
        return new SsoTokenExchangeCommandService(
            loadSsoAuthorizationCodePort,
            loadSsoClientPort,
            new SecureTokenGenerator(),
            new PkceVerifier(),
            authenticationTokenIssuer,
            getMemberUseCase,
            getMemberOAuthUseCase,
            eventPublisher
        );
    }

    private ExchangeSsoAuthorizationCodeCommand command(
        String grantType,
        String redirectUri,
        String codeVerifier
    ) {
        return command(grantType, CLIENT_ID, redirectUri, codeVerifier);
    }

    private ExchangeSsoAuthorizationCodeCommand command(
        String grantType,
        String clientId,
        String redirectUri,
        String codeVerifier
    ) {
        return ExchangeSsoAuthorizationCodeCommand.of(
            grantType,
            RAW_CODE,
            clientId,
            redirectUri,
            codeVerifier
        );
    }

    private SsoAuthorizationCode authorizationCode(Instant expiresAt) {
        return authorizationCode(CLIENT_ID, REDIRECT_URI, expiresAt);
    }

    private SsoAuthorizationCode authorizationCode(String clientId, String redirectUri, Instant expiresAt) {
        return SsoAuthorizationCode.create(
            sha256Hex(RAW_CODE),
            MEMBER_ID,
            clientId,
            redirectUri,
            CODE_CHALLENGE,
            PkceChallengeMethod.S256,
            expiresAt
        );
    }

    private SsoClient ssoClient(String redirectUri) {
        return SsoClient.of(
            CLIENT_ID,
            "UMC Backoffice",
            ClientServiceType.UMC_BACKOFFICE,
            ClientEnvironment.PROD,
            true,
            Duration.ofHours(1),
            List.of(redirectUri),
            List.of("https://backoffice.university.neordinary.com")
        );
    }

    private MemberInfo memberInfo() {
        return MemberInfo.builder()
            .id(MEMBER_ID)
            .name("홍길동")
            .nickname("길동")
            .email("gildong@example.com")
            .status(MemberStatus.ACTIVE)
            .build();
    }

    private static String sha256Hex(String value) {
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
