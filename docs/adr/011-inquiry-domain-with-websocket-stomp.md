# ADR-006: 문의사항 도메인을 도입하고 메시지 송수신을 WebSocket + STOMP 로 구성한다

## Status

Proposed

## Context

2026-05-07 기준, UMC PRODUCT 서비스에는 챌린저(일반 사용자) 가 운영진에게 문의를 남기고 응답받을 공식 채널이 없다. 현재까지는 운영진의 카카오톡 / 디스코드 등 외부 채널로 문의가 흘러가고 있어, 문의 기록의 일관성·검색성·분쟁 처리(증빙) 측면에서 모두 한계가 있다. 신규 기획에서는 다음을 요구한다.

- 로그인한 사용자는 누구나 문의를 등록할 수 있고, 운영진도 문의 작성자가 될 수 있다.
- 등록 시 사용자는 **문의 대상(`InquiryTarget`)** 을 선택하며, 그 대상에 속한 운영진 전원이 해당 문의를 조회/응답할 권한을 가진다.
- 문의자와 운영진은 한 문의(=문의방) 안에서 **실시간 대화** 형태로 메시지를 주고받는다 (단순 댓글이 아닌 채팅에 가까운 UX).
- 메시지에는 첨부파일이 포함될 수 있다.
- 운영진 측은 본인 권한 범위로 들어오는 문의를 한 화면에서 확인할 수 있어야 하며, 상태(`RECEIVED`, `IN_PROGRESS`, `CLOSED`) 별 필터, 읽지 않은 문의 카운트 등 운영 가시성이 필요하다.

이번 결정에서 정해야 할 사항은 다음과 같다.

1. **실시간 메시지 채널 기술 선택.** 폴링 / SSE / WebSocket(+STOMP) / 외부 메시징 서비스 중 무엇으로 구현할 것인가.
2. **문의방 권한 모델.** "이 문의에 누가 들어올 수 있는가" 를 어디서, 어떤 단위로 검증할 것인가 (전송 권한과 구독 권한을 따로 본다).
3. **상태 머신.** `RECEIVED`, `IN_PROGRESS`, `CLOSED` 사이의 자동 전환과 수동 전환을 어떻게 정의할 것인가.
4. **메시지 / 문의 본문의 수정·삭제 정책.** 분쟁 처리 시 원본 보존을 어디까지 보장할 것인가.
5. **첨부파일 정책.** 기존 `FileMetadata` / `FileCategory` 와 어떻게 연결할 것인가.
6. **알림 / 미읽음 처리.** FCM 발송 트리거를 어디에 둘지, 미읽음 카운트를 어떻게 집계할지.
7. **확장 경로.** in-memory broker 로 시작하더라도 멀티 인스턴스 환경(=수평 확장) 에서 끊기지 않도록 마이그레이션 경로를 어떻게 잡을 것인가.

기술적 / 운영적 제약은 다음과 같다.

- 프로젝트는 Hexagonal Architecture + CQRS 컨벤션을 따른다 (`{Domain}CommandService` / `{Domain}QueryService`).
- `application/port/in` 은 Command / Query 로 디렉터리가 나뉜다 (`port/in/command`, `port/in/query`).
- 컨트롤러는 도메인 엔티티를 직접 노출하지 않는다 (`*Response` / `*Info` 매핑 강제).
- ID 참조는 도메인 경계를 넘는다 — 다른 도메인의 정보가 필요하면 해당 도메인의 Query UseCase 를 호출한다.
- 파일은 `storage` 도메인의 `FileMetadata.id` 로만 참조하고, 카테고리별 정책은 `FileCategory` 에서 관리한다.
- FCM 발송은 `notification` 도메인의 `SendNotificationToAudienceUseCase.sendToMembers(memberIds, title, body)` 로 일원화되어 있다.
- 운영진/일반 사용자 구분은 `ChallengerRoleType` / `ChallengerRole` 기준이며, 권한 검증 헬퍼는 `GetChallengerRoleUseCase` (`isCentralMemberInGisu`, `isSchoolAdminInGisu`, `isChapterPresidentInGisu` 등) 에 모여 있다.
- `gradle/dependencies.gradle.kts`에는 `spring-boot-starter-websocket` 의존성이 활성화되어 있다.
- 현재 단일 인스턴스 운영이지만, 수평 확장 시점이 멀지 않다 (모집 / 이벤트 시즌). 이 시점에 끊김 없이 확장 가능한 broker 구조여야 한다.

기획 측 검토에서 추가로 확정된 항목:

- **수정 / 삭제 미허용** — 분쟁 처리 / 기록 신뢰성 사유. 카카오톡 채널 / GitHub 정책 사례를 참고해 원본 기록을 보존한다.
- **운영진 측 미읽음 카운트 + 상태 필터** 를 함께 제공.
- `RECEIVED → CLOSED` **직접 전환 허용** — 스팸/장난성 문의 즉시 종료 용도.
- **운영진도 `CLOSED → IN_PROGRESS` 재오픈 가능** — 처음에는 작성자만 재오픈으로 잡혀 있었으나, 운영진 측 재오픈 니즈도 있어 양쪽 모두 허용으로 확정.
- **STOMP 구독 권한** — 전송뿐 아니라 구독 시점에도 권한 검증이 필요하다는 피드백 반영.

질의/미확정 항목:

- `InquiryTarget.UMC_PRODUCT` 이 매핑되는 `ChallengerRoleType` 이 코드상 존재하지 않는다. 이번 ADR 시점에는 임시로 "중앙운영사무국 멤버 전체" 로 라우팅하되, 별도 Role 신설 또는 속성 도입은 후속 결정으로 분리한다 (Phase 2 합의 사항 참고).
- `InquiryCategory` 목록은 기획 확정 후 변경 가능. 1차 구현은 기획 초안 그대로 enum 으로 두고, 추가/삭제는 Flyway 마이그레이션 + enum 변경으로 처리한다.

## Decision

우리는 다음과 같이 결정한다.

