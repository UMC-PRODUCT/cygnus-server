# Recruiting 테스트 카탈로그

이 문서는 `src/test/java/com/umc/product/recruiting`의 테스트 범위를 계층별로 정리한다. 실제 assertion과 최신 케이스 이름은 각 테스트 소스가 기준이다.

## 도메인 규칙

| 테스트 | 검증 케이스 |
|---|---|
| `RecruitingCoreDomainTest` | Season 생성, Round 상태, Application 생성·제출·판정·등록 상태의 기본 전이와 잘못된 전이 차단 |
| `RecruitingSeasonTrackQuotaTest` | TO 음수, `INFRA_PLUS`, target 변경 제한 |
| `RecruitingSeasonLifecycleTest` | Season의 memo와 상태 비소유 모델 |
| `RecruitingRoundConfigurationTest` | 모집 트랙 필수·중복·null·`INFRA_PLUS`, 면접 진행/생략 설정 |
| `RecruitingRoundScheduleValidationTest` | 접수·서류 발표·면접·최종 발표 순서와 null 조합 |
| `RecruitingRoundApplicationWindowTest` | DRAFT/CLOSED/Form 미게시 차단, 시작 포함·종료 제외 경계 |
| `RecruitingFormApplicationV2DomainTest` | Round당 Form 하나, COMMON/TRACK 정책, 기본 지원자 문항과 1·2지망 제한 |
| `RecruitingEvaluationDomainTest` | 평가 decision 필수, `APPROVED`/`REJECTED`, comment 2000자 경계와 수정 |
| `RecruitingEvaluatorQuestionDomainTest` | evaluator ID, 질문 content/order, 공통 질문 생성자·최종 변경자, 비활성화 |
| `RecruitingInterviewScheduleDomainTest` | 요청·응답·확정 상태, 시간·장소·연락처, 메일 성공/실패와 재시도 |

## Command 서비스

