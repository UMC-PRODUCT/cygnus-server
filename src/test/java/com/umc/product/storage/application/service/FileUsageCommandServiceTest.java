package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.port.out.LoadFileUsagePort;
import com.umc.product.storage.application.port.out.LockFileMetadataPort;
import com.umc.product.storage.application.port.out.LockFileUsageOwnerPort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileUsagePort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.FileUsageOwner;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
class FileUsageCommandServiceTest {

    private static final FileUsageCoordinate COORDINATE =
        FileUsageCoordinate.of("project", "10", "attachments");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-07-16T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Mock
    private LockFileUsageOwnerPort lockFileUsageOwnerPort;

    @Mock
    private LockFileMetadataPort lockFileMetadataPort;

    @Mock
    private LoadFileUsagePort loadFileUsagePort;

    @Mock
    private SaveFileUsagePort saveFileUsagePort;

    @Mock
    private SaveFileMetadataPort saveFileMetadataPort;

    @Mock
    private FileUsageRegistryReadinessPort readinessPort;

    private FileUsageCommandService sut;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        sut = new FileUsageCommandService(
            lockFileUsageOwnerPort,
            lockFileMetadataPort,
            loadFileUsagePort,
            saveFileUsagePort,
            saveFileMetadataPort,
            readinessPort,
            clock
        );
    }

    @Test
    @DisplayName("replace는 owner lock 뒤 기존·신규 합집합을 정렬 lock하고 exact diff를 반영한다")
    void replace는_owner_lock_뒤_합집합을_정렬_lock하고_exact_diff를_반영한다() {
        // given
        FileUsageOwner owner = owner(10L);
        FileMetadata oldFile = confirmedFile("file-b", 7L);
        FileMetadata newFile = confirmedFile("file-a", 7L);
        given(lockFileUsageOwnerPort.lockOrCreateOwner(COORDINATE)).willReturn(owner);
        given(loadFileUsagePort.findFileIdsByOwnerId(10L)).willReturn(Set.of("file-b"));
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-a", "file-b")))
            .willReturn(List.of(newFile, oldFile));
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(loadFileUsagePort.countByFileId("file-b")).willReturn(0L);

        // when
        sut.replaceUsages(replace(Set.of("file-a"), 7L));

        // then
        InOrder order = inOrder(lockFileUsageOwnerPort, loadFileUsagePort, lockFileMetadataPort, saveFileUsagePort);
        order.verify(lockFileUsageOwnerPort).lockOrCreateOwner(COORDINATE);
        order.verify(loadFileUsagePort).findFileIdsByOwnerId(10L);
        order.verify(lockFileMetadataPort).lockAllByFileIds(List.of("file-a", "file-b"));
        order.verify(saveFileUsagePort).removeUsages(10L, Set.of("file-b"));
        order.verify(saveFileUsagePort).addUsages(10L, Set.of("file-a"));
        InOrder lifecycleOrder = inOrder(saveFileMetadataPort);
        lifecycleOrder.verify(saveFileMetadataPort).save(newFile);
        lifecycleOrder.verify(saveFileMetadataPort).save(oldFile);
        lifecycleOrder.verify(saveFileMetadataPort).flush();
        assertThat(newFile.getUnreferencedAt()).isNull();
        assertThat(oldFile.getUnreferencedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("공유 파일에서 한 owner만 detach하면 unreferencedAt을 기록하지 않는다")
    void 공유_파일에서_한_owner만_detach하면_unreferencedAt을_기록하지_않는다() {
        // given
        FileUsageOwner owner = owner(10L);
        FileMetadata sharedFile = confirmedFile("shared-file", 7L);
        given(lockFileUsageOwnerPort.lockOrCreateOwner(COORDINATE)).willReturn(owner);
        given(loadFileUsagePort.findFileIdsByOwnerId(10L)).willReturn(Set.of("shared-file"));
        given(lockFileMetadataPort.lockAllByFileIds(List.of("shared-file"))).willReturn(List.of(sharedFile));
        given(loadFileUsagePort.countByFileId("shared-file")).willReturn(1L);

        // when
        sut.replaceUsages(replace(Set.of(), null));

        // then
        assertThat(sharedFile.getUnreferencedAt()).isNull();
        then(saveFileUsagePort).should().removeUsages(10L, Set.of("shared-file"));
    }

    @Test
    @DisplayName("첫 usage attach는 기존 unreferencedAt을 제거한다")
    void 첫_usage_attach는_기존_unreferencedAt을_제거한다() {
        // given
        FileUsageOwner owner = owner(10L);
        FileMetadata metadata = confirmedFile("file-a", 7L);
        metadata.markUnreferenced(CONFIRMED_AT);
        given(lockFileUsageOwnerPort.lockOrCreateOwner(COORDINATE)).willReturn(owner);
        given(loadFileUsagePort.findFileIdsByOwnerId(10L)).willReturn(Set.of());
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-a"))).willReturn(List.of(metadata));
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);

        // when
        sut.replaceUsages(replace(Set.of("file-a"), 7L));

        // then
        assertThat(metadata.getUnreferencedAt()).isNull();
        then(saveFileUsagePort).should().addUsages(10L, Set.of("file-a"));
    }

    @Test
    @DisplayName("신규 attach 파일이 하나라도 없으면 usage를 부분 변경하지 않는다")
    void 신규_attach_파일이_하나라도_없으면_부분_변경하지_않는다() {
        // given
        FileUsageOwner owner = owner(10L);
        FileMetadata attached = confirmedFile("old-file", 7L);
        FileMetadata candidate = confirmedFile("file-a", 7L);
        given(lockFileUsageOwnerPort.lockOrCreateOwner(COORDINATE)).willReturn(owner);
        given(loadFileUsagePort.findFileIdsByOwnerId(10L)).willReturn(Set.of("old-file"));
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-a", "missing", "old-file")))
            .willReturn(List.of(candidate, attached));

        // when & then
        assertRejected(StorageErrorCode.FILE_NOT_FOUND, replace(Set.of("file-a", "missing"), 7L));
        assertThat(attached.getUnreferencedAt()).isNull();
        assertThat(candidate.getUnreferencedAt()).isNull();
    }

    @Test
    @DisplayName("bulk remove는 coordinate와 file을 각각 정렬 lock하고 anchor를 보존한다")
    void bulk_remove는_coordinate와_file을_정렬_lock하고_anchor를_보존한다() {
        // given
        FileUsageCoordinate first = FileUsageCoordinate.of("notice", "10", "images");
        FileUsageCoordinate second = FileUsageCoordinate.of("project", "20", "attachments");
        FileUsageOwner firstOwner = owner(10L);
        FileUsageOwner secondOwner = owner(20L);
        FileMetadata firstFile = confirmedFile("file-a", 7L);
        FileMetadata secondFile = confirmedFile("file-b", 7L);
        given(lockFileUsageOwnerPort.lockOrCreateOwner(first)).willReturn(firstOwner);
        given(lockFileUsageOwnerPort.lockOrCreateOwner(second)).willReturn(secondOwner);
        given(loadFileUsagePort.findFileIdsByOwnerId(10L)).willReturn(Set.of("file-b"));
        given(loadFileUsagePort.findFileIdsByOwnerId(20L)).willReturn(Set.of("file-a"));
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-a", "file-b")))
            .willReturn(List.of(firstFile, secondFile));
        given(loadFileUsagePort.countByFileId("file-a")).willReturn(0L);
        given(loadFileUsagePort.countByFileId("file-b")).willReturn(0L);

        // when
        sut.removeAll(new BulkRemoveFileUsagesCommand(List.of(second, first)));

        // then
        InOrder lockOrder = inOrder(lockFileUsageOwnerPort, lockFileMetadataPort);
        lockOrder.verify(lockFileUsageOwnerPort).lockOrCreateOwner(first);
        lockOrder.verify(lockFileUsageOwnerPort).lockOrCreateOwner(second);
        lockOrder.verify(lockFileMetadataPort).lockAllByFileIds(List.of("file-a", "file-b"));
        then(saveFileUsagePort).should().removeUsages(10L, Set.of("file-b"));
        then(saveFileUsagePort).should().removeUsages(20L, Set.of("file-a"));
        InOrder lifecycleOrder = inOrder(saveFileMetadataPort);
        lifecycleOrder.verify(saveFileMetadataPort).save(firstFile);
        lifecycleOrder.verify(saveFileMetadataPort).save(secondFile);
        lifecycleOrder.verify(saveFileMetadataPort).flush();
        assertThat(firstFile.getUnreferencedAt()).isEqualTo(NOW);
        assertThat(secondFile.getUnreferencedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("빈 desired snapshot도 owner serialization anchor를 생성하고 유지한다")
    void 빈_snapshot도_owner_anchor를_생성하고_유지한다() {
        // given
        FileUsageOwner owner = owner(10L);
        given(lockFileUsageOwnerPort.lockOrCreateOwner(COORDINATE)).willReturn(owner);
        given(loadFileUsagePort.findFileIdsByOwnerId(10L)).willReturn(Set.of());

        // when
        sut.replaceUsages(replace(Set.of(), null));

        // then
        then(lockFileUsageOwnerPort).should().lockOrCreateOwner(COORDINATE);
        then(lockFileMetadataPort).should(never()).lockAllByFileIds(List.of());
        verifyNoInteractions(saveFileUsagePort, saveFileMetadataPort);
    }

    private void assertRejected(StorageErrorCode expectedError, ReplaceFileUsagesCommand command) {
        assertThatThrownBy(() -> sut.replaceUsages(command))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(expectedError);
        verifyNoInteractions(saveFileUsagePort, saveFileMetadataPort);
    }

    private ReplaceFileUsagesCommand replace(Set<String> fileIds, Long requesterMemberId) {
        return new ReplaceFileUsagesCommand(COORDINATE, fileIds, requesterMemberId);
    }

    private FileUsageOwner owner(Long ownerId) {
        FileUsageOwner owner = org.mockito.Mockito.mock(FileUsageOwner.class);
        given(owner.getId()).willReturn(ownerId);
        return owner;
    }

    private FileMetadata confirmedFile(String fileId, Long uploadedMemberId) {
        FileMetadata metadata = pendingFile(fileId, uploadedMemberId);
        metadata.markAsUploaded(CONFIRMED_AT);
        metadata.markReferenced();
        return metadata;
    }

    private FileMetadata pendingFile(String fileId, Long uploadedMemberId) {
        return FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileId + ".pdf")
            .category(FileCategory.ETC)
            .contentType("application/pdf")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/" + fileId + ".pdf")
            .uploadedMemberId(uploadedMemberId)
            .build();
    }
}
