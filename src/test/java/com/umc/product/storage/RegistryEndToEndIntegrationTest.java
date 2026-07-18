package com.umc.product.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.umc.product.chat.application.port.out.ChatRoomOwnerPolicy;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.registry.support.ControllableStoragePort;
import com.umc.product.registry.support.MutableTestClock;
import com.umc.product.registry.support.RegistryEndToEndDatabaseProbe;
import com.umc.product.registry.support.RegistryEndToEndDatabaseProbe.FileState;
import com.umc.product.registry.support.RegistryEndToEndScenario;
import com.umc.product.registry.support.RegistryEndToEndScenario.FormAttachment;
import com.umc.product.registry.support.RegistryEndToEndScenario.FormFixture;
import com.umc.product.registry.support.RegistryEndToEndScenario.UploadedFile;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.RunFileCleanupUseCase;
import com.umc.product.storage.application.port.in.command.dto.FileCleanupBatchResult;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.service.FileCleanupClaimService;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;
import com.umc.product.support.IntegrationTestSupport;

@Import({
    RegistryEndToEndScenario.class,
    RegistryEndToEndIntegrationTest.E2eConfiguration.class
})
@TestPropertySource(properties = {
    "app.storage.cleanup.enabled=true",
    "app.storage.cleanup.unreferenced-retention=PT1H",
    "app.storage.cleanup.initial-backoff=PT1M",
    "app.storage.cleanup.max-backoff=PT5M"
})
@DisplayName("Storage usage와 engine attachment registry E2E")
class RegistryEndToEndIntegrationTest extends IntegrationTestSupport {

    private static final Instant START = Instant.parse("2026-07-18T00:00:00Z");

    private final ControllableStoragePort storage = new ControllableStoragePort();

    @Autowired
    private RegistryEndToEndScenario scenario;
    @Autowired
    private RegistryEndToEndDatabaseProbe database;
    @Autowired
    private MutableTestClock clock;
    @Autowired
    private RunFileCleanupUseCase cleanupUseCase;
    @Autowired
    private ManageFileUsageUseCase manageFileUsageUseCase;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private FileCleanupClaimService claimService;
    @Autowired
    private MutableFileUsageRegistryReadiness readiness;

    @BeforeEach
    void resetExternalBoundary() {
        clock.reset(START);
        readiness.setStatus(FileUsageRegistryStatus.READY);
        storage.reset();
        storage.bindTo(storagePort);
    }