| 테스트 | 검증 케이스 |
|---|---|
| `RecruitingSeasonCommandServiceTest` | Season 생성 권한, 중복 Season, quota 교체, 지부 전체 TO와 학교별 파트 TO 합계 일치, 사용량 미만 감소 거부, Round 트랙 보존 |
| `RecruitingRoundCreateCommandServiceTest` | 본/추가모집 차수, 양수 TO 부분집합, 중복 차수와 0 TO, REST·GraphQL 공용 UseCase의 `INFRA_PLUS` 거부 |
| `RecruitingRoundUpdateCommandServiceTest` | 제목·설정 수정, 대소문자 제목 중복, 지원서 존재 후 모집 정책 잠금, Form 제목 동기화, 게시된 availability Form의 OPEN 조건, 매핑이 없는 면접 Round OPEN 시 조율 Form 자동 생성과 이미 매핑·면접 미진행 시 생성 생략, 수정 요청에 매핑이 없을 때 기존 값 승계, 지원서/FormResponse 없는 OPEN의 DRAFT 복귀와 데이터 존재 시 충돌 |
| `RecruitingRoundLifecycleCommandServiceTest` | DRAFT hard delete 조건과 명시적 삭제 순서, 원본/대상 권한, 복제 설정·Form 조건부 이동 재매핑, availability Form 초기화 |
| `RecruitingApplicationFormStructureCommandServiceTest` | 전체 Form 생성, section client key를 실제 ID로 변환, TRACK 간 잘못된 이동 및 다른 Form ID 거부 |
| `RecruitingApplicationFormCommandServiceTest` | Round OPEN/DRAFT/CLOSED에 사용되는 Recruiting Form과 실제 Form 게시·취소·마감 lock 및 호출 순서, Season scope, 게시 전 정책 검증 |
| `RecruitingApplicationFormValidationServiceTest` | 모든 section 정책, 모집 트랙별 TRACK section, 조건부 이동 대상 정책 검증 |
| `RecruitingApplicationCommandServiceTest` | 로그인·익명 초안 생성, 수정, 제출, 제출 후 수정, 개인정보 동의, credential, 익명 철회 |
| `RecruitingApplicationValidationServiceTest` | Round·기수 내 email/member 중복, 이전 실패 후 재지원, 이전 합격/진행 지원 차단, FormResponse 소유권 |
| `RecruitingApplicationKeyIssuerTest` | application key 충돌 재발급, 10회 실패 처리 |
| `RecruitingRoundEvaluatorCommandServiceTest` | Round evaluator 등록 권한과 중복 처리 |
| `RecruitingRoundEvaluatorRemovalCommandServiceTest` | evaluator 삭제 권한, 존재하지 않는 등록 처리 |
| `RecruitingApplicationEvaluationCommandServiceTest` | whitelist 평가 허용, 비평가자 거부, stage 상태 검증, 최종 판정 전 기존 평가 Upsert |
| `RecruitingRoundInterviewQuestionCommandServiceTest` | 공통 질문 생성·수정·비활성화 권한과 audit member ID |
| `RecruitingRoundInterviewQuestionPreSubmissionCommandServiceTest` | 최초 면접 평가 전 공통 질문 변경 허용, 이후 동결 |
| `RecruitingApplicationInterviewQuestionCommandServiceTest` | 개별 질문 관리 권한과 Application scope |
| `RecruitingApplicationInterviewQuestionPreSubmissionCommandServiceTest` | 최초 면접 평가 전 개별 질문 변경 허용, 이후 동결 |
| `RecruitingDecisionCommandServiceTest` | 서류 불합격, 면접 진행 시 일정 자동 생성, 면접 미진행 시 즉시 skip, 최종 판정 권한·acceptedTrack·중복 합격 |
| `RecruitingInterviewAvailabilityFormProvisionerTest` | 면접 일정 조율 Form을 비익명으로 만들고 SCHEDULE 필수 질문 하나를 넣어 게시하는지, 차수 제목 기반 Form 제목과 요청자 ID 전달 |
| `RecruitingRoundOpenAvailabilityFormIntegrationTest` | 실제 Form 모듈과 DB로 OPEN 전이를 태워 자동 생성된 조율 Form이 곧바로 뒤따르는 검증을 통과하는지, OPEN 차수에서 면접을 껐다 켜면 Form을 새로 만들어 수정이 완료되는지 |
| `RecruitingInterviewAvailabilityRequestCoordinatorTest` | 일정 row와 Outbox의 같은 transaction 생성, 멱등 재요청과 실패 재시도 |
| `RecruitingInterviewCommandServiceTest` | 면접 생략 시 Application 전이와 기존 일정 `CANCELLED`, 일정 후보 overlap 위임 |
| `RecruitingInterviewScheduleCommandServiceTest` | 로그인한 면접 대상 지원자 본인만 `AVAILABILITY_REQUESTED` 일정에 제출, published·기명 Form의 지정된 sole-required `SCHEDULE` 질문 검증, FormResponse 즉시 최종 제출·ID 저장·`AVAILABILITY_SUBMITTED` 전이, 비어 있지 않은 times/null·Form 검증 오류, 시작 포함·종료 제외 기간과 `RECRUITING-0413`, 실패 시 일정 상태·응답 ID 미변경 |
| `RecruitingInterviewSessionCommandServiceTest` | 운영진의 Round 면접 세션 생성·수정·삭제, 15분 배수 슬롯 길이 설정, 다른 Round 차단, 확정/참조 일정이 있는 세션 변경·삭제 차단 |
| `RecruitingInterviewScheduleBatchCommandServiceTest` | 세션 슬롯·지원자·가능 응답을 잠근 뒤 여러 일정을 한 transaction에서 일괄 확정하고, 중복·범위·가능 시간 충돌과 부분 실패 rollback을 검증 |
| `RecruitingInterviewAvailabilitySubmissionIntegrationTest` | 실제 published Form `SCHEDULE` 응답이 기명 `SUBMITTED` FormResponse로 저장되는지, 빈 times Form 거부 시 FormResponse와 일정 상태가 함께 rollback되는지, application lock으로 동시 두 제출 중 한 번만 성공하는지, Form 가능 시간이 보드 슬롯과 batch 확정 결과로 이어지는지 |
| `RecruitingInterviewMailDeliveryCommandServiceTest` | 요청/확정 메일 상태 저장과 잘못된 상태 차단 |
| `RecruitingRegistrationCommandServiceTest` | 중앙 권한, quota lock, READY 예약·취소, REGISTERED와 Challenger track 멱등 추가 |
| `RecruitingManagementAuthorizationServiceTest` | 학교 회장단·중앙 총괄단·SUPER_ADMIN scope와 evaluator 권한 분리 |

