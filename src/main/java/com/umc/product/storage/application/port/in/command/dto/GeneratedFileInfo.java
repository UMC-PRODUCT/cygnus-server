package com.umc.product.storage.application.port.in.command.dto;

public record GeneratedFileInfo(
    String fileId,
    String storageKey,
    long fileSize
) {

    public static GeneratedFileInfo of(String fileId, String storageKey, long fileSize) {
        return new GeneratedFileInfo(fileId, storageKey, fileSize);
    }
}
