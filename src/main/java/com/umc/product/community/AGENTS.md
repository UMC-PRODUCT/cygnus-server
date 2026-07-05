# COMMUNITY KNOWLEDGE

## OVERVIEW

`community` owns posts, comments, reports, scraps, trophies, and user-facing community interactions.

## STRUCTURE

```text
community/
├── domain/                         # post, comment, report, scrap, trophy entities
├── application/port/in             # command/query UseCases and DTOs
├── application/port/out            # persistence ports
├── application/service/command     # write flows
├── application/service/query       # read flows
├── application/service/evaluator   # visibility/author evaluators
├── adapter/in/web                  # post/comment/report/trophy controllers
└── adapter/out/persistence         # adapters and QueryDSL repositories
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Posts | `PostController.java`, `PostQueryController.java` | command/read split |
| Comments | `CommentController.java`, `Comment*Service.java` | nested community replies/comments |
| Reports | `ReportController.java`, `ReportCommandService.java` | moderation signal flow |
| Scraps | `ScrapCommandService.java`, `ScrapPersistenceAdapter.java` | saved-post behavior |
| Trophies | `TrophyController.java`, `TrophyQueryController.java` | trophy command/query surface |
| QueryDSL | `PostQueryRepository.java`, `TrophyQueryRepository.java` | list/search projections |

## CONVENTIONS

- Keep post/comment/report/scrap/trophy write services separate.
- Author and visibility checks should happen before mutations or private reads.
- Report flows should preserve target identity and reporter identity without exposing internal moderation state.
- Query services should return application Info values and delegate joins to query repositories.
- Community interactions should be idempotency-aware when toggling scraps or reactions.
- Tests should cover ownership, visibility, and duplicate interaction cases.

## ANTI-PATTERNS

- Do not let controllers write directly through persistence adapters.
- Do not mix moderation/report state transitions into post query code.
- Do not expose deleted or hidden content through list projections.
- Do not rely on client-provided author/member IDs when `@CurrentMember` is available.
