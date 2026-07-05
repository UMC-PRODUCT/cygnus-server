# TEST KNOWLEDGE

## OVERVIEW

Tests mirror production domains and share infrastructure under `support`. Use the existing base classes before creating new test wiring.

## STRUCTURE

```text
src/test/java/com/umc/product/
├── {domain}/                         # domain-specific unit, slice, integration tests
├── integration/                      # cross-domain integration cases
└── support/                          # Testcontainers, fixtures, REST Docs, DB isolation
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Full integration base | `support/IntegrationTestSupport.java` | `@SpringBootTest`, `test` profile, Testcontainers, DB isolation |
| Persistence slice | `support/PersistenceAdapterTest.java` | JPA/QueryDSL/Testcontainers meta annotation |
| REST Docs slice | `support/DocumentationTest.java` | `@WebMvcTest`, MockMvc, common mocks |
| REST Docs config | `support/RestDocsConfig.java` | `{class-name}/{method-name}` snippets |
| FixtureMonkey setup | `support/CommonFixture.java` | shared object generation rules |
| Domain fixtures | `support/fixture/*Fixture.java` | reusable persisted test data |
| DB isolation | `support/isolation/*` | table truncation between integration tests |
| Test docs | `docs/onboarding/test` | domain-by-domain test inventory |

## CONVENTIONS

- Unit tests use JUnit 5 and Mockito: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`.
- Controller documentation tests use `DocumentationTest` or the local `@WebMvcTest` + `RestDocsConfig` pattern.
- DB-backed integration tests extend `IntegrationTestSupport`; do not add class-level `@Transactional` to hide persistence side effects.
- Persistence adapter tests should use `PersistenceAdapterTest` instead of hand-rolled container setup.
- Test names and `@DisplayName` values should be Korean and behavior-focused.
- Keep Given/When/Then structure visible.
- Fixtures should live under `support/fixture` and provide valid domain defaults.
- Prefer SavePorts in fixtures when persisting aggregate data.
- Mock external services with `@MockitoBean` in integration tests.
- REST Docs snippets should keep the `{class-name}/{method-name}` naming rule.

## HOTSPOTS

- Project tests are large and regression-sensitive: application, permissions, forms, matching, and statistics.
- Modules with thinner test coverage include `common`, `community`, `feedback`, and `notice`.
- `support` changes can affect most integration tests; run at least focused tests plus `compileTestJava`.

## ANTI-PATTERNS

- Do not duplicate Testcontainers configuration in individual tests.
- Do not rely on test execution order.
- Do not use random fixtures when the assertion needs stable values.
- Do not bypass fixture/domain factory rules by building invalid entities.
- Do not generate REST Docs snippets with ad hoc names.
