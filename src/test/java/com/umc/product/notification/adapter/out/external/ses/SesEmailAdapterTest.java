package com.umc.product.notification.adapter.out.external.ses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.InternalServiceErrorException;
import software.amazon.awssdk.services.sesv2.model.MessageRejectedException;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.TooManyRequestsException;

@DisplayName("AWS SES v2 이메일 adapter")
@ExtendWith(MockitoExtension.class)
class SesEmailAdapterTest {

    private static final String RAW_PII = "홍길동 연락처 010-1234-5678";

    @Mock
    private SesV2Client sesV2Client;

    private SesEmailAdapter adapter;

    @BeforeEach
    void setUp() {
        SesProperties properties = new SesProperties(
            "ap-northeast-2",
            "access-key",
            "secret-key",
            "transactional-email",
            Duration.ofSeconds(30),
            Duration.ofSeconds(10)
        );
        adapter = new SesEmailAdapter(sesV2Client, properties);
    }

    @Test
    @DisplayName("UTF-8 subject·HTML·from과 configuration set으로 SES를 호출한다")
    void testCase001() {
        given(sesV2Client.sendEmail(any(SendEmailRequest.class)))
            .willReturn(SendEmailResponse.builder().messageId("message-id").build());
        EmailMessage message = new EmailMessage(
            "noreply@test.umc.local",
            "UMC 테스트",
            "applicant@test.umc.local",
            "[UMC] 최종 합격을 축하드립니다",
            "<html><body>홍길동님, 합격을 축하드립니다.</body></html>"
        );

        adapter.send(message);

        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesV2Client).sendEmail(requestCaptor.capture());
        SendEmailRequest request = requestCaptor.getValue();
        assertThat(request.fromEmailAddress()).isEqualTo("\"UMC 테스트\" <noreply@test.umc.local>");
        assertThat(request.destination().toAddresses()).containsExactly("applicant@test.umc.local");
        assertThat(request.configurationSetName()).isEqualTo("transactional-email");
        assertThat(request.content().simple().subject().charset()).isEqualTo("UTF-8");
        assertThat(request.content().simple().subject().data()).isEqualTo(message.subject());
        assertThat(request.content().simple().body().html().charset()).isEqualTo("UTF-8");
        assertThat(request.content().simple().body().html().data()).isEqualTo(message.htmlBody());
    }

    @Test
    @DisplayName("SES provider 실패는 PII 없는 EMAIL send code로 변환한다")
    void testCase002() {
        MessageRejectedException failure = MessageRejectedException.builder()
            .message(RAW_PII)
            .statusCode(400)
            .build();
        given(sesV2Client.sendEmail(any(SendEmailRequest.class))).willThrow(failure);
        EmailMessage message = new EmailMessage(
            "noreply@test.umc.local",
            "UMC 테스트",
            "applicant@test.umc.local",
            "제목",
            "민감한 지원 정보"
        );
        Logger logger = (Logger) LoggerFactory.getLogger(SesEmailAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> adapter.send(message))
                .isInstanceOf(EmailDomainException.class)
                .satisfies(exception -> {
                    EmailDomainException emailException = (EmailDomainException) exception;
                    assertThat(emailException.getBaseCode()).isEqualTo(EmailErrorCode.EMAIL_SEND_FAILED);
                    assertThat(String.valueOf(emailException.getMessage()))
                        .doesNotContain(RAW_PII, "applicant@test.umc.local");
                    assertThat(emailException.getCause())
                        .as("telemetry error에 도달 가능한 raw provider cause")
                        .isNull();
                    assertThat(emailException.retryable()).isFalse();
                });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(RAW_PII, "applicant@test.umc.local");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    @Test
    @DisplayName("SES 5xx 실패는 PII 없는 재시도 가능 오류로 변환한다")
    void testCase003() {
        InternalServiceErrorException failure = InternalServiceErrorException.builder()
            .message(RAW_PII)
            .statusCode(500)
            .build();
        given(sesV2Client.sendEmail(any(SendEmailRequest.class))).willThrow(failure);
        EmailMessage message = new EmailMessage(
            "noreply@test.umc.local",
            "UMC 테스트",
            "applicant@test.umc.local",
            "제목",
            "민감한 지원 정보"
        );

        assertThatThrownBy(() -> adapter.send(message))
            .isInstanceOf(EmailDomainException.class)
            .satisfies(exception -> {
                EmailDomainException emailException = (EmailDomainException) exception;
                assertThat(emailException.getBaseCode()).isEqualTo(EmailErrorCode.EMAIL_SEND_FAILED);
                assertThat(emailException.retryable()).isTrue();
                assertThat(emailException.getCause()).isNull();
            });
    }

    @Test
    @DisplayName("SES throttling 실패는 재시도 가능 오류로 변환한다")
    void testCase004() {
        TooManyRequestsException failure = TooManyRequestsException.builder()
            .message(RAW_PII)
            .statusCode(429)
            .build();
        given(sesV2Client.sendEmail(any(SendEmailRequest.class))).willThrow(failure);

        assertRetryableCauseLessFailure();
    }

    @Test
    @DisplayName("상태를 알 수 없는 runtime 실패는 재시도 가능 오류로 변환한다")
    void testCase005() {
        given(sesV2Client.sendEmail(any(SendEmailRequest.class)))
            .willThrow(new IllegalStateException(RAW_PII));

        assertRetryableCauseLessFailure();
    }

    private void assertRetryableCauseLessFailure() {
        EmailMessage message = new EmailMessage(
            "noreply@test.umc.local",
            "UMC 테스트",
            "applicant@test.umc.local",
            "제목",
            "민감한 지원 정보"
        );

        assertThatThrownBy(() -> adapter.send(message))
            .isInstanceOf(EmailDomainException.class)
            .satisfies(exception -> {
                EmailDomainException emailException = (EmailDomainException) exception;
                assertThat(emailException.getBaseCode()).isEqualTo(EmailErrorCode.EMAIL_SEND_FAILED);
                assertThat(emailException.retryable()).isTrue();
                assertThat(emailException.getCause()).isNull();
            });
    }
}
