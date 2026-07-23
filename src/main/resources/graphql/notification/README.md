# Notification GraphQL IDL

Notification은 조회 resource가 아니라 전달 command와 설치 lifecycle을 소유한다.

- FCM token 원문은 등록 input으로만 받고 output에는 노출하지 않는다.
- 설치 등록·해제는 현재 인증 회원에게만 적용된다.
- 운영진 발송 요청은 `FCM.WRITE` 권한을 검사하고 실제 전송은 outbox/event consumer가 비동기로 수행한다.
- Webhook, email, legacy topic 재구독은 내부 transport·운영 작업이므로 GraphQL public contract에 포함하지 않는다.
