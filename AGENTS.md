# PROJECT KNOWLEDGE BASE

**Generated:** 2026-07-02 06:00:42 KST
**Base Commit:** f7f2ccbb
**Branch:** feature/init-deep-agent-guidelines

## OVERVIEW

UMC Product Backend is a Java 21 / Spring Boot 3.5 API server built with strict Hexagonal Architecture,
DDD-style domain rules, JPA, QueryDSL, Flyway, JWT/SSO, REST/GraphQL APIs, OpenAPI, REST Docs,
Prometheus, and OpenTelemetry.

All agent responses, generated reviews, and documentation comments must be in Korean unless identifiers or standard technical terms require English.

## STRUCTURE

```text
umc-product-server/
├── src/main/java/com/umc/product/
│   ├── {domain}/domain              # Entity, VO, domain enum, domain exception
│   ├── {domain}/application/port    # UseCase and outbound Port contracts
│   ├── {domain}/application/service # Command/Query service implementations
│   ├── {domain}/adapter/in          # REST/GraphQL controllers, schedulers, aspects
│   ├── {domain}/adapter/out         # Persistence and external adapters
│   ├── global/                      # response, exception, security, config, observability
│   └── common/                      # BaseEntity and shared domain enums
├── src/main/resources/db/migration/ # Flyway SQL migrations
├── src/main/resources/graphql/      # Spring GraphQL schema files
├── src/test/java/com/umc/product/   # domain tests plus shared support package
├── docs/adr                         # architecture decisions
├── docs/onboarding                  # domain/test maps
└── build.gradle.kts                 # Gradle, REST Docs, QueryDSL, quality gates
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Boot entry | `src/main/java/com/umc/product/UmcProductApplication.java` | `@SpringBootApplication`, configuration properties scan |
| Security flow | `global/config/SecurityConfig.java`, `global/security/*` | JWT, `@Public`, access denied/auth entry points |
| SSO/PKCE auth | `authentication/adapter/in/web/Sso*`, `authentication/application/service/Sso*` | browser login, authorization code, token exchange |
| Web pipeline | `global/config/WebMvcConfig.java` | current member resolver, logging, rate limit, docs redirect |
| GraphQL API | `{domain}/adapter/in/graphql`, `src/main/resources/graphql` | Spring GraphQL controllers and schema contracts |
| Response envelope | `global/response/*`, `global/exception/*` | success wrapping and error response paths |
| Domain API | `{domain}/adapter/in/web/*Controller.java` | controllers delegate to UseCases only |
| Application logic | `{domain}/application/service` | split command/query services |
| Persistence | `{domain}/adapter/out/persistence` | JPA repositories plus QueryDSL query repositories |
| Public contracts | `{domain}/application/port/in`, `{domain}/application/port/out` | UseCase and Port interfaces |
| High-complexity project flows | `src/main/java/com/umc/product/project` | application forms, matching, statistics, permissions |
| Organization model | `src/main/java/com/umc/product/organization` | school, chapter, gisu, study group, UMC product org |
| Test infrastructure | `src/test/java/com/umc/product/support` | Testcontainers, fixtures, REST Docs, isolation |
| Migrations | `src/main/resources/db/migration` | `VYYYY.MM.DD.HH.MM__snake_case.sql` |

## CODE MAP

Java LSP (`jdtls`) was unavailable; CodeGraph was available for review, but reference counts remain unmeasured here.

| Symbol | Type | Location | Refs | Role |
|--------|------|----------|------|------|
| `UmcProductApplication` | class | `src/main/java/com/umc/product/UmcProductApplication.java` | n/a | Boot entry |
| `SecurityConfig` | class | `global/config/SecurityConfig.java` | n/a | Security filter chain |
| `WebMvcConfig` | class | `global/config/WebMvcConfig.java` | n/a | MVC interceptors/resolvers |
| `GlobalResponseWrapper` | class | `global/response/GlobalResponseWrapper.java` | n/a | Success response envelope |
| `GlobalExceptionHandler` | class | `global/exception/GlobalExceptionHandler.java` | n/a | MVC exception handling |
| `JwtAuthenticationFilter` | class | `global/security/JwtAuthenticationFilter.java` | n/a | JWT authentication |
| `GraphQlRuntimeWiringConfig` | class | `global/config/GraphQlRuntimeWiringConfig.java` | n/a | GraphQL scalar/runtime wiring |
| `GraphQlExceptionAdvice` | class | `global/exception/GraphQlExceptionAdvice.java` | n/a | GraphQL error mapping |
| `SsoAuthorizationCommandService` | service | `authentication/application/service` | n/a | SSO authorization-code issue |
| `SsoTokenExchangeCommandService` | service | `authentication/application/service` | n/a | SSO token exchange |
| `TraceFlowAspect` | class | `global/observability/TraceFlowAspect.java` | n/a | service/adapter tracing |
| `BaseEntity` | class | `common/BaseEntity.java` | n/a | audit timestamps |
| `ProjectApplicationCommandService` | service | `project/application/service/command` | n/a | application submit/update flow |
| `ProjectStatisticsQueryService` | service | `project/application/service/query` | n/a | statistics aggregation |
| `AdminOperationsAnalyticsQueryRepository` | repository | `analytics/adapter/out/persistence` | n/a | QueryDSL analytics aggregation |
| `IntegrationTestSupport` | test support | `src/test/java/com/umc/product/support` | n/a | Spring Boot/Testcontainers base |

## ARCHITECTURE RULES

- Layers are `domain`, `application`, and `adapter`; dependency direction must point inward.
- Allowed: `adapter/in -> application/port/in`, `application/service -> domain`, `adapter/out -> application/port/out`.
- Forbidden: `domain -> application/adapter`, `application/port -> application/service`, `adapter/in -> adapter/out`.
- Cross-domain references must use IDs, not another domain aggregate object.
- When Domain A needs Domain B data, inject Domain B's public Query UseCase. Never access Domain B's entities or repositories directly.
- Within the same aggregate, `@ManyToOne(fetch = FetchType.LAZY)` is allowed for parent/root references.
- `@OneToMany` collections are forbidden, even inside the same domain.
- State changes belong in explicit domain methods. Avoid anemic entities.

## CONVENTIONS

- Command UseCases and Query UseCases stay separate. Command services use `@Transactional`; Query services use `@Transactional(readOnly = true)`.
- Controllers return adapter `Response` records or application `Info` values. They do not return entities and do not wrap success responses in `ApiResponse`.
- REST controllers live under `adapter/in/web`; GraphQL controllers live under `adapter/in/graphql`.
- GraphQL schema files in `src/main/resources/graphql` are API contracts and must stay aligned with GraphQL DTOs.
- SSO/PKCE flows must not log authorization codes, login tokens, refresh tokens, or client secrets.
- Request records live under `adapter/in/web/dto/request` and convert to command/query objects near the adapter boundary.
- Response records live under `adapter/in/web/dto/response` and usually expose `from(Info)`.
- Query DTOs in `application/port/in/query/dto` end with `Info`; command DTOs end with `Command`.
- JPA repositories follow Spring Data names (`findById`, `findAllBy...`, `existsBy...`). Custom Ports/UseCases use semantic names.
- `get[By]` returns `T` and throws if missing; `find[By]` returns `Optional<T>` and never throws not-found; `list[By]` returns a non-null list.
- `batchGet[By]` requires all inputs to exist; `search[By]` is for dynamic/complex filters.
- Static factories: `of` for multiple params, `from` for one source, `create`/`newInstance` for guaranteed new instances.
- QueryDSL code belongs in `*QueryRepository`; avoid N+1 by fetch joins, split queries, or IN/batch maps.
- Flyway files use `VYYYY.MM.DD.HH.MM__snake_case.sql`; duplicate versions are checked by Gradle.
- REST Docs snippets use `{class-name}/{method-name}` and are assembled into `docs/static`.

## ANTI-PATTERNS

- No `@Setter` on entities.
- No `@OneToMany` entity collections.
- No business logic in controllers.
- No controller-to-repository or controller-to-adapter/out dependency.
- No entity exposure from controllers.
- No missing `@Valid` on request bodies.
- No missing transaction boundary on command services.
- No `new` for command/domain entity creation outside adapter/test boundaries; use factories or builders.
- No direct cross-domain repository/model access.
- No hidden N+1 query path.
- No AI authorship trailers or AI committer/author metadata.

## TESTING

- Unit tests prefer JUnit 5 + Mockito with `@ExtendWith(MockitoExtension.class)`.
- Integration tests reuse `IntegrationTestSupport`; persistence slices reuse `PersistenceAdapterTest`; REST Docs/web slices reuse `DocumentationTest`.
- Test names and `@DisplayName` values should be Korean and behavior-focused.
- Given/When/Then structure is expected.
- Fixture code lives in `src/test/java/com/umc/product/support/fixture` and should persist through SavePorts where possible.

## REVIEW AND GIT

- Review priorities: P1 security/data loss/severe bugs; P2 architecture/performance/scalability; P3 code quality/conventions; P4 alternatives; P5 typos/questions.
- Commit format is `<type>: <subject>` with `feat`, `fix`, `refactor`, `docs`, `test`, or `chore`.
- PR titles use `[Feat]`, `[Fix]`, `[HotFix]`, `[Refactor]`, `[Chore]`, `[Docs]`, or `[Release]`; development PRs target `develop`.
- Never set AI authorship, AI committer metadata, or `Co-authored-by` trailers for AI tools.

## COMMANDS

```bash
./gradlew bootRun
./gradlew spotlessCheck checkstyleMain checkstyleTest
./gradlew compileJava compileTestJava
./gradlew test
./gradlew asciidoctor
./gradlew build
```

## NOTES

- Default profile is `local`; app port is `8080`; management/Prometheus port is `9090`.
- `build` depends on clean/docs copy behavior; `bootJar` is deployment-focused; Checkstyle tasks are diff-oriented.
- `CLAUDE.md`, `GEMINI.md`, and `.github/copilot-instructions.md` are pointers to this canonical file.