1. **신규 도메인 `inquiry` 를 추가한다.** 패키지는 다른 도메인과 동일하게 `domain` / `application/port/(in|out)` / `application/service` / `adapter/(in|out)` 4 계층으로 구성하고, `Inquiry` 를 Aggregate Root 로, `InquiryMessage` / `InquiryAttachment` / `InquiryMessageAttachment` 를 그 하위 엔티티로 둔다. 단, 동일 도메인 내라도 `@OneToMany` 컬렉션은 두지 않고 ID 기반 조회로 처리한다 (CLAUDE.md §2 준수).
2. **메시지 채널은 WebSocket + STOMP (Spring spring-boot-starter-websocket) 로 구현한다.**
    - 1차에는 Spring 내장 **simple in-memory broker** 를 사용한다 (`enableSimpleBroker("/topic")`).
    - 클라이언트 발신은 `/app/inquiry/{inquiryId}/message`, 구독은 `/topic/inquiry/{inquiryId}` 로 통일.
    - 인증은 공통 `StompPrincipalInterceptor` 가 CONNECT/STOMP 헤더의 JWT 를 검증하고 `Principal` 을 부착한다.
    - SUBSCRIBE 는 공통 registry 가 inquiry authorizer 에 위임하고, `/app/**` SEND 는 inquiry inbound adapter 가 호출하는 UseCase 에서 검증한다. 두 경로 모두 `InquiryAccessGuard` 의 동일한 도메인 접근 규칙을 사용한다.
3. **권한 모델은 두 그룹으로 정의한다.**
    - **문의자(`INQUIRER`)**: `Inquiry.createdByMemberId == 현재 사용자` 인 경우. 본인 문의방에만 구독 / 전송 가능.
    - **응답자(`RESPONDER`)**: 해당 문의의 `InquiryTarget` 에 속한 운영진. 전체 매핑은 다음과 같이 둔다.
        - `CENTRAL` → `ChallengerRoleType.isAtLeastCentralMember()`
        - `CHAPTER` → 작성자가 속한 기수의 `CHAPTER_PRESIDENT` (`isChapterPresidentInGisu(memberId, gisuId, chapterId)`)
        - `SCHOOL` → 작성자가 속한 (gisuId, schoolId) 의 `isSchoolAdminInGisu(memberId, gisuId, schoolId)`
        - `UMC_PRODUCT` → **임시로 중앙운영사무국 멤버 전체** 로 라우팅. 후속 ADR 또는 코드 변경으로 정식 매핑(예: `ChallengerRole.responsiblePart == PRODUCT` 같은 속성 기반) 을 도입.
    - 검증 로직은 `inquiry/application/service/InquiryAccessGuard` 한 곳에 모은다. Subscription authorizer와 REST adapter는 service 구현체를 직접 주입하지 않고 각각 공개 Inquiry Query/Command UseCase를 호출한다.
4. **상태 머신을 다음과 같이 고정한다.**

   ```
   RECEIVED ──(운영진 첫 메시지)──▶ IN_PROGRESS
   RECEIVED ──(운영진 직접 종료, 스팸 처리)──▶ CLOSED
   IN_PROGRESS ──(운영진 종료)──▶ CLOSED
   CLOSED ──(작성자 또는 운영진의 새 메시지/재오픈)──▶ IN_PROGRESS
   ```

    - `RECEIVED → IN_PROGRESS` 자동 전환은 운영진의 첫 응답 메시지 SEND 시 트리거.
    - `RECEIVED → CLOSED` 직접 전환은 운영진(`InquiryTarget` 권한 보유자) 만 가능. 별도 close API.
    - `CLOSED → IN_PROGRESS` 재오픈은 작성자 / 운영진 모두 가능. 새 메시지 전송 또는 명시적 reopen 동작으로 트리거.
5. **수정 / 삭제는 허용하지 않는다.** 문의 본문(`Inquiry.title`, `Inquiry.content`), 메시지 본문 모두 immutable. 잘못 보낸 메시지는 응답 메시지로 정정한다. 첨부파일은 `FileMetadata` 의 soft-delete 정책을 그대로 따른다 (`FileMetadata` 자체가 보존되는 한 첨부 매핑도 보존).
6. **첨부파일은 fileId 만 참조한다.**
    - 새 카테고리 `FileCategory.INQUIRY_ATTACHMENT` 를 추가 (이미지/문서 모두 허용, 최대 50MB).
    - `Inquiry` 등록과 메시지 송신 모두 `List<Long> fileIds` 를 받고, `FileQueryService.findAllByIds` (또는 동등) 로 검증 후 매핑 테이블에 저장한다. `FileMetadata` 자체는 storage 도메인이 단독 소유.
    - 매핑 엔티티는 `InquiryAttachment` (Inquiry ↔ FileMetadata.id), `InquiryMessageAttachment` (InquiryMessage ↔ FileMetadata.id) 두 종류.
7. **운영진 미읽음 카운트는 메시지 단위 read receipt 로 구현한다.**
    - 신규 테이블 `inquiry_message_read` (member_id, inquiry_message_id, read_at) 를 두고, 운영진이 문의 상세를 열거나 메시지를 수신할 때 `MarkInquiryMessagesReadUseCase` 가 일괄 upsert 한다.
    - 목록 응답은 "운영진 본인 기준 미읽음 메시지 수" 를 함께 내려준다 (1차는 N+1 회피 위해 IN 쿼리 + group by).
    - 문의자(`INQUIRER`) 측 미읽음 카운트는 1차 범위에서 빠진다 (필요 시 동일 구조로 후속 도입).
8. **상태 / 카테고리 / 대상 필터링을 목록 API 에 도입한다.**
    - `GET /api/v1/inquiries?status=RECEIVED&target=CENTRAL&category=BUG_REPORT&unreadOnly=true` 같은 multi-filter.
    - 운영진 응답: 본인 권한 범위 안의 문의 + 본인이 작성한 문의(union) 를 합쳐 반환. 일반 사용자: 본인이 작성한 문의만.
9. **알림 정책.**
    - 신규 문의 등록 → 해당 `InquiryTarget` 운영진 전원에게 FCM (제목·요약 본문).
    - 문의방에 새 메시지 발생 → 해당 문의의 반대편 참여자(들) 중 비활성 세션(=현재 STOMP 미접속 또는 백그라운드) 사용자에게 FCM.
    - 운영진에 의한 `CLOSED` 전환 → 작성자에게 FCM ("문의가 종료되었습니다") 발송.
    - 발송은 모두 기존 `SendNotificationToAudienceUseCase.sendToMembers(memberIds, title, body)` 로 위임.
