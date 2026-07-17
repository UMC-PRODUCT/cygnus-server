package com.umc.product.storage.application.port.in.command.dto;

import java.util.List;
import java.util.Objects;

import com.umc.product.storage.domain.FileUsageCoordinate;

public record BulkRemoveFileUsagesCommand(
    List<FileUsageCoordinate> owners
) {

    public BulkRemoveFileUsagesCommand {
        Objects.requireNonNull(owners, "제거할 파일 usage owner 목록은 필수입니다.");
        owners = List.copyOf(owners);
    }
}
