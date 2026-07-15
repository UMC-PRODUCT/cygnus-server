# ADR-006: UMC PRODUCT는 Chapter와 기간 기반 활동 이력으로 관리한다

## Status

Accepted

## Context

UMC PRODUCT 멤버는 더 이상 별도 기수에 소속되지 않는다. 한 멤버의 활동은 중간에 중단되었다가 다시 시작될 수 있고, Chapter 소속, Product Leadership, Squad 참여 역시 각자 유효 기간을 가진다. 따라서 `UmcProductGeneration`과 기수별 `UmcProductFunctionalUnit` tree로는 실제 이력을 정확히 표현할 수 없다.

UMC PRODUCT의 고정 기능 조직은 Chapter 한 단계로만 구성된다. Chapter 하위 Part와 Part Lead 역할은 두지 않는다. Product 전체의 Lead와 Vice Lead는 Chapter 소속과 다른 책임이므로 별도 이력으로 관리한다.

활동 기간은 시각이 아니라 달력 날짜의 의미를 가진다. 감사 시각인 `createdAt`, `updatedAt`과 달리 시간대 변환이 필요하지 않으므로 Java `LocalDate`와 PostgreSQL `DATE`를 사용한다.

이 모델은 `organization` 도메인 하위에 두되 다음 기존 모델과 혼합하지 않는다.

- 전역 `Gisu`와 `Chapter`는 UMC 전체 조직을 위한 기존 모델이며 그대로 유지한다.
- UMC PRODUCT는 별도 `UmcProductChapter`를 사용한다.
- 멤버 도메인은 aggregate가 아닌 `memberId`로만 참조한다.
- 부모 aggregate에 `@OneToMany`를 추가하지 않고 자식이 FK를 소유한다.

## Decision

### 1. 날짜 계약

- 모든 UMC PRODUCT 활동 이력은 필수 `startDate`와 nullable `endDate`를 가진다.
- 날짜 범위는 양 끝을 포함하며 같은 날 시작하고 종료할 수 있다.
- `endDate == null`은 진행 중을 뜻하고 `endDate < startDate`는 거부한다.
- Java와 JSON에서는 `LocalDate`/`yyyy-MM-dd`, DB에서는 `DATE`를 사용한다.
- 활동 날짜를 `Instant`로 변환하지 않는다. `BaseEntity`의 감사 시각은 기존 `Instant`를 유지한다.
- 현재 날짜가 필요한 권한과 조직도 조회는 주입된 `Clock`으로 `Asia/Seoul`의 날짜를 계산한다. 전역 `TimeConfig`의 UTC Clock 설정은 변경하지 않는다.

### 2. 멤버 활동 기간

`UmcProductMemberActivityPeriod`는 `UmcProductMember`에 N:1로 속하고 멤버가 실제로 활동한 기간을 저장한다.

- 한 멤버의 기간은 서로 겹칠 수 없다.
- 두 기간 사이에 비활동 날짜가 없으면 별도 행으로 나누지 않고 하나의 기간으로 입력한다.
- Chapter 소속, Product Leadership, Squad 참여의 기간은 하나의 멤버 활동 기간에 완전히 포함되어야 한다.
- 클라이언트는 하위 활동의 멤버 기간 ID를 지정하지 않는다. 서버가 기간으로 유일한 부모를 결정한다.
- 멤버 기간 축소나 삭제로 하위 활동이 범위를 벗어나면 요청을 거부한다.

### 3. Chapter

UMC PRODUCT의 기능 조직은 일반화된 tree가 아니라 Chapter 단일 계층으로 모델링한다.

- `UmcProductChapter`는 `code`, `name`, `description`, `sortOrder`, `isActive`를 가진다.
- Chapter code는 UMC PRODUCT Chapter 전체에서 유일하다.
- 과거를 포함한 소속 이력이 있는 Chapter는 삭제할 수 없으며 비활성화를 사용한다.
- Chapter는 현재형 조직 카탈로그다. 이름 변경은 과거 이력 조회에도 반영하며 과거 조직 snapshot은 저장하지 않는다.

### 4. Chapter 소속

`UmcProductChapterMembership`은 멤버 활동 기간, Chapter, position, 책임 정보와 자체 활동 기간을 저장한다.

- Chapter 소속에는 별도 role을 두지 않는다.
- 한 멤버가 같은 날짜에 여러 Chapter에서 활동할 수 있다.
- 동일한 멤버의 동일한 Chapter 소속 기간은 겹칠 수 없다.
- position과 책임 정보는 Chapter 활동 이력에 기록한다.

### 5. Product Leadership

`UmcProductLeadership`은 멤버 활동 기간, `UMC_PRODUCT_LEAD` 또는 `UMC_PRODUCT_VICE_LEAD`, 자체 활동 기간을 저장한다.

- 각 역할은 같은 날짜에 한 명만 유효할 수 있다.
- 한 멤버가 Lead와 Vice Lead를 같은 날짜에 겸임할 수 없다.
- Chapter 소속과 Leadership은 독립적으로 생성·수정한다. 서로의 존재나 기간을 검증하지 않는다.

### 6. Squad

`UmcProductSquad`는 필수 `startDate`, nullable `endDate`와 표시·관리 정보를 가진다. `UmcProductSquadParticipant`는 멤버 활동 기간, Squad, role, position, 책임 정보와 자체 활동 기간을 가진다.