## Query 서비스

| 테스트 | 검증 케이스 |
|---|---|
| `RecruitingSeasonQueryServiceTest` | 현재 학교 지부 결합, 관리자 Season/Round 필터, 권한 없는 Season 제외, 공개 종료 시각 OPEN/PAST 경계, 복수 학교·Round와 학교명 AND/OR 검색 |
| `RecruitingQueryServiceTest` | 중앙 통계 권한, 복수 학교·Round와 학교명 집계, 지원서 없는 학교·Round 0건 그룹, 선택 트랙 밖 조건부 이동 option 제외 |
| `RecruitingApplicationQuestionScopeQueryServiceTest` | COMMON + 1·2지망 TRACK의 allowed IDs와 Form required IDs 교집합 |
| `RecruitingPublicApplicationQueryServiceTest` | credential 검증, email 정규화, 서류·최종 발표 직전/정시 결과 마스킹 |
| `RecruitingApplicationReviewQueryServiceTest` | evaluator/운영진 목록·상세 권한, DRAFT 제외, 다중 상태·1·2지망 트랙 필터와 pagination count, 본인 평가 여부, 익명/회원 Form 답변 조회 |
| `RecruitingApplicationEvaluationQueryServiceTest` | 본인 평가 제출 전 타 평가 비공개, 제출 후 공개, 관리자 bypass |
| `RecruitingDecisionHistoryQueryServiceTest` | 평가 이력 권한·상태 집계·CSV 마스킹, 복수 지부/학교 OR와 두 목록 AND 범위 |
| `RecruitingEvaluatorQuestionQueryServiceTest` | 공통·개별 질문 조회 scope와 active 정렬 |
| `RecruitingInterviewScheduleQueryServiceTest` | 지원자와 운영진 일정 조회 권한 및 응답 변환 |
| `RecruitingInterviewSessionQueryServiceTest` | 운영진 세션 목록/단건 조회, 시작 시각 순서 유지, 다른 Round 및 권한 없는 조회 차단 |
| `RecruitingInterviewScheduleBoardQueryServiceTest` | `Asia/Seoul` 날짜 경계, 세션별 슬롯 길이 계산, 가능 지원자·대기/확정 지원자 결합, 저장된 확정 슬롯 불일치 차단 |
| `RecruitingInterviewMailDeliveryQueryServiceTest` | 일정·지원자·Round의 메일 발송 정보 조합 |
| `RecruitingCsvExportServiceTest` | 고정 header, masking, 원문 email·이름·키·답변·평가 정보 제외 |

## 영속성과 동시성

