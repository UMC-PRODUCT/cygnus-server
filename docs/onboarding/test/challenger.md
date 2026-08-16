# Challenger 테스트 케이스

- 테스트 파일: 13개
- 테스트 케이스: 56개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 16 |
| UseCase / Application Service | 23 |
| E2E / Integration | 3 |
| Domain | 14 |

## Controller / Inbound Adapter

### ChallengerCommandControllerTest
- 테스트 설명: ChallengerCommandController
- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [61](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandControllerTest.java#L61) | ChallengerCommandController / 챌린저 생성 성공 시 생성된 챌린저 정보를 반환한다 | HTTP POST /api/v1/challenger; body {"memberId":1,"part":"SPRINGBOOT","gisuId":9} | 성공: HTTP 200 OK |
| [80](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandControllerTest.java#L80) | ChallengerCommandController / 챌린저 생성 요청의 part가 없으면 400 | HTTP POST /api/v1/challenger; body {"memberId":1,"gisuId":9} | 실패: HTTP 400 Bad Request |
| [93](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandControllerTest.java#L93) | ChallengerCommandController / 챌린저 batch 생성 요청 내부 항목의 part가 없으면 400 | HTTP POST /api/v1/challenger/batch; body [{"memberId":1,"gisuId":9}] | 실패: HTTP 400 Bad Request |
| [106](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandControllerTest.java#L106) | ChallengerCommandController / 파트 변경 요청의 newPart가 없으면 400 | HTTP PATCH /api/v1/challenger/{challengerId}/part; body {} | 실패: HTTP 400 Bad Request |

### ChallengerPointCommandControllerTest
- 테스트 설명: ChallengerPointCommandController
- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerPointCommandControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [32](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerPointCommandControllerTest.java#L32) | 상벌점 부여 성공 시 챌린저 정보를 반환한다 | HTTP POST /api/v1/challenger/{challengerId}/points; body {"pointType":"CUSTOM","pointValue":1,"description":"조정"} | 성공: HTTP 200 OK |
| [63](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerPointCommandControllerTest.java#L63) | ChallengerPointCommandController / 상벌점 부여 요청의 pointType이 없으면 400 | HTTP POST /api/v1/challenger/{challengerId}/points; body {"pointValue":1,"description":"조정"} | 실패: HTTP 400 Bad Request |
| [76](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerPointCommandControllerTest.java#L76) | ChallengerPointCommandController / 상벌점 설명을 수정한다 | HTTP PATCH /api/v1/challenger/points/{challengerPointId}; body {"newDescription":"수정"} | 성공: HTTP 200 OK |
| [87](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerPointCommandControllerTest.java#L87) | ChallengerPointCommandController / 상벌점을 삭제한다 | HTTP DELETE /api/v1/challenger/points/{challengerPointId} | 성공: HTTP 200 OK |

### ChallengerQueryControllerTest
- 테스트 설명: ChallengerQueryController
- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerQueryControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [24](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerQueryControllerTest.java#L24) | 챌린저 단건 정보를 조회한다 | HTTP GET /api/v1/challenger/{challengerId} | 성공: HTTP 200 OK |

### ChallengerRecordControllerTest
- 테스트 설명: ChallengerRecordController
- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [60](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordControllerTest.java#L60) | ChallengerRecordController / 코드로 챌린저 기록을 조회한다 | HTTP GET /api/v1/challenger-record/code/{code} | 성공: HTTP 200 OK |
| [73](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordControllerTest.java#L73) | ChallengerRecordController / 회원 기록 추가 요청의 code가 blank이면 400 | HTTP POST /api/v1/challenger-record/member; body {"code":" "} | 실패: HTTP 400 Bad Request |
| [86](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordControllerTest.java#L86) | ChallengerRecordController / 기록 생성 요청의 gisuId가 없으면 400 | HTTP POST /api/v1/challenger-record; body {"chapterId":2,"schoolId":3,"part":"SPRINGBOOT","memberName":"홍길동"} | 실패: HTTP 400 Bad Request |
| [99](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordControllerTest.java#L99) | ChallengerRecordController / 기록 bulk 생성 요청 내부 항목의 gisuId가 없으면 400 | HTTP POST /api/v1/challenger-record/bulk; body [{"chapterId":2,"schoolId":3,"part":"SPRINGBOOT","memberName":"홍길동"}] | 실패: HTTP 400 Bad Request |

### ChallengerSearchControllerTest
- 테스트 설명: ChallengerSearchController
- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerSearchControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [35](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerSearchControllerTest.java#L35) | 커서 검색 size는 최대 50으로 제한된다 | HTTP GET /api/v1/challenger/search/cursor; param size="100" | 성공: HTTP 200 OK |
| [60](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerSearchControllerTest.java#L60) | ChallengerSearchController / offset 검색 결과를 반환한다 | HTTP GET /api/v1/challenger/search/offset; param page="0"; param size="10" | 성공: HTTP 200 OK |

### ChallengerSearchV2ControllerTest
- 테스트 설명: ChallengerSearchV2Controller
- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/v2/ChallengerSearchV2ControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [32](../../../src/test/java/com/umc/product/challenger/adapter/in/web/v2/ChallengerSearchV2ControllerTest.java#L32) | 챌린저 검색 v2 응답은 이메일을 마스킹한다 | HTTP GET /api/v2/challenger/search; param page="0"; param size="10" | 성공: HTTP 200 OK |

## UseCase / Application Service

### ChallengerActivityPeriodServiceTest
- 테스트 설명: ChallengerActivityPeriodService — 활동일 합산 로직
- 위치: `src/test/java/com/umc/product/challenger/application/service/ChallengerActivityPeriodServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [51](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerActivityPeriodServiceTest.java#L51) | ChallengerActivityPeriodService — 활동일 합산 로직 / ACTIVE와 GRADUATED 챌린저의 기수만 합산한다 | 호출 calculateActivityPeriod(challengers, gisus) | 성공: 검증 assertThat(summary.perGisu()).hasSize(2); assertThat(summary.perGisu()); .containsExactlyInAnyOrder(10L, 11L); assertThat(summary.totalActivityDays()).isGreaterThan(0L); |
| [80](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerActivityPeriodServiceTest.java#L80) | ChallengerActivityPeriodService — 활동일 합산 로직 / 챌린저가 없으면 빈 요약을 반환한다 | 호출 calculateActivityPeriod(List.of(), Map.of()) | 성공: 검증 assertThat(summary.totalActivityDays()).isZero(); assertThat(summary.perGisu()).isEmpty(); |
| [88](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerActivityPeriodServiceTest.java#L88) | ChallengerActivityPeriodService — 활동일 합산 로직 / EXPELLED WITHDRAWN만 있으면 0일을 반환한다 | 호출 calculateActivityPeriod(challengers, gisus) | 성공: 검증 assertThat(summary.totalActivityDays()).isZero(); assertThat(summary.perGisu()).isEmpty(); |
| [109](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerActivityPeriodServiceTest.java#L109) | 진행중인 기수는 now까지의 일수만 포함한다 | 호출 calculateActivityPeriod(challengers, gisus) | 성공: 검증 assertThat(summary.totalActivityDays()).isBetween(29L, 31L); |
| [128](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerActivityPeriodServiceTest.java#L128) | 종료된 기수는 전체 기간을 합산한다 | 호출 calculateActivityPeriod(challengers, gisus) | 성공: 검증 assertThat(summary.totalActivityDays()).isEqualTo(ChronoUnit.DAYS.between(start, end)); |
| [146](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerActivityPeriodServiceTest.java#L146) | getActivityPeriodByMemberId는 내부에서 챌린저와 기수를 조회한다 | 호출 getActivityPeriodByMemberId(100L) | 성공: 검증 assertThat(summary.totalActivityDays()).isEqualTo(ChronoUnit.DAYS.between(start, end)); |
| [162](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerActivityPeriodServiceTest.java#L162) | getActivityPeriodByMemberId는 챌린저 0건이면 빈 요약을 반환한다 | 호출 getActivityPeriodByMemberId(100L) | 성공: 검증 assertThat(summary.totalActivityDays()).isZero(); assertThat(summary.perGisu()).isEmpty(); |

### ChallengerCommandServiceTest
- 테스트 설명: ChallengerCommandService
- 위치: `src/test/java/com/umc/product/challenger/application/service/ChallengerCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [40](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerCommandServiceTest.java#L40) | 동일 기수 챌린저가 없으면 생성한다 | CreateChallengerCommand {memberId=1L, part=ChallengerPart.SPRINGBOOT, gisuId=9L}; 호출 createChallenger(command) | 성공: 검증 assertThat(result).isEqualTo(100L); |
| [85](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerCommandServiceTest.java#L85) | createChallenger / 동일 기수 챌린저가 있으면 생성하지 않는다 | CreateChallengerCommand {memberId=1L, part=ChallengerPart.SPRINGBOOT, gisuId=9L}; 호출 createChallenger(command)) | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS; 검증 .isEqualTo(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS); |
| [107](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerCommandServiceTest.java#L107) | createChallenger / 변경할 파트와 상태가 모두 없으면 실패한다 | 조건 createChallenger / 변경할 파트와 상태가 모두 없으면 실패한다 | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.BAD_CHALLENGER_UPDATE_REQUEST; 검증 .isEqualTo(ChallengerErrorCode.BAD_CHALLENGER_UPDATE_REQUEST); |
| [124](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerCommandServiceTest.java#L124) | updateChallenger / 비활성 챌린저는 파트를 변경할 수 없다 | 조건 updateChallenger / 비활성 챌린저는 파트를 변경할 수 없다 | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.CHALLENGER_NOT_ACTIVE; 검증 .isEqualTo(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE); |
| [140](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerCommandServiceTest.java#L140) | updateChallenger / deactivateChallenger는 EXPEL 타입을 EXPELLED 상태로 매핑한다 | 조건 updateChallenger / deactivateChallenger는 EXPEL 타입을 EXPELLED 상태로 매핑한다 | 성공: 검증 assertThat(challenger.getStatus()).isEqualTo(ChallengerStatus.EXPELLED); assertThat(challenger.getModifiedBy()).isEqualTo(99L); assertThat(challenger.getModificationReason()).isEqualTo("징계"); |
| [158](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerCommandServiceTest.java#L158) | updateChallenger / 비활성 챌린저에게 상벌점을 부여할 수 없다 | GrantChallengerPointCommand {challengerId=1L, pointType=PointType.CUSTOM, pointValue=1, description="조정"}; 호출 grantChallengerPoint(GrantChallengerPointCommand.builder() | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.CHALLENGER_NOT_ACTIVE; 검증 .isEqualTo(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE); |
| [176](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerCommandServiceTest.java#L176) | 상벌점 설명을 수정한다 | 호출 updateChallengerPoint(UpdateChallengerPointCommand.of(10L, "수정")) | 성공: 검증 assertThat(point.getDescription()).isEqualTo("수정"); |

### ChallengerRecordCommandServiceTest
- 테스트 설명: ChallengerRecordCommandService
- 위치: `src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [40](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java#L40) | 학교가 요청 지부에 속하면 기록 코드를 생성한다 | 호출 create(command) | 성공: 검증 assertThat(result).isEqualTo(10L); |
| [86](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java#L86) | ChallengerRecordCommandService / 학교가 요청 지부에 속하지 않으면 기록 코드를 생성하지 않는다 | 호출 create(recordCommand())) | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST; 검증 .isEqualTo(ChallengerErrorCode.INVALID_CHALLENGER_RECORD_CREATE_REQUEST); |
| [99](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java#L99) | ChallengerRecordCommandService / 일반 기록 코드를 소비하면 챌린저를 생성하고 코드를 사용 처리한다 | 호출 consumeCode(consumeCommand()) | 성공: 검증 assertThat(record.isUsed()).isTrue(); assertThat(record.getUsedMemberId()).isEqualTo(100L); |
| [114](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java#L114) | 이미 사용된 코드는 소비할 수 없다 | 호출 consumeCode(consumeCommand())) | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.USED_CHALLENGER_RECORD_CODE; 검증 .isEqualTo(ChallengerErrorCode.USED_CHALLENGER_RECORD_CODE); |
| [130](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java#L130) | 일반 기록의 이름이 회원 정보와 다르면 챌린저를 생성하지 않는다 | 호출 consumeCode(consumeCommand())) | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.INVALID_MEMBER_NAME_FOR_RECORD; 검증 .isEqualTo(ChallengerErrorCode.INVALID_MEMBER_NAME_FOR_RECORD); assertThat(record.isUsed()).isFalse(); |
| [146](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java#L146) | 일반 기록의 학교가 회원 정보와 다르면 챌린저를 생성하지 않는다 | 호출 consumeCode(consumeCommand())) | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.INVALID_SCHOOL_FOR_RECORD; 검증 .isEqualTo(ChallengerErrorCode.INVALID_SCHOOL_FOR_RECORD); assertThat(record.isUsed()).isFalse(); |
| [162](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java#L162) | 같은 기수의 챌린저가 이미 있으면 일반 기록 코드를 소비하지 않는다 | 호출 consumeCode(consumeCommand())) | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS; 검증 .isEqualTo(ChallengerErrorCode.CHALLENGER_ALREADY_EXISTS); assertThat(record.isUsed()).isFalse(); |
| [180](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java#L180) | 운영진 기록 코드는 기존 챌린저에 역할을 부여하고 코드를 사용 처리한다 | 호출 consumeCode(consumeCommand()) | 성공: 검증 assertThat(record.isUsed()).isTrue(); |
| [199](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerRecordCommandServiceTest.java#L199) | 운영진 기록 코드 소비 시 해당 기수 챌린저가 없으면 실패한다 | 호출 consumeCode(consumeCommand())) | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.NO_CHALLENGER_IN_MEMBER_GISU; 검증 .isEqualTo(ChallengerErrorCode.NO_CHALLENGER_IN_MEMBER_GISU); assertThat(record.isUsed()).isFalse(); |

## E2E / Integration

### ChallengerRecordControllerIntegrationTest
- 테스트 설명: ChallengerRecordController 통합 테스트
- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordControllerIntegrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [75](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordControllerIntegrationTest.java#L75) | ChallengerRecordController 통합 테스트 / 일반 챌린저 기록 코드를 소비하면 챌린저가 생성되고 코드가 사용 처리된다 | HTTP POST /api/v1/challenger-record/member | 성공: HTTP 200 OK; 검증 assertThat(savedChallenger.getPart()).isEqualTo(ChallengerPart.SPRINGBOOT); assertThat(usedRecord.isUsed()).isTrue(); assertThat(usedRecord.getUsedMemberId()).isEqualTo(member.getId()); assertThat(usedRecord.getUsedAt... |
| [110](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordControllerIntegrationTest.java#L110) | 운영진 기록 코드를 소비하면 기존 챌린저에 역할이 부여되고 새 챌린저는 만들지 않는다 | HTTP POST /api/v1/challenger-record/member | 성공: HTTP 200 OK; 검증 assertThat(challengerJpaRepository.findByMemberId(member.getId())); .containsExactly(challenger.getId()); assertThat(challengerRoleJpaRepository.findByChallengerId(challenger.getId())); .hasSize(1) |
| [159](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordControllerIntegrationTest.java#L159) | 이미 사용된 챌린저 기록 코드는 재사용할 수 없다 | HTTP POST /api/v1/challenger-record/member | 실패: HTTP 200 OK; HTTP 400 Bad Request; 검증 assertThat(challengerJpaRepository.findByMemberId(member.getId())); .hasSize(1); .isEqualTo(ChallengerPart.ANDROID); assertThat(usedRecord.isUsed()).isTrue(); |

## Domain

### ChallengerPointTest
- 테스트 설명: ChallengerPoint 도메인
- 위치: `src/test/java/com/umc/product/challenger/domain/ChallengerPointTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [9](../../../src/test/java/com/umc/product/challenger/domain/ChallengerPointTest.java#L9) | pointValue가 없으면 PointType의 기본 점수를 사용한다 | 조건 pointValue가 없으면 PointType의 기본 점수를 사용한다 | 성공: 검증 assertThat(point.getPointValue()).isEqualTo(PointType.BEST_WORKBOOK.getValue()); |
| [26](../../../src/test/java/com/umc/product/challenger/domain/ChallengerPointTest.java#L26) | ChallengerPoint 도메인 / pointValue가 있으면 PointType보다 custom 점수를 우선한다 | 조건 ChallengerPoint 도메인 / pointValue가 있으면 PointType보다 custom 점수를 우선한다 | 성공: 검증 assertThat(point.getPointValue()).isEqualTo(-7.0); |
| [40](../../../src/test/java/com/umc/product/challenger/domain/ChallengerPointTest.java#L40) | ChallengerPoint 도메인 / 상벌점 설명을 수정한다 | 조건 ChallengerPoint 도메인 / 상벌점 설명을 수정한다 | 성공: 검증 assertThat(point.getDescription()).isEqualTo("새 설명"); |

### ChallengerRecordTest
- 테스트 설명: ChallengerRecord 도메인
- 위치: `src/test/java/com/umc/product/challenger/domain/ChallengerRecordTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [12](../../../src/test/java/com/umc/product/challenger/domain/ChallengerRecordTest.java#L12) | 일반 챌린저 기록 코드를 생성한다 | 조건 일반 챌린저 기록 코드를 생성한다 | 성공: 검증 assertThat(record.getCode()).hasSize(6); assertThat(record.isUsed()).isFalse(); assertThat(record.isAdminRecord()).isFalse(); assertThat(record.getMemberName()).isEqualTo("홍길동"); |
| [26](../../../src/test/java/com/umc/product/challenger/domain/ChallengerRecordTest.java#L26) | ChallengerRecord 도메인 / 운영진 기록은 역할 타입과 조직 ID를 가진다 | 조건 ChallengerRecord 도메인 / 운영진 기록은 역할 타입과 조직 ID를 가진다 | 성공: 검증 assertThat(record.isAdminRecord()).isTrue(); assertThat(record.getChallengerRoleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT); assertThat(record.getOrganizationId()).isEqualTo(3L); |
| [39](../../../src/test/java/com/umc/product/challenger/domain/ChallengerRecordTest.java#L39) | ChallengerRecord 도메인 / 사용 처리 시 사용 회원과 시각을 기록한다 | 조건 ChallengerRecord 도메인 / 사용 처리 시 사용 회원과 시각을 기록한다 | 성공: 검증 assertThat(record.isUsed()).isTrue(); assertThat(record.getUsedMemberId()).isEqualTo(100L); assertThat(record.getUsedAt()).isNotNull(); |
| [51](../../../src/test/java/com/umc/product/challenger/domain/ChallengerRecordTest.java#L51) | ChallengerRecord 도메인 / 이미 사용된 코드는 다시 사용할 수 없다 | 조건 ChallengerRecord 도메인 / 이미 사용된 코드는 다시 사용할 수 없다 | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.USED_CHALLENGER_RECORD_CODE; 검증 .isEqualTo(ChallengerErrorCode.USED_CHALLENGER_RECORD_CODE); |
| [63](../../../src/test/java/com/umc/product/challenger/domain/ChallengerRecordTest.java#L63) | ChallengerRecord 도메인 / 기록의 회원 이름과 학교가 요청자 정보와 일치해야 한다 | 조건 ChallengerRecord 도메인 / 기록의 회원 이름과 학교가 요청자 정보와 일치해야 한다 | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.INVALID_MEMBER_NAME_FOR_RECORD, ChallengerErrorCode.INVALID_SCHOOL_FOR_RECORD; 검증 .isEqualTo(ChallengerErrorCode.INVALID_MEMBER_NAME_FOR_RECORD); .isEqualTo(ChallengerErrorCode.INVALID_SCHOOL_FOR_RECORD); |

### ChallengerTest
- 테스트 설명: Challenger 도메인
- 위치: `src/test/java/com/umc/product/challenger/domain/ChallengerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [27](../../../src/test/java/com/umc/product/challenger/domain/ChallengerTest.java#L27) | Challenger 도메인 / 챌린저 생성 시 기본적으로 활성화 상태이다 | 조건 Challenger 도메인 / 챌린저 생성 시 기본적으로 활성화 상태이다 | 성공: 검증 assertThat(challenger.getStatus()).isEqualTo(ChallengerStatus.ACTIVE); |
| [34](../../../src/test/java/com/umc/product/challenger/domain/ChallengerTest.java#L34) | Challenger 도메인 / ACTIVE 챌린저는 파트를 변경할 수 있다 | 조건 Challenger 도메인 / ACTIVE 챌린저는 파트를 변경할 수 있다 | 성공: 검증 assertThat(challenger.getPart()).isEqualTo(ChallengerPart.WEB); |
| [44](../../../src/test/java/com/umc/product/challenger/domain/ChallengerTest.java#L44) | changePart / ACTIVE 상태가 아니면 파트를 변경할 수 없다 | 조건 changePart / ACTIVE 상태가 아니면 파트를 변경할 수 없다 | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.CHALLENGER_NOT_ACTIVE; 검증 .isEqualTo(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE); |
| [58](../../../src/test/java/com/umc/product/challenger/domain/ChallengerTest.java#L58) | changePart / ACTIVE 챌린저는 상태 변경 사유와 수정자를 기록한다 | 조건 changePart / ACTIVE 챌린저는 상태 변경 사유와 수정자를 기록한다 | 성공: 검증 assertThat(challenger.getStatus()).isEqualTo(ChallengerStatus.EXPELLED); assertThat(challenger.getModifiedBy()).isEqualTo(99L); assertThat(challenger.getModificationReason()).isEqualTo("징계"); |
| [70](../../../src/test/java/com/umc/product/challenger/domain/ChallengerTest.java#L70) | changeStatus / ACTIVE 상태가 아니면 다시 상태를 변경할 수 없다 | 조건 changeStatus / ACTIVE 상태가 아니면 다시 상태를 변경할 수 없다 | 실패: 예외 ChallengerDomainException; 에러코드 ChallengerErrorCode.CHALLENGER_NOT_ACTIVE; 검증 .isEqualTo(ChallengerErrorCode.CHALLENGER_NOT_ACTIVE); |
| [82](../../../src/test/java/com/umc/product/challenger/domain/ChallengerTest.java#L82) | changeStatus / 상벌점 총합을 계산한다 | 조건 changeStatus / 상벌점 총합을 계산한다 | 성공: 검증 assertThat(challenger.getTotalPoints()).isEqualTo(-3.5); |
