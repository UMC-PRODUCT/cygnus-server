# Schedule 테스트 케이스

- 테스트 파일: 2개
- 테스트 케이스: 16개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| UseCase / Application Service | 16 |

## UseCase / Application Service

### ScheduleCommandServiceDeleteTest
- 테스트 설명: ScheduleCommandService 일정 삭제
- 위치: `src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceDeleteTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [69](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceDeleteTest.java#L69) | ScheduleCommandService 일정 삭제 / 일정과 모든 참여자를 단일 벌크 쿼리로 함께 삭제한다 | 호출 delete(SCHEDULE_ID) | 성공: ScheduleCommandService 일정 삭제 / 일정과 모든 참여자를 단일 벌크 쿼리로 함께 삭제한다 |
| [89](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceDeleteTest.java#L89) | delete - 일반 삭제 / 출석 기록이 존재하면 SCHEDULE-0033 예외가 발생한다 | 호출 delete(SCHEDULE_ID)) | 실패: 예외 ScheduleDomainException; 에러코드 ScheduleErrorCode.SCHEDULE_HAS_ATTENDANCE_RECORD; 검증 .isEqualTo(ScheduleErrorCode.SCHEDULE_HAS_ATTENDANCE_RECORD); |
| [109](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceDeleteTest.java#L109) | delete - 일반 삭제 / 존재하지 않는 일정 삭제 시 SCHEDULE_NOT_FOUND 예외가 발생한다 | 호출 delete(SCHEDULE_ID)) | 실패: 예외 ScheduleDomainException; 에러코드 ScheduleErrorCode.SCHEDULE_NOT_FOUND; 검증 .isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND); |
| [128](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceDeleteTest.java#L128) | delete - 일반 삭제 / 출석 기록 존재 여부와 무관하게 일정과 참여자를 단일 벌크 쿼리로 함께 삭제한다 | 호출 forceDelete(SCHEDULE_ID) | 성공: delete - 일반 삭제 / 출석 기록 존재 여부와 무관하게 일정과 참여자를 단일 벌크 쿼리로 함께 삭제한다 |
| [148](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceDeleteTest.java#L148) | forceDelete - 강제 삭제 / 존재하지 않는 일정 강제 삭제 시 SCHEDULE_NOT_FOUND 예외가 발생한다 | 호출 forceDelete(SCHEDULE_ID)) | 실패: 예외 ScheduleDomainException; 에러코드 ScheduleErrorCode.SCHEDULE_NOT_FOUND; 검증 .isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND); |

### SchedulePermissionEvaluatorTest
- 테스트 설명: SchedulePermissionEvaluator
- 위치: `src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [30](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L30) | supportedResourceType은 SCHEDULE을 반환한다 | 호출 supportedResourceType()).isEqualTo(ResourceType.SCHEDULE) | 성공: 검증 assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.SCHEDULE); |
| [94](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L94) | SchedulePermissionEvaluator / 일정 생성자 본인이면 허용 | 호출 evaluate(subject, permission)).isTrue() | 성공: 검증 assertThat(sut.evaluate(subject, permission)).isTrue(); |
| [108](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L108) | DELETE - 일반 삭제 권한 / 해당 일정 기수의 SUPER_ADMIN이면 허용 | 호출 evaluate(subject, permission)).isTrue() | 성공: 검증 assertThat(sut.evaluate(subject, permission)).isTrue(); |
| [121](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L121) | DELETE - 일반 삭제 권한 / 생성자도 아니고 해당 기수 SUPER_ADMIN도 아니면 거부 | 호출 evaluate(subject, permission)).isFalse() | 실패: 검증 assertThat(sut.evaluate(subject, permission)).isFalse(); |
| [133](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L133) | DELETE - 일반 삭제 권한 / 다른 기수의 SUPER_ADMIN이면 거부 | 호출 evaluate(subject, permission)).isFalse() | 실패: 검증 assertThat(sut.evaluate(subject, permission)).isFalse(); |
| [146](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L146) | DELETE - 일반 삭제 권한 / 해당 기수의 중앙총괄(SUPER_ADMIN 아님)은 생성자가 아니면 거부 | 호출 evaluate(subject, permission)).isFalse() | 실패: 검증 assertThat(sut.evaluate(subject, permission)).isFalse(); |
| [162](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L162) | DELETE - 일반 삭제 권한 / 해당 일정 기수의 SUPER_ADMIN이면 허용 | 호출 evaluate(subject, permission)).isTrue() | 성공: 검증 assertThat(sut.evaluate(subject, permission)).isTrue(); |
| [177](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L177) | FORCE_DELETE - 강제 삭제 권한 / 일정 생성자 본인이라도 SUPER_ADMIN이 아니면 거부 | 호출 evaluate(subject, permission)).isFalse() | 실패: 검증 assertThat(sut.evaluate(subject, permission)).isFalse(); |
| [189](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L189) | FORCE_DELETE - 강제 삭제 권한 / 다른 기수의 SUPER_ADMIN이면 거부 | 호출 evaluate(subject, permission)).isFalse() | 실패: 검증 assertThat(sut.evaluate(subject, permission)).isFalse(); |
| [202](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L202) | FORCE_DELETE - 강제 삭제 권한 / 해당 기수 중앙총괄(SUPER_ADMIN 아님)이면 거부 | 호출 evaluate(subject, permission)).isFalse() | 실패: 검증 assertThat(sut.evaluate(subject, permission)).isFalse(); |
| [215](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L215) | FORCE_DELETE - 강제 삭제 권한 / 아무 역할도 없는 사용자는 거부 | 호출 evaluate(subject, permission)).isFalse() | 실패: 검증 assertThat(sut.evaluate(subject, permission)).isFalse(); |
