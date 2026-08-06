# STORAGE KNOWLEDGE

## OVERVIEW

`storage` owns file upload/download metadata, S3-backed object storage, and file lifecycle commands.

## STRUCTURE

```text
storage/
├── domain/                         # file metadata, storage enums, exceptions
├── application/port/in             # file command/query UseCases
├── application/port/out            # metadata and object-storage ports
├── application/service             # file command/query orchestration
├── adapter/in/web                  # storage controller
├── adapter/out/persistence         # file metadata persistence
└── adapter/out/s3                  # S3 adapter and provider-specific behavior
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Storage API | `adapter/in/web/StorageController.java` | upload/download/delete surface |
| File commands | `application/service/FileCommandService.java` | write lifecycle |
| File queries | `application/service/FileQueryService.java` | metadata/read behavior |
| Metadata | `FileMetadataPersistenceAdapter.java` | DB-backed metadata |
| S3 integration | `adapter/out/s3/S3StorageAdapter.java` | object storage operations |
| Domain rules | `domain/enums`, `domain/exception` | file type/status/error behavior |

## CONVENTIONS

- Keep object storage operations behind application ports.
- Metadata writes and S3 writes must be ordered to avoid orphaned records or orphaned objects.
- Do not log presigned URLs, credentials, or raw upload tokens.
- File type and size validation should happen before external storage writes.
- Controllers should return response DTOs mapped from application Info values.

## ANTI-PATTERNS

- Do not call `S3StorageAdapter` from another domain directly.
- Do not rely on client-provided file metadata without server validation.
- Do not delete metadata and objects in separate untested flows.
- Do not expose provider-specific bucket/key details unless the API intentionally requires them.
