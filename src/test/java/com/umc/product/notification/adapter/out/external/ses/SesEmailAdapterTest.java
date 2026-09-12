package com.umc.product.notification.adapter.out.external.ses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.notification.application.port.out.dto.EmailMessage;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class SesEmailAdapterTest {

    @Mock
    private SesV2Client sesV2Client;

    @BeforeEach
    void setUp() {
        given(sesV2Client.sendEmail(any(SendEmailRequest.class)))
            .willReturn(SendEmailResponse.builder().messageId("test-message-id").build());
    }

    @Test
    @DisplayName("SES는 일반 텍스트 메일을 UTF-8 텍스트 본문으로 발송한다")
    void 일반_텍스트_본문을_발송한다() {
        // given
        SesEmailAdapter adapter = new SesEmailAdapter(
            sesV2Client, new SesProperties("ap-northeast-2", null, null, null));
        EmailMessage message = new EmailMessage(
            "sender@example.org", "윰씨", "recipient@example.org", "이메일 인증 코드", "인증 안내", false);

        // when
        adapter.send(message);

        // then
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        then(sesV2Client).should().sendEmail(captor.capture());
        SendEmailRequest request = captor.getValue();
        assertThat(request.fromEmailAddress()).isEqualTo("\"윰씨\" <sender@example.org>");
        assertThat(request.destination().toAddresses()).containsExactly(message.to());
        assertThat(request.configurationSetName()).isNull();
        Message content = request.content().simple();
        assertThat(content.subject().data()).isEqualTo(message.subject());
        assertThat(content.subject().charset()).isEqualTo("UTF-8");
        assertThat(content.body().text().data()).isEqualTo(message.body());
        assertThat(content.body().text().charset()).isEqualTo("UTF-8");
        assertThat(content.body().html()).isNull();
    }

    @Test
    @DisplayName("SES는 기존 HTML 본문과 Configuration Set 설정을 유지한다")
    void HTML_본문과_발송_설정_세트를_유지한다() {
        // given
        SesEmailAdapter adapter = new SesEmailAdapter(
            sesV2Client, new SesProperties("ap-northeast-2", null, null, "test-configuration"));
        EmailMessage message = new EmailMessage(
            "sender@example.org", "윰씨", "recipient@example.org", "면접 안내", "<html>면접 안내</html>", true);

        // when
        adapter.send(message);

        // then
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        then(sesV2Client).should().sendEmail(captor.capture());
        SendEmailRequest request = captor.getValue();
        assertThat(request.fromEmailAddress()).isEqualTo("\"윰씨\" <sender@example.org>");
        assertThat(request.destination().toAddresses()).containsExactly(message.to());
        assertThat(request.configurationSetName()).isEqualTo("test-configuration");
        Message content = request.content().simple();
        assertThat(content.subject().data()).isEqualTo(message.subject());
        assertThat(content.subject().charset()).isEqualTo("UTF-8");
        assertThat(content.body().html().data()).isEqualTo(message.body());
        assertThat(content.body().html().charset()).isEqualTo("UTF-8");
        assertThat(content.body().text()).isNull();
    }
}
