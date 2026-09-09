# COMMUNITY KNOWLEDGE

## OVERVIEW

`community` 도메인은 게시글, 댓글, 스크랩, 신고와 초대 전용 `CommunityThread`를 소유한다.
주차별 우수 워크북 선정은 `curriculum` 도메인의 `WeeklyBestWorkbook`이 소유한다.

## STRUCTURE

```text
community/
├── domain/                         # Post, Comment, Scrap, Report, CommunityThread aggregate
├── application/port/in             # REST·WebSocket inbound UseCase와 DTO
├── application/port/out            # community persistence port
├── application/service              # command/query와 Chat facade
├── adapter/in/web                  # 게시글·댓글·신고·thread REST
├── adapter/in/websocket             # thread STOMP command/subscription adapter
└── adapter/out/persistence          # JPA/QueryDSL adapter
```

모든 entity는 domain 패키지의 직접 JPA 객체다. `CommunityThread`와
`CommunityThreadMember`는 `BaseEntity`를 상속하며 `@OneToMany` 컬렉션을 갖지 않는다.
Chat·Member·Storage와의 연결은 scalar ID와 공개 UseCase로만 표현한다.

## WHERE TO LOOK

| 관심사 | 구현 위치 |
| --- | --- |
| thread REST control | `adapter/in/web/CommunityThreadLifecycleController.java`, `CommunityThreadMembershipController.java` |
| thread REST query/recovery | `adapter/in/web/CommunityThreadQueryController.java` |
| thread message report/admin | `adapter/in/web/CommunityThreadMessageReportController.java` |
| STOMP six SEND handlers | `adapter/in/websocket/CommunityThreadStompController.java` |
| exact SEND/SUBSCRIBE parser | `adapter/in/websocket/CommunityStompDestinationParser.java` |
| SEND/SUBSCRIBE business authorization | `CommunityStompSendAuthorizer.java`, `CommunityStompSubscriptionAuthorizer.java` |
| ACK/error correlation | `CommunityStompCommandSupport.java`, `CommunityStompAckPublisher.java`, `CommunityStompErrorMapper.java` |
| Chat facade and REST history | `application/service/message/CommunityThreadMessageCommandService.java`, `CommunityThreadMessageQueryService.java` |
| Chat/Community event fan-out | `adapter/in/event/CommunityThreadRealtimeEventListener.java`, `application/service/realtime/*` |
| external destination adapter | `adapter/out/realtime/CommunityThreadRealtimeBroadcastAdapter.java` |
| broker mode/TLS/readiness | `global/config/WebSocketMessageBrokerConfig.java`, `WebSocketBrokerProperties*.java`, `StompRelayTcpClientFactory.java`, `global/websocket/relay/*` |
| realtime metrics | `application/service/realtime/CommunityThreadRealtimeMetrics.java` |

## THREAD OWNERSHIP

- Community가 thread 메타데이터, category, membership/role/state, 초대 자격, 정원, pin/mute,
  unread/last-activity projection, 신고와 외부 destination을 소유한다.
- Chat engine은 room membership, 메시지/답장/첨부, mention/reaction, edit/tombstone,
  read watermark와 generic domain event만 소유한다.
- CommunityThread가 unique scalar `chatRoomId`를 보관하지만 client REST/STOMP 계약에는
  `chatRoomId`나 raw `roomId`를 노출하지 않는다. 외부 요청은 항상 `threadId`를 사용한다.
- Community application service가 Chat 공개 command/query UseCase를 호출한다. Community controller,
  websocket adapter와 domain은 Chat repository/entity 또는 Chat adapter/in을 직접 참조하지 않는다.
- thread soft delete는 Chat room hard delete를 호출하지 않는다. 저장된 Chat history는 REST history
  recovery의 source of truth로 남긴다.

## TRANSPORT CONTRACT

### REST

Base URI는 `/api/v1/community`다. REST는 thread 생성/수정/삭제, pin/mute, member·invitable 조회,
invite/kick/leave/role 변경, history backfill, message report와 admin moderation query를 담당한다.
메시지 생성·수정·tombstone, reaction add/remove, read watermark 변경 REST endpoint는 만들지 않는다.

현재 retained route는 다음 controller가 소유한다.

```text
GET,POST   /api/v1/community/threads
GET,PATCH,DELETE /api/v1/community/threads/{threadId}
POST,DELETE /api/v1/community/threads/{threadId}/pin
POST,DELETE /api/v1/community/threads/{threadId}/mute
GET        /api/v1/community/threads/{threadId}/members
GET        /api/v1/community/threads/{threadId}/invitable
POST       /api/v1/community/threads/{threadId}/invite
DELETE     /api/v1/community/threads/{threadId}/members/{memberId}
POST       /api/v1/community/threads/{threadId}/leave
PATCH      /api/v1/community/threads/{threadId}/members/{memberId}/role
GET        /api/v1/community/threads/{threadId}/messages
POST       /api/v1/community/messages/{messageId}/report
GET        /api/v1/community/admin/thread-message-reports
```

