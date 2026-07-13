# ORGANIZATION KNOWLEDGE

## OVERVIEW

`organization` models UMC structure: school, global chapter, gisu, study group, and the UMC PRODUCT Chapter-Part, activity-period, Leadership, and Squad archive.

## STRUCTURE

```text
organization/
├── domain/                         # organization aggregates and enums
├── exception/                      # organization domain exception/error code
├── application/port/in             # command/query UseCases and DTOs
├── application/port/out/command    # save/delete style Ports
├── application/port/out/query      # load/list/search style Ports
├── application/service             # command/query services
├── adapter/in/web                  # admin/public controllers and DTOs
├── adapter/in/graphql              # GraphQL organization query surface and DTOs
└── adapter/out/persistence         # per-subdomain persistence adapters/repositories
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| School APIs | `adapter/in/web/School*Controller.java` | Public query and admin command surfaces |
| GraphQL organization queries | `adapter/in/graphql/OrganizationGraphQlController.java` | gisu/chapter/school GraphQL surface |
| Gisu APIs | `adapter/in/web/Gisu*Controller.java`, `adapter/in/web/v2` | v1/v2 query split exists |
| UMC PRODUCT org APIs | `adapter/in/web/UmcProduct*Controller.java` | Chapter, Part, member activity period, Leadership, Squad |
| Study group APIs | `adapter/in/web/StudyGroup*Controller.java` | study group and schedule boundaries |
| Query DTOs | `application/port/in/query/dto` | often split by subdomain |
| Persistence | `adapter/out/persistence/{chapter,gisu,school,studygroup,umcproduct}` | one adapter/query repository family per subdomain |
| QueryDSL examples | `adapter/out/persistence/studygroup/StudyGroupQueryRepository.java` | split fetch joins and batch maps |

## CONVENTIONS

- This domain has many subdomains; keep new code near the matching subpackage instead of creating broad catch-all services.
- Use IDs for links to member, challenger, curriculum, or project domains.
- Public query endpoints often use `@Public`; admin/command endpoints usually require `@CurrentMember`.
- GraphQL response DTOs live under `adapter/in/graphql/dto` and should map from application `Info` records.
- Command DTOs stay under `application/port/in/command/dto`; query `Info` records stay under `application/port/in/query/dto`.
- Persistence adapters should translate Spring Data repository names into semantic Port methods.
- Query repositories handle complex joins, search filters, and batch loading; services should not build QueryDSL predicates.
- When multiple bag-like associations are needed, prefer separate queries plus map grouping over multi-bag fetch joins.
- Domain exceptions should use `OrganizationDomainException` and `OrganizationErrorCode`.
- UMC PRODUCT Chapter/Part는 전역 `Chapter`/`Gisu`와 별도 모델이며 UMC PRODUCT 기수를 두지 않는다.
- UMC PRODUCT 활동 기간은 `LocalDate`/DB `DATE`로 저장하고 종료일을 포함한다. 감사 시각은 기존 `Instant`를 유지한다.
- 멤버의 소속은 `UmcProductPartMembership`으로만 저장하고 Chapter는 Part에서 파생한다.
- `UmcProductPartRole`은 `MEMBER`, `PART_LEAD`만 허용한다. Chapter Lead 역할은 없다.
- `UmcProductLeadership`은 Part 및 Operation Part 소속과 독립적으로 관리한다.

## ANTI-PATTERNS

- Do not let controllers call `adapter/out/persistence` repositories.
- Do not merge school/chapter/gisu/study-group rules into one god service.
- Do not add GraphQL fields without updating `src/main/resources/graphql/organization.graphqls`.
- Do not introduce direct `Member`, `Project`, or `Challenger` aggregate references.
- Do not add `@OneToMany` collections to organization entities.
- Do not hide not-found behavior behind `findBy`; use `getBy` semantics when existence is required.
