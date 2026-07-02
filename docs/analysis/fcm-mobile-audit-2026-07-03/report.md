# FCM 모바일 친화성/보안/확장성 평가

## 결론

현재 FCM 알림 구조는 발송 요청 이벤트, audience 해석, 500개 단위 배치 발송, invalid token 비활성화까지 갖춰져 있어 서버 측 발송 파이프라인의 기본기는 양호하다. 단, durable queue로서의 `event_outbox` 장점은 `EVENT_OUTBOX_ENABLED=true` 환경에 한정된다. 기본 설정은 `src/main/resources/application.yml:324`에서 `enabled: ${EVENT_OUTBOX_ENABLED:false}`다.

다만 모바일 친화성은 "기본 알림 표시 + data 전달" 수준이다. Android/iOS가 FCM을 서로 다르게 해석하는 지점인 channel, click action/category, APNs badge/sound/content-available/mutable-content, priority/TTL/collapse key 같은 플랫폼별 설정이 서버 계약에 없다. 따라서 앱이 `data.deepLink`를 직접 파싱한다는 별도 클라이언트 계약 없이는 탭 이동, 포그라운드 처리, 백그라운드 처리의 일관성을 보장하기 어렵다.

## 모바일 친화성

근거:
- `FirebaseFcmMessageAdapter`는 `Notification.builder()`로 `title`/`body`를 설정하고 `imageUrl`이 있으면 `setImage`만 호출한다. 근거: `src/main/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapter.java:51`, `src/main/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapter.java:55`.
- 같은 어댑터는 `data` 맵을 복사한 뒤 `deepLink`를 `data.deepLink`로 넣고 `putAllData`를 호출한다. 근거: `src/main/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapter.java:63`, `src/main/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapter.java:65`, `src/main/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapter.java:68`.
- 실제 전송 DTO인 `FcmSendRequest`는 `targets`, `title`, `body`, `data`, `imageUrl`, `deepLink`만 가진다. Android channel, click action, APNs category/badge/sound, TTL, collapse key, priority 필드는 없다. 근거: `src/main/java/com/umc/product/notification/application/port/out/dto/FcmSendRequest.java:6`.
- 요청 이벤트와 배치 이벤트는 `data`, `imageUrl`, `deepLink`를 보존하지만 플랫폼별 해석 정책은 담지 않는다. 근거: `src/main/java/com/umc/product/notification/application/event/FcmNotificationRequestedEvent.java:24`, `src/main/java/com/umc/product/notification/application/event/FcmNotificationRequestedEvent.java:26`, `src/main/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEvent.java:16`, `src/main/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEvent.java:18`.
- Firebase 공식 문서상 notification message와 data message는 처리 방식이 다르며, Android background 수신은 notification payload가 시스템 트레이로 가고 data payload는 intent extra로 전달된다. 근거 URL: https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type, https://firebase.google.com/docs/cloud-messaging/android/receive-messages.
- Firebase 공식 문서는 플랫폼별 커스터마이징을 `android`/`apns` config로 분리하고, iOS silent update는 `content-available`을 요구한다. 근거 URL: https://firebase.google.com/docs/cloud-messaging/customize-messages/cross-platform, https://firebase.google.com/docs/cloud-messaging/ios/receive-messages.

판정:
- 단순 공지성 push는 가능하다.
- 모바일 앱에서 동일한 사용자 경험을 내려면 payload contract를 버전 관리해야 한다.
- `data`에는 최소 `type`, `schemaVersion`, `deepLink`, `notificationId/requestId`, `analyticsLabel`을 표준화하는 편이 좋다.
- `imageUrl`도 notification image만이 아니라 data에 중복 포함할지 앱팀과 명시적으로 결정해야 한다.

## 보안/남용 리스크

