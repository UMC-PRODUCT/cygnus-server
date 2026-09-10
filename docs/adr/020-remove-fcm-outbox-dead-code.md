# ADR-020: notification 도메인의 deprecated FCM Outbox / Topic 코드 일괄 제거

## Status

Accepted

## Context

`notification` 도메인은 한때 Firebase Cloud Messaging의 토픽(topic) 구독 모델을 사용했고,
이 토픽 subscribe/unsubscribe API 호출의 트랜잭션-안전 분리를 위해 `FcmOutbox` 메커니즘을
도입했다. 2026-03~2026-04 사이에 알림 모델이 **"토픽 구독" → "토큰 직접 발송"**으로 전환되면서
다음 두 영역이 동시에 dead path가 되었다.

### 토픽 모델 폐기 시점

마이그레이션 타임라인:

| 시점         | 마이그레이션                                              | 의미                |
|------------|----------------------------------------------------|-------------------|
| 2026-03-06 | `V2026.03.06.00.00__create_fcm_outbox.sql`         | outbox 테이블 도입     |
| 2026-03-17 | `V2026.03.17.00.00__create_fcm_token_topic.sql`    | 토픽 매핑 도입          |
| 2026-04-18 | `V2026.04.18.00.00__alter_fcm_token_multi_device.sql` | 멀티 디바이스 전환        |
| **2026-04-23** | **`V2026.04.23.23.50__drop_fcm_token_topic.sql`** | **토픽 매핑 폐기**     |

토픽 폐기와 함께 토픽 API 호출 자체가 사라졌고, outbox가 보호할 외부 호출도 함께 사라졌다.

### 현재 dead code 목록

**도메인/응용 레이어**

- `notification/domain/FcmOutbox.java` — 엔티티 (모든 row가 무의미한 상태)
- `notification/domain/FcmOutboxEvent.java` — **빈 record, 발행하는 코드 없음**
- `notification/domain/FcmOutboxEventType.java` — `FCM_SUBSCRIBE` / `FCM_UNSUBSCRIBE` 두 값, 사용처 없음
- `notification/domain/FcmOutboxStatus.java` — `PENDING` / `PROCESSED` / `FAILED`, 활성 path 없음
- `notification/application/service/FcmOutboxService.java` — PENDING을 즉시 FAILED로 마킹하는 stub
- `notification/application/port/in/ProcessFcmOutboxUseCase.java` — 위 stub의 인터페이스
- `notification/application/port/out/LoadFcmOutboxPort.java`, `SaveFcmOutboxPort.java`
- `notification/adapter/out/persistentce/FcmOutboxPersistenceAdapter.java`,
  `FcmOutboxJpaRepository.java`
- `notification/adapter/in/scheduler/FcmOutboxScheduler.java` — 빈 테이블만 polling
- `notification/adapter/in/event/FcmOutboxEventListener.java` — `FcmOutboxEvent` 발행 없으니
  호출되지 않음

**토픽 관련**

- `notification/application/service/FcmTopicService.java` — `log.warn("[DEPRECATED] ...")`만 남는
  no-op stub
- `notification/application/port/in/ManageFcmTopicUseCase.java` — `@Deprecated(forRemoval = true)`
- 컨트롤러 deprecated endpoints:
  - `DELETE /api/v1/notification/fcm/topics/legacy`
  - `resubscribeAllMemberLegacyTopics` 관련 endpoint

**설정**

- `app.fcm.outbox-interval-ms` 프로퍼티

**DB 스키마**

- `fcm_outbox` 테이블 (현재 row가 있을 수 있으나 모두 의미 없음)

### 정리해야 하는 이유

1. **인지 부담**: 신규 개발자가 코드베이스를 탐색할 때 outbox/topic 코드를 보고 "왜 있는지"
   추적하는 시간 손실.
2. **이름 충돌 위험**: [ADR-019](./019-introduce-transactional-event-outbox.md)의
   `EventOutbox`가 도입되면 `FcmOutbox`와 이름 유사성으로 인한 혼동 가능성. 같은 PR에서 두
   메커니즘이 공존하면 리뷰가 어려워진다.
3. **마이그레이션 부담**: `fcm_outbox` 테이블은 향후 DB 백업/복구/스키마 dump에 계속 포함된다.
4. **Spring Bean 등록 부담**: `@Component`로 등록된 dead bean들이 ApplicationContext에 남아 있다.

### 정리 결정의 risk

이 ADR이 다루어야 할 risk:

- **외부 클라이언트가 deprecated endpoint를 여전히 호출**할 가능성. 호출 결과가 no-op이지만
  HTTP 404로 바뀌면 클라이언트 측 에러 핸들링이 필요할 수 있음. 사전 로그 분석 필수.
