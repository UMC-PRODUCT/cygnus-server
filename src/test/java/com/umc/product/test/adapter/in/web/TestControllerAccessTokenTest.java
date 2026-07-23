package com.umc.product.test.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.adapter.out.external.AppleOAuthProperties;
import com.umc.product.authentication.adapter.out.external.AppleTokenVerifier;
import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.notification.application.port.in.SendEmailUseCase;
import com.umc.product.notification.application.port.in.SendNotificationToAudienceUseCase;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.term.application.port.in.query.GetRequiredTermConsentStatusUseCase;
import com.umc.product.term.application.port.in.query.dto.RequiredTermConsentStatusInfo;

@ExtendWith(MockitoExtension.class)
class TestControllerAccessTokenTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    ManageAuthenticationUseCase manageAuthenticationUseCase;

    @Mock
    AppleTokenVerifier appleTokenVerifier;

    @Mock
    AppleOAuthProperties appleOAuthProperties;

    @Mock
    SendWebhookAlarmUseCase sendWebhookAlarmUseCase;

    @Mock
    GetFileUseCase getFileUseCase;

    @Mock
    SendEmailUseCase sendEmailUseCase;

    @Mock
    SendNotificationToAudienceUseCase sendNotificationToAudienceUseCase;

    @Mock
    GetRequiredTermConsentStatusUseCase getRequiredTermConsentStatusUseCase;

    @InjectMocks
    TestController sut;

    @Test
    @DisplayName("개발용 기본 AccessToken에도 최신 필수 약관 동의 snapshot을 포함한다")
    void issueDefaultAccessTokenWithTermSnapshot() {
        given(getRequiredTermConsentStatusUseCase.getRequiredTermConsentStatus(10L))
            .willReturn(new RequiredTermConsentStatusInfo(true, List.of()));
        given(jwtTokenProvider.createAccessToken(10L, List.of(), null, false))
            .willReturn("access-token");

        String result = sut.getAccessToken(10L, null);

        assertThat(result).isEqualTo("access-token");
        then(jwtTokenProvider).should().createAccessToken(10L, List.of(), null, false);
        then(manageAuthenticationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("개발용 만료시간 지정 AccessToken에도 최신 필수 약관 동의 snapshot을 포함한다")
    void issueExpiringAccessTokenWithTermSnapshot() {
        given(getRequiredTermConsentStatusUseCase.getRequiredTermConsentStatus(10L))
            .willReturn(new RequiredTermConsentStatusInfo(false, List.of()));
        given(jwtTokenProvider.createAccessToken(10L, List.of(), true, 600L))
            .willReturn("access-token");

        String result = sut.getAccessToken(10L, 10L);

        assertThat(result).isEqualTo("access-token");
        then(jwtTokenProvider).should().createAccessToken(10L, List.of(), true, 600L);
    }
}
