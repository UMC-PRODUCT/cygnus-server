# ADR-027: 엔진 리소스 소유권을 opaque namespace registry로 고정한다

## Status

Accepted

Supersedes ADR-026 ([ADR-026](./026-separate-chat-engine-consumer-realtime-responsibilities.md))

## Context

2026년 7월 기준, Form과 Chat은 여러 소비 도메인이 재사용하는 내부 engine이다. Form은
Project application form, Notice vote, Feedback template처럼 서로 다른 business resource에
재사용되고, Chat은 consumer가 없는 standalone room도 지원해야 한다. 각 engine의 `formId`와
`chatRoomId` scalar는 빠른 navigation을 위해 기존 aggregate 또는 mapping에 남아 있지만, scalar만
검사해서는 누가 어떤 작업을 수행할 권한이 있는지 증명할 수 없다.

같은 시점에 Storage는 프로필, 이미지, 답변, 메시지 첨부 등 N:M 파일 사용 관계를 추적해야 한다.
파일 사용의 lifecycle과 엔진 리소스의 소유권은 cardinality, 삭제 규칙, 권한 의미가 다르므로 하나의
범용 registry로 합치면 각 불변식이 흐려진다. Storage가 런타임에 consumer entity/repository/use case를
조회하는 방식은 Hexagonal Architecture의 의존 방향을 거꾸로 만들고, 새 소비 도메인을 추가할 때
Storage를 수정하게 만든다.

소유권을 단순히 `resourceType`과 숫자 ID로 외부에 공개하면 호출자가 다른 resource 또는 다른 engine
ID를 주입할 수 있다. 반대로 namespace와 owner coordinate를 서버가 결정하고 immutable ownership row로
보관하면, engine ID는 내부 navigation mirror로만 사용하면서 권한 판단의 원본을 한 곳에 둘 수 있다.

### 문제점

1. **소유권과 navigation을 혼동한다.** 소비 aggregate의 `formId`/`chatRoomId`는 조회 경로일 뿐이며,
   ownership row가 없거나 다른 owner를 가리키는 경우를 허용하면 리소스 탈취와 교차 도메인 접근이
   발생한다.
2. **권한은 operation마다 다르다.** 구조 변경, 발행, 응답, 읽기, 메시지 전송, 멤버십 변경은 서로
   다른 actor와 business 정책을 요구한다. namespace만 해석하는 단일 boolean ACL은 anonymous 응답,
   respondent credential, Chat membership 같은 기존 규칙을 보존하지 못한다.
3. **정책 registry의 누락과 중복이 안전하지 않다.** evaluator가 없거나 둘 이상이면 어떤 정책을
   적용할지 결정할 수 없으므로 allow가 아니라 fail-closed 해야 한다.
4. **역사적 데이터는 이미 모호할 수 있다.** Project/Notice의 기존 mapping에 중복 owner 후보가
   있으면 data migration이 임의의 row를 선택해서는 안 되며, DB constraint로 hard-fail하고 운영자가
   정리해야 한다.
5. **실시간 책임 경계가 약해질 수 있다.** Chat engine이 consumer별 destination이나 권한을 직접
   알게 되면 재사용 가능한 engine 경계가 무너지고, DB commit 전 broadcast 같은 일관성 오류가 재발한다.

### 결정이 필요한 이유

Form과 Chat 구현, Storage usage 구현, legacy data migration이 병렬로 진행되기 전에 권한 원본과 확장 지점을
고정해야 한다. 이 결정을 미루면 각 consumer가 namespace 문자열, owner ID, engine ID를 제각각 해석하고
부분적으로만 검증하게 되어 rolling cutover와 rollback을 안전하게 수행할 수 없다.

프로젝트의 아키텍처 규칙은 domain이 application/adapter를 참조하지 않고, 도메인 간에는 공개 UseCase와
ID만 사용하도록 요구한다. 따라서 Storage, Form, Chat은 각각 자신의 registry와 port를 소유하되,
consumer 정책은 engine core 밖의 SPI adapter로 주입해야 한다.

## Decision

