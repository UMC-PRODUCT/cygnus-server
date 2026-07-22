package com.umc.product.notification.adapter.out.external.ses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.exception.EmailDomainException;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

@DisplayName("SesEmailAdapter")
class SesEmailAdapterTest {

    @Test
    @DisplayName("UTF-8 HTML 요청과 configuration set을 만들고 발신자 표시명을 RFC 형식으로 escape한다")
    void sends_complete_ses_request() {
        SesV2Client client = mock(SesV2Client.class);
        given(client.sendEmail(any(SendEmailRequest.class)))
            .willReturn(SendEmailResponse.builder().messageId("message-id").build());
        SesEmailAdapter sut = new SesEmailAdapter(client, new SesProperties("ap-northeast-2", "key", "secret", "config"));

        sut.send(new EmailMessage(
            "no-reply@example.org", "UMC \\\"운영팀", "member@example.org", "제목", "<b>본문</b>"
        ));

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        org.mockito.Mockito.verify(client).sendEmail(captor.capture());
        SendEmailRequest request = captor.getValue();
        assertThat(request.fromEmailAddress()).contains("UMC").contains("no-reply@example.org");
        assertThat(request.configurationSetName()).isEqualTo("config");
        assertThat(request.content().simple().subject().charset()).isEqualTo("UTF-8");
        assertThat(request.destination().toAddresses()).containsExactly("member@example.org");
    }

    @Test
    @DisplayName("AWS 오류와 예기치 못한 runtime 오류를 cause가 있는 이메일 도메인 예외로 변환한다")
    void wraps_provider_and_runtime_failures() {
        SesV2Client client = mock(SesV2Client.class);
        SesEmailAdapter sut = new SesEmailAdapter(client, new SesProperties("ap-northeast-2", "", "", null));
        EmailMessage message = new EmailMessage("from@example.org", "UMC", null, "제목", "본문");
        SesV2Exception providerFailure = (SesV2Exception) SesV2Exception.builder()
            .awsErrorDetails(AwsErrorDetails.builder().errorCode("Throttling").build())
            .message("failed")
            .build();
        given(client.sendEmail(any(SendEmailRequest.class))).willThrow(providerFailure);
        assertThatThrownBy(() -> sut.send(message))
            .isInstanceOf(EmailDomainException.class)
            .hasCause(providerFailure);

        given(client.sendEmail(any(SendEmailRequest.class))).willThrow(new IllegalStateException("runtime"));
        assertThatThrownBy(() -> sut.send(message))
            .isInstanceOf(EmailDomainException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("SES 설정은 static/default credentials와 configuration set 존재 여부를 정확히 판정한다")
    void resolves_configuration_boundaries() {
        SesProperties staticProperties = new SesProperties("ap-northeast-2", "key", "secret", "config");
        SesProperties defaultProperties = new SesProperties("ap-northeast-2", " ", null, " ");
        assertThat(staticProperties.hasStaticCredentials()).isTrue();
        assertThat(staticProperties.hasConfigurationSet()).isTrue();
        assertThat(defaultProperties.hasStaticCredentials()).isFalse();
        assertThat(defaultProperties.hasConfigurationSet()).isFalse();

        SesEmailConfig config = new SesEmailConfig();
        Object staticProvider = ReflectionTestUtils.invokeMethod(config, "resolveCredentials", staticProperties);
        Object defaultProvider = ReflectionTestUtils.invokeMethod(config, "resolveCredentials", defaultProperties);
        assertThat(staticProvider).isInstanceOf(StaticCredentialsProvider.class);
        assertThat(defaultProvider).isInstanceOf(DefaultCredentialsProvider.class);
    }
}
