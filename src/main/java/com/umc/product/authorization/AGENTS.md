# AUTHORIZATION KNOWLEDGE

## OVERVIEW

`authorization` owns resource permission checks, challenger roles, and reusable access-control evaluators.

## STRUCTURE

```text
authorization/
├── domain/                         # ResourceType, PermissionType, permission models
├── application/port/in             # permission and role UseCases
├── application/port/out            # permission/role persistence ports
├── application/service/command     # challenger role writes
├── application/service/query       # permission and role reads
├── application/service/evaluator   # resource-specific evaluators
├── adapter/in/aspect               # permission enforcement aspects
├── adapter/in/web                  # role and permission controllers
└── adapter/out/persistence         # role persistence and query repositories
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Permission checks | `application/service/query/CheckResourcePermissionService.java` | main check path |
| Authorization facade | `application/service/AuthorizationService.java` | shared permission entry point |
| Resource permissions | `adapter/in/web/ResourcePermissionController.java` | admin/query surface |
| Challenger roles | `ChallengerRoleController.java`, `ChallengerRole*Service.java` | role assignment/read flows |
| Evaluators | `application/service/evaluator` | resource-specific access logic |
| Persistence | `adapter/out/persistence/ChallengerRoleAdapter.java` | role storage adapter |

## CONVENTIONS

- Add new protected resources through `ResourceType` and a matching evaluator path.
- Permission checks should return explicit allow/deny semantics; avoid null-as-deny ambiguity.
- Controllers in other domains should call authorization UseCases, not role repositories.
- Keep evaluator logic deterministic and side-effect free.
- Role write flows belong in command services with transaction boundaries.
- Permission query services should remain read-only.

## ANTI-PATTERNS

- Do not duplicate resource permission logic inside project/member/organization controllers.
- Do not bypass evaluators for one-off permission checks.
- Do not add broad admin shortcuts without documenting the resource and permission type.
- Do not make permission checks depend on mutable request DTO state.
