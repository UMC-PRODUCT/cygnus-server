# SCHEDULE KNOWLEDGE

## OVERVIEW

`schedule` owns schedules, participants, participant capabilities, and v2 schedule command/query APIs.

## STRUCTURE

```text
schedule/
├── domain/                         # schedule, participant, enum, exception model
├── application/port/in             # schedule command/query UseCases
├── application/port/out            # schedule persistence ports
├── application/service/command     # schedule and participant writes
├── application/service/query       # schedule reads and capability checks
├── application/service/evaluator   # access/participant evaluators
├── adapter/in/web/v2               # v2 schedule controllers
└── adapter/out/persistence         # adapters and QueryDSL repositories
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Schedule commands | `adapter/in/web/v2/ScheduleCommandV2Controller.java` | create/update/delete flows |
| Schedule reads | `ScheduleQueryV2Controller.java`, `ScheduleQueryService.java` | v2 query surface |
| Participants | `ScheduleParticipantCommandService.java` | participant mutation |
| Capabilities | `ScheduleCapabilitiesService.java` | what current member can do |
| QueryDSL | `ScheduleQueryRepository.java`, `ScheduleParticipantQueryRepository.java` | read projections |
| Persistence | `SchedulePersistenceAdapter.java`, `ScheduleParticipantPersistenceAdapter.java` | storage boundary |

## CONVENTIONS

- New endpoints should follow the v2 package unless intentionally versioned.
- Participant changes must validate schedule access and role/capability rules.
- Capability responses should be derived server-side, not accepted from clients.
- Query repositories own participant joins and pagination.
- Use IDs for member/organization links instead of aggregate references.
- Time-related rules need tests around boundary dates and status transitions.

## ANTI-PATTERNS

- Do not mutate participants from query services.
- Do not trust client-provided capability flags.
- Do not expose schedules to non-participants without an explicit public rule.
- Do not duplicate schedule access checks in several controllers.
