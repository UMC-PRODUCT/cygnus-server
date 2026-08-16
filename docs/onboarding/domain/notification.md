# Notification Domain

## 역할

`notification` 도메인은 이메일, FCM 푸시 알림, 웹훅 발송을 담당한다. 다른 도메인의 이벤트를 사용자나 외부 채널로 전달한다.

## 책임

- 이메일 템플릿을 렌더링하고 발송한다.
- FCM 토큰과 푸시 알림 전송을 관리한다.
- 플랫폼별 웹훅 어댑터를 선택하고 메시지를 보낸다.
- 발송 실패와 요청 제한을 도메인 예외로 표현한다.

## 경계

notification은 메시지를 전달할 뿐, 메시지를 보내야 하는 업무 판단은 호출한 도메인이 한다. 외부 제공자 장애나 설정 누락은 사용자에게 복구 행동을 중심으로 안내한다.

## FCM installation 등록

FCM 등록은 물리적 기기가 아니라 앱 설치 인스턴스인 `installationId`를 기준으로 관리한다. 모바일 앱은 Firebase Installation ID처럼 재설치 시 교체 가능한 opaque identifier를 사용하며, 하드웨어 식별자를 보내지 않는다.

```http
POST /api/v1/notifications/fcm/installations
Content-Type: application/json

{
  "installationId": "firebase-installation-id",
  "fcmToken": "firebase-registration-token",
  "platform": "IOS",
  "appVersion": "1.0.0"
}
```

로그아웃할 때는 인증된 회원이 소유한 installation만 해제한다.

```http
DELETE /api/v1/notifications/fcm/installations/{installationId}
```

등록 상태는 다음 규칙을 따른다.

- 한 회원은 서로 다른 `installationId`를 여러 개 등록할 수 있다.
- 하나의 `installationId`에는 현재 `memberId`와 `fcmToken` 하나만 저장한다.
- 같은 installation에서 FCM token이 회전하면 기존 row의 token을 교체한다.
- 같은 installation에 다른 회원이 로그인하면 기존 row의 회원과 token을 새 회원 정보로 교체한다.
- 이전 회원의 로그아웃 요청이 늦게 도착해도 현재 소유자가 다르면 installation을 비활성화하지 않는다.
- installation 소유권은 요청 body의 회원 정보가 아니라 인증된 `MemberPrincipal`로 결정한다.

## Token-only API 비활성화

다음 API는 더 이상 제공하지 않는다.

- `PUT /api/v1/notifications/fcm/tokens`
- `DELETE /api/v1/notifications/fcm/tokens`
- `PUT /api/v1/notification/fcm/token`
- `DELETE /api/v1/notification/fcm/token`

FCM token은 회전할 수 있고 JWT는 회원만 식별하므로, token만으로 동일 installation인지 안전하게 판단할 수 없다. token, IP, User-Agent에서 `installationId`를 생성하는 fallback도 사용하지 않는다. 이런 fallback은 계정 전환 후 이전 회원의 token을 활성 상태로 남겨 알림이 잘못 전달될 수 있다.

installation metadata migration은 기존 token-only row를 모두 비활성화한다. 따라서 배포 후 신규 앱이 `installationId`와 함께 등록하기 전까지 해당 installation에는 FCM 알림이 발송되지 않는다.

## UX Writing Notes

`FCM`, `웹훅 어댑터` 같은 용어는 운영자 설정 화면 외에는 풀어 쓴다. 일반 사용자에게는 `푸시 알림을 보내지 못했어요. 잠시 후 다시 시도해주세요`처럼 표현한다.
