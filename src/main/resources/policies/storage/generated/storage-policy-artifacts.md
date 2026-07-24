# Storage Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `storage-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `1ae07680f8213f610411ad5bda8dabfc9a67de0565aadc994c6d929f685efff9`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:DELETE /api/v1/storage/{fileId} | com.umc.product.storage.adapter.in.web.StorageController#deleteFile | REST | storage-file:delete | storage-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| storage-resource | storage-file.delete.super-admin | ALLOW | storage-file:delete | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
| storage-resource | storage-file.delete.uploader | ALLOW | storage-file:delete | EQ(ATTRIBUTE(relation.isUploader), true) |  |
