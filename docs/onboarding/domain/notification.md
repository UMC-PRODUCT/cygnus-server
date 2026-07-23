# Notification Domain

## 역할

`notification` 도메인은 이메일 템플릿 렌더링·SES 발송, FCM 토큰/토픽·outbox, 웹훅 알림을
담당한다. 다른 도메인이 “메일을 보낼지”를 판단하고, Notification은 검증된 요청을 공용
event outbox에 기록해 전달한다.

## 책임과 경계

- 이메일 제목·template path·필수 변수 집합은 Notification-owned catalog가 결정한다.
- Thymeleaf 렌더링과 SES 호출은 `SendEmailPort` 뒤에서 수행한다.
- FCM 토큰/토픽, FCM outbox와 플랫폼별 웹훅 어댑터를 관리한다.
- 외부 제공자 장애는 PII 없는 `EmailDomainException`/도메인 예외로 표현하고 relay 재시도에
  연결한다.
- Recruiting 상태·지원자 조회·면접 일정 생성·메일 상태 갱신은 소유하지 않는다.

이 도메인에는 `TemplateEmailDelivery` 같은 이메일 전용 persistence entity, 이메일 전용 table,
또는 이메일 전용 poller가 없다. 요청·멱등·예약·retry·상태는 `global/event`의 공용
`event_outbox`가 책임진다.

## 템플릿 catalog와 변수 schema

호출자는 subject, HTML, template path를 직접 전달하지 않는다. `EmailTemplateCatalog`가 아래
네 타입의 subject, resource path, exact required keys를 소유하며 모든 값은 command 경계에서
trim/복사한 뒤 검증한다. 정의되지 않은 key, 누락·null·blank 값은 거부한다.

| `EmailTemplateType` | 필수 변수 | 기본 subject | template resource |
|---|---|---|---|
| `RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST` | `applicantName`, `contactSnapshot`, `actionUrl` | `[UMC] 서류 전형 합격 및 면접 가능 시간 제출 안내` | `email/recruitment/document-passed-interview-availability-request` |
| `RECRUITMENT_INTERVIEW_CONFIRMATION` | `applicantName`, `interviewDate`, `interviewTime`, `location`, `contactSnapshot` | `[UMC] 면접 일정이 확정되었습니다` | `email/recruitment/interview-confirmation` |
| `RECRUITMENT_FINAL_PASSED` | `applicantName`, `acceptedTrack` | `[UMC] 최종 합격을 축하드립니다` | `email/recruitment/final-passed` |
| `RECRUITMENT_FINAL_FAILED` | `applicantName` | `[UMC] 최종 전형 결과를 안내드립니다` | `email/recruitment/final-failed` |

수신자는 최대 254자의 단일 ASCII dot-atom mailbox만 허용한다. CR/LF·제어문자·내부 공백·주소
구분자·display-name·복수 `@`·비정상 local/domain label은 거부한다. Punycode domain은 허용하지만
quoted local part와 국제화 local part는 v1 범위 밖이다. 변수 한도는 `applicantName` 100,
`contactSnapshot` 2,000,
`actionUrl` 2,048, `interviewDate` 50, `interviewTime` 100, `location` 500,
`acceptedTrack` 100자다. 1번 템플릿만 `actionUrl` CTA(“면접 가능 시간 제출하기”)를 가지며,
4번은 내부 판정 사유를 받거나 표시하지 않는다.

## URL·HTML 정책

서비스 및 footer 홈페이지 origin은 다음 값으로 고정한다.

```text
https://university.neordinary.com
```

`actionUrl`은 absolute HTTP(S) URL이고 user-info가 없어야 하며, scheme·lowercase host·effective
port로 계산한 origin이 `app.notification.email.template.allowed-action-origins`에 정확히
포함되어야 한다. production 기본 allowlist는 위 HTTPS origin이고, test profile에는 검증을 위한
`http://localhost` origin만 명시적으로 추가한다. path/query/fragment는 허용하되 다른 host·port,
`javascript:` scheme은 거부한다. 개인정보 처리방침·서비스 이용약관 링크는 기존 verification
template의 검증된 Notion URL을 재사용한다.

템플릿은 30px 좌우 gutter, 620px max/300px min content table, 44px banner/주요 간격을 사용하고
320px에서는 gutter 16px와 `min-width:0` media rule로 가로 overflow를 막는다. title은
26px/38px/800, body는 16px, divider는 `#dcdee3` 0.5px, footer는 중앙 12px/18px을 사용한다.
banner row는 빈 상태를 유지하며 `<img>`/`src`, 당근 자산, tracking pixel, 외부 font를 추가하지
않는다. 동적 문자열은 `th:text`/`th:href`로 escape한다.

## 요청·relay·SES 실행 경계

호출 도메인은 `SendTemplateEmailCommand`에 stable `eventId`, 수신자, catalog type, 검증된
`Map<String, String>` variables, 최초 실행 시각 `availableAt`을 전달한다. `SendEmailService`의
`requestTemplateEmail()`은 `@Transactional` 안에서 catalog validation → immutable
`TemplateEmailRequestedEvent` snapshot → `publishOnce(event, availableAt)` 순서로 수행한다.
이 event는 `OutboxDispatchMode.NON_TRANSACTIONAL`이며 응답은 event ID, outbox status,
`deduplicated`, `availableAt`, `nextAttemptAt`만 포함한다.