강점:
- 관리자 발송 API는 `@CheckAccess(ResourceType.FCM, WRITE)`와 audit annotation을 가진다. 근거: `src/main/java/com/umc/product/notification/adapter/in/web/FcmAdminController.java:35`, `src/main/java/com/umc/product/notification/adapter/in/web/FcmAdminController.java:38`, `src/main/java/com/umc/product/notification/adapter/in/web/FcmAdminController.java:39`.
- 토큰 등록/삭제는 `@CurrentMember`와 `@Valid` request를 사용한다. 근거: `src/main/java/com/umc/product/notification/adapter/in/web/FcmController.java:29`, `src/main/java/com/umc/product/notification/adapter/in/web/FcmController.java:31`, `src/main/java/com/umc/product/notification/adapter/in/web/FcmController.java:32`.
- Firebase credential은 설정 문자열에서 초기화되며 로그에는 credential 본문이 남지 않는다. 근거: `src/main/java/com/umc/product/global/config/FcmConfig.java:25`, `src/main/java/com/umc/product/global/config/FcmConfig.java:32`, `src/main/java/com/umc/product/global/config/FcmConfig.java:36`.
- 발송 결과에서 `UNREGISTERED`는 invalid token으로 반환되어 비활성화 경로로 연결된다. 근거: `src/main/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapter.java:86`, `src/main/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEventListener.java:69`, `src/main/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEventListener.java:77`.

Must-fix:
- 관리자 발송 target에는 "최소 하나의 target 필요", `memberIds` 최대 개수, target 조합 정책, 대량 발송 확인/승인 정책이 없다. `Target` 필드는 `memberIds`, `gisuId`, `chapterId`, `schoolId`, `parts`만 선언하고 검증 annotation이 없다. 근거: `src/main/java/com/umc/product/notification/adapter/in/web/dto/request/FcmAdminSendRequest.java:36`.
- `data` key/value는 길이 제한만 있고 Firebase reserved key, 민감정보, 내부 URL/deepLink scheme/domain allowlist 검증이 없다. 근거: `src/main/java/com/umc/product/notification/adapter/in/web/dto/request/FcmAdminSendRequest.java:48`, `src/main/java/com/umc/product/notification/adapter/in/web/dto/request/FcmAdminSendRequest.java:49`, `src/main/java/com/umc/product/notification/adapter/in/web/dto/request/FcmAdminSendRequest.java:50`. Firebase는 `from`, `gcm`, `google` 같은 reserved key와 민감 데이터 전송 주의를 문서화한다. 근거 URL: https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type.
- 인증 사용자 토큰 등록 abuse 방어가 약하다. `fcmToken`은 `@NotBlank`만 있고 길이 제한이 없다. 근거: `src/main/java/com/umc/product/notification/adapter/in/web/dto/request/FcmRegistrationRequest.java:8`, `src/main/java/com/umc/product/notification/adapter/in/web/dto/request/FcmRegistrationRequest.java:9`. 같은 회원의 새 토큰은 계속 저장될 수 있고 사용자별 활성 토큰 수 제한이 없다. 근거: `src/main/java/com/umc/product/notification/application/service/FcmService.java:26`, `src/main/java/com/umc/product/notification/application/service/FcmService.java:29`, `src/main/java/com/umc/product/notification/application/service/FcmService.java:35`.
- 신규/재등록 토큰은 `lastValidatedAt`이 등록 시각으로 설정되고, 기본 stale 기간은 30일이다. 유효하지 않은 과대/임의 토큰이 검증 대기에서 오래 빠질 수 있다. 근거: `src/main/java/com/umc/product/notification/domain/FcmToken.java:91`, `src/main/java/com/umc/product/notification/domain/FcmToken.java:97`, `src/main/resources/application.yml:336`.

Acceptable risks / 운영 주의:
- FCM token은 DB에 평문 TEXT로 저장된다. 즉시 취약점이라고 단정하긴 어렵지만, 유출 시 push 발송 대상 식별자로 재사용될 수 있으므로 최소 접근 통제/로그 마스킹/암호화 저장을 검토할 가치가 있다. 근거: `src/main/java/com/umc/product/notification/domain/FcmToken.java:31`.
- FCM enabled 환경에서 token validation scheduler 기본값이 true라 다중 인스턴스에서는 중복 검증이 발생할 수 있다. `.env.example`에는 batch instance만 true라고 설명되어 있으나 설정 기본값은 이를 강제하지 않는다. 근거: `src/main/resources/application.yml:333`, `.env.example:95`, `src/main/java/com/umc/product/notification/adapter/in/scheduler/FcmTokenValidationScheduler.java:16`, `src/main/java/com/umc/product/notification/adapter/in/scheduler/FcmTokenValidationScheduler.java:25`.
- FirebaseApp 초기화는 기존 앱 목록이 비어 있지 않고 default app이 없을 때 `firebaseApp`이 null로 남을 수 있다. 운영 부팅 리스크다. 근거: `src/main/java/com/umc/product/global/config/FcmConfig.java:38`, `src/main/java/com/umc/product/global/config/FcmConfig.java:41`, `src/main/java/com/umc/product/global/config/FcmConfig.java:62`.

