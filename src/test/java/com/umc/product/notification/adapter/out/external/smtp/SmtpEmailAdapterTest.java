package com.umc.product.notification.adapter.out.external.smtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class SmtpEmailAdapterTest {

    @Mock
    JavaMailSender mailSender;

    SmtpEmailAdapter sut;
    MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        sut = new SmtpEmailAdapter(mailSender);
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
    }

    @Test
    @DisplayName("SMTP는 발신자 표시명과 한국어 제목 및 HTML 본문을 유지한다")
    void sends_utf8_html_with_sender_identity() throws Exception {
        // given
        EmailMessage message = createMessage();

        // when
        sut.send(message);
        mimeMessage.saveChanges();

        // then
        then(mailSender).should().send(mimeMessage);
        InternetAddress from = (InternetAddress)mimeMessage.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo(message.fromAddress());
        assertThat(from.getPersonal()).isEqualTo(message.fromDisplayName());
        assertThat(mimeMessage.getAllRecipients()).extracting(Object::toString).containsExactly(message.to());
        assertThat(mimeMessage.getSubject()).isEqualTo(message.subject());
        assertThat(mimeMessage.getContentType()).containsIgnoringCase("text/html").containsIgnoringCase("UTF-8");
        assertThat(mimeMessage.getContent()).isEqualTo(message.body());
    }

    @Test
    @DisplayName("SMTP는 순수 텍스트 인증메일을 UTF-8 text/plain으로 발송한다")
    void sends_utf8_plain_text_without_html() throws Exception {
        // given
        EmailMessage message = new EmailMessage(
            "sender@example.org", "UMC", "recipient@example.org", "[UMC] 이메일 인증 코드",
            "인증 코드: 123456\n요청 시점부터 10분 동안 유효합니다.", false
        );

        // when
        sut.send(message);
        mimeMessage.saveChanges();

        // then
        then(mailSender).should().send(mimeMessage);
        assertThat(mimeMessage.getContentType()).containsIgnoringCase("text/plain").containsIgnoringCase("UTF-8")
            .doesNotContain("text/html", "multipart");
        assertThat(mimeMessage.getContent()).isEqualTo(message.body());
        assertThat(mimeMessage.getAllRecipients()).extracting(Object::toString).containsExactly(message.to());
        assertThat(mimeMessage.getSubject()).isEqualTo(message.subject());
    }

    @Test
    @DisplayName("SMTP 실패는 도메인 오류로 변환하고 원문이나 민감정보를 로그와 cause에 남기지 않는다")
    void sanitizes_smtp_failure() {
        // given
        String sensitiveError = "recipient@example.org 인증코드 123456 app-password-secret";
        doThrow(new MailSendException(sensitiveError)).when(mailSender).send(any(MimeMessage.class));
        Logger logger = (Logger)LoggerFactory.getLogger(SmtpEmailAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            // when / then
            assertThatThrownBy(() -> sut.send(createMessage()))
                .isInstanceOfSatisfying(EmailDomainException.class,
                    error -> assertThat(error.getBaseCode()).isEqualTo(EmailErrorCode.EMAIL_SEND_FAILED))
                .hasCause(null)
                .hasStackTraceContaining("EmailDomainException")
                .hasStackTraceContaining("SmtpEmailAdapter");
            assertThat(appender.list).isNotEmpty().allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain(
                    "recipient@example.org", "123456", "app-password-secret", "<html>");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private EmailMessage createMessage() {
        return new EmailMessage(
            "sender@example.org", "윰씨", "recipient@example.org", "이메일 인증 코드", "<html>인증 안내</html>", true
        );
    }
}