우리는 **Storage FileUsageRegistry와 engine ownership registry를 분리하고, immutable ownership row를
authorization truth로 사용하며, consumer scalar를 navigation mirror로만 유지**하기로 결정한다.

### 1. Registry 경계를 분리한다

- **Storage FileUsageRegistry**는 파일과 owner coordinate 사이의 N:M snapshot, attach/detach, lifecycle
  timestamp, cleanup claim을 소유한다. Storage 런타임은 consumer table/entity/repository/application
  service를 import하거나 조회하지 않는다. 기존 테이블을 읽는 SQL은 배포 시점의 Flyway data migration에만
  존재한다.
- **Form ownership registry**는 `formId`를 engine ID로 하는 Form 소유권 binding을 소유한다. Form
  domain/application에는 Project·Notice·Feedback 타입이나 공용 generic ownership entity를 두지 않고,
  consumer가 `FormOwnerPolicy` SPI로 자신의 정책을 제공한다.
- **Chat room ownership registry**는 `roomId`를 engine ID로 하는 Chat 소유권 binding을 소유한다.
  Chat core에는 consumer enum, consumer package import, consumer type switch를 넣지 않는다. consumer가
  `ChatRoomOwnerPolicy` SPI로 정책을 확장한다.

두 ownership registry와 Storage usage registry는 cardinality와 삭제·권한 불변식이 다르므로 테이블,
aggregate, port, migration을 공유하지 않는다.

### 2. Opaque ownership coordinate와 불변식

모든 ownership row와 FileUsage owner row의 공개 가능한 내부 좌표는 다음 opaque tuple이다.

```text
(namespace, ownerResourceKey, slot)
```

- `namespace`는 소문자 segment를 점(`.`)으로 연결한 이름이다. 각 segment는
  `[a-z][a-z0-9-]{0,31}`, 전체 길이는 100자 이하다.
- `ownerResourceKey`는 `[A-Za-z0-9][A-Za-z0-9._:-]{0,127}` 형식의 opaque 문자열이다.
- `slot`은 `[a-z][a-z0-9-]{0,49}` 형식의 stable 의미 이름이다.
- tuple은 서버의 trusted factory가 resource를 검증한 뒤 생성하며 client request, STOMP destination,
  GraphQL input에서 namespace/ownerResourceKey/slot을 받지 않는다.
- ownership binding은 insert-only immutable이다. 같은 engine ID에 다른 tuple을 덮어쓰거나 tuple의
  owner를 transfer하지 않는다. 동일 값의 재시도만 idempotent하게 허용한다.
- `(namespace, ownerResourceKey, slot)`은 한 engine ID에만 연결되고, 한 engine ID도 한 ownership row만
  가진다. 중복 후보는 애플리케이션에서 선택하지 않고 DB constraint로 migration을 hard-fail한다.

`formId`와 `chatRoomId`는 이 tuple의 navigation mirror다. mirror를 먼저 믿어 ownership row를 생성하거나
권한을 우회하지 않으며, 생성/변경 시 engine row, scalar mirror, ownership row는 하나의 REQUIRED
transaction에서 함께 기록한다. row와 mirror가 불일치하면 read와 mutation 모두 fail-closed한다.

### 3. Operation-aware exact-one policy SPI

각 engine은 operation과 actor context를 포함하는 policy port만 호출한다. policy registry는 namespace를
지원하는 evaluator를 **정확히 하나**로 요구한다.

- evaluator 0개(미등록 namespace), 2개 이상(중복 등록), malformed tuple, missing ownership row,
  expected owner 불일치, actor context 누락은 모두 `false`/도메인 거부로 끝낸다.
- policy는 `namespace`, immutable owner reference, operation, authenticated actor, 필요한 access key나
  response credential을 받아 판단한다. client가 전달한 owner/actor를 신뢰하는 fallback overload를 만들지
  않는다.
- ownership row 확인은 authorization의 첫 단계이며, consumer policy가 business 상태·역할을 추가로
  확인한다. policy 성공만으로 engine membership이나 respondent 권한을 대신하지 않는다.
- namespace coverage 검증은 persisted namespace뿐 아니라 선언된 policy namespace와 engine-native
  standalone policy의 합집합에 대해 수행한다. 누락/unknown namespace가 있으면 readiness를 열지 않는다.

