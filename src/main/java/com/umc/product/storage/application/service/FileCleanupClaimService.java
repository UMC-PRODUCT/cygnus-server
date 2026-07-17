package com.umc.product.storage.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.out.FileCleanupClaimPort;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaim;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaimCriteria;
import com.umc.product.storage.application.port.out.dto.FileCleanupFailure;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileCleanupClaimService {

    private final FileCleanupClaimPort claimPort;
    private final FileUsageRegistryReadinessPort readinessPort;
    private final FileCleanupProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<FileCleanupClaim> claimBatch() {
        if (!properties.enabled() || readinessPort.getStatus() != FileUsageRegistryStatus.READY) {
            return List.of();
        }

        Instant now = clock.instant();
        return claimPort.claimBatch(new FileCleanupClaimCriteria(
            now,
            now.minus(properties.pendingRetention()),
            now.minus(properties.unreferencedRetention()),
            now.minus(properties.claimTimeout()),
            properties.batchSize(),
            properties.maxAttempts()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean finalizeDeletion(FileCleanupClaim claim) {
        return claimPort.finalizeDeletion(claim);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(FileCleanupClaim claim) {
        Instant failedAt = clock.instant();
        Instant nextAttemptAt = claim.attempt() >= properties.maxAttempts()
            ? null
            : failedAt.plus(properties.backoffForAttempt(claim.attempt()));
        return claimPort.recordFailure(new FileCleanupFailure(
            claim,
            failedAt,
            nextAttemptAt,
            properties.maxAttempts()
        ));
    }
}
