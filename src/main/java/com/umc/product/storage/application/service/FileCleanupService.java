package com.umc.product.storage.application.service;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.in.command.RunFileCleanupUseCase;
import com.umc.product.storage.application.port.in.command.dto.FileCleanupBatchResult;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaim;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.storage.domain.exception.StorageException;

@Service
public class FileCleanupService implements RunFileCleanupUseCase {

    private final FileCleanupClaimService claimService;
    private final StoragePort storagePort;
    private final FileUsageRegistryReadinessPort readinessPort;
    private final FileCleanupProperties properties;

    @org.springframework.beans.factory.annotation.Autowired
    public FileCleanupService(
        FileCleanupClaimService claimService,
        StoragePort storagePort,
        FileUsageRegistryReadinessPort readinessPort,
        FileCleanupProperties properties
    ) {
        this.claimService = claimService;
        this.storagePort = storagePort;
        this.readinessPort = readinessPort;
        this.properties = properties;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileCleanupBatchResult cleanupOrphans() {
        List<FileCleanupClaim> claims = claimService.claimBatch();
        int deleted = 0;
        int retryScheduled = 0;

        for (FileCleanupClaim claim : claims) {
            if (!properties.enabled()
                || readinessPort.getStatus() != FileUsageRegistryStatus.READY) {
                continue;
            }
            try {
                if (!claimService.validateDeletionFence(claim)) {
                    continue;
                }
                storagePort.delete(claim.storageKey());
            } catch (StorageException | DataAccessException | TransactionException exception) {
                if (claimService.recordFailure(claim)) {
                    retryScheduled++;
                }
                continue;
            }

            try {
                if (claimService.finalizeDeletion(claim)) {
                    deleted++;
                }
            } catch (DataAccessException | TransactionException exception) {
                if (claimService.recordFailure(claim)) {
                    retryScheduled++;
                }
            }
        }

        return new FileCleanupBatchResult(claims.size(), deleted, retryScheduled);
    }
}