#### Form operation matrix

| Namespace | `MANAGE_STRUCTURE`/`PUBLISH`/`DELETE`/`READ_RESPONSES` | `READ` | `RESPOND` |
|---|---|---|---|
| `project.application-form` | Project가 server-resolve한 owner만 허용 | Project resource의 공개/상태 정책 | 기존 Form 응답 자격과 Project 정책 |
| `notice.vote` | Notice가 server-resolve한 owner만 허용 | Notice 공개/투표 기간 정책 | 투표 자격·중복 투표 정책 |
| `feedback.template` | Feedback template owner 정책 | template 공개/상태 정책 | response credential 및 기존 respondent 정책 |
| `form.standalone` | `createdMemberId`만 허용 | 공개 상태·access key 등 기존 규칙 | 기존 공개 상태·access key·응답 자격 규칙 |

모든 section/question/option/answer/response child operation은 parent chain으로 root `formId`를
resolve한 뒤 같은 ownership assertion을 적용한다. anonymous `READ`/`RESPOND`의 기존 계약은 유지하지만
management와 `READ_RESPONSES`는 authenticated owner를 요구한다.

#### Chat standalone operation matrix

`chat.standalone/{roomId}/default` ownership policy는 engine-native 방에만 적용한다.

| Operation | 허용 조건 |
|---|---|
| `CREATE` | authenticated actor가 room 생성 요청을 수행하고 새 ownership row·creator membership이 같은 transaction에서 생성됨 |
| `READ` / `SEND` | ownership row의 expected owner가 맞고, 요청 actor가 해당 Chat room member임 |
| `JOIN` / `LEAVE` | actor와 target이 동일한 자기 자신인 경우만 허용. membership mutation은 별도 Chat 불변식을 통과해야 함 |
| `PIN` / `UNPIN` / `DELETE` | standalone policy에서 거부 |
| `MEMBERSHIP_MANAGE` | standalone policy에서 거부. 타인 초대/강제 퇴장도 포함 |

named Form namespace와 future Chat consumer namespace도 같은 operation-aware SPI를 사용하지만, 소비
도메인 정책을 Chat/Form core switch로 구현하지 않는다. 모든 Chat data-plane `READ`/`SEND`는 ownership
policy와 **Chat membership 및 consumer business permission을 모두** 만족해야 한다.

### 4. 외부 계약과 실시간 흐름을 보호한다

- 이 ADR은 기존 Form/Chat REST/GraphQL/STOMP API 계약을 즉시 제거하거나 breaking change하지 않는다.
  다만 **새 외부 REST/GraphQL/STOMP 계약으로** raw engine `roomId`/`formId`, owner coordinate, 호출자
  namespace를 추가 노출하지 않는다. 신규 ownership 경로는 consumer resource key 또는 server-generated
  route를 사용하며, 기존 계약의 raw field 전환은 별도 contract migration에서 수행한다. raw `roomId`를
  받아 임의의 방을 선택하는 새 overload는 제공하지 않는다.
- consumer inbound adapter는 자신의 facade Port In만 호출하고, facade application service가 ownership
  policy, consumer business permission, 공개 Chat/Form UseCase를 조합한다. consumer가 engine entity나
  repository를 직접 참조하지 않는다.
- Chat engine은 채팅 불변식, membership, message/pin event만 소유한다. consumer destination/payload,
  consumer 후속 상태, broadcast 호출은 consumer facade/listener와 global `BroadcastPort`가 소유한다.
- Chat event listener는 `AFTER_COMMIT`에서 consumer Port In으로 변환해 전달한다. listener와 facade는
  `findByChatRoomId` 역매핑으로 자기 소유 room만 처리하고, 소유하지 않은 event는 예외나 broadcast 없이
  무시한다. broadcast는 best-effort이며 조회 API와 client backfill이 source of truth다.
- global WebSocket registry는 exact destination을 지원하는 authorizer가 하나이고 승인한 경우에만
  SUBSCRIBE를 허용한다. malformed destination, 인증 주체 없음, authorizer 없음/복수 매칭은 fail-closed다.
  공통 계층은 위임과 broker 보호만 하며 consumer policy를 직접 분기하지 않는다.

