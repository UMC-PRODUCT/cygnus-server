package com.umc.product.notification.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fcm_token")
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "fcm_token", columnDefinition = "TEXT")
    private String fcmToken;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "platform", length = 30)
    private String platform;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "last_registered_at")
    private Instant lastRegisteredAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "last_validated_at")
    private Instant lastValidatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private FcmToken(Long memberId, String fcmToken, String platform, String deviceId, String appVersion) {
        this.memberId = memberId;
        this.fcmToken = fcmToken;
        register(platform, deviceId, appVersion);
    }

    public static FcmToken create(Long memberId, String fcmToken) {
        return FcmToken.builder().memberId(memberId).fcmToken(fcmToken).build();
    }

    public static FcmToken create(
        Long memberId,
        String fcmToken,
        String platform,
        String deviceId,
        String appVersion
    ) {
        return FcmToken.builder()
            .memberId(memberId)
            .fcmToken(fcmToken)
            .platform(platform)
            .deviceId(deviceId)
            .appVersion(appVersion)
            .build();
    }

    public boolean belongsTo(Long memberId) {
        return this.memberId != null && this.memberId.equals(memberId);
    }

    public void activate() {
        this.isActive = true;
        this.deactivatedAt = null;
    }

    public void register(String platform, String deviceId, String appVersion) {
        Instant registeredAt = Instant.now();
        this.platform = platform;
        this.deviceId = deviceId;
        this.appVersion = appVersion;
        this.lastRegisteredAt = registeredAt;
        this.lastValidatedAt = registeredAt;
        activate();
    }

    public void deactivate() {
        this.isActive = false;
        this.deactivatedAt = Instant.now();
    }

    public void markValidated() {
        this.lastValidatedAt = Instant.now();
    }
}