    @AfterEach
    void dropFinalizeFailureFixture() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS cleanup_fencing_finalize_blocker");
    }

    @Test
    @DisplayName("upload confirm부터 Form·Chat 공유 보호와 마지막 detach 후 S3 finalize까지 완료한다")
    void uploadConfirmAttachShareDetachAndCleanup() {
        // given: pending metadata를 먼저 기록하고 fake object HEAD로 canonical confirm한다.
        UploadedFile file = scenario.preparePendingPdf();
        assertThat(database.fileState(file.fileId()))
            .isEqualTo(new FileState(false, null, null, null, null, 0, null, null));

        scenario.confirmPdf(file, storage);
        assertThat(database.fileState(file.fileId()))
            .isEqualTo(new FileState(true, START, START, null, null, 0, null, null));

        FormFixture form = scenario.createForm();
        assertThatThrownBy(() -> scenario.addSectionWithWrongNamespace(form))
            .isInstanceOf(FormDomainException.class)
            .extracting(error -> ((FormDomainException) error).getBaseCode())
            .isEqualTo(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        assertThat(database.formSectionCount(form.formId())).isZero();

        // when: 실제 Form answer와 Chat message command가 같은 파일의 서로 다른 usage를 만든다.
        FormAttachment formAttachment = scenario.attachForm(form, file.fileId());
        var chatAttachment = scenario.attachChat(file.fileId());

        // then: 첫 detach는 공유 파일을 보호하고 마지막 detach만 유예 시각을 기록한다.
        assertThat(database.usageRows(file.fileId()))
            .containsExactly(
                "chat.message/" + chatAttachment.messageId() + "/attachments",
                "form.answer/" + formAttachment.answerId() + "/attachments"
            );
        clock.advance(Duration.ofMinutes(30));
        scenario.detachForm(formAttachment);
        assertThat(database.usageRows(file.fileId())).singleElement()
            .isEqualTo("chat.message/" + chatAttachment.messageId() + "/attachments");
        assertThat(database.fileState(file.fileId()))
            .isEqualTo(new FileState(true, START, null, null, null, 0, null, null));
        scenario.detachChat(chatAttachment);
        assertThat(database.usageRows(file.fileId())).isEmpty();
        assertThat(database.fileState(file.fileId())).isEqualTo(new FileState(
            true, START, START.plus(Duration.ofMinutes(30)), null, null, 0, null, null
        ));

        assertThat(cleanupUseCase.cleanupOrphans().claimed()).isZero();
        clock.advance(Duration.ofHours(2));
        readiness.setStatus(FileUsageRegistryStatus.DISABLED);
        assertThat(cleanupUseCase.cleanupOrphans().claimed()).isZero();
        assertThat(storage.calls()).noneMatch(call -> call.startsWith("delete:"));

        readiness.setStatus(FileUsageRegistryStatus.READY);
        storage.observeActiveClaim(() -> database.claimObservation(file.fileId()));
        FileCleanupBatchResult result = cleanupUseCase.cleanupOrphans();

        assertThat(result).isEqualTo(new FileCleanupBatchResult(1, 1, 0));
        assertThat(storage.claimObservations()).singleElement().satisfies(observation -> {
            assertThat(observation.token()).isNotNull();
            assertThat(observation.claimedAt()).isEqualTo(clock.instant());
            assertThat(observation.attempt()).isOne();
        });
        assertThat(storage.calls()).containsExactly(
            "upload-url:" + file.storageKey(),
            "head:" + file.storageKey(),
            "delete:" + file.storageKey() + ":active-claim=true"
        );
        assertThat(database.metadataCount(file.fileId())).isZero();
    }

    @Test
    @DisplayName("다른 uploader는 usage를 만들지 못하고 물리 삭제도 호출하지 않는다")
    void wrongUploaderIsDeniedWithoutPartialUsage() {
        UploadedFile file = scenario.uploadConfirmedPdf(storage);

        assertThatThrownBy(() -> scenario.attachWithWrongUploader(file.fileId()))
            .isInstanceOf(StorageException.class)
            .extracting(error -> ((StorageException) error).getBaseCode())
            .isEqualTo(StorageErrorCode.FILE_USE_FORBIDDEN);

        assertThat(database.usageRows(file.fileId())).isEmpty();
        assertThat(storage.calls()).containsExactly(
            "upload-url:" + file.storageKey(),
            "head:" + file.storageKey()
        );
    }

    @Test
    @DisplayName("S3 transient 실패는 claim을 유지하고 backoff 뒤 새 token으로 reclaim한다")
    void transientStorageFailureRetriesWithGuardedClaim() {
        UploadedFile file = scenario.uploadConfirmedPdf(storage);
        clock.advance(Duration.ofHours(2));
        storage.observeActiveClaim(() -> database.claimObservation(file.fileId()));
        storage.failNextDelete();

        FileCleanupBatchResult failed = cleanupUseCase.cleanupOrphans();

        assertThat(failed).isEqualTo(new FileCleanupBatchResult(1, 0, 1));
        var firstClaim = storage.claimObservations().getFirst();
        assertThat(database.fileState(file.fileId()))
            .isEqualTo(new FileState(
                true, START, START, firstClaim.token(), firstClaim.claimedAt(), 1,
                clock.instant().plus(Duration.ofMinutes(1)), null
            ));
        assertThat(storage.containsObject(file.storageKey())).isTrue();

        clock.advance(Duration.ofMinutes(1));
        FileCleanupBatchResult retried = cleanupUseCase.cleanupOrphans();

        assertThat(retried).isEqualTo(new FileCleanupBatchResult(1, 1, 0));
        assertThat(storage.claimObservations())
            .extracting(observation -> observation.attempt())
            .containsExactly(1, 2);
        assertThat(storage.claimObservations())
            .extracting(observation -> observation.token())
            .doesNotHaveDuplicates();
        assertThat(storage.calls()).filteredOn(call -> call.startsWith("delete:"))
            .containsExactly(
                "delete:" + file.storageKey() + ":active-claim=true",
                "delete:" + file.storageKey() + ":active-claim=true"
            );
        assertThat(database.metadataCount(file.fileId())).isZero();
    }

    @Test
    @ResourceLock("storage-cleanup-finalize-failure")
    @DisplayName("S3 삭제 성공 후 DB finalize 실패는 claim을 유지하고 attach를 거부한다")
    void finalizeFailureKeepsFenceAndRejectsAttach() {
        // given
        UploadedFile file = scenario.uploadConfirmedPdf(storage);
        clock.advance(Duration.ofHours(2));
        storage.observeActiveClaim(() -> database.claimObservation(file.fileId()));
        jdbcTemplate.execute("""
            CREATE TABLE cleanup_fencing_finalize_blocker (
                file_id VARCHAR(255) PRIMARY KEY
                    REFERENCES file_metadata(id) ON DELETE RESTRICT
            )
            """);
        jdbcTemplate.update(
            "INSERT INTO cleanup_fencing_finalize_blocker (file_id) VALUES (?)",
            file.fileId()
        );

        // when
        FileCleanupBatchResult result = cleanupUseCase.cleanupOrphans();

        // then
        assertThat(result).isEqualTo(new FileCleanupBatchResult(1, 0, 1));
        assertThat(storage.containsObject(file.storageKey())).isFalse();
        assertThatThrownBy(() -> manageFileUsageUseCase.replaceUsages(
            new ReplaceFileUsagesCommand(
                FileUsageCoordinate.of("e2e.cleanup", "finalize-failure", "attachment"),
                Set.of(file.fileId()),
                RegistryEndToEndScenario.MEMBER_ID
            )
        ))
            .isInstanceOf(StorageException.class)
            .extracting(error -> ((StorageException) error).getBaseCode())
            .isEqualTo(StorageErrorCode.FILE_CLEANUP_IN_PROGRESS);

        var claim = storage.claimObservations().getFirst();
        assertThat(database.fileState(file.fileId())).isEqualTo(new FileState(
            true,
            START,
            START,
            claim.token(),
            claim.claimedAt(),
            1,
            clock.instant().plus(Duration.ofMinutes(1)),
            null
        ));
        assertThat(database.usageRows(file.fileId())).isEmpty();
        assertThat(storage.calls()).filteredOn(call -> call.startsWith("delete:"))
            .containsExactly("delete:" + file.storageKey() + ":active-claim=true");
    }

    @Test
    @DisplayName("T1 timeout 후 T2 reclaim이 실패하면 attach를 거부하고 stale T1 side effect를 막는다")
    void reclaimedFailureRejectsAttachAndFencesResumedStaleWorker() {
        // given: T1이 claim한 뒤 S3 호출 전에 timeout된다.
        UploadedFile file = scenario.uploadConfirmedPdf(storage);
        clock.advance(Duration.ofHours(2));
        var staleT1 = claimService.claimBatch().getFirst();
        clock.advance(Duration.ofMinutes(16));
        storage.observeActiveClaim(() -> database.claimObservation(file.fileId()));
        storage.failNextDelete();

        // when: T2가 새 token으로 reclaim하고 S3 transient 실패를 기록한다.
        FileCleanupBatchResult t2Result = cleanupUseCase.cleanupOrphans();
        var t2Claim = storage.claimObservations().getFirst();

        // then: backoff fence가 attach를 거부한다.
        assertThat(t2Result).isEqualTo(new FileCleanupBatchResult(1, 0, 1));
        assertThat(t2Claim.token()).isNotEqualTo(staleT1.token());
        assertThat(t2Claim.attempt()).isEqualTo(2);
        assertThatThrownBy(() -> manageFileUsageUseCase.replaceUsages(
            new ReplaceFileUsagesCommand(
                FileUsageCoordinate.of("e2e.cleanup", "stale-worker", "attachment"),
                Set.of(file.fileId()),
                RegistryEndToEndScenario.MEMBER_ID
            )
        ))
            .isInstanceOf(StorageException.class)
            .extracting(error -> ((StorageException) error).getBaseCode())
            .isEqualTo(StorageErrorCode.FILE_CLEANUP_IN_PROGRESS);

        // when: timeout된 T1이 다시 S3 삭제 직전 fence에 진입한다.
        int storageCallsBeforeT1Resume = storage.calls().size();
        boolean staleDeleteAllowed = claimService.validateDeletionFence(staleT1);
        boolean staleFinalized = claimService.finalizeDeletion(staleT1);

        // then: stale token은 외부 호출도 DB finalize도 하지 못한다.
        assertThat(staleDeleteAllowed).isFalse();
        assertThat(staleFinalized).isFalse();
        assertThat(storage.calls()).hasSize(storageCallsBeforeT1Resume);
        assertThat(storage.calls()).filteredOn(call -> call.startsWith("delete:"))
            .containsExactly("delete:" + file.storageKey() + ":active-claim=true");
        assertThat(storage.containsObject(file.storageKey())).isTrue();
        assertThat(database.usageRows(file.fileId())).isEmpty();
        assertThat(database.fileState(file.fileId())).isEqualTo(new FileState(
            true,
            START,
            START,
            t2Claim.token(),
            t2Claim.claimedAt(),
            2,
            clock.instant().plus(Duration.ofMinutes(2)),
            null
        ));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class E2eConfiguration {

        @Bean
        @Primary
        MutableTestClock registryEndToEndClock() {
            return new MutableTestClock(START);
        }

        @Bean
        @Primary
        MutableFileUsageRegistryReadiness registryEndToEndReadiness() {
            return new MutableFileUsageRegistryReadiness();
        }

        @Bean
        RegistryEndToEndDatabaseProbe registryEndToEndDatabaseProbe(
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate
        ) {
            return new RegistryEndToEndDatabaseProbe(jdbcTemplate);
        }

        @Bean
        ChatRoomOwnerPolicy registryEndToEndChatOwnerPolicy() {
            return new ChatRoomOwnerPolicy() {
                @Override
                public String namespace() {
                    return RegistryEndToEndScenario.CHAT_NAMESPACE;
                }

                @Override
                public boolean allows(
                    ChatRoomOwnerReference owner,
                    ChatRoomOperation operation,
                    ChatRoomActorContext actor
                ) {
                    return owner != null
                        && RegistryEndToEndScenario.CHAT_NAMESPACE.equals(owner.namespace())
                        && actor != null
                        && Long.valueOf(RegistryEndToEndScenario.MEMBER_ID).equals(actor.actorMemberId());
                }
            };
        }
    }

    static class MutableFileUsageRegistryReadiness implements FileUsageRegistryReadinessPort {

        private FileUsageRegistryStatus status = FileUsageRegistryStatus.READY;

        @Override
        public FileUsageRegistryStatus getStatus() {
            return status;
        }

        void setStatus(FileUsageRegistryStatus status) {
            this.status = status;
        }
    }
}
