# Notification Lambda

Node.js 24/TypeScript 기반 Notification provider worker다. 공개 HTTP 서버나 Express/NestJS를
사용하지 않으며 EventBridge, SQS, Scheduler, Java IAM invoke event만 처리한다.

## Handler

| Handler | Trigger | 역할 |
|---|---|---|
| `fcm-installation-command` | Java Lambda invoke | installation 등록·해제 |
| `fcm-request-resolver` | FCM request SQS | member ID를 installation ID로 변환 |
| `fcm-batch-sender` | FCM batch SQS | 최대 500 installation FCM 전송 |
| `fcm-token-validator` | EventBridge Scheduler | stale token dry-run 검증 |
| `email-sender` | Email SQS | 인증/채용 template 렌더링과 SES v2 전송 |
| `webhook-sender` | Webhook SQS | Telegram/Discord/Slack 독립 전송 |

## Local verification

```bash
npm ci
npm run check
```

`npm run check`는 AsyncAPI의 JSON Schema에서 TypeScript 계약을 다시 생성한 뒤 typecheck, 단위
테스트, Lambda bundle 생성을 순서대로 검증한다.

AWS 리소스는 `../../infra/notification/template.yaml`, event 계약은
`../../contracts/notification/asyncapi.yaml`을 기준으로 한다.

## Logging policy

로그에는 event ID, request ID, channel, 건수, provider 결과 종류만 기록한다. raw FCM token,
이메일 주소, 인증 코드, Webhook URL, secret, 원문 event payload는 기록하지 않는다.
