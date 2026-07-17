package com.umc.product.storage.application.port.in.command.dto;

public record FileCleanupBatchResult(
    int claimed,
    int deleted,
    int retryScheduled
) {

    public FileCleanupBatchResult {
        if (claimed < 0 || deleted < 0 || retryScheduled < 0) {
            throw new IllegalArgumentException("cleanup batch 집계는 음수일 수 없습니다.");
        }
        if (deleted + retryScheduled > claimed) {
            throw new IllegalArgumentException("삭제와 retry 합계는 claim 수를 초과할 수 없습니다.");
        }
    }
}
