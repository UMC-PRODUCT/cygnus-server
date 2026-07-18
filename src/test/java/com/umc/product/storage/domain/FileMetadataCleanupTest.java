package com.umc.product.storage.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;

class FileMetadataCleanupTest {

    private static final UUID TOKEN =
        UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CLAIMED_AT = Instant.parse("2026-07-18T00:00:00Z");
    private static final Instant FAILED_AT = Instant.parse("2026-07-18T00:01:00Z");

    @Test
    @DisplayName("transient cleanup 실패는 token과 claimedAt을 유지하고 backoff를 설정한다")
    void transient_failure는_cleanup_fence를_유지한다() {
        // given
        FileMetadata metadata = metadata();
        metadata.claimCleanup(TOKEN, CLAIMED_AT);
        Instant retryAt = FAILED_AT.plusSeconds(60);

        // when
        boolean recorded = metadata.recordCleanupFailure(TOKEN, FAILED_AT, retryAt, 10);

        // then
        assertThat(recorded).isTrue();
        assertThat(metadata.getCleanupClaimToken()).isEqualTo(TOKEN);
        assertThat(metadata.getCleanupClaimedAt()).isEqualTo(CLAIMED_AT);
        assertThat(metadata.getCleanupAttempts()).isOne();
        assertThat(metadata.getCleanupNextAttemptAt()).isEqualTo(retryAt);
        assertThat(metadata.getCleanupFailedAt()).isNull();
        assertThat(metadata.canDeleteWithCleanupClaim(TOKEN)).isFalse();
    }

    @Test
    @DisplayName("최종 cleanup 실패는 token을 유지한 FAILED fence로 격리한다")
    void final_failure는_cleanup_fence를_유지한다() {
        // given
        FileMetadata metadata = metadata();
        metadata.claimCleanup(TOKEN, CLAIMED_AT);

        // when
        boolean recorded = metadata.recordCleanupFailure(TOKEN, FAILED_AT, null, 1);

        // then
        assertThat(recorded).isTrue();
        assertThat(metadata.getCleanupClaimToken()).isEqualTo(TOKEN);
        assertThat(metadata.getCleanupClaimedAt()).isEqualTo(CLAIMED_AT);
        assertThat(metadata.getCleanupNextAttemptAt()).isNull();
        assertThat(metadata.getCleanupFailedAt()).isEqualTo(FAILED_AT);
        assertThat(metadata.canDeleteWithCleanupClaim(TOKEN)).isFalse();
    }

    private FileMetadata metadata() {
        return FileMetadata.builder()
            .fileId("cleanup-file")
            .originalFileName("cleanup-file.pdf")
            .category(FileCategory.ETC)
            .contentType("application/pdf")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("cleanup/cleanup-file.pdf")
            .uploadedMemberId(1L)
            .build();
    }
}
