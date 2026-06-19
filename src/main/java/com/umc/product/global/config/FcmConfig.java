package com.umc.product.global.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
public class FcmConfig {

    @Value("${app.fcm.firebase-configuration:}")
    private String firebaseCredentials;

    @Bean
    FirebaseMessaging firebaseMessaging() throws IOException {
        log.info("Firebase 초기화 시작");

        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ByteArrayInputStream(firebaseCredentials.getBytes(StandardCharsets.UTF_8))
        );

        log.debug("Firebase credentials를 로드했습니다");

        FirebaseApp firebaseApp = null;
        List<FirebaseApp> apps = FirebaseApp.getApps();

        if (apps != null && !apps.isEmpty()) {
            log.debug("기존 FirebaseApp 검색 중: count={}", apps.size());

            for (FirebaseApp app : apps) {
                if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
                    firebaseApp = app;
                    log.debug("기존 FirebaseApp 발견: name={}", app.getName());
                }
            }
        } else {
            log.info("FirebaseApp 신규 생성");

            FirebaseOptions options =
                FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            firebaseApp = FirebaseApp.initializeApp(options);
            log.info("FirebaseApp을 초기화했습니다: name={}", firebaseApp.getName());
        }

        log.info("FirebaseMessaging 인스턴스를 생성했습니다");
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