| 테스트 | 검증 케이스 |
|---|---|
| `RecruitingSeasonRoundPersistenceAdapterTest` | Season/TO/Round 저장·조회, quota 유일성, 다중 Season Round fetch |
| `RecruitingRoundCreationConcurrencyTest` | Season lock 기반 추가모집 순차 차수, 동일 제목 동시 생성 한 건만 성공 |
| `RecruitingPersistenceAdapterTest` | Application·Form·정책 저장과 조회, summary projection |
| `RecruitingFormSectionPolicyPersistenceAdapterTest` | section 정책 유일성, Form 단위 조회·삭제 |
| `RecruitingEvaluatorQuestionPersistenceAdapterTest` | evaluator와 공통·개별 질문 유일성, audit와 active 정렬 |
| `RecruitingEvaluationSchedulePersistenceAdapterTest` | 평가 유일성, decision 필수, 질문 동결 기준, Application당 일정 하나, 확정 시간 제약 |
| `RecruitingApplicantConcurrencyTest` | 같은 Round email/member 및 기수 재지원의 병렬 요청 직렬화 |
| `RecruitingEvaluationQuestionConcurrencyTest` | 최초 면접 평가와 질문 변경 경합 시 동결 일관성 |
| `RecruitingQuotaReservationConcurrencyTest` | 마지막 TO 한 자리에 대한 병렬 READY 중 하나만 성공 |
| `RecruitingApplicationDatabaseInvariantTest` | email/key, Round email/member, 트랙·개인정보·상태 DB CHECK |
| `RecruitingRegistrationDatabaseInvariantTest` | FINAL_PASSED acceptedTrack과 registration 상태 DB CHECK |
| `RecruitingLockExceptionTranslatorTest` | lock timeout/deadlock 예외를 Recruiting 충돌 오류로 변환 |
| `RecruitingInterviewScheduleConcurrencyTest` | 다중 assignment 일괄 확정의 row lock·중복 슬롯 충돌·부분 실패 rollback을 Testcontainers PostgreSQL에서 검증한다. |
| `RecruitingApplicationKeyGeneratorTest` | SecureRandom key 형식과 alphabet |

## Migration

| 테스트 | 검증 케이스 |
|---|---|
| `RecruitingSeasonRoundMigrationTest` | 빈 DB에서 Season/TO/Round schema와 제약 생성 |
| `RecruitingRoundScheduleMigrationTest` | 역전 일정, 필수 시각 누락, 면접 생략/필수 조합 DB 거부 |
| `RecruitingFormApplicationMigrationTest` | Form 정책, 지원자 기본 문항, key와 identity mode 제약 |
| `RecruitingEvaluationScheduleMigrationTest` | evaluator, 질문 audit, 평가 enum/유일성, 일정 `CANCELLED`와 mail shape |

Recruiting migration은 최초 배포 전이라는 전제에서 `V2026.07.15.13.30__create_recruiting_domain.sql` 하나로 통합한다. `RecruitingFormApplicationMigrationTestSupport`와 `RecruitingPersistenceAdapterTestSupport`는 fixture와 공통 검증을 제공한다.

## REST와 OpenAPI

| 테스트 | 검증 케이스 |
|---|---|
| `RecruitingPublicControllerTest` | 공개 Round 필터, 익명 생성·조회·철회, key 비노출과 email 정규화 |
| `RecruitingApplicationControllerTest` | 로그인 초안 생성과 인증·입력 검증 |
| `RecruitingApplicationMutationControllerTest` | 로그인 수정·제출·철회와 CurrentMember 전달 |
| `RecruitingInterviewScheduleControllerTest` | `SCHEDULE-001`의 times/CurrentMember command 변환, body·빈 목록·null 요소 validation, `RECRUITING-0413` error envelope |
| `RecruitingSeasonAdminControllerTest` | Season 설정·memo·TO, 그룹 Round 조회와 필수 gisu, 제목 확인, 생성·복제·삭제 command 변환 |
| `RecruitingRoundAdminControllerTest` | Round 생성·수정·상태 요청과 title/일정 schema |
| `RecruitingAdminControllerTest` | Form Upsert, 제거된 별도 게시/마감 route, 서류·최종 판정, skip, 등록, summary와 반복 `chapterIds`·`schoolIds` 평가 이력/CSV |
| `RecruitingAdminEvaluatorController` 범위 (`RecruitingManagementControllerTest`) | evaluator actor/target 분리 |
| `RecruitingAdminQuestionController` 범위 (`RecruitingManagementControllerTest`) | 질문 validation과 actor 전달 |
| `RecruitingAdminInterviewController` 범위 (`RecruitingManagementControllerTest`) | 요청·단건 확정·세션 생성·KST 보드 조회·batch 확정의 command 변환과 `sessionId` validation |
| `RecruitingRestContractTest` 면접 세션 범위 | 세션 CRUD, KST 보드, batch confirmation 및 단건 confirmation의 `sessionId` contract |
| `RecruitingApplicationReviewControllerTest` | Round 지원서 목록 필터·페이지와 CurrentMember 전달 |
| `RecruitingEvaluationControllerTest` | 평가 `PUT`, stage/path scope, CurrentMember, 평가 조회 |
| `RecruitingCredentialRestRateLimitInterceptorTest` | lookup/update/submit/cancel의 IP bucket 공유, 생성 제외, 429 header |
| `RecruitingRestContractTest` | 모든 admin prefix, OpenAPI description, 숫자형 operation ID의 중복·카테고리 대역, actor 및 파생 validation 필드 비노출, legacy route 제거 |
| `RecruitingApplicationRandomPortIntegrationTest` | 실제 REST/GraphQL socket, JWT/익명 보안, malformed email, credential 조회, CSV actor, P6Spy 민감정보 비노출 |

