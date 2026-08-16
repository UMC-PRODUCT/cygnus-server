# NOTICE KNOWLEDGE

## OVERVIEW

`notice` owns notices, notice content, targeting, read state, and vote response flows.

## STRUCTURE

```text
notice/
├── domain/                         # notice, content, target, vote entities and enums
├── application/port/in             # command/query UseCases and DTOs
├── application/port/out            # notice persistence ports
├── application/service/command     # notice/content/read/vote writes
├── application/service/query       # notice/content/target reads
├── adapter/in/web                  # command, content, query, vote controllers
└── adapter/out/persistence         # adapters and QueryDSL repositories
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Notice commands | `adapter/in/web/NoticeCommandController.java` | create/update/delete style flows |
| Notice reads | `NoticeQueryController.java`, `NoticeQueryService.java` | list/detail/read model |
| Notice content | `NoticeContentController.java`, `NoticeContentService.java` | content block behavior |
| Vote responses | `NoticeVoteResponseController.java`, `NoticeVoteResponseCommandService.java` | vote submission |
| Targets | `NoticeTargetQueryService.java`, `NoticeTargetPersistenceAdapter.java` | delivery/visibility targets |
| QueryDSL | `NoticeQueryRepository.java`, `NoticeContentsQueryRepository.java` | read projections |

## CONVENTIONS

- Keep notice metadata, content, target, read, and vote behavior separated.
- Target visibility should be enforced before returning notice detail or content.
- Vote response flows need duplicate and eligibility tests.
- Query repositories should handle list filters and content projection joins.
- Read-state updates should be explicit command behavior, not hidden inside query mapping.
- Domain enums should drive notice status/type behavior rather than raw strings.

## ANTI-PATTERNS

- Do not expose notices to non-target members through generic list queries.
- Do not mutate read or vote state from query services.
- Do not store content ordering rules only in request DTOs.
- Do not bypass domain methods for status transitions.
