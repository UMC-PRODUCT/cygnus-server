package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.event.SendVerificationEmailEvent;
import com.umc.product.authentication.application.port.in.command.dto.LogoutCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.in.command.dto.RenewAccessTokenCommand;
import com.umc.product.authentication.application.port.in.command.dto.ValidateEmailVerificationSessionCommand;
import com.umc.product.authentication.application.port.out.DeleteRefreshTokenPort;
import com.umc.product.authentication.application.port.out.LoadEmailVerificationPort;
import com.umc.product.authentication.application.port.out.LoadRefreshTokenPort;
import com.umc.product.authentication.application.port.out.LoadSsoClientPort;
import com.umc.product.authentication.application.port.out.SaveEmailVerificationPort;
import com.umc.product.authentication.domain.EmailVerification;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.authentication.domain.RefreshToken;
import com.umc.product.authentication.domain.SsoClient;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientEnvironment;
import com.umc.product.global.client.ClientServiceType;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.RefreshTokenClaims;
import com.umc.product.member.application.port.in.query.GetMemberCredentialUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberCredentialInfo;

/**
 * AuthenticationService 단위 테스트.
 * <p>
 * 이메일 인증 핫픽스 (C1~C4) 의 서비스 계층 동작을 검증한다:
 * - send 시 이메일 형식 검증 (CredentialPolicy)
 * - validate 시 세션 purpose 가 토큰 purpose 로 그대로 이어지는지
 * - validate 시 코드 불일치 → 도메인 예외 그대로 전달 (트랜잭션은 noRollbackFor 로 attempt_count 보존)
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String EMAIL = "alice@example.com";
    private static final Long SESSION_ID = 100L;
    private static final Long MEMBER_ID = 1L;
    private static final String CODE = "123456";
    private static final String TOKEN = "22222222-2222-2222-2222-222222222222";
    private static final String ISSUED_TOKEN = "issued.jwt.token";
    private static final String REFRESH_TOKEN = "refresh.jwt.token";
    private static final UUID REFRESH_JTI = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant REFRESH_EXPIRES_AT = Instant.now().plusSeconds(86_400);

    @Mock
    LoadEmailVerificationPort loadEmailVerificationPort;

    @Mock
    SaveEmailVerificationPort saveEmailVerificationPort;

    @Mock
    LoadRefreshTokenPort loadRefreshTokenPort;

    @Mock
    DeleteRefreshTokenPort deleteRefreshTokenPort;

    @Mock
    LoadSsoClientPort loadSsoClientPort;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    AuthenticationTokenIssuer authenticationTokenIssuer;

    @Mock
    GetMemberCredentialUseCase getMemberCredentialUseCase;

    @Mock
    DomainEventPublisher eventPublisher;

    @InjectMocks
    AuthenticationService service;

    private EmailVerification newSession(EmailVerificationPurpose purpose) {
        return EmailVerification.builder()
            .email(EMAIL)
            .code(CODE)
            .token(TOKEN)
            .purpose(purpose)
            .build();
    }

    @Nested
    @DisplayName("renewAccessToken")
    class RenewAccessToken {

        @Test
        @DisplayName("legacy RefreshToken이면 기존 jti를 삭제하고 legacy 경로로 새 토큰을 발급한다")
        void refresh_token_회전_성공() {
            // given
            RefreshTokenClaims claims = new RefreshTokenClaims(MEMBER_ID, REFRESH_JTI, REFRESH_EXPIRES_AT);
            RefreshToken stored = RefreshToken.create(REFRESH_JTI, MEMBER_ID, REFRESH_EXPIRES_AT);
            NewTokens newTokens = NewTokens.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

            given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(claims);
            given(loadRefreshTokenPort.findByJti(REFRESH_JTI)).willReturn(Optional.of(stored));
            given(deleteRefreshTokenPort.deleteByJti(REFRESH_JTI)).willReturn(true);
            given(authenticationTokenIssuer.issue(eq(MEMBER_ID), eq(null))).willReturn(newTokens);

            // when
            NewTokens result = service.renewAccessToken(
                RenewAccessTokenCommand.builder()
                    .refreshToken(REFRESH_TOKEN)
                    .build()
            );

            // then
            assertThat(result).isSameAs(newTokens);
            then(deleteRefreshTokenPort).should().deleteByJti(REFRESH_JTI);
            then(authenticationTokenIssuer).should().issue(MEMBER_ID, null);
            then(loadSsoClientPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("SSO RefreshToken이면 client context를 보존해서 새 토큰을 발급한다")
        void sso_refresh_token_context_보존_재발급() {
            // given
            String clientId = "backoffice";
            ClientContextClaims refreshTokenContext = ClientContextClaims.of(
                clientId,
                ClientServiceType.UNKNOWN,
                ClientEnvironment.UNKNOWN
            );
            ClientContextClaims expectedClientContext = ClientContextClaims.of(
                clientId,
                ClientServiceType.UMC_BACKOFFICE,
                ClientEnvironment.PROD
            );
            RefreshTokenClaims claims = new RefreshTokenClaims(
                MEMBER_ID,
                REFRESH_JTI,
                REFRESH_EXPIRES_AT,
                refreshTokenContext
            );
            RefreshToken stored = RefreshToken.create(REFRESH_JTI, MEMBER_ID, REFRESH_EXPIRES_AT, clientId);
            SsoClient client = SsoClient.of(
                clientId,
                "UMC Backoffice",
                ClientServiceType.UMC_BACKOFFICE,
                ClientEnvironment.PROD,
                true,
                Duration.ofHours(1),
                List.of("https://backoffice.university.neordinary.com/auth/callback"),
                List.of("https://backoffice.university.neordinary.com")
            );
            NewTokens newTokens = NewTokens.builder()
                .accessToken("new-sso-access-token")
                .refreshToken("new-sso-refresh-token")
                .clientContextClaims(expectedClientContext)
                .expiresIn(3600L)
                .build();

            given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(claims);
            given(loadRefreshTokenPort.findByJti(REFRESH_JTI)).willReturn(Optional.of(stored));
            given(deleteRefreshTokenPort.deleteByJti(REFRESH_JTI)).willReturn(true);
            given(loadSsoClientPort.getByClientId(clientId)).willReturn(client);
            given(authenticationTokenIssuer.issue(
                eq(MEMBER_ID),
                isNull(),
                eq(expectedClientContext),
                eq(Duration.ofHours(1))
            )).willReturn(newTokens);

            // when
            NewTokens result = service.renewAccessToken(
                RenewAccessTokenCommand.builder()
                    .refreshToken(REFRESH_TOKEN)
                    .build()
            );

            // then
            assertThat(result).isSameAs(newTokens);
            then(deleteRefreshTokenPort).should().deleteByJti(REFRESH_JTI);
            then(loadSsoClientPort).should().getByClientId(clientId);
            then(authenticationTokenIssuer).should().issue(
                MEMBER_ID,
                null,
                expectedClientContext,
                Duration.ofHours(1)
            );
            then(authenticationTokenIssuer).should(never()).issue(MEMBER_ID, null);
        }

        @Test
        @DisplayName("SSO RefreshToken의 stored clientId와 claim clientId가 다르면 갱신을 거부한다")
        void sso_refresh_token_client_id_mismatch_거부() {
            // given
            String claimClientId = "backoffice";
            RefreshTokenClaims claims = new RefreshTokenClaims(
                MEMBER_ID,
                REFRESH_JTI,
                REFRESH_EXPIRES_AT,
                ClientContextClaims.of(
                    claimClientId,
                    ClientServiceType.UMC_BACKOFFICE,
                    ClientEnvironment.PROD
                )
            );
            RefreshToken stored = RefreshToken.create(
                REFRESH_JTI,
                MEMBER_ID,
                REFRESH_EXPIRES_AT,
                "website"
            );

            given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(claims);
            given(loadRefreshTokenPort.findByJti(REFRESH_JTI)).willReturn(Optional.of(stored));

            // when / then
            assertThatThrownBy(() -> service.renewAccessToken(
                RenewAccessTokenCommand.builder()
                    .refreshToken(REFRESH_TOKEN)
                    .build()
            ))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);

            then(deleteRefreshTokenPort).should(never()).deleteByJti(any());
            then(loadSsoClientPort).shouldHaveNoInteractions();
            then(authenticationTokenIssuer).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("조회 후 삭제할 RefreshToken row가 없으면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void 동시_갱신으로_이미_삭제된_refresh_token_거부() {
            // given
            RefreshTokenClaims claims = new RefreshTokenClaims(MEMBER_ID, REFRESH_JTI, REFRESH_EXPIRES_AT);
            RefreshToken stored = RefreshToken.create(REFRESH_JTI, MEMBER_ID, REFRESH_EXPIRES_AT);

            given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(claims);
            given(loadRefreshTokenPort.findByJti(REFRESH_JTI)).willReturn(Optional.of(stored));
            given(deleteRefreshTokenPort.deleteByJti(REFRESH_JTI)).willReturn(false);

            // when / then
            assertThatThrownBy(() -> service.renewAccessToken(
                RenewAccessTokenCommand.builder()
                    .refreshToken(REFRESH_TOKEN)
                    .build()
            ))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);

            then(deleteRefreshTokenPort).should().deleteByJti(REFRESH_JTI);
            then(authenticationTokenIssuer).should(never()).issue(any(), any());
        }

        @Test
        @DisplayName("allow-list에 없는 RefreshToken이면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void 저장되지_않은_refresh_token_거부() {
            // given
            RefreshTokenClaims claims = new RefreshTokenClaims(MEMBER_ID, REFRESH_JTI, REFRESH_EXPIRES_AT);
            given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(claims);
            given(loadRefreshTokenPort.findByJti(REFRESH_JTI)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> service.renewAccessToken(
                RenewAccessTokenCommand.builder()
                    .refreshToken(REFRESH_TOKEN)
                    .build()
            ))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);

            then(deleteRefreshTokenPort).should(never()).deleteByJti(any());
            then(authenticationTokenIssuer).should(never()).issue(any(), any());
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("RefreshToken의 jti를 삭제한다")
        void refresh_token_삭제() {
            // given
            RefreshTokenClaims claims = new RefreshTokenClaims(MEMBER_ID, REFRESH_JTI, REFRESH_EXPIRES_AT);
            given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(claims);

            // when
            service.logout(LogoutCommand.from(REFRESH_TOKEN));

            // then
            then(deleteRefreshTokenPort).should().deleteByJti(REFRESH_JTI);
        }

        @Test
        @DisplayName("이미 삭제된 RefreshToken이어도 멱등하게 성공한다")
        void 이미_삭제된_refresh_token_로그아웃_성공() {
            // given
            RefreshTokenClaims claims = new RefreshTokenClaims(MEMBER_ID, REFRESH_JTI, REFRESH_EXPIRES_AT);
            given(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).willReturn(claims);

            // when / then
            assertThatCode(() -> service.logout(LogoutCommand.from(REFRESH_TOKEN)))
                .doesNotThrowAnyException();
            then(deleteRefreshTokenPort).should().deleteByJti(REFRESH_JTI);
        }
    }

    @Nested
    @DisplayName("createEmailVerificationSession")
    class CreateSession {

        @Test
        @DisplayName("잘못된 이메일 형식이면 INVALID_EMAIL_FORMAT 예외를 던지고 저장/이벤트 발행하지 않는다")
        void 잘못된_이메일_거부() {
            // given
            // when / then
            assertThatThrownBy(() -> service.createEmailVerificationSession("not-an-email",
                EmailVerificationPurpose.REGISTER))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_FORMAT);

            then(saveEmailVerificationPort).should(never()).save(any());
            then(eventPublisher).should(never()).publish(any(SendVerificationEmailEvent.class));
        }

        @Test
        @DisplayName("REGISTER + 미가입 이메일이면 세션을 저장하고 메일 발송 이벤트를 발행한다")
        void REGISTER_정상_세션_생성() {
            // given
            given(getMemberCredentialUseCase.existsByEmail(EMAIL)).willReturn(false);
            EmailVerification persisted = newSession(EmailVerificationPurpose.REGISTER);
            ReflectionTestUtils.setField(persisted, "id", SESSION_ID);
            given(saveEmailVerificationPort.save(any(EmailVerification.class))).willReturn(persisted);

            // when
            Long sessionId = service.createEmailVerificationSession(EMAIL, EmailVerificationPurpose.REGISTER);

            // then
            assertThat(sessionId).isEqualTo(SESSION_ID);
            then(eventPublisher).should().publish(any(SendVerificationEmailEvent.class));
        }

        @Test
        @DisplayName("REGISTER + 이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS 예외, 저장/이벤트 발행 모두 차단")
        void REGISTER_중복_이메일_거부() {
            // given
            given(getMemberCredentialUseCase.existsByEmail(EMAIL)).willReturn(true);

            // when / then
            assertThatThrownBy(() ->
                service.createEmailVerificationSession(EMAIL, EmailVerificationPurpose.REGISTER))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.EMAIL_ALREADY_EXISTS);

            then(saveEmailVerificationPort).should(never()).save(any());
            then(eventPublisher).should(never()).publish(any(SendVerificationEmailEvent.class));
        }

        @Test
        @DisplayName("PASSWORD_RESET + 자격증명 존재하면 세션을 저장하고 메일 발송 이벤트를 발행한다")
        void PASSWORD_RESET_정상_세션_생성() {
            // given
            given(getMemberCredentialUseCase.findCredentialByEmail(EMAIL))
                .willReturn(Optional.of(new MemberCredentialInfo(1L, "encoded-password")));
            EmailVerification persisted = newSession(EmailVerificationPurpose.PASSWORD_RESET);
            ReflectionTestUtils.setField(persisted, "id", SESSION_ID);
            given(saveEmailVerificationPort.save(any(EmailVerification.class))).willReturn(persisted);

            // when
            Long sessionId = service.createEmailVerificationSession(EMAIL, EmailVerificationPurpose.PASSWORD_RESET);

            // then
            assertThat(sessionId).isEqualTo(SESSION_ID);
            then(eventPublisher).should().publish(any(SendVerificationEmailEvent.class));
        }

        @Test
        @DisplayName("CHANGE_EMAIL + 미사용 이메일이면 세션을 저장하고 메일 발송 이벤트를 발행한다")
        void changeEmailSessionCreated() {
            // given
            given(getMemberCredentialUseCase.existsByEmail(EMAIL)).willReturn(false);
            EmailVerification persisted = newSession(EmailVerificationPurpose.CHANGE_EMAIL);
            ReflectionTestUtils.setField(persisted, "id", SESSION_ID);
            given(saveEmailVerificationPort.save(any(EmailVerification.class))).willReturn(persisted);

            // when
            Long sessionId = service.createEmailVerificationSession(EMAIL, EmailVerificationPurpose.CHANGE_EMAIL);

            // then
            assertThat(sessionId).isEqualTo(SESSION_ID);
            then(eventPublisher).should().publish(any(SendVerificationEmailEvent.class));
        }

        @Test
        @DisplayName("CHANGE_EMAIL + 이미 사용 중인 이메일이면 EMAIL_ALREADY_EXISTS 예외, 저장/이벤트 발행 모두 차단")
        void changeEmailDuplicateRejected() {
            // given
            given(getMemberCredentialUseCase.existsByEmail(EMAIL)).willReturn(true);

            // when / then
            assertThatThrownBy(() ->
                service.createEmailVerificationSession(EMAIL, EmailVerificationPurpose.CHANGE_EMAIL))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.EMAIL_ALREADY_EXISTS);

            then(saveEmailVerificationPort).should(never()).save(any());
            then(eventPublisher).should(never()).publish(any(SendVerificationEmailEvent.class));
        }

        @Test
        @DisplayName("직전 발송으로부터 60초 이내면 EMAIL_VERIFICATION_THROTTLED 예외로 거부한다")
        void throttle_위반_거부() {
            // given
            given(getMemberCredentialUseCase.existsByEmail(EMAIL)).willReturn(false);
            EmailVerification recent = newSession(EmailVerificationPurpose.REGISTER);
            recent.markSent(); // 직전 발송
            given(loadEmailVerificationPort.findLatestSentByEmail(EMAIL))
                .willReturn(java.util.Optional.of(recent));

            // when / then
            assertThatThrownBy(() ->
                service.createEmailVerificationSession(EMAIL, EmailVerificationPurpose.REGISTER))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.EMAIL_VERIFICATION_THROTTLED);

            then(saveEmailVerificationPort).should(never()).save(any());
            then(eventPublisher).should(never()).publish(any(SendVerificationEmailEvent.class));
        }

        @Test
        @DisplayName("PASSWORD_RESET + 자격증명 미존재면 세션은 저장하되 이벤트는 발행하지 않는다 (enumeration 방어)")
        void PASSWORD_RESET_미가입_silent() {
            // given
            given(getMemberCredentialUseCase.findCredentialByEmail(EMAIL)).willReturn(Optional.empty());
            EmailVerification persisted = newSession(EmailVerificationPurpose.PASSWORD_RESET);
            ReflectionTestUtils.setField(persisted, "id", SESSION_ID);
            given(saveEmailVerificationPort.save(any(EmailVerification.class))).willReturn(persisted);

            // when
            Long sessionId = service.createEmailVerificationSession(EMAIL, EmailVerificationPurpose.PASSWORD_RESET);

            // then: 응답은 정상이지만 메일 이벤트는 발행되지 않음 (이메일 열거 방어)
            assertThat(sessionId).isEqualTo(SESSION_ID);
            then(eventPublisher).should(never()).publish(any(SendVerificationEmailEvent.class));
        }
    }

    @Nested
    @DisplayName("validateEmailVerificationSession")
    class ValidateSession {

        @Test
        @DisplayName("정답 코드 검증 시 세션의 purpose 로 emailVerificationToken 을 발급한다")
        void 검증_성공_시_purpose_그대로_발급() {
            // given
            EmailVerification session = newSession(EmailVerificationPurpose.PASSWORD_RESET);
            given(loadEmailVerificationPort.getById(SESSION_ID)).willReturn(session);
            given(jwtTokenProvider.createEmailVerificationToken(EMAIL, EmailVerificationPurpose.PASSWORD_RESET))
                .willReturn(ISSUED_TOKEN);

            ValidateEmailVerificationSessionCommand command =
                ValidateEmailVerificationSessionCommand.builder()
                    .sessionId(SESSION_ID)
                    .code(CODE)
                    .build();

            // when
            String issued = service.validateEmailVerificationSession(command);

            // then
            assertThat(issued).isEqualTo(ISSUED_TOKEN);
            assertThat(session.isVerified()).isTrue();
            then(jwtTokenProvider).should()
                .createEmailVerificationToken(EMAIL, EmailVerificationPurpose.PASSWORD_RESET);
        }

        @Test
        @DisplayName("틀린 코드면 INVALID_EMAIL_VERIFICATION 예외를 던지고 토큰을 발급하지 않는다")
        void 틀린_코드_시_토큰_미발급() {
            // given
            EmailVerification session = newSession(EmailVerificationPurpose.REGISTER);
            given(loadEmailVerificationPort.getById(SESSION_ID)).willReturn(session);

            ValidateEmailVerificationSessionCommand command =
                ValidateEmailVerificationSessionCommand.builder()
                    .sessionId(SESSION_ID)
                    .code("000000")
                    .build();

            // when / then
            assertThatThrownBy(() -> service.validateEmailVerificationSession(command))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);

            then(jwtTokenProvider).should(never()).createEmailVerificationToken(anyString(), any());
            assertThat(session.getAttemptCount()).isEqualTo(1);
        }
    }
}
