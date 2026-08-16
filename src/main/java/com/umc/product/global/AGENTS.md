# GLOBAL/COMMON KNOWLEDGE

## OVERVIEW

`global` owns cross-cutting runtime behavior; `common` owns shared base entities and enums used by multiple domains.

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Success response wrapping | `response/GlobalResponseWrapper.java` | Auto-wraps successful responses in `ApiResponse` |
| Error response shape | `response/ApiErrorResponseFactory.java`, `response/ApiErrorResponseWriter.java` | MVC and servlet/filter error paths share the envelope |
| MVC exceptions | `exception/GlobalExceptionHandler.java` | Main exception conversion point |
| Fallback errors | `exception/CustomErrorController.java` | 404/filter-level fallback outside normal MVC handling |
| GraphQL errors | `exception/GraphQlExceptionAdvice.java` | GraphQL exception mapping |
| Security chain | `config/SecurityConfig.java` | JWT, stateless session, `@Public`, Swagger deny paths |
| GraphQL runtime | `config/GraphQlRuntimeWiringConfig.java`, `config/GraphQlExecutionConfig.java` | scalar/runtime wiring and execution settings |
| Client context | `client/*` | service/device/environment classification for SSO/client-aware flows |
| Public endpoint scan | `security/util/PublicEndpointCollector.java` | Collects `@Public` routes for permitAll |
| Current member | `security/resolver/CurrentMemberArgumentResolver.java` | Resolves `@CurrentMember MemberPrincipal` |
| Rate limit | `ratelimit/*` | MVC interceptor with authenticated/anonymous policies |
| Observability | `observability/*` | service/adapter span creation and context propagation |
| Shared entity base | `../common/BaseEntity.java` | Audit timestamps only; entities define IDs themselves |
| Shared enums | `../common/domain/enums/*` | Cross-domain enums and parsing rules |

## CONVENTIONS

- Controllers should not manually wrap success responses in `ApiResponse`; `GlobalResponseWrapper` handles that.
- Error paths may use `ApiResponse` directly through `ApiErrorResponseFactory` or `ApiErrorResponseWriter`.
- Security failures do not go through normal controller advice: use `ApiAuthenticationEntryPoint` or `ApiAccessDeniedHandler`.
- GraphQL failures use `GraphQlExceptionAdvice`; do not assume MVC advice covers GraphQL resolver errors.
- Public APIs should use `@Public`; avoid duplicating permitAll paths by hand unless `SecurityPathConfig` is the intended source.
- Request-member access should use `@CurrentMember`, not direct `SecurityContextHolder` reads in controllers.
- New cross-cutting configuration belongs in `global/config`; domain-specific config stays inside the domain package.
- Client classification belongs in `global/client`; domain code should consume the resolved context instead of reparsing headers.
- `BaseEntity` provides `createdAt` and `updatedAt` only. Do not move domain IDs or domain state into it.
- Shared enums may include parsing/domain-rule helpers, but should not depend on application services or adapters.
- Observability aspects should stay lightweight and must not change business behavior.

## ANTI-PATTERNS

- Do not bypass `GlobalResponseWrapper` from controllers for normal success responses.
- Do not leak detailed internal exception messages in production-facing handlers.
- Do not add security allow/deny paths in multiple places without updating `SecurityPathConfig`.
- Do not put domain-specific exception codes into `CommonErrorCode` unless they are truly global.
- Do not introduce adapter dependencies into `common`.
- Do not expose GraphQL sandbox or schema paths without checking `SecurityPathConfig`.