10. **종료된 문의의 보관 기간은 무기한.** 분쟁 처리 / 회고 자료로 가치 있고, 데이터 규모상 1차에서는 별도 아카이브 정책을 도입하지 않는다. 추후 데이터 규모가 임계치를 넘기면 별도 ADR 로 보관 정책을 도입한다.
11. **확장 경로.** in-memory broker 로 시작하되, 인스턴스 증설 시점에 `WebSocketConfig` 만 교체해 외부 broker(예: Redis pub/sub via `enableStompBrokerRelay` 또는 ActiveMQ) 로 전환할 수 있도록 broker 설정을 단일 `@Configuration` 으로 격리한다 (도메인 코드는 broker 변경에 영향받지 않도록).
12. **Chat engine 과 소비 도메인의 실시간 책임을 분리한다.** 상세 책임과 consumer 구현 계약은 [ADR-026](./026-separate-chat-engine-consumer-realtime-responsibilities.md)을 따른다. Chat engine 은 외부 STOMP destination 을 직접 제공하지 않고 chat 도메인 이벤트만 발행한다.
    - 실제 topic, broadcast, resource → roomId 매핑과 접근 규칙은 inquiry/community 같은 소비 도메인이 소유한다.
    - 공통 `StompAuthChannelInterceptor` 는 `/topic`, `/queue` 로 시작하는 SUBSCRIBE 를 `StompSubscriptionAuthorizerRegistry` 에 위임한다. 지원 authorizer 가 없거나 둘 이상이거나 인가에 실패하면 fail-closed 처리한다.
    - `/user/queue/errors` 는 공통 오류 수신 경로로 exact 허용한다. 그 외 `/user/**` SUBSCRIBE 는
      `StompSubscriptionAuthorizerRegistry`의 exact-match 승인을 요구하고, 모든 `/user/**` 직접 SEND 는
      차단한다. Community Thread user destination 결정은 [ADR-026](./026-separate-chat-engine-consumer-realtime-responsibilities.md)을 따른다.
    - `/app/**` SEND 는 소비 도메인의 inbound adapter 가 받고, 해당 adapter 가 호출하는 UseCase 에서 resource 매핑과 전송 권한을 검증한다. 공통 registry 가 소비 도메인의 SEND 정책을 대신하지 않는다.
    - STOMP 1.0/1.1 `CONNECT` 와 STOMP 1.2 `STOMP` 연결 명령은 모두 동일하게 JWT 를 검증한다.
    - 서버 전용 `MESSAGE` 명령이 client inbound 로 들어오면 destination 과 관계없이 거부한다.
    - 소비 도메인은 `ChatMessageCreatedEvent` 를 자신의 topic 으로 전달한다. `ChatRoomPinnedMessageChangedEvent` 는 roomId 와 nullable pinnedMessageId 만 전달하고, 클라이언트는 소비 도메인의 방 상세 API 를 재조회해 전체 고정 메시지를 복원한다.

## Alternatives Considered

### 1. WebSocket 없이 폴링(polling) 으로 메시지 동기화

REST `GET /api/v1/inquiries/{id}/messages?after=...` 를 클라이언트가 주기적으로 호출.

장점:

- 인프라 요구가 가장 작다. WebSocket 의존성, broker, ChannelInterceptor 등이 필요 없다.
- 모바일 앱이 백그라운드 일 때 추가 처리 없이 다음 호출 시점에 따라잡힌다.
- HTTP 단일 채널이라 권한 검증이 단순하다 (REST 인증/인가 그대로).

단점:

- 실시간성 저하. 운영진이 사용자에게 즉답할 때 사용자에게 도달하기까지 폴링 간격만큼 지연된다.
- 트래픽 비효율. 메시지가 없는 상태에서도 주기적으로 요청이 발생한다.
- "방금 들어온 문의" UX (운영진 화면에서 새 문의 카드 즉시 노출) 가 자연스럽지 않다.

선택하지 않은 이유:
기획이 명시적으로 "실시간 채팅에 가까운 UX" 를 요구한다. 폴링은 비용 측면에서 문제는 적지만, UX 요구를 충족하지 못한다.

### 2. SSE (Server-Sent Events) 로 단방향 푸시 + REST 로 발신

수신은 SSE, 발신은 일반 REST POST 로 분리.

장점:

- 프로토콜이 단순하다 (HTTP 위에서 동작, 프록시 친화적).
- 클라이언트 라이브러리 부담이 작다.

단점:

- 양방향이 아니라 발신 / 수신 채널이 분리된다. 발신 결과가 다시 SSE 로 도달하기까지의 일관성을 별도로 관리해야 한다.
- 다중 문의방 구독에 SSE 가 약하다 (기본 모델이 endpoint 단위 스트림이라, 여러 방을 한 연결로 다중화하려면 추가 설계가 필요).
- STOMP 만큼의 broker / 인증 인터셉터 생태계가 없다.

선택하지 않은 이유:
다중 문의방 동시 구독 (운영진 화면에서 여러 문의가 동시에 갱신되는 시나리오) 에 자연스럽게 맞지 않다. STOMP 의 destination 모델이 본 도메인의 권한 모델과 잘 맞는다.

### 3. 외부 메시징 서비스(Stream / Sendbird / PubNub 등) 도입

채팅 SDK 를 그대로 도입.

장점:

- 1:1, 채널 권한, 푸시, 미읽음 카운트 등이 즉시 제공된다.
- 클라이언트 SDK 가 잘 되어 있다.

단점:

- 외부 종속성과 비용이 추가된다.
- 사용자 / 권한 모델을 외부 서비스에 미러링해야 하며, `ChallengerRole` 기반 권한 매핑을 외부에 옮기는 비용이 크다.
- 첨부파일을 외부에 저장할지/우리 storage 에 둘지 분기점이 새로 생긴다.
- 분쟁 처리 시 원본 로그를 외부 SaaS 에 의존하게 된다.

선택하지 않은 이유:
요구 규모(문의 채널 1 종) 대비 외부 SaaS 도입 비용이 과도하다. 권한 모델이 우리 `ChallengerRole` 에 강하게 묶여 있어, 외부 SaaS 의 채널/권한 모델로 옮기는 비용이 직접 구현 비용을 초과한다.

