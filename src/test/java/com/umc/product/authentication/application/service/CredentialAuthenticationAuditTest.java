package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.authentication.application.port.in.command.dto.LoginByEmailCommand;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.command.ManageMemberCredentialUseCase;
import com.umc.product.member.application.port.in.query.GetMemberCredentialUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 실패 감사 로그 계약")
class CredentialAuthenticationAuditTest {

    private static final String MALFORMED_EMAIL = "not-an-email?token=login-token-secret";
    private static final String RAW_PASSWORD = "raw-password-secret";

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthenticationTokenIssuer authenticationTokenIssuer;

    @Mock
    GetMemberCredentialUseCase getMemberCredentialUseCase;

    @Mock
    ManageMemberCredentialUseCase manageMemberCredentialUseCase;

    @Mock
    SsoCredentialVerifier credentialVerifier;

    @Mock
    OperationalMetrics operationalMetrics;

    @Mock
    RecordAuditLogUseCase recordAuditLogUseCase;

    @InjectMocks
    CredentialAuthenticationService service;

    @Test
    @DisplayName("형식이 잘못된 자격증명 로그인 실패를 민감정보 없이 명시 감사 기록한다")
    void malformedCredentialsAreRecordedWithoutSecrets() throws Exception {
        // given
        givenCredentialFailure();

        // when
        AuthenticationDomainException thrown = catchThrowableOfType(
            AuthenticationDomainException.class,
            () -> service.loginByEmail(loginCommand())
        );

        // then
        assertThat(thrown.getBaseCode()).isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);

        RecordAuditLogCommand recorded = captureSingleRecord();
        assertLoginFailure(recorded);

        String serializedDetails = new ObjectMapper().writeValueAsString(recorded.details());
        assertThat(serializedDetails)
            .doesNotContainIgnoringCase("email")
            .doesNotContainIgnoringCase("password")
            .doesNotContainIgnoringCase("token")
            .doesNotContainIgnoringCase("authorization")
            .doesNotContain(MALFORMED_EMAIL)
            .doesNotContain(RAW_PASSWORD);
    }

    @Test
    @DisplayName("반복 로그인 실패는 각각 한 건씩 감사 기록한다")
    void repeatedFailuresAreRecordedIndividually() {
        // given
        givenCredentialFailure();

        // when
        catchThrowableOfType(
            AuthenticationDomainException.class,
            () -> service.loginByEmail(loginCommand())
        );
        catchThrowableOfType(
            AuthenticationDomainException.class,
            () -> service.loginByEmail(loginCommand())
        );

        // then
        then(recordAuditLogUseCase).should(times(2)).record(any(RecordAuditLogCommand.class));
    }

    @Test
    @DisplayName("감사 기록기가 실패해도 원 로그인 예외를 유지한다")
    void recorderFailureDoesNotReplaceAuthenticationFailure() {
        // given
        givenCredentialFailure();
        doThrow(new IllegalStateException("audit-recorder-failure"))
            .when(recordAuditLogUseCase)
            .record(any(RecordAuditLogCommand.class));

        // when
        AuthenticationDomainException thrown = catchThrowableOfType(
            AuthenticationDomainException.class,
            () -> service.loginByEmail(loginCommand())
        );

        // then
        assertThat(thrown.getBaseCode()).isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);
        then(recordAuditLogUseCase).should().record(any(RecordAuditLogCommand.class));
    }

    @Test
    @DisplayName("로그인 성공은 기존 Audited LOGIN 계약을 유지한다")
    void successfulLoginRetainsAuditedAnnotation() throws Exception {
        // given
        Method loginMethod = CredentialAuthenticationService.class.getMethod(
            "loginByEmail",
            LoginByEmailCommand.class
        );

        // when
        Audited audited = loginMethod.getAnnotation(Audited.class);

        // then
        assertThat(audited).isNotNull();
        assertThat(audited.domain()).isEqualTo(Domain.AUTHENTICATION);
        assertThat(audited.action()).isEqualTo(AuditAction.LOGIN);
        assertThat(audited.targetId()).isEqualTo("#result.memberId()");
    }

    private LoginByEmailCommand loginCommand() {
        return LoginByEmailCommand.of(MALFORMED_EMAIL, RAW_PASSWORD, ClientType.WEB);
    }

    private void givenCredentialFailure() {
        given(credentialVerifier.verifyEmailPassword(MALFORMED_EMAIL, RAW_PASSWORD))
            .willThrow(new AuthenticationDomainException(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL));
    }

    private RecordAuditLogCommand captureSingleRecord() {
        ArgumentCaptor<RecordAuditLogCommand> captor =
            ArgumentCaptor.forClass(RecordAuditLogCommand.class);
        then(recordAuditLogUseCase).should().record(captor.capture());
        return captor.getValue();
    }

    private void assertLoginFailure(RecordAuditLogCommand recorded) {
        assertThat(recorded.domain()).isEqualTo(Domain.AUTHENTICATION);
        assertThat(recorded.action()).isEqualTo(AuditAction.LOGIN);
        assertThat(recorded.targetType()).isEqualTo("MemberCredential");
        assertThat(recorded.targetId()).isNull();
        assertThat(recorded.actorMemberId()).isNull();
        assertThat(recorded.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(recorded.source()).isEqualTo(AuditSource.AUTHENTICATION_SERVICE);
        assertThat(recorded.details())
            .containsEntry("schemaVersion", 1)
            .containsEntry("actor", Map.of())
            .containsEntry("target", Map.of("type", "MemberCredential"))
            .containsEntry("context", Map.of())
            .containsEntry("before", Map.of())
            .containsEntry("after", Map.of());
    }
}
