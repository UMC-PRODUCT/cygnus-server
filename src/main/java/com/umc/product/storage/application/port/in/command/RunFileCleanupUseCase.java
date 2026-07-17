package com.umc.product.storage.application.port.in.command;

import com.umc.product.storage.application.port.in.command.dto.FileCleanupBatchResult;

public interface RunFileCleanupUseCase {

    FileCleanupBatchResult cleanupOrphans();
}