### 4. WebSocket "raw" (STOMP 미사용)

`@ServerEndpoint` 또는 `WebSocketHandler` 로 직접 처리.

장점:

- 프레임 포맷을 우리가 완전히 통제할 수 있다.
- 의존성이 가볍다.

단점:

- destination 라우팅 / subscription 관리 / broker 추상화를 우리가 직접 구현해야 한다.
- 연결 인증과 구독·발신 인가 경계를 직접 빌드해야 한다.
- 추후 외부 broker 로 전환할 때 호환 계층이 부재해 큰 리팩토링이 필요하다.

선택하지 않은 이유:
STOMP 가 destination 모델 / ChannelInterceptor / broker relay 등 이번 도메인이 필요한 것들을 그대로 제공한다. 직접 구현 시 얻는 이점이 없다.

### 5. 메시지 수정 / 삭제 허용

메시지 보낸 직후 일정 시간 안에 수정 / 삭제 허용.

장점:

- 사용자 측 오타 정정 / 잘못 보낸 메시지 회수가 용이.
- 카카오톡 류 UX 와 비슷.

단점:

- 분쟁 처리 시 원본 기록 신뢰성 훼손. "삭제된 메시지" 표식만 남기더라도 운영진 측 캡처와 어긋날 수 있다.
- soft-delete + edit-history 를 함께 도입하면 데이터 모델이 커진다.

선택하지 않은 이유:
이번 도메인의 핵심 가치 중 하나가 "공식 문의 채널의 기록 신뢰성" 이다. 정정은 응답 메시지로 처리한다.

### 6. 미읽음을 별도 테이블 없이 "마지막 읽은 메시지 ID" 로만 관리

`inquiry_member_last_read (inquiry_id, member_id, last_read_message_id)` 같은 단일 행 형태.

장점:

- 테이블/쿼리가 단순. 한 행으로 끝.
- 미읽음 카운트는 `messages.id > last_read_message_id` 카운트.

단점:

- "특정 메시지를 누가 읽었는가" 는 알 수 없다. 운영진이 여러 명일 때 "이 메시지를 본 운영진" 을 추적할 수 없다.
- 운영진 한 명이 늦게 들어와도 "이 운영진의 미읽음" 만 보여줄 수 있다 (이건 문제 없음). 다만 향후 "운영진 누구는 봤는데 누구는 못 봤다" 같은 시나리오 확장은 불가.

선택하지 않은 이유 (부분):
1차 요구사항만 보면 충분하지만, "운영진별 read receipt" 가 향후 요구로 등장할 가능성이 충분히 높다 (이미 기획에서 운영진 미읽음 카운트가 들어와 있음). 한 단계 더 정밀한 `inquiry_message_read` 테이블 비용은 작고, 확장성이 크다.

### 7. `UMC_PRODUCT` 을 위한 신규 `ChallengerRoleType` 즉시 도입

`ChallengerRoleType.UMC_PRODUCT_MEMBER` 같은 새 enum 추가.

장점:

- 정확한 매핑이 가능해진다.
- 이후 권한 검증이 `hasRoleTypeInGisu(memberId, gisuId, UMC_PRODUCT_MEMBER)` 한 줄로 풀린다.

단점:

- enum 신설은 권한 / 운영 / DB seed 모두에 영향이 있어 본 ADR 의 범위를 벗어난다.
- UMC PRODUCT의 정의(소속 기준) 가 코드 / 운영 측 합의를 거쳐야 한다.

선택하지 않은 이유:
본 ADR 은 문의 도메인 도입 자체에 집중한다. `UMC_PRODUCT` 라우팅은 1차에 임시 매핑으로 시작하고, 정식 Role 도입은 별도 ADR / 별도 PR 에서 다룬다.

## Consequences

### Positive

- 공식 문의 채널이 생기면서 카카오톡 / 디스코드로 흩어지던 문의 흐름을 일원화할 수 있다. 운영진 측 검색 / 회고 / 분쟁 처리 비용이 큰 폭으로 줄어든다.
- WebSocket + STOMP 도입으로 향후 다른 도메인의 채팅성 기능(예: 1:1 챌린저 매칭, 멘토링) 을 같은 인프라에 얹을 수 있다.
- in-memory broker 로 시작하지만 broker 설정만 분리해 두기 때문에, 인스턴스 증설 시점에 도메인 코드 변경 없이 외부 broker 로 전환할 수 있다.
- 권한 검증 로직을 `InquiryAccessGuard` 단일 빈에 모으므로 REST / STOMP 양쪽 채널의 인가 일관성을 보장하기 쉽다.
- 메시지 immutable 정책으로 분쟁 처리 시 원본 보존이 보장된다.

### Negative

- 도메인 / 인프라 양쪽에 적지 않은 신규 코드가 추가된다 (도메인 4 엔티티 + Service 다수 + WebSocket 설정 + ChannelInterceptor + read receipt 테이블).
- in-memory broker 한계: 인스턴스가 2 대 이상으로 늘어나는 순간 한 인스턴스의 SEND 이벤트가 다른 인스턴스 구독자에게 도달하지 않는다 — 그 시점에 broker 교체 마이그레이션이 강제된다 (코드 영향은 작지만, 운영 시점에 명시적 결정이 필요).
- WebSocket 채널은 일반 REST 와 다른 운영 / 모니터링 셋업이 필요하다 (연결 수 메트릭, 끊긴 세션 정리, heartbeat 튜닝 등).
- `UMC_PRODUCT` 임시 매핑(중앙운영사무국 멤버 전체) 으로 인해 1차 운영 중 일부 문의가 의도보다 넓은 범위에 노출될 수 있다. 이는 후속 ADR 까지의 한시적 trade-off.
- `inquiry_message_read` 테이블은 메시지 수에 비례해 빠르게 커질 수 있다 (운영진 N명 × 메시지 M건 = N*M 행).

### Neutral / Trade-offs

- 메시지 수정 / 삭제 미허용은 사용성 측면에서 일부 사용자 불편을 유발할 수 있다. 정책 차원에서는 수용 가능한 trade-off.
- 운영진의 `CLOSED → IN_PROGRESS` 재오픈 허용은 상태 머신을 단방향이 아닌 양방향으로 만들지만, 운영 현실(예: 추가 정보 발견 후 재대응) 을 반영하기 위한 의도적 결정.
- read receipt 테이블이 운영진 측에만 도입되어 데이터 모델이 비대칭이다. 필요 시 동일 구조로 문의자 측에도 확장 가능하므로, 비대칭은 비용보다 확장 가능성 측면이 크다고 본다.

