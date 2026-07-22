package com.umc.product.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@DisplayName("FcmConfig")
class FcmConfigTest {

    @Test
    @DisplayName("기본 FirebaseApp이 이미 있으면 재초기화 없이 messaging을 반환한다")
    void 기존_FirebaseApp을_재사용한다() throws Exception {
        FcmConfig sut = config();
        GoogleCredentials credentials = mock(GoogleCredentials.class);
        FirebaseApp other = mock(FirebaseApp.class);
        FirebaseApp existing = mock(FirebaseApp.class);
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        given(other.getName()).willReturn("other");
        given(existing.getName()).willReturn(FirebaseApp.DEFAULT_APP_NAME);

        try (MockedStatic<GoogleCredentials> google = Mockito.mockStatic(GoogleCredentials.class);
             MockedStatic<FirebaseApp> firebase = Mockito.mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> message = Mockito.mockStatic(FirebaseMessaging.class)) {
            google.when(() -> GoogleCredentials.fromStream(any())).thenReturn(credentials);
            firebase.when(FirebaseApp::getApps).thenReturn(List.of(other, existing));
            message.when(() -> FirebaseMessaging.getInstance(existing)).thenReturn(messaging);

            assertThat(sut.firebaseMessaging()).isSameAs(messaging);
        }
    }

    @Test
    @DisplayName("FirebaseApp이 없으면 credential 기반 기본 app을 한 번 초기화한다")
    void FirebaseApp을_신규_초기화한다() throws Exception {
        FcmConfig sut = config();
        GoogleCredentials credentials = mock(GoogleCredentials.class);
        FirebaseApp initialized = mock(FirebaseApp.class);
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        given(initialized.getName()).willReturn(FirebaseApp.DEFAULT_APP_NAME);

        try (MockedStatic<GoogleCredentials> google = Mockito.mockStatic(GoogleCredentials.class);
             MockedStatic<FirebaseApp> firebase = Mockito.mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> message = Mockito.mockStatic(FirebaseMessaging.class)) {
            google.when(() -> GoogleCredentials.fromStream(any())).thenReturn(credentials);
            firebase.when(FirebaseApp::getApps).thenReturn(List.of());
            firebase.when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class))).thenReturn(initialized);
            message.when(() -> FirebaseMessaging.getInstance(initialized)).thenReturn(messaging);

            assertThat(sut.firebaseMessaging()).isSameAs(messaging);
        }
    }

    private FcmConfig config() {
        FcmConfig config = new FcmConfig();
        ReflectionTestUtils.setField(config, "firebaseCredentials", "{}");
        return config;
    }
}
