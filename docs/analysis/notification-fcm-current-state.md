# notification 도메인 — FCM 알림 발송 및 토큰 관리 현황 보고서

작성일: 2026-05-07
작성 범위: `com.umc.product.notification` 패키지 + 관련 global 설정 + Flyway 마이그레이션

> 이 문서는 결정 기록(ADR)이 아니라 **현재 구현 상태에 대한 분석 보고서**다. 후속 변경 결정이 발생하면 그 시점에 별도 ADR을 작성한다.
>
> 2026-07-23 참고: 이 문서는 ADR-020 적용 전 상태의 역사적 분석이다. deprecated FCM Outbox/Topic 코드와 endpoint는 ADR-020 구현으로 제거되었다.

---

## 1. 한눈에 보기

```
[Client(앱)] ──PUT /api/v1/notification/fcm/token──> [FcmController]
                                                       └─> FcmService.registerFcmToken
                                                              └─> LoadFcmPort / SaveFcmPort

[다른 도메인 서비스]
        │
        ▼
[SendNotificationToAudienceUseCase]
        │ (FcmAudienceService 구현)
        ▼
   ┌─────────────────────────────────────────────┐
   │ 1) NoticeTargetInfo → memberIds 해석         │   (cross-domain UseCase 호출)
   │ 2) memberIds → 활성 FcmToken 조회 (LoadFcmPort)│
   │ 3) 500개씩 청크 → FirebaseMessaging.sendEachForMulticast │  ← 외부 SDK 직접 호출
   │ 4) UNREGISTERED 응답 토큰 → FcmTokenDeactivator.deactivate │
   └─────────────────────────────────────────────┘

[Deprecated 영역]
   FcmOutbox / FcmOutboxService / FcmOutboxScheduler / FcmOutboxEventListener
   FcmTopicService / ManageFcmTopicUseCase                  ← 모두 no-op + warn 로그
```

핵심 사실 5줄 요약:

1. **알림 모델은 "토픽 구독" → "토큰 직접 발송"으로 이미 전환 완료.** Outbox / Topic 코드는 현재 사실상 dead path이며 `@Deprecated(forRemoval = true)`.
2. **FCM 발송은 [FcmAudienceService](../../src/main/java/com/umc/product/notification/application/service/FcmAudienceService.java)가 `FirebaseMessaging`을 직접 의존**해 호출한다. 헥사고날 outbound port가 없다.
3. **토큰은 `(member_id, fcm_token)` 단위 멀티 디바이스**를 허용하며, 비활성은 `is_active = false` 플래그로 soft 처리.
4. **만료/무효 토큰 정리는 발송 시점 inline**으로만 일어난다(`MessagingErrorCode.UNREGISTERED` 응답을 만난 토큰만). 별도 청소 스케줄러는 없다.
5. **`FcmProperties.enabled` 한 토글로 모든 발송이 글로벌 비활성화 가능.** 로컬/테스트에서 외부 호출 차단 용도.

---

## 2. 토큰 데이터 모델

### 2.1 [FcmToken](../../src/main/java/com/umc/product/notification/domain/FcmToken.java) 엔티티

| 컬럼                          | 타입                            | 비고                        |
|-----------------------------|-------------------------------|---------------------------|
| `id`                        | BIGSERIAL                     | PK                        |
| `member_id`                 | BIGINT                        | 회원 ID 참조(다른 도메인이라 ID 참조만) |
| `fcm_token`                 | TEXT                          | FCM registration token    |
| `is_active`                 | BOOLEAN NOT NULL DEFAULT TRUE | soft 활성/비활성               |
| `created_at` / `updated_at` | TIMESTAMP                     | `BaseEntity`              |

도메인 메서드는 `activate()` / `deactivate()` 두 개. `isActive`는 record 단위 토글로만 변경된다.

### 2.2 멀티 디바이스 지원 이력 (마이그레이션)

