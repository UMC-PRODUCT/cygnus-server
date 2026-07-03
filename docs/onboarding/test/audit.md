# Audit 테스트 케이스

- 테스트 파일: 2개
- 테스트 케이스: 5개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller | 2 |
| Domain | 3 |

## Controller

### AuditLogControllerTest
- 위치: `src/test/java/com/umc/product/audit/adapter/in/web/AuditLogControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [35](../../../src/test/java/com/umc/product/audit/adapter/in/web/AuditLogControllerTest.java#L35) | 신규 감사 로그 admin 경로로 검색한다 | HTTP GET /api/v1/audit/admin/audit-logs | 성공: HTTP 200 OK |
| [50](../../../src/test/java/com/umc/product/audit/adapter/in/web/AuditLogControllerTest.java#L50) | 기존 감사 로그 admin 경로는 더 이상 지원하지 않는다 | HTTP GET /api/v1/admin/audit-logs | 실패: HTTP 404 Not Found |

## Domain

### AuditLogEventTest
- 위치: `src/test/java/com/umc/product/audit/domain/AuditLogEventTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [11](../../../src/test/java/com/umc/product/audit/domain/AuditLogEventTest.java#L11) | eventId와 occurredAt을 지정하지 않으면 기본값이 자동 주입된다 | 조건 eventId와 occurredAt을 지정하지 않으면 기본값이 자동 주입된다 | 성공: 검증 assertThat(event.eventId()).isNotNull(); assertThat(event.occurredAt()).isBetween(before, after); |
| [31](../../../src/test/java/com/umc/product/audit/domain/AuditLogEventTest.java#L31) | eventId와 occurredAt을 명시하면 그 값이 그대로 유지된다 | 조건 eventId와 occurredAt을 명시하면 그 값이 그대로 유지된다 | 성공: 검증 assertThat(event.eventId()).isEqualTo(givenId); assertThat(event.occurredAt()).isEqualTo(givenInstant); |
| [53](../../../src/test/java/com/umc/product/audit/domain/AuditLogEventTest.java#L53) | eventType은 'audit.log.<action>' 형식으로 생성된다 | 조건 eventType은 'audit.log.<action>' 형식으로 생성된다 | 성공: 검증 assertThat(registerEvent.eventType()).isEqualTo("audit.log.register"); assertThat(withdrawEvent.eventType()).isEqualTo("audit.log.withdraw"); |
