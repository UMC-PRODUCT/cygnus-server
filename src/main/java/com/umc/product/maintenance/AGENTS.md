# MAINTENANCE KNOWLEDGE

## OVERVIEW

`maintenance` owns system maintenance windows, bypass policy, status checks, and admin maintenance controls.

## STRUCTURE

```text
maintenance/
├── domain/                         # maintenance window and state model
├── exception/                      # maintenance-specific errors
├── application/port/in             # command/query UseCases
├── application/port/out            # persistence and bypass ports
├── application/service             # maintenance command/query orchestration
├── adapter/in/web                  # admin and status controllers
├── adapter/in/scheduler            # maintenance activation/cleanup jobs
├── adapter/out/bypass              # bypass decision adapter
└── adapter/out/persistence         # maintenance window persistence
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Admin controls | `adapter/in/web/AdminMaintenanceController.java` | maintenance window mutation |
| Public status | `adapter/in/web/SystemStatusController.java` | client-facing status |
| Command logic | `application/service/MaintenanceCommandService.java` | create/update/end flows |
| Query logic | `application/service/MaintenanceQueryService.java` | current status/window reads |
| Bypass policy | `adapter/out/bypass` | maintenance bypass behavior |
| Persistence | `MaintenanceWindowPersistenceAdapter.java` | window storage |

## CONVENTIONS

- Maintenance changes are operationally sensitive; keep command paths explicit and audited by tests.
- Public status responses should avoid exposing internal scheduling details.
- Bypass logic belongs in bypass adapters or policy ports, not scattered across controllers.
- Scheduler code should only activate or reconcile windows through UseCases.
- Security path changes must be checked against global security and maintenance filters.

## ANTI-PATTERNS

- Do not make every endpoint responsible for checking maintenance state by hand.
- Do not expose admin-only maintenance fields through `SystemStatusController`.
- Do not bypass persistence state with in-memory flags unless the adapter explicitly owns that behavior.