| 시점         | 마이그레이션                                                 | 변경                                                                                       |
|------------|--------------------------------------------------------|------------------------------------------------------------------------------------------|
| 2026-03-05 | `V2026.03.05.06.30__fcm_token_member_id_delete_fk.sql` | member_id FK 제거                                                                          |
| 2026-03-06 | `V2026.03.06.00.00__create_fcm_outbox.sql`             | (구) outbox 테이블 생성                                                                        |
| 2026-03-17 | `V2026.03.17.00.00__create_fcm_token_topic.sql`        | (구) 토픽 매핑 테이블 생성                                                                         |
| 2026-04-18 | `V2026.04.18.00.00__alter_fcm_token_multi_device.sql`  | `is_active` 컬럼, `(member_id, fcm_token)` partial UNIQUE, `(member_id, is_active)` 인덱스 추가 |
| 2026-04-23 | `V2026.04.23.23.50__drop_fcm_token_topic.sql`          | 토픽 매핑 테이블 drop                                                                           |
| 2026-04-24 | `V2026.04.24.19.15__drop_fcm_token_member_id_uk.sql`   | `fcm_token_member_id_key` UK 제거 (1:N 허용)                                                 |

→ "한 회원당 한 토큰" 모델에서 "회원 1명이 여러 기기 보유" 모델로 두 차례 마이그레이션을 거쳐 정착됨.

---

## 3. 토큰 등록 흐름

### 3.1 진입점 — [FcmController](../../src/main/java/com/umc/product/notification/adapter/in/web/FcmController.java)

```http
PUT /api/v1/notification/fcm/token
Authorization: Bearer <jwt>
Content-Type: application/json

{ "fcmToken": "..." }
```

- 인증된 사용자만 호출(`@CurrentMember MemberPrincipal`).
- Request DTO는 [FcmRegistrationRequest(@NotBlank fcmToken)](../../src/main/java/com/umc/product/notification/adapter/in/web/dto/request/FcmRegistrationRequest.java) — 단순 record + `@NotBlank`.

### 3.2 처리 — [FcmService.registerFcmToken](../../src/main/java/com/umc/product/notification/application/service/FcmService.java)

```java
loadFcmPort.findByMemberIdAndToken(memberId, request.fcmToken())
    .ifPresentOrElse(
        FcmToken::activate,                              // 동일 (member, token) 존재 → 재활성
        () -> saveFcmPort.save(FcmToken.create(...))     // 없으면 신규 발급
    );
```

- "재로그인 등으로 deactivate된 토큰을 다시 켤 때" 경로가 자연스럽게 처리된다.
- **삭제 API는 노출되지 않음.** `FcmController`에는 `PUT /token`과 deprecated topic legacy 두 endpoint만 존재한다.
- 트랜잭션 어노테이션은 `jakarta.transaction.Transactional`. (다른 곳은 대부분 `org.springframework.transaction.annotation.Transactional` — 일관성 차이는 작은 P3 이슈.)

### 3.3 영속화 — [FcmPersistenceAdapter](../../src/main/java/com/umc/product/notification/adapter/out/persistentce/FcmPersistenceAdapter.java)

`LoadFcmPort` + `SaveFcmPort`를 한 어댑터에서 동시에 구현. JPA 메서드 컨벤션은 표준 derivation:

- `findByMemberIdAndFcmToken(...)`
- `findAllByMemberIdAndIsActiveTrue(...)`
- `findAllByMemberIdInAndIsActiveTrue(...)`

> 패키지명 오타: `adapter/out/persistentce`(`persistence`가 아닌 철자). 다른 도메인은 `persistence`를 사용 — P3 정정 후보.

---

## 4. 알림 발송 흐름

### 4.1 인바운드 포트 — [SendNotificationToAudienceUseCase](../../src/main/java/com/umc/product/notification/application/port/in/SendNotificationToAudienceUseCase.java)

세 가지 진입점:

