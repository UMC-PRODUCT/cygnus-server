package com.umc.product.test.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.adapter.out.external.AppleOAuthProperties;
import com.umc.product.authentication.adapter.out.external.AppleTokenVerifier;
import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.notification.application.port.in.SendEmailUseCase;
import com.umc.product.notification.application.port.in.SendNotificationToAudienceUseCase;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.dto.SendVerificationEmailCommand;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.test.dto.FcmTestSendRequest;

@ExtendWith(MockitoExtension.class)
class TestControllerTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock ManageAuthenticationUseCase manageAuthenticationUseCase;
    @Mock AppleTokenVerifier appleTokenVerifier;
    @Mock SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    @Mock GetFileUseCase getFileUseCase;
    @Mock SendEmailUseCase sendEmailUseCase;
    @Mock SendNotificationToAudienceUseCase sendNotificationToAudienceUseCase;

    TestController sut;

    @BeforeEach
    void setUp() {
        AppleOAuthProperties properties = new AppleOAuthProperties(
            "ios-client", "web-client", "team", "key", "private", null
        );
        sut = new TestController(
            jwtTokenProvider,
            manageAuthenticationUseCase,
            appleTokenVerifier,
            properties,
            sendWebhookAlarmUseCase,
            getFileUseCase,
            sendEmailUseCase,
            sendNotificationToAudienceUseCase
        );
    }

    @Test
    @DisplayName("파일·알림·이메일 테스트 API는 입력을 변환해 각 UseCase에 위임한다")
    void 파일과_알림_API_위임() {
        FileInfo file = new FileInfo(
            "file-id", "image.png", FileCategory.PROFILE_IMAGE, "image/png", 10L,
            "https://cdn/file-id", true, 1L, Instant.EPOCH
        );
        given(getFileUseCase.getById("file-id")).willReturn(file);
        FcmTestSendRequest fcmRequest = new FcmTestSendRequest(1L, "제목", "내용");
        SendVerificationEmailCommand emailCommand = new SendVerificationEmailCommand(
            "member@example.com", "123456"
        );

        assertThat(sut.getFile("file-id").getResult().fileUrl()).isEqualTo("https://cdn/file-id");
        sut.sendTestNotification(fcmRequest);
        sut.sendTestEmail(emailCommand);
        assertThat(sut.sendAopWebhookAlarm("제목", "AOP 내용").content()).isEqualTo("AOP 내용");
        sut.sendWebhookAlarm(null, null);
        sut.sendWebhookAlarm("제목", "내용");
        sut.sendBufferedWebhookAlarm(null, null, 1);
        sut.sendBufferedWebhookAlarm("버퍼", "내용", 2);

        verify(sendNotificationToAudienceUseCase).sendToMember(fcmRequest.toCommand());
        verify(sendEmailUseCase).sendVerificationEmail(emailCommand);
        ArgumentCaptor<SendWebhookAlarmCommand> immediate =
            ArgumentCaptor.forClass(SendWebhookAlarmCommand.class);
        verify(sendWebhookAlarmUseCase, times(2)).send(immediate.capture());
        assertThat(immediate.getAllValues())
            .extracting(SendWebhookAlarmCommand::title)
            .containsExactly("알람 테스트", "제목");
        ArgumentCaptor<SendWebhookAlarmCommand> buffered =
            ArgumentCaptor.forClass(SendWebhookAlarmCommand.class);
        verify(sendWebhookAlarmUseCase, times(3)).sendBuffered(buffered.capture());
        assertThat(buffered.getAllValues())
            .extracting(SendWebhookAlarmCommand::title)
            .containsExactly("버퍼 알람 테스트", "버퍼 #1", "버퍼 #2");
    }

    @Test
    @DisplayName("개발용 토큰 API는 기본·사용자 지정 만료와 인증 정보를 보존한다")
    void 토큰과_상태_API_위임() {
        given(appleTokenVerifier.generateClientSecret("ios-client")).willReturn("apple-secret");
        given(jwtTokenProvider.createAccessToken(1L, null)).willReturn("default-access");
        given(jwtTokenProvider.createAccessToken(1L, null, 300L)).willReturn("custom-access");
        given(manageAuthenticationUseCase.issueTokens(any()))
            .willReturn(NewTokens.builder().refreshToken("refresh").build());
        given(jwtTokenProvider.createEmailVerificationToken(
            "member@example.com", EmailVerificationPurpose.REGISTER
        )).willReturn("email-token");
        given(jwtTokenProvider.createOAuthVerificationToken(
            "member@example.com", OAuthProvider.KAKAO, "provider-id"
        )).willReturn("oauth-token");
        MemberPrincipal principal = new MemberPrincipal(1L, ClientType.IOS);

        assertThat(sut.getAppleClientSecret(ClientType.IOS)).isEqualTo("apple-secret");
        assertThat(sut.getAccessToken(1L, null)).isEqualTo("default-access");
        assertThat(sut.getAccessToken(1L, 5L)).isEqualTo("custom-access");
        assertThat(sut.getRefreshToken(1L)).isEqualTo("refresh");
        assertThat(sut.getEmailVerification(
            "member@example.com", EmailVerificationPurpose.REGISTER
        )).isEqualTo("email-token");
        assertThat(sut.getOAuthVerificationToken(
            OAuthProvider.KAKAO, "provider-id", "member@example.com"
        )).isEqualTo("oauth-token");
        assertThat(sut.healthCheck()).isEqualTo("OK");
        assertThat(sut.checkAuthenticated(principal).getResult()).isEqualTo(principal.toString());
        sut.logTest();
    }
}
