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
import software.amazon.awssdk.services.sesv2.model.MessageRejectedException;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

@DisplayName("AWS SES v2 이메일 adapter")
@ExtendWith(MockitoExtension.class)
class SesEmailAdapterTest {

    private static final String RAW_PII = "applicant@test.umc.local 민감한 지원 정보";

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
    void UTF8_메일_요청을_SES에_전달한다() {
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
    void SES_실패를_PII_없는_도메인_예외로_변환한다() {
        MessageRejectedException failure = MessageRejectedException.builder()
            .message(RAW_PII)
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
}