relay가 due row를 claim한 뒤 `TemplateEmailRequestedEventListener`는 일반 `@EventListener`로
동기 실행되고 `DeliverTemplateEmailUseCase`/`TemplateEmailDispatchService`에 위임한다. 신규
listener와 dispatch method에는 `@Async`나 `@TransactionalEventListener`를 붙이지 않는다.
dispatch는 catalog subject/path로 Thymeleaf를 렌더링하고 `SendEmailPort`를 동기 호출한다.
SES 성공 시 정상 반환하여 outbox가 `PUBLISHED`가 되고, 실패 시 `EMAIL-0005` 등 안정적인
`EmailDomainException` code를 relay가 `last_error`에 기록해 retry/backoff를 적용한다. SES 외부
호출 중에는 DB transaction을 유지하지 않는다. 공용 relay의
`EventOutboxRelayPolicy.PROCESSING_LEASE`는 `PT5M`이며, SES 설정은
`0 < apiCallAttemptTimeout <= apiCallTimeout <= PT4M30S` invariant를 따른다. 따라서 processing
lease 완료 전에 최소 30초를 확보하며 `apiCallTimeout=PT4M31S` 이상은 configuration startup에서
거부된다.

신규 template-email dispatch의 Thymeleaf 렌더링/provider 경계에서 발생한 raw cause는
`EMAIL-0004`(render) 또는 `EMAIL-0005`(send)의 cause-less `EmailDomainException`으로 변환한다.
이 신규 동기 경로의 메시지·로그·outbox `last_error`에는 원문 PII나 외부 provider cause를 남기지
않고 stable code만 전달한다. 이는 기존 verification async 경로의 전체 오류 처리 계약을
일반화하거나 변경하는 설명이 아니다.

SES throttling·HTTP 5xx와 상태를 판별할 수 없는 client/runtime 오류는 retryable이다. 그 밖의
명확한 HTTP 4xx는 non-retryable이며 첫 실패에서 attempts를 증가시키고 즉시 `FAILED`로 전환한다.
알 수 없는 listener `RuntimeException`은 기존 호환성을 위해 retryable이다. 어느 경우든 외부 cause는
dispatch 경계에서 제거하고 `EMAIL-0005`와 retryable 여부만 공용 relay에 전달한다.

template-email outbox payload에는 수신자와 template 변수가 발송 복원을 위해 일시 저장된다.
`PUBLISHED`는 24시간, `FAILED`는 30일 후 `payload='{}'`와 `traceparent=NULL`로 정리하며 event ID,
fingerprint, 상태·시각·안정적인 `last_error` tombstone은 영구 보존한다. 이메일 주소와 변수는 로그,
span error, `last_error`, 상태 응답에는 노출하지 않는다.
신규 relay writer는 검증된 값을 `sanitized_last_error`에 같은 값으로 함께 기록한다. 이전 버전의
자유 형식 `last_error`와 rolling deploy 중 구버전 writer가 나중에 덮어쓴 값은 사본과 일치하지
않으므로 상태 응답에서 숨기고 terminal payload 정리와 함께 두 값을 `NULL`로 바꾼다.

기존 인증 메일 `SendEmailService.sendVerificationEmail()`의 `@Async("emailTaskExecutor")`와
Authentication event 경로는 그대로 유지한다. 신규 template-email 동기 경로를 추가한 것이며
기존 verification async 발송을 이관하거나 의미를 바꾸지 않는다.

## 후속 Recruiting 연동 계약

후속 Recruiting PR만 Notification 공개 UseCase를 호출한다. Recruiting은 재시도에도 같은
stable UUID를 `eventId`로 사용하고, 아래 `type`과 exact `vars`를 구성하며 결과 공개 시각을
`availableAt`에 매핑한다.

| 메일 | `type` | 호출자가 준비할 값 |
|---|---|---|
| 서류 합격·면접 가능 시간 요청 | `RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST` | stable ID, 이름, 문의 snapshot, 허용 origin action URL, 결과 공개 시각 |
| 면접 일정 확정 | `RECRUITMENT_INTERVIEW_CONFIRMATION` | stable ID, 이름, 표시용 날짜·시간·장소, 문의 snapshot, 즉시 시각 |
| 최종 합격 | `RECRUITMENT_FINAL_PASSED` | stable ID, 이름, 표시용 합격 트랙, 결과 공개 시각 |
| 최종 불합격 | `RECRUITMENT_FINAL_FAILED` | stable ID, 이름, 결과 공개 시각 |

Notification은 값의 출처나 Recruiting 상태, ID 생성 규칙을 해석하지 않으며 Recruiting package,
entity, repository, event를 참조하지 않는다. 후속 연동은 ID/type/vars/availableAt라는 공개
계약만 사용한다.

## FCM·웹훅 및 UX writing

FCM 토큰 소유권은 신뢰된 member context에 묶고, 외부 provider 요청은 adapter를 통해 보낸다.
웹훅 URL·토큰·provider credential·raw payload는 로그에 남기지 않는다. `FCM`, `웹훅 어댑터`는
운영자 설정 화면 외에는 풀어 쓰고, 사용자 메시지는 `푸시 알림을 보내지 못했어요. 잠시 후
다시 시도해주세요`처럼 복구 행동 중심으로 안내한다.
