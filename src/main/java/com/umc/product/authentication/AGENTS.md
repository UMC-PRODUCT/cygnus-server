# AUTHENTICATION KNOWLEDGE

## OVERVIEW

`authentication` owns local login, OAuth login, JWT issuing/refresh, email verification, SSO browser login, and SSO authorization-code/PKCE flows.

## STRUCTURE

```text
authentication/
├── domain/                         # refresh token, OAuth, SSO client/code entities
├── config/                         # SSO properties
├── application/event               # authentication/SSO lifecycle events
├── application/port/in             # auth and SSO UseCases
├── application/port/out            # token/code/client persistence and external ports
├── application/service             # login, token issue, SSO authorize/exchange
├── adapter/in/web                  # REST auth and SSO controllers
├── adapter/in/event                # security metric event listeners
├── adapter/in/scheduler            # cleanup/scheduled auth jobs
└── adapter/out                     # OAuth providers, SSO config, persistence
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Local/OAuth login | `adapter/in/web/AuthenticationController.java`, `CredentialAuthenticationService.java` | email/password and provider login |
| Token refresh/logout | `TokenAuthenticationController.java`, `AuthenticationService.java` | JWT renewal and refresh token validation |
| SSO browser login | `SsoBrowserLoginController.java`, `SsoBrowserLoginCommandService.java` | Auth App cookie login |
| SSO authorize | `SsoOAuthController.java`, `SsoAuthorizationCommandService.java` | authorization code issue |
| SSO token exchange | `SsoTokenExchangeCommandService.java` | authorization code + PKCE verifier |
| Client config | `adapter/out/config/SsoClientConfigAdapter.java`, `config/SsoProperties.java` | allowed clients/origins/redirects |
| Persistence | `SsoAuthorizationCodePersistenceAdapter.java`, `RefreshToken*` | code and token storage |
| Events/metrics | `application/event`, `adapter/in/event/SsoSecurityEventListener.java` | post-commit security metrics |

## CONVENTIONS

- SSO uses Authorization Code with PKCE S256. Reject non-S256 or missing verifier/challenge data.
- SSO client/redirect/origin validation must happen before issuing codes or tokens.
- Browser login cookie work belongs in `SsoCookieWriter`, not controllers.
- JWT issuing should go through `AuthenticationTokenIssuer` or dedicated token providers.
- Lifecycle events must not include raw authorization codes, login tokens, refresh tokens, or secrets.
- External provider details stay in `adapter/out/external`; services depend on ports.
- Command services require `@Transactional`; query services use `@Transactional(readOnly = true)`.

## ANTI-PATTERNS

- Do not log OAuth codes, SSO login tokens, JWTs, refresh tokens, or PKCE verifiers.
- Do not bypass `SsoClientConfigAdapter`/client validation for new SSO clients.
- Do not store refresh token behavior in controllers.
- Do not couple authentication directly to member persistence internals beyond public ports.
- Do not add auth endpoints without reviewing `SecurityPathConfig`.
