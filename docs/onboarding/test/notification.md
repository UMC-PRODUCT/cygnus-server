# Notification 테스트 케이스

- 테스트 파일: 3개
- 테스트 케이스: 10개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| UseCase / Application Service | 7 |
| Domain | 3 |

## UseCase / Application Service

### FcmOutboxServiceTest
- 위치: `src/test/java/com/umc/product/notification/application/service/FcmOutboxServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [25](../../../src/test/java/com/umc/product/notification/application/service/FcmOutboxServiceTest.java#L25) | SUBSCRIBE 이벤트는 토픽 기반 비활성화로 인해 즉시 FAILED 처리된다 | 조건 SUBSCRIBE 이벤트는 토픽 기반 비활성화로 인해 즉시 FAILED 처리된다 | 성공: 검증 assertThat(loadFcmOutboxPort.findPendingEvents()).isEmpty(); |
| [38](../../../src/test/java/com/umc/product/notification/application/service/FcmOutboxServiceTest.java#L38) | UNSUBSCRIBE 이벤트는 토픽 기반 비활성화로 인해 즉시 FAILED 처리된다 | 조건 UNSUBSCRIBE 이벤트는 토픽 기반 비활성화로 인해 즉시 FAILED 처리된다 | 성공: 검증 assertThat(loadFcmOutboxPort.findPendingEvents()).isEmpty(); |
| [51](../../../src/test/java/com/umc/product/notification/application/service/FcmOutboxServiceTest.java#L51) | PENDING 이벤트가 없으면 아무것도 처리하지 않는다 | 조건 PENDING 이벤트가 없으면 아무것도 처리하지 않는다 | 성공: 검증 assertThat(loadFcmOutboxPort.findPendingEvents()).isEmpty(); |
| [58](../../../src/test/java/com/umc/product/notification/application/service/FcmOutboxServiceTest.java#L58) | 여러 PENDING 이벤트가 있으면 모두 FAILED 처리된다 | 조건 여러 PENDING 이벤트가 있으면 모두 FAILED 처리된다 | 성공: 검증 assertThat(loadFcmOutboxPort.findPendingEvents()).isEmpty(); |

### FcmServiceTest
- 위치: `src/test/java/com/umc/product/notification/application/service/FcmServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [28](../../../src/test/java/com/umc/product/notification/application/service/FcmServiceTest.java#L28) | 신규 토큰 등록 시 FCM 토큰이 활성 상태로 저장된다 | 호출 registerFcmToken(memberId, request) | 성공: 검증 assertThat(tokens).hasSize(1); assertThat(tokens.get(0).getFcmToken()).isEqualTo("new-token"); assertThat(tokens.get(0).isActive()).isTrue(); |
| [44](../../../src/test/java/com/umc/product/notification/application/service/FcmServiceTest.java#L44) | 동일 토큰 재등록 시 INSERT 없이 활성화만 된다 | 호출 registerFcmToken(memberId, new FcmRegistrationRequest("existing-token")) | 성공: 검증 assertThat(tokens).hasSize(1); assertThat(tokens.get(0).getFcmToken()).isEqualTo("existing-token"); assertThat(tokens.get(0).isActive()).isTrue(); |
| [61](../../../src/test/java/com/umc/product/notification/application/service/FcmServiceTest.java#L61) | 새 기기 토큰 등록 시 기존 토큰과 함께 저장된다 | 호출 registerFcmToken(memberId, new FcmRegistrationRequest("new-device-token")) | 성공: 검증 assertThat(tokens).hasSize(2); assertThat(tokens).extracting(FcmToken::getFcmToken); .containsExactlyInAnyOrder("old-token", "new-device-token"); |

## Domain

### FcmOutboxEventTest
- 위치: `src/test/java/com/umc/product/notification/domain/FcmOutboxEventTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [9](../../../src/test/java/com/umc/product/notification/domain/FcmOutboxEventTest.java#L9) | create는 매번 새로운 인스턴스와 새로운 eventId를 반환한다 | 조건 create는 매번 새로운 인스턴스와 새로운 eventId를 반환한다 | 성공: 검증 assertThat(first.eventId()).isNotEqualTo(second.eventId()); |
| [20](../../../src/test/java/com/umc/product/notification/domain/FcmOutboxEventTest.java#L20) | create로 생성된 이벤트의 occurredAt은 호출 시점 근처여야 한다 | 조건 create로 생성된 이벤트의 occurredAt은 호출 시점 근처여야 한다 | 성공: 검증 assertThat(event.occurredAt()).isBetween(before, after); |
| [34](../../../src/test/java/com/umc/product/notification/domain/FcmOutboxEventTest.java#L34) | eventType은 'fcm.outbox.created'이다 | 조건 eventType은 'fcm.outbox.created'이다 | 성공: 검증 assertThat(event.eventType()).isEqualTo("fcm.outbox.created"); |