`RecruitingApplicationControllerTestSupport`와 `RecruitingHttpTestPayloads`는 web 테스트 공통 fixture다.

## GraphQL

| 테스트 | 검증 케이스 |
|---|---|
| `RecruitingGraphQlSurfaceTest` | schema Query/Mutation/enum/input/output 표면, 평가 이력 `chapterIds`·`schoolIds` 목록 입력과 제거된 legacy field |
| `RecruitingGraphQlArchitectureTest` | GraphQL adapter가 UseCase만 의존하고 persistence를 직접 참조하지 않음 |
| `RecruitingGraphQlSecurityTest` | 공개 Query·익명 mutation과 인증/관리 mutation 경계, CurrentMember 적용 |
| `RecruitingGraphQlExceptionAdviceTest` | domain·validation·authorization 오류 code 변환 |
| `RecruitingCredentialGraphQlRateLimitInterceptorTest` | alias/fragment 우회 차단, cancel 포함 credential field count, 비credential 통과 |
| `RecruitingSeasonAdminGraphQlControllerTest` | Season별 Round 그룹, 필터, Season memo/TO, 평가 이력 다중 범위 입력과 validation |
| `RecruitingRoundAdminGraphQlControllerTest` | Round title·생성·수정·상태·복제·삭제 mutation |
| `RecruitingFormAdminGraphQlControllerTest` | Form 전체 구조 Upsert와 조건부 section key |
| `RecruitingEvaluatorQuestionGraphQlControllerTest` | evaluator와 공통·개별 질문 Query/Mutation |
| `RecruitingEvaluationGraphQlControllerTest` | 평가 Upsert와 visibility Query |
| `RecruitingApplicationReviewGraphQlControllerTest` | 평가용 지원서 필터·페이지 Query와 CurrentMember 전달 |
| `RecruitingDecisionGraphQlControllerTest` | 서류·최종 판정, 면접 생략, 등록 mutation |
| `RecruitingScheduleGraphQlControllerTest` | 일정 요청·확정과 `submitRecruitingInterviewAvailability(applicationId, input { times })`의 CurrentMember 전달, `Boolean!` 반환, 누락·빈·null times validation, `RECRUITING-0413` extension |
| `RecruitingScheduleGraphQlControllerTest` 세션 범위 | 세션 CRUD, 단건 조회 facade, KST 보드, batch confirmation의 `seasonId`/`roundId` 권한 및 `sessionId` 전달 |
| `RecruitingGraphQlRandomPortIntegrationTest` | 실제 GraphQL HTTP 실행, schema validation과 보안 응답 |

## Event와 메일

| 테스트 | 검증 케이스 |
|---|---|
| `InterviewAvailabilityRequestedEventListenerTest` | Thymeleaf 변수, HTML 메일 성공, 실패 오류 sanitizing과 재throw, SENT 멱등 처리, CANCELLED 일정 발송 차단 |

## 실행 명령

```bash
./gradlew compileJava compileTestJava
./gradlew test
```

변경 범위가 좁더라도 마지막에는 전체 테스트를 실행한다. PostgreSQL 제약과 pessimistic lock 검증은 Testcontainers를 사용하므로 Docker 실행 환경이 필요하다.
