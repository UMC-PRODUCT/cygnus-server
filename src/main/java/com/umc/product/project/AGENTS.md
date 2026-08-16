# PROJECT KNOWLEDGE

## OVERVIEW

`project` is a high-complexity domain for project creation, application forms, applications, permissions, matching rounds, statistics, and scheduler-driven deadlines.

## STRUCTURE

```text
project/
├── domain/                         # project, application, form, member, matching entities
├── application/access              # access scope resolver/policy helpers
├── application/service/command     # write flows and matching finalization
├── application/service/query       # read models, statistics, permissions
├── application/service/policy      # matching/deadline policies
├── application/service/evaluator   # permission evaluators
├── adapter/in/web                  # command/query/form/application controllers
├── adapter/in/graphql              # GraphQL project query surface and DTOs
├── adapter/in/scheduler            # deadline and matching schedulers
└── adapter/out/persistence         # JPA and QueryDSL persistence
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Project CRUD | `ProjectCommandController.java`, `ProjectQueryController.java` | Main project surfaces |
| GraphQL project queries | `adapter/in/graphql/ProjectGraphQlController.java` | pilot GraphQL query surface |
| Application flow | `ProjectApplicationController.java`, `ProjectApplicationQueryController.java` | draft/submit/update/cancel |
| Form flow | `ProjectApplicationFormController.java` | form policy and question structure |
| Matching rounds | `ProjectMatchingRoundController.java`, `adapter/in/scheduler` | scheduler-sensitive logic |
| Permissions | `application/access`, `application/service/evaluator`, `ProjectPermissionController.java` | access-scope and evaluator rules |
| Statistics | `ProjectStatisticsQueryController.java`, `ProjectStatisticsQueryService.java` | aggregation-heavy read model |
| Persistence | `adapter/out/persistence/*QueryRepository.java` | complex QueryDSL and projection code |

## CONVENTIONS

- Treat this as multiple bounded flows. Do not add unrelated project, application, form, matching, and statistics behavior to the same service.
- Command services are already large; prefer extracting policy/helper classes when adding branching rules.
- Permission logic belongs in `application/access` or `application/service/evaluator`, not in controllers.
- Statistics and search should stay read-only and use Query services plus QueryDSL repositories.
- GraphQL controllers should delegate to query UseCases and map through `*GraphQlResponse` DTOs.
- Project-member and applicant relationships should use member IDs, not `Member` aggregates.
- Scheduler code must delegate into application UseCases; schedulers should not own business rules.
- Response assemblers under `adapter/in/web/assembler` map application `Info` objects to API responses.
- Large tests are concentrated in `src/test/java/com/umc/product/project`; add focused cases near the changed flow.

## HOTSPOTS

- `ProjectStatisticsQueryService.java` is aggregation-heavy.
- `ProjectApplicationFormCommandService.java` and `ProjectApplicationCommandService.java` are command-flow hotspots.
- `ProjectPermissionQueryService.java` and evaluator tests carry access-control risk.
- Matching finalization and deadline scheduling require time-sensitive test coverage.

## ANTI-PATTERNS

- Do not add another responsibility to existing large services without checking extraction first.
- Do not compute permissions in controllers.
- Do not add GraphQL fields without updating `src/main/resources/graphql/project.graphqls`.
- Do not query member/organization internals directly; use public UseCases or IDs.
- Do not make application-form changes without checking form policy and existing large test suites.
- Do not bypass domain methods for status transitions.
