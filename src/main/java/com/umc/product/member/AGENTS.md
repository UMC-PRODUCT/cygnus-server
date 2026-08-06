# MEMBER KNOWLEDGE

## OVERVIEW

`member` owns member identity, profile, credentials, email registration, member search, and member GraphQL reads.

## STRUCTURE

```text
member/
├── domain/                         # member and profile entities plus exceptions
├── application/port/in             # member command/query UseCases and DTOs
├── application/port/out            # member/profile persistence ports
├── application/service             # credential, email, profile, query, search services
├── adapter/in/web                  # v1/v2 REST controllers and assemblers
├── adapter/in/graphql              # member GraphQL controller and DTOs
└── adapter/out/persistence         # JPA adapters and QueryDSL repository
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Member commands | `adapter/in/web/MemberCommandController.java` | profile/account command surface |
| Member reads | `MemberQueryController.java`, `web/v2/MemberQueryV2Controller.java` | v1/v2 read split |
| GraphQL reads | `adapter/in/graphql/MemberGraphQlController.java` | `me`, `member`, `members`, nested fields |
| Credentials | `MemberCredential*Service.java` | password/credential behavior |
| Email registration | `EmailMemberRegisterService.java`, `MemberEmailCommandService.java` | email registration flow |
| Search | `MemberSearchService.java`, `MemberQueryRepository.java` | member search/projection |
| Profile persistence | `MemberProfilePersistenceAdapter.java` | profile storage boundary |

## CONVENTIONS

- Treat member identity and profile updates as separate concerns.
- GraphQL member fields must enforce visibility before resolving nested school/challenger data.
- Cross-domain consumers should use `GetMemberUseCase` or member IDs, not persistence adapters.
- Credential and email flows must not log passwords, verification codes, or tokens.
- V2 query behavior should stay compatible with existing v1 response expectations unless versioned deliberately.
- Search and summary projections belong in query services/repositories.

## ANTI-PATTERNS

- Do not expose private member fields through public or GraphQL responses.
- Do not bypass permission checks in `members(ids)` or nested GraphQL batch mappings.
- Do not couple authentication services to member JPA repositories directly.
- Do not mix credential mutation rules into profile command handlers.
