# CURRICULUM KNOWLEDGE

## OVERVIEW

`curriculum` owns curricula, weekly curriculum, workbooks, missions, submissions, feedback, and weekly best workbook flows.

## STRUCTURE

```text
curriculum/
├── domain/                         # curriculum, workbook, mission, submission entities
├── application/port/in             # v2 command/query UseCases and DTOs
├── application/port/out            # persistence ports
├── application/service/command     # curriculum/workbook/mission write flows
├── application/service/query       # curriculum/workbook read flows
├── adapter/in/web/v2               # v2 controllers only
├── adapter/in/scheduler            # scheduled curriculum jobs
└── adapter/out/persistence         # adapters and QueryDSL repositories
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Curriculum API | `adapter/in/web/v2/Curriculum*V2Controller.java` | v2 command/query surface |
| Workbooks | `OriginalWorkbook*V2Controller.java`, `ChallengerWorkbook*V2Controller.java` | original and challenger workbook flows |
| Missions | `*WorkbookMissionCommandV2Controller.java`, `MissionSubmissionCommandService.java` | mission/submission writes |
| Weekly curriculum | `WeeklyCurriculum*Service.java`, `WeeklyCurriculumQueryRepository.java` | weekly read/write model |
| Weekly best | `WeeklyBestWorkbook*Service.java` | best workbook selection |
| Persistence | `adapter/out/persistence/*QueryRepository.java` | projection and submission queries |

## CONVENTIONS

- New HTTP surfaces should follow the existing v2 controller package unless intentionally versioned.
- Keep original workbook, challenger workbook, mission, and submission rules separated.
- Submission and feedback writes need transaction boundaries and requester validation.
- Query repositories should own workbook/submission joins and pagination.
- Scheduler code should delegate to application UseCases.
- Cross-domain links should use challenger/member/gisu IDs, not aggregate references.

## ANTI-PATTERNS

- Do not add v1-style controllers for new curriculum APIs.
- Do not bypass workbook/mission ownership checks for submissions.
- Do not return curriculum entities directly from controllers.
- Do not combine weekly curriculum scheduling rules with request DTO mapping.
