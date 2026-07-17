package com.umc.product.storage.application.port.in.command.dto;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.umc.product.storage.domain.FileUsageCoordinate;

public record ReplaceFileUsagesCommand(
    FileUsageCoordinate owner,
    Set<String> fileIds,
    Long requesterMemberId
) {

    public ReplaceFileUsagesCommand {
        Objects.requireNonNull(owner, "파일 usage owner는 필수입니다.");
        Objects.requireNonNull(fileIds, "파일 usage snapshot은 필수입니다.");
        if (fileIds.stream().anyMatch(fileId -> fileId == null || fileId.isBlank())) {
            throw new IllegalArgumentException("파일 usage file ID는 비어 있을 수 없습니다.");
        }
        fileIds = Collections.unmodifiableSet(new LinkedHashSet<>(fileIds));
    }
}
