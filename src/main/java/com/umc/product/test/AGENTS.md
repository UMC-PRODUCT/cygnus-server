# SEED/TEST UTILITY KNOWLEDGE

## OVERVIEW

`test` is production code for seed/test utility endpoints and scenario data setup; it is not the JUnit test tree.

## STRUCTURE

```text
test/
├── dto/                            # seed request/response DTOs
├── application/port/in             # seed UseCases
├── application/port/out            # cleanup/persistence ports
├── application/service             # member/challenger/project/curriculum/notice seed flows
├── adapter/in/web                  # seed/test controllers
└── adapter/out/persistence         # seed cleanup persistence adapter
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Seed API | `adapter/in/web/SeedController.java` | scenario seed surface |
| Test API | `adapter/in/web/TestController.java` | utility/test endpoint surface |
| Project seeds | `ProjectSeedService.java`, `ProjectScenarioSeedService.java` | project/application scenario data |
| Member/challenger seeds | `MemberSeedService.java`, `ChallengerSeedService.java` | member/challenger setup |
| Curriculum seeds | `CurriculumSeedService.java` | curriculum/workbook setup |
| Notice seeds | `NoticeSeedService.java` | notice setup |
| Cleanup | `ProjectSeedDataCleanupService.java`, `ProjectSeedDataCleanupPersistenceAdapter.java` | seed data cleanup |

## CONVENTIONS

- Treat this package as privileged utility code; check profile/security expectations before exposing endpoints.
- Seed services should create data through application/domain paths where possible, not by bypassing invariants.
- Scenario data must be deterministic enough for repeatable QA and local setup.
- Cleanup should target only seed-owned data and avoid deleting user-created production-like data.
- Cross-domain seed flows should document ordering assumptions in service names or tests.

## ANTI-PATTERNS

- Do not put JUnit test helpers under this production package.
- Do not expose seed endpoints broadly without checking `SecurityPathConfig` and profile conditions.
- Do not create invalid domain objects to make scenario setup faster.
- Do not run cleanup by broad table truncation from this package.
