# Global 테스트 케이스

- 테스트 파일: 59개
- 테스트 케이스: 251개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Domain`, `External Adapter`, `Support`
- 집계 기준: 위 수치는 `src/test/java/com/umc/product/global` 전체의 Java 파일과 `@Test`,
  `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, `@TestTemplate` 메서드 수다. 파라미터별 실행 횟수는 합산하지 않는다.
- 문서 범위: 아래 표는 운영에 영향이 큰 대표 검증 17개 파일·67개 케이스를 다룬다. 테스트 인덱스의 Global 행은 이 문서 범위를 집계한다.

## UseCase / Application Service

### EventOutboxRelayServiceTest
- 테스트 설명: EventOutboxRelayService
- 위치: `src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [39](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L39) | publishable outbox를 DomainEvent로 복원해 Spring event bus로 발행하고 published 처리한다 | 호출 relay() | 실패: 예외 TestEvent; 검증 assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED); assertThat(publisher.events).hasSize(1); assertThat(publisher.events.getFirst()).isInstanceOf(TestEvent.class); assertThat(((TestEvent) publisher.... |
| [69](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L69) | EventOutboxRelayService / 이벤트 복원 또는 발행 실패 시 별도 상태 저장 트랜잭션에서 attempts를 증가시키고 pending으로 남긴다 | 호출 relay() | 실패: 검증 assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING); assertThat(outbox.getAttempts()).isEqualTo(1); assertThat(outbox.getLastError()).contains("publish failed"); assertThat(savePort.savedStatuses).cont... |
| [100](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L100) | 최대 재시도 횟수에 도달하면 failed 상태로 저장한다 | 호출 relay() | 성공: 검증 assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED); assertThat(outbox.getAttempts()).isEqualTo(2); assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.FAILED); |
| [131](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L131) | 저장된 traceparent가 있으면 relay span에 원 요청 trace link를 부착한다 | 유효한 W3C traceparent | relay span link의 trace ID가 원 요청과 일치하고 PUBLISHED 처리 |
| [167](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L167) | traceparent가 없으면 link 없이 relay span을 생성한다 | traceparent 없음 | link 없이 relay span 생성 후 PUBLISHED 처리 |
| [198](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L198) | non-transactional listener 성공 후 상태 저장 실패를 재시도로 연결한다 | PUBLISHED 저장에서 예외 | listener 1회 실행, attempts 증가, PENDING 저장 |
| [227](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L227) | non-transactional listener를 transaction 밖에서 실행한다 | `OutboxDispatchMode.NON_TRANSACTIONAL` | listener transaction 비활성, 별도 transaction에서 PUBLISHED 처리 |
| [256](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L256) | non-transactional listener 예외를 재시도로 연결한다 | listener에서 외부 호출 예외 | attempts 증가, error 기록, PENDING 저장 |
| [286](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L286) | lease 소유권을 잃은 worker는 최신 상태를 덮어쓰지 않는다 | PUBLISHED 저장에서 optimistic lock 예외 | listener 1회 실행 후 stale worker 상태 저장 생략 |

## Repository / Outbound Persistence

### EventOutboxJpaRepositoryTest
- 테스트 설명: EventOutbox JPA polling 및 lease fencing
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxJpaRepositoryTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [41](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxJpaRepositoryTest.java#L41) | 발행 가능한 이벤트를 다음 시도 시각 순서로 조회한다 | PENDING/PUBLISHED 행 혼합 | 발행 대상 PENDING 행만 시각 순서로 반환 |
| [58](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxJpaRepositoryTest.java#L58) | lease 재획득 후 이전 worker의 상태 덮어쓰기를 차단한다 | 두 EntityManager가 같은 outbox version으로 시작 | stale merge에서 `OptimisticLockException` 발생 |

## E2E / Integration

### SecurityConfigIntegrationTest
- 테스트 설명: SecurityConfig 통합 테스트
- 위치: `src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [13](../../../src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java#L13) | docs 진입 경로는 Scalar HTML로 리다이렉트한다 | HTTP GET /docs | 성공: is3xxRedirection |
| [24](../../../src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java#L24) | SecurityConfig 통합 테스트 / 인증된 요청이어도 Swagger UI 경로는 접근할 수 없다 | HTTP GET /swagger-ui/index.html | 실패: HTTP 403 Forbidden |
| [38](../../../src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java#L38) | SecurityConfig 통합 테스트 / 인증된 요청이어도 기존 OpenAPI JSON 경로는 접근할 수 없다 | HTTP GET /v3/api-docs | 실패: HTTP 403 Forbidden |

### EventOutboxRelayJdbcIntegrationTest
- 테스트 설명: non-transactional relay의 실제 JDBC connection 경계
- 위치: `src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayJdbcIntegrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [42](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayJdbcIntegrationTest.java#L42) | non-transactional listener 실행 중에는 JDBC connection을 점유하지 않는다 | 실제 PostgreSQL DataSource와 Hikari pool | 트랜잭션 비활성, active connection 0, PUBLISHED 처리 |

## Domain

### EventOutboxTest
- 테스트 설명: EventOutbox
- 위치: `src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [16](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L16) | EventOutbox / 발행 실패 시 attempts를 증가시키고 다음 시도 시간을 기록한다 | 조건 EventOutbox / 발행 실패 시 attempts를 증가시키고 다음 시도 시간을 기록한다 | 실패: 검증 assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING); assertThat(outbox.getAttempts()).isEqualTo(1); assertThat(outbox.getNextAttemptAt()).isEqualTo(nextAttemptAt); assertThat(outbox.getLastError()).isE... |
| [30](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L30) | 최대 시도 횟수에 도달하면 failed 상태로 전환한다 | 조건 최대 시도 횟수에 도달하면 failed 상태로 전환한다 | 성공: 검증 assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED); assertThat(outbox.getAttempts()).isEqualTo(2); assertThat(outbox.getLastError()).isEqualTo("second"); |

## External Adapter

### EventPayloadDeserializerTest
- 테스트 설명: EventPayloadDeserializer
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [13](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java#L13) | eventClass와 payload로 DomainEvent를 복원한다 | 조건 eventClass와 payload로 DomainEvent를 복원한다 | 실패: 예외 TestEvent; 검증 assertThat(result).isInstanceOf(TestEvent.class); assertThat(((TestEvent) result).message()).isEqualTo("hello"); |
| [31](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java#L31) | EventPayloadDeserializer / eventClass가 DomainEvent 타입이 아니면 예외를 던진다 | 조건 EventPayloadDeserializer / eventClass가 DomainEvent 타입이 아니면 예외를 던진다 | 실패: 예외 IllegalStateException |
| [44](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java#L44) | EventPayloadDeserializer / eventClass를 찾을 수 없으면 예외를 던진다 | 조건 EventPayloadDeserializer / eventClass를 찾을 수 없으면 예외를 던진다 | 실패: 예외 IllegalStateException |
| [57](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java#L57) | EventPayloadDeserializer / payload JSON을 복원할 수 없으면 예외를 던진다 | 조건 EventPayloadDeserializer / payload JSON을 복원할 수 없으면 예외를 던진다 | 실패: 예외 IllegalStateException |

### EventPayloadSerializerTest
- 테스트 설명: EventPayloadSerializer
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/EventPayloadSerializerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [22](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadSerializerTest.java#L22) | EventPayloadSerializer / 직렬화 실패 시 eventType을 포함한 예외를 던진다 | 조건 EventPayloadSerializer / 직렬화 실패 시 eventType을 포함한 예외를 던진다 | 실패: 예외 IllegalStateException |

### OutboxDomainEventPublisherTest
- 테스트 설명: OutboxDomainEventPublisher
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/OutboxDomainEventPublisherTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [15](../../../src/test/java/com/umc/product/global/event/adapter/out/OutboxDomainEventPublisherTest.java#L15) | publish는 도메인 이벤트를 직발행하지 않고 event outbox로 저장한다 | 조건 publish는 도메인 이벤트를 직발행하지 않고 event outbox로 저장한다 | 성공: 검증 assertThat(savePort.saved).hasSize(1); assertThat(outbox.getEventId()).isEqualTo(event.eventId()); assertThat(outbox.getEventType()).isEqualTo("test.created"); assertThat(outbox.getPayload()).contains("\"message\":\"h... |
| [37](../../../src/test/java/com/umc/product/global/event/adapter/out/OutboxDomainEventPublisherTest.java#L37) | OutboxDomainEventPublisher / publishAll은 입력 순서대로 모든 이벤트를 일괄 저장한다 | 조건 OutboxDomainEventPublisher / publishAll은 입력 순서대로 모든 이벤트를 일괄 저장한다 | 성공: 검증 assertThat(savePort.saved); .containsExactly(first.eventId(), second.eventId()); assertThat(savePort.saveAllCalled).isTrue(); |

## Support / Config / Utility

### ApiAccessDeniedHandlerTest
- 위치: `src/test/java/com/umc/product/global/security/ApiAccessDeniedHandlerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [18](../../../src/test/java/com/umc/product/global/security/ApiAccessDeniedHandlerTest.java#L18) | 인가 실패 응답은 내부 AccessDeniedException 메시지를 노출하지 않는다 | 조건 인가 실패 응답은 내부 AccessDeniedException 메시지를 노출하지 않는다 | 실패: 에러코드 CommonErrorCode.FORBIDDEN; 검증 assertThat(response.getStatus()).isEqualTo(403); assertThat(response.getContentAsString()); .contains("\"success\":false"); .contains("\"code\":\"" + CommonErrorCode.FORBIDDEN.getCode() + "\"") |

### CustomErrorControllerTest
- 위치: `src/test/java/com/umc/product/global/exception/CustomErrorControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [19](../../../src/test/java/com/umc/product/global/exception/CustomErrorControllerTest.java#L19) | fallback error controller도 BusinessException 상세 메시지를 유지한다 | 조건 fallback error controller도 BusinessException 상세 메시지를 유지한다 | 성공: 에러코드 AuthorizationErrorCode.PERMISSION_DENIED; 검증 assertThat(response.getStatusCode()).isEqualTo(AuthorizationErrorCode.PERMISSION_DENIED.getHttpStatus()); assertThat(response.getBody()).satisfies(body -> {; assertThat(body.getCode()).isEqualTo(AuthorizationErrorCode.P... |
| [38](../../../src/test/java/com/umc/product/global/exception/CustomErrorControllerTest.java#L38) | fallback error controller도 RESOURCE_ACCESS_DENIED 기본 메시지를 detail로 내려준다 | 조건 fallback error controller도 RESOURCE_ACCESS_DENIED 기본 메시지를 detail로 내려준다 | 성공: 에러코드 AuthorizationErrorCode.RESOURCE_ACCESS_DENIED; 검증 assertThat(response.getStatusCode()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED.getHttpStatus()); assertThat(response.getBody()).satisfies(body -> {; assertThat(body.getCode()).isEqualTo(AuthorizationErrorC... |

### EmailMaskerTest
- 테스트 설명: EmailMasker — 이메일 마스킹 유틸
- 위치: `src/test/java/com/umc/product/global/util/EmailMaskerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [6](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L6) | EmailMasker — 이메일 마스킹 유틸 | 조건 EmailMasker — 이메일 마스킹 유틸 | 성공: 검증 assertThat(EmailMasker.mask(null)).isNull(); |
| [14](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L14) | EmailMasker — 이메일 마스킹 유틸 / 빈 문자열은 그대로 반환한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 빈 문자열은 그대로 반환한다 | 성공: 검증 assertThat(EmailMasker.mask("")).isEqualTo(""); assertThat(EmailMasker.mask(" ")).isEqualTo(" "); |
| [20](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L20) | EmailMasker — 이메일 마스킹 유틸 / 골뱅이가 없는 입력은 원문 그대로 반환한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 골뱅이가 없는 입력은 원문 그대로 반환한다 | 성공: 검증 assertThat(EmailMasker.mask("notAnEmail")).isEqualTo("notAnEmail"); |
| [26](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L26) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트가 비어있으면 원문 그대로 반환한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트가 비어있으면 원문 그대로 반환한다 | 성공: 검증 assertThat(EmailMasker.mask("@domain.com")).isEqualTo("@domain.com"); |
| [31](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L31) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 1은 그대로 반환한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 1은 그대로 반환한다 | 성공: 검증 assertThat(EmailMasker.mask("a@umc.com")).isEqualTo("a@umc.com"); |
| [36](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L36) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 2는 앞 1글자만 남기고 마스킹한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 2는 앞 1글자만 남기고 마스킹한다 | 성공: 검증 assertThat(EmailMasker.mask("ab@umc.com")).isEqualTo("a*@umc.com"); |
| [41](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L41) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 3은 앞 1글자만 남기고 마스킹한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 3은 앞 1글자만 남기고 마스킹한다 | 성공: 검증 assertThat(EmailMasker.mask("abc@umc.com")).isEqualTo("a**@umc.com"); |
| [46](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L46) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 4는 앞 3글자만 남기고 마스킹한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 4는 앞 3글자만 남기고 마스킹한다 | 성공: 검증 assertThat(EmailMasker.mask("abcd@umc.com")).isEqualTo("abc*@umc.com"); |
| [51](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L51) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트가 긴 경우 앞 3글자만 남기고 마스킹한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트가 긴 경우 앞 3글자만 남기고 마스킹한다 | 성공: 검증 assertThat(EmailMasker.mask("donggukcd200@gmail.com")); .isEqualTo("don*********@gmail.com"); |
| [57](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L57) | EmailMasker — 이메일 마스킹 유틸 / 도메인은 절대 마스킹되지 않는다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 도메인은 절대 마스킹되지 않는다 | 성공: 검증 assertThat(masked).endsWith("@hanyang.ac.kr"); |

### ExternalApiCallLoggerTest
- 위치: `src/test/java/com/umc/product/global/logging/ExternalApiCallLoggerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [54](../../../src/test/java/com/umc/product/global/logging/ExternalApiCallLoggerTest.java#L54) | 성공 호출은 INFO + result=SUCCESS + durationMs 로 기록되고 반환값을 그대로 돌려준다 | 조건 성공 호출은 INFO + result=SUCCESS + durationMs 로 기록되고 반환값을 그대로 돌려준다 | 성공: 검증 assertThat(result).isEqualTo("pr-list"); assertThat(event.getLevel()).isEqualTo(Level.INFO); assertThat(event.getMessage()).isEqualTo("external_api_called"); assertThat(kvOf(event, "provider")).isEqualTo("GITHUB"); |
| [76](../../../src/test/java/com/umc/product/global/logging/ExternalApiCallLoggerTest.java#L76) | RuntimeException 발생 시 WARN + result=FAILURE + errorClass 가 기록되고 예외는 재던져진다 | 조건 RuntimeException 발생 시 WARN + result=FAILURE + errorClass 가 기록되고 예외는 재던져진다 | 실패: 예외 발생; 검증 assertThat(event.getLevel()).isEqualTo(Level.WARN); assertThat(event.getMessage()).isEqualTo("external_api_called"); assertThat(kvOf(event, "provider")).isEqualTo("OPENAI"); assertThat(kvOf(event, "operation")).isEqua... |

### GlobalExceptionHandlerTest
- 위치: `src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [33](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L33) | RESOURCE_ACCESS_DENIED 기본 예외도 message가 null로 내려가지 않는다 | 조건 RESOURCE_ACCESS_DENIED 기본 예외도 message가 null로 내려가지 않는다 | 실패: 에러코드 AuthorizationErrorCode.RESOURCE_ACCESS_DENIED; 검증 assertThat(response.getStatusCode()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED.getHttpStatus()); assertThat(response.getBody()); assertThat(body.getCode()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_... |
| [53](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L53) | Spring Security AccessDeniedException은 MVC 경로에서도 403으로 응답한다 | HTTP GET /access-denied | 실패: HTTP 403 Forbidden; 에러코드 CommonErrorCode.FORBIDDEN |
| [70](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L70) | JSON 파싱 오류는 내부 파서 상세 메시지를 응답에 노출하지 않는다 | HTTP POST /body | 실패: HTTP 400 Bad Request; 검증 assertThat(result.getResponse().getContentAsString()) |
| [94](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L94) | 요청 본문이 없으면 사용자가 이해할 수 있는 다음 행동을 안내한다 | HTTP POST /body | 실패: HTTP 400 Bad Request; 에러코드 CommonErrorCode.BAD_REQUEST |
| [111](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L111) | 요청 값 형식이 맞지 않으면 사용자가 확인할 값을 안내한다 | HTTP GET /number; param value="not-number" | 실패: HTTP 400 Bad Request; 에러코드 CommonErrorCode.BAD_REQUEST |

### JwtTokenProviderEmailVerificationTest
- 위치: `src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [44](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L44) | REGISTER 로 발급한 토큰은 REGISTER 로 파싱 시 이메일을 반환한다 | 조건 REGISTER 로 발급한 토큰은 REGISTER 로 파싱 시 이메일을 반환한다 | 성공: 검증 assertThat(parsedEmail).isEqualTo(EMAIL); |
| [57](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L57) | PASSWORD_RESET 로 발급한 토큰은 PASSWORD_RESET 로 파싱 시 이메일을 반환한다 | 조건 PASSWORD_RESET 로 발급한 토큰은 PASSWORD_RESET 로 파싱 시 이메일을 반환한다 | 성공: 검증 assertThat(parsedEmail).isEqualTo(EMAIL); |
| [70](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L70) | CHANGE_EMAIL 로 발급한 토큰은 CHANGE_EMAIL 로 파싱 시 이메일을 반환한다 | 조건 CHANGE_EMAIL 로 발급한 토큰은 CHANGE_EMAIL 로 파싱 시 이메일을 반환한다 | 성공: 검증 assertThat(parsedEmail).isEqualTo(EMAIL); |
| [83](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L83) | REGISTER 토큰을 PASSWORD_RESET 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 REGISTER 토큰을 PASSWORD_RESET 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); |
| [97](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L97) | PASSWORD_RESET 토큰을 REGISTER 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 PASSWORD_RESET 토큰을 REGISTER 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); |
| [111](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L111) | REGISTER 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 REGISTER 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); |
| [125](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L125) | PASSWORD_RESET 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 PASSWORD_RESET 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); |
| [139](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L139) | RefreshToken 발급 시 jti와 만료시각을 포함하고 파싱 결과로 반환한다 | 조건 RefreshToken 발급 시 jti와 만료시각을 포함하고 파싱 결과로 반환한다 | 실패: 검증 assertThat(claims.memberId()).isEqualTo(memberId); assertThat(claims.jti()).isNotNull(); assertThat(claims.expiresAt()).isNotNull(); |

### LoggingInterceptorTest
- 위치: `src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [57](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L57) | preHandle 시 method / path 가 MDC 에 들어가고 traceId 가 있으면 X-Trace-Id 헤더가 채워진다 | HTTP PUT traceId; HTTP GET method; HTTP GET path; HTTP GET requestId | 성공: 검증 assertThat(result).isTrue(); assertThat(MDC.get("method")).isEqualTo("GET"); assertThat(MDC.get("path")).isEqualTo("/forms/123/answers"); assertThat(response.getHeader("X-Trace-Id")).isEqualTo("test-trace-abc123"); |
| [78](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L78) | traceId 가 없으면 X-Trace-Id 헤더도 비어 있어야 한다 | 조건 traceId 가 없으면 X-Trace-Id 헤더도 비어 있어야 한다 | 성공: 검증 assertThat(response.getHeader("X-Trace-Id")).isNull(); |
| [92](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L92) | afterCompletion 의 finally 에서 MDC 가 반드시 비워진다 | HTTP GET method | 성공: 검증 assertThat(MDC.get("method")).isEqualTo("GET"); assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty(); |
| [110](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L110) | api_request_completed 로그의 MDC 스냅샷에 uriTemplate / statusCode / durationMs 가 포함된다 | 조건 api_request_completed 로그의 MDC 스냅샷에 uriTemplate / statusCode / durationMs 가 포함된다 | 성공: 검증 assertThat(snapshot).isNotNull(); assertThat(snapshot).containsEntry("event", "api_request_completed"); assertThat(snapshot).containsEntry("uriTemplate", "/forms/{formId}/answers"); assertThat(snapshot).containsEntry(... |
| [136](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L136) | preHandle 이 호출되지 않은 상태에서 afterCompletion 이 호출되어도 안전하게 종료된다 | 조건 preHandle 이 호출되지 않은 상태에서 afterCompletion 이 호출되어도 안전하게 종료된다 | 성공: 검증 assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty(); |
| [148](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L148) | 인증된 MemberPrincipal 의 clientType 이 없으면 MDC clientType 을 UNKNOWN 으로 채운다 | HTTP GET memberId; HTTP GET clientType; HTTP GET userId | 성공: 검증 assertThat(MDC.get("memberId")).isEqualTo("42"); assertThat(MDC.get("clientType")).isEqualTo("UNKNOWN"); assertThat(MDC.get("userId")).isNull(); |
| [171](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L171) | 인증된 MemberPrincipal 의 clientType 이 있으면 해당 값을 MDC 에 채운다 | HTTP GET memberId; HTTP GET clientType | 성공: 검증 assertThat(MDC.get("memberId")).isEqualTo("42"); assertThat(MDC.get("clientType")).isEqualTo("IOS"); |
| [192](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L192) | 익명 사용자는 MDC memberId 가 채워지지 않는다 | HTTP GET memberId; HTTP GET clientType | 성공: 검증 assertThat(MDC.get("memberId")).isNull(); assertThat(MDC.get("clientType")).isEqualTo("UNKNOWN"); |
| [207](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L207) | X-Forwarded-For 헤더가 있으면 첫 번째 IP 가 clientIp 로 채워진다 | 조건 X-Forwarded-For 헤더가 있으면 첫 번째 IP 가 clientIp 로 채워진다 | 성공: 검증 assertThat(snapshot).isNotNull(); assertThat(snapshot).containsEntry("clientIp", "203.0.113.7"); |

### QueryStatsJdbcEventListenerTest
- 위치: `src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [44](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L44) | DB 쿼리 실행을 child span으로 남기고 요청 단위 쿼리 통계를 기록한다 | 호출 onBeforeExecuteQuery(info); 호출 onAfterExecuteQuery(info, 12_500_000L, null) | 성공: 검증 assertThat(QueryStatsHolder.getQueryCount()).isEqualTo(1L); assertThat(QueryStatsHolder.getTotalTimeMs()).isEqualTo(12L); |
| [65](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L65) | SQL 앞에 주석이 있어도 실제 DB operation을 기록한다 | 호출 onBeforeExecuteQuery(info); 호출 onAfterExecuteQuery(info, 1_000_000L, null) | 성공: SQL 앞에 주석이 있어도 실제 DB operation을 기록한다 |
| [77](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L77) | DB 쿼리 실패 시 span에 예외를 기록하고 요청 통계에는 성공 쿼리만 반영한다 | 호출 onBeforeExecuteQuery(info); 호출 onAfterExecuteQuery(info, 3_000_000L, exception) | 실패: 검증 assertThat(QueryStatsHolder.getQueryCount()).isZero(); |

### TraceFlowAspectTest
- 위치: `src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [45](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L45) | UseCase 구현체 호출을 UseCase 이름의 span으로 감싼다 | 호출 traceUseCaseAndAdapter(joinPoint) | 성공: 검증 assertThat(result).isEqualTo("result"); |
| [63](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L63) | adapter.out 호출을 adapter span으로 감싼다 | 호출 traceUseCaseAndAdapter(joinPoint) | 성공: 검증 assertThat(result).isEqualTo("entity"); |
| [80](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L80) | 상위 클래스가 구현한 UseCase interface도 UseCase span으로 감싼다 | 호출 traceUseCaseAndAdapter(joinPoint) | 성공: 검증 assertThat(result).isEqualTo("result"); |
