# Chat 도메인

## 역할과 경계

`com.umc.product.chat`은 room/member/message/pin, membership 불변식과 domain event를 소유한다. Chat core는 Project·Notice·Feedback 같은 consumer type이나 consumer repository를 import하지 않는다. consumer가 room ownership policy와 facade를 제공하고, Chat은 public UseCase와 membership 검증만 조합한다. 이 경계와 `AFTER_COMMIT` broadcast 원칙은 [ADR-027](../../adr/027-engine-resource-ownership-namespaces.md)과 [ADR-026](../../adr/026-separate-chat-engine-consumer-realtime-responsibilities.md)의 retained 규칙이다.

## Standalone ownership

standalone room은 `chat_room_ownership(room_id PK, namespace, owner_resource_key, slot)`에 immutable binding을 가진다. schema는 [V2026.07.16.00.20](../../../src/main/resources/db/migration/V2026.07.16.00.20__create_chat_room_ownership_registry.sql)이고, legacy `roomId`는 navigation mirror일 뿐 authorization truth가 아니다. canonical 좌표는 다음과 같다.

```text
chat.standalone/{roomId}/default
```

`chat_room` 전체를 `chat.standalone`으로 keyset backfill하며, missing/stale/broken/conflict를 임의 수정하지 않는다. Form과 동일하게 persisted·declared·evaluator namespace 집합을 exact-one으로 검증하고 coverage가 누락/중복/오염되면 readiness를 열지 않는다.

## Operation matrix

| Operation | 허용 조건 |
| --- | --- |
| `CREATE` | authenticated actor가 room을 만들고 ownership row와 creator membership을 한 transaction에서 생성한다. |
| `READ` / `SEND` | persisted ownership exact match, standalone policy, 요청 actor의 Chat membership을 모두 통과해야 한다. |
| `JOIN` / `LEAVE` | requester와 target이 같은 자기 자신일 때만 허용하고 기존 membership 불변식을 적용한다. |
| `PIN` / `UNPIN` / `DELETE` | standalone policy에서 거부한다. |
| `MEMBERSHIP_MANAGE` | standalone policy에서 거부한다. 타인 초대·강제 퇴장도 포함한다. |

consumer가 생기더라도 Chat core에 switch를 추가하지 않고 `ChatRoomOwnerPolicy` SPI로 namespace를 등록한다. policy가 허용해도 membership 또는 consumer business permission이 실패하면 READ/SEND는 거부한다. raw room ID나 caller-supplied namespace를 새 REST/GraphQL/STOMP 계약에 추가하지 않는다.

## Message와 Storage 연계

메시지 첨부는 [FileUsageRegistry](storage.md)의 `chat.message/{chatMessageId}/attachments` snapshot이다. 메시지 저장과 usage 등록, room 삭제 시 message usage cascade는 같은 transaction 경계에서 처리한다. 공유 file은 한 room/message가 제거돼도 다른 usage가 있으면 보존되며, 마지막 detach 뒤에만 Storage cleanup retention 대상이 된다.

message event는 DB commit 후 consumer facade로 위임한다. broadcast는 best-effort이고 조회 API/client backfill이 source of truth다. consumer가 소유하지 않은 room event는 무시하며 예외나 broadcast를 발생시키지 않는다.

## 코드와 테스트 진입점

- ownership: [`ChatRoomOwnershipAccessService`](../../../src/main/java/com/umc/product/chat/application/service/ChatRoomOwnershipAccessService.java), [`ChatStandaloneRoomOwnerPolicy`](../../../src/main/java/com/umc/product/chat/application/policy/ChatStandaloneRoomOwnerPolicy.java)
- persistence/backfill: [`ChatRoomOwnershipPersistenceAdapter`](../../../src/main/java/com/umc/product/chat/adapter/out/persistence/ChatRoomOwnershipPersistenceAdapter.java), [`ChatRoomOwnershipRolloutAdapter`](../../../src/main/java/com/umc/product/chat/adapter/out/backfill/ChatRoomOwnershipRolloutAdapter.java)
- usage: [`ChatMessageAttachmentUsageService`](../../../src/main/java/com/umc/product/chat/application/service/ChatMessageAttachmentUsageService.java)
- 테스트: [`ChatRoomOwnershipAccessServiceTest`](../../../src/test/java/com/umc/product/chat/application/service/ChatRoomOwnershipAccessServiceTest.java), [`ChatRoomCommandServiceAtomicityTest`](../../../src/test/java/com/umc/product/chat/application/service/command/ChatRoomCommandServiceAtomicityTest.java), [`ChatMessageUsageCascadeIntegrationTest`](../../../src/test/java/com/umc/product/chat/application/service/command/ChatMessageUsageCascadeIntegrationTest.java)
