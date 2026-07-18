package com.umc.product.storage.adapter.out.persistence;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.storage.application.port.out.dto.FileCleanupClaim;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaimCriteria;
import com.umc.product.storage.application.port.out.dto.FileCleanupFailure;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(FileCleanupClaimPersistenceAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ResourceLock("file-upload-lifecycle-contract")
class FileCleanupClaimPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    private FileCleanupClaimPersistenceAdapter sut;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void useExpandOnlyLifecycleSchema() {
        jdbcTemplate.execute("""
            ALTER TABLE file_metadata
            DROP CONSTRAINT IF EXISTS ck_file_metadata_upload_lifecycle
            """);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM file_usage WHERE file_id LIKE 'cleanup-test-%'");
        jdbcTemplate.update("DELETE FROM file_usage_owner WHERE usage_namespace = 'cleanup-test'");
        jdbcTemplate.update("DELETE FROM file_metadata WHERE id LIKE 'cleanup-test-%'");
        jdbcTemplate.execute("""
            ALTER TABLE file_metadata
            ADD CONSTRAINT ck_file_metadata_upload_lifecycle
            CHECK (is_uploaded = (confirmed_at IS NOT NULL)) NOT VALID
            """);
        jdbcTemplate.execute("""
            ALTER TABLE file_metadata
            VALIDATE CONSTRAINT ck_file_metadata_upload_lifecycle
            """);
    }

    @Test
    @DisplayName("retention을 지난 pending과 confirmed unreferenced usage 0 파일만 claim한다")
    void retention과_usage를_원자적으로_재검증해_claim한다() {
        // given
        insertPending("cleanup-test-pending-old", NOW.minus(Duration.ofHours(25)), false);
        insertPending("cleanup-test-pending-fresh", NOW.minus(Duration.ofHours(23)), false);
        insertPending("cleanup-test-legacy-uploaded", NOW.minus(Duration.ofHours(30)), true);
        insertConfirmed("cleanup-test-unreferenced-old", NOW.minus(Duration.ofDays(8)));
        insertConfirmed("cleanup-test-unreferenced-fresh", NOW.minus(Duration.ofDays(6)));
        insertConfirmed("cleanup-test-in-use", NOW.minus(Duration.ofDays(8)));
        attachUsage("cleanup-test-in-use");

        // when
        List<FileCleanupClaim> claims = inTransaction(() -> sut.claimBatch(criteria(NOW, 100)));

        // then
        assertThat(claims).extracting(FileCleanupClaim::fileId)
            .containsExactlyInAnyOrder("cleanup-test-pending-old", "cleanup-test-unreferenced-old");
        assertThat(claims).allSatisfy(claim -> {
            assertThat(claim.token()).isNotNull();
            assertThat(claim.attempt()).isEqualTo(1);
        });
        assertThat(cleanupToken("cleanup-test-pending-fresh")).isNull();
        assertThat(cleanupToken("cleanup-test-legacy-uploaded")).isNull();
        assertThat(cleanupToken("cleanup-test-in-use")).isNull();
    }

    @Test
    @DisplayName("두 worker의 SKIP LOCKED batch claim은 서로 겹치지 않는다")
    void parallel_SKIP_LOCKED_claim은_disjoint하다() throws Exception {
        // given
        for (int index = 0; index < 4; index++) {
            insertPending("cleanup-test-parallel-" + index, NOW.minus(Duration.ofDays(2)), false);
        }
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // when
            Future<List<FileCleanupClaim>> first = executor.submit(() -> claimAfterSignal(ready, start));
            Future<List<FileCleanupClaim>> second = executor.submit(() -> claimAfterSignal(ready, start));
            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();
            Set<String> firstIds = fileIds(first.get(10, SECONDS));
            Set<String> secondIds = fileIds(second.get(10, SECONDS));

            // then
            assertThat(firstIds).hasSize(2);
            assertThat(secondIds).hasSize(2);
            assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
            Set<String> claimed = new HashSet<>(firstIds);
            claimed.addAll(secondIds);
            assertThat(claimed).containsExactlyInAnyOrder(
                "cleanup-test-parallel-0",
                "cleanup-test-parallel-1",
                "cleanup-test-parallel-2",
                "cleanup-test-parallel-3"
            );
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("claim timeout reclaim은 새 token을 발급하고 old token finalize를 no-op으로 만든다")
    void stale_claim은_새_token으로_reclaim한다() {
        // given
        insertPending("cleanup-test-stale", NOW.minus(Duration.ofDays(2)), false);
        FileCleanupClaim original = inTransaction(() -> sut.claimBatch(criteria(NOW, 1))).getFirst();

        // when
        List<FileCleanupClaim> beforeTimeout = inTransaction(() -> sut.claimBatch(criteria(NOW.plusSeconds(899), 1)));
        FileCleanupClaim reclaimed = inTransaction(() -> sut.claimBatch(criteria(NOW.plusSeconds(901), 1))).getFirst();
        boolean staleFinalized = inTransaction(() -> sut.finalizeDeletion(original));
        boolean staleFailure = inTransaction(() -> sut.recordFailure(new FileCleanupFailure(
            original,
            NOW.plusSeconds(902),
            NOW.plusSeconds(962),
            10
        )));

        // then
        assertThat(beforeTimeout).isEmpty();
        assertThat(reclaimed.token()).isNotEqualTo(original.token());
        assertThat(reclaimed.attempt()).isEqualTo(2);
        assertThat(staleFinalized).isFalse();
        assertThat(staleFailure).isFalse();
        assertThat(cleanupToken("cleanup-test-stale")).isEqualTo(reclaimed.token());
    }

    @Test
    @DisplayName("matching token과 usage 0인 cleanup receipt만 metadata를 삭제한다")
    void matching_receipt는_metadata를_삭제한다() {
        // given
        String fileId = "cleanup-test-finalize";
        insertPending(fileId, NOW.minus(Duration.ofDays(2)), false);
        FileCleanupClaim claim = inTransaction(() -> sut.claimBatch(criteria(NOW, 1))).getFirst();

        // when
        boolean finalized = inTransaction(() -> sut.finalizeDeletion(claim));
        boolean replayed = inTransaction(() -> sut.finalizeDeletion(claim));

        // then
        assertThat(finalized).isTrue();
        assertThat(replayed).isFalse();
        assertThat(fileExists(fileId)).isFalse();
    }

    @Test
    @DisplayName("failure token을 유지하고 due 시점에만 새 token으로 retry한 뒤 FAILED로 격리한다")
    void due_retry는_기존_token을_새_token으로_reclaim한다() {
        // given
        insertPending("cleanup-test-retry", NOW.minus(Duration.ofDays(2)), false);
        FileCleanupClaim first = inTransaction(() -> sut.claimBatch(criteria(NOW, 1))).getFirst();
        Instant retryAt = NOW.plus(Duration.ofMinutes(1));

        // when
        boolean recorded = inTransaction(() -> sut.recordFailure(
            new FileCleanupFailure(first, NOW, retryAt, 10)
        ));
        UUID retainedToken = cleanupToken("cleanup-test-retry");
        Instant retainedClaimedAt = cleanupClaimedAt("cleanup-test-retry");
        Instant scheduledAt = cleanupNextAttemptAt("cleanup-test-retry");
        boolean backoffDeletionAllowed = inTransaction(() -> sut.validateDeletionFence(first));
        List<FileCleanupClaim> early = inTransaction(() -> sut.claimBatch(criteria(retryAt.minusMillis(1), 1)));
        FileCleanupClaim retry = inTransaction(() -> sut.claimBatch(criteria(retryAt, 1))).getFirst();
        boolean failed = inTransaction(() -> sut.recordFailure(
            new FileCleanupFailure(retry, retryAt, retryAt.plusSeconds(1), 2)
        ));

        // then
        assertThat(recorded).isTrue();
        assertThat(retainedToken).isEqualTo(first.token());
        assertThat(retainedClaimedAt).isEqualTo(NOW);
        assertThat(scheduledAt).isEqualTo(retryAt);
        assertThat(backoffDeletionAllowed).isFalse();
        assertThat(early).isEmpty();
        assertThat(retry.token()).isNotEqualTo(first.token());
        assertThat(retry.attempt()).isEqualTo(2);
        assertThat(failed).isTrue();
        assertThat(cleanupFailedAt("cleanup-test-retry")).isEqualTo(retryAt);
        assertThat(cleanupToken("cleanup-test-retry")).isEqualTo(retry.token());
        assertThat(inTransaction(() -> sut.claimBatch(criteria(retryAt.plus(Duration.ofDays(1)), 1)))).isEmpty();
    }

    @Test
    @DisplayName("S3 직전 fence는 current token과 usage 0인 active claim만 허용한다")
    void deletion_fence는_current_active_unused_claim만_허용한다() {
        // given
        String fileId = "cleanup-test-delete-fence";
        insertPending(fileId, NOW.minus(Duration.ofDays(2)), false);
        FileCleanupClaim current = inTransaction(() -> sut.claimBatch(criteria(NOW, 1))).getFirst();
        FileCleanupClaim stale = new FileCleanupClaim(
            current.fileId(),
            current.storageKey(),
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            current.attempt()
        );

        // when
        boolean currentAllowed = inTransaction(() -> sut.validateDeletionFence(current));
        boolean staleAllowed = inTransaction(() -> sut.validateDeletionFence(stale));
        attachUsage(fileId);
        boolean usedAllowed = inTransaction(() -> sut.validateDeletionFence(current));

        // then
        assertThat(currentAllowed).isTrue();
        assertThat(staleAllowed).isFalse();
        assertThat(usedAllowed).isFalse();
    }

    @Test
    @DisplayName("마지막 attempt worker가 timeout되면 외부 호출 없이 FAILED로 격리한다")
    void max_attempt_stale_claim은_FAILED로_격리한다() {
        // given
        String fileId = "cleanup-test-stale-max";
        insertPending(fileId, NOW.minus(Duration.ofDays(2)), false);
        jdbcTemplate.update("""
            UPDATE file_metadata
            SET cleanup_claim_token = ?, cleanup_claimed_at = ?, cleanup_attempts = 10
            WHERE id = ?
            """,
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            atUtc(NOW.minus(Duration.ofMinutes(16))),
            fileId
        );

        // when
        List<FileCleanupClaim> claims = inTransaction(() -> sut.claimBatch(criteria(NOW, 1)));

        // then
        assertThat(claims).isEmpty();
        assertThat(cleanupToken(fileId))
            .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(cleanupFailedAt(fileId)).isEqualTo(NOW);
    }

    @Test
    @DisplayName("attach와 cleanup claim race는 usage와 active claim을 동시에 만들지 않는다")
    void attach_vs_claim은_metadata_lock으로_직렬화된다() throws Exception {
        // given
        String fileId = "cleanup-test-attach-race";
        insertConfirmed(fileId, NOW.minus(Duration.ofDays(8)));
        Long ownerId = insertOwner(fileId);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // when
            Future<Boolean> attached = executor.submit(() -> attachAfterSignal(fileId, ownerId, ready, start));
            Future<List<FileCleanupClaim>> claimed = executor.submit(() -> {
                awaitStart(ready, start);
                return inTransaction(() -> sut.claimBatch(criteria(NOW, 1)));
            });
            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();
            boolean attachWon = attached.get(10, SECONDS);
            List<FileCleanupClaim> claims = claimed.get(10, SECONDS);

            // then
            assertThat(attachWon).isNotEqualTo(!claims.isEmpty());
            assertThat(usageCount(fileId) > 0L).isEqualTo(attachWon);
            assertThat(cleanupToken(fileId) != null).isEqualTo(!attachWon);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
        }
    }

    private List<FileCleanupClaim> claimAfterSignal(CountDownLatch ready, CountDownLatch start) {
        awaitStart(ready, start);
        return inTransaction(() -> sut.claimBatch(criteria(NOW, 2)));
    }

    private boolean attachAfterSignal(
        String fileId,
        Long ownerId,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        awaitStart(ready, start);
        return inTransaction(() -> {
            String token = jdbcTemplate.queryForObject(
                "SELECT cleanup_claim_token::text FROM file_metadata WHERE id = ? FOR UPDATE",
                String.class,
                fileId
            );
            if (token != null) {
                return false;
            }
            jdbcTemplate.update(
                "INSERT INTO file_usage (owner_id, file_id, created_at) VALUES (?, ?, NOW())",
                ownerId,
                fileId
            );
            jdbcTemplate.update("UPDATE file_metadata SET unreferenced_at = NULL WHERE id = ?", fileId);
            return true;
        });
    }

    private void awaitStart(CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, SECONDS)) {
                throw new IllegalStateException("concurrency start signal timeout");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("concurrency test interrupted", exception);
        }
    }

    private <T> T inTransaction(Supplier<T> work) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> work.get());
    }

    private FileCleanupClaimCriteria criteria(Instant now, int batchSize) {
        return new FileCleanupClaimCriteria(
            now,
            now.minus(Duration.ofHours(24)),
            now.minus(Duration.ofHours(168)),
            now.minus(Duration.ofMinutes(15)),
            batchSize,
            10
        );
    }

    private Set<String> fileIds(List<FileCleanupClaim> claims) {
        Set<String> ids = new HashSet<>();
        claims.forEach(claim -> ids.add(claim.fileId()));
        return ids;
    }

    private void insertPending(String fileId, Instant createdAt, boolean uploaded) {
        insertFile(fileId, createdAt, uploaded, null, null);
    }

    private void insertConfirmed(String fileId, Instant unreferencedAt) {
        insertFile(fileId, NOW.minus(Duration.ofDays(10)), true, NOW.minus(Duration.ofDays(10)), unreferencedAt);
    }

    private void insertFile(
        String fileId,
        Instant createdAt,
        boolean uploaded,
        Instant confirmedAt,
        Instant unreferencedAt
    ) {
        jdbcTemplate.update("""
            INSERT INTO file_metadata (
                id, original_file_name, category, content_type, file_size,
                storage_provider, storage_key, uploaded_member_id, is_uploaded,
                confirmed_at, unreferenced_at, cleanup_attempts, created_at, updated_at
            ) VALUES (?, ?, 'ETC', 'application/octet-stream', 1,
                'AWS_S3', ?, 1, ?, ?, ?, 0, ?, ?)
            """,
            fileId,
            fileId + ".bin",
            "cleanup-test/" + fileId,
            uploaded,
            atUtc(confirmedAt),
            atUtc(unreferencedAt),
            atUtc(createdAt),
            atUtc(createdAt)
        );
    }

    private void attachUsage(String fileId) {
        Long ownerId = insertOwner(fileId);
        jdbcTemplate.update(
            "INSERT INTO file_usage (owner_id, file_id, created_at) VALUES (?, ?, NOW())",
            ownerId,
            fileId
        );
    }

    private Long insertOwner(String resourceKey) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO file_usage_owner (usage_namespace, resource_key, slot, created_at, updated_at)
            VALUES ('cleanup-test', ?, 'attachment', NOW(), NOW())
            RETURNING id
            """, Long.class, resourceKey);
    }

    private UUID cleanupToken(String fileId) {
        return jdbcTemplate.queryForObject(
            "SELECT cleanup_claim_token FROM file_metadata WHERE id = ?",
            UUID.class,
            fileId
        );
    }

    private Instant cleanupFailedAt(String fileId) {
        OffsetDateTime failedAt = jdbcTemplate.queryForObject(
            "SELECT cleanup_failed_at FROM file_metadata WHERE id = ?",
            OffsetDateTime.class,
            fileId
        );
        return failedAt == null ? null : failedAt.toInstant();
    }

    private Instant cleanupClaimedAt(String fileId) {
        return cleanupInstant(fileId, "cleanup_claimed_at");
    }

    private Instant cleanupNextAttemptAt(String fileId) {
        return cleanupInstant(fileId, "cleanup_next_attempt_at");
    }

    private Instant cleanupInstant(String fileId, String column) {
        OffsetDateTime value = jdbcTemplate.queryForObject(
            "SELECT " + column + " FROM file_metadata WHERE id = ?",
            OffsetDateTime.class,
            fileId
        );
        return value == null ? null : value.toInstant();
    }

    private long usageCount(String fileId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM file_usage WHERE file_id = ?",
            Long.class,
            fileId
        );
        return count == null ? 0L : count;
    }

    private boolean fileExists(String fileId) {
        Boolean exists = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM file_metadata WHERE id = ?)",
            Boolean.class,
            fileId
        );
        return Boolean.TRUE.equals(exists);
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
