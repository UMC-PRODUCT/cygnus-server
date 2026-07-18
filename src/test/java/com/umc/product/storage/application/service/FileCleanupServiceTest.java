package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.in.command.dto.FileCleanupBatchResult;
import com.umc.product.storage.application.port.out.FileCleanupClaimPort;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaim;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaimCriteria;
import com.umc.product.storage.application.port.out.dto.FileCleanupFailure;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
class FileCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    private FileCleanupClaimService claimService;

    @Mock
    private StoragePort storagePort;

    @Mock
    private FileCleanupClaimPort claimPort;

    @Mock
    private FileUsageRegistryReadinessPort readinessPort;

    @Mock
    private FileCleanupProperties cleanupProperties;

    private FileCleanupService sut;

    @BeforeEach
    void setUp() {
        sut = new FileCleanupService(
            claimService,
            storagePort,
            readinessPort,
            cleanupProperties
        );
        lenient().when(readinessPort.getStatus()).thenReturn(FileUsageRegistryStatus.READY);
        lenient().when(cleanupProperties.enabled()).thenReturn(true);
        lenient().when(claimService.validateDeletionFence(any())).thenReturn(true);
    }

    @Test
    @DisplayName("claim과 S3 직전 fence transaction이 끝난 뒤 delete와 CAS finalize를 순서대로 수행한다")
    void claim_밖에서_S3를_삭제하고_CAS_finalize한다() {
        // given
        FileCleanupClaim first = claim("file-a", 1);
        FileCleanupClaim second = claim("file-b", 1);
        given(claimService.claimBatch()).willReturn(List.of(first, second));
        given(claimService.finalizeDeletion(first)).willReturn(true);
        given(claimService.finalizeDeletion(second)).willReturn(true);

        // when
        FileCleanupBatchResult result = sut.cleanupOrphans();

        // then
        assertThat(result).isEqualTo(new FileCleanupBatchResult(2, 2, 0));
        InOrder ordered = inOrder(claimService, storagePort);
        ordered.verify(claimService).claimBatch();
        ordered.verify(claimService).validateDeletionFence(first);
        ordered.verify(storagePort).delete(first.storageKey());
        ordered.verify(claimService).finalizeDeletion(first);
        ordered.verify(claimService).validateDeletionFence(second);
        ordered.verify(storagePort).delete(second.storageKey());
        ordered.verify(claimService).finalizeDeletion(second);
    }

    @Test
    @DisplayName("S3 삭제 실패는 matching token failure로 기록하고 다음 claim 처리를 계속한다")
    void S3_삭제_실패는_retry를_기록하고_다음_claim을_계속한다() {
        // given
        FileCleanupClaim failed = claim("file-a", 1);
        FileCleanupClaim succeeded = claim("file-b", 1);
        given(claimService.claimBatch()).willReturn(List.of(failed, succeeded));
        willThrow(new StorageException(StorageErrorCode.STORAGE_DELETE_FAILED))
            .given(storagePort).delete(failed.storageKey());
        given(claimService.recordFailure(failed)).willReturn(true);
        given(claimService.finalizeDeletion(succeeded)).willReturn(true);

        // when
        FileCleanupBatchResult result = sut.cleanupOrphans();

        // then
        assertThat(result).isEqualTo(new FileCleanupBatchResult(2, 1, 1));
        then(claimService).should().recordFailure(failed);
        then(claimService).should(never()).finalizeDeletion(failed);
        then(storagePort).should().delete(succeeded.storageKey());
    }

    @Test
    @DisplayName("S3 삭제 후 DB finalize가 일시 실패하면 같은 token에 retry를 기록한다")
    void DB_finalize_실패는_retry를_기록한다() {
        // given
        FileCleanupClaim claim = claim("file-a", 2);
        given(claimService.claimBatch()).willReturn(List.of(claim));
        given(claimService.finalizeDeletion(claim))
            .willThrow(new TransientDataAccessResourceException("finalize unavailable"));
        given(claimService.recordFailure(claim)).willReturn(true);

        // when
        FileCleanupBatchResult result = sut.cleanupOrphans();

        // then
        assertThat(result).isEqualTo(new FileCleanupBatchResult(1, 0, 1));
        then(storagePort).should().delete(claim.storageKey());
        then(claimService).should().recordFailure(claim);
    }

    @Test
    @DisplayName("finalize 시 token 또는 usage 조건이 달라진 claim은 retry 상태를 덮어쓰지 않는다")
    void CAS_finalize_no_op은_failure를_기록하지_않는다() {
        // given
        FileCleanupClaim claim = claim("file-a", 1);
        given(claimService.claimBatch()).willReturn(List.of(claim));
        given(claimService.finalizeDeletion(claim)).willReturn(false);

        // when
        FileCleanupBatchResult result = sut.cleanupOrphans();

        // then
        assertThat(result).isEqualTo(new FileCleanupBatchResult(1, 0, 0));
        then(claimService).should(never()).recordFailure(claim);
    }

    @Test
    @DisplayName("S3 직전 fence가 stale token을 거부하면 delete와 finalize를 수행하지 않는다")
    void stale_token은_S3_delete와_finalize를_수행하지_않는다() {
        // given
        FileCleanupClaim stale = claim("file-a", 1);
        given(claimService.claimBatch()).willReturn(List.of(stale));
        given(claimService.validateDeletionFence(stale)).willReturn(false);

        // when
        FileCleanupBatchResult result = sut.cleanupOrphans();

        // then
        assertThat(result).isEqualTo(new FileCleanupBatchResult(1, 0, 0));
        then(storagePort).should(never()).delete(stale.storageKey());
        then(claimService).should(never()).finalizeDeletion(stale);
        then(claimService).should(never()).recordFailure(stale);
    }

    @Test
    @DisplayName("cleanup orchestrator는 transaction을 중단해 S3 호출 중 DB lock을 유지하지 않는다")
    void cleanup_orchestrator는_NOT_SUPPORTED_transaction이다() throws NoSuchMethodException {
        // when
        Method cleanup = FileCleanupService.class.getMethod("cleanupOrphans");

        // then
        assertThat(cleanup.getAnnotation(Transactional.class).propagation())
            .isEqualTo(Propagation.NOT_SUPPORTED);
    }

    @Test
    @DisplayName("S3 직전 fence 검증은 독립 DB transaction에서 commit한다")
    void deletion_fence는_REQUIRES_NEW_transaction이다() throws NoSuchMethodException {
        // when
        Method validateFence = FileCleanupClaimService.class.getMethod(
            "validateDeletionFence",
            FileCleanupClaim.class
        );

        // then
        assertThat(validateFence.getAnnotation(Transactional.class).propagation())
            .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    @DisplayName("claim 뒤 registry가 DISABLED가 되면 S3 호출 직전 cleanup을 중단한다")
    void claim_뒤_registry_disabled면_S3_delete를_호출하지_않는다() {
        FileCleanupClaim claim = claim("file-a", 1);
        given(claimService.claimBatch()).willReturn(List.of(claim));
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.DISABLED);

        FileCleanupBatchResult result = sut.cleanupOrphans();

        assertThat(result.deleted()).isZero();
        then(claimService).should(never()).validateDeletionFence(any());
        then(storagePort).should(never()).delete(org.mockito.ArgumentMatchers.anyString());
        then(claimService).should(never()).finalizeDeletion(any());
    }

    @Test
    @DisplayName("claim 뒤 cleanup property가 false면 S3 호출 직전 cleanup을 중단한다")
    void claim_뒤_cleanup_property_false면_S3_delete를_호출하지_않는다() {
        FileCleanupClaim claim = claim("file-a", 1);
        given(claimService.claimBatch()).willReturn(List.of(claim));
        given(cleanupProperties.enabled()).willReturn(false);

        FileCleanupBatchResult result = sut.cleanupOrphans();

        assertThat(result.deleted()).isZero();
        then(claimService).should(never()).validateDeletionFence(any());
        then(storagePort).should(never()).delete(org.mockito.ArgumentMatchers.anyString());
        then(claimService).should(never()).finalizeDeletion(any());
    }

    @Test
    @DisplayName("claim transaction은 disabled를 readiness 조회 전 다시 확인한다")
    void claim은_disabled를_다시_확인한다() {
        // given
        FileCleanupClaimService transactionService = transactionService(properties(false, 10));

        // when
        List<FileCleanupClaim> claims = transactionService.claimBatch();

        // then
        assertThat(claims).isEmpty();
        then(readinessPort).shouldHaveNoInteractions();
        then(claimPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("claim transaction은 enabled여도 registry가 READY가 아니면 port를 호출하지 않는다")
    void claim은_READY를_다시_확인한다() {
        // given
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.DISABLED);
        FileCleanupClaimService transactionService = transactionService(properties(true, 10));

        // when
        List<FileCleanupClaim> claims = transactionService.claimBatch();

        // then
        assertThat(claims).isEmpty();
        then(claimPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("READY claim은 현재 시각에서 retention과 timeout cutoff를 계산한다")
    void READY_claim은_typed_cutoff로_port를_호출한다() {
        // given
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(claimPort.claimBatch(any(FileCleanupClaimCriteria.class))).willReturn(List.of());
        FileCleanupClaimService transactionService = transactionService(properties(true, 10));
        ArgumentCaptor<FileCleanupClaimCriteria> criteriaCaptor =
            ArgumentCaptor.forClass(FileCleanupClaimCriteria.class);

        // when
        transactionService.claimBatch();

        // then
        then(claimPort).should().claimBatch(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue()).isEqualTo(new FileCleanupClaimCriteria(
            NOW,
            NOW.minus(Duration.ofHours(24)),
            NOW.minus(Duration.ofHours(168)),
            NOW.minus(Duration.ofMinutes(15)),
            100,
            10
        ));
    }

    @Test
    @DisplayName("실패 attempt는 지수 backoff를 적용하고 max attempt는 FAILED 입력으로 기록한다")
    void failure는_exponential_backoff와_max_attempts를_적용한다() {
        // given
        FileCleanupClaim retryClaim = claim("file-a", 3);
        FileCleanupClaim finalClaim = claim("file-b", 10);
        given(claimPort.recordFailure(any(FileCleanupFailure.class))).willReturn(true);
        FileCleanupClaimService transactionService = transactionService(properties(true, 10));
        ArgumentCaptor<FileCleanupFailure> failureCaptor = ArgumentCaptor.forClass(FileCleanupFailure.class);

        // when
        boolean retryRecorded = transactionService.recordFailure(retryClaim);
        boolean failedRecorded = transactionService.recordFailure(finalClaim);

        // then
        assertThat(retryRecorded).isTrue();
        assertThat(failedRecorded).isTrue();
        then(claimPort).should(org.mockito.Mockito.times(2)).recordFailure(failureCaptor.capture());
        assertThat(failureCaptor.getAllValues().get(0).nextAttemptAt())
            .isEqualTo(NOW.plus(Duration.ofMinutes(4)));
        assertThat(failureCaptor.getAllValues().get(1).nextAttemptAt()).isNull();
    }

    private FileCleanupClaimService transactionService(FileCleanupProperties properties) {
        return new FileCleanupClaimService(
            claimPort,
            readinessPort,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private FileCleanupProperties properties(boolean enabled, int maxAttempts) {
        return new FileCleanupProperties(
            enabled,
            Duration.ofMinutes(1),
            Duration.ofHours(24),
            Duration.ofHours(168),
            Duration.ofMinutes(15),
            100,
            maxAttempts,
            Duration.ofMinutes(1),
            Duration.ofHours(6)
        );
    }

    private FileCleanupClaim claim(String fileId, int attempt) {
        UUID token = UUID.nameUUIDFromBytes((fileId + attempt).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new FileCleanupClaim(fileId, "cleanup-test/" + fileId, token, attempt);
    }
}