## Implementation Notes

### 사전 작업: 기존 코드 수정

- `gradle/dependencies.gradle.kts`
    - `spring-boot-starter-websocket`을 `implementation`으로 선언한다. (WebSocket의 STOMP 지원은 starter에 포함됨.)
- `global/exception/constant/Domain` enum
    - `INQUIRY` 추가.
- `authorization/domain/ResourceType` enum
    - `INQUIRY("inquiry", "문의사항", Set.of(READ, WRITE))` 추가.
    - `INQUIRY_MESSAGE("inquiry_message", "문의사항 메시지", Set.of(READ, WRITE))` 추가.
    - 단, 본 도메인의 권한 검증은 1차에서 `InquiryAccessGuard` 안에서 직접 처리하므로 ResourceType 은 감사/리소스 식별 용도로만 추가. `@CheckPermission` 기반 검증으로 옮기는 것은 후속 작업.
- `storage/domain/enums/FileCategory` enum
    - `INQUIRY_ATTACHMENT("public/inquiry", 50 * 1024 * 1024, new String[]{"jpg","jpeg","png","webp","gif","heic","pdf","doc","docx","xls","xlsx","ppt","pptx","zip"})` 추가.
- `global/config/WebSocketConfig` 신규
    - `@EnableWebSocketMessageBroker`.
    - `registerStompEndpoints` → `addEndpoint("/ws").setAllowedOriginPatterns(...)` (CORS 동일 정책 재사용).
    - `configureMessageBroker` → `enableSimpleBroker("/topic")`, `setApplicationDestinationPrefixes("/app")`.
    - `configureClientInboundChannel` → 공통 JWT 인증, broker 직접 발행 차단, subscription authorizer registry 인터셉터 등록.
    - 향후 broker 교체 시 본 클래스 한 곳만 수정한다.
- `global/config/SecurityConfig`
    - `auth.requestMatchers("/ws/**").permitAll()` (STOMP 핸드셰이크는 별도 인터셉터에서 JWT 검증, Spring Security 단계에서는 통과시킨다).

### 도메인 / 패키지 구조

```
src/main/java/com/umc/product/inquiry/
├── domain/
│   ├── Inquiry.java
│   ├── InquiryMessage.java
│   ├── InquiryAttachment.java
│   ├── InquiryMessageAttachment.java
│   ├── enums/
│   │   ├── InquiryStatus.java          // RECEIVED / IN_PROGRESS / CLOSED
│   │   ├── InquiryTarget.java          // CENTRAL / UMC_PRODUCT / CHAPTER / SCHOOL
│   │   ├── InquiryCategory.java        // 기획 초안 그대로 (기획 확정 후 수정)
│   │   └── InquiryMessageSenderType.java // INQUIRER / RESPONDER
│   └── exception/
│       ├── InquiryDomainException.java
│       └── InquiryErrorCode.java
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── command/
│   │   │   │   ├── SubmitInquiryUseCase.java
│   │   │   │   ├── SendInquiryMessageUseCase.java
│   │   │   │   ├── CloseInquiryUseCase.java
│   │   │   │   ├── ReopenInquiryUseCase.java
│   │   │   │   ├── MarkInquiryMessagesReadUseCase.java
│   │   │   │   └── dto/...
│   │   │   └── query/
│   │   │       ├── GetInquiryUseCase.java
│   │   │       ├── ListInquiryMessageUseCase.java
│   │   │       └── dto/...
│   │   └── out/
│   │       ├── LoadInquiryPort.java
│   │       ├── SaveInquiryPort.java
│   │       ├── LoadInquiryMessagePort.java
│   │       ├── SaveInquiryMessagePort.java
│   │       ├── LoadInquiryMessageReadPort.java
│   │       └── SaveInquiryMessageReadPort.java
│   └── service/
│       ├── command/
│       │   ├── InquiryCommandService.java
│       │   ├── InquiryMessageCommandService.java
│       │   └── InquiryReadReceiptService.java
│       ├── query/
│       │   ├── InquiryQueryService.java
│       │   └── InquiryMessageQueryService.java
│       └── InquiryAccessGuard.java        // resource 매핑과 접근 규칙 공용
└── adapter/
    ├── in/
    │   ├── web/
    │   │   ├── InquiryController.java                // REST
    │   │   ├── InquiryMessageController.java         // REST (메시지 목록 / 미읽음 처리)
    │   │   ├── ws/InquiryStompController.java        // @MessageMapping, SEND 인가
    │   │   ├── ws/InquiryStompSubscriptionAuthorizer.java
    │   │   ├── event/ChatMessageEventListener.java   // consumer topic broadcast
    │   │   └── dto/(request|response)/...
    └── out/
        └── persistence/
            ├── InquiryJpaRepository.java
            ├── InquiryMessageJpaRepository.java
            ├── InquiryMessageReadJpaRepository.java
            └── InquiryPersistenceAdapter.java
```

### DB 스키마 (Flyway)

