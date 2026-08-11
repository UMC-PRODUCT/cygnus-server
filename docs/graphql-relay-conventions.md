# GraphQL Relay 서버 컨벤션

이 문서는 본 서버의 GraphQL API가 따르는 [Relay 서버 스펙](https://relay.dev/docs/guides/graphql-server-specification/)
적용 규칙을 정의한다. 공용 구현은 `com.umc.product.global.graphql.relay` 패키지에 있다.

## 1. 전역 ID (Global Object Identification)

- 스키마의 모든 `ID` 값(출력 필드, 인자, input 필드)은 불투명 전역 ID다.
  `GlobalId.encode(typeName, rawId)` = `base64url("{TypeName}:{rawId}")`.
- 타입 이름은 반드시 `GlobalIdTypes` 상수를 사용한다. 도메인 간 동일 엔티티(예: `memberId`)는
  동일 상수로 인코딩해야 한다. 누락된 타입은 상수를 추가한다.
- 어댑터 경계에서만 인코딩/디코딩한다. UseCase/도메인 레이어에는 raw `Long` ID만 전달한다.
  - 인자 디코딩: `GlobalId.decodeLong(value, GlobalIdTypes.X)` — 타입 불일치 시 `IllegalArgumentException`.
  - GraphQL request 레코드는 `String` ID 필드를 받고 `toQuery()`/`toCommand()`에서 디코딩한다.

## 2. Node 인터페이스

- `relay.graphqls`가 기본 `type Query { node, nodes }`, `interface Node`, `type PageInfo`를 정의한다.
  각 도메인 스키마는 반드시 `extend type Query`를 사용한다.
- 단건 재조회 UseCase가 존재하는 엔티티 타입만 `implements Node`를 선언한다.
  Node 타입의 `id: ID!`는 전역 ID이고, 필드 이름은 반드시 `id`다.
- Node 타입의 응답 DTO는 `RelayNode`를 구현한다(`String id()` — 인코딩된 전역 ID 반환).
  구체 타입 결정은 전역 ID에 내장된 타입 이름으로 이뤄지므로, DTO의 `id`는 반드시
  스키마 타입 이름과 동일한 `GlobalIdTypes` 상수로 인코딩해야 한다.
- 도메인별 `NodeFetcher` 구현을 어댑터 패키지에 `@Component`로 등록한다.
  기존 단건 쿼리(예: `member(id:)`)와 **동일한 권한 검증**을 수행해야 하며,
  대상이 없으면 null을 반환한다(NotFound 예외를 던지지 않는다).
- 한 요청에서 같은 전역 ID로 조회한 Node는 어디서 조회하든 같은 필드 값을 반환해야 한다.
  요청자에 따라 공개 범위를 달리하는 타입도 `node(id:)`, 단건 쿼리, 현재 사용자 쿼리 간 표현을 맞춘다.
- Node가 아닌 타입은 `id`라는 이름의 필드를 두지 않는다. 참조 필드는 `xxxId` 이름을 유지하되
  값은 참조 대상 타입의 전역 ID로 인코딩한다.
- 같은 타입에 `xxxId` 스칼라와 `xxx` 객체 필드가 공존하면 스칼라를 제거하고 객체 필드만 남긴다.
  (배치 로딩에 필요한 raw ID는 DTO 레코드 컴포넌트로 유지하되 스키마에 노출하지 않는다.)

## 3. Connection 페이지네이션

- 루트 필드가 엔티티 목록을 반환하면 Connection으로 노출한다. 중첩된 소규모 목록은 일반 리스트를 유지한다.
- 페이지네이션 인자는 `first: Int, after: String, last: Int, before: String`이며
  기존 `page/size` input 필드는 모두 제거한다. 필터 input에서 페이지 관련 필드도 제거한다.
- `first`와 `last`는 0 이상 100 이하로 제한한다. 둘 다 전달되면 Relay 알고리즘대로
  cursor 범위를 적용한 뒤 `first`, `last` 순서로 슬라이싱한다.
- SDL 패턴:

  ```graphql
  type XConnection {
    edges: [XEdge!]!
    pageInfo: PageInfo!
    totalCount: Long!
  }

  type XEdge {
    cursor: String!
    node: X!
  }
  ```

  Connection 타입에 도메인 특화 필드(예: `asOf`)를 추가해도 된다(스펙 허용).
- 컨트롤러 구현:
  - 인자: `@Argument Integer first, @Argument String after, @Argument Integer last, @Argument String before`
    → `ConnectionArguments.of(first, after, last, before)`.
  - 저장소 페이지 기반: `arguments.toPageable(totalSizeSupplier)`(= `OffsetPageRequest`)로 조회 후
    `RelayConnection.fromPage(page, arguments, mapper)`. `last`가 있고 `before`가 없을 때만
    공급자를 호출해 전체 건수를 확인한다. `first: 0`의 최소 조회 보정은 Connection 변환 시 제거한다.
    저장소 QueryRepository가 `pageable.getOffset()`을 사용하는지 확인하고,
    `getPageNumber() * getPageSize()`로 offset을 계산하면 `getOffset()` 사용으로 고친다.
  - 전체 목록 기반: `RelayConnection.fromList(items, arguments, mapper)`.
- 제네릭 `RelayConnection<T>`를 그대로 반환하면 스키마의 `XConnection`에 매핑된다.
  추가 필드가 필요한 Connection만 전용 응답 레코드를 만든다(내부에 edges/pageInfo/totalCount 유지).

## 4. Mutation

- 모든 뮤테이션은 단일 non-null `input` 인자만 받는다. 경로성 인자(`seasonId`, `roundId` 등)는
  input 객체 안으로 옮긴다.
- 뮤테이션별 전용 Payload 타입을 반환한다: `<MutationName>Payload`.
  - 생성/변경 대상 노드를 싸게 재조회할 수 있으면 노드 필드를 담는다.
  - 이전에 ID만 반환하던 뮤테이션은 대상의 전역 ID 필드(예: `seasonId: ID!`)를 담는다.
  - 이전에 `Boolean!`을 반환하던 뮤테이션은 `success: Boolean!` 필드를 담는다.
- `clientMutationId`는 사용하지 않는다(modern Relay).

## 5. 기타

- 스칼라 타입(`Instant`, `Long`, `String` 날짜 필드 등)은 기존 그대로 유지한다. 단, 엔티티 ID가 아닌
  값이 `ID` 타입으로 선언돼 있으면(예: 기수 `generation`) 의미에 맞는 스칼라(Int 등)로 바로잡는다.
- 헥사고날 규칙 유지: 이 개편은 `adapter/in/graphql`과 스키마 파일 안에서만 이뤄진다.
  UseCase/Port 시그니처는 필요 최소한(Pageable 전달 등)으로만 손댄다.
- 권한 검증, 레이트리밋, 익명 플로우 등 기존 시맨틱은 그대로 보존한다.
