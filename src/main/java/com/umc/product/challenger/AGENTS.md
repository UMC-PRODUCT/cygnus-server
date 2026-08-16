# CHALLENGER KNOWLEDGE

## OVERVIEW

`challenger` owns challenger lifecycle, activity periods, records, points, search, and role-sensitive access.

## STRUCTURE

```text
challenger/
├── domain/                         # challenger, record, point entities and enums
├── application/port/in             # challenger command/query UseCases
├── application/port/out            # challenger persistence ports
├── application/service             # lifecycle, point, record, search services
├── application/service/evaluator   # record/permission evaluators
├── adapter/in/web                  # v1/v2 command, query, record, search controllers
└── adapter/out/persistence         # persistence adapters and QueryDSL repositories
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Challenger commands | `adapter/in/web/ChallengerCommandController.java` | create/update style flows |
| Challenger reads | `ChallengerQueryController.java`, `ChallengerSearchController.java` | detail/list/search |
| V2 search | `adapter/in/web/v2/ChallengerSearchV2Controller.java` | newer search surface |
| Records | `ChallengerRecordController.java`, `ChallengerRecord*Service.java` | activity record flows |
| Points | `ChallengerPointCommandController.java`, `ChallengerPointQueryService.java` | point mutation/read |
| Activity period | `ChallengerActivityPeriodService.java` | period-sensitive rules |
| Persistence | `ChallengerQueryRepository.java`, `ChallengerPointQueryRepository.java` | QueryDSL reads |

## CONVENTIONS

- Keep lifecycle state changes in domain methods or command services, not controllers.
- Use member IDs and organization IDs for cross-domain links.
- Period-sensitive logic should go through `ChallengerActivityPeriodService`.
- Search changes require attention to v1/v2 controller differences.
- Record permission logic belongs in evaluator classes before exposing record data.
- Point changes should keep command and query behavior separately testable.

## ANTI-PATTERNS

- Do not mutate challenger status with direct field access.
- Do not bypass activity-period checks for record or point flows.
- Do not make search repositories return entities for API responses.
- Do not merge point, record, and challenger lifecycle responsibilities into one service.