```sql
-- inquiry
CREATE TABLE inquiry (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200)   NOT NULL,
    content         TEXT           NOT NULL,
    category        VARCHAR(40)    NOT NULL,
    target          VARCHAR(40)    NOT NULL,
    target_school_id BIGINT        NULL,    -- target=SCHOOL 일 때만
    target_chapter_id BIGINT       NULL,    -- target=CHAPTER 일 때만
    target_gisu_id  BIGINT         NULL,    -- target 매핑 시 기수 컨텍스트
    status          VARCHAR(20)    NOT NULL,
    created_by_member_id BIGINT    NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    closed_at       TIMESTAMPTZ    NULL
);
CREATE INDEX idx_inquiry_target_status ON inquiry (target, status, created_at DESC);
CREATE INDEX idx_inquiry_creator ON inquiry (created_by_member_id, created_at DESC);

-- inquiry_message
CREATE TABLE inquiry_message (
    id              BIGSERIAL PRIMARY KEY,
    inquiry_id      BIGINT         NOT NULL REFERENCES inquiry(id),
    sender_type     VARCHAR(20)    NOT NULL,  -- INQUIRER / RESPONDER
    sender_member_id BIGINT        NOT NULL,
    content         TEXT           NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX idx_inquiry_message_inquiry_created
    ON inquiry_message (inquiry_id, created_at);

-- inquiry_attachment (문의 본문 첨부)
CREATE TABLE inquiry_attachment (
    id              BIGSERIAL PRIMARY KEY,
    inquiry_id      BIGINT         NOT NULL REFERENCES inquiry(id),
    file_metadata_id BIGINT        NOT NULL,  -- storage 도메인 참조 (FK 미설정, 도메인 경계)
    UNIQUE (inquiry_id, file_metadata_id)
);

-- inquiry_message_attachment (메시지 첨부)
CREATE TABLE inquiry_message_attachment (
    id              BIGSERIAL PRIMARY KEY,
    inquiry_message_id BIGINT     NOT NULL REFERENCES inquiry_message(id),
    file_metadata_id BIGINT        NOT NULL,
    UNIQUE (inquiry_message_id, file_metadata_id)
);

-- read receipt
CREATE TABLE inquiry_message_read (
    inquiry_message_id BIGINT     NOT NULL REFERENCES inquiry_message(id),
    member_id          BIGINT     NOT NULL,
    read_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (inquiry_message_id, member_id)
);
CREATE INDEX idx_inquiry_message_read_member
    ON inquiry_message_read (member_id);
```

도메인 경계 원칙에 따라 `file_metadata_id` 는 FK 를 걸지 않고 ID 참조만 한다 (`FileMetadata` 는 storage 도메인 소유).

### STOMP destination / 인터셉터

| 단계        | destination / 명령                   | 인가 검증 위치                                                                      | 검증 내용                                             |
|-----------|------------------------------------|-------------------------------------------------------------------------------|---------------------------------------------------|
| CONNECT   | `/ws`                              | 공통 `StompPrincipalInterceptor`                                                   | JWT 헤더 검증 → `Principal` 부착                        |
| SUBSCRIBE | `/topic/inquiry/{inquiryId}`       | 공통 `StompAuthChannelInterceptor` → registry → `InquiryStompSubscriptionAuthorizer` | `InquiryAccessGuard.canRead(memberId, inquiryId)` |
| SEND      | `/app/inquiry/{inquiryId}/message` | inquiry inbound adapter → inquiry UseCase                                         | `InquiryAccessGuard.canSend(memberId, inquiryId)` |

`InquiryAccessGuard` 메서드 시그니처:

```java
public class InquiryAccessGuard {

    public void ensureCanRead(Long memberId, Long inquiryId) { ... }   // 위반 시 예외

    public void ensureCanSend(Long memberId, Long inquiryId) { ... }   // 위반 시 예외

    public InquiryMessageSenderType resolveSenderType(Long memberId, Long inquiryId) { ... }
        // INQUIRER 또는 RESPONDER 결정. 양쪽 모두인 경우(=본인이 작성하고 권한도 보유) 우선순위는 RESPONDER 가 아닌 INQUIRER (작성자 컨텍스트 우선).
}
```

### REST API (1차 범위)

| Method | Path                                          | UseCase                           | 비고                                                   |
|--------|-----------------------------------------------|-----------------------------------|------------------------------------------------------|
| POST   | `/api/v1/inquiries`                           | `SubmitInquiryUseCase`            | 첨부 fileId[] 포함                                       |
| GET    | `/api/v1/inquiries`                           | `GetInquiryUseCase.listForViewer` | status/target/category/unreadOnly 필터                 |
| GET    | `/api/v1/inquiries/{inquiryId}`               | `GetInquiryUseCase.getById`       | 작성자 Member/Challenger/ChallengerRole 정보 동봉 (운영진 노출용) |
| GET    | `/api/v1/inquiries/{inquiryId}/messages`      | `ListInquiryMessageUseCase`       | 메시지 목록 (createdAt asc)                               |
| POST   | `/api/v1/inquiries/{inquiryId}/messages/read` | `MarkInquiryMessagesReadUseCase`  | 미읽음 일괄 처리                                            |
| POST   | `/api/v1/inquiries/{inquiryId}/close`         | `CloseInquiryUseCase`             | RECEIVED/IN_PROGRESS → CLOSED                        |
| POST   | `/api/v1/inquiries/{inquiryId}/reopen`        | `ReopenInquiryUseCase`            | CLOSED → IN_PROGRESS, 작성자/운영진 모두 가능                  |

WebSocket(STOMP) 측은 SUBSCRIBE/SEND 두 가지뿐이다. 발신 페이로드는 `{ content, fileIds[] }` 이고, inquiry adapter 가 resource → roomId 매핑과 전송 권한을 검증한 뒤 chat UseCase 를 호출한다. 저장된 메시지 이벤트는 inquiry listener 가 `/topic/inquiry/{inquiryId}` 로 broadcast 한다.

### FCM 트리거 위치

- `SubmitInquiryUseCase` 끝부분에서 대상 운영진 memberId 목록을 산출 후 `sendToMembers`.
- `SendInquiryMessageUseCase` (= STOMP 컨트롤러에서 호출되는 Service) 에서 메시지 저장 직후 반대편 참여자 중 비활성 세션 목록을 산출 후 `sendToMembers`. "비활성 세션" 판단은 1차 단순화 — `SimpUserRegistry` 로 현재 STOMP 접속자 memberId 집합을 얻고, 그 외 모두 FCM 발송. 백그라운드 모드 정확 감지는 클라이언트 응답에 의존.
- `CloseInquiryUseCase` 에서 작성자에게 `sendToMembers([creatorMemberId], ...)`.

### 테스트 전략

- 도메인 단위: `InquiryTest`, `InquiryMessageTest` — 상태 머신 / sender type 결정.
- Service 단위 (Mockito): `InquiryCommandServiceTest`, `InquiryAccessGuardTest`, `InquiryReadReceiptServiceTest`.
- WebSocket 통합: `InquiryStompIntegrationTest` (Spring Boot Test + StandardWebSocketClient + StompSession). 시나리오:
    - 인증 없는 SUBSCRIBE → 거절.
    - 권한 없는 사용자의 SUBSCRIBE → 거절.
    - 정상 전송 → 구독자에게 broadcast.
    - 운영진 첫 메시지 → 상태가 IN_PROGRESS 로 전환.
    - CLOSED 상태에서 작성자 SEND → 상태가 IN_PROGRESS 로 재오픈.
