package com.umc.product.community.adapter.in.websocket;

import static org.mockito.Mockito.mock;

import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.product.storage.application.port.out.StoragePort;

@TestConfiguration(proxyBeanMethods = false)
class CommunityThreadTwoInstanceTestConfig {

    @Bean(name = "firebaseMessaging")
    FirebaseMessaging firebaseMessaging() {
        return mock(FirebaseMessaging.class);
    }

    @Bean
    @Primary
    JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    StoragePort storagePort() {
        return mock(StoragePort.class);
    }

    @Bean
    RelayAvailabilityProbe relayAvailabilityProbe() {
        return new RelayAvailabilityProbe();
    }

    @Bean
    @Primary
    CommunityThreadE2EClock communityThreadE2EClock() {
        return new CommunityThreadE2EClock(Instant.parse("2026-07-18T00:00:00Z"));
    }
}
