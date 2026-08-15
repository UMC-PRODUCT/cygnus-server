# DEMODAY 테스트 지침

이 파일은 `src/test/java/com/umc/product/demoday/**`에 적용된다. 도메인 불변식과 Port·영속성 원칙은 루트 및 `src/main/java/com/umc/product/demoday/AGENTS.md`를 함께 참고한다.

## 테스트 경계

- Persistence Adapter 계약 테스트는 Aggregate별로 나누고, Application이 소비하는 `Load`/`Save` Port 타입을 주입한다. 구현체가 교체돼도 동일한 Port 계약을 검증하게 한다.
- 하나의 테스트 메서드는 하나의 저장·조회 동작 또는 하나의 실패 규칙만 검증한다. 여러 Aggregate의 전체 연결을 확인해야 하면 별도의 작은 Smoke Test로 유지한다.
- Adapter 내부 구현이나 DB 스키마 자체가 테스트 대상인 경우에는 구체 JPA Repository, `EntityManager`, `JdbcTemplate`을 직접 사용할 수 있다. 이 테스트를 Application 계약 테스트와 같은 목적으로 설명하지 않는다.

## DB 제약 테스트

- DB 제약 자체를 검증하는 테스트는 `DataIntegrityViolationException`을 기대한다. 도메인 오류 변환 여부는 Adapter 또는 Application 경계의 별도 테스트에서 검증한다.
- 도메인 팩토리나 JPA 매핑이 잘못된 상태의 생성을 먼저 차단해 CHECK 제약에 도달할 수 없다면, `JdbcTemplate`으로 최소 SQL을 실행해 DB 제약을 직접 검증한다.
- 중복 제약 테스트는 가능하면 제약 이름 또는 원인까지 확인해, 의도하지 않은 NOT NULL·FK 위반으로도 테스트가 통과하는 일을 막는다.
- 동시성 정합성이 핵심인 제약은 단일 스레드의 순차 중복 저장 테스트만으로 충분하다고 간주하지 않는다. 필요하면 별도 동시성 테스트로 한 요청만 성공하고 경쟁 요청이 제약 위반으로 실패하는지 확인한다.

## Fixture와 재조회

- Fixture 저장은 테스트 경계에 맞는 Save Port 또는 Repository를 사용하고, 반복 준비 코드는 의미가 드러나는 fixture helper로 추출한다.
- `flush()`와 `clear()` 뒤 재조회는 영속성 컨텍스트의 동일 객체가 아니라 DB에서 복원된 상태를 검증할 때 사용한다.
- 준비 직후 fixture를 다시 찾는 `orElseThrow()`는 "반드시 존재해야 한다"는 테스트 전제를 표현한다. 이 실패는 비즈니스 Not Found가 아니라 준비 실패이므로 별도 도메인 예외로 변환하지 않는다.
