package com.umc.product.storage.application.service;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.in.command.RunFileCleanupUseCase;
import com.umc.product.storage.application.port.in.command.dto.FileCleanupBatchResult;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaim;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileCleanupService implements RunFileCleanupUseCase {

    private final FileCleanupClaimService claimService;
    private final StoragePort storagePort;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileCleanupBatchResult cleanupOrphans() {
        List<FileCleanupClaim> claims = claimService.claimBatch();
        int deleted = 0;
        int retryScheduled = 0;

        for (FileCleanupClaim claim : claims) {
            try {
                storagePort.delete(claim.storageKey());
            } catch (StorageException exception) {
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
