package com.umc.product.storage.application.port.out;

import java.util.List;

import com.umc.product.storage.application.port.out.dto.FileCleanupClaim;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaimCriteria;
import com.umc.product.storage.application.port.out.dto.FileCleanupFailure;

public interface FileCleanupClaimPort {

    List<FileCleanupClaim> claimBatch(FileCleanupClaimCriteria criteria);

    boolean finalizeDeletion(FileCleanupClaim claim);

    boolean recordFailure(FileCleanupFailure failure);
}
