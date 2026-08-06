# FIGMA KNOWLEDGE

## OVERVIEW

`figma` integrates Figma OAuth, watched files, comment sync, routing-domain classification, summaries, Discord forwarding, caching, and schedulers.

## STRUCTURE

```text
figma/
├── domain/                         # watched files, comments, sync cursors, routing domains
├── config/                         # domain-specific Figma sync properties
├── application/port/in             # usecases and request/query DTOs
├── application/port/out            # Figma, Discord, cache, persistence ports
├── application/service             # sync, OAuth, summary, routing logic
├── adapter/in/web                  # OAuth/sync/watched-file/routing controllers
├── adapter/in/scheduler            # comment dispatch/sync scheduling
└── adapter/out                     # external Figma/Discord, cache, persistence
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| OAuth API | `adapter/in/web/FigmaOAuthController.java` | OAuth entry surface |
| Sync API | `adapter/in/web/FigmaSyncController.java` | manual/triggered sync |
| Watched files | `adapter/in/web/FigmaWatchedFileController.java` | file registration/query |
| Routing domains | `adapter/in/web/FigmaRoutingDomainController.java` | classification management |
| Figma external calls | `adapter/out/external/*Figma*` | API clients/properties |
| Discord forwarding | `adapter/out/external/DiscordMentionWebhookAdapter.java` | large integration hotspot |
| Cache | `adapter/out/cache` | Figma-related cache adapter |
| Schedulers | `adapter/in/scheduler` | periodic sync/dispatch |

## CONVENTIONS

- External API details belong in `adapter/out/external`; application services depend on Ports.
- OAuth state and token validation rules belong in application services, not controllers.
- Scheduler classes should trigger UseCases and keep timing/enablement concerns separate from business logic.
- Domain-specific properties belong in `figma/config` or external adapter properties, not `global/config`.
- Comment summary/routing commands should use static factories from request/query DTOs where available.
- External failures should map to `FigmaDomainException` or a clear adapter-level fallback policy.
- Discord payload construction is integration-heavy; keep formatting changes covered by focused tests.

## ANTI-PATTERNS

- Do not call Figma/Discord clients directly from controllers.
- Do not put Figma-specific configuration under `global` unless it is truly cross-cutting.
- Do not mix cache invalidation, external fetch, and domain state transition in a controller.
- Do not bypass watched-file/routing-domain validation when adding sync paths.
- Do not log tokens, OAuth codes, or raw secrets.
