package com.umc.product.authentication.application.service;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.event.SendVerificationEmailEvent;
import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.IssueAuthenticationTokensCommand;
import com.umc.product.authentication.application.port.in.command.dto.LogoutCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.in.command.dto.RenewAccessTokenCommand;
import com.umc.product.authentication.application.port.in.command.dto.ValidateEmailVerificationSessionCommand;
import com.umc.product.authentication.application.port.out.DeleteRefreshTokenPort;
import com.umc.product.authentication.application.port.out.LoadEmailVerificationPort;
import com.umc.product.authentication.application.port.out.LoadRefreshTokenPort;
import com.umc.product.authentication.application.port.out.LoadSsoClientPort;
import com.umc.product.authentication.application.port.out.SaveEmailVerificationPort;
import com.umc.product.authentication.domain.CredentialPolicy;
import com.umc.product.authentication.domain.EmailVerification;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.authentication.domain.RefreshToken;
import com.umc.product.authentication.domain.SsoClient;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.RefreshTokenClaims;
import com.umc.product.member.application.port.in.query.GetMemberCredentialUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService implements ManageAuthenticationUseCase {

    /**
     * 6자리 인증 코드 생성에 사용하는 보안 난수 생성기. JVM 단위 단일 인스턴스로 재사용한다.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoadEmailVerificationPort loadEmailVerificationPort;
    private final SaveEmailVerificationPort saveEmailVerificationPort;
    private final LoadRefreshTokenPort loadRefreshTokenPort;
    private final DeleteRefreshTokenPort deleteRefreshTokenPort;
    private final LoadSsoClientPort loadSsoClientPort;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationTokenIssuer authenticationTokenIssuer;
    private final GetMemberCredentialUseCase getMemberCredentialUseCase;
    private final DomainEventPublisher eventPublisher;

    // TODO: EmailSendUseCase와 구분할 필요가 있습니다.
    @Override
    public NewTokens issueTokens(IssueAuthenticationTokensCommand command) {
        return authenticationTokenIssuer.issue(command.memberId(), command.clientType());
    }

    @Override
    @Transactional
    public NewTokens renewAccessToken(RenewAccessTokenCommand command) {
        /*
            TODO
            ---
            RT의 마지막 사용으로부터 5분 이내에는 사용 가능하도록 하는 grace window 는 후속 과제.

            이는 네트워크 이슈로 인해서 Server에 요청이 접수되었지만 클라이언트에게는 응답이 가지 않은 경우를 대비하기 위함임
         */
        RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(command.refreshToken());
        RefreshToken storedRefreshToken = loadRefreshTokenPort.findByJti(claims.jti())
            .orElseThrow(() -> new AuthenticationDomainException(AuthenticationErrorCode.INVALID_REFRESH_TOKEN));

        String clientId = claims.clientContext().clientId();
        storedRefreshToken.validateActiveFor(claims.memberId(), clientId);
        if (!deleteRefreshTokenPort.deleteByJti(claims.jti())) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (clientId != null && !clientId.isBlank()) {
            SsoClient client = loadSsoClientPort.getByClientId(clientId);
            ClientContextClaims clientContext = ClientContextClaims.of(
                client.clientId(),
                client.serviceType(),
                client.environment()
            );
            return authenticationTokenIssuer.issue(
                claims.memberId(),
                null,
                clientContext,
                client.accessTokenTtl()
            );
        }

        return authenticationTokenIssuer.issue(claims.memberId(), null);
    }

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(command.refreshToken());
        deleteRefreshTokenPort.deleteByJti(claims.jti());
    }

    @Override
    @Transactional
    public Long createEmailVerificationSession(String email, EmailVerificationPurpose purpose) {
        // DTO 단계 검증을 통과해도, Service 진입 시 도메인 SSOT 인 CredentialPolicy 로 한번 더 검증한다.
        CredentialPolicy.validateEmail(email);

        // purpose 별 가입 여부 분기 검증.
        // - REGISTER / CHANGE_EMAIL: 이미 가입된 이메일이면 인증 진행 자체를 차단 (가입/변경 마지막 단계의
        //   UNIQUE 충돌로 인한 "인증 다 했는데 실패" UX 방지). 두 흐름에서는 이메일 사용 여부 노출이 자연스러움.
        // - PASSWORD_RESET: 미가입 / 자격증명 미등록 이메일에 대해서는 user enumeration 방어를 위해
        //   응답은 동일하게 내려보내되 실제 메일 발송만 건너뛴다. 후속 reset 흐름에서도 INVALID_LOGIN_CREDENTIAL
        //   단일 메시지로 응답하므로 끝까지 가입 여부가 노출되지 않는다.
        boolean shouldSendEmail = switch (purpose) {
            case REGISTER, CHANGE_EMAIL -> {
                if (getMemberCredentialUseCase.existsByEmail(email)) {
                    throw new AuthenticationDomainException(AuthenticationErrorCode.EMAIL_ALREADY_EXISTS);
                }
                yield true;
            }
            case PASSWORD_RESET -> {
                boolean hasCredential = getMemberCredentialUseCase.findCredentialByEmail(email).isPresent();
                if (!hasCredential) {
                    log.info("PASSWORD_RESET 발송 요청이지만 가입/자격증명 미존재 — 발송 건너뜀 (enumeration 방어)");
                }
                yield hasCredential;
            }
        };

        // 실제로 메일이 나갈 경우에만 throttle 을 검사한다. silent skip 으로 발송하지 않는 경로는
        // 메일 폭주 위험이 없으므로 throttle 대상이 아니다.
        if (shouldSendEmail) {
            loadEmailVerificationPort.findLatestSentByEmail(email)
                .filter(EmailVerification::isSendThrottled)
                .ifPresent(latest -> {
                    throw new AuthenticationDomainException(AuthenticationErrorCode.EMAIL_VERIFICATION_THROTTLED);
                });
        }

        String code = generateRandomCode();
        String token = UUID.randomUUID().toString();

        EmailVerification emailVerification = EmailVerification.builder()
            .email(email)
            .code(code)
            .token(token)
            .purpose(purpose)
            .build();

        Long sessionId = saveEmailVerificationPort.save(emailVerification).getId();

        if (shouldSendEmail) {
            emailVerification.markSent();
            publishSendEmailEvent(email, code);
        }

        return sessionId;
    }

    @Override
    @Transactional
    public void resendEmailVerification(Long sessionId) {
        EmailVerification emailVerification = loadEmailVerificationPort.getById(sessionId);

        if (emailVerification.isSendThrottled()) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.EMAIL_VERIFICATION_THROTTLED);
        }

        String newCode = generateRandomCode();
        String newToken = UUID.randomUUID().toString();

        emailVerification.regenerate(newCode, newToken);
        emailVerification.markSent();

        publishSendEmailEvent(emailVerification.getEmail(), newCode);
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationDomainException.class)
    public String validateEmailVerificationSession(ValidateEmailVerificationSessionCommand command) {
        if (command.code() == null) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.NO_EMAIL_VERIFICATION_METHOD_GIVEN);
        }

        EmailVerification emailVerification = loadEmailVerificationPort.getById(command.sessionId());

        // verifyCode 가 실패해도 attempt_count 증가/세션 무효화는 영속되어야 하므로
        // AuthenticationDomainException 에 대해서는 트랜잭션을 롤백하지 않는다.
        emailVerification.verifyCode(command.code());

        return jwtTokenProvider.createEmailVerificationToken(
            emailVerification.getEmail(),
            emailVerification.getPurpose()
        );
    }

    /**
     * 실제 SMTP 호출을 트랜잭션 commit 이후로 미루기 위해 이벤트를 발행한다.
     * AFTER_COMMIT 단계에서 SendVerificationEmailEventListener 가 메일을 발송한다.
     */
    private void publishSendEmailEvent(String email, String code) {
        eventPublisher.publish(SendVerificationEmailEvent.of(email, code));
    }

    private String generateRandomCode() {
        return String.valueOf(RANDOM.nextInt(900000) + 100000);
    }
}
