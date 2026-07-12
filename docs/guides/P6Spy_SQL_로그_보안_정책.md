# P6Spy SQL 로그 보안 정책

## 기본 원칙

P6Spy 로그와 DB tracing span에는 SQL 구조만 남기고 바인딩 값은 남기지 않는다. 이메일, 토큰, 지원 키뿐 아니라 모든 prepared statement parameter가 같은 정책을 적용받는다.

- prepared statement는 P6Spy의 `prepared` SQL을 선택해 `?` placeholder를 유지한다.
- plain statement에 포함된 문자열 literal, dollar-quoted literal, SQL comment 내용은 `[REDACTED]`로 치환한다.
- 선택적으로 `db.statement` span을 활성화해도 같은 `SqlLogRedactor`를 적용한다.
- `spy.properties`에서 formatter를 등록해 Spring `@PostConstruct`보다 먼저 실행되는 Flyway/JPA bootstrap SQL에도 정책을 적용한다.

## 운영 디버깅 가치

테이블, 컬럼, JOIN, predicate 형태, placeholder 수, SQL operation과 실행 시간은 유지한다. 따라서 N+1, 잘못된 JOIN, 인덱스 후보, query shape와 latency는 분석할 수 있다. 실제 parameter 값이 필요한 장애 분석은 로그 설정을 완화하지 않고 재현 가능한 비식별 fixture와 DB-side 제한 조회를 사용한다.

## 금지 사항

- 운영 또는 local 환경에서 bound SQL 원문을 출력하도록 formatter를 교체하지 않는다.
- application key, access token, email 등 민감값을 임시 debug log에 기록하지 않는다.
- 테스트 transcript에는 생성 결과의 `applicationKey`를 반드시 `[REDACTED]`로 치환한다.

## 검증

- `P6SpyConfigTest`: prepared INSERT/SELECT와 plain SQL redaction, 비민감 SQL 구조 보존.
- `QueryStatsJdbcEventListenerTest`: DB span의 inline literal redaction.
- `RecruitingApplicationRandomPortIntegrationTest`: 실제 PostgreSQL P6Spy INSERT/SELECT 출력과 random-port HTTP transcript 검증.
