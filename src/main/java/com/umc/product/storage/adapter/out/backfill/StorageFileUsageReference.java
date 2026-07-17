package com.umc.product.storage.adapter.out.backfill;

record StorageFileUsageReference(
    long parentId,
    String resourceKey,
    String fileId
) {

    boolean isValid() {
        return fileId != null && !fileId.isBlank();
    }

    String coordinate(StorageFileUsageSource source) {
        return source.usageNamespace() + "/" + resourceKey + "/" + source.slot() + "=" + fileId;
    }
}
