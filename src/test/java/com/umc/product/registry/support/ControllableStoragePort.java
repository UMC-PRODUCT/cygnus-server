package com.umc.product.registry.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.mockito.Mockito;

import com.umc.product.storage.application.port.in.command.dto.FileUploadInfo;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.storage.application.port.out.dto.StorageObjectInfo;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

public final class ControllableStoragePort implements StoragePort {

    private static final CleanupClaimObservation UNCLAIMED =
        new CleanupClaimObservation(null, null, 0);

    private final Map<String, StorageObjectInfo> objects = new LinkedHashMap<>();
    private final List<String> calls = new ArrayList<>();
    private final List<CleanupClaimObservation> claimObservations = new ArrayList<>();
    private int deleteFailuresRemaining;
    private Supplier<CleanupClaimObservation> activeClaimProbe = () -> UNCLAIMED;

    public void bindTo(StoragePort storagePortBean) {
        Mockito.reset(storagePortBean);
        given(storagePortBean.generateStorageKey(any(FileCategory.class), anyString(), anyString()))
            .willAnswer(invocation -> generateStorageKey(
                invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        given(storagePortBean.generateUploadUrl(anyString(), anyString(), anyLong(), anyLong()))
            .willAnswer(invocation -> generateUploadUrl(
                invocation.getArgument(0), invocation.getArgument(1),
                invocation.getArgument(2), invocation.getArgument(3)));
        given(storagePortBean.findObjectInfoByStorageKey(anyString()))
            .willAnswer(invocation -> findObjectInfoByStorageKey(invocation.getArgument(0)));
        willAnswer(invocation -> {
            delete(invocation.getArgument(0));
            return null;
        }).given(storagePortBean).delete(anyString());
    }

    public void reset() {
        objects.clear();
        calls.clear();
        claimObservations.clear();
        deleteFailuresRemaining = 0;
        activeClaimProbe = () -> UNCLAIMED;
    }

    public void completeUpload(String storageKey, long contentLength, String contentType) {
        objects.put(storageKey, StorageObjectInfo.of(storageKey, contentLength, contentType));
    }

    public void failNextDelete() {
        deleteFailuresRemaining++;
    }

    public void observeActiveClaim(Supplier<CleanupClaimObservation> probe) {
        activeClaimProbe = probe;
    }

    public List<String> calls() {
        return List.copyOf(calls);
    }

    public List<CleanupClaimObservation> claimObservations() {
        return List.copyOf(claimObservations);
    }

    public boolean containsObject(String storageKey) {
        return objects.containsKey(storageKey);
    }

    @Override
    public FileUploadInfo generateUploadUrl(
        String storageKey,
        String contentType,
        long durationMinutes
    ) {
        return generateUploadUrl(storageKey, contentType, 0L, durationMinutes);
    }

    @Override
    public FileUploadInfo generateUploadUrl(
        String storageKey,
        String contentType,
        long fileSize,
        long durationMinutes
    ) {
        calls.add("upload-url:" + storageKey);
        return new FileUploadInfo(
            null,
            "https://storage.test/" + storageKey,
            "PUT",
            Map.of("Content-Type", contentType),
            LocalDateTime.of(2030, 1, 1, 0, 0)
        );
    }

    @Override
    public String generateAccessUrl(String storageKey, long durationMinutes) {
        calls.add("access-url:" + storageKey);
        return "https://storage.test/" + storageKey;
    }

    @Override
    public void uploadObject(String storageKey, String contentType, byte[] content) {
        calls.add("put:" + storageKey);
        completeUpload(storageKey, content.length, contentType);
    }

    @Override
    public boolean exists(String storageKey) {
        return objects.containsKey(storageKey);
    }

    @Override
    public Optional<StorageObjectInfo> findObjectInfoByStorageKey(String storageKey) {
        calls.add("head:" + storageKey);
        return Optional.ofNullable(objects.get(storageKey));
    }

    @Override
    public void delete(String storageKey) {
        CleanupClaimObservation observation = activeClaimProbe.get();
        claimObservations.add(observation);
        calls.add("delete:" + storageKey + ":active-claim=" + (observation.token() != null));
        if (deleteFailuresRemaining > 0) {
            deleteFailuresRemaining--;
            throw new StorageException(StorageErrorCode.STORAGE_DELETE_FAILED);
        }
        objects.remove(storageKey);
    }

    public record CleanupClaimObservation(UUID token, Instant claimedAt, int attempt) {
    }
}
