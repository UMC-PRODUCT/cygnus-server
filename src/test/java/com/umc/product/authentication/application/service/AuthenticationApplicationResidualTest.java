package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.event.SendVerificationEmailEvent;
import com.umc.product.authentication.application.event.SendVerificationEmailEventListener;
import com.umc.product.authentication.application.port.in.command.dto.IssueAuthenticationTokensCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.in.command.dto.ValidateEmailVerificationSessionCommand;
import com.umc.product.authentication.application.port.out.DeleteRefreshTokenPort;
import com.umc.product.authentication.application.port.out.LoadEmailVerificationPort;
import com.umc.product.authentication.application.port.out.LoadMemberOAuthPort;
import com.umc.product.authentication.application.port.out.LoadRefreshTokenPort;
import com.umc.product.authentication.application.port.out.LoadSsoClientPort;
import com.umc.product.authentication.application.port.out.SaveEmailVerificationPort;
import com.umc.product.authentication.application.port.out.SaveRefreshTokenPort;
import com.umc.product.authentication.domain.EmailVerification;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.authentication.domain.MemberOAuth;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.RefreshTokenClaims;
import com.umc.product.member.application.port.in.command.ManageMemberCredentialUseCase;
import com.umc.product.member.application.port.in.query.GetMemberCredentialUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberCredentialInfo;
import com.umc.product.notification.application.port.in.SendEmailUseCase;

@DisplayName("Authentication application 잔여 계약")
class AuthenticationApplicationResidualTest {

    @Test
    @DisplayName("legacy token 발급은 access·refresh token과 refresh row를 함께 생성한다")
    void legacy_token을_발급한다() {
        JwtTokenProvider jwt = mock(JwtTokenProvider.class);
        SaveRefreshTokenPort savePort = mock(SaveRefreshTokenPort.class);
        AuthenticationTokenIssuer sut = new AuthenticationTokenIssuer(jwt, savePort);
        UUID jti = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant expiresAt = Instant.parse("2026-12-31T00:00:00Z");
        given(jwt.createAccessToken(1L, List.of(), ClientType.WEB)).willReturn("access");
        given(jwt.createRefreshToken(1L)).willReturn("refresh");
        given(jwt.parseRefreshToken("refresh")).willReturn(new RefreshTokenClaims(1L, jti, expiresAt));

        NewTokens result = sut.issue(1L, ClientType.WEB);

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        then(savePort).should().save(any());
    }

    @Test
    @DisplayName("보안 token 생성과 SHA-256 미지원 방어 경로를 검증한다")
    void secure_token과_sha256_실패를_처리한다() throws Exception {
        SecureTokenGenerator generator = new SecureTokenGenerator();
        assertThat(generator.generateAuthorizationCode()).hasSize(43);

        try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256"))
                .thenThrow(new NoSuchAlgorithmException("not available"));

            assertThatThrownBy(() -> generator.sha256Hex("value"))
                .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> new PkceVerifier().verify("a".repeat(43), "b".repeat(43)))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("재해시는 정책 갱신이 필요할 때만 수행하고 저장 실패를 로그인에 전파하지 않는다")
    void credential_rehash를_안전하게_처리한다() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        ManageMemberCredentialUseCase credentialUseCase = mock(ManageMemberCredentialUseCase.class);
        CredentialRehashService sut = new CredentialRehashService(encoder, credentialUseCase);
        MemberCredentialInfo credential = new MemberCredentialInfo(1L, "old-hash");
        given(encoder.upgradeEncoding("old-hash")).willReturn(true);
        given(encoder.encode("raw-password")).willReturn("new-hash");

        assertThatCode(() -> sut.rehashIfNeeded(credential, "raw-password")).doesNotThrowAnyException();

