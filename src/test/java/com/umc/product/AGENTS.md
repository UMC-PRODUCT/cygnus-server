# TEST KNOWLEDGE

## OVERVIEW

Tests mirror production domains and share infrastructure under `support`. Use the existing base classes before creating new test wiring.

## STRUCTURE

```text
src/test/java/com/umc/product/
├── {domain}/                         # domain-specific unit, slice, integration tests
├── integration/                      # cross-domain integration cases
└── support/                          # Testcontainers, fixtures, MockMvc, DB isolation
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Full integration base | `support/IntegrationTestSupport.java` | `@SpringBootTest`, `test` profile, Testcontainers, DB isolation |
| Persistence slice | `support/PersistenceAdapterTest.java` | JPA/QueryDSL/Testcontainers meta annotation |
| MVC 테스트 지원 | `support/ControllerTestSupport.java` | `@WebMvcTest`, MockMvc, 공통 mock |
| FixtureMonkey setup | `support/CommonFixture.java` | shared object generation rules |
| Domain fixtures | `support/fixture/*Fixture.java` | reusable persisted test data |
| DB isolation | `support/isolation/*` | table truncation between integration tests |
| Test docs | `docs/onboarding/test` | domain-by-domain test inventory |

## CONVENTIONS

- Unit tests use JUnit 5 and Mockito: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`.
- 컨트롤러 테스트는 `ControllerTestSupport` 또는 개별 `@WebMvcTest` 구성을 사용한다.
- DB-backed integration tests extend `IntegrationTestSupport`; do not add class-level `@Transactional` to hide persistence side effects.
- Persistence adapter tests should use `PersistenceAdapterTest` instead of hand-rolled container setup.
- Test names and `@DisplayName` values should be Korean and behavior-focused.
- Keep Given/When/Then structure visible.
- Fixtures should live under `support/fixture` and provide valid domain defaults.
- Prefer SavePorts in fixtures when persisting aggregate data.
- Mock external services with `@MockitoBean` in integration tests.

## HOTSPOTS

- Project tests are large and regression-sensitive: application, permissions, forms, matching, and statistics.
- Modules with thinner test coverage include `common`, `community`, `feedback`, and `notice`.
- `support` changes can affect most integration tests; run at least focused tests plus `compileTestJava`.

## ANTI-PATTERNS

- Do not duplicate Testcontainers configuration in individual tests.
- Do not rely on test execution order.
- Do not use random fixtures when the assertion needs stable values.
- Do not bypass fixture/domain factory rules by building invalid entities.