위 목록 밖의 message create/edit/delete, reaction add/remove, read mutation REST handler와
`/api/v1/admin/...` top-level admin path는 금지한다.

Admin surface도 `/api/v1/community/admin/...` 아래에만 둔다. `/api/v1/admin/community/...`와
`/api/v1/admin/...` 형태는 금지한다. 현재 admin inbox는
`GET /api/v1/community/admin/thread-message-reports`다.

### WebSocket/STOMP

연결 endpoint는 `/ws`이며 native CONNECT의 `Authorization: Bearer <access-token>`와 각 SEND의
canonical lowercase UUID `x-command-id`를 요구한다. 외부 relay 환경에서도 application은
`BroadcastPort`의 user destination 전송과 per-member fan-out을 사용한다.

허용된 SEND destination은 다음 여섯 가지다.

```text
/app/community/threads/{threadId}/messages
/app/community/threads/{threadId}/messages/{messageId}/edit
/app/community/threads/{threadId}/messages/{messageId}/delete
/app/community/threads/{threadId}/messages/{messageId}/reactions/add
/app/community/threads/{threadId}/messages/{messageId}/reactions/remove
/app/community/threads/{threadId}/read
```

구독 destination은 authenticated principal이 있을 때 다음 exact user destination만 허용한다.

```text
/user/queue/community/threads/events
/user/queue/errors
```

event queue는 정상 상태 event와 ACK를 모두 전달하고 error queue는 recoverable error만
전달한다. 한 회원의 여러 session이 event queue를 구독하면 모든 session이 수신한다.
공용 Thread topic은 만들지 않고, event마다 ACTIVE recipient를 다시 계산하는 per-member
fan-out을 유지해 kick/leave 후 stale subscription으로의 후속 event 유출을 막는다.

## EVENTS, ACK, AND RECOVERY

모든 state event는 stable `eventId`, `type`, string `threadId`, `occurredAt`, typed payload를 갖는다.
Chat generic message/reaction/read event는 Community facade가 consumer payload로 변환하고, lifecycle
event는 Community가 직접 만든다. caller ACK는 `/user/queue/community/threads/events`로만
best-effort 전송하며
`commandId`, command type, affected IDs, optional `clientMessageId`, `deduplicated`를 포함한다.

envelope는 `application/port/in/realtime/dto/CommunityThreadRealtimeEvent`와
`CommunityThreadRealtimePayload`의 sealed typed record를 사용하며 `Map<String,Object>`나 global
serializer override를 사용하지 않는다. ACK는 `CommunityStompEventEnvelope`의
`command.acknowledged`이고, error payload는 `global/websocket/handler/WebSocketErrorPayload`의
`commandId`, `clientMessageId`, numeric HTTP-like `status`, stable `code`/`message`, `retryable`이다.

validation/permission/not-found/conflict/application/rate-limit 오류는 `/user/queue/errors`로 보내고
session을 닫지 않는다. 21번째 SEND는 typed 429 후 해당 frame만 버리고 다음 bucket에서 같은 session이
회복되어야 한다. CONNECT/JWT 실패, protocol 공격, broker 직접 SEND, malformed SUBSCRIBE는 terminal
STOMP ERROR다.

broker fan-out은 outbox commit 이후에 수행한다. partial fan-out은 모든 recipient를 시도한 뒤 실패를
집계하고 재시도하며, stable event ID로 중복을 허용한다. broker가 내려가도 business transaction은
rollback하지 않는다. client는 reconnect 뒤 REST thread detail/member/history API로 backfill한다.

복구 원천은 message/reaction/read gap의 `GET /threads/{threadId}/messages`, metadata/member/settings
gap의 thread detail/list/member query다. kick/leave/delete의 terminal event는 broker replay가 아니므로
REST가 terminal state를 반영하거나 거부하는지 확인한다.

## ALARM SEAM

알람 발송은 이 도메인의 책임이 아니다. 다만 미래 consumer가 사용할 수 있도록
`CommunityThreadInvitedEvent`, `CommunityThreadMessageCreatedEvent`,
`CommunityThreadMentionedEvent`를 ID-only immutable snapshot으로 남긴다. 이 event에는 message text,
title, name, token, deeplink, provider payload, mute/offline/DND 결정이나 notification dependency를
넣지 않는다. `CommunityThreadInvitedEvent`는 Community lifecycle의 `thread.invited` realtime
source로도 사용되지만, `CommunityThreadMessageCreatedEvent`와 `CommunityThreadMentionedEvent`는
Chat generic realtime event와 분리된 alarm-ready fact이며 realtime relay가 다시 소비하지 않는다.

