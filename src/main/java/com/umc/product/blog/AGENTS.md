# BLOG KNOWLEDGE

## OVERVIEW

`blog` owns blog contents, comments, likes, hashtags, series, SEO data, and interaction counters.

## STRUCTURE

```text
blog/
├── domain/                         # content, comment, reaction, hashtag, series entities
├── application/port/in             # command/query UseCases and DTOs
├── application/port/out            # persistence and interaction ports
├── application/service             # content/comment/series command and query flows
├── application/service/evaluator   # author/visibility evaluators
├── adapter/in/web                  # content, hashtag, interaction, series controllers
└── adapter/out/persistence         # JPA adapters and QueryDSL repositories
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Content API | `adapter/in/web/BlogContentController.java` | content CRUD/query surface |
| Comments/reactions | `BlogInteractionController.java`, `BlogContentReactionService.java` | likes/comments/interactions |
| Hashtags | `BlogHashtagController.java`, `BlogHashtagQueryService.java` | tag discovery |
| Series | `BlogSeriesController.java`, `BlogSeries*Service.java` | ordered content grouping |
| SEO/search | `BlogSeoQueryRepository.java`, `BlogContentQueryRepository.java` | read projections |
| Persistence | `adapter/out/persistence/Blog*QueryRepository.java` | QueryDSL-heavy reads |

## CONVENTIONS

- Keep content writes, comment writes, and reaction writes in separate services.
- Visibility and author checks belong in evaluators or application services before persistence writes.
- Query repositories should own search, SEO, and list projection joins.
- Controller responses should map from application Info values, not entities.
- Counter or reaction updates must be transactionally consistent with the interaction event.
- Hashtag/series changes need focused tests for ordering and duplicate handling.

## ANTI-PATTERNS

- Do not compute author permissions in controllers.
- Do not expose draft/private content through public query paths.
- Do not mix comment, content, and series write rules in one service.
- Do not add search filters without checking QueryDSL pagination and count-query behavior.
