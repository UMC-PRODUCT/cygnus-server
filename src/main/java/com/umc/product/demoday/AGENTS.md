# DEMODAY 도메인 지침

이 파일은 `src/main/java/com/umc/product/demoday/**`에 적용된다. 공통 레이어 규칙은 루트 `AGENTS.md`를 따르고, DEMODAY 테스트에는 `src/test/java/com/umc/product/demoday/AGENTS.md`를 추가로 적용한다.

## 도메인 경계

DEMODAY는 기수별 데모데이 투표를 운영한다.

- `DemodayPoll`은 투표의 기수, 이름, 운영 상태와 투표 기간을 관리하는 루트다.
- `DemodayBooth`는 투표에 속한 부스다. 프로젝트 부스는 `projectId`로 생성해 스탬프와 투표에 사용하고, 외부 부스는 표시 이름으로 생성해 스탬프 적립에만 사용한다.
- `DemodayEntryCode`는 비회원 방문자의 투표·스탬프 주체를 식별한다. 코드 원문이 아니라 `codeHash`만 저장한다.
- `DemodayVote`는 부스 투표, `DemodayStamp`는 부스 방문 스탬프를 나타낸다. 두 엔티티 모두 회원 또는 방문자 코드 중 정확히 하나를 주체로 가진다.

기수, 회원, 프로젝트 등 다른 도메인과의 연결은 ID만 사용한다. DEMODAY 안에서 다른 도메인의 엔티티나 Repository를 직접 참조하거나 JPA 연관관계를 추가하지 않는다.

## 모델 불변식

- `DemodayPoll.create(...)`는 `CLOSED` 상태로 시작한다. 투표 기간만으로 상태를 계산하거나 필드를 직접 변경하지 말고, 운영 상태 변경이 필요하면 명시적인 도메인 메서드를 추가한다.
- 투표 시작 시각은 종료 시각보다 이전이어야 하며, 투표·부스의 이름 정규화와 길이 검증은 기존 팩토리의 책임으로 유지한다.
- `DemodayBooth`의 `pollId`는 스칼라 FK다. 프로젝트 부스와 외부 부스의 생성 경로를 `forProject(...)`, `forExternal(...)` 팩토리로 구분한다.
- 투표와 스탬프는 생성 시 부스·방문자 코드가 같은 Poll에 속하는지 검증한다. 이 검증을 서비스의 조건문만으로 옮기거나 ID만 받는 팩토리로 약화하지 않는다.
- `DemodayVote`의 대상 부스는 해당 투표의 Poll에 속한 프로젝트 부스여야 한다. 외부 부스는 스탬프 적립만 허용한다. `DemodayStamp`의 방문자 코드와 부스도 같은 Poll에 속해야 한다.
- 취소는 `revoke(...)`로만 수행한다. 이미 취소된 기록을 다시 취소하거나, 취소됐다는 이유로 같은 주체의 재투표를 허용하지 않는다.

## 영속성 및 동시성

- Poll에서 Booth로 향하는 컬렉션은 기존의 읽기 전용 조회 뷰다. FK의 쓰기 주체는 `DemodayBooth.pollId`이므로 이 컬렉션에 cascade·변경 감지·소유권을 추가하지 않는다.
- 회원/방문자 주체 XOR, Poll별 회원 투표 1회, 방문자 코드별 투표 1회, 부스별 스탬프 1회 제약은 Flyway 마이그레이션의 DB 제약이 최종 보장한다.
- 사용자 경험을 위한 사전 조회는 가능하지만, 중복 처리의 정합성을 사전 조회에 의존하지 않는다. 비즈니스 의미를 식별할 수 있는 제약 위반만 애플리케이션 경계에서 도메인 오류로 변환한다.
- 제약이나 인덱스를 바꾸면 `V2026.07.26.10.44__create_demoday_vote_domain.sql`의 의도와 기존 데이터 영향을 검토하고, 필요한 새 Flyway 마이그레이션과 영속성 테스트를 함께 추가한다. 기존 마이그레이션은 수정하지 않는다.

## Port 계약