### 5. Flyway data migration과 enable 안전장치

Project/Notice의 역사적 mapping을 ownership row로 옮길 때 동일 engine에 복수 owner가 있거나 동일
`(namespace, ownerResourceKey, slot)`에 복수 engine이 있으면 **PK/unique/FK constraint로 Flyway migration을
hard-fail**한다. 어떤 후보를 임의로 선택하거나 `ON CONFLICT DO NOTHING`으로 숨기지 않는다. Storage의
broken file reference와 pending upload reference도 migration helper table의 constraint로 차단한다.

기존 consumer table SQL은 application adapter가 아니라 Flyway가 소유한다. Flyway transaction이 실패하면
application startup도 실패하며 원본 데이터를 정리한 뒤 같은 artifact를 다시 적용한다. Flyway 성공만으로는
동시에 실행 중인 구버전 writer를 증명할 수 없으므로 Storage cutover는 `PENDING`으로 시작한다. maintenance
write barrier에서 구버전 writer를 모두 drain하고 9개 source와 usage registry의 invalid/duplicate/broken
reference 및 양방향 차집합이 0인 guarded SQL만 `READY`를 기록한다. application adapter가 backfill이나
consumer table reconciliation을 실행하지 않는다. Form/Chat은 ownership row 누락을 AUDIT에서도 기록 후
거부하고, exact-one namespace coverage를 함께 사용한다. 관측 window가 끝나기 전에는 cleanup과 strict
ownership enforcement를 켜지 않는다. 이후 rollback은 property를 먼저 끄고 forward migration으로 보정한다.

## Superseded / Retained from ADR-026

ADR-027은 ADR-026의 Proposed 결정을 대체하지만, 다음 보호 규칙과 책임 경계는 그대로 유지한다.

### Retain

- Chat engine은 consumer type과 business aggregate를 알지 않고 채팅 불변식, membership, domain event를
  소유한다.
- consumer facade/application service가 resource→engine mapping과 consumer 권한을 검증하고 public Chat
  UseCase만 호출한다. consumer entity/repository를 cross-domain으로 직접 참조하지 않는다.
- consumer가 destination과 payload, `BroadcastPort`를 소유하고, Chat event listener는 `AFTER_COMMIT`에서
  facade Port In으로 위임한다. broadcast 실패는 저장 transaction을 rollback하지 않으며 client backfill로
  복구한다.
- 외부 요청에 raw engine ID를 받지 않는 보호, exact destination parsing, global authorizer registry의
  fail-closed 원칙, Chat membership과 consumer permission의 이중 검사를 유지한다.

### Supersede / Clarify

- ADR-026은 consumer aggregate의 scalar `chatRoomId` mapping을 주된 관계로 설명했으나, 이제 immutable
  ownership row가 authorization truth이고 scalar는 navigation mirror다.
- ADR-026의 consumer별 ad-hoc mapping은 Form/Chat ownership registry와 operation-aware exact-one policy
  SPI로 표준화한다. 기존 consumer는 `project.application-form`, `notice.vote`, `feedback.template`
  policy adapter로 이행하고, owner 중복은 Flyway constraint에서 중단한다.
- engine-native `form.standalone`과 `chat.standalone`의 operation matrix를 명시한다. 특히 standalone
  Chat의 `PIN`, `UNPIN`, `DELETE`, `MEMBERSHIP_MANAGE`는 허용하지 않는다.
- Storage FileUsageRegistry는 engine ownership과 별개이며, Storage runtime consumer lookup은 금지한다.

## Alternatives Considered

### 대안 A: consumer scalar만 authorization source로 사용

각 consumer의 `formId`/`chatRoomId`를 조회해 owner와 권한을 판단하고 별도 row를 만들지 않는 방식이다.

장점:

- migration과 저장 모델이 작다.
- 기존 service가 가진 scalar를 그대로 재사용할 수 있다.

단점:

