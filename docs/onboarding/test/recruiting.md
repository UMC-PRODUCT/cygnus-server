# Recruiting v2 테스트 가이드

## 범위

현재 `src/test/java/com/umc/product/recruiting`에는 실행 가능한 `*Test.java` 77개가 있다. 최종 remediation focused 실행의 JUnit XML 기준 Recruiting은 77 suites, 344 tests, failures 0, errors 0, skipped 0이다. 원시 로그와 집계는 로컬 evidence `.omo/evidence/recruiting-v2-final-remediation.md`에 기록한다.

| 계층 | 테스트 클래스 수 | 주요 검증 |
|---|---:|---|
| Domain | 10 | 시즌·차수·지원서·평가·일정 상태와 불변식 |
| Application command | 20 | 지원, 평가, 권한, 판정, quota 예약, 등록 |
| Application query/evaluator | 8 | visibility, question scope, CSV, 권한 판정 |
| REST adapter | 10 | route, OpenAPI, 인증·인가, DTO mapping, 실제 HTTP |
| GraphQL adapter | 12 | 실행 schema, introspection, CurrentMember, 실제 HTTP |
| Persistence/ID adapter | 17 | migration, DB constraint, repository, key 충돌, concurrency |

## 검증 층위

### Focused domain/application/controller

빠른 개발 루프에서는 전체 Recruiting package를 먼저 실행한다.

```bash
./gradlew test --tests 'com.umc.product.recruiting.*'
```

주요 보호 범위는 다음과 같다.

- Season/Quota/Round: lifecycle, 트랙 subset, `INFRA_PLUS` 거부, 일정 순서와 면접 optional shape
- Form/Application: Round당 Form 하나, section policy, 로그인 소유권, Form response 연결, 제출·철회, 재지원
- Evaluator/Question: stage별 whitelist, 공통·개별 문항, 첫 제출 후 mutation freeze
- Evaluation/Schedule: `DRAFT/SUBMITTED`, peer visibility, 가능 일정 요청·제출·확정, mail state shape
- Decision/Registration: 최종 합격 track, 중복 합격, READY 예약·취소, REGISTERED 멱등 처리
- REST/GraphQL/CSV: actor spoofing 차단, deferred/legacy surface 부재, exact CSV header와 PII 제외

### PostgreSQL integration과 migration

DB 검증은 H2/in-memory 대체가 아니라 Testcontainers PostgreSQL을 사용한다.

| 테스트 | 검증 내용 |
|---|---|
| `RecruitingSeasonRoundMigrationTest` | 시즌 quota와 Round 설정 migration, constraint |
| `RecruitingRoundScheduleMigrationTest` | 서류·면접·결과 시각의 DB invariant |
| `RecruitingFormApplicationMigrationTest` | 기존 form/application 제거와 v2 schema, unique/check constraint |
| `RecruitingEvaluationScheduleMigrationTest` | legacy score/assignment 제거, 평가·일정 schema |
| `RecruitingApplicationDatabaseInvariantTest` | 지원서 email/member/track/application key invariant |
| `RecruitingRegistrationDatabaseInvariantTest` | `FINAL_PASSED`와 registration status/accepted track 정합성 |
| `RecruitingPersistenceAdapterTest` 및 세부 adapter tests | 실제 JPA save/load/search, scope와 ordering |

Migration 테스트는 빈 최신 schema만 확인하지 않는다. 필요한 테스트는 이전 migration 지점까지 적용한 뒤 v2 migration을 실행해 upgrade path를 검증한다.

### Quota concurrency

`RecruitingQuotaReservationConcurrencyTest`는 실제 PostgreSQL에서 동일 시즌·트랙의 마지막 한 자리를 동시에 READY로 요청한다.

- quota row와 application row의 pessimistic lock이 실제 transaction 사이에서 동작해야 한다.
- 두 요청 중 하나만 성공하고 다른 하나는 `RECRUITING_QUOTA_EXCEEDED`로 거부되어야 한다.
- 최종 `READY + REGISTERED` 수가 `targetCount`를 초과하지 않아야 한다.
- in-memory fake나 순차 호출만으로 이 근거를 대체하지 않는다.

단독 재실행:

```bash
./gradlew test --tests 'com.umc.product.recruiting.adapter.out.persistence.RecruitingQuotaReservationConcurrencyTest'
```

### RANDOM_PORT 실제 HTTP

`MockMvc`와 `GraphQlTester` slice 외에 실제 socket과 Spring Security filter chain을 통과한다.

| 테스트 | 실제 관찰 |
|---|---|
| `RecruitingApplicationRandomPortIntegrationTest` | JWT 지원서 생성, 비로그인·malformed 요청 거부, 공개 Form 익명 접근, CSV actor 결속과 exact redaction, PostgreSQL P6Spy binding redaction |
| `RecruitingGraphQlRandomPortIntegrationTest` | `/graphql` JWT CurrentMember 성공, 비로그인 `COMMON-403`, 제거된 익명 query의 validation 실패 |

대표 재실행:

```bash
./gradlew test \
  --tests 'com.umc.product.recruiting.adapter.in.web.RecruitingApplicationRandomPortIntegrationTest' \
  --tests 'com.umc.product.recruiting.adapter.in.graphql.RecruitingGraphQlRandomPortIntegrationTest'
```

실행 transcript에는 JWT, application key, 원문 email을 기록하지 않는다. SQL redaction 기준은 [P6Spy SQL 로그 보안 정책](../../guides/P6Spy_SQL_로그_보안_정책.md)을 따른다.

## 통합 gate

Task 12에서는 다음 gate를 각각 독립 실행하고 exit code와 원시 로그를 보존한다.

```bash
./gradlew clean compileJava compileTestJava
./gradlew test
./gradlew asciidoctor
./gradlew spotlessCheck
git diff --check
```

Migration version 중복은 파일명에서 `V<version>__` 부분을 추출해 같은 version이 두 번 이상 존재하는지 검사한다. 중복 0건이어야 한다.

전체 `test`가 통과한 뒤 RANDOM_PORT 두 클래스를 다시 실행한다. 실패 시 XML의 `tests`, `failures`, `errors`, `skipped` 합계를 근거로 보고하고, 테스트 task stdout의 추정치로 건수를 만들지 않는다.

## Evidence

- `.omo/evidence/recruiting-v2-final-remediation.md`: 77 suites/344 tests focused 실행, REST/GraphQL 인가, CSV, Form 상태, root-only lock, format/diff 검증

## 실패 분류

- domain/service 실패: 상태 전이와 business invariant를 먼저 확인한다.
- controller/schema 실패: 실제 route/schema와 actor source가 문서 계약과 일치하는지 확인한다.
- PostgreSQL/Testcontainers 실패: Docker availability와 migration failure를 분리한다.
- concurrency 실패: test flake로 치부하지 않고 lock acquisition과 transaction boundary를 조사한다.
- P6Spy redaction 실패: 로그 레벨을 낮춰 숨기지 말고 formatter/listener의 원문 값 노출을 수정한다.