        willThrow(new IllegalStateException("save failed"))
            .given(credentialUseCase).changePassword(any());
        assertThatCode(() -> sut.rehashIfNeeded(credential, "raw-password")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("메일 발송 event listener는 수신자와 인증 코드를 그대로 전달한다")
    void verification_email_event를_전달한다() {
        SendEmailUseCase sendEmailUseCase = mock(SendEmailUseCase.class);
        SendVerificationEmailEventListener sut = new SendVerificationEmailEventListener(sendEmailUseCase);

        sut.handle(SendVerificationEmailEvent.of("member@example.com", "123456"));

        then(sendEmailUseCase).should().sendVerificationEmail(org.mockito.ArgumentMatchers.argThat(command ->
            command.to().equals("member@example.com") && command.verificationCode().equals("123456")));
    }

    @Test
    @DisplayName("MemberOAuth query는 단건을 DTO로 변환하고 미존재를 도메인 예외로 변환한다")
    void member_oauth_query를_처리한다() {
        LoadMemberOAuthPort loadPort = mock(LoadMemberOAuthPort.class);
        MemberOAuthQueryService sut = new MemberOAuthQueryService(loadPort);
        MemberOAuth oauth = MemberOAuth.builder()
            .memberId(1L)
            .provider(OAuthProvider.GOOGLE)
            .providerId("provider")
            .build();
        given(loadPort.findByProviderAndProviderId(OAuthProvider.GOOGLE, "provider"))
            .willReturn(Optional.of(oauth));
        given(loadPort.findByProviderAndProviderId(OAuthProvider.GOOGLE, "missing"))
            .willReturn(Optional.empty());

        assertThat(sut.getByProviderAndProviderId(OAuthProvider.GOOGLE, "provider").memberId()).isEqualTo(1L);
        assertThatThrownBy(() -> sut.getByProviderAndProviderId(OAuthProvider.GOOGLE, "missing"))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("AuthenticationService는 token 발급을 issuer에 위임한다")
    void issue_tokens를_위임한다() {
        AuthenticationTokenIssuer issuer = mock(AuthenticationTokenIssuer.class);
        AuthenticationService sut = authenticationService(issuer, mock(LoadEmailVerificationPort.class),
            mock(DomainEventPublisher.class));
        NewTokens tokens = NewTokens.builder().accessToken("access").refreshToken("refresh").build();
        given(issuer.issue(1L, ClientType.IOS)).willReturn(tokens);

        assertThat(sut.issueTokens(IssueAuthenticationTokensCommand.of(1L, ClientType.IOS))).isSameAs(tokens);
    }

    @Test
    @DisplayName("이메일 인증 재발급은 새 코드로 세션을 갱신하고 발송 event를 게시한다")
    void 이메일_인증을_재발급한다() {
        LoadEmailVerificationPort loadPort = mock(LoadEmailVerificationPort.class);
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        AuthenticationService sut = authenticationService(mock(AuthenticationTokenIssuer.class), loadPort, publisher);
        EmailVerification verification = verification();
        given(loadPort.getById(100L)).willReturn(verification);

        sut.resendEmailVerification(100L);

        assertThat(verification.getLastSentAt()).isNotNull();
        then(publisher).should().publish(any(SendVerificationEmailEvent.class));
    }

    @Test
    @DisplayName("최근 발송 세션은 재발급을 제한하고 코드 없는 검증 요청은 즉시 거부한다")
    void 이메일_인증_재발급과_검증을_방어한다() {
        LoadEmailVerificationPort loadPort = mock(LoadEmailVerificationPort.class);
        AuthenticationService sut = authenticationService(mock(AuthenticationTokenIssuer.class), loadPort,
            mock(DomainEventPublisher.class));
        EmailVerification verification = verification();
        verification.markSent();
        given(loadPort.getById(100L)).willReturn(verification);

        assertThatThrownBy(() -> sut.resendEmailVerification(100L))
            .isInstanceOf(AuthenticationDomainException.class);
        assertThatThrownBy(() -> sut.validateEmailVerificationSession(
            new ValidateEmailVerificationSessionCommand(100L, null)))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    private AuthenticationService authenticationService(
        AuthenticationTokenIssuer issuer,
        LoadEmailVerificationPort loadEmailVerificationPort,
        DomainEventPublisher publisher
    ) {
        return new AuthenticationService(
            loadEmailVerificationPort,
            mock(SaveEmailVerificationPort.class),
            mock(LoadRefreshTokenPort.class),
            mock(DeleteRefreshTokenPort.class),
            mock(LoadSsoClientPort.class),
            mock(JwtTokenProvider.class),
            issuer,
            mock(GetMemberCredentialUseCase.class),
            publisher
        );
    }

    private EmailVerification verification() {
        EmailVerification verification = EmailVerification.builder()
            .email("member@example.com")
            .code("123456")
            .token("verification-token")
            .purpose(EmailVerificationPurpose.REGISTER)
            .build();
        ReflectionTestUtils.setField(verification, "id", 100L);
        return verification;
    }
}