- scalar가 stale하거나 다른 resource를 가리킬 때 권한 우회가 가능하다.
- namespace별 operation 정책과 duplicate detection을 공통으로 검증하기 어렵다.
- data migration에서 역사적 중복을 숨기고 임의의 owner를 선택할 위험이 있다.

선택하지 않은 이유:

소유권을 명시적으로 잠그고 일관성을 검증할 원본이 필요하므로 scalar는 navigation mirror로 제한한다.

### 대안 B: Storage와 Form/Chat ownership을 하나의 polymorphic registry로 통합

파일 사용과 엔진 소유권을 하나의 `resource_type/resource_id` 테이블과 generic ACL로 처리하는 방식이다.

장점:

- registry 조회 API와 migration 수를 줄일 수 있다.
- 모든 리소스를 한 화면에서 관찰하기 쉽다.

단점:

- Storage N:M usage와 engine 1:1 ownership의 cardinality·lifecycle·삭제 규칙이 섞인다.
- generic ACL이 Chat membership, Form respondent credential, file cleanup claim을 표현하기 어렵다.
- Storage가 consumer 모델을 알아야 하거나 공용 enum/의존성이 생긴다.

선택하지 않은 이유:

서로 다른 불변식은 별도 registry로 유지해야 Hexagonal 의존 방향과 운영 cutover를 독립적으로 통제할 수
있다.

### 대안 C: Chat/Form core가 namespace와 consumer type을 직접 switch

engine core가 `Project`, `Notice`, `Feedback` 등의 종류를 분기해 owner와 권한을 직접 조회하는 방식이다.

장점:

- 정책 호출 경로가 한 service에 모인다.
- consumer adapter 코드가 줄어드는 것처럼 보인다.

단점:

- engine이 consumer entity/repository와 결합되고 새 consumer마다 core 수정이 필요하다.
- global/engine 계층에 business 권한이 누적되어 bounded context 경계가 무너진다.
- raw engine ID와 consumer destination을 core가 조립하게 될 가능성이 크다.

선택하지 않은 이유:

consumer 정책은 SPI adapter와 facade에 두고 engine core는 고정된 불변식만 소유해야 재사용성과 fail-closed
확장을 동시에 얻을 수 있다.

## Consequences

### Positive

- ownership row가 하나의 authorization truth가 되어 stale scalar와 역사적 중복을 탐지할 수 있다.
- namespace와 owner coordinate가 server-derived opaque tuple이므로 raw engine ID·호출자 namespace 주입을
  차단한다.
- operation-aware exact-one evaluator가 누락·중복 정책을 fail-closed로 처리해 새 consumer 추가 시
  readiness 검증을 강제한다.
- Storage cleanup과 Form/Chat 권한을 독립적으로 rollout·rollback할 수 있다.
- ADR-026의 consumer facade, AFTER_COMMIT broadcast, membership 이중 검증과 migration 보호를 유지하면서
  standalone Chat/Form의 계약도 명시된다.

### Negative

- Form과 Chat에 각각 ownership row, persistence adapter, policy SPI, lock 및 migration을 추가해야 한다.
- consumer scalar, ownership row, engine aggregate를 한 transaction에서 함께 기록해야 하므로 구현·운영
  복잡도가 증가한다.
- exact-one registry와 Flyway constraint가 실패하면 신규 consumer 또는 legacy data가 readiness를 통과하지
  못하고 수동 정리가 필요하다.
- broadcast는 여전히 best-effort라서 consumer 조회 API, deduplication, metric과 운영 알람을 유지해야 한다.

### Neutral / Trade-offs

- owner coordinate는 opaque라서 운영자가 row만 보고 의미를 알기 어렵지만, namespace별 source adapter와
  audit log를 통해 추적할 수 있다.
- immutable ownership은 owner transfer를 단순 update로 처리할 수 없게 한다. transfer가 필요하면 기존
  binding을 명시적으로 폐기한 별도 migration/operation을 설계해야 한다.
- standalone Chat에서 membership이 있는 사용자도 policy가 금지한 pin/delete/membership 관리에는
  접근할 수 없다. 이는 engine 기능의 축소를 의도한 안전한 기본값이다.

## Implementation Notes

### 단계적 진행 / PR 분할

