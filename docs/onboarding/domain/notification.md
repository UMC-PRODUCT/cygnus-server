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

## 이메일 제공자 전환

이메일 템플릿과 인증 API는 그대로 두고 `EMAIL_PROVIDER`로 `SendEmailPort` 구현체를 선택한다.
기본값은 `ses`이며, 설정 변경 후 애플리케이션을 재시작해야 한다. 실패 시 다른 제공자로 자동 재발송하지 않는다.

- SES: `EMAIL_PROVIDER=ses`, `SES_REGION`, `SES_ACCESS_KEY_ID`, `SES_SECRET_ACCESS_KEY`와 검증된
  `EMAIL_NO_REPLY_ADDRESS`를 설정한다. `SES_CONFIGURATION_SET`은 선택 항목이다.
- 개인 Gmail SMTP: `EMAIL_PROVIDER=smtp`, `SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`,
  `SMTP_USERNAME`과 `SMTP_PASSWORD`를 설정한다. `SMTP_PASSWORD`에는 Google 2단계 인증 후 발급한 앱 비밀번호를
  사용하며, 일반 로그인 비밀번호를 넣지 않는다. `EMAIL_NO_REPLY_ADDRESS`는 해당 Gmail 주소와 맞춘다.
- 선택하지 않은 제공자의 자격증명은 필요하지 않다. SMTP는 인증과 STARTTLS를 필수로 사용하고,
  서버 인증서의 호스트 이름을 검사하며 연결·읽기·쓰기 대기에 각각 5초 제한을 둔다.

실제 자격증명은 배포 Secret 또는 Git에서 제외된 로컬 env에만 보관한다. SMTP 프로토콜 디버그를 켜거나
인증코드·수신 주소·메일 본문·비밀번호를 로그에 기록하지 않는다.
SMTP 예외 원문은 수신 주소를 포함할 수 있어 예외 종류만 기록하고 기존 `EMAIL_SEND_FAILED`로 변환한다.
SMTP health check는 상태 조회마다 외부 인증을 반복하지 않도록 끄고, 발송 성공·실패 로그와 지표를 확인한다.

SES로 복귀할 때는 프로덕션 액세스 승인 여부를 먼저 확인한 뒤 `EMAIL_PROVIDER=ses`와
`EMAIL_NO_REPLY_ADDRESS`를 기존 검증된 SES 발신 주소로 함께 되돌린다. 배포 후 허가된 테스트 주소에서
실제 수신을 확인하고, 더 이상 쓰지 않는 Gmail 앱 비밀번호를 폐기한다.
개인 Gmail 발송 제한은 별도로 적용되므로 SMTP 전환만으로 대량 발송이 보장되지는 않는다.

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