## 확장성

강점:
- 요청 이벤트와 발송 배치 이벤트가 분리되어 있고, 실제 Firebase 호출은 outbound port 뒤에 있다. 근거: `src/main/java/com/umc/product/notification/application/event/FcmNotificationRequestedEvent.java:14`, `src/main/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEvent.java:11`, `src/main/java/com/umc/product/notification/application/port/out/SendFcmMessagePort.java:6`.
- `FcmAudienceResolver`와 `FcmSendBatchRequestedEventListener`가 분리되어 audience 해석과 전송 책임은 나뉘어 있다. 근거: `src/main/java/com/umc/product/notification/application/service/FcmAudienceResolver.java:30`, `src/main/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEventListener.java:35`, `src/main/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEventListener.java:49`.
- 공용 `event_outbox`를 durable queue로 쓰는 방향은 at-least-once delivery와 재시도 모델에 맞지만, 실제 durable 동작은 `EVENT_OUTBOX_ENABLED=true`일 때로 제한해 표현해야 한다. 근거: `src/main/resources/application.yml:324`.

제한:
- 타깃 모델이 `memberIds + gisu/chapter/school/parts`에 고정되어 있다. role, subscription tag, dynamic segment, user preference 기반 알림은 event/command/resolver를 함께 확장해야 한다. 근거: `src/main/java/com/umc/product/notification/application/event/FcmNotificationRequestedEvent.java:19`, `src/main/java/com/umc/product/notification/application/event/FcmNotificationRequestedEvent.java:20`, `src/main/java/com/umc/product/notification/application/event/FcmNotificationRequestedEvent.java:23`.
- `FcmAudienceResolver`는 gisu가 없으면 조직 필터를 해석하지 않고, gisu/chapter/school/part 순서의 UMC 조직 모델에 직접 묶여 있다. 근거: `src/main/java/com/umc/product/notification/application/service/FcmAudienceResolver.java:36`, `src/main/java/com/umc/product/notification/application/service/FcmAudienceResolver.java:41`, `src/main/java/com/umc/product/notification/application/service/FcmAudienceResolver.java:71`.
- 메시지 모델이 raw title/body/data 중심이라 template, localization, priority, channel, schedule, quiet hours, per-user preference 같은 기능을 넣을 표준 위치가 없다. 근거: `src/main/java/com/umc/product/notification/application/port/out/dto/FcmSendRequest.java:6`, `src/main/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEvent.java:16`.
- `SendFcmMessagePort` 결과가 aggregate 중심이라 per-token error taxonomy, retry class, dead-letter/reporting을 세밀하게 확장하기 어렵다. 근거: `src/main/java/com/umc/product/notification/application/port/out/dto/FcmSendResult.java:5`, `src/main/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapter.java:74`.

## 권장 개선 순서

1. `NotificationPayload` 또는 `PushMessage` 값 객체를 도입해 `type`, `schemaVersion`, `deepLink`, `imageUrl`, `platformOptions`, `collapseKey`, `priority`, `ttl`을 명시한다.
2. Android/iOS 전용 옵션을 `FcmSendRequest`에 추가하고 `FirebaseFcmMessageAdapter`에서 `AndroidConfig`/`ApnsConfig`를 구성한다.
3. 관리자 발송 target validation, 대량 발송 제한, deepLink/imageUrl allowlist, data reserved key denylist를 추가한다.
4. audience를 pluggable resolver 전략으로 분리해 새로운 대상 축을 event 계약 수정 없이 추가할 수 있게 한다.
5. 토큰 등록에는 token 길이 제한, 회원별 활성 토큰 수 제한, deviceId 기반 upsert 정책, invalid-format 즉시 dry-run 검증 또는 빠른 quarantine을 추가한다.
6. requestId/notificationId 기반 deduplication과 delivery metric/reporting을 표준화한다.

## Evidence

- `./evidence/C001-mobile-payload.txt`
- `./evidence/C002-security.txt`
- `./evidence/C003-extensibility.txt`
- `./evidence/code-review.md`
- `./evidence/manual-qa.md`
- `./evidence/final-gate-review.md`
- `./evidence/quality-gate.json`
- `./evidence/ulw-goals.json`
- cleanup: no runtime resources spawned; no server, tmux, browser, or container left running.