- **`fcm_outbox` 테이블 row가 운영 환경에 남아있을 가능성**. 모두 FAILED 상태로 추정되지만
  실제 행 수와 상태 확인 필수.

## Decision

우리는 `notification` 도메인의 deprecated FCM Outbox / Topic 관련 코드를 **단일 PR로 일괄 제거**
하기로 결정한다.

### 제거 대상

위 "현재 dead code 목록" 전체.

### 마이그레이션 전략

1. **사전 확인**:
   - 운영 환경 로그에서 `DELETE /topics/legacy` 등 deprecated endpoint 호출 빈도 최근 30일 분석.
     호출이 없거나 무시 가능한 수준이면 제거. 의미 있는 호출이 있다면 클라이언트 팀과 합의 후 진행.
   - `SELECT count(*), status FROM fcm_outbox GROUP BY status;` 실행하여 잔존 row 상태 확인.
2. **Flyway 마이그레이션**: `V2026.NN.NN.NN.NN__drop_fcm_outbox.sql`로 `fcm_outbox` 테이블 DROP.
3. **Java 코드 일괄 삭제**: 위 "현재 dead code 목록"의 모든 파일 제거.
4. **테스트 영향 확인**: `FcmOutboxServiceTest`, `FcmOutboxEventTest` 등 관련 테스트 동시 제거.
   다른 테스트에서 dead code를 import하는지 grep으로 전수 확인.
5. **`FcmController` 정리**: deprecated endpoint 메서드 제거. 남는 endpoint(`PUT /token`)만 유지.
6. **`FcmProperties` 정리**: `outbox-interval-ms` 사용처 없어지므로 `application-*.yaml`에서
   해당 키 제거.

### 진행 순서

`ADR-019`(EventOutbox 도입) 구현보다 **선행 권장**. 이름 충돌과 리뷰 혼동을 줄이기 위함.

## Alternatives Considered

### 대안 A: 그대로 유지 (현 상태)

장점:
- 변경 없음. risk 0.

단점:
- 모든 dead code 단점이 누적된다 (인지 부담, 이름 충돌, 마이그레이션 부담).
- `@Deprecated(forRemoval = true)`가 이미 붙어 있는데 실제 제거를 하지 않는 모순.

선택하지 않은 이유:
이미 5주 이상 dead path였고, EventOutbox 도입을 앞두고 정리하지 않으면 두 outbox 메커니즘이
공존하는 어색한 상태가 길어진다.

### 대안 B: `@Deprecated` 마킹만 유지하고 제거는 더 미루기

장점:
- 외부 클라이언트 호환성 유지.

단점:
- 이미 일부는 `forRemoval = true` 상태인데 그 약속을 지키지 않는 셈.
- 새 개발자/AI 에이전트가 deprecated 코드를 활성 path로 오인할 위험.

선택하지 않은 이유:
사전 로그 분석에서 외부 호출이 없다면 미룰 이유가 없다. 호출이 있다면 그건 별개 협의 사항.

### 대안 C: 단계적 제거 (Java 코드 먼저, 테이블은 뒤에)

장점:
- 운영 안전성 (테이블이 살아있으면 rollback 가능).

단점:
- 두 번의 PR / 두 번의 리뷰 비용.
- Spring JPA에서 엔티티 클래스를 제거하면 Flyway validation이 실패할 수 있음 (`fcm_outbox`가
  엔티티 매핑 없이 DB에 남아있는 상태). 추가 설정 필요.

선택하지 않은 이유:
JPA 엔티티-테이블 매핑 정합성 때문에 단계 분리가 오히려 복잡해진다. 단일 PR로 일괄 처리하는
것이 더 안전하다.

### 대안 D: `FcmOutbox`를 `EventOutbox`의 기반으로 재활용

장점:
- 기존 마이그레이션과 인프라 활용.

단점:
- 두 outbox의 목적이 다름. `FcmOutbox`는 Firebase 토픽 API 호출 보호용, `EventOutbox`는
  도메인 이벤트 전달용. 스키마 호환성 없음 (`FcmOutbox.event_type`은 enum 두 개, EventOutbox는
  자유 문자열; payload 컬럼 의미 다름).
- 재활용하려면 마이그레이션으로 스키마 변경이 필요한데, 결국 새 테이블 만드는 것과 비용이 같음.
- 의미론적 혼란 가중.

선택하지 않은 이유:
ADR-018의 "FcmOutbox 일반화" 표현이 잘못된 것과 같은 이유. 두 outbox는 다른 도메인이다.
별도 신규 + 기존 일괄 정리가 가장 깔끔.

## Consequences

### Positive

- 코드베이스에서 dead code 약 10개 파일 + 1개 테이블 + 1개 프로퍼티 제거.
- ADR-019의 `EventOutbox` 도입 시 이름 충돌 / 인지 충돌 없음.
- Spring ApplicationContext에서 dead bean 4개 제거 (`FcmOutboxService`, `FcmOutboxScheduler`,
  `FcmOutboxEventListener`, `FcmTopicService`).