- 참여 기간은 멤버 활동 기간과 Squad 기간에 모두 포함되어야 한다.
- 동일 멤버의 동일 Squad 참여 기간은 역할과 무관하게 겹칠 수 없다.
- `SQUAD_LEAD`는 Squad별 같은 날짜에 한 명만 유효할 수 있다.
- Squad 기간 축소로 참여 이력이 범위를 벗어나면 요청을 거부한다.

### 7. 무결성과 동시성

- 애플리케이션은 멤버 행을 비관적 잠금한 뒤 멤버 기간과 하위 활동을 검증한다.
- Squad 참여 변경은 멤버와 Squad를 일관된 순서로 잠근다.
- PostgreSQL `btree_gist`와 `daterange` exclusion constraint로 기간 중첩과 날짜별 단일 리더 규칙을 동시 요청에서도 보장한다.
- 부모 기간 포함처럼 단일 행 제약으로 표현할 수 없는 규칙은 같은 transaction 안의 애플리케이션 검증으로 보장한다.

### 8. API와 권한

- `/api/v1/umc-product/generations/**`, `/functional-units/**`, `/parts/**`와 전체 교체 방식 membership/participant API를 제거한다.
- Chapter, 멤버 활동 기간, Chapter 소속, Product Leadership, Squad 참여를 각각 개별 CRUD로 제공한다.
- Chapter 소속 API는 `/members/{memberId}/chapter-memberships[/{chapterMembershipId}]`를 사용한다.
- 멤버와 Squad 검색의 선택적 `activeOn`은 해당 날짜에 유효한 이력만 대상으로 한다. 생략하면 전체 이력을 대상으로 한다.
- 조직도는 활성 Chapter 목록과 KST 오늘 기준 유효한 Squad를 반환하며 과거 `asOf` tree는 제공하지 않는다.
- 중앙 운영진의 기존 override와 본인 프로필 수정 권한은 유지한다.
- 그 외 관리 권한은 오늘 유효한 멤버 활동 기간과 Product Lead/Vice Lead 이력으로만 판단한다. Chapter 소속은 권한 조건이 아니다.

## Alternatives Considered

### 대안 A: UMC PRODUCT 기수를 유지한다

선택하지 않았다. 멤버 활동은 기수 경계와 일치하지 않고 중단과 재개가 가능하며, Squad도 기수에 종속되지 않는다.

### 대안 B: Chapter와 Part를 계층으로 유지한다

선택하지 않았다. 현재 조직에는 Part 계층과 Part Lead 역할이 없으므로 코드와 데이터 모델에 존재하지 않는 구조를 남기게 된다.

### 대안 C: Chapter를 일반화된 Functional Unit tree로 유지한다

선택하지 않았다. 단일 Chapter 계층에 불필요한 unit type과 parent 관계를 도입해 허용하지 않는 상태까지 표현하게 된다.

### 대안 D: Product Leadership을 Chapter 소속에서 파생한다

선택하지 않았다. Product 전체 책임과 Chapter 활동은 독립적으로 변경될 수 있으며 둘의 기간이 항상 일치한다는 보장이 없다.

### 대안 E: 활동 기간을 `Instant`로 저장한다

선택하지 않았다. 활동의 의미는 특정 시각이나 시간대가 아니라 포함 경계를 가진 날짜다. `Instant`를 사용하면 KST 자정 변환과 DB/JVM time zone 차이로 날짜가 달라질 수 있다.

## Consequences

### Positive

- 멤버의 중단·재개와 각 활동의 실제 기간을 손실 없이 기록한다.
- 코드와 API가 Chapter 단일 계층과 실제 존재하는 역할만 표현한다.
- Product Leadership과 Chapter 소속의 독립적인 변경 이력을 보존한다.
- 날짜 계약이 시간대 설정과 분리되어 JSON, Java, DB 왕복 시 같은 달력 날짜를 유지한다.

### Negative

- 활동 이력별 테이블과 CRUD가 늘어나며 상위 기간 포함 검증이 필요하다.
- 기간 중첩과 날짜별 리더 유일성 때문에 PostgreSQL `btree_gist` 확장에 의존한다.
- 현재형 Chapter 이름을 수정하면 과거 활동 표시도 함께 바뀐다.

## Implementation Notes

- 기존 UMC PRODUCT 데이터와 v1 계약은 폐기 가능하므로 백필, `Instant` 변환, dual-read, 호환 API를 제공하지 않는다.
- 새 migration은 기존 UMC PRODUCT 전용 테이블만 초기화하고 다음 7개 테이블을 생성한다: member, member activity period, chapter, chapter membership, leadership, squad, squad participant.
- 전역 Gisu, 일반 Chapter, Challenger, Project, Schedule과 GraphQL schema는 변경하지 않는다.
- 하위 활동 FK는 삭제 제한을 기본으로 하며 전체 멤버 삭제는 application service가 의존 순서대로 명시적으로 수행한다.

## References

- Java `LocalDate`: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDate.html
- PostgreSQL 날짜 타입: https://www.postgresql.org/docs/current/datatype-datetime.html
- PostgreSQL 범위 타입: https://www.postgresql.org/docs/current/rangetypes.html
- 재구축 migration: `src/main/resources/db/migration/V2026.07.13.00.01__rebuild_umc_product_activity_period_model.sql`