- Out Port는 Aggregate별 `Load`/`Save` 분리를 기본값으로 삼는다. 파일 수를 줄이거나 ISP를 형식적으로 적용하기 위해 메서드마다 인터페이스를 만들지 않는다.
- 실제 Application Service들이 서로 다른 기능 집합만 필요로 해 불필요한 의존이 생길 때는, 호출자가 요구하는 capability를 기준으로 Port를 추가 분리한다.
- Port 메서드 이름은 Spring Data 파생 쿼리 문법을 복제하지 말고 호출자의 도메인 의도를 표현한다. Spring Data 규칙을 따르는 긴 이름은 JPA Repository 내부에만 둔다.
- 반환형은 호출자가 필요로 하는 정보의 양을 드러내야 한다. 존재 여부만 필요하면 `boolean`, 조회 결과가 없을 수 있고 엔티티가 필요하면 `Optional<T>`, 반드시 있어야 하면 루트 규칙에 따라 `T`와 의미 있는 예외를 사용한다.
- 일괄 저장의 입력과 결과를 인덱스로 대응해야 하는 계약은 `List<T> saveAll(List<T>)`로 선언한다. 단지 여러 컬렉션을 받을 수 있다는 이유로 `Iterable`이나 `Collection`으로 넓히지 않는다.

## 조회 구현 선택

- 조회 순서는 도메인 계약이 명시하지 않는 한 Persistence 구현 세부사항이다. 재현 가능한 결과가 필요하면 Adapter가 정렬 기준을 소유한다.
- 고정 조건과 단순 정렬은 Spring Data 기본 메서드와 재사용 가능한 `Sort` 상수로 먼저 표현한다.
- 고정 쿼리지만 파생 메서드 이름이 읽기 어려워지거나 `Sort`로 명확히 표현하기 어려우면 `@Query`를 사용한다.
- 선택 조건의 유무에 따라 런타임에 predicate를 조립해야 하거나 동적 검색·복합 projection이 필요하면 `*QueryRepository`의 QueryDSL로 전환한다.
- 조회 기술이 바뀌어도 Application Port의 이름과 반환 계약은 유지한다. 기술 선택이 Port 밖으로 새지 않게 Persistence Adapter에서 변환한다.

## 영속성 오류 경계

- DB 제약은 동시 요청에서도 데이터 무결성을 지키는 최종 방어선이다. Unique 제약은 같은 키를 쓰는 경쟁 삽입을 DB가 원자적으로 중재해 중복 행을 막으며, 전체 요청을 직렬 실행하거나 기존 행의 덮어쓰기를 막는 장치로 설명하지 않는다.
- 중복 투표·중복 스탬프처럼 제약 이름과 비즈니스 의미의 대응이 명확한 위반만 Persistence Adapter 또는 Application 경계에서 `DemodayErrorCode`로 변환한다.
- 원인을 식별할 수 없는 `DataIntegrityViolationException`을 임의의 도메인 오류로 바꾸지 않는다. 알려지지 않은 DB 오류는 인프라·시스템 오류로 남겨 원인 정보를 보존한다.

## 작업 위치와 테스트

| 변경 대상 | 위치 | 확인할 사항 |
| --- | --- | --- |
| 도메인 규칙·예외 | `domain/` | 팩토리와 상태 변경 메서드에서 불변식 유지 |
| Port 계약 | `application/port/` | 조회와 저장 의도를 분리하고, 영속 기술을 노출하지 않음 |
| JPA 구현 | `adapter/out/persistence/` | Port 의미를 유지하고 N+1·중복 FK 매핑 방지 |
| 스키마 | `src/main/resources/db/migration/` | 새 Flyway 파일과 DB 제약·인덱스 동시 반영 |
| 도메인 테스트 | `src/test/java/com/umc/product/demoday/domain/` | 동일 Poll 검증, 주체 XOR, 취소 상태 검증 |
| 영속성 테스트 | `src/test/java/com/umc/product/demoday/adapter/out/persistence/` | 유니크·CHECK 제약과 재조회 후 정합성 검증 |

`DemodayEntityMappingTest`는 Booth FK를 Poll 컬렉션이 소유하지 않는 현재 매핑을 보호한다. 연관관계 또는 컬럼 이름을 변경하면 이 테스트와 `DemodayPersistenceConstraintTest`를 함께 갱신해 의도를 명시한다.

## 피할 것

- 투표·스탬프 생성 시 동일 Poll 검증을 생략하는 것
- 회원과 방문자 코드를 동시에 저장하거나 둘 다 비워 두는 것
- 다른 도메인 엔티티를 직접 참조하는 `@ManyToOne`을 추가하는 것
- 읽기 전용 Booth 컬렉션을 쓰기 모델로 사용하는 것
- 취소된 투표를 중복 투표 가능 상태로 해석하는 것