| 메서드                                                | 입력                              | 용도                       |
|----------------------------------------------------|---------------------------------|--------------------------|
| `sendToAudience(AudienceNotificationCommand)`      | `NoticeTargetInfo` + title/body | 공지 대상 해석 후 전체 일괄 발송      |
| `sendToMember(NotificationCommand)`                | `memberId` + title/body         | 단일 회원                    |
| `sendToMembers(List<Long> memberIds, title, body)` | 명시적 회원 리스트                      | bulk 발송(예: remindNotice) |

세 메서드 모두 동기 호출. 외부 호출자는 자기 트랜잭션 안에서 호출하는 형태.

### 4.2 구현 — [FcmAudienceService](../../src/main/java/com/umc/product/notification/application/service/FcmAudienceService.java)

핵심 단계:

1. **글로벌 토글 체크**: `fcmProperties.enabled() == false`이면 즉시 return.
2. **대상자 해석**(audience만): `NoticeTargetInfo.targetGisuId` 기준으로 챌린저 → 멤버 → school → chapter 매핑을 거쳐 최종 `memberIds` 산출. cross-domain UseCase 세 개를 호출:
    - `GetChallengerUseCase.getAllByGisuId(...)`
    - `GetMemberUseCase.findAllSchoolIdsByIds(...)`
    - `GetChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(...)`
    - 그 결과를 `NoticeTargetInfo.isTarget(gisuId, chapterId, schoolId, part)`로 필터.
3. **활성 토큰 일괄 조회**: `loadFcmPort.findAllActiveByMemberIds(memberIds)` 한 번. (N+1 방어)
4. **500건 단위 배치 발송**: `FirebaseMessaging.sendEachForMulticast(MulticastMessage)`.
5. **무효 토큰 비활성화**: 응답에서 `MessagingErrorCode.UNREGISTERED`인 항목은 [FcmTokenDeactivator](../../src/main/java/com/umc/product/notification/application/service/FcmTokenDeactivator.java)로 `is_active=false` 처리. `@Transactional(propagation = REQUIRES_NEW)`로 호출 트랜잭션과 분리.

#### 발송 코드 발췌

```java
private void sendBatch(List<FcmToken> tokens, String title, String body) {
    Notification notification = Notification.builder().setTitle(title).setBody(body).build();
    for (List<FcmToken> batch : partition(tokens, FCM_MULTICAST_BATCH_SIZE)) {
        List<String> tokenStrings = batch.stream().map(FcmToken::getFcmToken).toList();
        try {
            MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokenStrings)
                .setNotification(notification)
                .build();
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            fcmTokenDeactivator.deactivateInvalidTokens(batch, response.getResponses());
        } catch (FirebaseMessagingException e) {
            log.error("FCM 배치 발송 실패 batchSize={}", batch.size(), e);
        }
    }
}
```

→ FCM 한 호출당 multicast 한도(500개)에 맞춘 청킹이 적용되어 있다. 500개 이내면 한 번, 1000개면 두 번 호출.

