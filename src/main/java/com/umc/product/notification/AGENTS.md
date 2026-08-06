# NOTIFICATION KNOWLEDGE

## OVERVIEW

`notification` owns FCM tokens/topics/outbox, email sending, webhook alarms, event listeners, and notification schedulers.

## STRUCTURE

```text
notification/
├── domain/                         # notification token/outbox models and exceptions
├── application/port/in             # FCM, email, webhook UseCases
├── application/port/out            # external and persistence ports
├── application/service             # FCM, outbox, topic, email, webhook services
├── application/service/evaluator   # audience/permission helpers
├── adapter/in/aop                  # notification-related aspects
├── adapter/in/event                # domain event listeners
├── adapter/in/scheduler            # outbox/scheduled delivery
├── adapter/in/web                  # FCM controller
└── adapter/out                     # external senders and persistence adapters
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| FCM API | `adapter/in/web/FcmController.java` | token/topic surface |
| FCM sending | `FcmService.java`, `FcmOutboxService.java` | direct and outbox delivery |
| Audience | `FcmAudienceService.java`, `FcmTopicService.java` | target expansion/topic behavior |
| Email | `SendEmailService.java`, `adapter/out/external/ses/SesEmailAdapter.java` | SES path |
| Webhooks | `WebhookAlarmService.java`, `Discord/Slack/TelegramWebhookAdapter.java` | external alarm paths |
| Persistence | `adapter/out/persistentce` | current persistence adapter package name |

## CONVENTIONS

- External delivery should go through ports/adapters; services should not know provider HTTP details.
- Outbox writes and sends must stay idempotency-aware.
- Avoid logging tokens, provider credentials, webhook URLs, or raw message payload secrets.
- Event listeners should keep event handling thin and delegate to UseCases.
- Scheduler retries need bounded failure handling and observable outcomes.
- Keep FCM token ownership tied to member identity from trusted server context.

## ANTI-PATTERNS

- Do not send provider requests directly from controllers or event DTOs.
- Do not drop failed notifications silently.
- Do not make topic membership updates depend only on client-provided member IDs.
- Do not rename `persistentce` casually; it is an existing package path and requires coordinated refactor.