Chat generic event의 consumer 변환은 `CommunityThreadChatRealtimeRelay`, Community lifecycle event의
fan-out은 `CommunityThreadLifecycleRealtimeRelay`가 담당한다. 알람 seam의 세 event는
message command가 `CommunityThreadMessageCreatedEvent`/`CommunityThreadMentionedEvent`를 outbox fact로
기록하고, membership/lifecycle command가 `CommunityThreadInvitedEvent`를 발행한다. 알림 발송/FCM/APNs
consumer는 이 범위에 없다.

## BROKER, READINESS, AND METRICS

- 단일 application instance 운영에서는 모든 프로필이 `app.websocket.broker.mode=SIMPLE`을 사용할 수
  있다. `WebSocketBrokerPropertiesValidator`는 `RELAY`를 선택한 경우에만 host/virtual-host/system·client
  credential 누락과 port 범위를 startup에서 fail-fast로 검증한다.
- `dev`/`prod`에서 `RELAY`를 선택하면 `relay.tls-enabled=true`가 필수다.
  `StompRelayTcpClientFactory`는 TLS hostname verification(`HTTPS`)을 적용하며,
  `WebSocketBrokerProperties.Relay.toString()`은 credentials를 redacted한다. secret은 환경변수/secret
  injection으로만 주입하고 로그에 기록하지 않는다.
- `StompBrokerRelayMonitor`의 availability/reconnect와 `WebSocketBrokerRelayStartupValidator`의
  `startup-timeout` readiness 대기는 운영 broker가 실제로 연결된 경우에만 통과한다.
- simple broker의 구독과 session registry는 instance-local이다. rolling deploy의 일시적 instance 중첩을
  포함해 다중 instance에서 무손실 실시간 전달이 필요해지면, shared RabbitMQ STOMP plugin/relay,
  secret injection, ALB SockJS fallback stickiness, configured heartbeat보다 긴 idle timeout과 전환
  검증을 scale-out 선행 조건으로 갖춘다. simple 운영 중 연결 종료·전달 공백은 client reconnect와 REST
  backfill로 복구한다.
- `CommunityThreadRealtimeMetrics`는 send/reject/rate-limit/fan-out/broadcast-failure/backfill을
  `operation`, `outcome`, `reason` 같은 고정 bucket으로만 기록한다. `threadId`, `memberId`,
  `messageId`, `eventId` 등 ID를 metric tag로 사용하지 않는다. broker availability/reconnect 및
  outbox retry/failed metric도 동일하게 low-cardinality를 유지한다.
  정의된 이름은 `community.thread.realtime.send.commands`,
  `community.thread.realtime.reject.commands`, `community.thread.realtime.rate.limit.rejections`,
  `community.thread.realtime.fanout.events`, `community.thread.realtime.fanout.recipients`,
  `community.thread.realtime.broadcast.failures`, `community.thread.realtime.backfill.requests`다.

## BOUNDARY RULES

- controller는 `application/port/in`만 호출하고 entity/adapter/out를 반환하거나 참조하지 않는다.
- cross-domain repository/entity import, `@OneToMany`, global ObjectMapper serializer 변경,
  notification delivery, typing/readers 기능은 금지한다.
- Chat message lifecycle invariant는 Chat에 두고 Community는 resource 권한과 mapping을 검증한다.
- thread 기존 상태 변경 순서는 CommunityThread lock → ACTIVE membership/role 검증 → ChatRoom lock
  → Chat membership/reply/mention/idempotency 재검증 → mutation → projection/outbox다.
- REST DTO의 numeric ID/count/offset 변환은 adapter response record에서만 수행하며 global ObjectMapper를
  수정하지 않는다.

## TEST MAP

- `community/domain`, `community/adapter/out/persistence`: entity invariant, index/unique constraint,
  capacity와 query projection.
- `community/application/service`: lifecycle permission matrix, Chat facade, lock/revalidation,
  idempotency와 outbox facts.
- `community/adapter/in/web`: retained REST route, forbidden mutation route, validation과 recovery.
- `community/adapter/in/websocket`: real JWT SockJS/STOMP command, stale-subscription leak, ACK/error,
  cross-instance relay와 broker failure recovery.
- `community/architecture/CommunityThreadArchitectureTest`: source boundary와 destination namespace를
  static scan으로 고정한다.
