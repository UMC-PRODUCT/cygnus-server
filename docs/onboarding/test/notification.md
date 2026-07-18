# Notification 테스트 케이스

- 테스트 파일: 17개
- 테스트 케이스: 50개 (`@Test` 49 + `@ParameterizedTest` 1)
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`
- source of truth: `src/test/java/com/umc/product/notification`의 현재 파일과 JUnit test annotation

| 카테고리 | 파일 수 | 케이스 수 |
|---|---:|---:|
| UseCase / Application Service | 9 | 27 |
| Port / DTO | 2 | 5 |
| Domain | 1 | 3 |
| Event Adapter | 2 | 3 |
| External Adapter | 2 | 8 |
| Integration | 1 | 4 |
| 합계 | 17 | 50 |

## Issue #1147 template-email 신규 계약 map

아래는 실제 신규 template-email 요청·렌더링·동기 listener·SES 경계 테스트다. Recruiting 구현이나
메일 delivery entity를 전제로 하지 않으며, Notification 공개 계약과 공용 outbox relay 결과만 검증한다.

### Port / DTO

| 테스트 클래스 | 위치 | 검증 목적 |
|---|---|---|
| `EmailContractBaselineTest` | [`application/port/in/dto/EmailContractBaselineTest.java`](../../../src/test/java/com/umc/product/notification/application/port/in/dto/EmailContractBaselineTest.java) | 기존 `SendHtmlEmailCommand`와 발신자 properties의 baseline shape를 보존한다. |
| `SendTemplateEmailCommandTest` | [`application/port/in/dto/SendTemplateEmailCommandTest.java`](../../../src/test/java/com/umc/product/notification/application/port/in/dto/SendTemplateEmailCommandTest.java) | event ID/type/recipient/variables/availableAt record shape, trim snapshot과 unmodifiable 변수 map을 고정한다. |

### Application Service

| 테스트 클래스 | 위치 | 검증 목적 |
|---|---|---|
| `EmailTemplateCatalogTest` | [`application/service/EmailTemplateCatalogTest.java`](../../../src/test/java/com/umc/product/notification/application/service/EmailTemplateCatalogTest.java) | 4 catalog의 exact required keys·subject·resource path, 길이 경계, blank/null/추가 key와 안전하지 않은 URL/origin 거부를 검증한다. |
| `EmailTemplatePropertiesBindingTest` | [`application/service/EmailTemplatePropertiesBindingTest.java`](../../../src/test/java/com/umc/product/notification/application/service/EmailTemplatePropertiesBindingTest.java) | 기본 서비스 origin과 allowlist binding/immutable list를 검증한다. |
| `SendEmailServiceTest` | [`application/service/SendEmailServiceTest.java`](../../../src/test/java/com/umc/product/notification/application/service/SendEmailServiceTest.java) | template-email request가 검증된 snapshot을 `publishOnce`에 전달하고 `NON_TRANSACTIONAL` event/응답을 반환하는지 검증한다. |
| `SendEmailServiceProxyTest` | [`application/service/SendEmailServiceProxyTest.java`](../../../src/test/java/com/umc/product/notification/application/service/SendEmailServiceProxyTest.java) | 실제 Spring proxy에서 template request가 호출 thread의 read-write transaction 안에서 publish되고, 기존 verification 발송이 `emailTaskExecutor`에서 호출 반환 후 비동기로 실행되는지 검증한다. |
| `TemplateEmailDispatchServiceTest` | [`application/service/TemplateEmailDispatchServiceTest.java`](../../../src/test/java/com/umc/product/notification/application/service/TemplateEmailDispatchServiceTest.java) | catalog subject/path로 Thymeleaf를 렌더링해 UTF-8 `EmailMessage`를 동기 전송하고 render/send 실패를 PII 없는 `EMAIL-*` 예외로 변환하는지 검증한다. |
| `TemplateEmailRenderingTest` | [`application/service/TemplateEmailRenderingTest.java`](../../../src/test/java/com/umc/product/notification/application/service/TemplateEmailRenderingTest.java) | 4 HTML preview를 실제 SpringTemplateEngine으로 생성하며 unresolved expression, `<img>`, 당근/tracking/external font, 미 escape 동적 값이 없는지와 1번 CTA만 존재하는지를 검증한다. |
| `FcmOutboxServiceTest` | [`application/service/FcmOutboxServiceTest.java`](../../../src/test/java/com/umc/product/notification/application/service/FcmOutboxServiceTest.java) | 기존 FCM outbox SUBSCRIBE/UNSUBSCRIBE 비활성화와 pending 없음/복수 pending 처리를 검증한다. |
| `FcmServiceTest` | [`application/service/FcmServiceTest.java`](../../../src/test/java/com/umc/product/notification/application/service/FcmServiceTest.java) | 신규·동일·다중 기기 FCM token 등록과 활성화를 검증한다. |
| `WebhookAlarmServiceTest` | [`application/service/WebhookAlarmServiceTest.java`](../../../src/test/java/com/umc/product/notification/application/service/WebhookAlarmServiceTest.java) | webhook alarm 요청이 대상 platform event로만 발행되고 직접 외부 전송하지 않는지 검증한다. |

### Domain / Event Adapter

| 테스트 클래스 | 위치 | 검증 목적 |
|---|---|---|
| `FcmOutboxEventTest` | [`domain/FcmOutboxEventTest.java`](../../../src/test/java/com/umc/product/notification/domain/FcmOutboxEventTest.java) | FCM event 생성 시 event ID·occurredAt·event type을 검증한다. |
| `TemplateEmailRequestedEventListenerTest` | [`adapter/in/event/TemplateEmailRequestedEventListenerTest.java`](../../../src/test/java/com/umc/product/notification/adapter/in/event/TemplateEmailRequestedEventListenerTest.java) | Spring event dispatch가 호출 thread에서 transaction 없이 동기 완료되고 listener 예외를 호출자에게 전파하는지 검증한다. |
| `WebhookAlarmEventListenerTest` | [`adapter/in/event/WebhookAlarmEventListenerTest.java`](../../../src/test/java/com/umc/product/notification/adapter/in/event/WebhookAlarmEventListenerTest.java) | webhook event를 `SendWebhookAlarmUseCase` command로 변환해 위임하는지 검증한다. |

### External Adapter

| 테스트 클래스 | 위치 | 검증 목적 |
|---|---|---|
| `SesEmailAdapterTest` | [`adapter/out/external/ses/SesEmailAdapterTest.java`](../../../src/test/java/com/umc/product/notification/adapter/out/external/ses/SesEmailAdapterTest.java) | SES v2 요청의 UTF-8 subject/body, from/to/configuration set과 provider 실패의 `EMAIL-0005`·PII-safe log를 검증한다. |
| `SesEmailConfigTest` | [`adapter/out/external/ses/SesEmailConfigTest.java`](../../../src/test/java/com/umc/product/notification/adapter/out/external/ses/SesEmailConfigTest.java) | 기본 `PT30S`/`PT10S` binding과 AWS client override, 0 이하·`attempt timeout > call timeout` 거부, `apiCallTimeout < PT5M` invariant를 검증한다. `@ParameterizedTest`로 `PT5M`과 `PT6M` startup 거부를 고정한다. |

### Integration

| 테스트 클래스 | 위치 | 검증 목적 |
|---|---|---|
| `TemplateEmailOutboxRelayIntegrationTest` | [`TemplateEmailOutboxRelayIntegrationTest.java`](../../../src/test/java/com/umc/product/notification/TemplateEmailOutboxRelayIntegrationTest.java) | scheduler를 끈 실제 context에서 due 요청은 SES 1회·`PUBLISHED`, future 요청은 미호출·`PENDING`, provider 실패는 `EMAIL-0005`/backoff/retry/최종 `FAILED`, 동일 ID는 중복 row 없이 dedupe, listener transaction inactive를 검증한다. |

## 실패 관찰 가능성·timeout 경계

- `TemplateEmailDispatchServiceTest`의 신규 template-email 렌더링·provider 실패 케이스는 raw cause를
  `null`로 제거하고 메시지에 지원자 PII가 남지 않는지 검증한다. 렌더링은 `EMAIL-0004`, provider
  전송은 `EMAIL-0005`로 표현된다. `SesEmailAdapterTest`는 provider 실패의 cause-less
  `EMAIL-0005`와 PII-safe log를 별도로 검증한다.
- 공용 relay 경계의 [`EventOutboxRelayFailureSanitizationJdbcIntegrationTest`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayFailureSanitizationJdbcIntegrationTest.java)는
  실제 PostgreSQL에서 provider business failure의 `EMAIL-0005`와 일반 runtime failure의 예외 class만
  `last_error`에 남기고 raw PII를 DB/log에 기록하지 않는지 검증한다. 이 범위에는 직접 error-span E2E
  테스트가 없으므로 해당 테스트가 있다고 전제하지 않는다.
- `EventOutboxRelayPolicy.PROCESSING_LEASE`는 `PT5M`이고 SES `apiCallTimeout`은 반드시 그보다 짧아야
  한다. 따라서 `PT5M`·`PT6M` 설정은 startup에서 거부되며, `apiCallAttemptTimeout`도 양수이고
  `apiCallTimeout` 이하이어야 한다.

## 기존 FCM·웹훅 회귀 범위

위 map에 포함한 `FcmOutboxServiceTest`, `FcmServiceTest`, `WebhookAlarmServiceTest`,
`FcmOutboxEventTest`, `WebhookAlarmEventListenerTest`는 신규 이메일 흐름과 독립적인 기존
알림 경계를 계속 고정한다. 이메일 문서/테스트는 Recruiting package를 import하지 않으며
`TemplateEmailDelivery`, `template_email_delivery`, `EmailDeliveryPoller`를 사용하지 않는다.
