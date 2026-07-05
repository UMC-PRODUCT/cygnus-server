# ANALYTICS KNOWLEDGE

## OVERVIEW

`analytics` owns admin-facing aggregation read models across operations, school, risk challenger, and dashboard surfaces.

## STRUCTURE

```text
analytics/
├── domain/                         # analytics enums and lightweight domain values
├── application/port/in             # admin analytics query UseCases and DTOs
├── application/port/out            # aggregation ports used by query services
├── application/service/query       # read-only orchestration
├── application/service/evaluator   # admin visibility/permission helpers
├── adapter/in/web                  # admin analytics controllers
└── adapter/out/persistence         # QueryDSL aggregation repositories and adapters
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Admin dashboard | `adapter/in/web/AdminDashboardController.java` | dashboard summary surface |
| School analytics | `adapter/in/web/AdminSchoolAnalyticsController.java` | school-level admin metrics |
| Operations metrics | `adapter/out/persistence/AdminOperationsAnalyticsQueryRepository.java` | operations aggregation |
| Risk challenger metrics | `AdminRiskChallengerAnalyticsQueryRepository.java` | risk-oriented challenger aggregation |
| Query orchestration | `application/service/query/AdminAnalyticsQueryService.java` | combines port outputs |
| Persistence adapters | `adapter/out/persistence/*PersistenceAdapter.java` | translate query repositories to ports |

## CONVENTIONS

- Treat analytics as read-only unless a feature explicitly introduces stored snapshots.
- Query services should delegate heavy aggregation to `*QueryRepository` classes.
- Keep admin authorization checks near controller/evaluator boundaries before running broad queries.
- Aggregation DTOs should expose stable API fields and avoid leaking entity graphs.
- Prefer split queries and map joins over multi-fetch joins when aggregating across many domains.
- Time windows, status filters, and denominators must be explicit in method names or DTO fields.

## ANTI-PATTERNS

- Do not put write-side business rules in analytics services.
- Do not call domain repositories directly from controllers.
- Do not add dashboard metrics without tests for empty data and filtered data.
- Do not hide cross-domain joins inside application services when QueryDSL belongs in persistence.
