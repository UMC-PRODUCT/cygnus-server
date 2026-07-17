package com.umc.product.storage.application.port.out;

import java.util.List;

import com.umc.product.storage.domain.FileMetadata;

public interface LockFileMetadataPort {

    /**
     * 중복을 제거한 file ID 오름차순으로 metadata row에 비관적 쓰기 lock을 획득합니다.
     */
    List<FileMetadata> lockAllByFileIds(List<String> fileIds);
}
