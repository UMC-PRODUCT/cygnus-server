package com.umc.product.storage.adapter.out.backfill;

import java.util.List;

record StorageFileUsageSource(
    String sourceName,
    String tableName,
    String resourceKeyColumn,
    String fileColumn,
    String usageNamespace,
    String slot,
    ReferenceShape shape
) {

    static final List<StorageFileUsageSource> ALL = List.of(
        scalar("member-profile-image", "member", "id", "profile_image_id",
            "member", "profile-image"),
        scalar("school-logo", "school", "id", "logo_image_id",
            "organization.school", "logo"),
        scalar("notice-images", "notice_image", "notice_id", "image_id",
            "notice", "images"),
        scalar("project-logo", "project", "id", "logo_file_id",
            "project", "logo"),
        scalar("project-thumbnail", "project", "id", "thumbnail_file_id",
            "project", "thumbnail"),
        array("form-answer-attachments", "answer", "id", "file_ids",
            "form.answer", "attachments"),
        array("chat-message-attachments", "chat_message", "id", "file_metadata_ids",
            "chat.message", "attachments"),
        scalar("umc-product-member-profile-image", "umc_product_member", "id",
            "profile_image_id", "organization.umc-product-member", "profile-image"),
        scalar("certificate-file", "certificate", "id", "file_id",
            "certificate", "file")
    );

    String parentIdsSql() {
        return """
            SELECT id
            FROM %s
            WHERE id > ?
            ORDER BY id
            LIMIT ?
            """.formatted(tableName);
    }

    String batchReferencesSql() {
        if (shape == ReferenceShape.ARRAY) {
            return """
                SELECT DISTINCT source.id AS parent_id,
                       CAST(source.%s AS text) AS resource_key,
                       reference.file_id
                FROM %s source
                CROSS JOIN LATERAL unnest(source.%s) AS reference(file_id)
                WHERE source.id > ? AND source.id <= ?
                  AND reference.file_id IS NOT NULL
                  AND btrim(reference.file_id) <> ''
                ORDER BY parent_id, resource_key, reference.file_id
                """.formatted(resourceKeyColumn, tableName, fileColumn);
        }
        return """
            SELECT source.id AS parent_id,
                   CAST(source.%s AS text) AS resource_key,
                   source.%s AS file_id
            FROM %s source
            WHERE source.id > ? AND source.id <= ?
              AND source.%s IS NOT NULL
              AND btrim(source.%s) <> ''
            ORDER BY parent_id, resource_key, file_id
            """.formatted(resourceKeyColumn, fileColumn, tableName, fileColumn, fileColumn);
    }

    String allReferencesSql() {
        if (shape == ReferenceShape.ARRAY) {
            return """
                SELECT DISTINCT source.id AS parent_id,
                       CAST(source.%s AS text) AS resource_key,
                       reference.file_id
                FROM %s source
                CROSS JOIN LATERAL unnest(source.%s) AS reference(file_id)
                ORDER BY parent_id, resource_key, reference.file_id NULLS FIRST
                """.formatted(resourceKeyColumn, tableName, fileColumn);
        }
        return """
            SELECT source.id AS parent_id,
                   CAST(source.%s AS text) AS resource_key,
                   source.%s AS file_id
            FROM %s source
            WHERE source.%s IS NOT NULL
            ORDER BY parent_id, resource_key, file_id
            """.formatted(resourceKeyColumn, fileColumn, tableName, fileColumn);
    }

    private static StorageFileUsageSource scalar(
        String sourceName,
        String tableName,
        String resourceKeyColumn,
        String fileColumn,
        String usageNamespace,
        String slot
    ) {
        return new StorageFileUsageSource(
            sourceName, tableName, resourceKeyColumn, fileColumn,
            usageNamespace, slot, ReferenceShape.SCALAR);
    }

    private static StorageFileUsageSource array(
        String sourceName,
        String tableName,
        String resourceKeyColumn,
        String fileColumn,
        String usageNamespace,
        String slot
    ) {
        return new StorageFileUsageSource(
            sourceName, tableName, resourceKeyColumn, fileColumn,
            usageNamespace, slot, ReferenceShape.ARRAY);
    }

    enum ReferenceShape {
        SCALAR,
        ARRAY
    }
}
