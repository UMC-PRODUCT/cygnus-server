package com.umc.product.notification.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.notification.application.port.in.ValidateFcmTokensUseCase;
import com.umc.product.notification.application.port.in.dto.FcmTokenValidationInfo;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.application.port.out.ValidateFcmTokenPort;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationRequest;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationResult;
import com.umc.product.notification.domain.FcmToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class FcmTokenValidationService implements ValidateFcmTokensUseCase {

    private final FcmProperties fcmProperties;
    private final LoadFcmPort loadFcmPort;
    private final SaveFcmPort saveFcmPort;
    private final ValidateFcmTokenPort validateFcmTokenPort;
    private final OperationalMetrics operationalMetrics;
    private final int batchSize;
    private final Duration staleDuration;

    public FcmTokenValidationService(
        FcmProperties fcmProperties,
        LoadFcmPort loadFcmPort,
        SaveFcmPort saveFcmPort,
        ValidateFcmTokenPort validateFcmTokenPort,
        OperationalMetrics operationalMetrics,
        @Value("${app.fcm.token-validation-batch-size:500}") int batchSize,
        @Value("${app.fcm.token-validation-stale-duration:P30D}") Duration staleDuration
    ) {
        this.fcmProperties = fcmProperties;
        this.loadFcmPort = loadFcmPort;
        this.saveFcmPort = saveFcmPort;
        this.validateFcmTokenPort = validateFcmTokenPort;
        this.operationalMetrics = operationalMetrics;
        this.batchSize = batchSize;
        this.staleDuration = staleDuration;
    }

    @Override
    public FcmTokenValidationInfo validateDueTokens() {
        Instant checkedAt = Instant.now();
        if (!fcmProperties.enabled() || !fcmProperties.tokenValidationEnabled()) {
            return FcmTokenValidationInfo.of(0, 0, checkedAt);
        }

        List<FcmToken> tokens = loadFcmPort.listActiveForValidation(checkedAt.minus(staleDuration), batchSize);
        if (tokens.isEmpty()) {
            return FcmTokenValidationInfo.of(0, 0, checkedAt);
        }

        FcmTokenValidationResult result = validateFcmTokenPort.validate(FcmTokenValidationRequest.of(targets(tokens)));
        updateTokenValidationState(tokens, result.validTokenIds(), result.invalidTokenIds());
        recordMetric(result);
        log.info("FCM 토큰 유효성 검증 결과: requested={}, invalid={}",
            tokens.size(), result.invalidTokenIds().size());
        return FcmTokenValidationInfo.of(tokens.size(), result.invalidTokenIds().size(), checkedAt);
    }

    private List<FcmSendTarget> targets(List<FcmToken> tokens) {
        return tokens.stream()
            .map(token -> FcmSendTarget.of(token.getId(), token.getFcmToken()))
            .toList();
    }

    private void updateTokenValidationState(List<FcmToken> tokens, List<Long> validTokenIds, List<Long> invalidTokenIds) {
        Set<Long> validTokenIdSet = new HashSet<>(validTokenIds);
        Set<Long> invalidTokenIdSet = new HashSet<>(invalidTokenIds);
        List<FcmToken> updatedTokens = new ArrayList<>();
        tokens.forEach(token -> {
            if (invalidTokenIdSet.contains(token.getId())) {
                token.deactivate();
                updatedTokens.add(token);
                return;
            }
            if (validTokenIdSet.contains(token.getId())) {
                token.markValidated();
                updatedTokens.add(token);
            }
        });
        if (!updatedTokens.isEmpty()) {
            saveFcmPort.saveAll(updatedTokens);
        }
    }

    private void recordMetric(FcmTokenValidationResult result) {
        operationalMetrics.recordNotification("FCM", "VALIDATE_TOKEN", "success", result.successCount());
        operationalMetrics.recordNotification("FCM", "VALIDATE_TOKEN", "failure", result.failureCount());
    }
}
