# Notification 테스트 케이스

- 테스트 파일: 2개
- 테스트 케이스: 4개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| UseCase / Application Service | 3 |
| Repository / Migration | 1 |

## UseCase / Application Service

### FcmServiceTest
- 위치: `src/test/java/com/umc/product/notification/application/service/FcmServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [28](../../../src/test/java/com/umc/product/notification/application/service/FcmServiceTest.java#L28) | 신규 토큰 등록 시 FCM 토큰이 활성 상태로 저장된다 | 호출 registerFcmToken(memberId, request) | 성공: 검증 assertThat(tokens).hasSize(1); assertThat(tokens.get(0).getFcmToken()).isEqualTo("new-token"); assertThat(tokens.get(0).isActive()).isTrue(); |
| [44](../../../src/test/java/com/umc/product/notification/application/service/FcmServiceTest.java#L44) | 동일 토큰 재등록 시 INSERT 없이 활성화만 된다 | 호출 registerFcmToken(memberId, new FcmRegistrationRequest("existing-token")) | 성공: 검증 assertThat(tokens).hasSize(1); assertThat(tokens.get(0).getFcmToken()).isEqualTo("existing-token"); assertThat(tokens.get(0).isActive()).isTrue(); |
| [61](../../../src/test/java/com/umc/product/notification/application/service/FcmServiceTest.java#L61) | 새 기기 토큰 등록 시 기존 토큰과 함께 저장된다 | 호출 registerFcmToken(memberId, new FcmRegistrationRequest("new-device-token")) | 성공: 검증 assertThat(tokens).hasSize(2); assertThat(tokens).extracting(FcmToken::getFcmToken); .containsExactlyInAnyOrder("old-token", "new-device-token"); |

## Repository / Migration

### FcmOutboxRemovalMigrationTest
- 위치: `src/test/java/com/umc/product/notification/adapter/out/persistentce/FcmOutboxRemovalMigrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [35](../../../src/test/java/com/umc/product/notification/adapter/out/persistentce/FcmOutboxRemovalMigrationTest.java#L35) | legacy FCM outbox 테이블을 삭제하고 재실행해도 성공한다 | row가 있는 `fcm_outbox`에 migration을 두 번 적용한다 | 테이블이 삭제되고 `IF EXISTS`로 재실행도 성공한다 |