- REST 컨트롤러: MockMvc (`@DisplayName` 한국어, Given/When/Then).

---

## Implementation Plan (Phase / Commit 단위)

각 커밋은 단독 빌드 / 테스트 통과 가능해야 한다. Conventional Commits 준수 (`feat:`, `chore:`, `test:`, `docs:`).

### Phase 0: 사전 준비 (인프라 / 공용 enum)

도메인 코드 추가 전, 공용 자원 / 의존성을 먼저 정리한다. Phase 1 이후의 모든 커밋이 이 변경을 전제로 동작.

1. `chore: WebSocket 의존성 활성화`
    - `gradle/dependencies.gradle.kts`에 `spring-boot-starter-websocket` 선언.
    - 빌드 / 테스트 통과 확인 (이 커밋만으로는 도메인 동작 변경 없음).
2. `feat: 공용 enum 에 inquiry 도메인 항목 추가`
    - `Domain.INQUIRY` 추가.
    - `ResourceType.INQUIRY`, `INQUIRY_MESSAGE` 추가.
    - `FileCategory.INQUIRY_ATTACHMENT` 추가 (경로 / 최대 크기 / 허용 확장자 명시).
    - 단위 테스트 (`FileCategoryTest` 가 있다면 새 카테고리 케이스 추가).
3. `feat: WebSocket + STOMP 글로벌 설정 추가`
    - `global/config/WebSocketConfig` 신규.
    - `SecurityConfig` 에 `/ws/**` permitAll 추가 (STOMP 핸드셰이크 통과).
    - Phase 0 당시에는 endpoint 가 비어 있어 외부 호출이 무동작이었다.
    - 현재는 `WebSocketMessageBrokerConfig`가 `/ws` endpoint와 STOMP broker를 활성화한 상태다.

### Phase 1: 도메인 + 영속화 (CRUD 기반)

기능 동작 전, 도메인 / 테이블 / 어댑터를 먼저 구축. 외부 노출 API 는 아직 없다.

4. `feat: inquiry 도메인 엔티티 추가`
    - `Inquiry`, `InquiryMessage`, `InquiryAttachment`, `InquiryMessageAttachment` 엔티티 + enum 4 종 + `InquiryDomainException`/`InquiryErrorCode`.
    - 상태 머신 이전 메서드(`receive()`, `markInProgressByResponder()`, `closeBy(...)`, `reopen()`) 를 도메인 안에 둔다.
    - 단위 테스트 — 상태 전환 / sender type 결정 / 잘못된 전이 시 예외.
5. `feat: inquiry Flyway 마이그레이션 추가`
    - `V2026.MM.DD.HH.mm__create_inquiry_tables.sql` — `inquiry`, `inquiry_message`, `inquiry_attachment`, `inquiry_message_attachment`, `inquiry_message_read` + 인덱스.
    - 로컬 / Testcontainers 양쪽에서 마이그레이션 검증.
6. `feat: inquiry 영속화 어댑터 추가`
    - JPA 엔티티 매핑 (`@OneToMany` 미사용, `@ManyToOne` 만 동일 도메인 경계 안에서 사용).
    - `InquiryJpaRepository`, `InquiryMessageJpaRepository`, `InquiryMessageReadJpaRepository`.
    - Outbound port 6 종 + `InquiryPersistenceAdapter` 구현.
    - 영속화 통합 테스트 (Testcontainers).

### Phase 2: 권한 / 접근 제어 통합

REST / STOMP 양쪽이 공유할 권한 검증 빈을 먼저 만든다 (이후 모든 Phase 의 전제).

7. `feat: InquiryAccessGuard 추가`
    - `GetMemberUseCase` (작성자 소속 학교/지부 조회), `GetChallengerRoleUseCase` 의존.
    - 메서드: `ensureCanRead`, `ensureCanSend`, `resolveSenderType`.
    - `UMC_PRODUCT` 임시 매핑(중앙운영사무국 멤버 전체) 명시 — 코드 주석에 후속 작업 TODO 표시.
    - 단위 테스트: 4 가지 `InquiryTarget` × (작성자 / 권한 보유 운영진 / 권한 없는 사용자) 매트릭스.

### Phase 3: 등록 / 종료 / 재오픈 (REST Command)

실시간 채널보다 먼저 핵심 상태 전이 API 를 노출. 운영진은 이 시점부터 문의를 "받을" 수 있다 (다만 응답은 아직 REST 메시지로만).

8. `feat: 문의 등록 UseCase / Service / API`
    - `SubmitInquiryUseCase` + `InquiryCommandService.submit(...)`.
    - 첨부 fileId 검증은 `FileQueryService.findAllByIds` (또는 동등) 호출.
    - 컨트롤러: `POST /api/v1/inquiries`.
    - FCM 발송: 대상 운영진 memberId 목록 산출 → `sendToMembers`.
9. `feat: 문의 종료 / 재오픈 UseCase / API`
    - `CloseInquiryUseCase`, `ReopenInquiryUseCase`.
    - `RECEIVED → CLOSED` 직접 전환(스팸 처리) / `CLOSED → IN_PROGRESS` 양방향(작성자 + 운영진).
    - 컨트롤러: `POST /api/v1/inquiries/{id}/close`, `POST /api/v1/inquiries/{id}/reopen`.
    - 종료 시 작성자에게 FCM 발송.
    - 단위 / MockMvc 테스트.

### Phase 4: 메시지 송수신 (REST 1차)

STOMP 도입 전, REST 폴백 경로로 메시지 전송 / 조회를 먼저 검증. 이로써 STOMP 가 들어와도 도메인 / Service 는 그대로 재사용된다.

10. `feat: 문의 메시지 전송 UseCase / Service`
    - `SendInquiryMessageUseCase` + `InquiryMessageCommandService.send(...)`.
    - 운영진 첫 메시지 시 자동 `RECEIVED → IN_PROGRESS` 전환.
    - 작성자가 `CLOSED` 에서 메시지 보내면 자동 `IN_PROGRESS` 재오픈.
    - 첨부 fileId 검증 / `InquiryMessageAttachment` 저장.
    - REST 컨트롤러는 1차에 노출하지 않거나(STOMP 가 본 채널), 운영자 디버그용으로만 둠 — 정책 결정 시 본 커밋 안에서 한 줄로 결정.
    - 발송 완료 후 반대편 참여자에게 FCM (비활성 세션 대상).
    - 단위 테스트 (Mockito).