### 4.3 무효 토큰 정리 — [FcmTokenDeactivator](../../src/main/java/com/umc/product/notification/application/service/FcmTokenDeactivator.java)

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void deactivateInvalidTokens(List<FcmToken> tokens, List<SendResponse> responses) {
    for (int i = 0; i < responses.size(); i++) {
        SendResponse response = responses.get(i);
        if (!response.isSuccessful()
            && response.getException() != null
            && MessagingErrorCode.UNREGISTERED.equals(response.getException().getMessagingErrorCode())) {
            FcmToken token = tokens.get(i);
            token.deactivate();
            saveFcmPort.save(token);
        }
    }
}
```

다음 사항이 명시적이다.

- **`UNREGISTERED`만 비활성화한다.** `INVALID_ARGUMENT`, `SENDER_ID_MISMATCH`, `THIRD_PARTY_AUTH_ERROR` 등 다른 영구 오류는 비활성 처리하지 않고 로그만 남긴다.
- **REQUIRES_NEW 트랜잭션**이라 호출 측에서 발송 트랜잭션이 롤백돼도 비활성 처리는 보존된다.
- **별도 정기 청소 스케줄러는 없다.** 즉 deactivate가 발생하려면 누군가에게 한 번 발송이 시도되어야 한다. 사용 빈도가 낮은 회원의 만료 토큰은 long-tail로 DB에 누적된다.

---

## 5. Deprecated 영역 — 토픽 / Outbox

### 5.1 토픽 기반 발송 (Deprecated)

- [ManageFcmTopicUseCase](../../src/main/java/com/umc/product/notification/application/port/in/ManageFcmTopicUseCase.java) 인터페이스 전체에 `@Deprecated(since = "v1.5.0", forRemoval = true)` 붙음.
- 구현체 [FcmTopicService](../../src/main/java/com/umc/product/notification/application/service/FcmTopicService.java)의 모든 메서드는 **`log.warn("[DEPRECATED] ...")`만 남기고 no-op**. 즉 실제 Firebase 토픽 호출은 일어나지 않는다.
- 컨트롤러에는 여전히 두 endpoint가 살아 있다(`DELETE /topics/legacy` 등). 호출되어도 로그 한 줄만 남고 끝.

### 5.2 Outbox 패턴 (Deprecated)

- [FcmOutbox](../../src/main/java/com/umc/product/notification/domain/FcmOutbox.java)는 `FCM_SUBSCRIBE` / `FCM_UNSUBSCRIBE` 두 가지 이벤트 타입과 `PENDING/PROCESSED/FAILED` 상태, retry count(`MAX_RETRY = 3`)를 갖는 풀 스펙 outbox 엔티티다.
- 처리 서비스 [FcmOutboxService](../../src/main/java/com/umc/product/notification/application/service/FcmOutboxService.java)는 토픽 전환에 따라 다음 로직만 남았다:

  ```java
  log.warn("[DEPRECATED] FCM Outbox 토픽 이벤트 {} 건이 남아있어 FAILED 처리합니다. ...");
  for (FcmOutbox event : pendingEvents) {
      event.markFailed();
      saveFcmOutboxPort.save(event);
  }
  ```

  즉 **PENDING이 발견되면 즉시 FAILED로 마킹해 재처리 루프를 끊는 stub**다.

- 트리거는 두 곳:
    - [FcmOutboxScheduler](../../src/main/java/com/umc/product/notification/adapter/in/scheduler/FcmOutboxScheduler.java): `@Scheduled(fixedRateString = "${app.fcm.outbox-interval-ms}")`.
    - [FcmOutboxEventListener](../../src/main/java/com/umc/product/notification/adapter/in/event/FcmOutboxEventListener.java): `@TransactionalEventListener(AFTER_COMMIT) + @Async`로 [FcmOutboxEvent](../../src/main/java/com/umc/product/notification/domain/FcmOutboxEvent.java)(빈 record) 발행 시 즉시 처리.
- `FcmOutboxEvent`는 **빈 record**로 정의되어 있으며, 코드베이스 어디에서도 `publishEvent(new FcmOutboxEvent())`를 부르지 않는 상태로 보인다. 즉 listener는 호출되지 않는다.

### 5.3 정리 우선순위

세 layer가 동시에 dead path에 가깝다.

1. `ManageFcmTopicUseCase` / `FcmTopicService`
2. `FcmOutbox*` (도메인 / 서비스 / 스케줄러 / 이벤트 / 마이그레이션)
3. 컨트롤러 endpoint(`DELETE /topics/legacy`, `resubscribeAllMemberLegacyTopics`)

→ 향후 정리 결정이 합의되면 ADR로 별도 기록할 가치가 있는 영역.

---

## 6. 외부 통합 / 설정

### 6.1 [FcmConfig](../../src/main/java/com/umc/product/global/config/FcmConfig.java)

`@Value("${app.fcm.firebase-configuration}")`로 service account JSON **문자열 통째**를 받아 `GoogleCredentials.fromStream`으로 파싱한 뒤 `FirebaseApp.initializeApp(options)` → `FirebaseMessaging` 빈을 등록한다. 기존 `FirebaseApp.DEFAULT_APP_NAME`이 있으면 그것을 재사용한다(테스트 환경에서 다중 초기화 방지).

> service account JSON은 secret이다. application property에 평문으로 들어가면 노출 위험. KMS 또는 Jasypt 적용 여부 확인 필요(현 시점 코드만으로는 판단 불가).

### 6.2 [FcmProperties](../../src/main/java/com/umc/product/global/config/FcmProperties.java)

```java
@ConfigurationProperties(prefix = "app.fcm")
public record FcmProperties(boolean enabled) {}
```

- `app.fcm.enabled` 단일 토글로 글로벌 발송 차단.
- `app.fcm.firebase-configuration`(JSON), `app.fcm.outbox-interval-ms`(스케줄)도 사용되지만 record에는 정의되어 있지 않다(`@Value`/`@Scheduled` 직접 주입). → record 확장으로 정합 가능 (P3).

---

## 7. 권한 정책

[FcmPermissionEvaluator](../../src/main/java/com/umc/product/notification/application/service/evaluator/FcmPermissionEvaluator.java):

```java
if (resourcePermission.permission().equals(PermissionType.DELETE)) {
    return subjectAttributes.roleAttributes().stream()
        .anyMatch(roleAttribute -> roleAttribute.roleType().isAtLeastCentralCore());
}
return false;
```

- `ResourceType.FCM`에 대해 **DELETE 권한은 중앙운영사무국 총괄단 이상**만 허용.
- 다른 권한 타입(READ/UPDATE 등)은 모두 false 반환 — 즉 admin API 자체가 없거나, 컨트롤러에서는 `@PreAuthorize` 등으로 별도 처리되는 형태.
- 현재 컨트롤러 코드에 DELETE endpoint는 노출되지 않음. 즉 evaluator는 미래 admin API 확장을 대비한 자리.

---

## 8. 데이터 모델 / 검증 규칙 정리

### `fcm_token`

- PK `id`
- `member_id` BIGINT (FK 제거됨; ID 참조만)
- `fcm_token` TEXT
- `is_active` BOOLEAN NOT NULL DEFAULT TRUE
- 인덱스:
    - `uix_fcm_token_member_token`: `(member_id, fcm_token)` partial UNIQUE WHERE `member_id IS NOT NULL` (동일 기기 중복 등록 방지)
    - `ix_fcm_token_member_id_active`: `(member_id, is_active)` (활성 토큰 조회 성능)
- BaseEntity audit 컬럼

### `fcm_outbox` (Deprecated 사용 중)

- PK `id`
- `member_id` BIGINT NOT NULL
- `event_type` VARCHAR (`FCM_SUBSCRIBE` / `FCM_UNSUBSCRIBE`)
- `payload` (UNSUBSCRIBE 시 oldToken)
- `status` VARCHAR (`PENDING`/`PROCESSED`/`FAILED`)
- `retry_count` INT (MAX 3)
- `processed_at` TIMESTAMP

---

## 9. 헥사고날 / 도메인 정합성 평가

| 항목                                   | 상태       | 비고                                                                                                              |
|--------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------|
| `domain` → `application/adapter` 역의존 | ✅ 없음     | `FcmToken`, `FcmOutbox`는 순수 JPA 엔티티                                                                             |
| `adapter/in` → `adapter/out` 직접 의존   | ✅ 없음     | 컨트롤러는 UseCase만 의존                                                                                               |
| `application/service` → 외부 SDK 직접 의존 | ❌ **있음** | `FcmAudienceService`, `FcmTokenDeactivator`가 `FirebaseMessaging`/`SendResponse`/`MessagingErrorCode`를 직접 import |
| Cross-domain 호출 = UseCase 경유         | ✅ 준수     | `GetChallengerUseCase`, `GetMemberUseCase`, `GetChapterUseCase`                                                 |
| Cross-aggregate 참조 = ID              | ✅ 준수     | `FcmToken.memberId(Long)`, `FcmOutbox.memberId(Long)`                                                           |
| 컨트롤러가 Entity 노출                      | ✅ 미노출    | 등록 API는 void 반환                                                                                                 |
| Read 메서드 시맨틱(`get`/`find`/`list`)    | ⚠️ 부분 준수 | `LoadFcmPort.findAllActiveBy*`는 `list*`가 더 의미상 맞음(0건 시 빈 List 반환)                                               |
| 트랜잭션 어노테이션 일관성                       | ⚠️ 혼용    | `FcmService`만 `jakarta.transaction.Transactional`, 다른 곳은 Spring 어노테이션                                           |

→ **외부 시스템 통합을 outbound port로 추상화하지 않은 점이 가장 큰 헥사고날 위반**. 정상 구조라면:

```
SendFcmMessagePort (port/out, interface)
DeactivateFcmTokenPort 또는 SendResult 추상 record
FirebaseMessagingAdapter (adapter/out/external) implements SendFcmMessagePort
FcmAudienceService → SendFcmMessagePort에만 의존
```

이 형태가 ADR-003/004에서 외부 시스템 통합에 적용한 패턴(`SendWebhookPort`, `IssueInstallationTokenPort`)과 일관된다.

---

## 10. 관찰된 위반·개선 후보 (P1~P5)

CLAUDE.md의 review priority 분류 기준.

### P1 (Critical)

- 없음. (보안 사고로 직결되거나 데이터 손실을 일으키는 항목은 본 분석 시점 기준 미발견.)

### P2 (Significant — 아키텍처 / 성능 / 확장성)

- **외부 SDK 직접 의존 (헥사고날 위반)**. 위 9절 참조. 인프라 교체(Firebase → AWS SNS 등) 또는 테스트(Stubbing) 비용을 키운다.
- **만료 토큰 정리가 발송 시점 inline에만 의존**. 발송 빈도가 낮은 회원의 무효 토큰은 영구 잔존. 정기 청소 잡(예: `MessagingErrorCode.UNREGISTERED`로 한번이라도 비활성된 토큰을 N개월 뒤 hard delete) 부재.
- **NoticeTargetInfo 해석 단계에서 cross-domain UseCase 3개를 동시 호출**. 챌린저가 많아질수록(수만 명 단위) `findAllSchoolIdsByIds` / `getChapterMapByGisuIdsAndSchoolIds`에서 메모리·쿼리 비용이 증가한다. 현재 챌린저 모집 규모 기준으로는 문제가 안 되지만, 발송 대상 확대 시 지점 모니터링 필요.
- **`FirebaseMessagingException`을 잡으면 그 batch 전체를 fail로 카운트하고 그대로 진행**. 401/quota exceeded 등의 일시적 오류와 영구 오류를 구분하지 않는다. retry 정책 없음.

### P3 (Code Quality)

- 패키지 오타 `adapter/out/persistentce` → `persistence`.
- `FcmService`만 `jakarta.transaction.Transactional`. 프로젝트 표준은 Spring 어노테이션.
- `LoadFcmPort.findAllActiveBy*` 시맨틱은 `listActiveBy*`로 바꾸는 것이 CLAUDE.md 컨벤션과 일치.
- `FcmProperties`는 `enabled` 한 필드만 정의. `firebase-configuration`, `outbox-interval-ms`도 record에 흡수 가능.
- Dead 코드(`FcmTopicService`, `FcmOutboxService`, `FcmOutboxEvent`, `FcmOutboxEventListener`, `FcmOutboxScheduler`, deprecated controller endpoints, `fcm_outbox` 테이블) 일괄 제거 후보. 단 제거 결정은 별도 ADR로 합의가 필요.

### P4 (Alternative)

- `FcmAudienceService`가 도메인 해석(`resolveTargetMemberIds`)과 발송(`sendBatch`)을 동시에 책임진다. `NoticeAudienceResolver` 같은 별도 컴포넌트로 분리하면 단위 테스트 용이성과 재사용성이 올라간다(현재도 단점이 큰 정도는 아님).
- `FcmController`가 deprecated topic endpoint와 active token endpoint를 한곳에 묶고 있음. 분리 가능.

### P5 (Minor)

- `FcmOutbox.MAX_RETRY = 3`은 dead code 안의 상수. 정리될 때 함께 사라진다.
- 로그 레벨 일부 혼재(`log.info`로 비활성 안내 / `log.warn`으로 deprecated 안내). 운영 alert 룰에 따라 통일 필요할 수 있음.

---

## 11. 미해결 / 후속 결정 후보

다음 항목은 ADR 후보다(현 보고서는 결정하지 않는다).

1. **Firebase SDK를 outbound port로 추상화할지** — 결정 시 `SendFcmMessagePort` 신설 + `FcmAudienceService` 리팩토링 PR로 이어진다.
2. **Deprecated topic / outbox 코드의 일괄 제거 시점과 마이그레이션 전략** — `fcm_outbox` 테이블 drop 마이그레이션 + 컨트롤러 endpoint 제거 + `ManageFcmTopicUseCase`/구현체/이벤트 record 삭제. 외부 클라이언트가 deprecated endpoint를 여전히 호출 중인지 사전 확인 필요.
3. **만료 토큰 정기 정리 정책** — 비활성 후 N일 경과 토큰 hard delete? 또는 비활성 그대로 유지?
4. **Firebase 발송 실패 시 재시도 정책** — 어떤 에러 코드는 재시도, 어떤 에러는 즉시 영구 실패로 분류할지 정의가 없다.
5. **Service account credentials 보관 방식** — KMS / Jasypt / plain env 중 무엇으로 운영 중인지 확인 + 회전 절차 정립.

---

## 12. 빠른 참조 — 코드 위치

| 책임                      | 파일                                                                                                                                                                                                                                                 |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 토큰 등록 컨트롤러              | [FcmController](../../src/main/java/com/umc/product/notification/adapter/in/web/FcmController.java)                                                                                                                                                |
| 토큰 등록 서비스               | [FcmService](../../src/main/java/com/umc/product/notification/application/service/FcmService.java)                                                                                                                                                 |
| 토큰 도메인                  | [FcmToken](../../src/main/java/com/umc/product/notification/domain/FcmToken.java)                                                                                                                                                                  |
| 토큰 영속화                  | [FcmPersistenceAdapter](../../src/main/java/com/umc/product/notification/adapter/out/persistentce/FcmPersistenceAdapter.java), [FcmJpaRepository](../../src/main/java/com/umc/product/notification/adapter/out/persistentce/FcmJpaRepository.java) |
| 알림 발송 진입점               | [SendNotificationToAudienceUseCase](../../src/main/java/com/umc/product/notification/application/port/in/SendNotificationToAudienceUseCase.java)                                                                                                   |
| 알림 발송 구현                | [FcmAudienceService](../../src/main/java/com/umc/product/notification/application/service/FcmAudienceService.java)                                                                                                                                 |
| 무효 토큰 비활성화              | [FcmTokenDeactivator](../../src/main/java/com/umc/product/notification/application/service/FcmTokenDeactivator.java)                                                                                                                               |
| Firebase Admin SDK 설정   | [FcmConfig](../../src/main/java/com/umc/product/global/config/FcmConfig.java)                                                                                                                                                                      |
| 글로벌 토글                  | [FcmProperties](../../src/main/java/com/umc/product/global/config/FcmProperties.java)                                                                                                                                                              |
| 권한 평가기                  | [FcmPermissionEvaluator](../../src/main/java/com/umc/product/notification/application/service/evaluator/FcmPermissionEvaluator.java)                                                                                                               |
| (Deprecated) 토픽 서비스     | [FcmTopicService](../../src/main/java/com/umc/product/notification/application/service/FcmTopicService.java)                                                                                                                                       |
| (Deprecated) Outbox 서비스 | [FcmOutboxService](../../src/main/java/com/umc/product/notification/application/service/FcmOutboxService.java)                                                                                                                                     |
| (Deprecated) Outbox 도메인 | [FcmOutbox](../../src/main/java/com/umc/product/notification/domain/FcmOutbox.java)                                                                                                                                                                |