- DB 백업/복구/스키마 dump에서 무의미 데이터 제거.
- `@Deprecated(forRemoval = true)` 약속 이행.

### Negative

- Flyway 마이그레이션 추가 (DROP TABLE). 운영 환경에서 잔존 row가 있다면 영구 삭제.
- 외부 클라이언트가 deprecated endpoint를 호출 중이라면 404 응답으로 전환 (사전 분석 필수).
- Git history에서 dead code 컨텍스트를 찾으려면 commit 검색이 필요해짐 (대안: ADR 보관).

### Neutral / Trade-offs

- **잔존 row의 의미**: `fcm_outbox` row가 운영에 남아있더라도 처리하는 코드가 stub이라
  실효적 영향 없음. 백업 1회 후 drop이 안전.
- **외부 호출 0 가정 깨질 경우**: 사전 분석에서 호출이 발견되면 이 ADR을 보류하고 클라이언트
  팀과 협의 후 단계적 deprecation 일정 별도 합의.

## Implementation Notes

### 사전 확인 체크리스트 (PR 만들기 전)

- [ ] 운영 환경 nginx/Spring 로그에서 최근 30일간 다음 endpoint 호출 0건 확인:
  - `DELETE /api/v1/notification/fcm/topics/legacy`
  - `resubscribeAllMemberLegacyTopics` 관련 path
- [ ] `SELECT count(*), status, max(created_at) FROM fcm_outbox GROUP BY status;` 결과 확인
  (모두 FAILED 또는 PROCESSED, 최근 30일 신규 row 없음 검증).
- [ ] `application-*.yaml` 전체에서 `app.fcm.outbox-interval-ms` 사용처 grep.
- [ ] 다른 도메인 코드에서 `FcmOutbox*` 또는 `ManageFcmTopicUseCase` import 여부 grep
  (현 분석에서는 0건이지만 PR 시점 재확인).

### Flyway 마이그레이션

```sql
-- V2026.NN.NN.NN.NN__drop_fcm_outbox.sql
DROP TABLE IF EXISTS fcm_outbox;
```

기존 마이그레이션(`create_fcm_outbox.sql`, `create_fcm_token_topic.sql`,
`drop_fcm_token_topic.sql`)은 **삭제하지 않는다.** Flyway 히스토리는 보존한다.

### 제거 파일 일람

```
src/main/java/com/umc/product/notification/
  domain/
    FcmOutbox.java
    FcmOutboxEvent.java
    FcmOutboxEventType.java
    FcmOutboxStatus.java
  application/
    port/in/
      ProcessFcmOutboxUseCase.java
      ManageFcmTopicUseCase.java
    port/out/
      LoadFcmOutboxPort.java
      SaveFcmOutboxPort.java
    service/
      FcmOutboxService.java
      FcmTopicService.java
  adapter/
    in/
      scheduler/FcmOutboxScheduler.java
      event/FcmOutboxEventListener.java
    out/persistentce/
      FcmOutboxPersistenceAdapter.java
      FcmOutboxJpaRepository.java

src/test/java/com/umc/product/notification/
  application/service/FcmOutboxServiceTest.java
  domain/FcmOutboxEventTest.java   ← ADR-018 작업에서 추가된 테스트, 같이 제거
```

`FcmController`의 deprecated endpoint 메서드만 제거하고 클래스 자체는 유지.

### 검증 절차

1. `./gradlew compileJava compileTestJava` — 컴파일 통과
2. `./gradlew test` — 전 테스트 통과
3. Spring Boot 기동 확인 (Bean 등록 누락 없음)
4. PR 본문에 다음 정보 첨부:
   - 사전 분석 결과 (endpoint 호출 빈도, 잔존 row 수)
   - 마이그레이션 SQL
   - 제거된 파일 수

### ADR-018, ADR-019와의 관계

- ADR-018에서 `FcmOutboxEvent`를 `DomainEvent`로 마이그레이션한 작업은 일관성 유지를 위한
  잠정 조치였다. 이 ADR로 해당 record 자체가 제거된다.
- ADR-019의 `EventOutbox` 신규 도입은 이 ADR 정리 후에 진행해야 이름/스키마 충돌이 없다.

## References

- [ADR-018: Spring Event Publisher 추상화](./018-abstract-spring-event-publisher-for-future-broker.md)
- [ADR-019: Transactional Event Outbox 도입](./019-introduce-transactional-event-outbox.md)
- Flyway 히스토리:
  - `V2026.03.06.00.00__create_fcm_outbox.sql`
  - `V2026.03.17.00.00__create_fcm_token_topic.sql`
  - `V2026.04.23.23.50__drop_fcm_token_topic.sql`
