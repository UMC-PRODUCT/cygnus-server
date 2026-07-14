package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.authentication.application.port.in.command.dto.AccessTokenLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.AuthorizationCodeLoginCommand;
import com.umc.product.authentication.application.port.out.LoadMemberOAuthPort;
import com.umc.product.authentication.application.port.out.RevokeOAuthTokenPort;
import com.umc.product.authentication.application.port.out.SaveMemberOAuthPort;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.command.LockMemberCredentialUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth 로그인 실패 감사")
class OAuthAuthenticationFailureAuditTest {

    private static final String ACCESS_TOKEN = "raw-oauth-access-token-secret";
    private static final String AUTHORIZATION_CODE = "raw-authorization-code-secret";
    private static final String REDIRECT_URI = "https://client.example/callback";

    @Mock
    VerifyOAuthTokenPort verifyOAuthTokenPort;
    @Mock
    LoadMemberOAuthPort loadMemberOAuthPort;
    @Mock
    SaveMemberOAuthPort saveMemberOAuthPort;
    @Mock
    RevokeOAuthTokenPort revokeOAuthTokenPort;
    @Mock
    LockMemberCredentialUseCase lockMemberCredentialUseCase;
    @Mock
    OperationalMetrics operationalMetrics;
    @Mock
    RecordAuditLogUseCase recordAuditLogUseCase;

    OAuthAuthenticationService sut;

    @BeforeEach
    void setUp() {
        OAuthLoginAuditRecorder auditRecorder = new OAuthLoginAuditRecorder(recordAuditLogUseCase);
        sut = new OAuthAuthenticationService(
            verifyOAuthTokenPort,
            loadMemberOAuthPort,
            saveMemberOAuthPort,
            lockMemberCredentialUseCase,
            operationalMetrics,
            auditRecorder,
            new OAuthTokenRevoker(revokeOAuthTokenPort, verifyOAuthTokenPort)
        );
    }

    @Test
    @DisplayName("access token 검증 실패는 credential 없이 FAILURE 감사를 기록한다")
    void accessTokenFailureRecordsSanitizedAudit() {
        given(verifyOAuthTokenPort.verify(OAuthProvider.GOOGLE, ACCESS_TOKEN))
            .willThrow(new IllegalArgumentException("invalid token"));

        assertThatThrownBy(() -> sut.accessTokenLogin(
            AccessTokenLoginCommand.of(OAuthProvider.GOOGLE, ACCESS_TOKEN)
        )).isInstanceOf(IllegalArgumentException.class);

        assertFailureAuditDoesNotContain(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("authorization code 교환 실패는 code와 redirect URI 없이 FAILURE 감사를 기록한다")
    void authorizationCodeFailureRecordsSanitizedAudit() {
        given(verifyOAuthTokenPort.verifyAuthorizationCode(
            OAuthProvider.KAKAO,
            AUTHORIZATION_CODE,
            REDIRECT_URI
        )).willThrow(new IllegalArgumentException("exchange failed"));

        assertThatThrownBy(() -> sut.authorizationCodeLogin(
            AuthorizationCodeLoginCommand.of(OAuthProvider.KAKAO, AUTHORIZATION_CODE, REDIRECT_URI)
        )).isInstanceOf(IllegalArgumentException.class);

        assertFailureAuditDoesNotContain(AUTHORIZATION_CODE, REDIRECT_URI);
    }

    private void assertFailureAuditDoesNotContain(String... forbiddenValues) {
        ArgumentCaptor<RecordAuditLogCommand> captor =
            ArgumentCaptor.forClass(RecordAuditLogCommand.class);
        then(recordAuditLogUseCase).should().record(captor.capture());

        RecordAuditLogCommand command = captor.getValue();
        assertThat(command.action()).isEqualTo(AuditAction.LOGIN);
        assertThat(command.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(command.source()).isEqualTo(AuditSource.AUTHENTICATION_SERVICE);
        assertThat(command.targetType()).isEqualTo("OAuthAuthentication");
        assertThat(command.targetId()).isNull();
        assertThat(command.toString()).doesNotContain(forbiddenValues);
        assertThat(command.toString())
            .doesNotContainIgnoringCase("accessToken")
            .doesNotContainIgnoringCase("authorizationCode")
            .doesNotContainIgnoringCase("rawBody");
    }
}