11. `feat: 문의 메시지 조회 UseCase / API`
    - `ListInquiryMessageUseCase`, `GetInquiryUseCase.getById` (작성자 Member/Challenger/ChallengerRole 정보 동봉).
    - REST: `GET /api/v1/inquiries`, `GET /api/v1/inquiries/{id}`, `GET /api/v1/inquiries/{id}/messages`.
    - 다중 필터 (status / target / category / unreadOnly).

### Phase 5: 운영진 미읽음 / 카운트

12. `feat: inquiry message read receipt 구현`
    - `MarkInquiryMessagesReadUseCase` + `InquiryReadReceiptService`.
    - REST: `POST /api/v1/inquiries/{id}/messages/read` (운영진 본인 기준 일괄 read).
    - 목록 응답 DTO 에 `unreadCount` 필드 추가 (운영진 본인 기준).
    - 단위 / MockMvc 테스트.

### Phase 6: WebSocket + STOMP

REST 가 모든 동작을 커버하는 상태에서 실시간 채널을 추가. 도메인 / Service / Guard 는 그대로 재사용.

13. `feat: STOMP 인증 / 인가 인터셉터`
    - `InquiryStompSubscriptionAuthorizer` 가 inquiry destination 을 식별하고 공개 `CheckInquiryChatAccessUseCase` 로 SUBSCRIBE 를 인가한다.
    - 공통 `StompSubscriptionAuthorizerRegistry` 가 정확히 하나의 authorizer 에 위임하며, 없음/거부/복수 매칭은 fail-closed 처리한다.
    - `/app/**` SEND 는 inquiry inbound adapter 가 resource → roomId 매핑 후 inquiry UseCase 에서 인가한다.
    - `WebSocketConfig` 의 broker / endpoint 셋업도 이 커밋에서 finalize (`/ws`, `/topic`, `/app`).
    - 통합 테스트 (StompSession): 비인증 / 권한 없음 / 정상 분기.
14. `feat: STOMP 메시지 수신 / 브로드캐스트 컨트롤러`
    - `@MessageMapping("/inquiry/{inquiryId}/message")` 핸들러.
    - 핸들러는 inquiry 권한 검증과 resource → roomId 매핑 후 chat 메시지 전송 UseCase 에 위임한다.
    - inquiry event listener 는 commit 이후 `ChatMessageCreatedEvent` 를 받아 공통 `BroadcastPort` 로 `/topic/inquiry/{id}` 에 전달한다.
    - `ChatRoomPinnedMessageChangedEvent` 는 nullable pinnedMessageId 변경만 전달하며, 클라이언트는 inquiry 방 상세 API 를 재조회한다.
    - 발신자도 동일 토픽 구독 중이면 자기 메시지를 echo 로 받게 됨 (클라이언트 측에서 messageId 로 dedupe).
    - 통합 테스트: 정상 송수신 / 상태 자동 전환 / 재오픈 시나리오.

### Phase 7: 운영 가시성 / 마무리

15. `test: 문의 도메인 시나리오 통합 테스트`
    - 등록 → 운영진 첫 응답(자동 IN_PROGRESS) → 작성자 응답 → 운영진 종료 → 작성자 재오픈 전체 흐름.
    - WebSocket / REST 혼합 시나리오 (한쪽이 SUBSCRIBE, 다른 쪽이 REST 전송 — STOMP-only 정책일 경우 본 케이스 제외).
16. `docs: ADR-006 운영 가이드 갱신 / Status 전환`
    - 본 ADR Status 를 `Accepted` 로 전환.
    - `UMC_PRODUCT` 매핑 후속 작업 항목을 별도 ADR / 이슈로 분리해 링크 추가.
    - 운영 가이드 문서가 있다면 미읽음 카운트 / 필터 사용법 / 종료 정책을 추가.

### Phase 8 (후속, 별도 ADR)

본 ADR 의 범위 밖. 별도 의사결정으로 분리.

- `UMC_PRODUCT` 라우팅의 정식 매핑 (신규 `ChallengerRoleType` 또는 `ChallengerRole.responsibleScope` 도입).
- 인스턴스 증설 시점 broker 외부화 (Redis pub/sub via `enableStompBrokerRelay` 등).
- 종료된 문의의 보관 / 아카이브 정책.
- 문의자 측 미읽음 카운트.
- `@CheckPermission(ResourceType.INQUIRY, ...)` 기반 권한 검증으로 `InquiryAccessGuard` 대체.

## References

- 관련 ADR
    - 본 ADR 이 첫 도메인 도입이므로 직접 의존하는 선행 ADR 은 없음.
- 기존 코드 / 컨벤션
    - [CLAUDE.md §2 Architecture & Domain Rules](../../CLAUDE.md) — Hexagonal / CQRS / read 메서드 명명 / `@OneToMany` 금지
    - [Domain](../../src/main/java/com/umc/product/global/exception/constant/Domain.java)
    - [ResourceType](../../src/main/java/com/umc/product/authorization/domain/ResourceType.java)
    - [FileCategory](../../src/main/java/com/umc/product/storage/domain/enums/FileCategory.java)
    - [ChallengerRoleType](../../src/main/java/com/umc/product/common/domain/enums/ChallengerRoleType.java)
    - [GetChallengerRoleUseCase](../../src/main/java/com/umc/product/authorization/application/port/in/query/GetChallengerRoleUseCase.java)
    - [FcmAudienceService](../../src/main/java/com/umc/product/notification/application/service/FcmAudienceService.java) (FCM 발송 진입점)
    - [SecurityConfig](../../src/main/java/com/umc/product/global/config/SecurityConfig.java)
    - [dependencies.gradle.kts](../../gradle/dependencies.gradle.kts) — `spring-boot-starter-websocket` 선언
- 외부 문서
    - Spring WebSocket / STOMP 가이드: <https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html>
    - STOMP `ChannelInterceptor` 인증 / 인가 패턴: <https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication-token-based.html>