- **Phase 1 (이 ADR과 schema expand)**: Storage FileUsageRegistry, FormOwnership, ChatRoomOwnership,
  namespace grammar, immutable constraints와 port 계약을 추가한다. cleanup/strict enforcement는 끈다.
- **Phase 2 (consumer dual-write)**: Project·Notice·Feedback의 trusted owner factory와 Form/Chat facade,
  operation policy, consumer scalar+ownership transaction을 적용한다. Chat event는 기존처럼 AFTER_COMMIT
  consumer broadcast를 사용한다.
- **Phase 3 (Flyway data migration)**: Storage의 현재 9개 source와 Form ownership mapping을 set-based SQL로
  이관하고 Chat의 미도입 데이터를 비운다. broken/pending/duplicate mapping은 constraint로 hard-fail한다.
- **Phase 4 (certify/enable)**: 구버전 writer drain 뒤 Storage exact reconciliation이 `READY`이고 Form/Chat
  namespace coverage가 exact-one일 때만 strict ownership enforcement와 tokenized cleanup을 활성화한다.
  missing ownership은 AUDIT에서도 허용하지 않는다. rollback은 property off 후 forward migration을 원칙으로 한다.

### 변경 영역 요약

1. **Storage domain/application** (`com.umc.product.storage.*`): FileUsageRegistry와 cleanup lifecycle을
   소유하며 runtime consumer lookup은 금지한다.
2. **Form domain/application** (`com.umc.product.form.*`): Form ownership row, `FormOperation`, policy
   registry/ports를 소유한다. Project·Notice·Feedback 의존은 consumer adapter를 통해서만 들어온다.
3. **Chat domain/application** (`com.umc.product.chat.*`): Chat room ownership row, `ChatRoomOperation`,
   membership/access policy를 소유한다. consumer switch/import는 금지한다.
4. **Consumer adapters** (`project`, `notice`, `feedback`): trusted namespace/owner factory와 Form policy
   adapter, dual-write transaction을 소유한다.
5. **Global WebSocket** (`com.umc.product.global.websocket.*`): JWT, broker 보호, exact-one authorizer
   dispatch, generic `BroadcastPort`만 소유한다.
6. **DB/migration** (`src/main/resources/db/migration/V*__*.sql`): 세 registry의 독립 schema, 기존 데이터 이관,
   unique/FK/grammar constraint validation을 한 Flyway 순서로 배포한다.
7. **Test/운영** (`src/test/...`, `docs/onboarding/...`): operation matrix, duplicate hard-fail, migration rollback,
   AFTER_COMMIT broadcast, readiness/cleanup gate를 검증한다.

### 구현 체크리스트

- [ ] 모든 public Form/Chat command/query가 server-resolved owner와 actor context를 요구하는가?
- [ ] ownership row가 없거나 scalar mirror와 다르면 fail-closed하는가?
- [ ] policy evaluator 수가 namespace·operation별로 정확히 하나인가?
- [ ] Chat data-plane READ/SEND가 membership와 consumer permission을 모두 확인하는가?
- [ ] 새 외부 DTO와 destination에 raw engine ID, owner coordinate, 호출자 namespace를 추가하지 않는가?
- [ ] Storage runtime이 consumer entity/repository/application service를 import/query하지 않는가?
- [ ] Project/Notice historical duplicate와 broken Storage reference가 Flyway constraint로 hard-fail하는가?
- [ ] event listener가 AFTER_COMMIT이고 broadcast가 source of truth가 아닌가?

## References

- [ADR-026: Chat engine과 소비 도메인의 실시간 책임을 분리한다](./026-separate-chat-engine-consumer-realtime-responsibilities.md)
- [ADR-018: DomainEventPublisher 추상화](./018-abstract-spring-event-publisher-for-future-broker.md)
- [ADR-019: Transactional Event Outbox](./019-introduce-transactional-event-outbox.md)
- `src/main/java/com/umc/product/global/websocket/application/service/StompSubscriptionAuthorizerRegistry.java`
- `src/main/java/com/umc/product/global/websocket/application/port/in/StompSubscriptionAuthorizer.java`
