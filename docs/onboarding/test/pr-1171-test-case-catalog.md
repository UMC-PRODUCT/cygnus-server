# PR #1171 신규 테스트 전체 목록

> 테스트 변경 비교 기준: `origin/develop` `ac1c18866e` ... 구현 commit `4f5dccea81`
> 소스 범위: `src/test/java/com/umc/product`
> 목록 단위: PR diff에서 새로 추가된 JUnit test annotation 1,678개

## 1. 범위와 집계 기준

- 이 문서는 PR에서 새로 추가한 `@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, `@TestTemplate` source method 1,678개를 각각 나열한다.
- 테스트 관련 변경 Java 파일은 434개이며, 그중 신규 test annotation을 포함한 파일은 417개다. fixture·scenario helper처럼 직접 실행 annotation이 없는 지원 파일은 개별 테스트 목록에서 제외한다.
- `@ParameterizedTest` 한 method는 여러 runtime invocation으로 확장될 수 있다. 따라서 이 문서의 source test 수와 Gradle XML의 전체 실행 건수 5,002건은 서로 다른 지표다.
- 기존 test method 내부에 assertion이나 fixture만 보강하고 annotation을 새로 추가하지 않은 경우에는 중복 나열하지 않는다.
- `신규 파일`은 develop에 없던 테스트 파일, `보강 파일`은 develop에 존재하던 파일에 새 테스트를 추가한 경우다.

## 2. 도메인별 신규 source test 요약

| 도메인 | 파일 | 신규 파일 | 보강 파일 | 신규 source test |
|---|---:|---:|---:|---:|
| Application root | 1 | 1 | 0 | 1 |
| `common` | 3 | 1 | 2 | 6 |
| `global` | 42 | 16 | 26 | 119 |
| `audit` | 4 | 4 | 0 | 15 |
| `documentation` | 1 | 0 | 1 | 1 |
| `maintenance` | 7 | 6 | 1 | 14 |
| `storage` | 8 | 3 | 5 | 33 |
| `llm` | 3 | 3 | 0 | 17 |
| `term` | 4 | 4 | 0 | 13 |
| `blog` | 9 | 9 | 0 | 61 |
| `certificate` | 11 | 2 | 9 | 26 |
| `chat` | 19 | 4 | 15 | 36 |
| `community` | 47 | 20 | 27 | 166 |
| `feedback` | 5 | 5 | 0 | 20 |
| `notice` | 17 | 16 | 1 | 81 |
| `analytics` | 10 | 4 | 6 | 19 |
| `notification` | 23 | 14 | 9 | 60 |
| `schedule` | 11 | 8 | 3 | 83 |
| `curriculum` | 15 | 14 | 1 | 63 |
| `form` | 20 | 19 | 1 | 100 |
| `project` | 32 | 8 | 24 | 130 |
| `recruiting` | 31 | 8 | 23 | 83 |
| `member` | 19 | 12 | 7 | 75 |
| `challenger` | 13 | 12 | 1 | 89 |
| `authentication` | 17 | 14 | 3 | 125 |
| `authorization` | 16 | 12 | 4 | 88 |
| `organization` | 17 | 15 | 2 | 120 |
| Production test seed | 12 | 6 | 6 | 34 |
| **전체** | **417** | **240** | **177** | **1,678** |

## 3. 테스트별 검증 상황

각 항목은 `소스 라인 · annotation 유형 · method — 검증 상황 · parameter source` 순서다.

## 3.1 Application root — 1개

### Contract / Misc

#### UmcProductApplicationTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/UmcProductApplicationTest.java`

1. [L13](../../../src/test/java/com/umc/product/UmcProductApplicationTest.java#L13) · `@Test` · `main은_Spring_Boot를_시작한다` — main은 전달받은 인자로 Spring Boot 애플리케이션을 시작한다

## 3.2 `common` — 6개

### Domain

#### ChallengerRoleTypeTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/common/domain/enums/ChallengerRoleTypeTest.java`

1. [L34](../../../src/test/java/com/umc/product/common/domain/enums/ChallengerRoleTypeTest.java#L34) · `@Test` · `모든_역할의_조직_유형을_분류한다` — 모든 운영진 역할은 중앙·지부·학교 조직으로 빠짐없이 분류된다
2. [L52](../../../src/test/java/com/umc/product/common/domain/enums/ChallengerRoleTypeTest.java#L52) · `@Test` · `역할_계층을_판정한다` — 역할 계층 predicate는 각 경계 역할만 허용한다

#### ChallengerTrackTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/common/domain/enums/ChallengerTrackTest.java`

1. [L46](../../../src/test/java/com/umc/product/common/domain/enums/ChallengerTrackTest.java#L46) · `@Test` · `null_파트는_모집_트랙으로_변환할_수_없다` — null 파트는 모집 트랙으로 변환할 수 없다

#### CommonEnumResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/common/domain/enums/CommonEnumResidualTest.java`

1. [L16](../../../src/test/java/com/umc/product/common/domain/enums/CommonEnumResidualTest.java#L16) · `@Test` · `챌린저_파트를_변환한다` — 챌린저 파트는 정확한 이름만 변환한다
2. [L26](../../../src/test/java/com/umc/product/common/domain/enums/CommonEnumResidualTest.java#L26) · `@Test` · `OAuth_provider를_변환한다` — OAuth provider는 대소문자를 정규화하고 미지원 값을 거부한다
3. [L35](../../../src/test/java/com/umc/product/common/domain/enums/CommonEnumResidualTest.java#L35) · `@Test` · `공통_예외의_메시지를_보존한다` — 공통 예외는 호출자가 지정한 안전한 메시지를 보존한다

## 3.3 `global` — 119개

### Application Service

#### EventOutboxRelayServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java`

1. [L314](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L314) · `@Test` · `relay_failure_record_optimistic_lock` — 실패 상태 저장 중 lease를 잃으면 새 소유자의 상태를 덮어쓰지 않는다
2. [L350](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L350) · `@Test` · `relay_backoff_cap_and_blank_error` — 재시도 횟수가 커지면 backoff를 5분으로 제한하고 blank 오류는 class명으로 저장한다
3. [L379](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java#L379) · `@Test` · `constructor_without_tracer_bean` — Autowired 생성자는 Tracer bean이 없으면 NOOP tracer로 구성된다

#### StompClientMessageIdResolverRegistryTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/websocket/application/service/StompClientMessageIdResolverRegistryTest.java`

1. [L53](../../../src/test/java/com/umc/product/global/websocket/application/service/StompClientMessageIdResolverRegistryTest.java#L53) · `@Test` · `nullInputShortCircuits` — destination 또는 payload가 null이면 resolver를 호출하지 않고 빈 값을 반환한다

### Domain

#### CacheNamespaceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/cache/domain/CacheNamespaceTest.java`

1. [L29](../../../src/test/java/com/umc/product/global/cache/domain/CacheNamespaceTest.java#L29) · `@Test` · `duplicate_namespace_count` — 중복 namespace count는 즉시 거부한다

#### CacheSpecTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/cache/domain/CacheSpecTest.java`

1. [L56](../../../src/test/java/com/umc/product/global/cache/domain/CacheSpecTest.java#L56) · `@Test` · `필수값_검증` — namespace와 valueType은 필수다
2. [L76](../../../src/test/java/com/umc/product/global/cache/domain/CacheSpecTest.java#L76) · `@Test` · `ttl_null_음수_검증` — ttl은 null이거나 음수일 수 없다

#### EventOutboxTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java`

1. [L41](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L41) · `@Test` · `domain_event_null_검증` — domain event는 필수다
2. [L72](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L72) · `@Test` · `processing_lease_null_검증` — processing lease 만료 시각은 필수다

### Global Infrastructure

#### ClientRequestClassifierTest (6개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/client/ClientRequestClassifierTest.java`

1. [L211](../../../src/test/java/com/umc/product/global/client/ClientRequestClassifierTest.java#L211) · `@Test` · `null_client_claim_분류` — principal의 client claim이 null이면 빈 claim으로 안전하게 분류한다
2. [L228](../../../src/test/java/com/umc/product/global/client/ClientRequestClassifierTest.java#L228) · `@Test` · `unknown_service_origin_환경_유지` — 등록 Origin의 서비스가 UNKNOWN이고 토큰도 없으면 Origin 환경만 유지한다
3. [L241](../../../src/test/java/com/umc/product/global/client/ClientRequestClassifierTest.java#L241) · `@Test` · `잘못된_referer_무시` — Referer가 blank, 상대 경로, 잘못된 URI이면 Origin을 유추하지 않는다
4. [L252](../../../src/test/java/com/umc/product/global/client/ClientRequestClassifierTest.java#L252) · `@Test` · `referer_port_포함` — Referer의 명시적 port까지 포함하여 Origin을 유추한다
5. [L263](../../../src/test/java/com/umc/product/global/client/ClientRequestClassifierTest.java#L263) · `@Test` · `blank_user_agent` — blank User-Agent는 UNKNOWN 기기로 분류한다
6. [L270](../../../src/test/java/com/umc/product/global/client/ClientRequestClassifierTest.java#L270) · `@Test` · `origin_registry_정규화` — Origin registry는 blank를 무시하고 slash를 제거하며 중복은 마지막 설정을 사용한다

#### AsyncConfigTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/config/AsyncConfigTest.java`

1. [L12](../../../src/test/java/com/umc/product/global/config/AsyncConfigTest.java#L12) · `@Test` · `async_exception_handler` — 비동기 예외 handler는 method와 예외를 안전하게 기록한다

#### CustomHighlightConverterTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/config/CustomHighlightConverterTest.java`

1. [L17](../../../src/test/java/com/umc/product/global/config/CustomHighlightConverterTest.java#L17) · `@Test` · `log_level_색상을_변환한다` — 각 log level을 고정된 ANSI 색상으로 변환하고 미지원 level은 기본색을 사용한다

#### FcmConfigTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/config/FcmConfigTest.java`

1. [L24](../../../src/test/java/com/umc/product/global/config/FcmConfigTest.java#L24) · `@Test` · `기존_FirebaseApp을_재사용한다` — 기본 FirebaseApp이 이미 있으면 재초기화 없이 messaging을 반환한다
2. [L46](../../../src/test/java/com/umc/product/global/config/FcmConfigTest.java#L46) · `@Test` · `FirebaseApp을_신규_초기화한다` — FirebaseApp이 없으면 credential 기반 기본 app을 한 번 초기화한다

#### GraphQlExecutionConfigTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/config/GraphQlExecutionConfigTest.java`

1. [L28](../../../src/test/java/com/umc/product/global/config/GraphQlExecutionConfigTest.java#L28) · `@Test` · `execution_properties_기본값과_검증` — 실행 설정은 null에 기본값을 적용하고 0 이하 값을 거부한다
2. [L50](../../../src/test/java/com/umc/product/global/config/GraphQlExecutionConfigTest.java#L50) · `@Test` · `timeout과_depth_instrumentation_생성` — timeout과 depth instrumentation bean을 설정값으로 생성한다
3. [L65](../../../src/test/java/com/umc/product/global/config/GraphQlExecutionConfigTest.java#L65) · `@Test` · `non_number_page_size` — page.size가 숫자가 아니면 최대 비용으로 계산한다

#### GraphQlScalarResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/config/GraphQlScalarResidualTest.java`

1. [L32](../../../src/test/java/com/umc/product/global/config/GraphQlScalarResidualTest.java#L32) · `@Test` · `Instant_scalar를_검증한다` — Instant scalar는 ISO-8601 값의 serialize·variable·literal 왕복만 허용한다
2. [L62](../../../src/test/java/com/umc/product/global/config/GraphQlScalarResidualTest.java#L62) · `@Test` · `Long_scalar의_호환_타입을_검증한다` — Long scalar는 정수 호환 타입을 exact long으로 변환한다
3. [L81](../../../src/test/java/com/umc/product/global/config/GraphQlScalarResidualTest.java#L81) · `@Test` · `Long_scalar의_잘못된_값을_거부한다` — Long scalar는 소수·범위 초과·미지원 타입을 단계별 coercing 예외로 변환한다

#### LoggingInterceptorTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java`

1. [L238](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L238) · `@Test` · `authenticated_non_member_principal` — 인증 객체의 principal이 MemberPrincipal이 아니면 회원 MDC를 채우지 않는다
2. [L254](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L254) · `@Test` · `after_completion_with_exception` — 처리 예외는 class명만 MDC에 기록하고 민감 message는 완료 로그에 노출하지 않는다
3. [L273](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L273) · `@Test` · `after_completion_fallback_context_and_status` — client context attribute가 없고 비표준 status여도 UNKNOWN 값으로 완료 메트릭을 남긴다

#### P6SpyConfigTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/config/P6SpyConfigTest.java`

1. [L99](../../../src/test/java/com/umc/product/global/config/P6SpyConfigTest.java#L99) · `@Test` · `blank와_bound_sql_fallback` — blank SQL은 그대로 유지하고 prepared가 없으면 bound SQL을 사용한다
2. [L107](../../../src/test/java/com/umc/product/global/config/P6SpyConfigTest.java#L107) · `@Test` · `line_comment와_dollar_quote_redaction` — line comment와 dollar quoted 문자열도 민감값을 치환한다
3. [L119](../../../src/test/java/com/umc/product/global/config/P6SpyConfigTest.java#L119) · `@Test` · `ddl과_non_statement_format` — statement DDL과 statement가 아닌 category를 각각 형식화한다

#### QueryStatsJdbcEventListenerTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java`

1. [L141](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L141) · `@Test` · `p6spy_execute_callback_variants` — P6Spy의 prepared·statement 실행 변형을 모두 동일하게 집계한다
2. [L170](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L170) · `@Test` · `after_without_before와_실패_query` — span 없이 after callback만 수신해도 stack을 정리하고 성공 쿼리만 기록한다
3. [L182](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L182) · `@Test` · `unknown_operation과_sql_length_limit` — null·해석 불가 SQL은 UNKNOWN이며 span SQL 길이 제한을 적용한다
4. [L207](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L207) · `@Test` · `query_stats_holder_noop_before_init` — QueryStatsHolder는 초기화 전 record를 무시하고 기본값을 반환한다

#### SecurityConfigIntegrationTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java`

1. [L110](../../../src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java#L110) · `@Test` · `publicEndpointWithoutMethodAllowsAllMethods` — HTTP method가 지정되지 않은 Public endpoint는 모든 method에 공개한다

#### SecurityConfigResidualTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/config/SecurityConfigResidualTest.java`

1. [L17](../../../src/test/java/com/umc/product/global/config/SecurityConfigResidualTest.java#L17) · `@Test` · `dummy_user_details_service_rejects_lookup` — dummy UserDetailsService는 password 인증을 시도하면 명시적으로 거부한다

#### SensitiveDataSanitizingTurboFilterTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/config/SensitiveDataSanitizingTurboFilterTest.java`

1. [L101](../../../src/test/java/com/umc/product/global/config/SensitiveDataSanitizingTurboFilterTest.java#L101) · `@Test` · `marker_보존` — 민감값을 정제한 로그에도 marker를 유지한다

#### WebSocketBrokerPropertiesValidatorTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/config/WebSocketBrokerPropertiesValidatorTest.java`

1. [L166](../../../src/test/java/com/umc/product/global/config/WebSocketBrokerPropertiesValidatorTest.java#L166) · `@Test` · `validate_relayNumericBoundaries` — relay port·heartbeat·startup timeout의 숫자 경계를 검증한다

#### WebSocketMessageBrokerConfigTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/config/WebSocketMessageBrokerConfigTest.java`

1. [L190](../../../src/test/java/com/umc/product/global/config/WebSocketMessageBrokerConfigTest.java#L190) · `@Test` · `outboundTaskWithParentObservation` — outbound 작업은 현재 observation이 있으면 child observation 안에서 실행한다

#### ExceptionResidualTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/exception/ExceptionResidualTest.java`

1. [L39](../../../src/test/java/com/umc/product/global/exception/ExceptionResidualTest.java#L39) · `@Test` · `ConstraintViolation_메시지를_결합한다` — ConstraintViolation은 모든 사용자 메시지를 순서대로 결합한다
2. [L54](../../../src/test/java/com/umc/product/global/exception/ExceptionResidualTest.java#L54) · `@Test` · `MethodArgumentNotValid_필드_오류를_병합한다` — @Valid field 오류는 같은 필드 메시지를 합치고 null 기본 메시지는 빈 값으로 처리한다
3. [L76](../../../src/test/java/com/umc/product/global/exception/ExceptionResidualTest.java#L76) · `@Test` · `HttpMessageNotReadable_메시지를_축약한다` — 읽을 수 없는 body는 원인 유형별 안전한 안내 문구로 축약한다
4. [L86](../../../src/test/java/com/umc/product/global/exception/ExceptionResidualTest.java#L86) · `@Test` · `request_parameter_오류를_변환한다` — 필수 parameter 누락과 타입 불일치는 actionable BAD_REQUEST로 변환한다
5. [L105](../../../src/test/java/com/umc/product/global/exception/ExceptionResidualTest.java#L105) · `@Test` · `미처리_예외의_profile별_마스킹을_검증한다` — 미처리 예외는 prod에서 내부 메시지를 숨기고 local에서만 원인을 제공한다
6. [L118](../../../src/test/java/com/umc/product/global/exception/ExceptionResidualTest.java#L118) · `@Test` · `fallback_status와_root_cause를_변환한다` — CustomErrorController는 servlet status와 중첩 root cause를 공통 envelope로 변환한다
7. [L139](../../../src/test/java/com/umc/product/global/exception/ExceptionResidualTest.java#L139) · `@Test` · `예외_생성자를_검증한다` — NotImplementedException과 BusinessException cause 생성자는 code·message·cause를 보존한다

#### GraphQlExceptionAdviceTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/exception/GraphQlExceptionAdviceTest.java`

1. [L27](../../../src/test/java/com/umc/product/global/exception/GraphQlExceptionAdviceTest.java#L27) · `@Test` · `접근_거부를_변환한다` — 접근 거부는 FORBIDDEN error type과 공통 확장 필드로 변환한다
2. [L35](../../../src/test/java/com/umc/product/global/exception/GraphQlExceptionAdviceTest.java#L35) · `@Test` · `BusinessException_status를_매핑한다` — BusinessException의 HTTP status를 GraphQL error type으로 정확히 매핑한다
3. [L50](../../../src/test/java/com/umc/product/global/exception/GraphQlExceptionAdviceTest.java#L50) · `@Test` · `입력과_미처리_예외를_변환한다` — 입력 예외와 미처리 예외는 각각 BAD_REQUEST와 마스킹된 INTERNAL_ERROR로 변환한다

#### ExternalApiCallLoggerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/logging/ExternalApiCallLoggerTest.java`

1. [L117](../../../src/test/java/com/umc/product/global/logging/ExternalApiCallLoggerTest.java#L117) · `@Test` · `metrics_binder_lifecycle` — metrics binder가 연결되면 호출 메트릭을 기록하고 종료 시 연결을 해제한다

#### OperationalMetricsTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/logging/OperationalMetricsTest.java`

1. [L97](../../../src/test/java/com/umc/product/global/logging/OperationalMetricsTest.java#L97) · `@Test` · `normalize_metric_edge_values` — 0건 알림은 무시하고 null·blank·긴 tag 및 음수 duration을 안전한 값으로 정규화한다
2. [L126](../../../src/test/java/com/umc/product/global/logging/OperationalMetricsTest.java#L126) · `@Test` · `client_request_enum_names` — client request enum은 enum 이름으로 기록한다

#### ObservabilityErrorSanitizerTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/observability/ObservabilityErrorSanitizerTest.java`

1. [L57](../../../src/test/java/com/umc/product/global/observability/ObservabilityErrorSanitizerTest.java#L57) · `@Test` · `suppressed_sensitive_error_copy` — suppressed 예외의 민감 메시지까지 원본을 변경하지 않고 복제·정제한다
2. [L71](../../../src/test/java/com/umc/product/global/observability/ObservabilityErrorSanitizerTest.java#L71) · `@Test` · `cyclic_cause_graph` — 순환 cause graph도 무한 재귀 없이 한 번씩 복제한다
3. [L84](../../../src/test/java/com/umc/product/global/observability/ObservabilityErrorSanitizerTest.java#L84) · `@Test` · `suppressed_sql_metadata` — suppressed SQLException과 constraint에서 DB 진단 metadata를 추출한다
4. [L102](../../../src/test/java/com/umc/product/global/observability/ObservabilityErrorSanitizerTest.java#L102) · `@Test` · `null_and_plain_error_metadata` — SQL 예외가 없는 null·일반 예외는 DB tag 없이 안전하게 기록한다

#### ObservabilityInfrastructureResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/observability/ObservabilityInfrastructureResidualTest.java`

1. [L19](../../../src/test/java/com/umc/product/global/observability/ObservabilityInfrastructureResidualTest.java#L19) · `@Test` · `traceparent_capture_and_restore_edges` — traceparent는 sampled 여부를 보존하고 빈 값·잘못된 형식을 거부한다
2. [L38](../../../src/test/java/com/umc/product/global/observability/ObservabilityInfrastructureResidualTest.java#L38) · `@Test` · `span_accessor_without_tracer` — Tracer bean이 없으면 span context accessor 등록을 건너뛴다
3. [L53](../../../src/test/java/com/umc/product/global/observability/ObservabilityInfrastructureResidualTest.java#L53) · `@Test` · `scheduled_task_tracer_without_tracer_bean` — ScheduledTaskTracer의 provider 생성자는 Tracer가 없어도 NOOP tracer를 사용한다

#### TraceFlowAspectTest (5개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java`

1. [L111](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L111) · `@Test` · `usecase_span_disabled` — 비활성 usecase span은 span을 만들지 않고 원 호출만 진행한다
2. [L125](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L125) · `@Test` · `traced_invocation_failure` — trace 대상 호출 실패는 정제된 error를 기록하고 원 예외를 다시 던진다
3. [L143](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L143) · `@Test` · `null_target_uses_declaring_type` — target이 없는 join point는 signature 선언 type으로 metadata를 계산한다
4. [L154](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L154) · `@Test` · `constructor_without_tracer_bean` — provider 생성자는 Tracer bean이 없어도 NOOP tracer를 사용한다
5. [L163](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L163) · `@Test` · `metadata_naming_edges` — metadata naming은 service suffix와 adapter package·domain 경계를 안정적으로 해석한다

#### ApiRateLimitMetricsTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/ratelimit/ApiRateLimitMetricsTest.java`

1. [L30](../../../src/test/java/com/umc/product/global/ratelimit/ApiRateLimitMetricsTest.java#L30) · `@Test` · `normalize_edge_tags` — null·blank·긴 tag와 식별자 URI를 low-cardinality 값으로 축약한다

#### RateLimitResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/ratelimit/RateLimitResidualTest.java`

1. [L22](../../../src/test/java/com/umc/product/global/ratelimit/RateLimitResidualTest.java#L22) · `@Test` · `properties_null_defaults` — null 설정은 안전한 기본값으로 정규화한다
2. [L43](../../../src/test/java/com/umc/product/global/ratelimit/RateLimitResidualTest.java#L43) · `@Test` · `properties_nested_value_normalization` — 빈 path, 낮은 limit, 불완전 route/cache 설정을 최소 안전값으로 보정한다
3. [L78](../../../src/test/java/com/umc/product/global/ratelimit/RateLimitResidualTest.java#L78) · `@Test` · `route_policy_anonymous_fallback_and_blank_method` — method 제한이 없는 custom 정책은 익명 기본 limit fallback을 사용한다
4. [L115](../../../src/test/java/com/umc/product/global/ratelimit/RateLimitResidualTest.java#L115) · `@Test` · `hash_key_algorithm_unavailable` — SHA-256을 제공하지 않는 비정상 JVM에서도 rate-limit 로그 key를 안전하게 대체한다

#### ResponseResidualTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/response/ResponseResidualTest.java`

1. [L30](../../../src/test/java/com/umc/product/global/response/ResponseResidualTest.java#L30) · `@Test` · `PageResponse를_변환한다` — PageResponse는 원본 page metadata와 mapper 적용 결과를 보존한다
2. [L46](../../../src/test/java/com/umc/product/global/response/ResponseResidualTest.java#L46) · `@Test` · `CursorResponse를_계산한다` — CursorResponse는 초과 row를 제거하고 순서·next cursor·마지막 page를 계산한다
3. [L64](../../../src/test/java/com/umc/product/global/response/ResponseResidualTest.java#L64) · `@Test` · `PageRequest_offset을_계산한다` — 공통 PageRequest는 1-based page를 안전한 long offset으로 변환한다
4. [L70](../../../src/test/java/com/umc/product/global/response/ResponseResidualTest.java#L70) · `@Test` · `ApiResponse와_error_factory를_검증한다` — ApiResponse와 error factory는 기본·사용자 메시지와 상세 정보를 보존한다
5. [L89](../../../src/test/java/com/umc/product/global/response/ResponseResidualTest.java#L89) · `@Test` · `ApiErrorResponseWriter_overload를_검증한다` — error writer의 세 overload는 status·UTF-8 JSON envelope를 동일하게 기록한다
6. [L108](../../../src/test/java/com/umc/product/global/response/ResponseResidualTest.java#L108) · `@Test` · `GlobalResponseWrapper_제외_타입을_검증한다` — 전역 wrapper는 이미 제어되는 응답 타입과 Spring 내부 controller를 제외한다
7. [L124](../../../src/test/java/com/umc/product/global/response/ResponseResidualTest.java#L124) · `@Test` · `GlobalResponseWrapper_body를_변환한다` — body wrapper는 기존 envelope와 String을 보존하고 일반 DTO/null만 성공 envelope로 감싼다

#### GlobalSecurityValueResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/security/GlobalSecurityValueResidualTest.java`

1. [L27](../../../src/test/java/com/umc/product/global/security/GlobalSecurityValueResidualTest.java#L27) · `@Test` · `현재_회원_부재를_처리한다` — 인증 정보 부재·미인증·다른 principal은 nullable 회원과 ID를 null로 반환한다
2. [L46](../../../src/test/java/com/umc/product/global/security/GlobalSecurityValueResidualTest.java#L46) · `@Test` · `현재_회원_ID와_문자열을_반환한다` — 인증 회원의 nullable ID와 principal 문자열은 모든 client context를 보존한다
3. [L57](../../../src/test/java/com/umc/product/global/security/GlobalSecurityValueResidualTest.java#L57) · `@Test` · `RefreshTokenClaims_context를_정규화한다` — RefreshTokenClaims 보조 생성자와 null context는 빈 context로 정규화한다

#### JwtTokenProviderResidualTest (14개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java`

1. [L47](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L47) · `@Test` · `SSO_secret_격리를_검증한다` — SSO 전용 secret은 refresh·OAuth·email verification secret과도 같을 수 없다
2. [L58](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L58) · `@Test` · `OAuth_verification_token을_왕복한다` — OAuth verification token은 email·provider·providerId를 손실 없이 왕복한다
3. [L69](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L69) · `@Test` · `Access_token_overload를_왕복한다` — Access token 생성 overload는 역할·회원·client type과 만료 설정을 보존한다
4. [L87](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L87) · `@Test` · `blank_client_context를_정규화한다` — Access token client context는 blank client ID를 audience 없이 UNKNOWN 기본값과 함께 보존한다
5. [L102](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L102) · `@Test` · `알_수_없는_client_claim을_fail_soft한다` — 알 수 없는 client enum과 roles 타입은 null·UNKNOWN·빈 목록으로 fail-soft 처리한다
6. [L121](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L121) · `@Test` · `legacy_client_context의_누락_enum을_복원한다` — client ID만 있는 legacy token은 service와 environment를 UNKNOWN으로 복원한다
7. [L134](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L134) · `@Test` · `Refresh_token_null_context를_정규화한다` — Refresh token context는 null 입력을 빈 context로 정규화한다
8. [L142](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L142) · `@Test` · `잘못된_Refresh_token_claim을_거부한다` — Refresh token은 jti 누락·잘못된 UUID·만료 누락을 모두 거부한다
9. [L156](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L156) · `@Test` · `잘못된_OAuth_provider를_거부한다` — OAuth provider claim이 알 수 없는 값이면 검증 token을 거부한다
10. [L169](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L169) · `@Test` · `Access_token_오류를_정규화한다` — Access token의 잘못된 서명·만료·형식은 안정적인 인증 error code로 변환한다
11. [L190](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L190) · `@Test` · `verification_token_공통_검증_오류를_정규화한다` — verification token 공통 검증은 잘못된 서명·만료·미지원·빈 형식을 정규화한다
12. [L207](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L207) · `@Test` · `숫자가_아닌_subject를_거부한다` — Access token subject가 숫자가 아니면 parseAndValidate는 INVALID_JWT로 변환한다
13. [L217](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L217) · `@Test` · `email_purpose_누락을_거부한다` — email verification token의 purpose 누락은 cross-purpose와 동일하게 거부한다
14. [L229](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderResidualTest.java#L229) · `@Test` · `잘못된_SSO_login_claim을_거부한다` — SSO login token은 type·인증 방식·회원 ID·시각 누락과 형식 오류를 모두 거부한다

#### CurrentMemberArgumentResolverTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/security/resolver/CurrentMemberArgumentResolverTest.java`

1. [L20](../../../src/test/java/com/umc/product/global/security/resolver/CurrentMemberArgumentResolverTest.java#L20) · `@Test` · `지원_parameter를_판단한다` — @CurrentMember MemberPrincipal 조합만 지원한다
2. [L30](../../../src/test/java/com/umc/product/global/security/resolver/CurrentMemberArgumentResolverTest.java#L30) · `@Test` · `현재_회원을_해석한다` — 현재 회원 해석은 nullable provider 결과를 그대로 반환한다

#### SecurityFilterResidualTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/security/SecurityFilterResidualTest.java`

1. [L30](../../../src/test/java/com/umc/product/global/security/SecurityFilterResidualTest.java#L30) · `@Test` · `jwt_domain_exception` — JWT domain 예외를 request에 보존하고 filter chain은 계속 진행한다
2. [L52](../../../src/test/java/com/umc/product/global/security/SecurityFilterResidualTest.java#L52) · `@Test` · `jwt_unknown_exception` — 예상하지 못한 JWT 예외를 별도 attribute에 보존한다
3. [L71](../../../src/test/java/com/umc/product/global/security/SecurityFilterResidualTest.java#L71) · `@Test` · `jwt_roles_to_authorities` — 유효한 JWT의 role을 authority로 변환한다
4. [L90](../../../src/test/java/com/umc/product/global/security/SecurityFilterResidualTest.java#L90) · `@Test` · `entry_point_filter_domain_error` — AuthenticationEntryPoint는 filter domain 예외를 우선 응답한다
5. [L105](../../../src/test/java/com/umc/product/global/security/SecurityFilterResidualTest.java#L105) · `@Test` · `entry_point_unknown_error` — AuthenticationEntryPoint는 알 수 없는 JWT 예외를 내부 오류로 응답한다
6. [L122](../../../src/test/java/com/umc/product/global/security/SecurityFilterResidualTest.java#L122) · `@Test` · `entry_point_cause_domain_error` — AuthenticationEntryPoint는 cause의 domain 예외를 fallback으로 사용한다
7. [L138](../../../src/test/java/com/umc/product/global/security/SecurityFilterResidualTest.java#L138) · `@Test` · `entry_point_default_error` — 인증 오류 정보가 없으면 SECURITY_NOT_GIVEN을 응답한다

#### PublicEndpointCollectorTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/security/util/PublicEndpointCollectorTest.java`

1. [L22](../../../src/test/java/com/umc/product/global/security/util/PublicEndpointCollectorTest.java#L22) · `@Test` · `public_endpoint_without_method` — HTTP method가 없는 Public endpoint는 모든 method matcher로 수집한다
2. [L35](../../../src/test/java/com/umc/product/global/security/util/PublicEndpointCollectorTest.java#L35) · `@Test` · `method_public_and_private_endpoint` — Public이 아닌 endpoint는 제외하고 method가 있으면 함께 수집한다

#### GeometryUtilsTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/global/util/GeometryUtilsTest.java`

1. [L11](../../../src/test/java/com/umc/product/global/util/GeometryUtilsTest.java#L11) · `@Test` · `좌표를_Point로_변환한다` — 위도·경도를 SRID 4326의 longitude/latitude Point로 변환한다
2. [L21](../../../src/test/java/com/umc/product/global/util/GeometryUtilsTest.java#L21) · `@Test` · `null_좌표는_생성하지_않는다` — 위도 또는 경도가 null이면 부분 좌표를 만들지 않는다

#### ApiResponseStompErrorHandlerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/websocket/handler/ApiResponseStompErrorHandlerTest.java`

1. [L76](../../../src/test/java/com/umc/product/global/websocket/handler/ApiResponseStompErrorHandlerTest.java#L76) · `@Test` · `serialization_failure_uses_safe_fallback` — 응답 직렬화 실패 시 고정된 내부 오류 payload로 대체한다
2. [L91](../../../src/test/java/com/umc/product/global/websocket/handler/ApiResponseStompErrorHandlerTest.java#L91) · `@Test` · `self_referencing_cause` — cause가 자기 자신인 비정상 예외도 순환하지 않고 내부 오류로 변환한다

#### ShutdownAwareHandshakeInterceptorTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/websocket/interceptor/ShutdownAwareHandshakeInterceptorTest.java`

1. [L66](../../../src/test/java/com/umc/product/global/websocket/interceptor/ShutdownAwareHandshakeInterceptorTest.java#L66) · `@Test` · `lifecycle_noop_callbacks` — start와 afterHandshake callback은 상태를 변경하지 않는다

#### StompAuthChannelInterceptorCommandCorrelationTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/websocket/interceptor/StompAuthChannelInterceptorCommandCorrelationTest.java`

1. [L151](../../../src/test/java/com/umc/product/global/websocket/interceptor/StompAuthChannelInterceptorCommandCorrelationTest.java#L151) · `@Test` · `communitySendWithoutPrincipalRejected` — 인증 principal이 없는 Community SEND는 보안 오류로 즉시 거절한다

#### StompAuthChannelInterceptorTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/websocket/interceptor/StompAuthChannelInterceptorTest.java`

1. [L224](../../../src/test/java/com/umc/product/global/websocket/interceptor/StompAuthChannelInterceptorTest.java#L224) · `@Test` · `message_without_stomp_headers_passes` — STOMP header가 없는 일반 message는 그대로 통과한다

#### WebSocketRateLimitInterceptorTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/websocket/interceptor/WebSocketRateLimitInterceptorTest.java`

1. [L146](../../../src/test/java/com/umc/product/global/websocket/interceptor/WebSocketRateLimitInterceptorTest.java#L146) · `@Test` · `defaultConstructorHandlesRateLimitWithoutPublisher` — 기본 생성자는 no-op observer로 제한 초과를 안전하게 처리한다
2. [L158](../../../src/test/java/com/umc/product/global/websocket/interceptor/WebSocketRateLimitInterceptorTest.java#L158) · `@Test` · `clockProviderCompatibilityConstructor` — Clock provider 호환 생성자는 제공된 clock과 빈 observer 목록을 사용한다

#### WebSocketBrokerRelayStartupValidatorTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/websocket/relay/WebSocketBrokerRelayStartupValidatorTest.java`

1. [L44](../../../src/test/java/com/umc/product/global/websocket/relay/WebSocketBrokerRelayStartupValidatorTest.java#L44) · `@Test` · `productionStartsWhenRelayIsAvailable` — prod에서 relay가 준비되면 정상적으로 시작한다
2. [L99](../../../src/test/java/com/umc/product/global/websocket/relay/WebSocketBrokerRelayStartupValidatorTest.java#L99) · `@Test` · `interruptedReadinessRestoresInterruptFlag` — relay readiness 대기가 interrupt되면 interrupt 상태와 원인을 보존해 시작을 거부한다

### Outbound Adapter

#### CaffeineCacheStoreAdapterTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/global/cache/adapter/out/CaffeineCacheStoreAdapterTest.java`

1. [L59](../../../src/test/java/com/umc/product/global/cache/adapter/out/CaffeineCacheStoreAdapterTest.java#L59) · `@Test` · `cache_evict_생성되지_않은_namespace` — 생성되지 않은 namespace를 제거해도 예외가 발생하지 않는다
2. [L69](../../../src/test/java/com/umc/product/global/cache/adapter/out/CaffeineCacheStoreAdapterTest.java#L69) · `@Test` · `native_cache_재사용과_metric_등록` — 동일 namespace는 하나의 native cache를 재사용하고 metric을 한 번 등록한다
3. [L89](../../../src/test/java/com/umc/product/global/cache/adapter/out/CaffeineCacheStoreAdapterTest.java#L89) · `@Test` · `cache_value_type_불일치` — 저장된 값의 타입이 spec과 다르면 조회를 거부한다

## 3.4 `audit` — 15개

### Application Service

#### AuditLogServiceResidualTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/audit/application/service/AuditLogServiceResidualTest.java`

1. [L44](../../../src/test/java/com/umc/product/audit/application/service/AuditLogServiceResidualTest.java#L44) · `@Test` · `상세_정보를_직렬화해_저장한다` — 상세 정보가 있으면 JSON으로 직렬화해 로그를 저장한다
2. [L59](../../../src/test/java/com/umc/product/audit/application/service/AuditLogServiceResidualTest.java#L59) · `@Test` · `빈_상세_정보는_직렬화하지_않는다` — 상세 정보가 null이거나 비어 있으면 직렬화 없이 저장한다
3. [L73](../../../src/test/java/com/umc/product/audit/application/service/AuditLogServiceResidualTest.java#L73) · `@Test` · `직렬화_실패는_null_상세로_격리한다` — 상세 정보 직렬화가 실패해도 본 업무와 로그 저장을 중단하지 않는다
4. [L88](../../../src/test/java/com/umc/product/audit/application/service/AuditLogServiceResidualTest.java#L88) · `@Test` · `조회_조건을_전달하고_DTO로_변환한다` — 조회 조건과 페이지를 port에 그대로 전달하고 로그를 DTO로 변환한다
5. [L105](../../../src/test/java/com/umc/product/audit/application/service/AuditLogServiceResidualTest.java#L105) · `@Test` · `감사_로그_권한을_검증한다` — 감사 로그 조회 권한은 중앙 운영진에게만 허용하고 미지원 권한은 fail-closed 처리한다

### Contract / Misc

#### AuditInputAdapterResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/audit/adapter/in/AuditInputAdapterResidualTest.java`

1. [L43](../../../src/test/java/com/umc/product/audit/adapter/in/AuditInputAdapterResidualTest.java#L43) · `@Test` · `감사_문맥을_이벤트로_발행한다` — 감사 annotation의 인자·반환값과 인증 회원·프록시 IP를 이벤트로 발행한다
2. [L71](../../../src/test/java/com/umc/product/audit/adapter/in/AuditInputAdapterResidualTest.java#L71) · `@Test` · `선택_문맥_부재를_안전하게_처리한다` — 빈 표현식·비회원 인증·프록시 헤더 부재는 null과 원격 IP로 안전하게 처리한다
3. [L94](../../../src/test/java/com/umc/product/audit/adapter/in/AuditInputAdapterResidualTest.java#L94) · `@Test` · `HTTP_요청_문맥이_없으면_IP를_생략한다` — HTTP 요청 밖에서 실행되는 비동기 업무는 IP 없이 감사 이벤트를 발행한다
4. [L108](../../../src/test/java/com/umc/product/audit/adapter/in/AuditInputAdapterResidualTest.java#L108) · `@Test` · `문맥_조회_실패를_격리한다` — SecurityContext와 RequestContext 접근 실패도 감사 이벤트 발행과 본 업무를 방해하지 않는다
5. [L130](../../../src/test/java/com/umc/product/audit/adapter/in/AuditInputAdapterResidualTest.java#L130) · `@Test` · `잘못된_SpEL을_격리한다` — 잘못된 SpEL은 예외를 외부로 전파하거나 불완전한 이벤트를 발행하지 않는다
6. [L142](../../../src/test/java/com/umc/product/audit/adapter/in/AuditInputAdapterResidualTest.java#L142) · `@Test` · `이벤트_저장과_실패를_격리한다` — 이벤트 listener는 저장을 위임하고 저장 실패를 발행자에게 전파하지 않는다

### Domain

#### AuditLogTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/audit/domain/AuditLogTest.java`

1. [L18](../../../src/test/java/com/umc/product/audit/domain/AuditLogTest.java#L18) · `@Test` · `이벤트를_로그와_조회_DTO로_변환한다` — 이벤트의 모든 감사 속성을 불변 로그와 조회 DTO로 변환한다

### Persistence

#### AuditPersistenceResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/audit/adapter/out/persistence/AuditPersistenceResidualTest.java`

1. [L27](../../../src/test/java/com/umc/product/audit/adapter/out/persistence/AuditPersistenceResidualTest.java#L27) · `@Test` · `저장과_검색을_위임한다` — 저장과 검색을 각 repository에 위임한다
2. [L44](../../../src/test/java/com/umc/product/audit/adapter/out/persistence/AuditPersistenceResidualTest.java#L44) · `@Test` · `모든_검색_조건을_반영한다` — 모든 검색 조건과 페이지 경계를 QueryDSL에 반영한다
3. [L67](../../../src/test/java/com/umc/product/audit/adapter/out/persistence/AuditPersistenceResidualTest.java#L67) · `@Test` · `빈_검색_조건과_null_count를_처리한다` — 검색 조건과 count가 없으면 전체 조건과 0건 결과를 반환한다

## 3.5 `documentation` — 1개

### Application Service

#### ErrorCodeCatalogQueryServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/documentation/application/service/ErrorCodeCatalogQueryServiceTest.java`

1. [L91](../../../src/test/java/com/umc/product/documentation/application/service/ErrorCodeCatalogQueryServiceTest.java#L91) · `@Test` · `manifest_역직렬화_실패를_변환한다` — manifest 역직렬화 실패는 문서 도메인 예외로 변환한다

## 3.6 `maintenance` — 14개

### Application Service

#### MaintenanceQueryServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/maintenance/application/service/MaintenanceQueryServiceTest.java`

1. [L100](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceQueryServiceTest.java#L100) · `@Test` · `존재하는_윈도우는_도메인_집합과_함께_반환한다` — getById
2. [L141](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceQueryServiceTest.java#L141) · `@Test` · `목록의_윈도우를_응답으로_변환한다` — listAll

#### MaintenanceStateHolderTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/maintenance/application/service/MaintenanceStateHolderTest.java`

1. [L24](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceStateHolderTest.java#L24) · `@Test` · `조회_실패_시_마지막_스냅샷을_유지한다` — 조회 실패 시 마지막 정상 스냅샷을 유지한다

### Contract / Misc

#### MaintenanceDomainExceptionTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/maintenance/exception/MaintenanceDomainExceptionTest.java`

1. [L11](../../../src/test/java/com/umc/product/maintenance/exception/MaintenanceDomainExceptionTest.java#L11) · `@Test` · `상세_메시지를_보존한다` — 운영자가 이해할 수 있는 상세 메시지를 보존한다

### Domain

#### MaintenanceSnapshotTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/maintenance/domain/MaintenanceSnapshotTest.java`

1. [L15](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceSnapshotTest.java#L15) · `@Test` · `비활성_스냅샷은_차단하지_않는다` — 비활성 스냅샷은 어떤 요청도 차단하지 않는다
2. [L21](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceSnapshotTest.java#L21) · `@Test` · `전체_점검은_모든_요청을_차단한다` — 전체 점검은 모든 요청을 차단한다
3. [L38](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceSnapshotTest.java#L38) · `@Test` · `부분_점검은_선택한_도메인만_차단한다` — 부분 점검은 선택한 도메인만 차단하고 알 수 없는 URI는 허용한다
4. [L57](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceSnapshotTest.java#L57) · `@Test` · `윈도우를_스냅샷으로_복사한다` — 도메인 집합이 있는 윈도우를 불변 스냅샷으로 복사한다

### Outbound Adapter

#### ChallengerRoleBasedBypassPolicyTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/maintenance/adapter/out/bypass/ChallengerRoleBasedBypassPolicyTest.java`

1. [L16](../../../src/test/java/com/umc/product/maintenance/adapter/out/bypass/ChallengerRoleBasedBypassPolicyTest.java#L16) · `@Test` · `null_회원은_우회하지_않는다` — null 회원은 권한 조회 없이 우회하지 않는다
2. [L26](../../../src/test/java/com/umc/product/maintenance/adapter/out/bypass/ChallengerRoleBasedBypassPolicyTest.java#L26) · `@Test` · `SUPER_ADMIN만_우회한다` — SUPER_ADMIN 판정 결과를 그대로 반환한다

### REST / Web

#### AdminMaintenanceControllerUnitTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/maintenance/adapter/in/web/AdminMaintenanceControllerUnitTest.java`

1. [L25](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/AdminMaintenanceControllerUnitTest.java#L25) · `@Test` · `SUPER_ADMIN은_단건_조회한다` — SUPER_ADMIN은 점검 윈도우를 ID로 조회한다
2. [L42](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/AdminMaintenanceControllerUnitTest.java#L42) · `@Test` · `권한_없는_단건_조회를_거부한다` — 비인증 및 일반 회원은 단건 조회를 할 수 없다

#### MaintenanceFilterUnitTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterUnitTest.java`

1. [L43](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterUnitTest.java#L43) · `@Test` · `비활성_점검은_요청을_통과시킨다` — 비활성 점검은 일반 API 요청을 그대로 통과시킨다
2. [L58](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterUnitTest.java#L58) · `@Test` · `비회원_principal은_점검을_우회하지_않는다` — MemberPrincipal이 아닌 인증 principal은 점검 우회에 사용하지 않는다

## 3.7 `storage` — 33개

### Application Service

#### FileCommandServiceUnitTest (7개, 보강 파일)

- 위치: `src/test/java/com/umc/product/storage/application/service/FileCommandServiceUnitTest.java`

1. [L272](../../../src/test/java/com/umc/product/storage/application/service/FileCommandServiceUnitTest.java#L272) · `@Test` · `서버_생성_파일을_저장한다` — 서버 생성 파일은 객체 저장 후 업로드 완료 metadata를 저장한다
2. [L289](../../../src/test/java/com/umc/product/storage/application/service/FileCommandServiceUnitTest.java#L289) · `@Test` · `서버_생성_파일의_확장자와_크기를_검증한다` — 서버 생성 파일은 확장자와 실제 byte 크기 제한을 외부 저장 전에 검증한다
3. [L309](../../../src/test/java/com/umc/product/storage/application/service/FileCommandServiceUnitTest.java#L309) · `@Test` · `ETC는_확장자_없이_업로드_URL을_생성한다` — 확장자가 선택인 ETC 파일은 점 없는 이름으로도 업로드 URL을 생성한다
4. [L325](../../../src/test/java/com/umc/product/storage/application/service/FileCommandServiceUnitTest.java#L325) · `@Test` · `업로드_URL_요청의_파일_정책을_검증한다` — 필수 확장자 누락·미허용 확장자·크기 초과는 metadata 저장 전에 거부한다
5. [L340](../../../src/test/java/com/umc/product/storage/application/service/FileCommandServiceUnitTest.java#L340) · `@Test` · `업로드_확인의_미존재와_멱등성_경계를_검증한다` — 업로드 확인은 metadata 미존재와 이미 완료된 중복 요청을 거부한다
6. [L351](../../../src/test/java/com/umc/product/storage/application/service/FileCommandServiceUnitTest.java#L351) · `@Test` · `잘못된_객체_정리_실패에도_최초_예외를_보존한다` — 검증 실패 객체 정리까지 실패해도 최초 검증 예외를 보존한다
7. [L365](../../../src/test/java/com/umc/product/storage/application/service/FileCommandServiceUnitTest.java#L365) · `@Test` · `삭제의_미존재와_소유자_단축을_검증한다` — 파일 삭제는 미존재를 거부하고 소유자는 관리자 조회 없이 삭제한다

#### FileQueryServiceUnitTest (5개, 보강 파일)

- 위치: `src/test/java/com/umc/product/storage/application/service/FileQueryServiceUnitTest.java`

1. [L105](../../../src/test/java/com/umc/product/storage/application/service/FileQueryServiceUnitTest.java#L105) · `@Test` · `단건_조회를_검증한다` — 단건 조회는 signed URL을 결합하고 미존재는 예외로 변환한다
2. [L119](../../../src/test/java/com/umc/product/storage/application/service/FileQueryServiceUnitTest.java#L119) · `@Test` · `batch_링크와_상세를_변환한다` — batch 링크와 상세 조회는 DB 반환 데이터만 signed URL과 함께 map으로 변환한다
3. [L133](../../../src/test/java/com/umc/product/storage/application/service/FileQueryServiceUnitTest.java#L133) · `@Test` · `빈_batch와_중복_ID를_처리한다` — 빈 batch는 DB를 조회하지 않고 중복 ID는 최초 순서로 한 번씩 반환한다
4. [L146](../../../src/test/java/com/umc/product/storage/application/service/FileQueryServiceUnitTest.java#L146) · `@Test` · `batch_입력을_fail_closed한다` — batch의 null·blank ID와 null 회원은 fail-closed한다
5. [L158](../../../src/test/java/com/umc/product/storage/application/service/FileQueryServiceUnitTest.java#L158) · `@Test` · `존재_확인_계약을_검증한다` — 존재 확인과 throw 계약은 port 결과를 정확히 반영한다

### Contract / Misc

#### StorageResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/storage/StorageResidualTest.java`

1. [L27](../../../src/test/java/com/umc/product/storage/StorageResidualTest.java#L27) · `@Test` · `생성_파일_command의_팩토리와_크기를_검증한다` — 생성 파일 command는 유효 기본값과 정확한 byte 크기를 제공한다
2. [L42](../../../src/test/java/com/umc/product/storage/StorageResidualTest.java#L42) · `@Test` · `생성_파일_command의_필수값을_검증한다` — 생성 파일 command는 null 필드와 빈 content를 즉시 거부한다
3. [L60](../../../src/test/java/com/umc/product/storage/StorageResidualTest.java#L60) · `@Test` · `업로드_준비_command의_경계를_검증한다` — 업로드 준비 command는 보조 생성자와 null·blank·0 경계를 검증한다
4. [L81](../../../src/test/java/com/umc/product/storage/StorageResidualTest.java#L81) · `@Test` · `삭제_command의_필수값을_검증한다` — 삭제 command는 null과 blank 식별자를 거부한다
5. [L89](../../../src/test/java/com/umc/product/storage/StorageResidualTest.java#L89) · `@Test` · `업로드_정보_만료를_판단한다` — 업로드 정보는 만료 전후를 현재 시각 기준으로 판단한다
6. [L99](../../../src/test/java/com/umc/product/storage/StorageResidualTest.java#L99) · `@Test` · `storage_port_기본_계약을_검증한다` — 기본 storage port는 크기 없는 URL 생성에 위임하고 빈 metadata와 안정적인 key를 제공한다

### Domain

#### FileMetadataTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/storage/domain/FileMetadataTest.java`

1. [L133](../../../src/test/java/com/umc/product/storage/domain/FileMetadataTest.java#L133) · `@Test` · `요청과_실제_Content_Type이_모두_null이면_동일하게_처리한다` — 요청과_실제_Content_Type이_모두_null이면_동일하게_처리한다
2. [L142](../../../src/test/java/com/umc/product/storage/domain/FileMetadataTest.java#L142) · `@Test` · `ETC는_임의_확장자를_허용한다` — ETC는_임의_확장자를_허용한다

### Outbound Adapter

#### S3ConfigTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/storage/adapter/out/s3/S3ConfigTest.java`

1. [L53](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3ConfigTest.java#L53) · `@Test` · `S3_정적_자격증명이_불완전하면_기본_체인을_사용한다` — S3 정적 자격증명이 하나라도 비면 기본 자격증명 체인을 사용한다
2. [L64](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3ConfigTest.java#L64) · `@Test` · `CloudFront_설정_경계를_검증한다` — 활성 CloudFront는 배포 도메인이 필수이고 서명 키 누락만으로는 시작을 막지 않는다

#### S3StorageAdapterTest (7개, 보강 파일)

- 위치: `src/test/java/com/umc/product/storage/adapter/out/s3/S3StorageAdapterTest.java`

1. [L293](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3StorageAdapterTest.java#L293) · `@Test` · `파일_크기_없는_업로드_URL을_생성한다` — 파일 크기 없는 legacy 업로드 URL도 PUT 서명으로 생성한다
2. [L306](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3StorageAdapterTest.java#L306) · `@Test` · `업로드_실패를_변환한다` — 업로드 URL 생성과 객체 저장 실패는 원인을 보존한 storage 예외로 변환한다
3. [L327](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3StorageAdapterTest.java#L327) · `@Test` · `S3_404를_미존재로_정규화한다` — HeadObject의 S3 404도 미존재로 정규화한다
4. [L337](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3StorageAdapterTest.java#L337) · `@Test` · `객체_삭제의_성공과_실패를_검증한다` — 객체 삭제 요청은 bucket과 key를 전달하고 외부 실패를 변환한다
5. [L356](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3StorageAdapterTest.java#L356) · `@Test` · `CloudFront_private_key_형식을_파싱한다` — 헤더 없는 PKCS8와 PKCS1 PEM private key를 모두 파싱한다
6. [L369](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3StorageAdapterTest.java#L369) · `@Test` · `CloudFront_설정과_key_파싱_실패를_정규화한다` — 잘못된 CDN 도메인·private key와 미지원 PEM 객체는 서명 실패로 정규화한다
7. [L384](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3StorageAdapterTest.java#L384) · `@Test` · `SSM_private_key_실패를_변환한다` — SSM private key가 비거나 조회가 실패하면 서명 실패로 변환한다

### Persistence

#### FileMetadataPersistenceResidualTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/storage/adapter/out/persistence/FileMetadataPersistenceResidualTest.java`

1. [L19](../../../src/test/java/com/umc/product/storage/adapter/out/persistence/FileMetadataPersistenceResidualTest.java#L19) · `@Test` · `모든_계약을_위임한다` — 단건·batch·존재·저장·삭제를 repository에 위임한다

### REST / Web

#### StorageControllerResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/storage/adapter/in/web/StorageControllerResidualTest.java`

1. [L31](../../../src/test/java/com/umc/product/storage/adapter/in/web/StorageControllerResidualTest.java#L31) · `@Test` · `업로드_준비_요청을_변환한다` — 업로드 준비 요청에 인증 회원을 결합하고 usecase 응답을 반환한다
2. [L49](../../../src/test/java/com/umc/product/storage/adapter/in/web/StorageControllerResidualTest.java#L49) · `@Test` · `업로드_확인과_삭제를_위임한다` — 업로드 확인과 삭제는 file ID와 인증 회원을 usecase에 전달한다
3. [L62](../../../src/test/java/com/umc/product/storage/adapter/in/web/StorageControllerResidualTest.java#L62) · `@Test` · `파일_조회_DTO를_변환한다` — 파일 조회 DTO는 API의 legacy fileUrl 필드까지 보존한다

## 3.8 `llm` — 17개

### Application Service

#### LlmServiceResidualTest (9개, 신규 파일)

- 위치: `src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java`

1. [L35](../../../src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java#L35) · `@Test` · `기본_생성자와_provider_gauge를_검증한다` — 기본 생성자는 production clock을 사용하고 활성 fallback provider gauge를 등록한다
2. [L54](../../../src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java#L54) · `@Test` · `provider_미구성을_검증한다` — provider가 구성되지 않은 비정상 wiring은 명시적인 도메인 예외로 실패한다
3. [L72](../../../src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java#L72) · `@Test` · `RuntimeException을_도메인_예외로_정규화한다` — adapter의 예상하지 못한 RuntimeException은 실패 metric·guard와 도메인 예외로 정규화한다
4. [L99](../../../src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java#L99) · `@Test` · `provider_gauge의_null을_정규화한다` — provider gauge는 null provider를 unknown으로 정규화하고 fallback 라벨을 보존한다
5. [L115](../../../src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java#L115) · `@Test` · `call_guard_기본_생성자를_검증한다` — 기본 call guard 생성자는 실패 임계 전 호출을 허용한다
6. [L123](../../../src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java#L123) · `@Test` · `rate_limiter_기본_생성자를_검증한다` — 기본 rate limiter 생성자는 비활성 설정에서 즉시 통과한다
7. [L131](../../../src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java#L131) · `@Test` · `rate_limiter_interrupt를_처리한다` — 토큰 고갈 대기 중 interrupt는 상태를 복원하고 즉시 실패한다
8. [L146](../../../src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java#L146) · `@Test` · `rate_limiter_최소_대기_후_리필한다` — 토큰 고갈 시 계산된 최소 대기 후 리필된 토큰을 소비한다
9. [L158](../../../src/test/java/com/umc/product/llm/application/service/LlmServiceResidualTest.java#L158) · `@Test` · `결과와_예외_생성자를_검증한다` — LLM 결과 단순 factory와 모든 예외 생성자를 보존한다

### External Adapter

#### LlmExternalAdapterResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/llm/adapter/out/external/LlmExternalAdapterResidualTest.java`

1. [L27](../../../src/test/java/com/umc/product/llm/adapter/out/external/LlmExternalAdapterResidualTest.java#L27) · `@Test` · `mock_adapter_echo_경계를_검증한다` — mock adapter는 null·짧은 prompt를 그대로 echo하고 긴 prompt만 안전하게 자른다
2. [L40](../../../src/test/java/com/umc/product/llm/adapter/out/external/LlmExternalAdapterResidualTest.java#L40) · `@Test` · `fallback_adapter를_제공한다` — fallback config는 활성 adapter가 없을 때 mock port를 제공한다
3. [L48](../../../src/test/java/com/umc/product/llm/adapter/out/external/LlmExternalAdapterResidualTest.java#L48) · `@Test` · `OpenAI_adapter_성공과_실패를_검증한다` — OpenAI adapter는 응답을 정규화하고 외부 실패를 도메인 예외로 변환한다
4. [L68](../../../src/test/java/com/umc/product/llm/adapter/out/external/LlmExternalAdapterResidualTest.java#L68) · `@Test` · `Vertex_Gemini_adapter_성공과_실패를_검증한다` — Vertex Gemini adapter는 응답을 정규화하고 외부 실패를 도메인 예외로 변환한다
5. [L88](../../../src/test/java/com/umc/product/llm/adapter/out/external/LlmExternalAdapterResidualTest.java#L88) · `@Test` · `Google_GenAI_adapter_null_응답과_실패를_검증한다` — Google GenAI adapter는 null 응답을 빈 문자열로 처리하고 외부 실패를 도메인 예외로 변환한다
6. [L108](../../../src/test/java/com/umc/product/llm/adapter/out/external/LlmExternalAdapterResidualTest.java#L108) · `@Test` · `부분_응답_누락을_빈_문자열로_처리한다` — 응답 result 또는 output이 누락되어도 빈 문자열로 정규화한다

#### LlmPropertiesResidualTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/llm/adapter/out/external/LlmPropertiesResidualTest.java`

1. [L11](../../../src/test/java/com/umc/product/llm/adapter/out/external/LlmPropertiesResidualTest.java#L11) · `@Test` · `null_설정을_기본값으로_정규화한다` — null provider와 모델 설정은 운영 기본값으로 정규화한다
2. [L22](../../../src/test/java/com/umc/product/llm/adapter/out/external/LlmPropertiesResidualTest.java#L22) · `@Test` · `rate_limit_경계를_보정한다` — 음수 RPM과 0 burst는 비활성·최소 burst 설정으로 보정한다

## 3.9 `term` — 13개

### Application Service

#### TermPermissionEvaluatorTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/term/application/service/evaluator/TermPermissionEvaluatorTest.java`

1. [L23](../../../src/test/java/com/umc/product/term/application/service/evaluator/TermPermissionEvaluatorTest.java#L23) · `@Test` · `전역_관리자만_약관을_작성한다` — 약관 쓰기는 전역 관리자에게만 허용한다
2. [L39](../../../src/test/java/com/umc/product/term/application/service/evaluator/TermPermissionEvaluatorTest.java#L39) · `@Test` · `미지원_permission을_거부한다` — 미지원 permission은 기본 허용하지 않고 fail-closed한다

#### TermQueryResidualTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/term/application/service/query/TermQueryResidualTest.java`

1. [L27](../../../src/test/java/com/umc/product/term/application/service/query/TermQueryResidualTest.java#L27) · `@Test` · `약관_동의_조회_입력과_빈_이력을_처리한다` — 회원 ID가 없거나 동의 이력이 비어 있으면 port 오조회 없이 실패 또는 빈 목록을 반환한다
2. [L43](../../../src/test/java/com/umc/product/term/application/service/query/TermQueryResidualTest.java#L43) · `@Test` · `동의한_약관_row_누락을_검증한다` — 동의 이력의 약관 row가 부분 누락되면 과거 동의를 다른 버전으로 재해석하지 않고 실패한다
3. [L59](../../../src/test/java/com/umc/product/term/application/service/query/TermQueryResidualTest.java#L59) · `@Test` · `활성_필수_약관_부재를_단축한다` — 필수 약관이 없으면 동의 port를 조회하지 않고 재동의 불필요를 반환한다
4. [L76](../../../src/test/java/com/umc/product/term/application/service/query/TermQueryResidualTest.java#L76) · `@Test` · `필수_약관_ID_중복을_제거한다` — 활성 필수 약관 ID는 중복을 제거한 Set으로 반환한다
5. [L88](../../../src/test/java/com/umc/product/term/application/service/query/TermQueryResidualTest.java#L88) · `@Test` · `사용자_지정_예외_메시지를_보존한다` — 약관 예외의 사용자 지정 메시지를 보존한다

### Persistence

#### TermPersistenceResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/term/adapter/out/persistence/TermPersistenceResidualTest.java`

1. [L27](../../../src/test/java/com/umc/product/term/adapter/out/persistence/TermPersistenceResidualTest.java#L27) · `@Test` · `약관_adapter를_위임한다` — 약관 adapter의 모든 조회·존재·저장·삭제 계약을 repository에 위임한다
2. [L56](../../../src/test/java/com/umc/product/term/adapter/out/persistence/TermPersistenceResidualTest.java#L56) · `@Test` · `약관_동의_adapter를_위임한다` — 약관 동의 adapter는 단건·batch·존재 조회와 저장·삭제를 그대로 위임한다
3. [L82](../../../src/test/java/com/umc/product/term/adapter/out/persistence/TermPersistenceResidualTest.java#L82) · `@Test` · `동의_이력_저장을_위임한다` — 동의 이력 adapter는 append할 로그 저장을 위임한다
4. [L93](../../../src/test/java/com/umc/product/term/adapter/out/persistence/TermPersistenceResidualTest.java#L93) · `@Test` · `약관_QueryDSL_조건과_미존재를_검증한다` — QueryDSL은 활성 타입과 활성 필수 조건을 적용하고 미존재를 Optional.empty로 반환한다

### REST / Web

#### TermControllerResidualTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/term/adapter/in/web/TermControllerResidualTest.java`

1. [L29](../../../src/test/java/com/umc/product/term/adapter/in/web/TermControllerResidualTest.java#L29) · `@Test` · `약관_조회_응답을_변환한다` — 타입·ID 조회 결과와 필수 약관 누락을 응답 DTO로 변환한다
2. [L52](../../../src/test/java/com/umc/product/term/adapter/in/web/TermControllerResidualTest.java#L52) · `@Test` · `약관_생성_요청을_command로_변환한다` — 생성 요청의 link·필수 여부·타입을 command로 정확히 변환한다

## 3.10 `blog` — 61개

### Application Service

#### BlogCommandServiceTest (13개, 신규 파일)

- 위치: `src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java`

1. [L85](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L85) · `@Test` · `생성은_hashtag를_중복_제거해_순서대로_저장한다` — 생성은 기본 DRAFT를 적용하고 중복 hashtag를 입력 순서대로 한 번만 저장한다
2. [L121](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L121) · `@Test` · `생성은_중복_slug를_거부한다` — 생성은 동일 type과 slug가 있으면 저장하지 않는다
3. [L131](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L131) · `@Test` · `빈_hashtag는_관계만_비운다` — 생성과 수정은 빈 hashtag 입력에서 관계를 비우고 조회를 생략한다
4. [L149](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L149) · `@Test` · `수정은_존재와_slug_중복을_검증한다` — 수정은 존재 여부와 slug 중복을 검사한 뒤 변경값을 저장한다
5. [L164](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L164) · `@Test` · `hashtag는_최대_10개까지_허용한다` — hashtag는 최대 10개를 초과하면 기존 관계를 삭제하기 전에 거부한다
6. [L180](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L180) · `@Test` · `삭제는_soft_delete한다` — 삭제는 존재하는 콘텐츠를 soft delete하고 저장한다
7. [L192](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L192) · `@Test` · `삭제는_없는_콘텐츠를_거부한다` — 삭제는 콘텐츠가 없으면 not-found를 반환한다
8. [L233](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L233) · `@Test` · `생성은_slug_중복을_검증한다` — 생성은 중복 slug를 거부하고 정상 입력은 저장한다
9. [L249](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L249) · `@Test` · `수정과_삭제는_존재하는_시리즈만_처리한다` — 수정과 삭제는 존재하고 삭제되지 않은 시리즈만 처리한다
10. [L262](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L262) · `@Test` · `수정은_slug_중복을_거부한다` — 수정은 다른 시리즈의 slug와 중복되면 저장하지 않는다
11. [L274](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L274) · `@Test` · `수정과_삭제는_변경을_저장한다` — 수정과 삭제는 도메인 변경을 저장하고 assembler 결과를 반환한다
12. [L291](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L291) · `@Test` · `콘텐츠_교체는_중복을_제거하고_입력_순서를_보존한다` — 콘텐츠 교체는 중복 ID를 제거하고 입력 순서대로 관계를 저장한다
13. [L314](../../../src/test/java/com/umc/product/blog/application/service/BlogCommandServiceTest.java#L314) · `@Test` · `콘텐츠_교체는_잘못된_콘텐츠를_거부한다` — 콘텐츠 교체는 누락, 삭제, 다른 type 콘텐츠를 거부한다

#### BlogCommentCommandServiceResidualTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/blog/application/service/BlogCommentCommandServiceResidualTest.java`

1. [L56](../../../src/test/java/com/umc/product/blog/application/service/BlogCommentCommandServiceResidualTest.java#L56) · `@Test` · `이단계_댓글은_거부한다` — 대댓글에 다시 답글을 작성하는 2단계 댓글은 거부한다

#### BlogInfoAssemblerTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/blog/application/service/BlogInfoAssemblerTest.java`

1. [L45](../../../src/test/java/com/umc/product/blog/application/service/BlogInfoAssemblerTest.java#L45) · `@Test` · `작성자_assembler는_빈_batch와_부분_조회에_대응한다` — 작성자 assembler는 빈 batch를 단축하고 조회된 회원만 공개 정보로 변환한다
2. [L62](../../../src/test/java/com/umc/product/blog/application/service/BlogInfoAssemblerTest.java#L62) · `@Test` · `콘텐츠_assembler는_빈_목록을_단축한다` — 콘텐츠 assembler는 빈 목록에서 외부 조회 없이 빈 결과를 반환한다
3. [L74](../../../src/test/java/com/umc/product/blog/application/service/BlogInfoAssemblerTest.java#L74) · `@Test` · `콘텐츠_assembler는_연관_정보와_권한을_조립한다` — 콘텐츠 assembler는 시리즈·hashtag·작성자 관계와 권한을 한 번에 조립한다
4. [L111](../../../src/test/java/com/umc/product/blog/application/service/BlogInfoAssemblerTest.java#L111) · `@Test` · `시리즈_assembler는_count와_권한을_조립한다` — 시리즈 assembler는 단건과 목록에서 count 기본값과 관리자 삭제 권한을 적용한다
5. [L131](../../../src/test/java/com/umc/product/blog/application/service/BlogInfoAssemblerTest.java#L131) · `@Test` · `댓글_assembler는_작성자_누락과_삭제를_처리한다` — 댓글 assembler는 guest fallback과 누락 회원, 삭제 placeholder를 안전하게 처리한다

#### BlogQueryServiceTest (10개, 신규 파일)

- 위치: `src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java`

1. [L80](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L80) · `@Test` · `목록은_cursor와_최대_크기를_계산한다` — 목록은 size+1 조회 후 다음 cursor와 최대 크기를 계산한다
2. [L101](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L101) · `@Test` · `목록은_기본_size와_다음_cursor를_계산한다` — 목록은 기본 size를 적용하고 size+1행이면 마지막 노출 ID를 cursor로 반환한다
3. [L121](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L121) · `@Test` · `preview는_미존재와_삭제_콘텐츠를_숨긴다` — preview는 미존재 또는 삭제 콘텐츠를 숨기고 정상 콘텐츠를 조립한다
4. [L140](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L140) · `@Test` · `SEO_path를_info로_변환한다` — SEO path는 persistence row를 application info로 변환한다
5. [L187](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L187) · `@Test` · `시리즈_목록은_cursor를_계산한다` — 시리즈 목록은 optional type과 size+1 cursor 규칙을 적용한다
6. [L211](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L211) · `@Test` · `공개_시리즈는_공개_콘텐츠가_있어야_한다` — 공개 시리즈는 삭제되지 않고 공개 콘텐츠가 있을 때만 조회한다
7. [L226](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L226) · `@Test` · `preview는_미존재와_삭제_시리즈를_숨긴다` — preview는 미존재와 삭제 시리즈를 숨긴다
8. [L243](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L243) · `@Test` · `시리즈_콘텐츠_목록은_cursor를_계산한다` — 시리즈 콘텐츠 목록은 정렬을 검증하고 최대 size와 다음 cursor를 적용한다
9. [L291](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L291) · `@Test` · `해시태그_목록은_기본_size와_count를_적용한다` — 해시태그 목록은 기본 size와 누락 count 0을 적용한다
10. [L309](../../../src/test/java/com/umc/product/blog/application/service/BlogQueryServiceTest.java#L309) · `@Test` · `댓글_목록은_빈_경로를_단축한다` — 댓글 목록은 콘텐츠가 없거나 댓글 page가 비면 빈 cursor를 반환한다

#### BlogPermissionEvaluatorTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/blog/application/service/evaluator/BlogPermissionEvaluatorTest.java`

1. [L56](../../../src/test/java/com/umc/product/blog/application/service/evaluator/BlogPermissionEvaluatorTest.java#L56) · `@Test` · `각_evaluator는_자신의_resource_type을_지원한다` — 각 evaluator는 자신의 blog resource type만 지원한다
2. [L64](../../../src/test/java/com/umc/product/blog/application/service/evaluator/BlogPermissionEvaluatorTest.java#L64) · `@Test` · `생성은_SUPER_ADMIN만_허용한다` — 콘텐츠와 시리즈 생성은 SUPER_ADMIN만 허용하고 ID가 없는 나머지 요청은 거부한다
3. [L79](../../../src/test/java/com/umc/product/blog/application/service/evaluator/BlogPermissionEvaluatorTest.java#L79) · `@Test` · `존재하지_않거나_삭제된_리소스는_거부한다` — 존재하지 않거나 삭제된 콘텐츠와 시리즈는 fail-closed로 거부한다
4. [L101](../../../src/test/java/com/umc/product/blog/application/service/evaluator/BlogPermissionEvaluatorTest.java#L101) · `@Test` · `콘텐츠와_시리즈는_작성자_권한과_관리자_호환성을_보장한다` — 콘텐츠와 시리즈의 READ DELETE는 작성자 또는 관리자, EDIT는 작성자만 허용한다
5. [L127](../../../src/test/java/com/umc/product/blog/application/service/evaluator/BlogPermissionEvaluatorTest.java#L127) · `@Test` · `댓글은_작성자와_관리자_규칙을_따른다` — 댓글은 작성자만 수정하고 작성자 또는 관리자만 삭제한다
6. [L147](../../../src/test/java/com/umc/product/blog/application/service/evaluator/BlogPermissionEvaluatorTest.java#L147) · `@Test` · `지원하지_않는_permission은_거부한다` — 지원하지 않는 permission은 모든 evaluator에서 fail-closed로 거부한다

### Domain

#### BlogDomainResidualTest (9개, 신규 파일)

- 위치: `src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java`

1. [L14](../../../src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java#L14) · `@Test` · `콘텐츠_수정은_선택값을_정규화하고_상태를_멱등_처리한다` — 콘텐츠 수정은 선택값을 정규화하고 공개 전환과 동일 상태 요청을 멱등 처리한다
2. [L38](../../../src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java#L38) · `@Test` · `콘텐츠는_잘못된_상태_전이를_거부한다` — 콘텐츠는 공개되지 않은 상태와 직접 삭제 상태 전환을 거부한다
3. [L58](../../../src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java#L58) · `@Test` · `콘텐츠_생성은_모든_경계를_검증한다` — 콘텐츠 생성은 모든 필수값과 길이 제한을 검증한다
4. [L76](../../../src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java#L76) · `@Test` · `시리즈_수정과_삭제는_불변식을_보장한다` — 시리즈 수정과 삭제는 정규화, 멱등성, 삭제 후 변경 금지를 보장한다
5. [L100](../../../src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java#L100) · `@Test` · `시리즈_생성은_모든_경계를_검증한다` — 시리즈 생성은 모든 필수값과 길이 제한을 검증한다
6. [L115](../../../src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java#L115) · `@Test` · `댓글은_식별자와_삭제_경계를_검증한다` — 댓글은 parent ID, 삭제 주체, 중복 삭제와 수정 경계를 검증한다
7. [L136](../../../src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java#L136) · `@Test` · `연결_엔티티와_hashtag는_경계를_검증한다` — 연결 엔티티와 hashtag는 유효하지 않은 ID와 이름 경계를 거부한다
8. [L152](../../../src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java#L152) · `@Test` · `정렬_값은_입력을_엄격히_파싱한다` — 정렬 값은 기본값과 방향을 제공하고 지원하지 않는 입력을 거부한다
9. [L169](../../../src/test/java/com/umc/product/blog/domain/BlogDomainResidualTest.java#L169) · `@Test` · `콘텐츠_타입과_domain_예외를_검증한다` — 콘텐츠 타입 null과 사용자 메시지 domain 예외를 명시적으로 지원한다

### Persistence

#### BlogPersistenceQueryRepositoryTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/blog/adapter/out/persistence/BlogPersistenceQueryRepositoryTest.java`

1. [L112](../../../src/test/java/com/umc/product/blog/adapter/out/persistence/BlogPersistenceQueryRepositoryTest.java#L112) · `@Test` · `콘텐츠_목록은_필터와_cursor를_검증한다` — 콘텐츠 목록은 공개 상태, type, 시리즈, hashtag, 양방향 cursor와 정렬을 검증한다
2. [L157](../../../src/test/java/com/umc/product/blog/adapter/out/persistence/BlogPersistenceQueryRepositoryTest.java#L157) · `@Test` · `연결_콘텐츠_조회는_cursor와_순서를_검증한다` — 시리즈와 hashtag 콘텐츠 조회는 표시 순서·type·cursor를 보장하고 잘못된 cursor를 거부한다
3. [L180](../../../src/test/java/com/umc/product/blog/adapter/out/persistence/BlogPersistenceQueryRepositoryTest.java#L180) · `@Test` · `시리즈_조회는_공개_관계와_빈_batch를_검증한다` — 시리즈 조회는 공개 콘텐츠 존재, type, 양방향 cursor, 그룹·count의 빈 입력을 검증한다
4. [L210](../../../src/test/java/com/umc/product/blog/adapter/out/persistence/BlogPersistenceQueryRepositoryTest.java#L210) · `@Test` · `hashtag_조회는_검색_cursor_count를_검증한다` — hashtag 조회는 검색어·count 정렬·cursor·빈 batch와 type count를 검증한다
5. [L240](../../../src/test/java/com/umc/product/blog/adapter/out/persistence/BlogPersistenceQueryRepositoryTest.java#L240) · `@Test` · `댓글과_좋아요_조회는_가시성과_빈_batch를_검증한다` — 댓글 조회는 top-level 가시성, 양방향 cursor, reply 빈 batch와 좋아요 집계를 검증한다
6. [L272](../../../src/test/java/com/umc/product/blog/adapter/out/persistence/BlogPersistenceQueryRepositoryTest.java#L272) · `@Test` · `adapter_계약과_SEO_path를_검증한다` — adapter는 기본 조회·중복 제외·빈 ID와 SEO content/series/hashtag path를 보존한다

### REST / Web

#### BlogControllerUnitTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/blog/adapter/in/web/BlogControllerUnitTest.java`

1. [L68](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogControllerUnitTest.java#L68) · `@Test` · `목록은_cursor_response로_변환한다` — 목록은 비회원과 회원 식별자를 구분해 cursor response로 변환한다
2. [L83](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogControllerUnitTest.java#L83) · `@Test` · `preview와_수정은_info를_응답으로_변환한다` — preview와 수정은 application info를 응답으로 변환한다
3. [L95](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogControllerUnitTest.java#L95) · `@Test` · `삭제는_회원_ID를_요구한다` — 삭제는 현재 회원 ID를 command에 담고 비인증 요청은 거부한다
4. [L134](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogControllerUnitTest.java#L134) · `@Test` · `목록은_cursor_metadata를_보존한다` — 시리즈 목록과 콘텐츠 목록은 cursor metadata를 보존한다
5. [L156](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogControllerUnitTest.java#L156) · `@Test` · `상세와_수정은_info를_응답으로_변환한다` — 공개 상세, preview, 수정은 application info를 응답으로 변환한다
6. [L170](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogControllerUnitTest.java#L170) · `@Test` · `삭제는_회원_ID를_요구한다` — 삭제는 현재 회원 ID를 command에 담고 비인증 요청은 거부한다
7. [L216](../../../src/test/java/com/umc/product/blog/adapter/in/web/BlogControllerUnitTest.java#L216) · `@Test` · `댓글_좋아요는_인증_회원을_요구한다` — 댓글 좋아요 토글은 인증 회원을 요구한다

#### BlogDtoResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/blog/adapter/in/web/dto/BlogDtoResidualTest.java`

1. [L30](../../../src/test/java/com/umc/product/blog/adapter/in/web/dto/BlogDtoResidualTest.java#L30) · `@Test` · `시리즈_요약_응답은_모든_조회_값을_보존한다` — 시리즈 요약 응답은 작성자를 포함한 모든 조회 값을 보존한다
2. [L76](../../../src/test/java/com/umc/product/blog/adapter/in/web/dto/BlogDtoResidualTest.java#L76) · `@Test` · `목록과_삭제_command_factory는_값을_그대로_전달한다` — 목록과 삭제 command factory는 식별자와 조회 조건을 그대로 전달한다
3. [L86](../../../src/test/java/com/umc/product/blog/adapter/in/web/dto/BlogDtoResidualTest.java#L86) · `@Test` · `콘텐츠_수정_command는_hashtag를_안전하게_복사한다` — 콘텐츠 수정 command는 hashtag null을 빈 불변 목록으로 정규화한다
4. [L98](../../../src/test/java/com/umc/product/blog/adapter/in/web/dto/BlogDtoResidualTest.java#L98) · `@Test` · `웹_요청은_공백을_정규화하고_command로_변환한다` — 웹 요청은 공백을 정규화하고 create 및 update command로 변환한다

## 3.11 `certificate` — 26개

### Application DTO / Port

#### CertificateDtoTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/certificate/application/port/CertificateDtoTest.java`

1. [L18](../../../src/test/java/com/umc/product/certificate/application/port/CertificateDtoTest.java#L18) · `@Test` · `PDF_렌더_명령은_일련번호를_발급번호_기본값으로_사용한다` — PDF 렌더 명령은 일련번호를 발급번호 기본값으로 사용한다
2. [L33](../../../src/test/java/com/umc/product/certificate/application/port/CertificateDtoTest.java#L33) · `@Test` · `폐기_명령은_null_또는_blank_사유를_거부한다` — 폐기 명령은 null 또는 blank 사유를 거부한다

### Application Service

#### CertificateCommandServiceTest (6개, 보강 파일)

- 위치: `src/test/java/com/umc/product/certificate/application/service/CertificateCommandServiceTest.java`

1. [L350](../../../src/test/java/com/umc/product/certificate/application/service/CertificateCommandServiceTest.java#L350) · `@Test` · `운영진은_발급된_인증서를_폐기할_수_있다` — 운영진은 발급된 인증서를 폐기할 수 있다
2. [L373](../../../src/test/java/com/umc/product/certificate/application/service/CertificateCommandServiceTest.java#L373) · `@Test` · `재발급이_완료되면_기존_인증서를_폐기하고_새_인증서를_저장한다` — 재발급이 완료되면 기존 인증서를 폐기하고 새 인증서를 저장한다
3. [L414](../../../src/test/java/com/umc/product/certificate/application/service/CertificateCommandServiceTest.java#L414) · `@Test` · `일련번호가_연속_중복되면_제한_횟수_뒤_발급을_중단한다` — 일련번호가 연속 중복되면 제한 횟수 뒤 발급을 중단한다
4. [L443](../../../src/test/java/com/umc/product/certificate/application/service/CertificateCommandServiceTest.java#L443) · `@Test` · `해당_기수_중앙_운영진은_관리자_발급_경로에_진입할_수_있다` — 해당 기수 중앙 운영진은 관리자 발급 경로에 진입할 수 있다
5. [L460](../../../src/test/java/com/umc/product/certificate/application/service/CertificateCommandServiceTest.java#L460) · `@Test` · `관리_권한이_없는_회원의_관리자_발급을_거부한다` — 관리 권한이 없는 회원의 관리자 발급을 거부한다
6. [L475](../../../src/test/java/com/umc/product/certificate/application/service/CertificateCommandServiceTest.java#L475) · `@Test` · `JVM이_SHA_256을_제공하지_않으면_인증서_발급을_안전하게_중단한다` — JVM이 SHA-256을 제공하지 않으면 인증서 발급을 안전하게 중단한다

#### CertificateIssueContextResolverTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/certificate/application/service/CertificateIssueContextResolverTest.java`

1. [L166](../../../src/test/java/com/umc/product/certificate/application/service/CertificateIssueContextResolverTest.java#L166) · `@Test` · `해당_기수_challenger가_없으면_수료증_발급_조건을_만족하지_않는다` — 해당 기수 challenger가 없으면 수료증 발급 조건을 만족하지 않는다
2. [L249](../../../src/test/java/com/umc/product/certificate/application/service/CertificateIssueContextResolverTest.java#L249) · `@Test` · `공로_인증서의_커스텀_제목과_기본_제목이_모두_없으면_발급을_거부한다` — 공로 인증서의 커스텀 제목과 기본 제목이 모두 없으면 발급을 거부한다

#### CertificatePropertiesTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/certificate/application/service/CertificatePropertiesTest.java`

1. [L10](../../../src/test/java/com/umc/product/certificate/application/service/CertificatePropertiesTest.java#L10) · `@Test` · `검증_URL_설정이_비어_있으면_기본_경로를_사용한다` — 검증 URL 설정이 비어 있으면 기본 경로를 사용한다

#### CertificateQueryServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/certificate/application/service/CertificateQueryServiceTest.java`

1. [L160](../../../src/test/java/com/umc/product/certificate/application/service/CertificateQueryServiceTest.java#L160) · `@Test` · `유효한_본인_인증서는_파일_다운로드_정보를_반환한다` — 유효한 본인 인증서는 파일 다운로드 정보를 반환한다
2. [L187](../../../src/test/java/com/umc/product/certificate/application/service/CertificateQueryServiceTest.java#L187) · `@Test` · `다른_회원의_인증서는_다운로드할_수_없다` — 다른 회원의 인증서는 다운로드할 수 없다
3. [L196](../../../src/test/java/com/umc/product/certificate/application/service/CertificateQueryServiceTest.java#L196) · `@Test` · `만료된_인증서는_소유자도_다운로드할_수_없다` — 만료된 인증서는 소유자도 다운로드할 수 없다
4. [L206](../../../src/test/java/com/umc/product/certificate/application/service/CertificateQueryServiceTest.java#L206) · `@Test` · `한_글자와_두_글자_이름은_노출_없이_길이에_맞게_마스킹한다` — 한 글자와 두 글자 이름은 노출 없이 길이에 맞게 마스킹한다

#### CertificateSerialNumberGeneratorTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/certificate/application/service/CertificateSerialNumberGeneratorTest.java`

1. [L43](../../../src/test/java/com/umc/product/certificate/application/service/CertificateSerialNumberGeneratorTest.java#L43) · `@Test` · `기본_생성자는_보안_난수로_일련번호를_생성한다` — 기본 생성자는 보안 난수로 일련번호를 생성한다

### Domain

#### CertificateTemplateTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/certificate/domain/CertificateTemplateTest.java`

1. [L69](../../../src/test/java/com/umc/product/certificate/domain/CertificateTemplateTest.java#L69) · `@Test` · `템플릿은_행사_식별자와_영문_기수_서수를_제공한다` — 템플릿은 행사 식별자와 영문 기수 서수를 제공한다

#### CertificateTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/certificate/domain/CertificateTest.java`

1. [L49](../../../src/test/java/com/umc/product/certificate/domain/CertificateTest.java#L49) · `@Test` · `이미_폐기된_인증서는_다시_폐기할_수_없다` — 이미 폐기된 인증서는 다시 폐기할 수 없다

### Outbound Adapter

#### ThymeleafCertificatePdfAdapterTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/certificate/adapter/out/pdf/ThymeleafCertificatePdfAdapterTest.java`

1. [L100](../../../src/test/java/com/umc/product/certificate/adapter/out/pdf/ThymeleafCertificatePdfAdapterTest.java#L100) · `@Test` · `레이아웃의_도형_fallback_색상_blank_wrap_clip_분기를_실제_PDF에_반영한다` — 레이아웃의 도형·fallback 색상·blank·wrap·clip 분기를 실제 PDF에 반영한다
2. [L115](../../../src/test/java/com/umc/product/certificate/adapter/out/pdf/ThymeleafCertificatePdfAdapterTest.java#L115) · `@Test` · `최소_글꼴로도_필드에_맞지_않고_overflow_정책이_error이면_렌더링을_거부한다` — 최소 글꼴로도 필드에 맞지 않고 overflow 정책이 error이면 렌더링을 거부한다
3. [L125](../../../src/test/java/com/umc/product/certificate/adapter/out/pdf/ThymeleafCertificatePdfAdapterTest.java#L125) · `@Test` · `템플릿_설정_리소스가_없으면_렌더링_예외로_변환한다` — 템플릿 설정 리소스가 없으면 렌더링 예외로 변환한다
4. [L134](../../../src/test/java/com/umc/product/certificate/adapter/out/pdf/ThymeleafCertificatePdfAdapterTest.java#L134) · `@Test` · `QR_코드로_만들_수_없는_빈_검증_URL은_렌더링_예외로_변환한다` — QR 코드로 만들 수 없는 빈 검증 URL은 렌더링 예외로 변환한다

### Persistence

#### CertificatePersistenceAdapterTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/certificate/adapter/out/persistence/CertificatePersistenceAdapterTest.java`

1. [L177](../../../src/test/java/com/umc/product/certificate/adapter/out/persistence/CertificatePersistenceAdapterTest.java#L177) · `@Test` · `저장_및_단건_목록_조회_port_계약을_repository에_위임한다` — 저장 및 단건·목록 조회 port 계약을 repository에 위임한다
2. [L203](../../../src/test/java/com/umc/product/certificate/adapter/out/persistence/CertificatePersistenceAdapterTest.java#L203) · `@Test` · `필수_조회_대상_인증서가_없으면_not_found_예외를_던진다` — 필수 조회 대상 인증서가 없으면 not-found 예외를 던진다

### REST / Web

#### CertificateControllerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/certificate/adapter/in/web/CertificateControllerTest.java`

1. [L221](../../../src/test/java/com/umc/product/certificate/adapter/in/web/CertificateControllerTest.java#L221) · `@Test` · `본인_인증서_목록을_응답_DTO로_변환한다` — 본인 인증서 목록을 응답 DTO로 변환한다
2. [L244](../../../src/test/java/com/umc/product/certificate/adapter/in/web/CertificateControllerTest.java#L244) · `@Test` · `본인_인증서_다운로드_정보를_응답_DTO로_변환한다` — 본인 인증서 다운로드 정보를 응답 DTO로 변환한다

## 3.12 `chat` — 36개

### Application Service

#### ChatMemberCommandServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/service/command/ChatMemberCommandServiceTest.java`

1. [L116](../../../src/test/java/com/umc/product/chat/application/service/command/ChatMemberCommandServiceTest.java#L116) · `@Test` · `joinChatRoom_concurrentInsertRejected` — 사전 조회 뒤 동시 삽입이 발생하면 원자적 save 결과로 중복 참여를 거절한다

#### ChatMessageLifecycleCreateServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/service/command/ChatMessageLifecycleCreateServiceTest.java`

1. [L213](../../../src/test/java/com/umc/product/chat/application/service/command/ChatMessageLifecycleCreateServiceTest.java#L213) · `@Test` · `create_legacyCanonicalPayloadReplay` — fingerprint가 없는 legacy row도 canonical payload와 mention이 같으면 replay한다

#### ChatMessageLifecycleMutationServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/service/command/ChatMessageLifecycleMutationServiceTest.java`

1. [L181](../../../src/test/java/com/umc/product/chat/application/service/command/ChatMessageLifecycleMutationServiceTest.java#L181) · `@Test` · `tombstone_nonAuthorWithoutModeratorRejected` — 작성자가 아니며 moderator도 아닌 멤버는 tombstone할 수 없다

#### ChatRoomCommandServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/service/command/ChatRoomCommandServiceTest.java`

1. [L52](../../../src/test/java/com/umc/product/chat/application/service/command/ChatRoomCommandServiceTest.java#L52) · `@Test` · `create` — 채팅방 생성 시 생성자를 첫 멤버로 저장하고 방 정보를 반환한다
2. [L72](../../../src/test/java/com/umc/product/chat/application/service/command/ChatRoomCommandServiceTest.java#L72) · `@Test` · `delete` — 채팅방 삭제는 대상 방을 조회한 뒤 같은 entity를 삭제한다

#### ChatMessageInfoAssemblerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/service/query/ChatMessageInfoAssemblerTest.java`

1. [L121](../../../src/test/java/com/umc/product/chat/application/service/query/ChatMessageInfoAssemblerTest.java#L121) · `@Test` · `assembleForViewers_emptyBatchShortCircuits` — 빈 viewer batch는 mention·reaction·reply 조회 없이 빈 결과를 반환한다
2. [L130](../../../src/test/java/com/umc/product/chat/application/service/query/ChatMessageInfoAssemblerTest.java#L130) · `@Test` · `assemble_replyWithoutContentKeepsNullSnippet` — 본문이 없는 IMAGE 답장의 snippet은 null로 유지한다

#### ChatMessageQueryServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/service/query/ChatMessageQueryServiceTest.java`

1. [L126](../../../src/test/java/com/umc/product/chat/application/service/query/ChatMessageQueryServiceTest.java#L126) · `@Test` · `getMessageForViewers_emptyBatchShortCircuits` — 빈 viewer batch는 membership과 메시지를 조회하지 않고 빈 결과를 반환한다
2. [L138](../../../src/test/java/com/umc/product/chat/application/service/query/ChatMessageQueryServiceTest.java#L138) · `@Test` · `getRoomId_returnsMessageRoom` — message ID로 소유 room ID를 조회한다

#### ChatRoomQueryServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/service/query/ChatRoomQueryServiceTest.java`

1. [L106](../../../src/test/java/com/umc/product/chat/application/service/query/ChatRoomQueryServiceTest.java#L106) · `@Test` · `has_chat_room_access` — 채팅방 접근 여부는 멤버 존재 조회 결과를 그대로 반환한다

### Contract / Misc

#### ChatDtoEdgeCaseTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/chat/ChatDtoEdgeCaseTest.java`

1. [L22](../../../src/test/java/com/umc/product/chat/ChatDtoEdgeCaseTest.java#L22) · `@Test` · `createCommand_rejectsInvalidCollections` — 메시지 생성 command는 blank 첨부와 비양수 mention을 거절한다
2. [L43](../../../src/test/java/com/umc/product/chat/ChatDtoEdgeCaseTest.java#L43) · `@Test` · `viewerQuery_validatesAndDeduplicatesIds` — viewer batch query는 양수 ID만 허용하고 입력 중복을 제거한다

### Domain

#### ChatMessageTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/domain/ChatMessageTest.java`

1. [L206](../../../src/test/java/com/umc/product/chat/domain/ChatMessageTest.java#L206) · `@Test` · `edit_validatesByContentType` — TEXT blank 수정은 거절하고 IMAGE는 null caption 수정도 허용한다
2. [L226](../../../src/test/java/com/umc/product/chat/domain/ChatMessageTest.java#L226) · `@Test` · `edit_rejectsSystemAndDeletedMessages` — SYSTEM 또는 이미 삭제된 메시지의 수정은 mutation forbidden으로 거절한다

#### ChatValueResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/chat/domain/ChatValueResidualTest.java`

1. [L23](../../../src/test/java/com/umc/product/chat/domain/ChatValueResidualTest.java#L23) · `@Test` · `command_factories` — command factory는 입력 ID를 변경하지 않는다
2. [L31](../../../src/test/java/com/umc/product/chat/domain/ChatValueResidualTest.java#L31) · `@Test` · `chat_message_info_legacy_constructor` — 7개 필드 ChatMessageInfo 생성자는 reply ID를 null로 둔다
3. [L49](../../../src/test/java/com/umc/product/chat/domain/ChatValueResidualTest.java#L49) · `@Test` · `chat_message_created_event` — 메시지 생성 event는 전체 payload와 고정 event type을 보존한다
4. [L83](../../../src/test/java/com/umc/product/chat/domain/ChatValueResidualTest.java#L83) · `@Test` · `chat_domain_exception_constructors` — ChatDomainException은 기본·사용자 message 생성자를 모두 지원한다

#### ChatRealtimeEventDispatchModeTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/domain/event/ChatRealtimeEventDispatchModeTest.java`

1. [L53](../../../src/test/java/com/umc/product/chat/domain/event/ChatRealtimeEventDispatchModeTest.java#L53) · `@Test` · `createdEventReplyCompatibilityConstructor` — reply ID 호환 생성자는 나머지 optional realtime 필드를 비운다

### Persistence

#### ChatCommunityMessagePersistenceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/adapter/out/persistence/ChatCommunityMessagePersistenceTest.java`

1. [L148](../../../src/test/java/com/umc/product/chat/adapter/out/persistence/ChatCommunityMessagePersistenceTest.java#L148) · `@Test` · `emptyBatchShortCircuits` — 빈 message·viewer batch는 SQL을 만들지 않고 빈 projection을 반환한다

#### ChatMessageQueryRepositoryTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/adapter/out/persistence/ChatMessageQueryRepositoryTest.java`

1. [L101](../../../src/test/java/com/umc/product/chat/adapter/out/persistence/ChatMessageQueryRepositoryTest.java#L101) · `@Test` · `empty_room_inputs` — 빈 방 ID 입력과 메시지가 없는 방 목록은 빈 결과를 반환한다

#### ChatPersistenceAdapterTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/chat/adapter/out/persistence/ChatPersistenceAdapterTest.java`

1. [L44](../../../src/test/java/com/umc/product/chat/adapter/out/persistence/ChatPersistenceAdapterTest.java#L44) · `@Test` · `room_crud` — 채팅방 저장·삭제·일반/lock 조회를 repository에 위임한다
2. [L60](../../../src/test/java/com/umc/product/chat/adapter/out/persistence/ChatPersistenceAdapterTest.java#L60) · `@Test` · `room_not_found` — 일반/lock 조회에서 채팅방이 없으면 동일한 not-found 예외를 던진다
3. [L70](../../../src/test/java/com/umc/product/chat/adapter/out/persistence/ChatPersistenceAdapterTest.java#L70) · `@Test` · `member_operations` — 채팅 멤버 저장·중복 방지·삭제·존재·목록·읽음 갱신을 repository에 위임한다
4. [L99](../../../src/test/java/com/umc/product/chat/adapter/out/persistence/ChatPersistenceAdapterTest.java#L99) · `@Test` · `member_not_found` — 채팅 멤버가 없으면 not-found 예외를 던진다
5. [L110](../../../src/test/java/com/umc/product/chat/adapter/out/persistence/ChatPersistenceAdapterTest.java#L110) · `@Test` · `message_operations` — 메시지 저장·조회·존재·cursor/unread 조회를 repository에 위임한다
6. [L133](../../../src/test/java/com/umc/product/chat/adapter/out/persistence/ChatPersistenceAdapterTest.java#L133) · `@Test` · `message_not_found` — ID 또는 방 조건으로 메시지가 없으면 동일한 not-found 예외를 던진다

### Policy

#### ChatAttachmentPolicyTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/policy/ChatAttachmentPolicyTest.java`

1. [L89](../../../src/test/java/com/umc/product/chat/application/policy/ChatAttachmentPolicyTest.java#L89) · `@Test` · `validate_attachment_not_allowed` — 첨부를 허용하지 않는 메시지 타입은 즉시 거부한다
2. [L98](../../../src/test/java/com/umc/product/chat/application/policy/ChatAttachmentPolicyTest.java#L98) · `@Test` · `validate_null_metadata` — 확장자나 contentType이 null인 파일은 거부한다

#### ChatMessagePayloadFingerprintTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/policy/ChatMessagePayloadFingerprintTest.java`

1. [L47](../../../src/test/java/com/umc/product/chat/application/policy/ChatMessagePayloadFingerprintTest.java#L47) · `@Test` · `unavailableSha256FailsExplicitly` — 런타임에 SHA-256을 제공하지 않으면 원인을 보존해 명시적으로 실패한다

#### ChatReactionPolicyTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/policy/ChatReactionPolicyTest.java`

1. [L35](../../../src/test/java/com/umc/product/chat/application/policy/ChatReactionPolicyTest.java#L35) · `@Test` · `validate_requiredBoundaries` — null·blank·길이 초과 reaction을 grapheme 검사 전에 거절한다

#### ChatRoomAccessPolicyTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/chat/application/policy/ChatRoomAccessPolicyTest.java`

1. [L20](../../../src/test/java/com/umc/product/chat/application/policy/ChatRoomAccessPolicyTest.java#L20) · `@Test` · `verify_member` — 방 멤버는 접근할 수 있고 비멤버는 fail-closed 처리한다

#### CommunityChatMessagePolicyTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/chat/application/policy/CommunityChatMessagePolicyTest.java`

1. [L116](../../../src/test/java/com/umc/product/chat/application/policy/CommunityChatMessagePolicyTest.java#L116) · `@Test` · `create_rejectsMissingClientIdAndUnsupportedType` — client ID 누락과 외부 생성 불가 type을 명시 오류로 거절한다
2. [L136](../../../src/test/java/com/umc/product/chat/application/policy/CommunityChatMessagePolicyTest.java#L136) · `@Test` · `create_rejectsTypeSpecificInvalidPayloads` — TEXT는 blank 본문과 첨부를 거절하고 IMAGE는 중복 첨부를 거절한다
3. [L163](../../../src/test/java/com/umc/product/chat/application/policy/CommunityChatMessagePolicyTest.java#L163) · `@Test` · `create_rejectsMentionOverflowAndMissingFileSize` — 멘션 최대 수 초과와 이미지 크기 정보 누락을 fail-closed한다
4. [L194](../../../src/test/java/com/umc/product/chat/application/policy/CommunityChatMessagePolicyTest.java#L194) · `@Test` · `attachments_rejectsTotalSizeOverflow` — 개별 파일 제한 이하여도 이미지 전체 크기가 40MiB를 넘으면 거절한다

## 3.13 `community` — 166개

### Application DTO / Port

#### CommunityThreadCommandDtoEdgeCaseTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/port/in/command/thread/dto/CommunityThreadCommandDtoEdgeCaseTest.java`

1. [L19](../../../src/test/java/com/umc/product/community/application/port/in/command/thread/dto/CommunityThreadCommandDtoEdgeCaseTest.java#L19) · `@Test` · `normalizesValidCreateCommand` — create command는 텍스트를 정규화하고 null invitee를 빈 목록으로 고정한다
2. [L37](../../../src/test/java/com/umc/product/community/application/port/in/command/thread/dto/CommunityThreadCommandDtoEdgeCaseTest.java#L37) · `@Test` · `rejectsInvalidCreateCommands` — create command는 필수값·길이·중복·자기 초대 위반을 동일 domain code로 거부한다
3. [L53](../../../src/test/java/com/umc/product/community/application/port/in/command/thread/dto/CommunityThreadCommandDtoEdgeCaseTest.java#L53) · `@Test` · `validatesKickCommand` — kick command는 양수 ID와 actor·target 분리를 강제한다
4. [L61](../../../src/test/java/com/umc/product/community/application/port/in/command/thread/dto/CommunityThreadCommandDtoEdgeCaseTest.java#L61) · `@Test` · `validatesSharedCommandConstraints` — 공통 validation은 empty 허용 여부와 null required 값을 fail-fast로 구분한다

#### CommunityThreadQueryDtoEdgeCaseTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/port/in/query/thread/dto/CommunityThreadQueryDtoEdgeCaseTest.java`

1. [L15](../../../src/test/java/com/umc/product/community/application/port/in/query/thread/dto/CommunityThreadQueryDtoEdgeCaseTest.java#L15) · `@Test` · `validatesSharedQueryConstraints` — 공통 query constraint는 keyword를 정규화하고 양수·unique ID를 보장한다
2. [L36](../../../src/test/java/com/umc/product/community/application/port/in/query/thread/dto/CommunityThreadQueryDtoEdgeCaseTest.java#L36) · `@Test` · `validatesListQueries` — thread 목록 query는 filter·generation·page 경계를 검증한다
3. [L50](../../../src/test/java/com/umc/product/community/application/port/in/query/thread/dto/CommunityThreadQueryDtoEdgeCaseTest.java#L50) · `@Test` · `validatesPageDtos` — page DTO는 null collection을 비우고 음수 cursor·total을 거부한다

#### CommunityThreadMessageDtoEdgeCaseTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/port/in/query/thread/message/dto/CommunityThreadMessageDtoEdgeCaseTest.java`

1. [L25](../../../src/test/java/com/umc/product/community/application/port/in/query/thread/message/dto/CommunityThreadMessageDtoEdgeCaseTest.java#L25) · `@Test` · `normalizesCreateCommandCollections` — create command는 null collection을 비우고 mention을 정렬·중복 제거한다
2. [L38](../../../src/test/java/com/umc/product/community/application/port/in/query/thread/message/dto/CommunityThreadMessageDtoEdgeCaseTest.java#L38) · `@Test` · `rejectsInvalidCreateCommand` — create command는 ID·client ID·type·mention·reply 경계를 거부한다
3. [L57](../../../src/test/java/com/umc/product/community/application/port/in/query/thread/message/dto/CommunityThreadMessageDtoEdgeCaseTest.java#L57) · `@Test` · `validatesMutationCommandIds` — edit·delete·reaction·read command는 모든 resource ID를 양수로 강제한다
4. [L73](../../../src/test/java/com/umc/product/community/application/port/in/query/thread/message/dto/CommunityThreadMessageDtoEdgeCaseTest.java#L73) · `@Test` · `validatesMessageQueries` — message query는 cursor·limit·recipient의 양수 및 null 계약을 검증한다
5. [L95](../../../src/test/java/com/umc/product/community/application/port/in/query/thread/message/dto/CommunityThreadMessageDtoEdgeCaseTest.java#L95) · `@Test` · `validatesMessageReadModels` — message read model은 null collection을 비우고 음수 count·cursor·ID를 거부한다

#### CommunityThreadRealtimeDtoEdgeCaseTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/port/in/realtime/dto/CommunityThreadRealtimeDtoEdgeCaseTest.java`

1. [L29](../../../src/test/java/com/umc/product/community/application/port/in/realtime/dto/CommunityThreadRealtimeDtoEdgeCaseTest.java#L29) · `@Test` · `exposesEveryCommandWireValue` — 모든 realtime command enum은 외부 wire value를 그대로 노출한다
2. [L47](../../../src/test/java/com/umc/product/community/application/port/in/realtime/dto/CommunityThreadRealtimeDtoEdgeCaseTest.java#L47) · `@Test` · `validatesPreviouslyUnexecutedPayloads` — ACK와 thread deleted payload는 필수 상관관계·삭제 시각을 검증한다
3. [L91](../../../src/test/java/com/umc/product/community/application/port/in/realtime/dto/CommunityThreadRealtimeDtoEdgeCaseTest.java#L91) · `@Test` · `validatesRealtimeEnvelope` — realtime event는 양수 thread ID 문자열과 필수 envelope 필드를 강제한다
4. [L128](../../../src/test/java/com/umc/product/community/application/port/in/realtime/dto/CommunityThreadRealtimeDtoEdgeCaseTest.java#L128) · `@Test` · `validatesRemainingPayloads` — message·reaction·read·lifecycle payload는 null과 non-positive 경계를 거부한다

### Application Event

#### CommunityThreadApplicationEventEdgeCaseTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/event/CommunityThreadApplicationEventEdgeCaseTest.java`

1. [L17](../../../src/test/java/com/umc/product/community/application/event/CommunityThreadApplicationEventEdgeCaseTest.java#L17) · `@Test` · `normalizesRecipientSnapshots` — created·mentioned event는 null recipient를 비우고 ID를 정렬·중복 제거한다
2. [L37](../../../src/test/java/com/umc/product/community/application/event/CommunityThreadApplicationEventEdgeCaseTest.java#L37) · `@Test` · `rejectsInvalidEventIds` — created·mentioned event는 aggregate·message·sender·recipient ID를 양수로 강제한다

### Application Service

#### CommunityCommandServiceResidualTest (16개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java`

1. [L76](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L76) · `@Test` · `번개_게시글을_생성하고_작성자_정보를_결합한다` — 번개 게시글을 생성하고 작성자 정보를 결합한다
2. [L99](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L99) · `@Test` · `일반_게시글을_수정하고_기존_작성자_정보를_유지한다` — 일반 게시글을 수정하고 기존 작성자 정보를 유지한다
3. [L114](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L114) · `@Test` · `없는_일반_게시글_수정은_not_found로_실패한다` — 없는 일반 게시글 수정은 not-found로 실패한다
4. [L124](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L124) · `@Test` · `번개_게시글을_수정하고_새_번개_정보를_반환한다` — 번개 게시글을 수정하고 새 번개 정보를 반환한다
5. [L148](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L148) · `@Test` · `없는_번개_게시글_수정은_not_found로_실패한다` — 없는 번개 게시글 수정은 not-found로 실패한다
6. [L164](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L164) · `@Test` · `게시글_좋아요는_존재_확인_후_port_결과를_반환한다` — 게시글 좋아요는 존재 확인 후 port 결과를 반환한다
7. [L175](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L175) · `@Test` · `없는_게시글은_좋아요를_변경하지_않는다` — 없는 게시글은 좋아요를 변경하지 않는다
8. [L185](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L185) · `@Test` · `댓글을_생성하고_작성자_이름을_결합한다` — 댓글을 생성하고 작성자 이름을 결합한다
9. [L200](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L200) · `@Test` · `없는_게시글에는_댓글을_생성하지_않는다` — 없는 게시글에는 댓글을 생성하지 않는다
10. [L209](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L209) · `@Test` · `댓글은_작성자만_삭제할_수_있다` — 댓글은 작성자만 삭제할 수 있다
11. [L220](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L220) · `@Test` · `없는_댓글과_다른_작성자의_댓글_삭제를_거부한다` — 없는 댓글과 다른 작성자의 댓글 삭제를 거부한다
12. [L233](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L233) · `@Test` · `댓글_좋아요는_존재_확인_후_port_결과를_반환한다` — 댓글 좋아요는 존재 확인 후 port 결과를 반환한다
13. [L244](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L244) · `@Test` · `없는_게시글의_스크랩_toggle을_거부한다` — 없는 게시글의 스크랩 toggle을 거부한다
14. [L253](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L253) · `@Test` · `게시글_스크랩_toggle_결과와_최신_개수를_반환한다` — 게시글 스크랩 toggle 결과와 최신 개수를 반환한다
15. [L266](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L266) · `@Test` · `게시글과_댓글_신고를_생성한다` — 게시글과 댓글 신고를 생성한다
16. [L283](../../../src/test/java/com/umc/product/community/application/service/command/CommunityCommandServiceResidualTest.java#L283) · `@Test` · `없는_신고_대상과_중복_신고를_거부한다` — 없는 신고 대상과 중복 신고를 거부한다

#### CommunityThreadInviteManagerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/command/CommunityThreadInviteManagerTest.java`

1. [L229](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadInviteManagerTest.java#L229) · `@Test` · `invite_activeMemberIsRejected` — 이미 ACTIVE인 멤버가 포함되면 적격성 조회 전에 전체 초대를 거부한다

#### CommunityThreadLifecycleCommandServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/command/CommunityThreadLifecycleCommandServiceTest.java`

1. [L174](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadLifecycleCommandServiceTest.java#L174) · `@Test` · `create_withoutInviteesSkipsInviteFlow` — 초대 없는 생성은 invite manager와 초대 event를 호출하지 않는다
2. [L405](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadLifecycleCommandServiceTest.java#L405) · `@Test` · `update_rejectsMissingThreadAndActor` — 미존재 thread와 미존재 actor는 저장 전에 각각 명시 오류로 거부한다

#### CommunityThreadMembershipCommandServiceTest (6개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/command/CommunityThreadMembershipCommandServiceTest.java`

1. [L320](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadMembershipCommandServiceTest.java#L320) · `@Test` · `changeRole_adminSavesOnlyTarget` — OWNER가 MEMBER를 ADMIN으로 변경하면 대상만 저장한다
2. [L354](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadMembershipCommandServiceTest.java#L354) · `@Test` · `invite_missingThreadRejected` — 존재하지 않는 스레드의 membership 명령은 THREAD_NOT_FOUND로 실패한다
3. [L370](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadMembershipCommandServiceTest.java#L370) · `@Test` · `invite_deletedThreadRejectedBeforeActorLookup` — 삭제된 스레드의 membership 명령은 actor 조회 전에 실패한다
4. [L388](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadMembershipCommandServiceTest.java#L388) · `@Test` · `invite_inactiveActorRejected` — 탈퇴한 actor의 membership 명령은 THREAD_ACCESS_DENIED로 실패한다
5. [L411](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadMembershipCommandServiceTest.java#L411) · `@Test` · `changeRole_inactiveTargetRejected` — 활성 상태가 아닌 역할 변경 대상은 THREAD_MEMBER_NOT_FOUND로 실패한다
6. [L444](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadMembershipCommandServiceTest.java#L444) · `@Test` · `changeRole_invalidTransitionsRejected` — 자기 자신이나 동일 역할로의 변경은 THREAD_INVALID_ROLE_CHANGE로 실패한다

#### CommunityThreadSettingCommandServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/command/CommunityThreadSettingCommandServiceTest.java`

1. [L125](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadSettingCommandServiceTest.java#L125) · `@Test` · `updatesRemainingSettings` — unpin·mute·unmute는 실제 상태 변경 때만 저장하고 lifecycle info를 반환한다
2. [L152](../../../src/test/java/com/umc/product/community/application/service/command/CommunityThreadSettingCommandServiceTest.java#L152) · `@Test` · `rejectsMissingAndDeletedThread` — 미존재 또는 삭제된 thread는 멤버 조회 전에 거부한다

#### CommunityPermissionEvaluatorTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/evaluator/CommunityPermissionEvaluatorTest.java`

1. [L51](../../../src/test/java/com/umc/product/community/application/service/evaluator/CommunityPermissionEvaluatorTest.java#L51) · `@Test` · `community_evaluator는_각_resource_type을_지원한다` — community evaluator는 각 resource type을 지원한다
2. [L58](../../../src/test/java/com/umc/product/community/application/service/evaluator/CommunityPermissionEvaluatorTest.java#L58) · `@Test` · `게시글_READ는_허용하고_EDIT는_작성자_회원에게만_허용한다` — 게시글 READ는 허용하고 EDIT는 작성자 회원에게만 허용한다
3. [L68](../../../src/test/java/com/umc/product/community/application/service/evaluator/CommunityPermissionEvaluatorTest.java#L68) · `@Test` · `게시글_WRITE는_요청자의_challenger_이력을_검사한다` — 게시글 WRITE는 리소스 작성자가 아닌 요청자의 challenger 이력을 검사한다
4. [L80](../../../src/test/java/com/umc/product/community/application/service/evaluator/CommunityPermissionEvaluatorTest.java#L80) · `@Test` · `게시글_DELETE는_작성자_또는_중앙_총괄단에게만_허용한다` — 게시글 DELETE는 작성자 또는 중앙 총괄단에게만 허용한다
5. [L90](../../../src/test/java/com/umc/product/community/application/service/evaluator/CommunityPermissionEvaluatorTest.java#L90) · `@Test` · `댓글_READ는_허용하고_EDIT는_작성자_회원에게만_허용한다` — 댓글 READ는 허용하고 EDIT는 작성자 회원에게만 허용한다
6. [L100](../../../src/test/java/com/umc/product/community/application/service/evaluator/CommunityPermissionEvaluatorTest.java#L100) · `@Test` · `댓글_WRITE는_요청자의_challenger_이력을_검사한다` — 댓글 WRITE는 리소스 작성자가 아닌 요청자의 challenger 이력을 검사한다
7. [L112](../../../src/test/java/com/umc/product/community/application/service/evaluator/CommunityPermissionEvaluatorTest.java#L112) · `@Test` · `댓글_DELETE는_작성자_또는_중앙_총괄단에게만_허용한다` — 댓글 DELETE는 작성자 또는 중앙 총괄단에게만 허용한다
8. [L122](../../../src/test/java/com/umc/product/community/application/service/evaluator/CommunityPermissionEvaluatorTest.java#L122) · `@Test` · `지원하지_않는_permission은_fail_closed로_거부한다` — 지원하지 않는 permission은 fail-closed로 거부한다

#### CommunityThreadMessageCommandServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageCommandServiceTest.java`

1. [L463](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageCommandServiceTest.java#L463) · `@Test` · `create_missingThreadRejectedBeforeMembershipLookup` — 존재하지 않는 thread는 잠금 조회 직후 THREAD_NOT_FOUND로 거절한다
2. [L476](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageCommandServiceTest.java#L476) · `@Test` · `read_missingRoomSummaryFailsClosed` — Chat unread summary가 누락되면 임의의 projection을 저장하지 않고 실패한다
3. [L496](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageCommandServiceTest.java#L496) · `@Test` · `create_mapsEveryCommunityMessageType` — IMAGE와 SYSTEM 생성 명령은 대응하는 Chat content type으로 변환한다

#### CommunityThreadMessageInfoAssemblerTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageInfoAssemblerTest.java`

1. [L140](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageInfoAssemblerTest.java#L140) · `@Test` · `assemble_emptyBatchShortCircuits` — null·빈 batch는 Member 조회 없이 빈 결과를 반환한다
2. [L152](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageInfoAssemblerTest.java#L152) · `@Test` · `assembleForRecipients_preservesOrderAndMasksMissingName` — recipient별 조립은 입력 순서를 보존하고 이름 누락을 안전한 기본값으로 마스킹한다
3. [L169](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageInfoAssemblerTest.java#L169) · `@Test` · `assemble_withoutAnyMemberIdsRejectedByResponseContract` — 발신자 ID가 없는 메시지는 Member batch 조회를 생략하고 응답 계약에서 거절한다
4. [L196](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageInfoAssemblerTest.java#L196) · `@Test` · `assemble_nullContentTypeRejected` — content type 누락은 임의 타입으로 변환하지 않고 명시적으로 실패한다

#### CommunityThreadMessageQueryServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageQueryServiceTest.java`

1. [L226](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageQueryServiceTest.java#L226) · `@Test` · `recipients_emptyBatchShortCircuits` — 빈 recipient batch는 thread와 Chat을 조회하지 않고 빈 결과를 반환한다
2. [L238](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageQueryServiceTest.java#L238) · `@Test` · `recipients_validatesAllMembersBeforeChatQuery` — recipient batch는 모든 ACTIVE membership을 검증한 뒤 Chat viewer 결과를 조립한다
3. [L285](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageQueryServiceTest.java#L285) · `@Test` · `recipients_inactiveMemberRejectsWholeBatch` — recipient 중 하나라도 비활성이면 전체 viewer 조회를 fail-closed한다
4. [L311](../../../src/test/java/com/umc/product/community/application/service/message/CommunityThreadMessageQueryServiceTest.java#L311) · `@Test` · `single_missingThreadRejected` — 존재하지 않는 thread는 membership과 Chat 조회 전에 THREAD_NOT_FOUND로 실패한다

#### CommentQueryServiceTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/query/CommentQueryServiceTest.java`

1. [L48](../../../src/test/java/com/umc/product/community/application/service/query/CommentQueryServiceTest.java#L48) · `@Test` · `없는_게시글의_댓글_목록_조회를_거부한다` — 없는 게시글의 댓글 목록 조회를 거부한다
2. [L58](../../../src/test/java/com/umc/product/community/application/service/query/CommentQueryServiceTest.java#L58) · `@Test` · `댓글이_없으면_작성자_조회_없이_빈_목록을_반환한다` — 댓글이 없으면 작성자 조회 없이 빈 목록을 반환한다
3. [L69](../../../src/test/java/com/umc/product/community/application/service/query/CommentQueryServiceTest.java#L69) · `@Test` · `댓글_목록은_부분_작성자_데이터를_허용하고_현재_challenger의_isAuthor를_표시한다` — 댓글 목록은 부분 작성자 데이터를 허용하고 현재 challenger의 isAuthor를 표시한다
4. [L90](../../../src/test/java/com/umc/product/community/application/service/query/CommentQueryServiceTest.java#L90) · `@Test` · `단일_댓글은_challenger와_member_작성자_정보를_결합한다` — 단일 댓글은 challenger와 member 작성자 정보를 결합한다
5. [L104](../../../src/test/java/com/umc/product/community/application/service/query/CommentQueryServiceTest.java#L104) · `@Test` · `단일_댓글의_member_정보가_없으면_안전한_기본_작성자_정보를_사용한다` — 단일 댓글의 member 정보가 없으면 안전한 기본 작성자 정보를 사용한다
6. [L117](../../../src/test/java/com/umc/product/community/application/service/query/CommentQueryServiceTest.java#L117) · `@Test` · `없는_단일_댓글_조회는_not_found로_실패한다` — 없는 단일 댓글 조회는 not-found로 실패한다

#### CommunityThreadListDetailQueryServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/query/CommunityThreadListDetailQueryServiceTest.java`

1. [L228](../../../src/test/java/com/umc/product/community/application/service/query/CommunityThreadListDetailQueryServiceTest.java#L228) · `@Test` · `listThreads_카테고리_filter를_모두_변환한다` — 카테고리 filter 네 종류를 persistence category로 정확히 변환한다

#### CommunityThreadMemberInvitableQueryServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/query/CommunityThreadMemberInvitableQueryServiceTest.java`

1. [L226](../../../src/test/java/com/umc/product/community/application/service/query/CommunityThreadMemberInvitableQueryServiceTest.java#L226) · `@Test` · `getMembersByIds_중복_row와_빈_challenger_이력을_안전하게_처리한다` — 중복 ACTIVE row는 첫 row를 사용하고 Challenger 이력 없는 멤버도 조립한다
2. [L252](../../../src/test/java/com/umc/product/community/application/service/query/CommunityThreadMemberInvitableQueryServiceTest.java#L252) · `@Test` · `memberQueries_emptyBatchShortCircuits` — 빈 멤버 목록과 빈 지정 ID batch는 외부 read model 조회 없이 빈 결과를 반환한다
3. [L273](../../../src/test/java/com/umc/product/community/application/service/query/CommunityThreadMemberInvitableQueryServiceTest.java#L273) · `@Test` · `listMembers_selectsLatestChallengerDeterministically` — 여러 Challenger 이력에서는 generation·gisu·challenger ID 순으로 최신 이력을 선택한다

#### PostQueryServiceTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/query/PostQueryServiceTest.java`

1. [L55](../../../src/test/java/com/umc/product/community/application/service/query/PostQueryServiceTest.java#L55) · `@Test` · `게시글_상세는_작성자_challenger와_member_정보를_결합한다` — 게시글 상세는 작성자 challenger와 member 정보를 결합한다
2. [L68](../../../src/test/java/com/umc/product/community/application/service/query/PostQueryServiceTest.java#L68) · `@Test` · `없는_게시글_상세_조회는_not_found로_실패한다` — 없는 게시글 상세 조회는 not-found로 실패한다
3. [L77](../../../src/test/java/com/umc/product/community/application/service/query/PostQueryServiceTest.java#L77) · `@Test` · `viewer_게시글_상세는_좋아요_댓글_스크랩_interaction을_결합한다` — viewer 게시글 상세는 좋아요·댓글·스크랩 interaction을 결합한다
4. [L96](../../../src/test/java/com/umc/product/community/application/service/query/PostQueryServiceTest.java#L96) · `@Test` · `없는_viewer_게시글_상세_조회는_not_found로_실패한다` — 없는 viewer 게시글 상세 조회는 not-found로 실패한다
5. [L105](../../../src/test/java/com/umc/product/community/application/service/query/PostQueryServiceTest.java#L105) · `@Test` · `게시글_목록은_빈_page를_동일_pageable의_빈_결과로_반환한다` — 게시글 목록은 빈 page를 동일 pageable의 빈 결과로 반환한다
6. [L118](../../../src/test/java/com/umc/product/community/application/service/query/PostQueryServiceTest.java#L118) · `@Test` · `게시글_목록은_batch_작성자_데이터_누락과_댓글_기본값을_안전하게_처리한다` — 게시글 목록은 batch 작성자 데이터 누락과 댓글 기본값을 안전하게 처리한다
7. [L148](../../../src/test/java/com/umc/product/community/application/service/query/PostQueryServiceTest.java#L148) · `@Test` · `검색_결과는_각_게시글_작성자_정보를_결합한다` — 검색 결과는 각 게시글 작성자 정보를 결합한다
8. [L165](../../../src/test/java/com/umc/product/community/application/service/query/PostQueryServiceTest.java#L165) · `@Test` · `내_작성_댓글_스크랩_목록은_최신_활성_challenger_ID로_조회한다` — 내 작성·댓글·스크랩 목록은 최신 활성 challenger ID로 조회한다

#### CommunityThreadChatRealtimeRelayTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadChatRealtimeRelayTest.java`

1. [L191](../../../src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadChatRealtimeRelayTest.java#L191) · `@Test` · `messageUpdatedAndDeletedUsePersonalizedSnapshots` — message.updated와 message.deleted는 수신자별 최신 snapshot을 대응 payload로 전송한다
2. [L234](../../../src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadChatRealtimeRelayTest.java#L234) · `@Test` · `readUpdatedUsesOneStableEnvelope` — read.updated는 한 번 만든 동일 event를 delivery-time ACTIVE audience에 전송한다

#### CommunityThreadLifecycleRealtimeRelayTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadLifecycleRealtimeRelayTest.java`

1. [L243](../../../src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadLifecycleRealtimeRelayTest.java#L243) · `@Test` · `threadDeletedUsesPreDeleteAudience` — thread.deleted는 삭제 전 ACTIVE snapshot 전체에 terminal event를 전송한다

#### CommunityThreadRealtimeDeliveryTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadRealtimeDeliveryTest.java`

1. [L116](../../../src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadRealtimeDeliveryTest.java#L116) · `@Test` · `eventFactoryFailureAttemptsRemainingRecipients` — event factory 실패도 다음 recipient 전송을 계속한 뒤 원인을 집계한다
2. [L141](../../../src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadRealtimeDeliveryTest.java#L141) · `@Test` · `terminalAudience_requiresAffectedMember` — terminal audience에 대상 멤버가 없으면 잘못된 snapshot으로 판단한다
3. [L151](../../../src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadRealtimeDeliveryTest.java#L151) · `@Test` · `audience_rejectsInvalidMemberAndCapacityOverflow` — fan-out audience는 비양수 member ID와 설정 용량 초과를 거절한다

#### CommunityThreadRealtimeFanOutServiceTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadRealtimeFanOutServiceTest.java`

1. [L23](../../../src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadRealtimeFanOutServiceTest.java#L23) · `@Test` · `relay_delegatesEverySupportedEventType` — 모든 Chat·Community event 유형을 전용 relay에 그대로 위임한다

#### CommunityThreadRealtimeMetricsTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadRealtimeMetricsTest.java`

1. [L58](../../../src/test/java/com/umc/product/community/application/service/realtime/CommunityThreadRealtimeMetricsTest.java#L58) · `@Test` · `negativeFanOutRecipientCountRejected` — 음수 fan-out recipient 수는 metric을 기록하지 않고 거절한다

#### CommunityThreadMessageReportQueryServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/application/service/report/CommunityThreadMessageReportQueryServiceTest.java`

1. [L166](../../../src/test/java/com/umc/product/community/application/service/report/CommunityThreadMessageReportQueryServiceTest.java#L166) · `@Test` · `search_서비스_페이지_경계를_재검증한다` — 서비스 경계도 음수 offset과 허용 범위 밖 limit을 방어한다

### Contract / Misc

#### CommunityDtoResidualTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/CommunityDtoResidualTest.java`

1. [L35](../../../src/test/java/com/umc/product/community/CommunityDtoResidualTest.java#L35) · `@Test` · `PostInfo는_일반글_작성자_정보와_null_작성자_정보를_모두_변환한다` — PostInfo는 일반글 작성자 정보와 null 작성자 정보를 모두 변환한다
2. [L54](../../../src/test/java/com/umc/product/community/CommunityDtoResidualTest.java#L54) · `@Test` · `PostInfo의_현재_legacy_변환은_번개_정보를_보존한다` — PostInfo의 현재·legacy 변환은 번개 정보를 보존한다
3. [L70](../../../src/test/java/com/umc/product/community/CommunityDtoResidualTest.java#L70) · `@Test` · `CommentInfo는_현재_legacy_작성자_변환과_null_교차_도메인_정보를_처리한다` — CommentInfo는 현재·legacy 작성자 변환과 null 교차 도메인 정보를 처리한다
4. [L92](../../../src/test/java/com/umc/product/community/CommunityDtoResidualTest.java#L92) · `@Test` · `검색_DTO는_relevance와_match_type을_보존하고_긴_본문만_100자로_자른다` — 검색 DTO는 relevance와 match type을 보존하고 긴 본문만 100자로 자른다
5. [L110](../../../src/test/java/com/umc/product/community/CommunityDtoResidualTest.java#L110) · `@Test` · `PostResponse는_현재_검색_legacy_번개_응답의_작성자와_번개_정보를_변환한다` — PostResponse는 현재 검색·legacy 번개 응답의 작성자와 번개 정보를 변환한다
6. [L132](../../../src/test/java/com/umc/product/community/CommunityDtoResidualTest.java#L132) · `@Test` · `PostDetailResponse는_번개_상세_정보와_interaction_집계를_변환한다` — PostDetailResponse는 번개 상세 정보와 interaction 집계를 변환한다
7. [L146](../../../src/test/java/com/umc/product/community/CommunityDtoResidualTest.java#L146) · `@Test` · `community_command는_null_필수값과_blank_댓글을_거부한다` — community command는 null 필수값과 blank 댓글을 거부한다

#### CommunityThreadPageAndReportDtoEdgeCaseTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/CommunityThreadPageAndReportDtoEdgeCaseTest.java`

1. [L22](../../../src/test/java/com/umc/product/community/CommunityThreadPageAndReportDtoEdgeCaseTest.java#L22) · `@Test` · `validatesAdminReportQuery` — admin report query는 requester·optional filter·page 양수 계약을 검증한다
2. [L43](../../../src/test/java/com/umc/product/community/CommunityThreadPageAndReportDtoEdgeCaseTest.java#L43) · `@Test` · `validatesPersistenceReportQuery` — persistence report query는 optional ID와 offset·limit 경계를 검증한다
3. [L59](../../../src/test/java/com/umc/product/community/CommunityThreadPageAndReportDtoEdgeCaseTest.java#L59) · `@Test` · `validatesPageResults` — report와 thread row page는 null collection을 비우고 음수 total을 거부한다
4. [L74](../../../src/test/java/com/umc/product/community/CommunityThreadPageAndReportDtoEdgeCaseTest.java#L74) · `@Test` · `validatesReportCommand` — 메시지 신고 command는 양수 ID와 필수 사유를 강제한다

### Domain

#### CommunityDomainResidualTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/domain/CommunityDomainResidualTest.java`

1. [L22](../../../src/test/java/com/umc/product/community/domain/CommunityDomainResidualTest.java#L22) · `@Test` · `일반_게시글은_번개_카테고리와_빈_필수값을_거부한다` — 일반 게시글은 번개 카테고리와 빈 필수값을 거부한다
2. [L36](../../../src/test/java/com/umc/product/community/domain/CommunityDomainResidualTest.java#L36) · `@Test` · `번개_게시글은_필수_번개_정보와_게시글_종류_전이를_강제한다` — 번개 게시글은 필수 번개 정보와 게시글 종류 전이를 강제한다
3. [L58](../../../src/test/java/com/umc/product/community/domain/CommunityDomainResidualTest.java#L58) · `@Test` · `번개_정보는_시간_장소_인원_URL_불변식과_과거_시간_경계를_검증한다` — 번개 정보는 시간·장소·인원·URL 불변식과 과거 시간 경계를 검증한다
4. [L75](../../../src/test/java/com/umc/product/community/domain/CommunityDomainResidualTest.java#L75) · `@Test` · `댓글_수정과_좋아요_toggle은_빈_내용과_멱등_상태를_처리한다` — 댓글 수정과 좋아요 toggle은 빈 내용과 멱등 상태를 처리한다
5. [L88](../../../src/test/java/com/umc/product/community/domain/CommunityDomainResidualTest.java#L88) · `@Test` · `댓글은_양수_challenger_ID와_non_blank_내용을_요구한다` — 댓글은 양수 challenger ID와 non-blank 내용을 요구한다
6. [L99](../../../src/test/java/com/umc/product/community/domain/CommunityDomainResidualTest.java#L99) · `@Test` · `신고는_PENDING으로_생성되고_승인_또는_거절할_수_있다` — 신고는 PENDING으로 생성되고 승인 또는 거절할 수 있다
7. [L113](../../../src/test/java/com/umc/product/community/domain/CommunityDomainResidualTest.java#L113) · `@Test` · `스크랩은_양수_challenger_ID를_요구한다` — 스크랩은 양수 challenger ID를 요구한다
8. [L120](../../../src/test/java/com/umc/product/community/domain/CommunityDomainResidualTest.java#L120) · `@Test` · `community_예외는_custom_message를_보존한다` — community 예외는 custom message를 보존한다

#### CommunityThreadMemberTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/domain/CommunityThreadMemberTest.java`

1. [L119](../../../src/test/java/com/umc/product/community/domain/CommunityThreadMemberTest.java#L119) · `@Test` · `pinAndMuteAreIdempotent` — pin·mute 전이는 멱등이고 반대 전이는 실제 상태 변경만 보고한다
2. [L139](../../../src/test/java/com/umc/product/community/domain/CommunityThreadMemberTest.java#L139) · `@Test` · `rejectsInvalidStateAndIdentityTransitions` — 비활성 재전이·null role·non-positive ID를 거부한다

#### CommunityThreadPropertiesTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/domain/CommunityThreadPropertiesTest.java`

1. [L56](../../../src/test/java/com/umc/product/community/domain/CommunityThreadPropertiesTest.java#L56) · `@Test` · `remainingCapacityIsClamped` — 남은 정원은 0 아래로 내려가지 않고 음수 active count는 거부한다

#### CommunityThreadTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/domain/CommunityThreadTest.java`

1. [L152](../../../src/test/java/com/umc/product/community/domain/CommunityThreadTest.java#L152) · `@Test` · `ignoresStaleProjectionUpdates` — 오래된 message·activity projection은 무시하고 최신 시각만 반영한다
2. [L171](../../../src/test/java/com/umc/product/community/domain/CommunityThreadTest.java#L171) · `@Test` · `validatesMetadataAndActivity` — metadata 수정은 정규화하며 null category와 null activity를 거부한다

#### CommunityThreadLifecycleEventTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/domain/event/CommunityThreadLifecycleEventTest.java`

1. [L68](../../../src/test/java/com/umc/product/community/domain/event/CommunityThreadLifecycleEventTest.java#L68) · `@Test` · `exposesLifecycleEventTypes` — delete·kick·update lifecycle event는 타입과 정렬된 recipient snapshot을 보존한다
2. [L84](../../../src/test/java/com/umc/product/community/domain/event/CommunityThreadLifecycleEventTest.java#L84) · `@Test` · `rejectsInvalidLifecycleEventIdentity` — lifecycle event는 null snapshot과 non-positive ID를 거부한다

#### ReportTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/domain/ReportTest.java`

1. [L47](../../../src/test/java/com/umc/product/community/domain/ReportTest.java#L47) · `@Test` · `rejectsInvalidThreadMessageReports` — 일반 factory의 THREAD_MESSAGE 사용과 thread report 필수값 누락을 거부한다
2. [L62](../../../src/test/java/com/umc/product/community/domain/ReportTest.java#L62) · `@Test` · `transitionsReviewStatus` — 신고 검토 결과는 approve와 reject 상태로 명시적으로 전이한다

### Persistence

#### CommunityPersistenceAdapterUnitTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/out/persistence/CommunityPersistenceAdapterUnitTest.java`

1. [L33](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityPersistenceAdapterUnitTest.java#L33) · `@Test` · `Post_adapter는_조회_검색_목록_port를_각_repository에_위임한다` — Post adapter는 조회·검색·목록 port를 각 repository에 위임한다
2. [L59](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityPersistenceAdapterUnitTest.java#L59) · `@Test` · `Post_adapter는_ID_조회_저장_삭제와_작성자_조회_계약을_보존한다` — Post adapter는 ID 조회·저장·삭제와 작성자 조회 계약을 보존한다
3. [L85](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityPersistenceAdapterUnitTest.java#L85) · `@Test` · `Post_adapter는_transient_삭제를_무시하고_없는_좋아요_작성자_조회를_거부한다` — Post adapter는 transient 삭제를 무시하고 없는 좋아요·작성자 조회를 거부한다
4. [L99](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityPersistenceAdapterUnitTest.java#L99) · `@Test` · `Comment_adapter는_빈_batch와_count_row를_map으로_변환한다` — Comment adapter는 빈 batch와 count row를 map으로 변환한다
5. [L112](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityPersistenceAdapterUnitTest.java#L112) · `@Test` · `Comment_adapter는_transient_삭제를_무시하고_없는_댓글_좋아요를_거부한다` — Comment adapter는 transient 삭제를 무시하고 없는 댓글 좋아요를 거부한다
6. [L128](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityPersistenceAdapterUnitTest.java#L128) · `@Test` · `Scrap_adapter는_복합_key_삭제와_Post_전체_삭제를_위임한다` — Scrap adapter는 복합 key 삭제와 Post 전체 삭제를 위임한다
7. [L141](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityPersistenceAdapterUnitTest.java#L141) · `@Test` · `Report_adapter는_중복_확인과_저장을_repository에_위임한다` — Report adapter는 중복 확인과 저장을 repository에 위임한다
8. [L155](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityPersistenceAdapterUnitTest.java#L155) · `@Test` · `PostRepository_default_map_변환은_projection을_ID_map으로_바꾼다` — PostRepository default map 변환은 projection을 ID map으로 바꾼다

#### CommunityThreadPersistenceAdapterUnitTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/out/persistence/CommunityThreadPersistenceAdapterUnitTest.java`

1. [L35](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityThreadPersistenceAdapterUnitTest.java#L35) · `@Test` · `delegatesQueryOperations` — 검색과 초대 차단 ID 조회를 Query repository에 그대로 위임한다
2. [L47](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityThreadPersistenceAdapterUnitTest.java#L47) · `@Test` · `transferOwnership_requiresExactlyOneDemotedOwner` — 소유권 이전에서 기존 OWNER가 정확히 한 명 갱신되지 않으면 승격하지 않는다
3. [L59](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityThreadPersistenceAdapterUnitTest.java#L59) · `@Test` · `transferOwnership_requiresExactlyOnePromotedOwner` — 소유권 이전에서 새 OWNER가 정확히 한 명 갱신되지 않으면 실패한다

#### CommunityThreadQueryRepositoryTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/out/persistence/CommunityThreadQueryRepositoryTest.java`

1. [L72](../../../src/test/java/com/umc/product/community/adapter/out/persistence/CommunityThreadQueryRepositoryTest.java#L72) · `@Test` · `listActiveMemberIds_비양수_limit을_거절한다` — 팬아웃 조회 limit은 양수만 허용한다

#### PostQueryRepositoryTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/out/persistence/PostQueryRepositoryTest.java`

1. [L172](../../../src/test/java/com/umc/product/community/adapter/out/persistence/PostQueryRepositoryTest.java#L172) · `@Test` · `필터_키워드_댓글_스크랩이_비어_있으면_동일_pageable의_빈_결과를_반환한다` — 필터·키워드·댓글·스크랩이 비어 있으면 동일 pageable의 빈 결과를 반환한다

### REST / Web

#### CommunityControllerUnitTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/web/CommunityControllerUnitTest.java`

1. [L50](../../../src/test/java/com/umc/product/community/adapter/in/web/CommunityControllerUnitTest.java#L50) · `@Test` · `Post_controller는_번개_생성_수정_요청을_command로_변환한다` — Post controller는 번개 생성·수정 요청을 command로 변환한다
2. [L97](../../../src/test/java/com/umc/product/community/adapter/in/web/CommunityControllerUnitTest.java#L97) · `@Test` · `Comment_controller는_현재_challenger_ID로_댓글을_삭제한다` — Comment controller는 현재 challenger ID로 댓글을 삭제한다
3. [L118](../../../src/test/java/com/umc/product/community/adapter/in/web/CommunityControllerUnitTest.java#L118) · `@Test` · `Report_controller는_현재_member_ID로_게시글과_댓글을_신고한다` — Report controller는 현재 member ID로 게시글과 댓글을 신고한다
4. [L133](../../../src/test/java/com/umc/product/community/adapter/in/web/CommunityControllerUnitTest.java#L133) · `@Test` · `Post_query_controller는_상세_목록_검색_내_활동_page를_응답으로_변환한다` — Post query controller는 상세·목록·검색·내 활동 page를 응답으로 변환한다

#### CommunityThreadFilterParserTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/web/CommunityThreadFilterParserTest.java`

1. [L14](../../../src/test/java/com/umc/product/community/adapter/in/web/CommunityThreadFilterParserTest.java#L14) · `@Test` · `parsesSupportedValuesAndRejectsUnknownValue` — 지원 filter wire value를 매핑하고 알 수 없는 값은 거절한다

#### CommunityRequestResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/web/dto/CommunityRequestResidualTest.java`

1. [L21](../../../src/test/java/com/umc/product/community/adapter/in/web/dto/CommunityRequestResidualTest.java#L21) · `@Test` · `일반_게시글_요청은_command로_변환하고_잘못된_값을_거부한다` — 일반 게시글 요청은 command로 변환하고 blank와 번개 category를 거부한다
2. [L34](../../../src/test/java/com/umc/product/community/adapter/in/web/dto/CommunityRequestResidualTest.java#L34) · `@Test` · `일반_게시글_수정_요청은_command로_변환하고_blank를_거부한다` — 일반 게시글 수정 요청은 command로 변환하고 blank를 거부한다
3. [L45](../../../src/test/java/com/umc/product/community/adapter/in/web/dto/CommunityRequestResidualTest.java#L45) · `@Test` · `번개_생성_요청은_모든_필드를_command로_변환한다` — 번개 생성 요청은 모든 필드를 command로 변환한다
4. [L55](../../../src/test/java/com/umc/product/community/adapter/in/web/dto/CommunityRequestResidualTest.java#L55) · `@Test` · `번개_생성_요청은_blank_URL_형식_인원_경계를_거부한다` — 번개 생성 요청은 blank·URL 형식·인원 경계를 거부한다
5. [L72](../../../src/test/java/com/umc/product/community/adapter/in/web/dto/CommunityRequestResidualTest.java#L72) · `@Test` · `번개_수정_요청은_모든_필드를_command로_변환한다` — 번개 수정 요청은 모든 필드를 command로 변환한다
6. [L82](../../../src/test/java/com/umc/product/community/adapter/in/web/dto/CommunityRequestResidualTest.java#L82) · `@Test` · `번개_수정_요청은_blank_URL_형식_인원_경계를_거부한다` — 번개 수정 요청은 blank·URL 형식·인원 경계를 거부한다

#### UpdateCommunityThreadRequestTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/web/dto/request/UpdateCommunityThreadRequestTest.java`

1. [L14](../../../src/test/java/com/umc/product/community/adapter/in/web/dto/request/UpdateCommunityThreadRequestTest.java#L14) · `@Test` · `exposesPresentFieldsAndConvertsToCommand` — 명시된 patch 필드를 조회하고 command로 그대로 변환한다
2. [L36](../../../src/test/java/com/umc/product/community/adapter/in/web/dto/request/UpdateCommunityThreadRequestTest.java#L36) · `@Test` · `rejectsInvalidIconBoundaries` — blank icon과 단일 grapheme이 아닌 icon은 patch validation에서 거절한다

### WebSocket / STOMP

#### CommunityStompClientMessageIdResolverTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompClientMessageIdResolverTest.java`

1. [L47](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompClientMessageIdResolverTest.java#L47) · `@Test` · `resolvesTypedAndStringPayloads` — typed request와 String JSON에서도 clientMessageId를 복원한다
2. [L64](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompClientMessageIdResolverTest.java#L64) · `@Test` · `rejectsUnsupportedAndMalformedPayloads` — null·미지원 payload·잘못된 JSON·비문자 필드는 빈 결과를 반환한다

#### CommunityStompCommandSupportTest (5개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompCommandSupportTest.java`

1. [L135](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompCommandSupportTest.java#L135) · `@ParameterizedTest` · `acknowledgesEveryCommandType` — 모든 command type은 대응 operation으로 성공 metric과 ACK를 기록한다 · @EnumSource
2. [L156](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompCommandSupportTest.java#L156) · `@Test` · `rejectsInvalidCommandContext` — 인증 타입·principal·member ID·command ID가 잘못되면 context 생성을 거부한다
3. [L183](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompCommandSupportTest.java#L183) · `@Test` · `normalizesBusinessExceptionReasons` — BusinessException HTTP status를 저카디널리티 reject reason으로 정규화한다
4. [L195](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompCommandSupportTest.java#L195) · `@Test` · `normalizesTechnicalFailureReasons` — 변환·validation 예외 원인은 VALIDATION, 알 수 없거나 순환 cause는 UNKNOWN으로 기록한다
5. [L212](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompCommandSupportTest.java#L212) · `@Test` · `handlesMessageExceptionWithAndWithoutDestination` — message exception handler는 destination이 있으면 실패 metric을 남기고 항상 오류 mapper에 위임한다

#### CommunityStompErrorMapperTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompErrorMapperTest.java`

1. [L42](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompErrorMapperTest.java#L42) · `@Test` · `rejectsMissingPrincipal` — principal이 없으면 사용자별 오류를 발행하지 않고 즉시 거부한다
2. [L51](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompErrorMapperTest.java#L51) · `@Test` · `preservesCommandCorrelationFromTypedPayload` — destination과 command ID가 유효하면 command 및 client message 상관관계를 보존한다
3. [L74](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompErrorMapperTest.java#L74) · `@Test` · `extractsClientMessageIdWithoutStompHeaders` — 헤더가 없으면 raw JSON의 canonical clientMessageId만 복원해 오류를 발행한다
4. [L91](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompErrorMapperTest.java#L91) · `@Test` · `mapsValidationFailuresToBadRequest` — validation 계열 예외는 원인 체인을 따라 BAD_REQUEST로 정규화한다
5. [L110](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompErrorMapperTest.java#L110) · `@Test` · `mapsUnknownAndCyclicFailuresToInternalServerError` — 알 수 없는 예외와 순환 cause는 INTERNAL_SERVER_ERROR이며 재시도 가능하다

#### CommunityStompRequestTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompRequestTest.java`

1. [L89](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompRequestTest.java#L89) · `@Test` · `validatesCreateMessageEdgeCases` — message create는 파일·mention·reply·길이·type 경계를 모두 검증한다
2. [L144](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompRequestTest.java#L144) · `@Test` · `validatesOtherRequestContracts` — request DTO는 unknown field와 emoji·content 경계를 fail-fast로 거부한다

#### CommunityStompSendAuthorizerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompSendAuthorizerTest.java`

1. [L123](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompSendAuthorizerTest.java#L123) · `@ParameterizedTest` · `rejectsEveryCommandWhenLookupFails` — 모든 SEND command는 접근 조회 실패 시 fail-closed로 거부한다 · @ValueSource
2. [L140](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityStompSendAuthorizerTest.java#L140) · `@Test` · `rejectsNonPositiveMemberId` — 0 이하 member ID도 조회 없이 거부하고 command별 metric을 남긴다

#### CommunityThreadOutboxProbeTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/websocket/CommunityThreadOutboxProbeTest.java`

1. [L67](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityThreadOutboxProbeTest.java#L67) · `@Test` · `findsCommittedChatMessageByAcknowledgedMessageId` — ACK의 messageId와 일치하는 commit된 채팅 메시지 payload row를 찾는다

#### CommunityThreadStompControllerTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/community/adapter/in/websocket/CommunityThreadStompControllerTest.java`

1. [L169](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityThreadStompControllerTest.java#L169) · `@Test` · `editMessageAcknowledgesMutationResult` — message edit 성공은 수정 message ID와 deduplication을 ACK에 보존한다
2. [L195](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityThreadStompControllerTest.java#L195) · `@Test` · `delegatesRemainingCommandsAndAcknowledges` — delete·reaction·read 성공은 인증 member command와 command별 ACK로 변환한다
3. [L252](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityThreadStompControllerTest.java#L252) · `@Test` · `rejectsFailuresForEveryRemainingCommand` — 각 command application 실패는 command별 correlation을 보존해 reject한다
4. [L301](../../../src/test/java/com/umc/product/community/adapter/in/websocket/CommunityThreadStompControllerTest.java#L301) · `@Test` · `delegatesMessageException` — message exception handler는 공통 command support에 원문을 위임한다

## 3.14 `feedback` — 20개

### Application Service

#### UserFeedbackResponseCommandServiceTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/feedback/application/service/command/UserFeedbackResponseCommandServiceTest.java`

1. [L33](../../../src/test/java/com/umc/product/feedback/application/service/command/UserFeedbackResponseCommandServiceTest.java#L33) · `@Test` · `피드백_템플릿의_Form에_응답을_즉시_제출한다` — 피드백 템플릿의 Form에 응답을 즉시 제출한다

#### UserFeedbackTemplateQueryServiceTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/feedback/application/service/query/UserFeedbackTemplateQueryServiceTest.java`

1. [L52](../../../src/test/java/com/umc/product/feedback/application/service/query/UserFeedbackTemplateQueryServiceTest.java#L52) · `@Test` · `활성_기수가_없으면_피드백_대상_분류를_수행하지_않는다` — 활성 기수가 없으면 피드백 대상 분류를 수행하지 않는다
2. [L61](../../../src/test/java/com/umc/product/feedback/application/service/query/UserFeedbackTemplateQueryServiceTest.java#L61) · `@Test` · `현재_기수_중앙_운영진은_ADMIN_템플릿을_조회한다` — 현재 기수 중앙 운영진은 challenger 이력과 무관하게 ADMIN 템플릿을 조회한다
3. [L74](../../../src/test/java/com/umc/product/feedback/application/service/query/UserFeedbackTemplateQueryServiceTest.java#L74) · `@Test` · `현재_기수_challenger가_아니면_템플릿을_반환하지_않는다` — 현재 기수 challenger가 아니면 템플릿을 반환하지 않는다
4. [L87](../../../src/test/java/com/umc/product/feedback/application/service/query/UserFeedbackTemplateQueryServiceTest.java#L87) · `@Test` · `이전_기수_이력이_있으면_EXPERIENCED_CHALLENGER로_분류한다` — 이전 기수 이력이 있으면 EXPERIENCED_CHALLENGER로 분류한다
5. [L102](../../../src/test/java/com/umc/product/feedback/application/service/query/UserFeedbackTemplateQueryServiceTest.java#L102) · `@Test` · `십기_PM은_이전_기수_이력이_없어도_EXPERIENCED_CHALLENGER로_분류한다` — 10기 PM은 이전 기수 이력이 없어도 EXPERIENCED_CHALLENGER로 분류한다
6. [L115](../../../src/test/java/com/umc/product/feedback/application/service/query/UserFeedbackTemplateQueryServiceTest.java#L115) · `@Test` · `십기_비PM_신규_challenger는_NEW_CHALLENGER로_분류한다` — 10기 비PM 신규 challenger는 NEW_CHALLENGER로 분류한다
7. [L128](../../../src/test/java/com/umc/product/feedback/application/service/query/UserFeedbackTemplateQueryServiceTest.java#L128) · `@Test` · `십기가_아닌_신규_challenger는_NEW_CHALLENGER로_분류한다` — 10기가 아닌 신규 challenger는 파트와 무관하게 NEW_CHALLENGER로 분류한다
8. [L141](../../../src/test/java/com/umc/product/feedback/application/service/query/UserFeedbackTemplateQueryServiceTest.java#L141) · `@Test` · `분류에_맞는_활성_템플릿이_없으면_Form을_조회하지_않는다` — 분류에 맞는 활성 템플릿이 없으면 Form을 조회하지 않는다

### Contract / Misc

#### FeedbackDtoTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/feedback/FeedbackDtoTest.java`

1. [L24](../../../src/test/java/com/umc/product/feedback/FeedbackDtoTest.java#L24) · `@Test` · `피드백_템플릿은_생성_시_활성_상태와_참조_Form을_보존한다` — 피드백 템플릿은 생성 시 활성 상태와 참조 Form을 보존한다
2. [L39](../../../src/test/java/com/umc/product/feedback/FeedbackDtoTest.java#L39) · `@Test` · `제출_요청은_모든_answer_값을_command로_손실_없이_변환한다` — 제출 요청은 모든 answer 값을 command로 손실 없이 변환한다
3. [L64](../../../src/test/java/com/umc/product/feedback/FeedbackDtoTest.java#L64) · `@Test` · `Form_중첩_구조를_피드백_응답_구조로_변환한다` — Form 중첩 구조를 피드백 응답 구조로 변환한다
4. [L90](../../../src/test/java/com/umc/product/feedback/FeedbackDtoTest.java#L90) · `@Test` · `제출_응답과_feedback_예외_code_계약을_보존한다` — 제출 응답과 feedback 예외 code 계약을 보존한다

### Persistence

#### UserFeedbackTemplatePersistenceAdapterTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/feedback/adapter/out/persistence/UserFeedbackTemplatePersistenceAdapterTest.java`

1. [L26](../../../src/test/java/com/umc/product/feedback/adapter/out/persistence/UserFeedbackTemplatePersistenceAdapterTest.java#L26) · `@Test` · `context와_target_type에_맞는_활성_템플릿을_조회한다` — context와 target type에 맞는 활성 템플릿을 조회한다
2. [L41](../../../src/test/java/com/umc/product/feedback/adapter/out/persistence/UserFeedbackTemplatePersistenceAdapterTest.java#L41) · `@Test` · `필수_템플릿을_ID로_조회한다` — 필수 템플릿을 ID로 조회한다
3. [L50](../../../src/test/java/com/umc/product/feedback/adapter/out/persistence/UserFeedbackTemplatePersistenceAdapterTest.java#L50) · `@Test` · `필수_템플릿이_없으면_domain_not_found_예외를_던진다` — 필수 템플릿이 없으면 domain not-found 예외를 던진다

### REST / Web

#### UserFeedbackControllerTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/feedback/adapter/in/web/UserFeedbackControllerTest.java`

1. [L72](../../../src/test/java/com/umc/product/feedback/adapter/in/web/UserFeedbackControllerTest.java#L72) · `@Test` · `분류된_피드백_템플릿을_현재_회원에게_반환한다` — 분류된 피드백 템플릿을 현재 회원에게 반환한다
2. [L90](../../../src/test/java/com/umc/product/feedback/adapter/in/web/UserFeedbackControllerTest.java#L90) · `@Test` · `활성_피드백_템플릿이_없으면_null_result를_반환한다` — 활성 피드백 템플릿이 없으면 null result를 반환한다
3. [L102](../../../src/test/java/com/umc/product/feedback/adapter/in/web/UserFeedbackControllerTest.java#L102) · `@Test` · `피드백_답변을_현재_회원의_제출_command로_변환한다` — 피드백 답변을 현재 회원의 제출 command로 변환한다
4. [L137](../../../src/test/java/com/umc/product/feedback/adapter/in/web/UserFeedbackControllerTest.java#L137) · `@Test` · `피드백_answers가_비어_있으면_제출하지_않는다` — 피드백 answers가 비어 있으면 제출하지 않는다

## 3.15 `notice` — 81개

### Application Service

#### NoticeContentServiceTest (9개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java`

1. [L87](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java#L87) · `@Test` · `투표_추가는_작성자와_중복을_검증한다` — 투표 추가는 작성자와 중복을 검증하고 form vote와 notice vote ID를 반환한다
2. [L110](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java#L110) · `@Test` · `투표_추가는_공지와_작성자를_검증한다` — 투표 추가는 미존재 공지와 다른 작성자를 거부한다
3. [L120](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java#L120) · `@Test` · `이미지_추가는_입력과_상한을_검증한다` — 이미지 추가는 빈 입력과 누적 10장 초과를 거부한다
4. [L133](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java#L133) · `@Test` · `이미지_추가는_순서대로_저장한다` — 이미지 추가는 다음 표시 순서부터 저장하고 생성 ID를 반환한다
5. [L155](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java#L155) · `@Test` · `링크_추가는_입력과_순서를_검증한다` — 링크 추가는 빈 입력을 거부하고 다음 표시 순서부터 저장한다
6. [L173](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java#L173) · `@Test` · `투표_삭제는_두_저장소를_동기화한다` — 투표 삭제는 notice vote와 form vote를 함께 삭제하고 미존재 투표를 거부한다
7. [L190](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java#L190) · `@Test` · `전체_콘텐츠_삭제는_투표_존재를_분기한다` — 전체 콘텐츠 삭제는 이미지·링크를 지우고 투표가 있을 때만 form까지 삭제한다
8. [L207](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java#L207) · `@Test` · `이미지_교체는_모든_입력_경계를_처리한다` — 이미지 교체는 null no-op, 10장 상한, 빈 목록 삭제, 재정렬을 처리한다
9. [L229](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeContentServiceTest.java#L229) · `@Test` · `링크_교체는_모든_입력_경계를_처리한다` — 링크 교체는 null no-op, 빈 목록 삭제, 비어 있지 않으면 재정렬한다

#### NoticeReadAndVoteResponseServiceTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/application/service/command/NoticeReadAndVoteResponseServiceTest.java`

1. [L71](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeReadAndVoteResponseServiceTest.java#L71) · `@Test` · `특정_기수_읽음은_멱등하게_기록한다` — 특정 기수 공지는 해당 기수 challenger로 읽음을 한 번만 기록한다
2. [L90](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeReadAndVoteResponseServiceTest.java#L90) · `@Test` · `전체_기수_읽음은_최신_challenger를_사용한다` — 전체 기수 공지는 최신 active challenger로 기록한다
3. [L103](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeReadAndVoteResponseServiceTest.java#L103) · `@Test` · `미존재_공지의_읽음을_거부한다` — 미존재 공지는 읽음 기록 전에 not-found로 거부한다
4. [L131](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeReadAndVoteResponseServiceTest.java#L131) · `@Test` · `진행_중_투표를_제출한다` — 진행 중 투표 제출은 primary question에 선택지를 담아 즉시 제출한다
5. [L149](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeReadAndVoteResponseServiceTest.java#L149) · `@Test` · `투표_응답을_수정하거나_취소한다` — 투표 응답 수정은 선택지를 갱신하고 빈 목록은 응답을 취소한다
6. [L170](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeReadAndVoteResponseServiceTest.java#L170) · `@Test` · `잘못된_투표_상태를_거부한다` — null 선택지와 미존재·시작 전·종료 투표는 거부한다

#### NoticeServiceTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/application/service/command/NoticeServiceTest.java`

1. [L94](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeServiceTest.java#L94) · `@Test` · `bulk_생성은_빈_입력과_정상_입력을_처리한다` — 빈 bulk는 저장하지 않고 정상 bulk는 ID와 target을 순서대로 생성한다
2. [L113](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeServiceTest.java#L113) · `@Test` · `알림_공지는_FCM을_요청하고_notified로_전환한다` — 알림 공지는 대상과 축약 제목·본문을 FCM에 전달하고 재전송을 방지한다
3. [L140](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeServiceTest.java#L140) · `@Test` · `파트_대상_알림은_FCM_Set으로_변환한다` — 파트 대상 알림은 입력 목록을 중복 없는 FCM 대상 Set으로 변환한다
4. [L165](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeServiceTest.java#L165) · `@Test` · `권한_부족은_SUPER_ADMIN으로만_override한다` — 일반 권한이 없으면 SUPER_ADMIN만 생성할 수 있다
5. [L182](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeServiceTest.java#L182) · `@Test` · `수정을_적용하고_미존재를_거부한다` — 수정은 제목·내용·필독을 변경하고 미존재 공지를 거부한다
6. [L197](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeServiceTest.java#L197) · `@Test` · `삭제는_모든_연관_데이터를_정리한다` — 삭제는 부가 콘텐츠, 읽음, 대상, 공지 순으로 정리한다
7. [L211](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeServiceTest.java#L211) · `@Test` · `리마인드는_challenger를_member로_변환한다` — 리마인드는 중복 challenger ID를 제거하고 조회된 member ID로만 발송한다
8. [L228](../../../src/test/java/com/umc/product/notice/application/service/command/NoticeServiceTest.java#L228) · `@Test` · `조회수를_증가시킨다` — 조회수 증가는 persistence port에 그대로 위임한다

#### NoticePermissionEvaluatorResidualTest (10개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java`

1. [L51](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L51) · `@Test` · `rejects_unsupported_permission` — 지원하지 않는 권한은 평가하지 않고 명시적인 권한 예외를 반환한다
2. [L62](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L62) · `@Test` · `exposes_supported_type_and_rejects_foreign_permission` — 지원 리소스 타입을 NOTICE로 선언하고 foreign permission은 구현되지 않은 권한으로 거부한다
3. [L78](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L78) · `@Test` · `challenger_scope_must_match_completely` — 일반 챌린저는 기수·지부·학교·파트가 모두 일치할 때만 읽을 수 있다
4. [L93](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L93) · `@Test` · `staff_notice_checks_role_gisu_school_and_part` — 운영진 공지는 역할 하한·기수·학교·담당 파트를 모두 확인한다
5. [L124](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L124) · `@Test` · `school_part_leader_scope_is_fail_closed` — 학교 파트장은 역할의 학교·파트·기수·지부 범위가 모두 맞아야 일반 공지를 읽는다
6. [L154](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L154) · `@Test` · `school_core_scope_is_fail_closed` — 학교 회장단은 같은 학교 또는 같은 기수·지부 범위의 공지만 읽는다
7. [L174](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L174) · `@Test` · `chapter_president_scope_is_fail_closed` — 지부장은 조직·학교·기수·지부 범위가 불완전하거나 전체 공지이면 역할 우회가 허용되지 않는다
8. [L203](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L203) · `@Test` · `central_member_reads_only_matching_gisu` — 중앙 운영진은 같은 기수의 일반 공지를 파트와 무관하게 읽는다
9. [L221](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L221) · `@Test` · `edit_and_delete_require_super_admin_or_author` — 수정·삭제는 SUPER_ADMIN 또는 작성자만 허용하고 누락 공지는 명시적으로 실패한다
10. [L240](../../../src/test/java/com/umc/product/notice/application/service/NoticePermissionEvaluatorResidualTest.java#L240) · `@Test` · `manage_permission_depends_on_target_scope` — 공지 관리 권한은 중앙·학교·지부·전체 공지 범위별로 fail-closed 한다

#### NoticeContentAndTargetQueryServiceTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/application/service/query/NoticeContentAndTargetQueryServiceTest.java`

1. [L63](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeContentAndTargetQueryServiceTest.java#L63) · `@Test` · `링크를_표시_순서대로_반환한다` — 링크는 displayOrder 오름차순으로 info를 반환한다
2. [L78](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeContentAndTargetQueryServiceTest.java#L78) · `@Test` · `투표_부분_데이터_누락을_처리한다` — 투표가 없거나 form 정보가 누락되면 null을 반환한다
3. [L90](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeContentAndTargetQueryServiceTest.java#L90) · `@Test` · `투표_정보를_조립한다` — 투표는 form 선택지와 참여 정보 및 notice 기간 상태를 조립한다
4. [L113](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeContentAndTargetQueryServiceTest.java#L113) · `@Test` · `이미지는_file_link를_batch로_조립한다` — 이미지는 file link 누락을 허용하며 displayOrder 오름차순으로 반환한다
5. [L158](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeContentAndTargetQueryServiceTest.java#L158) · `@Test` · `대상을_조회하고_미존재를_거부한다` — 대상을 info로 변환하고 미존재 대상은 구체적인 예외를 반환한다

#### NoticeQueryServiceTest (14개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java`

1. [L92](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L92) · `@Test` · `공지_목록은_연관_데이터를_batch로_조립한다` — 공지 목록은 target과 작성자를 batch 조회하고 누락 연관 데이터는 null로 매핑한다
2. [L116](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L116) · `@Test` · `파트_검색은_조회자_scope를_보완한다` — 파트 검색은 challenger의 지부·학교를 보완하고 keyword repository를 호출한다
3. [L132](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L132) · `@Test` · `운영진_목록은_역할_계층을_검증한다` — 운영진 목록은 중앙 역할이 요청 역할 이상일 때 조회한다
4. [L145](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L145) · `@Test` · `조회_scope와_역할을_fail_closed로_검증한다` — 다른 지부·학교와 권한 없는 운영진 조회는 fail-closed로 거부한다
5. [L165](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L165) · `@Test` · `공지_상세를_조립한다` — 공지 상세는 부가 콘텐츠와 target을 조립하고 target 누락을 허용한다
6. [L183](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L183) · `@Test` · `UNREAD_현황은_cursor_page를_계산한다` — UNREAD 현황은 20개 page와 challenger ID cursor를 만들고 다음 페이지를 표시한다
7. [L202](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L202) · `@Test` · `READ_현황은_read_ID_cursor를_사용한다` — READ 현황은 학교 filter와 NoticeRead ID cursor 다음부터 반환한다
8. [L223](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L223) · `@Test` · `현황은_빈_대상과_조직을_단축한다` — 현황은 대상 또는 조직 결과가 비면 외부 read 조회를 단축한다
9. [L240](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L240) · `@Test` · `읽음_통계는_대상자와_비율을_계산한다` — 읽음 통계는 전체 기수 최신 challenger 대상과 0명 경계를 계산한다
10. [L269](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L269) · `@Test` · `target_누락을_거부한다` — 현황과 통계는 target이 없으면 not-found로 거부한다
11. [L279](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L279) · `@Test` · `전체_기수_현황은_누락된_회원과_지부를_안전하게_처리한다` — 전체 기수 현황은 최신 challenger만 사용하고 회원·지부 누락을 안전하게 처리한다
12. [L306](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L306) · `@Test` · `지부_filter와_알_수_없는_cursor를_처리한다` — 지부 현황 filter는 사전 조회한 지부를 사용하고 알 수 없는 cursor는 첫 페이지로 복구한다
13. [L327](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L327) · `@Test` · `통계는_중첩_지부_cache와_학교_누락을_처리한다` — 통계 대상은 중첩 지부 cache를 사용하고 학교가 누락된 회원은 제외한다
14. [L348](../../../src/test/java/com/umc/product/notice/application/service/query/NoticeQueryServiceTest.java#L348) · `@Test` · `명시한_지부_분류를_보존한다` — 명시한 지부 분류는 자동 보완하지 않고 그대로 repository에 전달한다

### Domain

#### NoticeDomainTest (10개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java`

1. [L29](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L29) · `@Test` · `공지는_수정과_알림_상태를_관리한다` — 공지는 수정, 작성자 검증, 알림 상태 전이를 지원한다
2. [L47](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L47) · `@Test` · `부가_콘텐츠는_공지와_순서를_보존한다` — 공지 부가 콘텐츠는 공지와 표시 순서를 보존한다
3. [L65](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L65) · `@Test` · `투표는_시간_경계를_판정한다` — 투표는 시작 전, 진행 중, 종료 경계를 exclusive 종료 시각으로 판정한다
4. [L79](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L79) · `@Test` · `공지_대상은_scope를_판정한다` — 공지 대상은 엔티티 변환, staff 판정, 전체 및 세부 scope 일치를 지원한다
5. [L97](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L97) · `@Test` · `공지_분류를_target으로_변환한다` — 공지 분류는 challenger 여부와 target info 변환을 제공한다
6. [L111](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L111) · `@Test` · `공지_tab은_역할_계층을_판정한다` — 공지 tab은 읽기 계층과 challenger role 변환을 정확히 제공한다
7. [L138](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L138) · `@Test` · `모든_challenger_대상_패턴을_검증한다` — 모든 일반 challenger 대상 패턴은 구조와 권한 호출을 검증한다
8. [L160](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L160) · `@Test` · `잘못된_challenger_대상_조합을_거부한다` — 잘못된 challenger 대상 조합은 구체적인 domain 예외로 거부한다
9. [L172](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L172) · `@Test` · `staff_대상_패턴을_검증한다` — staff 대상 패턴은 기수·학교·파트 제약과 작성 권한 분기를 검증한다
10. [L193](../../../src/test/java/com/umc/product/notice/domain/NoticeDomainTest.java#L193) · `@Test` · `enum과_error_code를_노출한다` — 공지 enum과 error code는 전체 상수를 안정적으로 노출한다

### Persistence

#### NoticeContentPersistenceAdapterUnitTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/out/persistence/NoticeContentPersistenceAdapterUnitTest.java`

1. [L21](../../../src/test/java/com/umc/product/notice/adapter/out/persistence/NoticeContentPersistenceAdapterUnitTest.java#L21) · `@Test` · `delegates_all_content_contracts` — 이미지·링크·투표의 모든 조회·저장·삭제 port 계약을 위임한다

#### NoticePersistenceAdapterUnitTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/out/persistence/NoticePersistenceAdapterUnitTest.java`

1. [L27](../../../src/test/java/com/umc/product/notice/adapter/out/persistence/NoticePersistenceAdapterUnitTest.java#L27) · `@Test` · `delegates_notice_and_read_contracts` — 공지·읽음의 모든 persistence port 계약을 repository에 그대로 위임한다

#### NoticeQueryRepositoryTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/out/persistence/NoticeQueryRepositoryTest.java`

1. [L47](../../../src/test/java/com/umc/product/notice/adapter/out/persistence/NoticeQueryRepositoryTest.java#L47) · `@Test` · `challenger_classification_covers_all_scope_combinations` — 챌린저 분류는 전체·지부·학교·파트 조합과 잘못된 조합을 구분한다
2. [L88](../../../src/test/java/com/umc/product/notice/adapter/out/persistence/NoticeQueryRepositoryTest.java#L88) · `@Test` · `staff_classification_covers_role_school_and_part_conditions` — 운영진 분류는 역할 하한·학교·명시 파트·담당 파트와 빈 담당 파트를 적용한다
3. [L125](../../../src/test/java/com/umc/product/notice/adapter/out/persistence/NoticeQueryRepositoryTest.java#L125) · `@Test` · `keyword_read_aggregate_unread_and_content_order_are_persisted` — keyword·읽음 집계·미열람 조회·콘텐츠 표시 순서는 빈 입력과 실제 SQL 결과를 보장한다

#### NoticeTargetPersistenceAdapterUnitTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/out/persistence/NoticeTargetPersistenceAdapterUnitTest.java`

1. [L20](../../../src/test/java/com/umc/product/notice/adapter/out/persistence/NoticeTargetPersistenceAdapterUnitTest.java#L20) · `@Test` · `delegates_all_target_contracts` — 대상 조회·저장·삭제의 모든 port 계약을 repository에 위임한다

### REST / Web

#### NoticeViewerInfoAssemblerTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/in/web/assembler/NoticeViewerInfoAssemblerTest.java`

1. [L70](../../../src/test/java/com/umc/product/notice/adapter/in/web/assembler/NoticeViewerInfoAssemblerTest.java#L70) · `@Test` · `assembles_parts_chapter_and_highest_staff_role` — 챌린저 파트와 담당 파트를 합치고 학교의 지부와 최상위 역할을 조립한다
2. [L98](../../../src/test/java/com/umc/product/notice/adapter/in/web/assembler/NoticeViewerInfoAssemblerTest.java#L98) · `@Test` · `returns_empty_information_for_missing_inputs_and_failed_chapter_lookup` — 회원·기수가 없거나 소속 정보 조회가 실패하면 안전한 빈 조회자 정보를 반환한다
3. [L120](../../../src/test/java/com/umc/product/notice/adapter/in/web/assembler/NoticeViewerInfoAssemblerTest.java#L120) · `@Test` · `handles_null_inputs_and_missing_member` — null 입력과 조회되지 않은 회원은 외부 소속 조회 없이 빈 정보를 반환한다

#### NoticeDtoTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/in/web/dto/NoticeDtoTest.java`

1. [L67](../../../src/test/java/com/umc/product/notice/adapter/in/web/dto/NoticeDtoTest.java#L67) · `@Test` · `command_DTO는_입력값을_보존한다` — 모든 command DTO는 입력값을 손실 없이 보존한다
2. [L97](../../../src/test/java/com/umc/product/notice/adapter/in/web/dto/NoticeDtoTest.java#L97) · `@Test` · `투표_command는_선택지를_검증한다` — 투표 command는 선택지 개수와 빈 내용을 엄격히 검증한다
3. [L109](../../../src/test/java/com/umc/product/notice/adapter/in/web/dto/NoticeDtoTest.java#L109) · `@Test` · `웹_요청을_command와_query로_변환한다` — 모든 웹 요청은 식별자와 본문을 application command/query로 변환한다
4. [L135](../../../src/test/java/com/umc/product/notice/adapter/in/web/dto/NoticeDtoTest.java#L135) · `@Test` · `query_DTO와_응답을_매핑한다` — query DTO와 응답은 상세·요약·읽음·통계·투표 정보를 정확히 매핑한다
5. [L177](../../../src/test/java/com/umc/product/notice/adapter/in/web/dto/NoticeDtoTest.java#L177) · `@Test` · `command_응답은_ID를_노출한다` — command 응답 DTO는 생성된 식별자를 그대로 노출한다

#### NoticeCommandControllerUnitTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/in/web/NoticeCommandControllerUnitTest.java`

1. [L37](../../../src/test/java/com/umc/product/notice/adapter/in/web/NoticeCommandControllerUnitTest.java#L37) · `@Test` · `delegates_all_commands_with_current_member` — 현재 회원 ID와 요청을 모든 공지 command로 정확히 변환한다

#### NoticeContentControllerUnitTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/in/web/NoticeContentControllerUnitTest.java`

1. [L34](../../../src/test/java/com/umc/product/notice/adapter/in/web/NoticeContentControllerUnitTest.java#L34) · `@Test` · `delegates_all_content_commands` — 이미지·링크·투표의 추가·교체·삭제를 현재 회원 ID와 함께 위임한다

#### NoticeQueryControllerUnitTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/in/web/NoticeQueryControllerUnitTest.java`

1. [L51](../../../src/test/java/com/umc/product/notice/adapter/in/web/NoticeQueryControllerUnitTest.java#L51) · `@Test` · `maps_list_and_search_pages` — 목록·검색은 조회자 정보를 조립하고 page 응답으로 변환한다
2. [L77](../../../src/test/java/com/umc/product/notice/adapter/in/web/NoticeQueryControllerUnitTest.java#L77) · `@Test` · `maps_detail_statistics_and_cursor` — 상세 조회는 조회수를 증가시키고 통계·cursor 응답을 변환한다

#### NoticeVoteResponseControllerUnitTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notice/adapter/in/web/NoticeVoteResponseControllerUnitTest.java`

1. [L27](../../../src/test/java/com/umc/product/notice/adapter/in/web/NoticeVoteResponseControllerUnitTest.java#L27) · `@Test` · `delegates_submit_and_update_commands` — 투표 제출·수정·취소 요청에 공지와 현재 회원 ID를 결합한다

## 3.16 `analytics` — 19개

### Application Service

#### AdminAnalyticsPermissionEvaluatorTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/analytics/application/service/evaluator/AdminAnalyticsPermissionEvaluatorTest.java`

1. [L24](../../../src/test/java/com/umc/product/analytics/application/service/evaluator/AdminAnalyticsPermissionEvaluatorTest.java#L24) · `@Test` · `supports_read_and_super_admin` — ANALYTICS READ만 지원하고 SUPER_ADMIN은 항상 허용한다
2. [L36](../../../src/test/java/com/umc/product/analytics/application/service/evaluator/AdminAnalyticsPermissionEvaluatorTest.java#L36) · `@Test` · `permits_only_admin_roles` — 중앙·지부·학교 운영진만 허용하고 일반 역할은 fail-closed 한다

#### AdminAnalyticsQueryServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsQueryServiceTest.java`

1. [L176](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsQueryServiceTest.java#L176) · `@Test` · `delegates_remaining_analytics_queries_with_resolved_scope` — context·학교·위험군과 분리된 운영 지표를 동일한 권한 스코프로 조회한다

#### AdminAnalyticsScopeResolverTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java`

1. [L129](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java#L129) · `@Test` · `활성_기수와_지부장_스코프를_해석한다` — 기수를 생략하면 활성 기수를 사용하고 지부장은 본인 지부 스코프를 얻는다
2. [L142](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java#L142) · `@Test` · `학교와_파트_scope_우회를_거부한다` — 학교 운영진의 다른 학교 요청과 파트장의 다른 파트 요청은 거부한다
3. [L158](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java#L158) · `@Test` · `다른_기수_역할을_제외한다` — 다른 기수 역할만 있으면 접근을 거부한다
4. [L171](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java#L171) · `@Test` · `여러_역할에서_최상위_역할을_선택한다` — 여러 역할은 중앙·지부·학교·파트장 우선순위로 정렬한다

### Persistence

#### AdminAnalyticsPersistenceAdapterUnitTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminAnalyticsPersistenceAdapterUnitTest.java`

1. [L32](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminAnalyticsPersistenceAdapterUnitTest.java#L32) · `@Test` · `delegates_all_read_ports` — dashboard·operations·school·risk adapter는 query repository 결과를 그대로 반환한다

#### AdminDashboardAnalyticsQueryRepositoryTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminDashboardAnalyticsQueryRepositoryTest.java`

1. [L126](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminDashboardAnalyticsQueryRepositoryTest.java#L126) · `@Test` · `수료_임박_scope와_증감률을_계산한다` — 수료 임박과 지부·학교·파트 스코프 및 이전 주 대비 증감률을 계산한다

#### AdminOperationsAnalyticsQueryRepositoryTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminOperationsAnalyticsQueryRepositoryTest.java`

1. [L92](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminOperationsAnalyticsQueryRepositoryTest.java#L92) · `@Test` · `central_scope_aggregates_all_operations_metrics` — 중앙 운영 지표는 학교·포인트·출석·스터디·가입을 동일 기간으로 집계한다
2. [L121](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminOperationsAnalyticsQueryRepositoryTest.java#L121) · `@Test` · `scoped_and_out_of_period_queries_are_fail_closed` — 지부·학교·파트 스코프와 기간 밖 데이터는 각 집계에서 fail-closed 한다

#### AdminRiskChallengerAnalyticsQueryRepositoryTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminRiskChallengerAnalyticsQueryRepositoryTest.java`

1. [L93](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminRiskChallengerAnalyticsQueryRepositoryTest.java#L93) · `@Test` · `빈_결과와_세부_scope를_처리한다` — 빈 위험군과 지부·학교·파트 스코프를 안전하게 처리한다

#### AdminSchoolAnalyticsQueryRepositoryTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminSchoolAnalyticsQueryRepositoryTest.java`

1. [L165](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminSchoolAnalyticsQueryRepositoryTest.java#L165) · `@Test` · `운영진_정렬_빈_page와_scope를_처리한다` — 학교 요약은 운영진 조립·모든 정렬·빈 페이지와 학교·파트 스코프를 처리한다

### REST / Web

#### AdminDashboardControllerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/analytics/adapter/in/web/AdminDashboardControllerTest.java`

1. [L228](../../../src/test/java/com/umc/product/analytics/adapter/in/web/AdminDashboardControllerTest.java#L228) · `@Test` · `분리된_operations_API_응답` — 분리된 operations API들은 중첩 집계와 기간 요청을 응답으로 변환한다

#### AnalyticsDtoResidualTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/analytics/adapter/in/web/dto/AnalyticsDtoResidualTest.java`

1. [L31](../../../src/test/java/com/umc/product/analytics/adapter/in/web/dto/AnalyticsDtoResidualTest.java#L31) · `@Test` · `period_queries_default_and_reject_invalid_ranges` — 운영 기간 query는 기본 30일을 만들고 역전·동일 기간을 거부한다
2. [L53](../../../src/test/java/com/umc/product/analytics/adapter/in/web/dto/AnalyticsDtoResidualTest.java#L53) · `@Test` · `school_sort_is_whitelisted` — 학교 정렬은 기본·허용값을 해석하고 다른 화면 정렬과 알 수 없는 값을 거부한다
3. [L71](../../../src/test/java/com/umc/product/analytics/adapter/in/web/dto/AnalyticsDtoResidualTest.java#L71) · `@Test` · `maps_all_admin_role_types` — 모든 challenger 역할을 analytics 역할로 손실 없이 변환한다
4. [L87](../../../src/test/java/com/umc/product/analytics/adapter/in/web/dto/AnalyticsDtoResidualTest.java#L87) · `@Test` · `risk_response_preserves_optional_latest_negative_point` — 위험군 응답은 최근 감점의 존재·부재를 모두 보존한다
5. [L103](../../../src/test/java/com/umc/product/analytics/adapter/in/web/dto/AnalyticsDtoResidualTest.java#L103) · `@Test` · `domain_exception_supports_both_constructors` — analytics 예외는 기본 메시지와 사용자 메시지 생성자를 모두 지원한다

## 3.17 `notification` — 60개

### Application Event

#### FcmNotificationRequestedEventListenerTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notification/application/event/FcmNotificationRequestedEventListenerTest.java`

1. [L26](../../../src/test/java/com/umc/product/notification/application/event/FcmNotificationRequestedEventListenerTest.java#L26) · `@Test` · `disabled_skips_request` — FCM이 비활성화되면 audience와 토큰을 조회하지 않는다
2. [L44](../../../src/test/java/com/umc/product/notification/application/event/FcmNotificationRequestedEventListenerTest.java#L44) · `@Test` · `empty_audience_skips_token_lookup` — 해석된 audience가 비어 있으면 토큰을 조회하지 않는다
3. [L62](../../../src/test/java/com/umc/product/notification/application/event/FcmNotificationRequestedEventListenerTest.java#L62) · `@Test` · `no_active_token_skips_batch_event` — audience에 활성 토큰이 없으면 batch event를 발행하지 않는다

#### FcmSendBatchRequestedEventListenerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEventListenerTest.java`

1. [L36](../../../src/test/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEventListenerTest.java#L36) · `@Test` · `disabled_skips_batch` — FCM이 비활성화되면 토큰 조회와 발송을 생략한다
2. [L53](../../../src/test/java/com/umc/product/notification/application/event/FcmSendBatchRequestedEventListenerTest.java#L53) · `@Test` · `no_active_token_skips_send` — 활성 토큰이 없으면 FCM 발송을 생략한다

### Application Service

#### FcmPermissionEvaluatorTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notification/application/service/evaluator/FcmPermissionEvaluatorTest.java`

1. [L42](../../../src/test/java/com/umc/product/notification/application/service/evaluator/FcmPermissionEvaluatorTest.java#L42) · `@Test` · `central_core_can_delete` — 중앙운영사무국 총괄단은 FCM token 삭제 권한을 가진다
2. [L51](../../../src/test/java/com/umc/product/notification/application/service/evaluator/FcmPermissionEvaluatorTest.java#L51) · `@Test` · `read_is_not_supported` — FCM read는 지원하지 않아 총괄단도 fail-closed 처리한다

#### FcmAudienceResolverTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/application/service/FcmAudienceResolverTest.java`

1. [L39](../../../src/test/java/com/umc/product/notification/application/service/FcmAudienceResolverTest.java#L39) · `@Test` · `resolves_explicit_and_target_members_deterministically` — 명시 회원과 target 회원을 순서 유지·중복 제거하고 누락 조직은 제외한다
2. [L56](../../../src/test/java/com/umc/product/notification/application/service/FcmAudienceResolverTest.java#L56) · `@Test` · `missing_target_short_circuits` — target 기수 또는 challenger가 없으면 명시 회원만 반환한다

#### FcmAudienceServiceTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/application/service/FcmAudienceServiceTest.java`

1. [L71](../../../src/test/java/com/umc/product/notification/application/service/FcmAudienceServiceTest.java#L71) · `@Test` · `disabled_is_noop_for_all_entry_points` — FCM 비활성화 시 audience·단일·복수 발송을 모두 no-op 처리한다
2. [L83](../../../src/test/java/com/umc/product/notification/application/service/FcmAudienceServiceTest.java#L83) · `@Test` · `empty_audience_and_tokens_short_circuit` — 대상·챌린저·활성 토큰이 없으면 외부 발송을 단축한다
3. [L110](../../../src/test/java/com/umc/product/notification/application/service/FcmAudienceServiceTest.java#L110) · `@Test` · `audience_filters_targets_and_deactivates_invalid_tokens` — audience는 학교·지부·파트를 일괄 해석하고 invalid token만 비활성화한다
4. [L139](../../../src/test/java/com/umc/product/notification/application/service/FcmAudienceServiceTest.java#L139) · `@Test` · `member_sends_partition_and_count_partial_failures` — 단일·복수 발송은 500개씩 분할하고 배치 예외를 부분 실패로 집계한다

#### FcmTokenValidationServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notification/application/service/FcmTokenValidationServiceTest.java`

1. [L120](../../../src/test/java/com/umc/product/notification/application/service/FcmTokenValidationServiceTest.java#L120) · `@Test` · `no_due_token_short_circuit` — 검증 기한이 지난 토큰이 없으면 provider를 호출하지 않는다

#### FcmTopicServiceTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/application/service/FcmTopicServiceTest.java`

1. [L11](../../../src/test/java/com/umc/product/notification/application/service/FcmTopicServiceTest.java#L11) · `@Test` · `all_legacy_operations_are_noop` — 모든 legacy topic API는 외부 부작용 없이 호출 가능하다

#### SendEmailServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notification/application/service/SendEmailServiceTest.java`

1. [L74](../../../src/test/java/com/umc/product/notification/application/service/SendEmailServiceTest.java#L74) · `@Test` · `verification_email_uses_fixed_template_and_subject` — 인증 이메일은 고정 template과 인증 코드 제목으로 발송한다
2. [L87](../../../src/test/java/com/umc/product/notification/application/service/SendEmailServiceTest.java#L87) · `@Test` · `template_failures_are_wrapped` — 인증·HTML template 렌더링 실패는 원인을 보존한 도메인 예외로 변환한다
3. [L103](../../../src/test/java/com/umc/product/notification/application/service/SendEmailServiceTest.java#L103) · `@Test` · `send_failures_are_propagated` — 인증·HTML 발송 실패는 삼키지 않고 호출자에게 전파한다

#### WebhookAlarmServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notification/application/service/WebhookAlarmServiceTest.java`

1. [L37](../../../src/test/java/com/umc/product/notification/application/service/WebhookAlarmServiceTest.java#L37) · `@Test` · `플랫폼별_전송_결과를_격리한다` — 활성 profile을 제목에 표시하고 플랫폼별 성공·실패·미등록 결과를 격리한다

### Contract / Misc

#### WebhookAlarmAspectTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/in/aop/WebhookAlarmAspectTest.java`

1. [L33](../../../src/test/java/com/umc/product/notification/adapter/in/aop/WebhookAlarmAspectTest.java#L33) · `@Test` · `spel을_평가해_즉시_전송한다` — 메서드 인자와 반환값으로 SpEL을 평가해 즉시 전송한다
2. [L49](../../../src/test/java/com/umc/product/notification/adapter/in/aop/WebhookAlarmAspectTest.java#L49) · `@Test` · `buffered_설정이면_이벤트_경로로_전달한다` — buffered 설정이면 평가한 명령을 이벤트 발행 경로로 전달한다
3. [L65](../../../src/test/java/com/umc/product/notification/adapter/in/aop/WebhookAlarmAspectTest.java#L65) · `@Test` · `잘못된_spel은_예외를_전파하지_않는다` — 잘못된 SpEL은 원래 메서드의 성공 흐름에 예외를 전파하지 않는다

### Domain

#### NotificationResidualDomainTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/domain/NotificationResidualDomainTest.java`

1. [L27](../../../src/test/java/com/umc/product/notification/domain/NotificationResidualDomainTest.java#L27) · `@Test` · `fcm_outbox_state_transitions` — FCM outbox는 처리 완료 시각과 최대 재시도 실패 상태를 관리한다
2. [L46](../../../src/test/java/com/umc/product/notification/domain/NotificationResidualDomainTest.java#L46) · `@Test` · `request_command_normalizes_and_validates` — FCM 요청 command는 식별자를 정규화하고 제목·본문 필수값을 거부한다
3. [L77](../../../src/test/java/com/umc/product/notification/domain/NotificationResidualDomainTest.java#L77) · `@Test` · `fcm_events_assign_defaults` — FCM event는 null 식별자와 빈 대상 목록에 안전한 기본값을 부여한다
4. [L101](../../../src/test/java/com/umc/product/notification/domain/NotificationResidualDomainTest.java#L101) · `@Test` · `webhook_event_normalizes_event_type` — Webhook event는 blank event type을 표준값으로 바꾸고 컬렉션을 방어 복사한다
5. [L114](../../../src/test/java/com/umc/product/notification/domain/NotificationResidualDomainTest.java#L114) · `@Test` · `webhook_requires_platform` — Webhook 명령과 event는 빈 플랫폼을 거부한다
6. [L123](../../../src/test/java/com/umc/product/notification/domain/NotificationResidualDomainTest.java#L123) · `@Test` · `webhook_error_contract` — Webhook 오류 코드는 상태·code·메시지를 보존하고 도메인 예외에 연결된다
7. [L134](../../../src/test/java/com/umc/product/notification/domain/NotificationResidualDomainTest.java#L134) · `@Test` · `save_port_default_batch_preserves_order` — SaveFcmPort 기본 batch 저장은 입력 순서대로 단건 저장한다

### External Adapter

#### FirebaseFcmMessageAdapterTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapterTest.java`

1. [L41](../../../src/test/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapterTest.java#L41) · `@Test` · `empty_targets_short_circuit` — 대상이 비어 있으면 Firebase를 호출하지 않고 빈 결과를 반환한다
2. [L55](../../../src/test/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapterTest.java#L55) · `@Test` · `optional_payload_is_preserved` — 이미지·data·deep link를 Firebase multicast message에 보존한다
3. [L81](../../../src/test/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmMessageAdapterTest.java#L81) · `@Test` · `provider_failure_is_translated` — Firebase provider 오류를 FCM 도메인 예외로 변환한다

#### FirebaseFcmTokenValidationAdapterTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmTokenValidationAdapterTest.java`

1. [L39](../../../src/test/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmTokenValidationAdapterTest.java#L39) · `@Test` · `empty_targets_short_circuit` — 대상이 비어 있으면 Firebase 호출 없이 빈 검증 결과를 반환한다
2. [L51](../../../src/test/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmTokenValidationAdapterTest.java#L51) · `@Test` · `failure_without_exception_is_not_invalid` — 예외 정보가 없는 실패 응답은 무효 토큰으로 단정하지 않는다
3. [L70](../../../src/test/java/com/umc/product/notification/adapter/out/external/fcm/FirebaseFcmTokenValidationAdapterTest.java#L70) · `@Test` · `provider_failure_is_translated` — Firebase provider 오류를 FCM 도메인 예외로 변환한다

#### NoopFcmAdapterTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/out/external/fcm/NoopFcmAdapterTest.java`

1. [L18](../../../src/test/java/com/umc/product/notification/adapter/out/external/fcm/NoopFcmAdapterTest.java#L18) · `@Test` · `message_send_is_noop` — 메시지 발송은 외부 호출 없이 빈 결과를 반환한다
2. [L34](../../../src/test/java/com/umc/product/notification/adapter/out/external/fcm/NoopFcmAdapterTest.java#L34) · `@Test` · `token_validation_accepts_all_targets` — 토큰 검증은 모든 입력 토큰을 유효한 것으로 간주한다

#### SesEmailAdapterTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/out/external/ses/SesEmailAdapterTest.java`

1. [L28](../../../src/test/java/com/umc/product/notification/adapter/out/external/ses/SesEmailAdapterTest.java#L28) · `@Test` · `sends_complete_ses_request` — UTF-8 HTML 요청과 configuration set을 만들고 발신자 표시명을 RFC 형식으로 escape한다
2. [L49](../../../src/test/java/com/umc/product/notification/adapter/out/external/ses/SesEmailAdapterTest.java#L49) · `@Test` · `wraps_provider_and_runtime_failures` — AWS 오류와 예기치 못한 runtime 오류를 cause가 있는 이메일 도메인 예외로 변환한다
3. [L70](../../../src/test/java/com/umc/product/notification/adapter/out/external/ses/SesEmailAdapterTest.java#L70) · `@Test` · `resolves_configuration_boundaries` — SES 설정은 static/default credentials와 configuration set 존재 여부를 정확히 판정한다

#### DiscordWebhookAdapterTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapterTest.java`

1. [L24](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapterTest.java#L24) · `@Test` · `단문은_하나의_embed로_전송한다` — 단문은 하나의 embed로 전송한다
2. [L41](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapterTest.java#L41) · `@Test` · `긴_본문은_개행을_우선해_분할한다` — 최대 길이를 넘는 본문은 개행을 우선해 여러 embed로 분할한다
3. [L55](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapterTest.java#L55) · `@Test` · `개행이_없는_긴_본문도_분할한다` — 개행이 없는 긴 본문도 최대 길이 기준으로 분할한다

#### SlackWebhookAdapterTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/out/external/webhook/SlackWebhookAdapterTest.java`

1. [L22](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/SlackWebhookAdapterTest.java#L22) · `@Test` · `단문은_하나의_메시지로_전송한다` — 단문은 제목을 강조한 하나의 메시지로 전송한다
2. [L38](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/SlackWebhookAdapterTest.java#L38) · `@Test` · `긴_본문은_개행을_우선해_분할한다` — 제목을 제외한 최대 길이를 넘는 본문은 개행을 우선해 분할한다
3. [L51](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/SlackWebhookAdapterTest.java#L51) · `@Test` · `매우_긴_제목이면_기본_최대_길이를_사용한다` — 제목만으로 여유 길이가 소진되면 기본 최대 길이로 본문을 계산한다
4. [L62](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/SlackWebhookAdapterTest.java#L62) · `@Test` · `개행이_없는_긴_본문도_분할한다` — 개행이 없는 긴 본문도 최대 길이 기준으로 분할한다

#### TelegramWebhookAdapterTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/out/external/webhook/TelegramWebhookAdapterTest.java`

1. [L22](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/TelegramWebhookAdapterTest.java#L22) · `@Test` · `markdown_예약_문자를_escape해_전송한다` — Markdown 예약 문자를 escape해 지정한 채팅방으로 전송한다
2. [L38](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/TelegramWebhookAdapterTest.java#L38) · `@Test` · `긴_본문은_escape_후_개행을_우선해_분할한다` — 최대 길이를 넘는 본문은 escape 이후 개행을 우선해 분할한다
3. [L51](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/TelegramWebhookAdapterTest.java#L51) · `@Test` · `매우_긴_제목이면_기본_최대_길이를_사용한다` — 제목 escape 결과가 여유 길이를 소진하면 기본 최대 길이를 사용한다
4. [L62](../../../src/test/java/com/umc/product/notification/adapter/out/external/webhook/TelegramWebhookAdapterTest.java#L62) · `@Test` · `개행이_없는_긴_본문도_분할한다` — 개행이 없는 긴 본문도 최대 길이 기준으로 분할한다

### Inbound Event

#### FcmOutboxEventListenerTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/in/event/FcmOutboxEventListenerTest.java`

1. [L22](../../../src/test/java/com/umc/product/notification/adapter/in/event/FcmOutboxEventListenerTest.java#L22) · `@Test` · `enabled_processes_outbox` — FCM이 활성화되면 commit 이후 outbox를 즉시 처리한다
2. [L34](../../../src/test/java/com/umc/product/notification/adapter/in/event/FcmOutboxEventListenerTest.java#L34) · `@Test` · `disabled_skips_outbox` — FCM이 비활성화되면 outbox 처리를 생략한다

#### ServerLifecycleAlarmListenerTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/in/event/ServerLifecycleAlarmListenerTest.java`

1. [L22](../../../src/test/java/com/umc/product/notification/adapter/in/event/ServerLifecycleAlarmListenerTest.java#L22) · `@Test` · `애플리케이션_준비_완료를_알린다` — 애플리케이션 준비 완료 시 서버 시작 시각을 알린다
2. [L36](../../../src/test/java/com/umc/product/notification/adapter/in/event/ServerLifecycleAlarmListenerTest.java#L36) · `@Test` · `서버_종료를_알린다` — 종료 직전 서버 종료 시각을 알린다

### Outbound Adapter

#### FcmPersistenceAdapterTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/out/persistentce/FcmPersistenceAdapterTest.java`

1. [L27](../../../src/test/java/com/umc/product/notification/adapter/out/persistentce/FcmPersistenceAdapterTest.java#L27) · `@Test` · `load_operations_delegate_to_repository` — 조회 port의 모든 조건과 validation page 경계를 repository에 그대로 위임한다
2. [L49](../../../src/test/java/com/umc/product/notification/adapter/out/persistentce/FcmPersistenceAdapterTest.java#L49) · `@Test` · `save_operations_delegate_to_repository` — 단건과 batch 저장을 repository에 위임한다

### REST / Web

#### FcmControllerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/in/web/FcmControllerTest.java`

1. [L105](../../../src/test/java/com/umc/product/notification/adapter/in/web/FcmControllerTest.java#L105) · `@Test` · `legacy_topic_구독_해제` — 현재 회원의 모든 legacy topic 구독을 해제한다
2. [L114](../../../src/test/java/com/umc/product/notification/adapter/in/web/FcmControllerTest.java#L114) · `@Test` · `legacy_topic_전체_재구독` — 관리용 legacy topic 재구독 요청을 위임한다

### Scheduler

#### FcmOutboxSchedulerTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/notification/adapter/in/scheduler/FcmOutboxSchedulerTest.java`

1. [L20](../../../src/test/java/com/umc/product/notification/adapter/in/scheduler/FcmOutboxSchedulerTest.java#L20) · `@Test` · `scheduled_processing_delegates` — 스케줄마다 pending outbox 처리를 위임한다

## 3.18 `schedule` — 83개

### Application Service

#### ScheduleCommandServiceTest (11개, 신규 파일)

- 위치: `src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java`

1. [L79](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L79) · `@Test` · `creates_schedule_with_participants` — 유효한 일정과 참여자를 저장하고 생성 ID를 반환한다
2. [L100](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L100) · `@Test` · `validates_create_capabilities` — 생성 capability·최대 인원·출석 일정 권한을 각각 검증한다
3. [L127](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L127) · `@Test` · `validates_active_gisu_and_invited_members` — 활성 기수 밖의 일정과 존재하지 않는 초대 회원을 거부한다
4. [L144](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L144) · `@Test` · `updates_basic_fields_without_capability_lookup` — 기본 필드 수정은 capability 조회 없이 저장한다
5. [L164](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L164) · `@Test` · `converts_to_online_without_attendance` — 온라인·출석 불필요 전환은 장소와 정책을 제거한다
6. [L182](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L182) · `@Test` · `updates_location_time_and_policy` — 대면 위치·시간·출석 정책을 함께 변경한다
7. [L210](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L210) · `@Test` · `updates_only_participant_diff` — 참여자 diff는 삭제와 추가만 수행하고 새 회원 존재 여부를 검증한다
8. [L234](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L234) · `@Test` · `participant_update_noop_and_invalid_member` — 동일 참여자 집합이면 write를 생략하고 잘못된 신규 회원은 거부한다
9. [L257](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L257) · `@Test` · `validates_update_preconditions` — 수정은 not-found·시작 이후·권한별 최대값을 검증한다
10. [L285](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L285) · `@Test` · `delete_and_force_delete` — 일반 삭제는 출석 기록을 보호하고 강제 삭제는 참여자부터 제거한다
11. [L301](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleCommandServiceTest.java#L301) · `@Test` · `delete_not_found` — 삭제와 강제 삭제는 존재하지 않는 일정을 구분한다

#### ScheduleParticipantCommandServiceTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/schedule/application/service/command/ScheduleParticipantCommandServiceTest.java`

1. [L59](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleParticipantCommandServiceTest.java#L59) · `@Test` · `creates_attendance_with_location` — 위치가 있는 최초 출석 요청을 저장하고 좌표·pending 상태를 반환한다
2. [L80](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleParticipantCommandServiceTest.java#L80) · `@Test` · `creates_attendance_without_location_and_excuse` — 위치 없는 최초 출석과 사유 출석은 null 좌표를 보존한다
3. [L106](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleParticipantCommandServiceTest.java#L106) · `@Test` · `excuse_with_location` — 사유 출석의 좌표가 모두 있으면 point로 변환한다
4. [L123](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleParticipantCommandServiceTest.java#L123) · `@Test` · `decides_attendances_in_input_order` — 승인과 거절을 입력 순서대로 처리하고 결정자 정보를 매핑한다
5. [L153](../../../src/test/java/com/umc/product/schedule/application/service/command/ScheduleParticipantCommandServiceTest.java#L153) · `@Test` · `validates_lookup_preconditions` — 일정·정책·참여자 존재 조건을 각 command 경계에서 검증한다

#### AttendancePermissionEvaluatorTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/schedule/application/service/evaluator/AttendancePermissionEvaluatorTest.java`

1. [L58](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/AttendancePermissionEvaluatorTest.java#L58) · `@Test` · `supports_attendance_resource` — 지원 리소스 타입은 ATTENDANCE다
2. [L64](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/AttendancePermissionEvaluatorTest.java#L64) · `@Test` · `write_requires_resource_challenger_and_participant` — WRITE는 resource ID·챌린저 활동·일정 참여를 모두 요구한다
3. [L81](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/AttendancePermissionEvaluatorTest.java#L81) · `@Test` · `approve_requires_target_gisu_admin` — APPROVE는 resource ID와 해당 일정 기수의 운영진 역할을 요구한다
4. [L94](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/AttendancePermissionEvaluatorTest.java#L94) · `@Test` · `super_admin_is_global_override` — SUPER_ADMIN은 일정별 APPROVE와 READ를 전역 override한다
5. [L105](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/AttendancePermissionEvaluatorTest.java#L105) · `@Test` · `read_scope_differs_between_list_and_item` — READ 목록은 운영진 이력, 단건은 대상 기수 운영진을 확인한다
6. [L117](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/AttendancePermissionEvaluatorTest.java#L117) · `@Test` · `target_schedule_not_found` — 일정별 권한 평가에서 일정이 없으면 도메인 not-found를 반환한다
7. [L128](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/AttendancePermissionEvaluatorTest.java#L128) · `@Test` · `unsupported_permission_is_denied` — 지원하지 않는 permission은 fail-closed 처리한다

#### SchedulePermissionEvaluatorTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java`

1. [L65](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L65) · `@Test` · `read_and_write_require_challenger_or_super_admin` — READ와 WRITE는 SUPER_ADMIN 또는 챌린저 활동 이력을 요구한다
2. [L82](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L82) · `@Test` · `edit_requires_resource_and_author` — EDIT는 resource ID와 존재하는 일정의 작성자 조건을 검증한다
3. [L94](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L94) · `@Test` · `target_schedule_not_found` — 수정·강제 삭제 대상 일정이 없으면 not-found를 반환한다
4. [L109](../../../src/test/java/com/umc/product/schedule/application/service/evaluator/SchedulePermissionEvaluatorTest.java#L109) · `@Test` · `missing_resource_and_unsupported_permission_are_denied` — 강제 삭제에 resource ID가 없거나 permission을 지원하지 않으면 fail-closed 처리한다

#### ScheduleCapabilitiesServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/schedule/application/service/query/ScheduleCapabilitiesServiceTest.java`

1. [L57](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleCapabilitiesServiceTest.java#L57) · `@Test` · `member_without_challenger_history_is_not_allowed` — 챌린저 활동 이력이 없으면 일정 생성 권한이 없다
2. [L70](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleCapabilitiesServiceTest.java#L70) · `@Test` · `challenger_without_current_role_uses_default_limit` — 활성 기수 역할이 없으면 일반 챌린저 제한을 적용한다
3. [L85](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleCapabilitiesServiceTest.java#L85) · `@Test` · `maps_every_role_tier` — 현재 기수의 가장 높은 역할을 기준으로 모든 role tier를 매핑한다
4. [L95](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleCapabilitiesServiceTest.java#L95) · `@Test` · `selects_highest_role_independent_of_order` — 여러 역할이 있으면 입력 순서와 무관하게 가장 높은 권한을 선택한다

#### ScheduleQueryServiceTest (11개, 보강 파일)

- 위치: `src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java`

1. [L80](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L80) · `@Test` · `empty_my_schedules_short_circuit` — 내 일정이 없으면 참여자 상세 조회를 생략한다
2. [L90](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L90) · `@Test` · `search_my_schedules_uses_batch_participants` — 내 일정을 batch 참여자 map과 결합하고 참여자가 없는 일정은 빈 목록을 사용한다
3. [L108](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L108) · `@Test` · `gets_schedule_details` — 일정 상세는 tag 포함 일정과 참여자 상세를 결합한다
4. [L122](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L122) · `@Test` · `detail_not_found` — 일정 상세와 운영진 상세는 존재하지 않는 일정을 구분한다
5. [L132](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L132) · `@Test` · `search_admin_schedules_collects_role_union` — 운영진 역할별 일정 범위를 합집합으로 모으고 참여자 상태를 필터링한다
6. [L164](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L164) · `@Test` · `empty_admin_scope_short_circuit` — 운영진 역할이나 대상 일정이 없으면 일정 query를 생략한다
7. [L174](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L174) · `@Test` · `no_admin_schedule_short_circuit` — 조회 범위는 있지만 조건에 맞는 일정이 없으면 참여자를 조회하지 않는다
8. [L187](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L187) · `@Test` · `gets_admin_schedule_for_participant` — 운영진 상세는 일정 참여 조건을 검증하고 상태별 참여자를 반환한다
9. [L206](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L206) · `@Test` · `admin_detail_requires_participant` — 일정 참여자가 아니면 운영진 상세를 거부한다
10. [L218](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L218) · `@Test` · `gets_policy_and_base_info` — 출석 정책 존재 여부와 일정 기본 정보를 조회한다
11. [L232](../../../src/test/java/com/umc/product/schedule/application/service/query/ScheduleQueryServiceTest.java#L232) · `@Test` · `simple_queries_not_found` — 정책·기본 정보 조회는 존재하지 않는 일정을 구분한다

### Domain

#### ScheduleDomainTest (14개, 신규 파일)

- 위치: `src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java`

1. [L27](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L27) · `@Test` · `creates_valid_schedule` — 유효한 일정은 기본 정보와 출석 정책을 보존한다
2. [L39](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L39) · `@Test` · `tag_is_required` — tag가 null 또는 비어 있으면 일정을 생성할 수 없다
3. [L46](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L46) · `@Test` · `validates_time_range` — 종료가 시작보다 빠르거나 출석 인정 종료보다 빠르면 거부한다
4. [L60](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L60) · `@Test` · `attendance_policy_time_order_is_strict` — 출석 정책 시각은 check-in, 시작, 출석, 지각, 종료 순서여야 한다
5. [L78](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L78) · `@Test` · `progress_boundaries_are_exclusive` — 진행·종료 판정은 시작과 종료 경계를 제외한다
6. [L90](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L90) · `@Test` · `determines_attendance_status_at_time_boundaries` — 현재 시각과 출석 정책에 따라 최초 출석 상태를 결정한다
7. [L117](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L117) · `@Test` · `attendance_status_requires_active_policy` — 출석 정책이 없거나 이미 종료된 일정은 출석 상태를 계산하지 않는다
8. [L132](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L132) · `@Test` · `partial_update_preserves_omitted_values` — 수정은 제공된 값만 반영하고 시간·정책 조합을 다시 검증한다
9. [L164](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L164) · `@Test` · `online_and_policy_conversion_are_independent` — 온라인 전환은 장소만 제거하고 출석 정책 제거는 독립적으로 동작한다
10. [L178](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L178) · `@Test` · `participant_create_attendance_validates_preconditions` — 출석 요청은 정책과 최초 요청 조건을 검증한다
11. [L202](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L202) · `@Test` · `submit_excuse_transitions_supported_states` — 사유 제출은 최초·결석·지각 상태를 각각 pending 상태로 전이한다
12. [L227](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L227) · `@Test` · `decides_pending_attendance` — 승인·거절·강제 변경은 pending 상태를 올바른 최종 상태로 전이한다
13. [L247](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L247) · `@Test` · `decision_requires_pending_attendance` — 출석 기록이 없거나 확정 상태면 운영진 결정을 거부한다
14. [L268](../../../src/test/java/com/umc/product/schedule/domain/ScheduleDomainTest.java#L268) · `@Test` · `enum_and_error_contract` — enum과 도메인 예외의 외부 오류 계약을 보존한다

### Persistence

#### SchedulePersistenceAdapterUnitTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/schedule/adapter/out/persistence/SchedulePersistenceAdapterUnitTest.java`

1. [L34](../../../src/test/java/com/umc/product/schedule/adapter/out/persistence/SchedulePersistenceAdapterUnitTest.java#L34) · `@Test` · `schedule_adapter_delegates_all_operations` — Schedule load·save·delete port를 올바른 repository에 위임한다
2. [L65](../../../src/test/java/com/umc/product/schedule/adapter/out/persistence/SchedulePersistenceAdapterUnitTest.java#L65) · `@Test` · `participant_adapter_delegates_all_operations` — ScheduleParticipant의 모든 load·save·delete port를 위임한다

#### ScheduleQueryRepositoryTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/schedule/adapter/out/persistence/ScheduleQueryRepositoryTest.java`

1. [L66](../../../src/test/java/com/umc/product/schedule/adapter/out/persistence/ScheduleQueryRepositoryTest.java#L66) · `@Test` · `find_my_schedules_applies_filters_and_sorting` — 내 일정은 참여·기간·출석 정책 조건과 시작 시각 정렬을 적용한다
2. [L81](../../../src/test/java/com/umc/product/schedule/adapter/out/persistence/ScheduleQueryRepositoryTest.java#L81) · `@Test` · `details_and_author_ids` — tag fetch 상세과 작성자 ID 목록은 존재·부재 계약을 지킨다
3. [L92](../../../src/test/java/com/umc/product/schedule/adapter/out/persistence/ScheduleQueryRepositoryTest.java#L92) · `@Test` · `admin_schedule_filters_cover_all_combinations` — 운영진 조회는 빈 ID, 기간 조합, pending 포함, 명시 상태 필터를 구분한다
4. [L124](../../../src/test/java/com/umc/product/schedule/adapter/out/persistence/ScheduleQueryRepositoryTest.java#L124) · `@Test` · `participant_detail_filters` — 참여자 상세 조회는 빈 입력·단건·batch·상태 필터와 left join을 처리한다
5. [L147](../../../src/test/java/com/umc/product/schedule/adapter/out/persistence/ScheduleQueryRepositoryTest.java#L147) · `@Test` · `participant_ids_exists_and_bulk_delete` — 참여자·일정 ID와 출석 기록 존재 여부를 조회하고 schedule 단위로 벌크 삭제한다

### REST / Web

#### ScheduleDtoTest (12개, 신규 파일)

- 위치: `src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java`

1. [L49](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L49) · `@Test` · `create_request_to_entity` — 생성 request는 위치·정책·참여자를 command와 entity로 손실 없이 변환한다
2. [L71](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L71) · `@Test` · `create_request_normalizes_optional_fields` — 생성 request의 선택 필드가 없으면 빈 참여자와 온라인·무출석 일정으로 변환한다
3. [L89](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L89) · `@Test` · `edit_request_to_command` — 수정 request는 모든 선택 필드를 command로 변환한다
4. [L112](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L112) · `@Test` · `edit_command_validates_mode_changes` — 수정 command는 온라인·대면·출석 정책 조합의 모순을 거부한다
5. [L128](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L128) · `@Test` · `attendance_requests_to_commands` — 출석 관련 request는 경로와 현재 회원 식별자를 command에 결합한다
6. [L152](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L152) · `@Test` · `policy_request_time_order` — 출석 정책 request는 null을 validation에 위임하고 정상·역전 순서를 구분한다
7. [L166](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L166) · `@Test` · `schedule_info_and_response_mapping` — 일정 query info와 일반 response는 참여 여부·내 출석·중첩 위치·정책을 계산한다
8. [L189](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L189) · `@Test` · `online_schedule_nested_info_is_null` — 온라인·무출석 일정의 query response는 중첩 정보를 null로 유지한다
9. [L205](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L205) · `@Test` · `admin_info_and_response_mapping` — 운영진 info와 response는 참석자의 민감 출석 필드를 포함한다
10. [L224](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L224) · `@Test` · `attendance_result_response_mapping` — 출석 결과 response는 결정자 존재 여부에 따라 중첩 정보를 변환한다
11. [L252](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L252) · `@Test` · `capabilities_factories_and_response` — 일정 생성 capability의 모든 role별 제한을 response에 보존한다
12. [L275](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/dto/ScheduleDtoTest.java#L275) · `@Test` · `base_info_mapping` — ScheduleBaseInfo는 직접 변환해 일정 기본 필드를 보존한다

#### ScheduleControllerTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/schedule/adapter/in/web/v2/ScheduleControllerTest.java`

1. [L83](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/ScheduleControllerTest.java#L83) · `@Test` · `create_and_edit_delegate_commands` — 생성·수정 request에 현재 회원과 경로 ID를 결합해 usecase에 전달한다
2. [L107](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/ScheduleControllerTest.java#L107) · `@Test` · `delete_and_force_delete_delegate` — 일반 삭제와 강제 삭제를 구분해 위임한다
3. [L117](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/ScheduleControllerTest.java#L117) · `@Test` · `attendance_and_excuse_mapping` — 출석·사유 요청은 현재 회원 command와 결과 response를 변환한다
4. [L142](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/ScheduleControllerTest.java#L142) · `@Test` · `decisions_preserve_order` — 출석 결정 목록은 입력 순서대로 command와 response를 변환한다
5. [L165](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/ScheduleControllerTest.java#L165) · `@Test` · `basic_queries_map_responses` — capability·내 일정·상세 query 결과를 response로 변환한다
6. [L183](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/ScheduleControllerTest.java#L183) · `@Test` · `admin_list_uses_explicit_period` — 운영진 목록은 제공된 기간을 유지하고 결과를 response로 변환한다
7. [L199](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/ScheduleControllerTest.java#L199) · `@Test` · `admin_list_applies_default_period` — 운영진 목록의 생략 기간은 현재 기준 30일 전부터 24시간 후까지 계산한다
8. [L221](../../../src/test/java/com/umc/product/schedule/adapter/in/web/v2/ScheduleControllerTest.java#L221) · `@Test` · `admin_detail_maps_response` — 운영진 단건 출석 조회를 현재 회원·상태와 결합한다

## 3.19 `curriculum` — 63개

### Application DTO / Port

#### CurriculumDtoContractTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/application/port/in/CurriculumDtoContractTest.java`

1. [L48](../../../src/test/java/com/umc/product/curriculum/application/port/in/CurriculumDtoContractTest.java#L48) · `@Test` · `command_records_and_required_identifiers` — legacy command와 미션 command는 필수 식별자 및 nullable 제출을 보존한다
2. [L64](../../../src/test/java/com/umc/product/curriculum/application/port/in/CurriculumDtoContractTest.java#L64) · `@Test` · `workbook_command_defaults_and_values` — legacy workbook command는 실제 값과 UI 호환 기본값을 모두 해석한다
3. [L86](../../../src/test/java/com/umc/product/curriculum/application/port/in/CurriculumDtoContractTest.java#L86) · `@Test` · `query_size_defaults_and_fetch_boundary` — 검색 query는 0·음수 크기 기본값과 양수 fetch 경계를 적용한다
4. [L103](../../../src/test/java/com/umc/product/curriculum/application/port/in/CurriculumDtoContractTest.java#L103) · `@Test` · `projection_and_legacy_info_records` — projection과 legacy info record의 생성 계약을 보존한다
5. [L132](../../../src/test/java/com/umc/product/curriculum/application/port/in/CurriculumDtoContractTest.java#L132) · `@Test` · `new_workbook_info_records` — 신규 workbook info의 모든 중첩 record를 생성할 수 있다

### Application Service

#### CurriculumNotImplementedServiceTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/application/service/command/CurriculumNotImplementedServiceTest.java`

1. [L30](../../../src/test/java/com/umc/product/curriculum/application/service/command/CurriculumNotImplementedServiceTest.java#L30) · `@Test` · `challenger_workbook_operations_are_explicit` — 챌린저 워크북의 네 command는 silent no-op이 아니라 미구현 예외를 반환한다
2. [L44](../../../src/test/java/com/umc/product/curriculum/application/service/command/CurriculumNotImplementedServiceTest.java#L44) · `@Test` · `weekly_best_operations_are_explicit` — 주간 베스트 워크북의 선정·수정·철회는 미구현 예외를 반환한다
3. [L54](../../../src/test/java/com/umc/product/curriculum/application/service/command/CurriculumNotImplementedServiceTest.java#L54) · `@Test` · `submission_operations_are_explicit` — 미션 제출물의 생성·수정·철회는 미구현 예외를 반환한다
4. [L64](../../../src/test/java/com/umc/product/curriculum/application/service/command/CurriculumNotImplementedServiceTest.java#L64) · `@Test` · `feedback_operations_are_explicit` — 미션 피드백의 생성·수정·삭제는 미구현 예외를 반환한다

#### OriginalWorkbookCommandServiceTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/application/service/command/OriginalWorkbookCommandServiceTest.java`

1. [L57](../../../src/test/java/com/umc/product/curriculum/application/service/command/OriginalWorkbookCommandServiceTest.java#L57) · `@Test` · `empty_bulk_short_circuit` — 빈 bulk는 port 호출 없이 빈 ID를 반환한다
2. [L64](../../../src/test/java/com/umc/product/curriculum/application/service/command/OriginalWorkbookCommandServiceTest.java#L64) · `@Test` · `creates_draft_and_ready_in_order` — DRAFT와 READY 원본 워크북을 입력 순서대로 생성한다
3. [L87](../../../src/test/java/com/umc/product/curriculum/application/service/command/OriginalWorkbookCommandServiceTest.java#L87) · `@Test` · `released_initial_status_is_rejected` — RELEASED 상태로 직접 생성할 수 없다
4. [L96](../../../src/test/java/com/umc/product/curriculum/application/service/command/OriginalWorkbookCommandServiceTest.java#L96) · `@Test` · `edits_workbook` — 원본 워크북의 제공된 필드를 수정하고 저장한다
5. [L110](../../../src/test/java/com/umc/product/curriculum/application/service/command/OriginalWorkbookCommandServiceTest.java#L110) · `@Test` · `delete_protects_submissions` — 제출물이 없을 때만 원본 워크북을 삭제한다
6. [L123](../../../src/test/java/com/umc/product/curriculum/application/service/command/OriginalWorkbookCommandServiceTest.java#L123) · `@Test` · `changes_status_in_batch` — batch 상태 변경은 ID로 매핑해 전체를 한 번에 저장한다
7. [L145](../../../src/test/java/com/umc/product/curriculum/application/service/command/OriginalWorkbookCommandServiceTest.java#L145) · `@Test` · `auto_release_is_explicitly_not_implemented` — 자동 배포 미구현 경로는 명시적인 NotImplementedException을 반환한다

#### WeeklyCurriculumCommandServiceTest (7개, 보강 파일)

- 위치: `src/test/java/com/umc/product/curriculum/application/service/command/WeeklyCurriculumCommandServiceTest.java`

1. [L63](../../../src/test/java/com/umc/product/curriculum/application/service/command/WeeklyCurriculumCommandServiceTest.java#L63) · `@Test` · `empty_bulk_short_circuits` — 빈 일괄 생성은 조회와 저장 없이 빈 목록을 반환한다
2. [L72](../../../src/test/java/com/umc/product/curriculum/application/service/command/WeeklyCurriculumCommandServiceTest.java#L72) · `@Test` · `bulk_create_preserves_input_order` — 일괄 생성은 입력 순서대로 생성 ID를 반환한다
3. [L194](../../../src/test/java/com/umc/product/curriculum/application/service/command/WeeklyCurriculumCommandServiceTest.java#L194) · `@Test` · `rejects_already_ended_period` — 이미 종료된 주차는 생성할 수 없다
4. [L206](../../../src/test/java/com/umc/product/curriculum/application/service/command/WeeklyCurriculumCommandServiceTest.java#L206) · `@Test` · `rejects_duplicate_week_and_type` — 같은 커리큘럼·주차·부록 조합은 중복 생성할 수 없다
5. [L360](../../../src/test/java/com/umc/product/curriculum/application/service/command/WeeklyCurriculumCommandServiceTest.java#L360) · `@Test` · `rejects_edit_to_already_ended_period` — 종료 시각을 과거로 변경할 수 없다
6. [L376](../../../src/test/java/com/umc/product/curriculum/application/service/command/WeeklyCurriculumCommandServiceTest.java#L376) · `@Test` · `rejects_duplicate_when_only_week_changes` — 주차만 변경해 기존 부록 여부와 중복되면 수정할 수 없다
7. [L394](../../../src/test/java/com/umc/product/curriculum/application/service/command/WeeklyCurriculumCommandServiceTest.java#L394) · `@Test` · `edits_only_extra_flag_with_effective_week` — 부록 여부만 변경하면 기존 주차 번호로 중복을 검사하고 저장한다

#### CurriculumPermissionEvaluatorTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/application/service/evaluator/CurriculumPermissionEvaluatorTest.java`

1. [L24](../../../src/test/java/com/umc/product/curriculum/application/service/evaluator/CurriculumPermissionEvaluatorTest.java#L24) · `@Test` · `original_workbook_requires_central_member` — 원본 워크북 RELEASE·MANAGE는 중앙 운영진만 허용한다
2. [L44](../../../src/test/java/com/umc/product/curriculum/application/service/evaluator/CurriculumPermissionEvaluatorTest.java#L44) · `@Test` · `submission_read_requires_school_admin_or_super_admin` — 워크북 제출 READ는 SUPER_ADMIN 또는 학교 운영진만 허용하고 그 외 권한은 fail-closed한다

#### CurriculumOtherQueryServiceTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/application/service/query/CurriculumOtherQueryServiceTest.java`

1. [L29](../../../src/test/java/com/umc/product/curriculum/application/service/query/CurriculumOtherQueryServiceTest.java#L29) · `@Test` · `gets_weekly_curriculum_info` — 주차 query는 조회한 entity를 info로 변환한다
2. [L50](../../../src/test/java/com/umc/product/curriculum/application/service/query/CurriculumOtherQueryServiceTest.java#L50) · `@Test` · `not_implemented_queries_are_explicit` — 구현 전 query는 silent null 대신 명시적인 예외를 반환한다

#### CurriculumQueryServiceResidualTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/application/service/query/CurriculumQueryServiceResidualTest.java`

1. [L71](../../../src/test/java/com/umc/product/curriculum/application/service/query/CurriculumQueryServiceResidualTest.java#L71) · `@Test` · `empty_week_short_circuits` — 주차가 없으면 하위 port를 조회하지 않고 빈 진행률을 반환한다
2. [L82](../../../src/test/java/com/umc/product/curriculum/application/service/query/CurriculumQueryServiceResidualTest.java#L82) · `@Test` · `resolves_every_submission_status_and_feedback` — 피드백 없음·FAIL만 존재·PASS 포함 상태를 각각 PENDING·FAIL·PASS로 조립한다

### Domain

#### CurriculumDomainResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/domain/CurriculumDomainResidualTest.java`

1. [L23](../../../src/test/java/com/umc/product/curriculum/domain/CurriculumDomainResidualTest.java#L23) · `@Test` · `curriculum_title_update` — 커리큘럼 제목은 유효한 값만 갱신한다
2. [L37](../../../src/test/java/com/umc/product/curriculum/domain/CurriculumDomainResidualTest.java#L37) · `@Test` · `original_workbook_partial_edit` — 원본 워크북 edit는 제공된 필드만 변경한다
3. [L54](../../../src/test/java/com/umc/product/curriculum/domain/CurriculumDomainResidualTest.java#L54) · `@Test` · `original_workbook_mission_partial_edit` — 원본 워크북 미션 edit는 null을 유지하고 제공된 값을 반영한다
4. [L75](../../../src/test/java/com/umc/product/curriculum/domain/CurriculumDomainResidualTest.java#L75) · `@Test` · `legacy_mission_contract` — legacy 미션과 challenger 미션의 builder 및 제출 수정 계약을 보존한다
5. [L100](../../../src/test/java/com/umc/product/curriculum/domain/CurriculumDomainResidualTest.java#L100) · `@Test` · `persistence_only_entities_have_safe_defaults` — 생성 로직이 없는 persistence 전용 entity도 기본 상태로 안전하게 생성된다
6. [L114](../../../src/test/java/com/umc/product/curriculum/domain/CurriculumDomainResidualTest.java#L114) · `@Test` · `enum_and_error_contract` — enum과 도메인 오류 코드는 외부 계약을 보존한다

### Persistence

#### CurriculumPersistenceAdapterUnitTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumPersistenceAdapterUnitTest.java`

1. [L35](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumPersistenceAdapterUnitTest.java#L35) · `@Test` · `curriculum_adapter_contract` — Curriculum adapter는 Optional/get·존재·저장·삭제 계약을 위임한다
2. [L61](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumPersistenceAdapterUnitTest.java#L61) · `@Test` · `challenger_workbook_adapter_contract` — ChallengerWorkbook adapter는 not-found와 빈 IN을 fail-safe로 처리한다
3. [L83](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumPersistenceAdapterUnitTest.java#L83) · `@Test` · `original_workbook_mission_adapter_contract` — 원본 미션 adapter는 get·빈 batch·저장·삭제 계약을 보존한다
4. [L105](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumPersistenceAdapterUnitTest.java#L105) · `@Test` · `mission_submission_adapter_contract` — 제출·피드백 adapter는 빈 batch를 단축하고 각 repository를 구분해 위임한다
5. [L126](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumPersistenceAdapterUnitTest.java#L126) · `@Test` · `original_workbook_adapter_contract` — OriginalWorkbook adapter는 전체 batch 존재를 검증하고 모든 상태 조회·write를 위임한다
6. [L161](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumPersistenceAdapterUnitTest.java#L161) · `@Test` · `weekly_curriculum_adapter_contract` — WeeklyCurriculum adapter는 week filter 유무와 workbook 상태별 존재 검사를 구분한다
7. [L194](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumPersistenceAdapterUnitTest.java#L194) · `@Test` · `legacy_adapter_contract` — legacy mission·submission adapter는 query 결과를 그대로 보존한다

#### CurriculumQueryRepositoryTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumQueryRepositoryTest.java`

1. [L32](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumQueryRepositoryTest.java#L32) · `@Test` · `finds_curriculum_projection_or_empty` — 기수·파트 projection은 존재와 부재를 Optional로 구분한다
2. [L48](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumQueryRepositoryTest.java#L48) · `@Test` · `checks_workbook_existence_with_optional_status` — 원본 워크북 존재 조회는 상태 미지정·지정과 결과 true·false를 모두 구분한다
3. [L81](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/CurriculumQueryRepositoryTest.java#L81) · `@Test` · `unreleased_query_is_explicitly_not_implemented` — 자동 배포 조회 미구현 경로는 명시적인 예외를 반환한다

#### WorkbookSubmissionQueryRepositoryTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/adapter/out/persistence/WorkbookSubmissionQueryRepositoryTest.java`

1. [L19](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/WorkbookSubmissionQueryRepositoryTest.java#L19) · `@Test` · `explicit_not_implemented_paths` — 미구현 조회와 필수 week 조건은 명시적인 예외를 반환한다
2. [L30](../../../src/test/java/com/umc/product/curriculum/adapter/out/persistence/WorkbookSubmissionQueryRepositoryTest.java#L30) · `@Test` · `nullable_filter_boundaries` — nullable 동적 filter는 미제공 시 null, 제공 시 BooleanExpression을 반환한다

### REST / Web

#### CurriculumControllerUnitTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/adapter/in/web/v2/CurriculumControllerUnitTest.java`

1. [L51](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/CurriculumControllerUnitTest.java#L51) · `@Test` · `delegates_curriculum_and_weekly_commands` — 커리큘럼 controller는 path와 request를 command로 변환해 CUD를 위임한다
2. [L77](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/CurriculumControllerUnitTest.java#L77) · `@Test` · `delegates_original_workbook_commands` — 원본 워크북 controller는 READY·DRAFT 생성과 수정·삭제·상태 batch를 구분한다
3. [L110](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/CurriculumControllerUnitTest.java#L110) · `@Test` · `delegates_empty_status_batch` — 빈 상태 batch도 controller에서 빈 목록으로 안전하게 위임한다
4. [L120](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/CurriculumControllerUnitTest.java#L120) · `@Test` · `delegates_original_mission_commands` — 원본 미션 controller는 생성 응답과 path 결합 수정·삭제를 위임한다
5. [L142](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/CurriculumControllerUnitTest.java#L142) · `@Test` · `delegates_curriculum_queries` — query controller는 공개 필터와 현재 member ID를 use case에 전달하고 응답으로 변환한다
6. [L156](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/CurriculumControllerUnitTest.java#L156) · `@Test` · `challenger_workbook_controller_is_explicit` — 구현 전 challenger workbook controller의 모든 endpoint는 명시적인 예외를 반환한다
7. [L172](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/CurriculumControllerUnitTest.java#L172) · `@Test` · `remaining_controllers_are_explicit` — 구현 전 mission과 workbook query endpoint도 모두 명시적인 예외를 반환한다

#### CurriculumRequestContractTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/adapter/in/web/v2/dto/request/CurriculumRequestContractTest.java`

1. [L24](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/dto/request/CurriculumRequestContractTest.java#L24) · `@Test` · `converts_curriculum_requests` — 커리큘럼과 주차 생성·수정 request는 모든 필드를 command로 보존한다
2. [L44](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/dto/request/CurriculumRequestContractTest.java#L44) · `@Test` · `converts_workbook_and_mission_requests` — 원본 워크북과 미션 request는 path·requester·상태 값을 command에 결합한다
3. [L71](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/dto/request/CurriculumRequestContractTest.java#L71) · `@Test` · `preserves_plain_request_records` — 현재 command 변환이 없는 request도 직렬화 계약 필드를 보존한다

#### CurriculumResponseContractTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/adapter/in/web/v2/dto/response/CurriculumResponseContractTest.java`

1. [L26](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/dto/response/CurriculumResponseContractTest.java#L26) · `@Test` · `converts_overview` — 개요 info의 주차 순서와 필드를 응답으로 보존한다
2. [L44](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/dto/response/CurriculumResponseContractTest.java#L44) · `@Test` · `converts_nested_progress_with_submission_and_feedback` — 배포·제출·피드백이 있는 전체 중첩 info를 누락 없이 응답으로 변환한다
3. [L82](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/dto/response/CurriculumResponseContractTest.java#L82) · `@Test` · `converts_absent_nested_data` — 미배포·미제출 상태는 NOT_STARTED와 null 중첩 값으로 변환한다
4. [L106](../../../src/test/java/com/umc/product/curriculum/adapter/in/web/v2/dto/response/CurriculumResponseContractTest.java#L106) · `@Test` · `plain_response_contracts` — 정적 변환이 없는 응답 DTO와 enum도 생성·직렬화 계약을 유지한다

### Scheduler

#### WorkbookAutoReleaseSchedulerTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/curriculum/adapter/in/scheduler/WorkbookAutoReleaseSchedulerTest.java`

1. [L17](../../../src/test/java/com/umc/product/curriculum/adapter/in/scheduler/WorkbookAutoReleaseSchedulerTest.java#L17) · `@Test` · `records_success_metric` — 자동 배포 성공 시 처리 건수와 non-negative duration을 기록한다
2. [L34](../../../src/test/java/com/umc/product/curriculum/adapter/in/scheduler/WorkbookAutoReleaseSchedulerTest.java#L34) · `@Test` · `records_failure_metric_without_rethrowing` — 자동 배포 실패를 삼키되 failure metric을 0건으로 기록한다

## 3.20 `form` — 100개

### Application Service

#### AnswerCommandServiceResidualTest (14개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java`

1. [L76](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L76) · `@Test` · `updates_named_answer_and_keeps_null_files` — 기명 답변 수정은 기존 choice를 지우고 Answer PK를 유지해 저장한다
2. [L93](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L93) · `@Test` · `updates_named_choice_and_replaces_files` — 기명 객관식 수정은 새 파일 집합과 choice를 함께 교체한다
3. [L113](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L113) · `@Test` · `deletes_named_answer` — 기명 답변 삭제는 answer를 제거하고 draft 저장 시각을 갱신한다
4. [L129](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L129) · `@Test` · `updates_anonymous_choices_and_clears_files` — 익명 CHECKBOX 수정은 hash를 검증하고 파일을 비우며 choices를 교체한다
5. [L154](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L154) · `@Test` · `deletes_anonymous_answer` — 익명 답변 삭제 성공은 hash 일치 후 answer ID를 제거한다
6. [L169](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L169) · `@Test` · `creates_anonymous_choice` — 익명 객관식 생성은 access key와 option 소속을 확인한 뒤 choice를 저장한다
7. [L195](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L195) · `@Test` · `missing_and_non_draft_boundaries` — 없는·제출 완료 answer와 없는·제출 완료 response는 수정 경계에서 구분한다
8. [L224](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L224) · `@Test` · `question_presence_and_ownership` — 질문 부재와 다른 폼 소속 질문은 각각 NOT_FOUND와 NOT_OWNED를 반환한다
9. [L241](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L241) · `@Test` · `rejects_duplicate_named_and_anonymous_answers` — 같은 질문의 기명·익명 중복 답변 생성을 모두 거부한다
10. [L260](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L260) · `@Test` · `validates_text_types_and_ignores_irrelevant_choices` — SHORT/LONG 텍스트는 non-blank만 허용하고 non-objective 선택값은 choice로 저장하지 않는다
11. [L271](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L271) · `@Test` · `validates_single_choice_types` — RADIO/DROPDOWN은 정확히 한 개이면서 소속 질문의 option만 허용한다
12. [L284](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L284) · `@Test` · `validates_multiple_choice_type_and_loaded_options` — CHECKBOX는 한 개 이상 소속 option만 허용하고 조회 결과 누락도 거부한다
13. [L297](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L297) · `@Test` · `validates_file_and_portfolio_types` — FILE은 파일을 필수로 검증·중복 제거해 저장하고 PORTFOLIO는 텍스트 또는 파일을 허용한다
14. [L313](../../../src/test/java/com/umc/product/form/application/service/command/AnswerCommandServiceResidualTest.java#L313) · `@Test` · `schedule_is_explicitly_unsupported` — SCHEDULE은 지원 전임을 명시적인 예외로 알린다

#### FormCommandServiceResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/command/FormCommandServiceResidualTest.java`

1. [L59](../../../src/test/java/com/umc/product/form/application/service/command/FormCommandServiceResidualTest.java#L59) · `@Test` · `updates_form_metadata` — 폼 PATCH는 clearDescription과 중복 응답 정책을 적용하고 저장한다
2. [L76](../../../src/test/java/com/umc/product/form/application/service/command/FormCommandServiceResidualTest.java#L76) · `@Test` · `publishes_form` — 폼 발행은 DRAFT를 PUBLISHED로 변경해 저장한다
3. [L88](../../../src/test/java/com/umc/product/form/application/service/command/FormCommandServiceResidualTest.java#L88) · `@Test` · `missing_form_paths` — 수정·발행·발행취소·종료 대상이 없으면 동일한 FORM_NOT_FOUND를 반환한다
4. [L99](../../../src/test/java/com/umc/product/form/application/service/command/FormCommandServiceResidualTest.java#L99) · `@Test` · `deletes_full_form_tree_in_dependency_order` — 폼 삭제는 응답 트리와 구조를 자식부터 제거한 뒤 폼을 삭제한다

#### FormResponseCommandServiceResidualTest (18개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java`

1. [L91](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L91) · `@Test` · `updates_named_submitted_response_and_saves_empty_answers` — 기명 제출 응답 수정은 기존 답변을 교체하고 allowed 미답변을 snapshot으로 저장한다
2. [L118](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L118) · `@Test` · `deletes_named_submitted_response` — 기명 제출 응답 삭제는 answer를 먼저 지우고 response를 삭제한다
3. [L134](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L134) · `@Test` · `updates_named_draft_with_choices` — 기명 draft 수정은 객관식 option snapshot을 포함해 전체 답변을 교체한다
4. [L157](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L157) · `@Test` · `deletes_named_draft` — 기명 draft 삭제는 answer와 response를 함께 제거한다
5. [L170](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L170) · `@Test` · `updates_anonymous_submitted_response_with_files` — 익명 제출 응답 수정은 파일을 검증·중복 제거해 답변을 교체한다
6. [L194](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L194) · `@Test` · `deletes_anonymous_submitted_response` — 익명 제출 응답 삭제는 access key 검증 뒤 answer와 response를 제거한다
7. [L207](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L207) · `@Test` · `updates_anonymous_draft_with_checkbox_choices` — 익명 draft 수정은 CHECKBOX 선택값을 검증하고 snapshot choices로 교체한다
8. [L233](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L233) · `@Test` · `submits_anonymous_draft_with_multiple_choices` — 익명 draft 제출은 여러 choice 중 첫 값을 경로 계산에 사용하고 SUBMITTED로 전이한다
9. [L264](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L264) · `@Test` · `deletes_anonymous_draft` — 익명 draft 삭제는 access key 검증 뒤 answer와 response를 제거한다
10. [L277](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L277) · `@Test` · `creates_named_draft_when_duplicate_does_not_exist` — 일반 기명 draft 생성은 중복이 없을 때 새 응답 ID를 반환한다
11. [L295](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L295) · `@Test` · `distinguishes_missing_and_non_draft_boundaries` — 없는 form·제출 응답·draft와 이미 제출된 draft 접근을 구분한다
12. [L326](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L326) · `@Test` · `rejects_invalid_answer_collections` — answer 목록은 null question·중복 question·다른 form 질문을 거부한다
13. [L346](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L346) · `@Test` · `validates_objective_and_file_formats` — 객관식·파일 답변은 개수와 질문 소속을 fail-closed로 검증한다
14. [L375](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L375) · `@Test` · `validates_portfolio_alternatives` — PORTFOLIO는 text 또는 검증된 file을 허용하고 둘 다 없으면 거부한다
15. [L393](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L393) · `@Test` · `schedule_is_explicitly_unsupported` — SCHEDULE은 지원 전임을 명시적인 예외로 알린다
16. [L408](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L408) · `@Test` · `build_phase_revalidates_question_and_option_snapshots` — validation 이후 질문·option snapshot이 사라진 저장 race도 fail-closed로 처리한다
17. [L427](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L427) · `@Test` · `resolves_jump_and_sequential_paths` — 조건부 경로는 선택 jump와 선택 없는 순차 이동을 모두 계산한다
18. [L470](../../../src/test/java/com/umc/product/form/application/service/command/FormResponseCommandServiceResidualTest.java#L470) · `@Test` · `submits_empty_anonymous_draft_when_nothing_is_required` — 저장된 답변이 없는 익명 draft도 optional 구조에서는 제출할 수 있다

#### FormSectionCommandServiceTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/command/FormSectionCommandServiceTest.java`

1. [L55](../../../src/test/java/com/umc/product/form/application/service/command/FormSectionCommandServiceTest.java#L55) · `@Test` · `creates_after_max_order` — 기존 최대 순서 다음 번호로 섹션을 생성한다
2. [L76](../../../src/test/java/com/umc/product/form/application/service/command/FormSectionCommandServiceTest.java#L76) · `@Test` · `first_order_and_missing_form` — 첫 섹션은 1번이며 없는 폼은 FORM_NOT_FOUND를 반환한다
3. [L93](../../../src/test/java/com/umc/product/form/application/service/command/FormSectionCommandServiceTest.java#L93) · `@Test` · `updates_or_rejects_missing_section` — 섹션 수정은 clearDescription을 적용하고 없는 섹션은 예외를 반환한다
4. [L110](../../../src/test/java/com/umc/product/form/application/service/command/FormSectionCommandServiceTest.java#L110) · `@Test` · `deletes_children_before_section` — 섹션 삭제는 선택지→질문→섹션 순서로 cascade한다
5. [L121](../../../src/test/java/com/umc/product/form/application/service/command/FormSectionCommandServiceTest.java#L121) · `@Test` · `reorders_all_sections` — 재배치는 입력 순서를 1부터 부여하고 저장한다
6. [L137](../../../src/test/java/com/umc/product/form/application/service/command/FormSectionCommandServiceTest.java#L137) · `@Test` · `rejects_invalid_reorder_sets` — 재배치의 누락·중복·외부 ID는 모두 거부한다

#### QuestionCommandServiceResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/command/QuestionCommandServiceResidualTest.java`

1. [L57](../../../src/test/java/com/umc/product/form/application/service/command/QuestionCommandServiceResidualTest.java#L57) · `@Test` · `creates_after_max_order_or_rejects_missing_section` — 기존 최대 순서 다음에 질문을 생성하고 없는 섹션은 거부한다
2. [L84](../../../src/test/java/com/umc/product/form/application/service/command/QuestionCommandServiceResidualTest.java#L84) · `@Test` · `update_type_change_cleanup_matrix` — 질문 PATCH는 같은 타입은 유지하고 객관식에서 주관식으로 바뀔 때만 옵션을 정리한다
3. [L113](../../../src/test/java/com/umc/product/form/application/service/command/QuestionCommandServiceResidualTest.java#L113) · `@Test` · `deletes_dependencies_before_question` — 질문 삭제는 답변→선택지→질문 순서로 cascade한다
4. [L124](../../../src/test/java/com/umc/product/form/application/service/command/QuestionCommandServiceResidualTest.java#L124) · `@Test` · `reorder_requires_exact_unique_ids` — 질문 재배치는 전체 ID를 한 번씩 제공한 경우에만 순서를 저장한다
5. [L143](../../../src/test/java/com/umc/product/form/application/service/command/QuestionCommandServiceResidualTest.java#L143) · `@Test` · `deactivates_or_rejects_missing_question` — 질문 비활성화는 저장하고 없는 질문은 거부한다
6. [L156](../../../src/test/java/com/umc/product/form/application/service/command/QuestionCommandServiceResidualTest.java#L156) · `@Test` · `forks_and_deactivates_origin` — fork는 원본 snapshot을 같은 섹션에 저장하고 원본을 비활성화한다

#### QuestionOptionCommandServiceResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/command/QuestionOptionCommandServiceResidualTest.java`

1. [L54](../../../src/test/java/com/umc/product/form/application/service/command/QuestionOptionCommandServiceResidualTest.java#L54) · `@Test` · `creates_after_max_order_without_branch_target` — 선택지는 기존 최대 순서 다음에 생성되며 next section이 없으면 추가 검증을 생략한다
2. [L75](../../../src/test/java/com/umc/product/form/application/service/command/QuestionOptionCommandServiceResidualTest.java#L75) · `@Test` · `rejects_missing_question_and_unsupported_type` — 없는 질문과 비객관식 질문의 next section은 거부한다
3. [L90](../../../src/test/java/com/umc/product/form/application/service/command/QuestionOptionCommandServiceResidualTest.java#L90) · `@Test` · `validates_target_exists_and_same_form` — next section은 존재하고 같은 폼에 속해야 하며 DROPDOWN도 허용한다
4. [L116](../../../src/test/java/com/umc/product/form/application/service/command/QuestionOptionCommandServiceResidualTest.java#L116) · `@Test` · `updates_clears_or_rejects_missing_option` — 선택지 PATCH는 clear next section과 일반 값 변경을 구분하고 없는 선택지를 거부한다
5. [L141](../../../src/test/java/com/umc/product/form/application/service/command/QuestionOptionCommandServiceResidualTest.java#L141) · `@Test` · `deletes_by_id` — 선택지 삭제는 ID를 그대로 위임한다
6. [L148](../../../src/test/java/com/umc/product/form/application/service/command/QuestionOptionCommandServiceResidualTest.java#L148) · `@Test` · `reorder_requires_exact_unique_ids` — 선택지 재배치는 전체 ID를 한 번씩 제공한 경우에만 저장한다

#### AnswerQueryServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/query/AnswerQueryServiceTest.java`

1. [L85](../../../src/test/java/com/umc/product/form/application/service/query/AnswerQueryServiceTest.java#L85) · `@Test` · `getById_기명_답변_반환` — getById: 기명 답변은 choice와 함께 반환
2. [L112](../../../src/test/java/com/umc/product/form/application/service/query/AnswerQueryServiceTest.java#L112) · `@Test` · `listByFormResponseId_기명_답변_choice_조립` — listByFormResponseId: 기명 답변은 choice를 bulk 조회해 조립
3. [L127](../../../src/test/java/com/umc/product/form/application/service/query/AnswerQueryServiceTest.java#L127) · `@Test` · `listByFormResponseIds_빈_입력과_익명만_있으면_빈_map` — listByFormResponseIds: null·empty와 익명 전용 결과는 빈 map
4. [L138](../../../src/test/java/com/umc/product/form/application/service/query/AnswerQueryServiceTest.java#L138) · `@Test` · `selected_option_preserves_snapshot_after_option_deletion` — 삭제된 option의 choice는 snapshot content와 null ID로 반환

#### FormQueryServiceResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/query/FormQueryServiceResidualTest.java`

1. [L48](../../../src/test/java/com/umc/product/form/application/service/query/FormQueryServiceResidualTest.java#L48) · `@Test` · `find_get_contract` — find/get은 FormInfo로 변환하고 get 부재를 FORM_NOT_FOUND로 변환한다
2. [L64](../../../src/test/java/com/umc/product/form/application/service/query/FormQueryServiceResidualTest.java#L64) · `@Test` · `batch_empty_short_circuit` — batch null·empty는 모든 port 호출 없이 빈 map을 반환한다
3. [L72](../../../src/test/java/com/umc/product/form/application/service/query/FormQueryServiceResidualTest.java#L72) · `@Test` · `batch_deduplicates_and_assembles_partial_structures` — batch는 중복 form ID를 입력 순서로 제거하고 부분 구조를 각 폼에 조립한다
4. [L97](../../../src/test/java/com/umc/product/form/application/service/query/FormQueryServiceResidualTest.java#L97) · `@Test` · `structure_not_found_paths` — 구조 조회와 question 범위 구조 조회는 없는 폼을 동일하게 거부한다

#### FormResponseQueryServiceResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/query/FormResponseQueryServiceResidualTest.java`

1. [L42](../../../src/test/java/com/umc/product/form/application/service/query/FormResponseQueryServiceResidualTest.java#L42) · `@Test` · `exists_find_get_contract` — exists/find/get은 port 결과를 보존하고 get 부재만 예외로 변환한다
2. [L57](../../../src/test/java/com/umc/product/form/application/service/query/FormResponseQueryServiceResidualTest.java#L57) · `@Test` · `list_and_member_lookup_contract` — form·상태·응답자별 목록과 단건 조회를 info로 변환한다
3. [L74](../../../src/test/java/com/umc/product/form/application/service/query/FormResponseQueryServiceResidualTest.java#L74) · `@Test` · `response_with_answers_contract` — 기명 응답 상세은 answer 목록을 결합하고 없는 ID는 거부한다
4. [L87](../../../src/test/java/com/umc/product/form/application/service/query/FormResponseQueryServiceResidualTest.java#L87) · `@Test` · `batch_empty_and_partial_answer_map` — batch 상세 조회는 null·empty를 단축하고 answer 누락은 빈 목록으로 조립한다

#### FormStructureLeafQueryServiceTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/query/FormStructureLeafQueryServiceTest.java`

1. [L29](../../../src/test/java/com/umc/product/form/application/service/query/FormStructureLeafQueryServiceTest.java#L29) · `@Test` · `section_query_contract` — section query는 find/get/list를 info로 변환하고 get 부재를 거부한다
2. [L49](../../../src/test/java/com/umc/product/form/application/service/query/FormStructureLeafQueryServiceTest.java#L49) · `@Test` · `question_query_contract` — question query는 find/get/list를 info로 변환하고 get 부재를 거부한다
3. [L69](../../../src/test/java/com/umc/product/form/application/service/query/FormStructureLeafQueryServiceTest.java#L69) · `@Test` · `option_query_contract` — option query는 find/get/list를 info로 변환하고 get 부재를 거부한다

#### VoteQueryServiceTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/query/VoteQueryServiceTest.java`

1. [L53](../../../src/test/java/com/umc/product/form/application/service/query/VoteQueryServiceTest.java#L53) · `@Test` · `assembles_named_vote_statistics` — 기명 RADIO 투표는 득표율·내 선택·옵션별 선택 member를 조립한다
2. [L75](../../../src/test/java/com/umc/product/form/application/service/query/VoteQueryServiceTest.java#L75) · `@Test` · `anonymous_vote_hides_members_and_handles_zero_participants` — 익명 CHECKBOX 투표는 member 목록을 조회하지 않고 참여자 0명 득표율을 0.0으로 반환한다
3. [L95](../../../src/test/java/com/umc/product/form/application/service/query/VoteQueryServiceTest.java#L95) · `@Test` · `missing_structure_paths` — 폼·섹션·질문 누락은 명시적인 구조 오류로 fail-closed한다
4. [L114](../../../src/test/java/com/umc/product/form/application/service/query/VoteQueryServiceTest.java#L114) · `@Test` · `primary_question_contract` — primary question은 첫 질문 ID를 반환하고 빈 구조는 거부한다

#### VoteServiceTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/application/service/VoteServiceTest.java`

1. [L30](../../../src/test/java/com/umc/product/form/application/service/VoteServiceTest.java#L30) · `@Test` · `creates_multiple_choice_vote_structure` — 복수 선택 투표는 PUBLISHED Form·단일 section·CHECKBOX question·순서화된 option을 생성한다
2. [L62](../../../src/test/java/com/umc/product/form/application/service/VoteServiceTest.java#L62) · `@Test` · `creates_single_choice_vote_with_empty_options` — 단일 선택과 빈 option 투표는 RADIO 및 빈 저장 목록으로 생성한다
3. [L82](../../../src/test/java/com/umc/product/form/application/service/VoteServiceTest.java#L82) · `@Test` · `delete_delegates_to_form_cascade` — 투표 삭제는 form 전체 cascade 삭제 command로 위임한다

### Domain

#### FormDomainResidualTest (9개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java`

1. [L23](../../../src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java#L23) · `@Test` · `form_factories_preserve_defaults_and_explicit_values` — draft·published factory는 설명·익명·중복 응답 기본값과 명시값을 구분한다
2. [L42](../../../src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java#L42) · `@Test` · `form_transition_matrix` — 폼은 DRAFT→PUBLISHED→DRAFT 및 PUBLISHED→CLOSED 전이만 허용한다
3. [L60](../../../src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java#L60) · `@Test` · `form_patch_semantics` — 폼 PATCH는 null 유지·빈 값 저장·description 명시 삭제를 구분한다
4. [L76](../../../src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java#L76) · `@Test` · `form_response_draft_submit_and_idempotency` — 기명·익명 응답 draft와 제출은 식별 정보·시간·멱등성을 보존한다
5. [L99](../../../src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java#L99) · `@Test` · `form_section_patch_semantics` — section PATCH와 순서 변경은 null 유지·빈 값·명시 삭제를 구분한다
6. [L115](../../../src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java#L115) · `@Test` · `question_lifecycle_and_fork_snapshot` — 질문 생성·fork·PATCH·비활성화는 원본과 독립된 스냅샷을 만든다
7. [L142](../../../src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java#L142) · `@Test` · `question_option_lifecycle` — 선택지 생성·PATCH·분기 대상 삭제·질문 할당·순서 변경을 지원한다
8. [L163](../../../src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java#L163) · `@Test` · `answer_and_choice_semantics` — 답변 PATCH는 null 유지·blank/empty 삭제·값 교체를 구분하고 선택지 문구를 snapshot한다
9. [L189](../../../src/test/java/com/umc/product/form/domain/FormDomainResidualTest.java#L189) · `@Test` · `enums_and_error_contract` — enum과 모든 오류 코드는 안정적인 외부 오류 계약을 가진다

### Persistence

#### AnswerPersistenceAdapterTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/adapter/out/persistence/AnswerPersistenceAdapterTest.java`

1. [L41](../../../src/test/java/com/umc/product/form/adapter/out/persistence/AnswerPersistenceAdapterTest.java#L41) · `@Test` · `delegates_load_and_statistics` — 답변·choice 조회와 통계를 올바른 repository에 위임한다
2. [L69](../../../src/test/java/com/umc/product/form/adapter/out/persistence/AnswerPersistenceAdapterTest.java#L69) · `@Test` · `delegates_save_and_preserves_delete_order` — 저장은 대상별 repository에 위임하고 cascade 삭제는 choice를 먼저 제거한다

#### FormPersistenceAdapterTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/adapter/out/persistence/FormPersistenceAdapterTest.java`

1. [L28](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormPersistenceAdapterTest.java#L28) · `@Test` · `delegates_single_operations` — 단건 저장·조회·삭제를 JPA repository에 위임한다
2. [L43](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormPersistenceAdapterTest.java#L43) · `@Test` · `batch_get_short_circuits_and_deduplicates` — batch 조회는 빈 입력을 단축하고 중복 ID를 최초 순서대로 제거한다
3. [L58](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormPersistenceAdapterTest.java#L58) · `@Test` · `batch_get_requires_all_forms` — batch 결과에 누락된 폼이 있으면 전체 조회 실패로 처리한다

#### FormQueryRepositoriesTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/adapter/out/persistence/FormQueryRepositoriesTest.java`

1. [L54](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormQueryRepositoriesTest.java#L54) · `@Test` · `empty_batch_inputs_short_circuit` — 빈 batch 입력은 SQL 없이 빈 결과를 반환한다
2. [L70](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormQueryRepositoriesTest.java#L70) · `@Test` · `structure_queries_preserve_order_and_active_filter` — 섹션·질문·선택지는 orderNo와 active 계약에 따라 조회된다
3. [L113](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormQueryRepositoriesTest.java#L113) · `@Test` · `response_and_answer_queries_preserve_contracts` — 응답과 답변은 상태 필터·화면 순서·batch fetch 계약을 보존한다
4. [L153](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormQueryRepositoriesTest.java#L153) · `@Test` · `choice_queries_only_aggregate_submitted_responses` — 투표 집계는 제출 응답만 포함하고 option 순서와 member grouping을 보존한다

#### FormResponsePersistenceAdapterTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/adapter/out/persistence/FormResponsePersistenceAdapterTest.java`

1. [L29](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormResponsePersistenceAdapterTest.java#L29) · `@Test` · `delegates_load_operations` — 응답 조회 port는 상태와 access key 계약을 보존해 위임한다
2. [L74](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormResponsePersistenceAdapterTest.java#L74) · `@Test` · `delegates_save_and_delete_operations` — 응답 저장·일괄 삭제 port를 위임하고 삭제 건수를 반환한다

#### FormSectionPersistenceAdapterTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/adapter/out/persistence/FormSectionPersistenceAdapterTest.java`

1. [L27](../../../src/test/java/com/umc/product/form/adapter/out/persistence/FormSectionPersistenceAdapterTest.java#L27) · `@Test` · `delegates_all_operations` — 저장·조회·목록·삭제 port를 각각 올바른 repository에 위임한다

#### QuestionOptionPersistenceAdapterTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/adapter/out/persistence/QuestionOptionPersistenceAdapterTest.java`

1. [L31](../../../src/test/java/com/umc/product/form/adapter/out/persistence/QuestionOptionPersistenceAdapterTest.java#L31) · `@Test` · `delegates_all_operations` — 저장·조회·목록·삭제 port를 각각 올바른 repository에 위임한다

#### QuestionPersistenceAdapterTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/form/adapter/out/persistence/QuestionPersistenceAdapterTest.java`

1. [L33](../../../src/test/java/com/umc/product/form/adapter/out/persistence/QuestionPersistenceAdapterTest.java#L33) · `@Test` · `scoped_delete_requires_existing_question` — 질문 삭제는 실제 삭제 건수가 없을 때 NOT_FOUND를 반환한다
2. [L46](../../../src/test/java/com/umc/product/form/adapter/out/persistence/QuestionPersistenceAdapterTest.java#L46) · `@Test` · `delegates_all_operations` — 저장·조회·목록·삭제 port를 각각 올바른 repository에 위임한다

## 3.21 `project` — 130개

### Application Service

#### ProjectApplicationCommandServiceTest (11개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java`

1. [L157](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L157) · `@Test` · `모집중인_파트와_차수에_새_지원서_초안을_생성한다` — 모집중인_파트와_차수에_새_지원서_초안을_생성한다
2. [L181](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L181) · `@Test` · `지원폼이_없으면_생성을_거부한다` — 지원폼이_없으면_생성을_거부한다
3. [L188](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L188) · `@Test` · `모집중이_아니거나_자기_프로젝트면_생성을_거부한다` — 모집중이_아니거나_자기_프로젝트면_생성을_거부한다
4. [L199](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L199) · `@Test` · `모집하지_않는_파트거나_이미_팀원이면_생성을_거부한다` — 모집하지_않는_파트거나_이미_팀원이면_생성을_거부한다
5. [L213](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L213) · `@Test` · `닫힌_차수거나_파트와_차수_type이_다르면_생성을_거부한다` — 닫힌_차수거나_파트와_차수_type이_다르면_생성을_거부한다
6. [L228](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L228) · `@Test` · `같은_차수의_DRAFT가_이미_있으면_중복_생성을_거부한다` — 같은_차수의_DRAFT가_이미_있으면_중복_생성을_거부한다
7. [L242](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L242) · `@Test` · `디자인_파트는_PLAN_DESIGN_차수에_초안을_생성한다` — 디자인_파트는_PLAN_DESIGN_차수에_초안을_생성한다
8. [L267](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L267) · `@Test` · `submit은_닫힌_차수와_동일차수_중복제출을_거부한다` — submit은_닫힌_차수와_동일차수_중복제출을_거부한다
9. [L285](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L285) · `@Test` · `SUPER_ADMIN은_빈_지원자_목록에서도_강제_거절할_수_있다` — SUPER_ADMIN은_빈_지원자_목록에서도_강제_거절할_수_있다
10. [L299](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L299) · `@Test` · `지원_정책이_없으면_거절_결정을_fail_closed한다` — 지원_정책이_없으면_거절_결정을_fail_closed한다
11. [L313](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationCommandServiceTest.java#L313) · `@Test` · `update는_null_ID_미존재_ID_null_답변과_중복_정책을_안전하게_처리한다` — update는_null_ID_미존재_ID_null_답변과_중복_정책을_안전하게_처리한다

#### ProjectApplicationFormCommandServiceTest (6개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/command/ProjectApplicationFormCommandServiceTest.java`

1. [L300](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationFormCommandServiceTest.java#L300) · `@Test` · `IN_PROGRESS_차수_사이에서_삭제된_질문은_비활성화한다` — IN_PROGRESS_차수_사이에서_삭제된_질문은_비활성화한다
2. [L480](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationFormCommandServiceTest.java#L480) · `@Test` · `PART_정책의_allowedParts가_바뀌면_정책을_갱신한다` — PART_정책의_allowedParts가_바뀌면_정책을_갱신한다
3. [L532](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationFormCommandServiceTest.java#L532) · `@Test` · `기존_섹션에_새_질문과_옵션을_추가하고_순서를_반영한다` — 기존_섹션에_새_질문과_옵션을_추가하고_순서를_반영한다
4. [L554](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationFormCommandServiceTest.java#L554) · `@Test` · `기존_질문에_새_옵션을_추가한다` — 기존_질문에_새_옵션을_추가한다
5. [L820](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationFormCommandServiceTest.java#L820) · `@Test` · `기존_질문에_없는_optionId면_INVALID_OPTION_ID` — 기존_질문에_없는_optionId면_INVALID_OPTION_ID
6. [L834](../../../src/test/java/com/umc/product/project/application/service/command/ProjectApplicationFormCommandServiceTest.java#L834) · `@Test` · `신규_섹션의_새_질문에_optionId가_있으면_INVALID_OPTION_ID` — 신규_섹션의_새_질문에_optionId가_있으면_INVALID_OPTION_ID

#### ProjectCommandServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/command/ProjectCommandServiceTest.java`

1. [L292](../../../src/test/java/com/umc/product/project/application/service/command/ProjectCommandServiceTest.java#L292) · `@Test` · `SUPER_ADMIN이_다른_PLAN_챌린저를_PO로_지정하면_성공` — SUPER_ADMIN이_다른_PLAN_챌린저를_PO로_지정하면_성공

#### ProjectMatchingRoundCommandServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/command/ProjectMatchingRoundCommandServiceTest.java`

1. [L300](../../../src/test/java/com/umc/product/project/application/service/command/ProjectMatchingRoundCommandServiceTest.java#L300) · `@Test` · `수정_기간_중첩_거부` — 수정 기간이 다른 차수와 겹치면 거부한다
2. [L316](../../../src/test/java/com/umc/product/project/application/service/command/ProjectMatchingRoundCommandServiceTest.java#L316) · `@Test` · `다음_차수와_이전_차수_결정기한_간격_부족` — 다음 차수 시작 직전까지 이전 차수 결정 기한이 이어지면 생성할 수 없다
3. [L334](../../../src/test/java/com/umc/product/project/application/service/command/ProjectMatchingRoundCommandServiceTest.java#L334) · `@Test` · `차수_최소_간격_충족` — 차수 사이 최소 간격을 만족하면 다음 차수를 생성한다

#### ProjectMatchingRoundFinalizationCommandServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/command/ProjectMatchingRoundFinalizationCommandServiceTest.java`

1. [L596](../../../src/test/java/com/umc/product/project/application/service/command/ProjectMatchingRoundFinalizationCommandServiceTest.java#L596) · `@Test` · `PLAN_DEVELOPER_3차에_진행중_프로젝트가_없으면_랜덤_배정을_종료한다` — PLAN_DEVELOPER_3차에_진행중_프로젝트가_없으면_랜덤_배정을_종료한다
2. [L608](../../../src/test/java/com/umc/product/project/application/service/command/ProjectMatchingRoundFinalizationCommandServiceTest.java#L608) · `@Test` · `PLAN_DEVELOPER_3차_승인_예정자가_TO를_채우면_추가_랜덤_배정을_생략한다` — PLAN_DEVELOPER_3차_승인_예정자가_TO를_채우면_추가_랜덤_배정을_생략한다
3. [L632](../../../src/test/java/com/umc/product/project/application/service/command/ProjectMatchingRoundFinalizationCommandServiceTest.java#L632) · `@Test` · `개발자_후보의_파트에_남은_TO가_없으면_후보를_건너뛴다` — 개발자_후보의_파트에_남은_TO가_없으면_후보를_건너뛴다
4. [L656](../../../src/test/java/com/umc/product/project/application/service/command/ProjectMatchingRoundFinalizationCommandServiceTest.java#L656) · `@Test` · `남은_TO를_모두_채우면_후속_후보_순회를_중단한다` — 남은_TO를_모두_채우면_후속_후보_순회를_중단한다

#### ProjectApplicationPermissionEvaluatorTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/evaluator/ProjectApplicationPermissionEvaluatorTest.java`

1. [L74](../../../src/test/java/com/umc/product/project/application/service/evaluator/ProjectApplicationPermissionEvaluatorTest.java#L74) · `@Test` · `지원하지_않는_권한은_fail_closed한다` — 지원하지_않는_권한은_fail_closed한다
2. [L83](../../../src/test/java/com/umc/product/project/application/service/evaluator/ProjectApplicationPermissionEvaluatorTest.java#L83) · `@Test` · `WRITE_대상_프로젝트가_없으면_not_found` — WRITE_대상_프로젝트가_없으면_not_found
3. [L92](../../../src/test/java/com/umc/product/project/application/service/evaluator/ProjectApplicationPermissionEvaluatorTest.java#L92) · `@Test` · `READ_대상_지원서가_없으면_not_found` — READ_대상_지원서가_없으면_not_found

#### ProjectPermissionEvaluatorTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/evaluator/ProjectPermissionEvaluatorTest.java`

1. [L56](../../../src/test/java/com/umc/product/project/application/service/evaluator/ProjectPermissionEvaluatorTest.java#L56) · `@Test` · `지원하지_않는_권한은_fail_closed한다` — 지원하지_않는_권한은_fail_closed한다

#### ProjectStatisticsAccessPolicyTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/policy/ProjectStatisticsAccessPolicyTest.java`

1. [L67](../../../src/test/java/com/umc/product/project/application/service/policy/ProjectStatisticsAccessPolicyTest.java#L67) · `@Test` · `프로젝트_통계는_지부_권한으로_fallback한다` — PO와 보조 PM이 아니면 지부 통계 권한으로 fallback한다
2. [L77](../../../src/test/java/com/umc/product/project/application/service/policy/ProjectStatisticsAccessPolicyTest.java#L77) · `@Test` · `SUPER_ADMIN은_지부_통계를_조회할_수_있다` — SUPER_ADMIN은 지부 통계를 전역 조회할 수 있다

#### ProjectApplicationFormQueryServiceTest (5개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/query/ProjectApplicationFormQueryServiceTest.java`

1. [L324](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationFormQueryServiceTest.java#L324) · `@Test` · `findAllByProjectIds_폼이_모두_없으면_후속_batch를_호출하지_않는다` — findAllByProjectIds_폼이_모두_없으면_후속_batch를_호출하지_않는다
2. [L333](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationFormQueryServiceTest.java#L333) · `@Test` · `findByProjectId_SUPER_ADMIN은_전체_폼을_조회한다` — findByProjectId_SUPER_ADMIN은_전체_폼을_조회한다
3. [L348](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationFormQueryServiceTest.java#L348) · `@Test` · `findAllByProjectIds_중앙총괄_지부장_지원자_scope를_기수별로_합성한다` — findAllByProjectIds_중앙총괄_지부장_지원자_scope를_기수별로_합성한다
4. [L390](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationFormQueryServiceTest.java#L390) · `@Test` · `findAllByProjectIds_SUPER_ADMIN은_모든_프로젝트의_전체_폼을_조회한다` — findAllByProjectIds_SUPER_ADMIN은_모든_프로젝트의_전체_폼을_조회한다
5. [L408](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationFormQueryServiceTest.java#L408) · `@Test` · `findAllByProjectIds_제한된_기수의_챌린저가_없으면_접근을_거부한다` — findAllByProjectIds_제한된_기수의_챌린저가_없으면_접근을_거부한다

#### ProjectApplicationQueryServiceTest (9개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java`

1. [L606](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java#L606) · `@Test` · `searchByProjects_미존재와_전부_권한없음` — searchByProjects는 프로젝트 미존재와 전부 권한 없음 결과를 빈 목록으로 고정한다
2. [L1105](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java#L1105) · `@Test` · `batchGetDetails_빈_입력` — batchGetDetails는 null·빈 입력을 조회 없이 단축한다
3. [L1113](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java#L1113) · `@Test` · `batchGetDetails_중복_query는_첫_요청을_보존한다` — batchGetDetails는 중복 application query의 첫 요청을 보존한다
4. [L1137](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java#L1137) · `@Test` · `batchGetDetails_프로젝트_정합성_위반` — batchGetDetails는 미요청 지원서나 다른 프로젝트 지원서를 not-found로 위장한다
5. [L1159](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java#L1159) · `@Test` · `batchGetDetails_타인_진행중_차수_거부` — batchGetDetails는 타인의 진행 중 차수를 scope가 허용하지 않으면 거부한다
6. [L1177](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java#L1177) · `@Test` · `batchGetDetails_타인_종료된_차수_허용` — batchGetDetails는 scope가 허용하지 않아도 종료된 차수의 타인 지원서를 조회한다
7. [L1198](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java#L1198) · `@Test` · `batchGetDetails_지원자와_응답_누락` — batchGetDetails는 지원자·응답 누락을 not-found로 통일한다
8. [L1221](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java#L1221) · `@Test` · `batchGetDetails_파일_batch와_누락_필터` — batchGetDetails는 답변 파일을 일괄 조회하고 누락 파일만 제외한다
9. [L1251](../../../src/test/java/com/umc/product/project/application/service/query/ProjectApplicationQueryServiceTest.java#L1251) · `@Test` · `batchGetDetails_파일_분리중_응답_누락` — batchGetDetails 파일 분리는 응답이 누락된 지원서를 건너뛴 뒤 not-found로 통일한다

#### ProjectMatchingRoundQueryServiceTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/query/ProjectMatchingRoundQueryServiceTest.java`

1. [L31](../../../src/test/java/com/umc/product/project/application/service/query/ProjectMatchingRoundQueryServiceTest.java#L31) · `@Test` · `목록_조회_분기를_구분한다` — 시간·지부 조합에 따라 open, chapter, 전체 차수를 조회한다
2. [L46](../../../src/test/java/com/umc/product/project/application/service/query/ProjectMatchingRoundQueryServiceTest.java#L46) · `@Test` · `batch_조회는_빈_입력과_순서를_처리한다` — batch 조회는 빈 입력을 단축하고 원본 조회 순서를 보존한다

#### ProjectMemberQueryServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/query/ProjectMemberQueryServiceTest.java`

1. [L45](../../../src/test/java/com/umc/product/project/application/service/query/ProjectMemberQueryServiceTest.java#L45) · `@Test` · `랜덤_매칭_멤버를_파트에_맞게_조회한다` — 랜덤 매칭 멤버는 챌린저 파트에 맞는 matching type으로 조회한다
2. [L58](../../../src/test/java/com/umc/product/project/application/service/query/ProjectMemberQueryServiceTest.java#L58) · `@Test` · `랜덤_매칭_비대상은_빈_값을_반환한다` — 챌린저가 없거나 매칭 대상 파트가 아니면 랜덤 매칭 멤버는 없다
3. [L69](../../../src/test/java/com/umc/product/project/application/service/query/ProjectMemberQueryServiceTest.java#L69) · `@Test` · `프로젝트_멤버_단건과_목록을_조회한다` — 프로젝트 멤버 단건·목록 조회 결과를 info로 변환한다

#### ProjectPermissionQueryServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/query/ProjectPermissionQueryServiceTest.java`

1. [L287](../../../src/test/java/com/umc/product/project/application/service/query/ProjectPermissionQueryServiceTest.java#L287) · `@Test` · `null_빈값_null만_있는_ID_목록은_조회하지_않는다` — null_빈값_null만_있는_ID_목록은_조회하지_않는다
2. [L296](../../../src/test/java/com/umc/product/project/application/service/query/ProjectPermissionQueryServiceTest.java#L296) · `@Test` · `폼과_프로젝트_상태별_수정_검토_공개_삭제_거부사유를_구분한다` — 폼과_프로젝트_상태별_수정_검토_공개_삭제_거부사유를_구분한다
3. [L352](../../../src/test/java/com/umc/product/project/application/service/query/ProjectPermissionQueryServiceTest.java#L352) · `@Test` · `지원_생성의_상태_폼_본인_챌린저_파트_팀원_차수_거부사유를_구분한다` — 지원_생성의_상태_폼_본인_챌린저_파트_팀원_차수_거부사유를_구분한다
4. [L417](../../../src/test/java/com/umc/product/project/application/service/query/ProjectPermissionQueryServiceTest.java#L417) · `@Test` · `지원폼_읽기는_같은기수_총괄_지부장_챌린저와_port_챌린저를_허용한다` — 지원폼_읽기는_같은기수_총괄_지부장_챌린저와_port_챌린저를_허용한다

#### ProjectQueryServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/query/ProjectQueryServiceTest.java`

1. [L59](../../../src/test/java/com/umc/product/project/application/service/query/ProjectQueryServiceTest.java#L59) · `@Test` · `findAllByIds_빈값_누락_중복파일과_기본집계를_한번에_처리한다` — findAllByIds_빈값_누락_중복파일과_기본집계를_한번에_처리한다
2. [L89](../../../src/test/java/com/umc/product/project/application/service/query/ProjectQueryServiceTest.java#L89) · `@Test` · `단건_파일은_logo만_있거나_모두_없어도_안전하게_조립한다` — 단건_파일은_logo만_있거나_모두_없어도_안전하게_조립한다
3. [L111](../../../src/test/java/com/umc/product/project/application/service/query/ProjectQueryServiceTest.java#L111) · `@Test` · `batch_프로젝트에_파일이_전혀_없으면_storage를_호출하지_않는다` — batch_프로젝트에_파일이_전혀_없으면_storage를_호출하지_않는다
4. [L126](../../../src/test/java/com/umc/product/project/application/service/query/ProjectQueryServiceTest.java#L126) · `@Test` · `검색_scope의_모든_형태를_query로_변환한다` — 검색_scope의_모든_형태를_query로_변환한다

#### ProjectStatisticsQueryServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/service/query/ProjectStatisticsQueryServiceTest.java`

1. [L482](../../../src/test/java/com/umc/product/project/application/service/query/ProjectStatisticsQueryServiceTest.java#L482) · `@Test` · `프로젝트_ID_목록의_빈값_미존재_타지부_조합을_검증한다` — 프로젝트_ID_목록의_빈값_미존재_타지부_조합을_검증한다
2. [L502](../../../src/test/java/com/umc/product/project/application/service/query/ProjectStatisticsQueryServiceTest.java#L502) · `@Test` · `빈_지부와_빈_공개_프로젝트는_빈_요약을_반환한다` — 빈_지부와_빈_공개_프로젝트는_빈_요약을_반환한다
3. [L523](../../../src/test/java/com/umc/product/project/application/service/query/ProjectStatisticsQueryServiceTest.java#L523) · `@Test` · `학교_ID가_누락된_멤버와_지원서는_학교별_집계에서_제외한다` — 학교_ID가_누락된_멤버와_지원서는_학교별_집계에서_제외한다
4. [L550](../../../src/test/java/com/umc/product/project/application/service/query/ProjectStatisticsQueryServiceTest.java#L550) · `@Test` · `공개_학교별_집계는_null_멤버와_학교_ID_누락을_제외한다` — 공개_학교별_집계는_null_멤버와_학교_ID_누락을_제외한다

### Contract / Misc

#### ProjectDtoResidualTest (15개, 신규 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java`

1. [L81](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L81) · `@Test` · `request_dtos_map_to_commands` — 요청 DTO는 path·인증 정보와 payload를 command로 손실 없이 변환한다
2. [L117](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L117) · `@Test` · `application_access_scope_records_preserve_values` — 지원서 access scope record는 각 범위 값을 보존한다
3. [L127](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L127) · `@Test` · `summary_aggregates_quota_status` — 프로젝트 요약은 빈 TO와 모집 중·완료 TO를 올바르게 집계한다
4. [L145](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L145) · `@Test` · `member_and_page_graphql_responses` — Member·Page GraphQL DTO는 enrichment와 paging metadata를 보존한다
5. [L162](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L162) · `@Test` · `application_detail_conversions_preserve_nested_data` — 지원서 상세 Web·GraphQL 변환은 선택지 snapshot·파일 누락·시간을 안전하게 처리한다
6. [L183](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L183) · `@Test` · `application_graphql_null_boundaries` — 지원서 GraphQL 변환은 null 구조·round·answer collection을 빈 값으로 fail-safe 처리한다
7. [L205](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L205) · `@Test` · `application_form_web_dtos_convert_nested_structure` — 지원 폼 Web DTO는 section-question-option 구조를 양방향 변환한다
8. [L220](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L220) · `@Test` · `view_status_enums_cover_all_domain_values` — 상태 표시 enum은 모든 도메인 상태를 명시적으로 변환한다
9. [L250](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L250) · `@Test` · `detail_and_managed_responses_aggregate_quota_status` — 상세·관리 응답은 빈 TO와 모집 중·완료 TO를 각각 집계한다
10. [L268](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L268) · `@Test` · `my_application_response_handles_missing_project_and_round` — 지원 내역은 누락 프로젝트와 누락 라운드를 null-safe하게 변환한다
11. [L283](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L283) · `@Test` · `search_queries_validate_boundaries` — 검색 query는 필수 상태·owner 조합과 batch ID 경계를 검증한다
12. [L324](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L324) · `@Test` · `graphql_page_and_project_boundaries` — GraphQL page와 프로젝트 응답은 경계·시간·nullable collection을 처리한다
13. [L344](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L344) · `@Test` · `update_matching_round_request_rejects_invalid_fields` — 매칭 차수 수정 요청은 blank 이름과 chapterId 필드를 거부한다
14. [L357](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L357) · `@Test` · `web_answer_view_handles_null_file_ids` — Web AnswerView는 첨부 ID가 null이면 빈 파일 목록을 반환한다
15. [L369](../../../src/test/java/com/umc/product/project/adapter/in/ProjectDtoResidualTest.java#L369) · `@Test` · `application_detail_factory_preserves_first_duplicate_answer` — 지원서 상세 factory는 중복 질문 답변 중 첫 답변을 보존한다

#### ProjectAccessScopeResolverTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/access/ProjectAccessScopeResolverTest.java`

1. [L62](../../../src/test/java/com/umc/product/project/application/access/ProjectAccessScopeResolverTest.java#L62) · `@Test` · `publicSearch_은_SUPER_ADMIN이면_All이고_DRAFT는_거부한다` — publicSearch_은_SUPER_ADMIN이면_All이고_DRAFT는_거부한다
2. [L168](../../../src/test/java/com/umc/product/project/application/access/ProjectAccessScopeResolverTest.java#L168) · `@Test` · `management_은_SUPER_ADMIN이면_All` — management_은_SUPER_ADMIN이면_All
3. [L177](../../../src/test/java/com/umc/product/project/application/access/ProjectAccessScopeResolverTest.java#L177) · `@Test` · `management_PO의_상태_요청이_비면_전체_상태와_DRAFT를_포함한다` — management_PO의_상태_요청이_비면_전체_상태와_DRAFT를_포함한다
4. [L187](../../../src/test/java/com/umc/product/project/application/access/ProjectAccessScopeResolverTest.java#L187) · `@Test` · `management_운영진_PO의_상태_요청이_비면_owner에는_전체_상태를_포함한다` — management_운영진_PO의_상태_요청이_비면_owner에는_전체_상태를_포함한다

#### ProjectApplicationAccessScopeResolverTest (6개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/application/access/ProjectApplicationAccessScopeResolverTest.java`

1. [L53](../../../src/test/java/com/umc/product/project/application/access/ProjectApplicationAccessScopeResolverTest.java#L53) · `@Test` · `applicant_scope는_본인으로_제한한다` — 본인 지원 내역 scope는 호출자 owner로 고정한다
2. [L421](../../../src/test/java/com/umc/product/project/application/access/ProjectApplicationAccessScopeResolverTest.java#L421) · `@Test` · `projectApplicantLists_batch_빈_입력` — projectApplicantLists_batch는 null·빈 프로젝트를 조회 없이 단축한다
3. [L429](../../../src/test/java/com/umc/product/project/application/access/ProjectApplicationAccessScopeResolverTest.java#L429) · `@Test` · `projectApplicantLists_batch_SUPER_ADMIN` — projectApplicantLists_batch SUPER_ADMIN은 진행 중 차수까지 조회한다
4. [L443](../../../src/test/java/com/umc/product/project/application/access/ProjectApplicationAccessScopeResolverTest.java#L443) · `@Test` · `projectApplicantLists_batch_지부장과_학교회장단` — projectApplicantLists_batch는 같은 지부의 지부장과 학교 회장단만 허용한다
5. [L466](../../../src/test/java/com/umc/product/project/application/access/ProjectApplicationAccessScopeResolverTest.java#L466) · `@Test` · `projectApplicantLists_batch_학교_지부_매핑_누락` — projectApplicantLists_batch는 학교의 지부 매핑이 누락되면 fail-closed한다
6. [L482](../../../src/test/java/com/umc/product/project/application/access/ProjectApplicationAccessScopeResolverTest.java#L482) · `@Test` · `management_scope_역할_행렬` — management scope는 SUPER_ADMIN·총괄·지부장·일반 회원을 구분한다

### Domain

#### ProjectDomainResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/project/domain/ProjectDomainResidualTest.java`

1. [L25](../../../src/test/java/com/umc/product/project/domain/ProjectDomainResidualTest.java#L25) · `@Test` · `matching_round_overlap_boundaries` — 매칭 기간 중첩은 null·다른 지부·비중첩을 허용하고 같은 지부 경계 접촉은 거부한다
2. [L45](../../../src/test/java/com/umc/product/project/domain/ProjectDomainResidualTest.java#L45) · `@Test` · `reschedules_matching_round` — 매칭 일정 재설정은 검증된 세 시각을 모두 교체한다
3. [L60](../../../src/test/java/com/umc/product/project/domain/ProjectDomainResidualTest.java#L60) · `@Test` · `application_submission_and_validation_boundaries` — 지원서는 DRAFT에서 한 번만 제출되고 제출 검증 message를 보존한다
4. [L81](../../../src/test/java/com/umc/product/project/domain/ProjectDomainResidualTest.java#L81) · `@Test` · `application_form_belongs_to_project_by_id` — 지원 폼 소속 비교는 프로젝트 ID 값 기준으로 판단한다

### GraphQL

#### ProjectGraphQlControllerTest (7개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/in/graphql/ProjectGraphQlControllerTest.java`

1. [L336](../../../src/test/java/com/umc/product/project/adapter/in/graphql/ProjectGraphQlControllerTest.java#L336) · `@Test` · `projects_page_생략_기본값` — projects는 page 생략 시 기본 페이지 조건으로 검색한다
2. [L351](../../../src/test/java/com/umc/product/project/adapter/in/graphql/ProjectGraphQlControllerTest.java#L351) · `@Test` · `회원_field_resolver_null과_누락_처리` — 회원 field resolver는 null과 누락 회원을 안전하게 처리하고 입력 순서를 보존한다
3. [L381](../../../src/test/java/com/umc/product/project/adapter/in/graphql/ProjectGraphQlControllerTest.java#L381) · `@Test` · `빈_회원_ID_집합` — 빈 회원 ID 집합은 회원 usecase 호출 없이 빈 resolver 결과를 만든다
4. [L392](../../../src/test/java/com/umc/product/project/adapter/in/graphql/ProjectGraphQlControllerTest.java#L392) · `@Test` · `members_resolver_권한_거부` — members resolver는 프로젝트 읽기 권한 거부 시 조회를 중단한다
5. [L405](../../../src/test/java/com/umc/product/project/adapter/in/graphql/ProjectGraphQlControllerTest.java#L405) · `@Test` · `null_principal_현재회원_대체` — null principal은 보안 context의 현재 회원으로 대체한다
6. [L414](../../../src/test/java/com/umc/product/project/adapter/in/graphql/ProjectGraphQlControllerTest.java#L414) · `@Test` · `members_resolver_중복_key` — members resolver는 중복 프로젝트 key의 첫 결과를 보존한다
7. [L431](../../../src/test/java/com/umc/product/project/adapter/in/graphql/ProjectGraphQlControllerTest.java#L431) · `@Test` · `application_resolver_applicationId_없음` — application resolver는 applicationId가 없는 멤버를 상세 조회 없이 null로 반환한다

### Persistence

#### ProjectApplicationAndMemberQueryRepositoryResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/out/persistence/ProjectApplicationAndMemberQueryRepositoryResidualTest.java`

1. [L57](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectApplicationAndMemberQueryRepositoryResidualTest.java#L57) · `@Test` · `application_lookup_and_detail_contracts` — 지원서 단건·존재·detail·batch query는 project/member/round/status를 모두 제한한다
2. [L88](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectApplicationAndMemberQueryRepositoryResidualTest.java#L88) · `@Test` · `application_search_and_decidable_filters` — 본인 검색과 결정 대상 query는 DRAFT를 제외하고 명시 status를 적용한다
3. [L122](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectApplicationAndMemberQueryRepositoryResidualTest.java#L122) · `@Test` · `member_matching_grouping_and_count_contracts` — member query는 matching part·application null·ACTIVE와 grouping/count를 보존한다

#### ProjectApplicationPersistenceAdaptersResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/out/persistence/ProjectApplicationPersistenceAdaptersResidualTest.java`

1. [L41](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectApplicationPersistenceAdaptersResidualTest.java#L41) · `@Test` · `batch_and_crud_contracts` — batch는 빈 입력을 단축하고 프로젝트별 최초 form을 보존하며 CRUD를 위임한다
2. [L72](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectApplicationPersistenceAdaptersResidualTest.java#L72) · `@Test` · `batch_and_crud_contracts` — policy batch는 빈 입력을 단축하고 form별 grouping 후 CRUD를 위임한다
3. [L105](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectApplicationPersistenceAdaptersResidualTest.java#L105) · `@Test` · `delegates_application_operations` — 지원서 조회·검색·저장 port를 상태와 필터를 보존해 위임한다
4. [L176](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectApplicationPersistenceAdaptersResidualTest.java#L176) · `@Test` · `rejects_missing_draft_and_partial_batch` — draft와 batch 조회는 누락된 일부 데이터도 fail-closed로 처리한다

#### ProjectCorePersistenceAdaptersTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/out/persistence/ProjectCorePersistenceAdaptersTest.java`

1. [L51](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectCorePersistenceAdaptersTest.java#L51) · `@Test` · `delegates_all_operations` — 단건·목록·검색·저장 계약을 위임하고 not-found를 구분한다
2. [L99](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectCorePersistenceAdaptersTest.java#L99) · `@Test` · `batch_and_delegation_contracts` — batch는 중복 제거·입력 순서를 보존하고 누락을 거부하며 나머지 port를 위임한다
3. [L155](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectCorePersistenceAdaptersTest.java#L155) · `@Test` · `delegates_and_groups_members` — 활성 멤버 filter·grouping·count와 not-found 계약을 보존한다
4. [L215](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectCorePersistenceAdaptersTest.java#L215) · `@Test` · `delegates_and_short_circuits_empty_delete` — quota port 위임과 빈 삭제 short-circuit를 보존한다
5. [L246](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectCorePersistenceAdaptersTest.java#L246) · `@Test` · `delegates_statistics_queries` — 통계 row 조회를 위임하고 없는 프로젝트만 not-found로 변환한다

#### ProjectQueryRepositoryTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/out/persistence/ProjectQueryRepositoryTest.java`

1. [L130](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectQueryRepositoryTest.java#L130) · `@Test` · `복수_정렬_조건의_오름차순과_내림차순을_적용한다` — 복수_정렬_조건의_오름차순과_내림차순을_적용한다

#### ProjectStatisticsAndQuotaQueryRepositoryTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/out/persistence/ProjectStatisticsAndQuotaQueryRepositoryTest.java`

1. [L60](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectStatisticsAndQuotaQueryRepositoryTest.java#L60) · `@Test` · `empty_project_ids_short_circuit` — 빈 projectIds는 quota·member·application query를 단축한다
2. [L73](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectStatisticsAndQuotaQueryRepositoryTest.java#L73) · `@Test` · `project_and_round_rows_preserve_filters` — project·round 통계 row는 chapter·공개 상태·정렬 계약을 보존한다
3. [L96](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectStatisticsAndQuotaQueryRepositoryTest.java#L96) · `@Test` · `member_rows_preserve_active_and_public_filters` — member 통계는 ACTIVE·공개 project·matching 대상 part만 포함한다
4. [L126](../../../src/test/java/com/umc/product/project/adapter/out/persistence/ProjectStatisticsAndQuotaQueryRepositoryTest.java#L126) · `@Test` · `quota_and_application_rows_preserve_status_filters` — quota는 projectId로 grouping하고 application 통계는 counted·approved 상태를 구분한다

### REST / Web

#### ProjectApplicationResponseAssemblerTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectApplicationResponseAssemblerTest.java`

1. [L268](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectApplicationResponseAssemblerTest.java#L268) · `@Test` · `PM_식별자_없음` — myApplicationsFor는 PM 식별자가 없는 프로젝트도 회원 조회 없이 조립한다
2. [L588](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectApplicationResponseAssemblerTest.java#L588) · `@Test` · `applicantsForBatch_빈_Map` — applicantsForBatch는 검색 결과 Map이 비어 있으면 즉시 빈 Map을 반환한다
3. [L599](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectApplicationResponseAssemblerTest.java#L599) · `@Test` · `applicantsForBatch_빈_지원자_목록` — applicantsForBatch는 project key만 있고 지원자가 없으면 key와 빈 목록을 보존한다
4. [L615](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectApplicationResponseAssemblerTest.java#L615) · `@Test` · `applicantsForBatch_프로젝트_부가정보_누락` — applicantsForBatch는 프로젝트 부가 정보가 누락되어도 지원자 카드를 안전하게 조립한다

#### ProjectResponseAssemblerTest (6개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectResponseAssemblerTest.java`

1. [L329](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectResponseAssemblerTest.java#L329) · `@Test` · `searchFor_빈_페이지` — searchFor는 빈 페이지에서 회원 batch 조회를 생략한다
2. [L342](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectResponseAssemblerTest.java#L342) · `@Test` · `searchFor_PM_정보_조립` — searchFor는 조회된 프로젝트의 PM 정보를 조립한다
3. [L358](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectResponseAssemblerTest.java#L358) · `@Test` · `searchManagedFor_빈_페이지` — 관리 목록이 비어 있으면 회원 batch 조회 없이 빈 페이지를 반환한다
4. [L369](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectResponseAssemblerTest.java#L369) · `@Test` · `listProjectMembers_권한_거부와_조회_실패` — 일괄 팀원 조회는 권한 거부와 조회 실패 프로젝트를 제외한다
5. [L381](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectResponseAssemblerTest.java#L381) · `@Test` · `listProjectMembers_누락_회원과_중복_매칭_행` — 일괄 팀원 조회는 누락 회원을 제외하고 중복 매칭 행 중 첫 값을 보존한다
6. [L408](../../../src/test/java/com/umc/product/project/adapter/in/web/assembler/ProjectResponseAssemblerTest.java#L408) · `@Test` · `statistics_응답_조립` — 통계 응답 조립은 모든 중첩 통계 값을 보존한다

#### ProjectApplicationControllerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/in/web/ProjectApplicationControllerTest.java`

1. [L94](../../../src/test/java/com/umc/product/project/adapter/in/web/ProjectApplicationControllerTest.java#L94) · `@Test` · `지원서_초안_생성과_철회는_요청자_정보를_전달한다` — 지원서_초안_생성과_철회는_요청자_정보를_전달한다

#### ProjectApplicationQueryControllerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/in/web/ProjectApplicationQueryControllerTest.java`

1. [L149](../../../src/test/java/com/umc/product/project/adapter/in/web/ProjectApplicationQueryControllerTest.java#L149) · `@Test` · `내_지원내역과_상세조회_query_변환` — 내 지원 내역과 지원서 상세 조회는 식별자를 query로 변환한다

#### ProjectCommandControllerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/in/web/ProjectCommandControllerTest.java`

1. [L194](../../../src/test/java/com/umc/product/project/adapter/in/web/ProjectCommandControllerTest.java#L194) · `@Test` · `프로젝트_생성_수정_제출_소유권_팀원_공개_정원_흐름을_위임한다` — 프로젝트_생성_수정_제출_소유권_팀원_공개_정원_흐름을_위임한다

#### ProjectMatchingRoundControllerTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/in/web/ProjectMatchingRoundControllerTest.java`

1. [L36](../../../src/test/java/com/umc/product/project/adapter/in/web/ProjectMatchingRoundControllerTest.java#L36) · `@Test` · `delete_and_auto_decide_delegate_identifiers` — 삭제와 자동 선발은 매칭 차수와 현재 회원 ID를 전달한다

#### ProjectQueryControllerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/project/adapter/in/web/ProjectQueryControllerTest.java`

1. [L84](../../../src/test/java/com/umc/product/project/adapter/in/web/ProjectQueryControllerTest.java#L84) · `@Test` · `상세_팀원_batch_관리목록_초안_조회는_요청자와_식별자를_전달한다` — 상세_팀원_batch_관리목록_초안_조회는_요청자와_식별자를_전달한다

## 3.22 `recruiting` — 83개

### Application Service

#### RecruitingApplicationCommandServiceTest (5개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationCommandServiceTest.java`

1. [L321](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationCommandServiceTest.java#L321) · `@Test` · `updateAnonymousDraftApplication` — 작성 중 익명 지원서 수정은 access key 기반 draft 수정 API를 사용한다
2. [L347](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationCommandServiceTest.java#L347) · `@Test` · `updateSubmittedMemberApplication` — 제출 완료 로그인 지원서 수정은 필수·허용 문항 범위를 포함한 제출 응답 수정 API를 사용한다
3. [L372](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationCommandServiceTest.java#L372) · `@Test` · `rejectUpdateOfCancelledApplication` — 철회된 지원서는 수정 가능한 상태가 아니므로 Form 검증 전에 거부한다
4. [L395](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationCommandServiceTest.java#L395) · `@Test` · `rejectMalformedAnonymousApplicationKey` — 익명 지원 키 형식이 잘못되면 저장소 조회 없이 거부한다
5. [L464](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationCommandServiceTest.java#L464) · `@Test` · `rejectAlreadySubmittedAnonymousApplication` — 이미 제출한 익명 지원서는 중복 제출할 수 없다

#### RecruitingApplicationFormStructureCommandServiceTest (7개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormStructureCommandServiceTest.java`

1. [L170](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormStructureCommandServiceTest.java#L170) · `@Test` · `updateExistingStructureWithFullDiff` — 기존 Form 구조는 생성·수정·삭제·재정렬 diff를 ID 소유권 안에서 적용한다
2. [L233](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormStructureCommandServiceTest.java#L233) · `@Test` · `rejectMalformedStructure` — 빈·중복·공백 section key와 질문 타입에 맞지 않는 option 구조를 거절한다
3. [L254](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormStructureCommandServiceTest.java#L254) · `@Test` · `rejectInvalidTransitionAndPolicy` — 존재하지 않는 조건부 section과 유효하지 않은 section 정책을 거절한다
4. [L271](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormStructureCommandServiceTest.java#L271) · `@Test` · `rejectForeignNestedIds` — 기존 Form의 section·question·option ID 소유권 위조를 모두 거절한다
5. [L282](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormStructureCommandServiceTest.java#L282) · `@Test` · `rejectNonDraftRound` — DRAFT가 아닌 모집 차수의 Form 구조 변경을 거절한다
6. [L296](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormStructureCommandServiceTest.java#L296) · `@Test` · `restoreMissingSectionPolicy` — 기존 section 정책이 누락됐으면 요청 정책으로 복구한다
7. [L316](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormStructureCommandServiceTest.java#L316) · `@Test` · `rejectRoundFromAnotherSeason` — 다른 season의 round로 Form을 수정할 수 없다

#### RecruitingApplicationFormValidationServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormValidationServiceTest.java`

1. [L110](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormValidationServiceTest.java#L110) · `@Test` · `allowConditionalTransitionToCommonSection` — TRACK section에서 COMMON section으로 향하는 조건부 이동은 허용한다
2. [L127](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormValidationServiceTest.java#L127) · `@Test` · `rejectConditionalTransitionToUnknownSection` — 존재하지 않는 section으로 향하는 조건부 이동은 fail-closed로 거부한다

#### RecruitingApplicationValidationServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationValidationServiceTest.java`

1. [L143](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationValidationServiceTest.java#L143) · `@Test` · `allowOwnedAnonymousFormResponse` — 익명 Form 응답의 access key·응답 ID·Form 연결이 모두 일치하면 수정할 수 있다
2. [L155](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationValidationServiceTest.java#L155) · `@Test` · `rejectMismatchedAnonymousFormResponseOwnership` — 익명 Form 응답에 회원이 연결되거나 응답 ID·Form이 다르면 수정을 거부한다

#### RecruitingDecisionCommandServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingDecisionCommandServiceTest.java`

1. [L119](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingDecisionCommandServiceTest.java#L119) · `@Test` · `rejectUnknownDocumentDecision` — 정의되지 않은 서류 결정은 상태를 변경하거나 저장하지 않는다
2. [L163](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingDecisionCommandServiceTest.java#L163) · `@Test` · `decideFinalFailureAndRejectUnknownDecision` — 최종 불합격은 최종 상태를 변경하고 정의되지 않은 결정은 거부한다

#### RecruitingInterviewMailDeliveryCommandServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingInterviewMailDeliveryCommandServiceTest.java`

1. [L50](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingInterviewMailDeliveryCommandServiceTest.java#L50) · `@Test` · `이미_발송된_일정은_성공을_중복_저장하지_않는다` — 이미 발송된 일정의 성공 이벤트는 멱등하게 무시한다

#### RecruitingRegistrationCommandServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRegistrationCommandServiceTest.java`

1. [L88](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRegistrationCommandServiceTest.java#L88) · `@Test` · `superAdminPreparesRegistration` — SUPER_ADMIN은 기수 역할 없이도 등록 준비를 수행할 수 있다
2. [L166](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRegistrationCommandServiceTest.java#L166) · `@Test` · `rejectRegistrationWithoutMemberOrAcceptedTrack` — 익명 지원서와 합격 트랙이 누락된 지원서는 등록 확정을 거부한다

#### RecruitingRoundCreateCommandServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundCreateCommandServiceTest.java`

1. [L71](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundCreateCommandServiceTest.java#L71) · `@Test` · `createRegularRound` — 정규 모집 차수는 roundNo 1로 생성한다
2. [L90](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundCreateCommandServiceTest.java#L90) · `@Test` · `rejectInvalidRoundNumberSequence` — 정규 모집 roundNo와 추가 모집 순번은 연속성 규칙을 지켜야 한다

#### RecruitingRoundLifecycleCommandServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundLifecycleCommandServiceTest.java`

1. [L126](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundLifecycleCommandServiceTest.java#L126) · `@Test` · `rejectDeleteWhenRoundIsOpen` — OPEN Round는 지원서 조회 전에 hard delete를 거부한다
2. [L273](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundLifecycleCommandServiceTest.java#L273) · `@Test` · `rejectCloneWhenSectionPolicyIsMissing` — Round 복제 시 Form section 정책이 누락되면 fail-closed로 거부한다

#### RecruitingRoundUpdateCommandServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundUpdateCommandServiceTest.java`

1. [L100](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundUpdateCommandServiceTest.java#L100) · `@Test` · `updateRoundTitleSynchronizesForm` — 차수 제목 변경은 연결된 Form의 메타데이터를 함께 동기화한다
2. [L290](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundUpdateCommandServiceTest.java#L290) · `@Test` · `closeOpenRound` — OPEN 차수는 application Form을 함께 닫아 CLOSED로 전환한다
3. [L311](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundUpdateCommandServiceTest.java#L311) · `@Test` · `rejectNullRoundStatus` — 지원하지 않는 null 상태 전이는 fail-closed로 거절한다
4. [L372](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundUpdateCommandServiceTest.java#L372) · `@Test` · `rejectOpenInterviewRoundWithoutAvailabilityForm` — 면접 차수에 availability Form ID가 없으면 OPEN 전환을 거절한다

#### RecruitingSeasonCommandServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingSeasonCommandServiceTest.java`

1. [L212](../../../src/test/java/com/umc/product/recruiting/application/service/command/RecruitingSeasonCommandServiceTest.java#L212) · `@Test` · `replaceSeasonQuotasWithUpdateDeleteAndCreate` — 시즌 쿼터 교체는 기존 트랙을 수정·삭제하고 신규 트랙을 한 번에 생성한다

#### RecruitingPermissionEvaluatorTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/evaluator/RecruitingPermissionEvaluatorTest.java`

1. [L130](../../../src/test/java/com/umc/product/recruiting/application/service/evaluator/RecruitingPermissionEvaluatorTest.java#L130) · `@Test` · `typePermissionAllowsSchoolCoreOnly` — 리소스를 지정하지 않은 모집 READ는 학교 회장·부회장 역할만 허용한다

#### RecruitingApplicationReviewQueryServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingApplicationReviewQueryServiceTest.java`

1. [L147](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingApplicationReviewQueryServiceTest.java#L147) · `@Test` · `rejectWrongRoundOrDraftDetail` — 다른 Round 지원서와 DRAFT 지원서는 상세 조회에서 not-found로 숨긴다
2. [L171](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingApplicationReviewQueryServiceTest.java#L171) · `@Test` · `getAnonymousApplicationDetailWithAnswers` — 익명 지원서 상세는 내부 access key로 Form 응답을 조회한다

#### RecruitingInterviewScheduleQueryServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingInterviewScheduleQueryServiceTest.java`

1. [L70](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingInterviewScheduleQueryServiceTest.java#L70) · `@Test` · `allowPrivilegedScheduleReader` — 다른 지원자의 면접 일정은 해당 시즌 READ 권한이 있을 때만 조회한다
2. [L88](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingInterviewScheduleQueryServiceTest.java#L88) · `@Test` · `rejectUnprivilegedScheduleReader` — 다른 지원자의 면접 일정은 시즌 READ 권한이 없으면 fail-closed로 거부한다

#### RecruitingPublicApplicationQueryServiceTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingPublicApplicationQueryServiceTest.java`

1. [L86](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingPublicApplicationQueryServiceTest.java#L86) · `@Test` · `exposeEditableDraftBeforeDocumentPublication` — 접수 기간의 작성 중 지원서는 수정 가능하고 발표 결과는 대기 상태다
2. [L104](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingPublicApplicationQueryServiceTest.java#L104) · `@Test` · `exposeRejectedResultsAfterPublication` — 서류 탈락과 최종 탈락은 각 발표 시각부터 거절 결과로 공개한다
3. [L132](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingPublicApplicationQueryServiceTest.java#L132) · `@Test` · `keepPendingForUndecidedStatusAfterPublication` — 발표 이후에도 결정 상태가 아니면 결과를 대기로 유지한다
4. [L149](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingPublicApplicationQueryServiceTest.java#L149) · `@Test` · `rejectUnlinkedFormResponse` — 지원서와 연결되지 않은 Form 응답은 credential이 맞아도 공개하지 않는다

#### RecruitingQueryServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingQueryServiceTest.java`

1. [L240](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingQueryServiceTest.java#L240) · `@Test` · `validateAuthorizationScopesFailClosed` — 권한용 소속 검증은 null과 다른 시즌·차수를 fail-closed로 처리한다

#### RecruitingSeasonQueryServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingSeasonQueryServiceTest.java`

1. [L199](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingSeasonQueryServiceTest.java#L199) · `@Test` · `searchRoundGroupsWithAllSorts` — 차수 그룹 검색은 시즌별로 묶고 등록·모집일 정렬과 트랙 filter를 적용한다
2. [L235](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingSeasonQueryServiceTest.java#L235) · `@Test` · `searchRoundGroupsReturnsEmptyBeforePermissionLookup` — 조회 가능한 시즌 후보가 없으면 subject를 조회하지 않고 빈 결과를 반환한다
3. [L247](../../../src/test/java/com/umc/product/recruiting/application/service/query/RecruitingSeasonQueryServiceTest.java#L247) · `@Test` · `checkTitleAvailabilityForCreateAndUpdate` — 차수 제목 중복 검사는 신규와 수정 제외 ID를 구분한다

### Contract / Misc

#### RecruitingDtoContractTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/recruiting/RecruitingDtoContractTest.java`

1. [L58](../../../src/test/java/com/umc/product/recruiting/RecruitingDtoContractTest.java#L58) · `@Test` · `익명_지원서_request_변환_계약` — 익명 지원서 Web request는 이메일을 정규화하고 중첩 답변까지 command로 변환한다
2. [L94](../../../src/test/java/com/umc/product/recruiting/RecruitingDtoContractTest.java#L94) · `@Test` · `일정_교집합_unavailable_adapter_계약` — 일정 교집합 연동 전 unavailable adapter는 빈 성공으로 오인하지 않도록 명시적으로 실패한다
3. [L102](../../../src/test/java/com/umc/product/recruiting/RecruitingDtoContractTest.java#L102) · `@Test` · `모든_recruiting_DTO_계약` — 모든 Recruiting request·response·command·query record를 생성하고 변환한다
4. [L158](../../../src/test/java/com/umc/product/recruiting/RecruitingDtoContractTest.java#L158) · `@Test` · `모든_recruiting_controller_위임_계약` — 모든 Recruiting REST·GraphQL controller endpoint를 경량 fixture로 호출한다
5. [L196](../../../src/test/java/com/umc/product/recruiting/RecruitingDtoContractTest.java#L196) · `@Test` · `모든_recruiting_service_경계_계약` — 모든 Recruiting application service의 empty·not-found 경로를 경량 fixture로 호출한다
6. [L210](../../../src/test/java/com/umc/product/recruiting/RecruitingDtoContractTest.java#L210) · `@Test` · `recruiting_persistence_adapter_경계_계약` — Recruiting persistence adapter의 빈 조회·저장 위임 경로를 경량 fixture로 호출한다

### Domain

#### RecruitingDomainEdgeCaseTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/recruiting/domain/RecruitingDomainEdgeCaseTest.java`

1. [L27](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingDomainEdgeCaseTest.java#L27) · `@Test` · `rejectInvalidEvaluationAndInterviewQuestionIdentity` — 평가와 면접 질문은 null 식별자·대상·순서를 거부한다
2. [L48](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingDomainEdgeCaseTest.java#L48) · `@Test` · `rejectInvalidApplicantIdentityAndChoice` — 지원자 이메일과 profile은 null 및 모집하지 않는 1지망을 거부한다
3. [L69](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingDomainEdgeCaseTest.java#L69) · `@Test` · `rejectInvalidApplicationCreation` — 지원서 생성은 필수값·키·익명 동의·회원 access key 불변식을 지킨다
4. [L98](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingDomainEdgeCaseTest.java#L98) · `@Test` · `rejectInvalidApplicationTransitions` — 지원서 상태 전이와 수정·등록은 잘못된 상태 및 null profile을 거부한다
5. [L126](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingDomainEdgeCaseTest.java#L126) · `@Test` · `rejectInvalidRoundFormAndPolicy` — Round·Form·section policy는 null 일정과 대상 및 잘못된 상태를 거부한다
6. [L147](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingDomainEdgeCaseTest.java#L147) · `@Test` · `createDomainExceptionWithCustomMessage` — 도메인 예외는 override message를 보존한다
7. [L158](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingDomainEdgeCaseTest.java#L158) · `@Test` · `rejectRoundCommandWithoutConfiguration` — Round 생성·수정 command는 configuration 누락을 도메인 경계에서 거부한다

#### RecruitingInterviewScheduleDomainTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/domain/RecruitingInterviewScheduleDomainTest.java`

1. [L100](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingInterviewScheduleDomainTest.java#L100) · `@Test` · `생성_필수값_검증` — 면접 일정 생성은 지원서와 유효한 연락처 snapshot을 필수로 요구한다
2. [L121](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingInterviewScheduleDomainTest.java#L121) · `@Test` · `일정_확정_입력_경계` — 응답 ID·기간·장소·확정 연락처의 null과 길이 경계를 거절한다
3. [L147](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingInterviewScheduleDomainTest.java#L147) · `@Test` · `요청_메일_재시도와_취소` — 요청 메일은 실패 상태에서만 재시도하고 취소 후에는 변경할 수 없다
4. [L175](../../../src/test/java/com/umc/product/recruiting/domain/RecruitingInterviewScheduleDomainTest.java#L175) · `@Test` · `확정_메일_상태_기록` — 확정 메일의 실패와 성공은 독립된 시도 횟수와 상태를 기록한다

### GraphQL

#### RecruitingGraphQlDtoEdgeCaseTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/dto/RecruitingGraphQlDtoEdgeCaseTest.java`

1. [L17](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/dto/RecruitingGraphQlDtoEdgeCaseTest.java#L17) · `@Test` · `convertNullableCollectionsToEmptyCollections` — nullable collection 입력은 빈 collection command·query로 변환한다
2. [L39](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/dto/RecruitingGraphQlDtoEdgeCaseTest.java#L39) · `@Test` · `rejectInvalidIdentifiersAndPageBoundaries` — 필수 ID와 양수 collection·page 경계를 fail-fast로 검증한다

#### RecruitingApplicationReviewGraphQlControllerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingApplicationReviewGraphQlControllerTest.java`

1. [L110](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingApplicationReviewGraphQlControllerTest.java#L110) · `@Test` · `searchApplicationsWithDefaultInput` — 평가용 지원서 Query는 input 생략 시 기본 filter와 page를 사용한다

#### RecruitingCredentialGraphQlRateLimitInterceptorTest (5개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingCredentialGraphQlRateLimitInterceptorTest.java`

1. [L118](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingCredentialGraphQlRateLimitInterceptorTest.java#L118) · `@Test` · `passInvalidOrAmbiguousDocument` — 문법 오류와 operation 선택이 모호한 요청은 credential 제한을 건너뛴다
2. [L130](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingCredentialGraphQlRateLimitInterceptorTest.java#L130) · `@Test` · `allowSelectedCredentialOperation` — operationName으로 고른 단일 credential 요청은 실행하고 rate limit header를 제공한다
3. [L149](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingCredentialGraphQlRateLimitInterceptorTest.java#L149) · `@Test` · `countInlineFragmentWithUnknownClientIp` — inline fragment의 credential field와 원격 주소가 없는 요청도 제한한다
4. [L171](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingCredentialGraphQlRateLimitInterceptorTest.java#L171) · `@Test` · `useHostStringForUnresolvedClientAddress` — DNS 해석 전 원격 주소는 host 문자열을 rate limit key로 사용한다
5. [L186](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingCredentialGraphQlRateLimitInterceptorTest.java#L186) · `@Test` · `countNullSelectionSetAsZero` — 선택 집합이 없으면 credential field 수는 0이다

#### RecruitingGraphQlControllerEdgeCaseTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlControllerEdgeCaseTest.java`

1. [L47](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlControllerEdgeCaseTest.java#L47) · `@Test` · `defaultNullableMutationInputs` — 로그인 지원서 제출·철회 Mutation은 input 생략 시 기본 request를 사용한다

#### RecruitingGraphQlPermissionSupportTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlPermissionSupportTest.java`

1. [L30](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlPermissionSupportTest.java#L30) · `@Test` · `delegateAllPermissionChecks` — 현재 회원과 명시 회원의 모집 type·season 권한을 동일한 permission 계약으로 검사한다
2. [L55](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlPermissionSupportTest.java#L55) · `@Test` · `failClosedForMissingScopeAndPrincipal` — 리소스 소속과 principal은 null일 때 fail-closed로 처리한다
3. [L68](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlPermissionSupportTest.java#L68) · `@Test` · `resolveCurrentMemberFromProvider` — 현재 회원 ID의 required·nullable 조회를 provider에 위임한다

#### RecruitingScheduleGraphQlControllerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingScheduleGraphQlControllerTest.java`

1. [L105](../../../src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingScheduleGraphQlControllerTest.java#L105) · `@Test` · `면접_생략_Mutation은_기본_input을_사용한다` — 면접 생략 Mutation은 input 생략 시 reason 없는 command를 전달한다

### Inbound Event

#### InterviewAvailabilityRequestedEventListenerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/in/event/InterviewAvailabilityRequestedEventListenerTest.java`

1. [L89](../../../src/test/java/com/umc/product/recruiting/adapter/in/event/InterviewAvailabilityRequestedEventListenerTest.java#L89) · `@Test` · `메시지_없는_메일_실패는_예외_이름을_기록한다` — 메시지 없는 메일 실패는 예외 class 이름을 기록한다
2. [L105](../../../src/test/java/com/umc/product/recruiting/adapter/in/event/InterviewAvailabilityRequestedEventListenerTest.java#L105) · `@Test` · `이벤트_metadata_계약` — 면접 일정 요청 이벤트는 식별자·시각을 생성하고 non-transactional 계약을 제공한다

### Persistence

#### RecruitingPersistenceAdapterTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingPersistenceAdapterTest.java`

1. [L275](../../../src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingPersistenceAdapterTest.java#L275) · `@Test` · `searchApplicationsWithCollectionAndPageFilters` — 지원서 검색은 다중 학교·차수 filter와 빈 filter, page 경계를 모두 지원한다

#### RecruitingPersistenceEdgeCaseTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingPersistenceEdgeCaseTest.java`

1. [L18](../../../src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingPersistenceEdgeCaseTest.java#L18) · `@Test` · `shortCircuitEmptyCollectionQueries` — 빈 IN 조건은 repository를 호출하지 않고 빈 결과를 반환한다
2. [L42](../../../src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingPersistenceEdgeCaseTest.java#L42) · `@Test` · `rejectApplicantLockWithoutRequiredScope` — 지원자 lock key는 기수와 회원 ID가 없으면 생성하지 않는다

### REST / Web

#### RecruitingWebDtoEdgeCaseTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/in/web/dto/request/RecruitingWebDtoEdgeCaseTest.java`

1. [L22](../../../src/test/java/com/umc/product/recruiting/adapter/in/web/dto/request/RecruitingWebDtoEdgeCaseTest.java#L22) · `@Test` · `handleNullableQuotaAndDecision` — nullable quota와 decision은 각각 빈 목록과 Bean Validation 위임 값으로 처리한다
2. [L29](../../../src/test/java/com/umc/product/recruiting/adapter/in/web/dto/request/RecruitingWebDtoEdgeCaseTest.java#L29) · `@Test` · `validateRoundInterviewConfigurations` — Round 생성·수정의 면접 설정은 활성·비활성 조합을 검증한다
3. [L71](../../../src/test/java/com/umc/product/recruiting/adapter/in/web/dto/request/RecruitingWebDtoEdgeCaseTest.java#L71) · `@Test` · `allowNullPeriodForSeparateNotNullValidation` — 면접 일정 기간 검증은 Bean Validation의 null 필드 검증과 충돌하지 않는다

#### RecruitingControllerEdgeCaseTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingControllerEdgeCaseTest.java`

1. [L55](../../../src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingControllerEdgeCaseTest.java#L55) · `@Test` · `delegateAnonymousUpdateAndSubmit` — 익명 지원서 수정·제출은 정규화 DTO와 원격 IP를 command에 전달한다
2. [L83](../../../src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingControllerEdgeCaseTest.java#L83) · `@Test` · `defaultNullableSubmitAndCancelRequests` — 로그인 지원서 제출·철회의 생략 가능한 body는 기본 request로 변환한다

## 3.23 `member` — 75개

### Application DTO / Port

#### SearchMemberInvitationQueryTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/member/application/port/in/query/dto/SearchMemberInvitationQueryTest.java`

1. [L56](../../../src/test/java/com/umc/product/member/application/port/in/query/dto/SearchMemberInvitationQueryTest.java#L56) · `@Test` · `공백과_null_입력을_정규화한다` — 공백 검색어와 null 제외 목록은 빈 조건으로 정규화한다
2. [L65](../../../src/test/java/com/umc/product/member/application/port/in/query/dto/SearchMemberInvitationQueryTest.java#L65) · `@Test` · `검색_결과의_음수_메타데이터를_거부한다` — 초대 검색 결과의 음수 페이지 정보와 전체 개수를 거부한다

### Application Service

#### EmailMemberRegisterServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/member/application/service/EmailMemberRegisterServiceTest.java`

1. [L130](../../../src/test/java/com/umc/product/member/application/service/EmailMemberRegisterServiceTest.java#L130) · `@Test` · `빈_batch_회원가입을_처리한다` — 빈 batch 회원가입 요청은 아무 작업 없이 빈 ID 목록을 반환한다
2. [L137](../../../src/test/java/com/umc/product/member/application/service/EmailMemberRegisterServiceTest.java#L137) · `@Test` · `batch_회원가입을_순서대로_처리한다` — batch 회원가입은 입력 순서대로 각 회원을 등록하고 ID 목록을 반환한다

#### MemberCredentialCommandServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/member/application/service/MemberCredentialCommandServiceTest.java`

1. [L73](../../../src/test/java/com/umc/product/member/application/service/MemberCredentialCommandServiceTest.java#L73) · `@Test` · `비밀번호를_변경한다` — 비밀번호 변경은 회원을 조회해 encoding된 비밀번호로 교체한다
2. [L85](../../../src/test/java/com/umc/product/member/application/service/MemberCredentialCommandServiceTest.java#L85) · `@Test` · `비밀번호_변경_회원_누락을_거부한다` — 비밀번호를 변경할 회원이 없으면 MEMBER_NOT_FOUND를 던진다
3. [L96](../../../src/test/java/com/umc/product/member/application/service/MemberCredentialCommandServiceTest.java#L96) · `@Test` · `null_회원_ID의_lock_조회를_거부한다` — lock 상태 조회의 회원 ID가 null이면 저장소 조회 전에 거부한다

#### MemberCredentialQueryServiceTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/application/service/MemberCredentialQueryServiceTest.java`

1. [L30](../../../src/test/java/com/umc/product/member/application/service/MemberCredentialQueryServiceTest.java#L30) · `@Test` · `빈_식별자를_즉시_반환한다` — null·blank email과 null member ID는 port 호출 없이 empty를 반환한다
2. [L41](../../../src/test/java/com/umc/product/member/application/service/MemberCredentialQueryServiceTest.java#L41) · `@Test` · `password_hash_없는_회원을_필터링한다` — 회원은 있지만 password hash가 없으면 자격증명을 숨긴다
3. [L52](../../../src/test/java/com/umc/product/member/application/service/MemberCredentialQueryServiceTest.java#L52) · `@Test` · `자격증명을_반환한다` — 등록된 password hash를 마스킹 DTO로 반환하고 email 존재 조회를 위임한다

#### MemberPermissionEvaluatorTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/application/service/MemberPermissionEvaluatorTest.java`

1. [L27](../../../src/test/java/com/umc/product/member/application/service/MemberPermissionEvaluatorTest.java#L27) · `@Test` · `챌린저의_회원_조회를_허용한다` — MEMBER 리소스를 지원하고 챌린저 이력이 있는 회원의 조회를 허용한다
2. [L42](../../../src/test/java/com/umc/product/member/application/service/MemberPermissionEvaluatorTest.java#L42) · `@Test` · `중앙_총괄단의_삭제를_허용한다` — 중앙 총괄단만 회원 삭제를 허용한다
3. [L57](../../../src/test/java/com/umc/product/member/application/service/MemberPermissionEvaluatorTest.java#L57) · `@Test` · `지원하지_않는_권한을_거부한다` — 정의되지 않은 회원 권한 종류는 내부 오류로 거부한다

#### MemberQueryServiceEdgeCaseTest (9개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java`

1. [L66](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java#L66) · `@Test` · `회원_정보를_결합한다` — 회원에 학교명·프로필 링크·역할을 결합한다
2. [L83](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java#L83) · `@Test` · `프로필_이미지가_없는_회원을_조회한다` — 프로필 이미지 ID가 없으면 storage를 조회하지 않고 null 링크를 반환한다
3. [L96](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java#L96) · `@Test` · `누락_회원_계약을_구분한다` — 존재하지 않는 ID는 Optional·null·필수 get 계약에 맞게 처리한다
4. [L109](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java#L109) · `@Test` · `프로필_없음을_빈_dto로_변환한다` — 회원 프로필이 없어도 빈 MemberProfileInfo를 반환한다
5. [L118](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java#L118) · `@Test` · `프로필_링크를_변환한다` — 회원 프로필의 모든 링크 유형을 누락 없이 변환한다
6. [L149](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java#L149) · `@Test` · `batch_누락_정보를_처리한다` — batch 회원의 school·profile 누락과 null school 상세를 graceful하게 처리한다
7. [L167](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java#L167) · `@Test` · `school_id_빈_입력을_처리한다` — school ID 배치와 단건 조회는 null·empty 입력을 port 호출 없이 처리한다
8. [L178](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java#L178) · `@Test` · `null_school을_제외한다` — 회원별 school ID는 null school을 제외하고 반환한다
9. [L189](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceEdgeCaseTest.java#L189) · `@Test` · `단순_조회를_port에_위임한다` — 존재·수·cursor 조회를 정확한 port 계약으로 위임한다

#### MemberRegistrationValidatorTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/application/service/MemberRegistrationValidatorTest.java`

1. [L50](../../../src/test/java/com/umc/product/member/application/service/MemberRegistrationValidatorTest.java#L50) · `@Test` · `null이면_파일을_조회하지_않는다` — 프로필 이미지 ID가 없으면 파일 저장소를 조회하지 않는다
2. [L58](../../../src/test/java/com/umc/product/member/application/service/MemberRegistrationValidatorTest.java#L58) · `@Test` · `ID가_있으면_파일_검증을_위임한다` — 프로필 이미지 ID가 있으면 존재하지 않을 때 예외를 위임한다
3. [L67](../../../src/test/java/com/umc/product/member/application/service/MemberRegistrationValidatorTest.java#L67) · `@Test` · `학교_존재_검증을_위임한다` — 학교 존재 검증은 학교 상세 조회에 위임한다
4. [L79](../../../src/test/java/com/umc/product/member/application/service/MemberRegistrationValidatorTest.java#L79) · `@Test` · `필수_약관을_모두_동의하면_통과한다` — 필수 약관을 모두 동의하면 선택 약관 상태와 관계없이 통과한다
5. [L93](../../../src/test/java/com/umc/product/member/application/service/MemberRegistrationValidatorTest.java#L93) · `@Test` · `필수_약관이_누락되면_거부한다` — 필수 약관 중 하나라도 없거나 미동의하면 거부한다
6. [L108](../../../src/test/java/com/umc/product/member/application/service/MemberRegistrationValidatorTest.java#L108) · `@Test` · `필수_약관이_없으면_빈_목록도_통과한다` — 필수 약관이 없으면 빈 동의 목록도 통과한다

#### MemberSearchServiceResidualTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/application/service/MemberSearchServiceResidualTest.java`

1. [L41](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceResidualTest.java#L41) · `@Test` · `활성_기수_챌린저의_빈_입력을_처리한다` — 활성 기수 챌린저 로딩은 빈 content와 null member ID를 빈 map으로 처리한다
2. [L59](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceResidualTest.java#L59) · `@Test` · `v2_검색_빈_입력을_처리한다` — 기수 정보와 챌린저가 없으면 generation map과 대표 챌린저도 비어 있다

#### MemberServiceTest (9개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/application/service/MemberServiceTest.java`

1. [L101](../../../src/test/java/com/umc/product/member/application/service/MemberServiceTest.java#L101) · `@Test` · `회원가입_전체_흐름을_순서대로_수행한다` — 검증 후 회원·OAuth·약관을 저장하고 알림과 감사 이벤트를 발행한다
2. [L131](../../../src/test/java/com/umc/product/member/application/service/MemberServiceTest.java#L131) · `@Test` · `검증_실패는_부수효과를_막는다` — 첫 검증이 실패하면 회원이나 외부 계정을 저장하지 않는다
3. [L151](../../../src/test/java/com/umc/product/member/application/service/MemberServiceTest.java#L151) · `@Test` · `입력_순서대로_일괄_가입한다` — 입력 순서대로 회원과 OAuth와 약관을 연결한다
4. [L177](../../../src/test/java/com/umc/product/member/application/service/MemberServiceTest.java#L177) · `@Test` · `빈_입력은_빈_결과를_반환한다` — 빈 입력은 빈 저장과 빈 OAuth 연결을 수행하고 빈 ID 목록을 반환한다
5. [L194](../../../src/test/java/com/umc/product/member/application/service/MemberServiceTest.java#L194) · `@Test` · `프로필_이미지를_변경한다` — 회원 조회와 이미지 검증 후 프로필 이미지를 변경한다
6. [L206](../../../src/test/java/com/umc/product/member/application/service/MemberServiceTest.java#L206) · `@Test` · `존재하지_않는_회원은_거부한다` — 존재하지 않는 회원은 이미지 검증 전에 거부한다
7. [L224](../../../src/test/java/com/umc/product/member/application/service/MemberServiceTest.java#L224) · `@Test` · `모든_OAuth를_해제하고_회원을_삭제한다` — 연결된 모든 OAuth를 탈퇴 모드로 해제한 뒤 회원과 권한 캐시를 삭제한다
8. [L258](../../../src/test/java/com/umc/product/member/application/service/MemberServiceTest.java#L258) · `@Test` · `OAuth가_없어도_회원과_캐시를_삭제한다` — 연결 OAuth가 없어도 회원과 권한 캐시는 삭제한다
9. [L273](../../../src/test/java/com/umc/product/member/application/service/MemberServiceTest.java#L273) · `@Test` · `존재하지_않는_회원은_탈퇴를_거부한다` — 존재하지 않는 회원은 OAuth 조회 전에 거부한다

#### MemberSummaryV2QueryServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/member/application/service/MemberSummaryV2QueryServiceTest.java`

1. [L246](../../../src/test/java/com/umc/product/member/application/service/MemberSummaryV2QueryServiceTest.java#L246) · `@Test` · `학교가_없고_일부_기수_정보가_누락되어도_이력을_안전하게_정렬한다` — 학교가_없고_일부_기수_정보가_누락되어도_이력을_안전하게_정렬한다

### GraphQL

#### MemberSchoolGraphQlResponseTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/adapter/in/graphql/dto/MemberSchoolGraphQlResponseTest.java`

1. [L17](../../../src/test/java/com/umc/product/member/adapter/in/graphql/dto/MemberSchoolGraphQlResponseTest.java#L17) · `@Test` · `null_필드를_변환한다` — null 링크와 시간은 빈 목록과 null 문자열로 변환한다
2. [L28](../../../src/test/java/com/umc/product/member/adapter/in/graphql/dto/MemberSchoolGraphQlResponseTest.java#L28) · `@Test` · `링크와_시간을_변환한다` — 학교 링크와 시간을 GraphQL 응답 형식으로 변환한다

#### MemberGraphQlControllerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/member/adapter/in/graphql/MemberGraphQlControllerTest.java`

1. [L964](../../../src/test/java/com/umc/product/member/adapter/in/graphql/MemberGraphQlControllerTest.java#L964) · `@Test` · `빈_batch_입력을_처리한다` — 빈 batch 입력은 외부 조회 없이 빈 결과를 반환한다
2. [L978](../../../src/test/java/com/umc/product/member/adapter/in/graphql/MemberGraphQlControllerTest.java#L978) · `@Test` · `batch_조회_중복_key를_처리한다` — batch 조회 결과의 중복 key는 최초 값을 안정적으로 유지한다

### Persistence

#### MemberPersistenceAdaptersTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdaptersTest.java`

1. [L43](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdaptersTest.java#L43) · `@Test` · `단건_조회를_위임한다` — 단건·이메일·닉네임·lock 조회를 각 저장소에 위임한다
2. [L58](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdaptersTest.java#L58) · `@Test` · `범위_조회를_변환한다` — 회원 ID와 학교 범위 조회를 위임하고 projection을 학교별 Set으로 묶는다
3. [L76](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdaptersTest.java#L76) · `@Test` · `빈_학교_범위를_처리한다` — 학교 ID 집합이 null 또는 비어 있으면 projection 조회를 생략한다
4. [L84](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdaptersTest.java#L84) · `@Test` · `기본_변경을_위임한다` — 존재 여부·저장·일괄 저장·삭제를 JPA 저장소에 위임한다
5. [L103](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdaptersTest.java#L103) · `@Test` · `검색을_위임한다` — 검색·범위 검색·cursor 조회를 해당 저장소에 위임한다
6. [L128](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdaptersTest.java#L128) · `@Test` · `회원_수를_집계한다` — 회원 수 집계는 빈 집합을 0으로 처리하고 그 외에는 저장소에 위임한다
7. [L140](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdaptersTest.java#L140) · `@Test` · `프로필_adapter를_검증한다` — 프로필 adapter는 단건·집합 조회와 모든 변경을 위임한다
8. [L159](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberPersistenceAdaptersTest.java#L159) · `@Test` · `시스템_역할_adapter를_검증한다` — 시스템 역할 adapter는 회원의 역할 목록 조회를 위임한다

#### MemberQueryRepositoryTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/member/adapter/out/persistence/MemberQueryRepositoryTest.java`

1. [L42](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberQueryRepositoryTest.java#L42) · `@Test` · `기본_단건_조회와_집계를_수행한다` — 전체 회원 수와 ID lock·닉네임 단건 조회를 수행한다
2. [L59](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberQueryRepositoryTest.java#L59) · `@Test` · `지부_학교_회원만_검색한다` — 지부 filter는 해당 지부에 연결된 학교 회원만 검색한다

### REST / Web

#### MemberInfoResponseAssemblerTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/adapter/in/web/assembler/MemberInfoResponseAssemblerTest.java`

1. [L51](../../../src/test/java/com/umc/product/member/adapter/in/web/assembler/MemberInfoResponseAssemblerTest.java#L51) · `@Test` · `챌린저_없는_회원을_조립한다` — 챌린저가 없으면 기수·지부 batch 조회 없이 회원과 프로필만 조립한다
2. [L69](../../../src/test/java/com/umc/product/member/adapter/in/web/assembler/MemberInfoResponseAssemblerTest.java#L69) · `@Test` · `챌린저_이력을_batch로_조립한다` — 챌린저의 중복 기수 ID를 제거해 기수·학교 지부를 batch 조회하고 응답한다
3. [L94](../../../src/test/java/com/umc/product/member/adapter/in/web/assembler/MemberInfoResponseAssemblerTest.java#L94) · `@Test` · `공개_응답을_마스킹한다` — 공개 응답은 email·상태·상벌점을 제거한다
4. [L116](../../../src/test/java/com/umc/product/member/adapter/in/web/assembler/MemberInfoResponseAssemblerTest.java#L116) · `@Test` · `학교_ID_없는_지부_조회를_처리한다` — 학교 ID가 없으면 지부 map에서 조회하지 않고 null을 반환한다

#### MemberInfoResponseTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponseTest.java`

1. [L26](../../../src/test/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponseTest.java#L26) · `@Test` · `구버전_factory_호환성을_유지한다` — 구버전 factory는 학교 이름과 프로필 URL의 기본값을 유지한다
2. [L43](../../../src/test/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponseTest.java#L43) · `@Test` · `현재_factory가_전체_정보를_변환한다` — 현재 factory는 회원·학교·프로필·역할 정보를 빠짐없이 변환한다
3. [L56](../../../src/test/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponseTest.java#L56) · `@Test` · `학교_미배정을_거부한다` — 학교가 배정되지 않은 회원은 학교 필수 기능을 사용할 수 없다
4. [L65](../../../src/test/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponseTest.java#L65) · `@Test` · `학교_배정_회원을_허용한다` — 학교가 배정된 회원은 학교 검증을 통과한다
5. [L73](../../../src/test/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponseTest.java#L73) · `@Test` · `memberInfo를_공개용으로_변환한다` — MemberInfo 공개 변환은 이메일만 비식별 기본값으로 바꾼다
6. [L86](../../../src/test/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponseTest.java#L86) · `@Test` · `프로필_없는_회원_응답을_변환한다` — 프로필 없는 회원 응답은 프로필을 비워 둔다
7. [L98](../../../src/test/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponseTest.java#L98) · `@Test` · `프로필과_챌린저_이력을_변환한다` — 프로필 있는 회원 응답은 프로필과 챌린저 이력을 함께 변환한다
8. [L111](../../../src/test/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponseTest.java#L111) · `@Test` · `공개_응답에서_민감_정보를_제거한다` — 회원 공개 응답은 이메일·상태와 모든 챌린저 상벌점을 제거한다

#### MemberCommandControllerUnitTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerUnitTest.java`

1. [L86](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerUnitTest.java#L86) · `@Test` · `oauth_회원가입을_조립한다` — OAuth·email 검증 claim과 Apple 자격 정보를 회원가입 command에 모두 전달한다
2. [L139](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerUnitTest.java#L139) · `@Test` · `profile_image를_수정한다` — profile image 수정은 로그인 회원 ID로 위임하고 최신 응답을 재조회한다
3. [L152](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerUnitTest.java#L152) · `@Test` · `profile_link를_수정한다` — 프로필 링크 수정은 요청 링크와 로그인 회원 ID를 command로 전달한다
4. [L175](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerUnitTest.java#L175) · `@Test` · `본인을_삭제한다` — 본인 삭제는 응답을 먼저 보존하고 provider access token을 command에 전달한다
5. [L196](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerUnitTest.java#L196) · `@Test` · `token_없는_삭제를_처리한다` — request body 없는 본인 삭제와 관리자 삭제는 provider token을 null로 전달한다

#### MemberQueryControllerTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/member/adapter/in/web/MemberQueryControllerTest.java`

1. [L83](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberQueryControllerTest.java#L83) · `@Test` · `공개_프로필을_조회한다` — 회원 ID로 공개 프로필을 조회한다

#### MemberV2ResponseContractTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/member/adapter/in/web/v2/dto/response/MemberV2ResponseContractTest.java`

1. [L25](../../../src/test/java/com/umc/product/member/adapter/in/web/v2/dto/response/MemberV2ResponseContractTest.java#L25) · `@Test` · `요약_중첩_응답을_변환한다` — 요약 응답은 현재 기수·활성 챌린저·전체 이력을 모두 변환한다
2. [L49](../../../src/test/java/com/umc/product/member/adapter/in/web/v2/dto/response/MemberV2ResponseContractTest.java#L49) · `@Test` · `현재_기수의_null_챌린저를_유지한다` — 현재 기수에 활성 챌린저가 없으면 중첩 challenger를 null로 유지한다
3. [L60](../../../src/test/java/com/umc/product/member/adapter/in/web/v2/dto/response/MemberV2ResponseContractTest.java#L60) · `@Test` · `검색_중첩_응답을_변환한다` — 검색 응답은 대표 챌린저와 모든 참여 이력을 변환하고 이메일을 마스킹한다

## 3.24 `challenger` — 89개

### Application Service

#### ChallengerApplicationResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/application/service/ChallengerApplicationResidualTest.java`

1. [L48](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerApplicationResidualTest.java#L48) · `@Test` · `단건_command를_처리한다` — 상태 변경·상벌점 부여·삭제 command를 도메인과 port에 위임한다
2. [L78](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerApplicationResidualTest.java#L78) · `@Test` · `상벌점_bulk를_처리한다` — 상벌점 bulk는 중복 challenger ID를 한 번 조회하고 각 command를 추가한다
3. [L97](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerApplicationResidualTest.java#L97) · `@Test` · `production_bulk를_차단한다` — production에서는 검증 없는 bulk command를 차단한다
4. [L106](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerApplicationResidualTest.java#L106) · `@Test` · `record_bulk와_삭제를_처리한다` — ChallengerRecord bulk 생성과 삭제를 port에 위임한다
5. [L134](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerApplicationResidualTest.java#L134) · `@Test` · `query_service를_변환한다` — record와 point query service는 entity를 DTO로 일괄 변환한다
6. [L167](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerApplicationResidualTest.java#L167) · `@Test` · `member_gisu_challenger를_변환한다` — member와 gisu로 조회한 challenger를 point 정보와 함께 변환한다

#### ChallengerQueryServiceEdgeCaseTest (19개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java`

1. [L54](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L54) · `@Test` · `id로_상벌점_포함_조회한다` — ID 조회는 상벌점을 합산한 챌린저 정보를 반환한다
2. [L68](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L68) · `@Test` · `find_by_id_성공` — findById는 존재하면 상벌점 포함 Optional을 반환한다
3. [L78](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L78) · `@Test` · `누락_결과를_추가_조회하지_않는다` — findById와 findByIdOrNull은 누락 결과를 각각 empty와 null로 반환한다
4. [L88](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L88) · `@Test` · `find_by_id_or_null_성공` — findByIdOrNull은 존재하는 챌린저를 상벌점과 함께 반환한다
5. [L98](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L98) · `@Test` · `member_gisu_조합_누락을_구분한다` — member·gisu 조합이 없으면 CHALLENGER_NOT_FOUND를 던진다
6. [L111](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L111) · `@Test` · `비활성_챌린저를_거부한다` — 활성 챌린저 조회는 비활성 상태를 거부한다
7. [L131](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L131) · `@Test` · `활성_챌린저를_반환한다` — 활성 챌린저 조회는 상벌점 정보를 반환한다
8. [L148](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L148) · `@Test` · `빈_목록은_즉시_반환한다` — 빈 챌린저 목록은 상벌점 배치 조회를 하지 않는다
9. [L157](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L157) · `@Test` · `상벌점을_배치로_조회한다` — 여러 챌린저의 상벌점을 한 번에 조회하고 누락 ID는 빈 목록으로 처리한다
10. [L171](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L171) · `@Test` · `빈_member_id_집합을_처리한다` — member ID 집합이 null 또는 비어 있으면 port를 호출하지 않는다
11. [L183](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L183) · `@Test` · `빈_port_결과를_처리한다` — port가 빈 목록을 반환하면 member 그룹 Map도 비어 있다
12. [L193](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L193) · `@Test` · `member_id별로_그룹핑한다` — 챌린저를 member ID별로 그룹핑하고 기본 정보 조회는 상벌점을 조회하지 않는다
13. [L210](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L210) · `@Test` · `batch_get을_map으로_변환한다` — batch get은 member ID를 key로 만들고 상벌점 누락을 빈 목록으로 처리한다
14. [L229](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L229) · `@Test` · `graceful_list를_변환한다` — graceful list는 결과 없음과 상벌점 누락을 구분해 처리한다
15. [L251](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L251) · `@Test` · `탈부와_제명을_거부한다` — 최신 챌린저가 탈부 또는 제명이면 작성자로 허용하지 않는다
16. [L283](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L283) · `@Test` · `활성_최신_챌린저를_반환한다` — 활성 최신 챌린저는 상태 정보로 반환한다
17. [L293](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L293) · `@Test` · `id_집합을_변환한다` — ID 집합은 null·empty를 즉시 반환하고 결과는 map과 list로 변환한다
18. [L309](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L309) · `@Test` · `간소화_조회는_상벌점을_제외한다` — 상벌점 제외 조회는 point UseCase를 호출하지 않는다
19. [L327](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerQueryServiceEdgeCaseTest.java#L327) · `@Test` · `기수_전체를_배치_조회한다` — 기수 전체 조회는 상벌점을 배치로 결합한다

#### ChallengerSearchServiceTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/application/service/ChallengerSearchServiceTest.java`

1. [L74](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerSearchServiceTest.java#L74) · `@Test` · `빈_검색_결과를_처리한다` — 빈 검색 결과는 부가 정보를 조회하지 않고 파트 집계만 반환한다
2. [L90](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerSearchServiceTest.java#L90) · `@Test` · `부가_정보를_배치로_결합한다` — 검색 행을 상벌점·역할·기수·프로필 링크와 배치로 결합한다
3. [L127](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerSearchServiceTest.java#L127) · `@Test` · `다음_페이지가_있는_경우` — size보다 한 건 많이 조회해 다음 페이지와 next cursor를 판별한다
4. [L153](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerSearchServiceTest.java#L153) · `@Test` · `다음_페이지가_없는_경우` — 결과가 size 이하이면 next cursor를 반환하지 않는다
5. [L166](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerSearchServiceTest.java#L166) · `@Test` · `글로벌_커서_검색을_변환한다` — 글로벌 커서 검색도 size+1 규칙과 프로필 링크 배치를 동일하게 적용한다
6. [L194](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerSearchServiceTest.java#L194) · `@Test` · `offset_v2를_변환한다` — 오프셋 V2는 각 챌린저의 상벌점을 결합해 Page를 변환한다
7. [L208](../../../src/test/java/com/umc/product/challenger/application/service/ChallengerSearchServiceTest.java#L208) · `@Test` · `cursor_v2를_변환한다` — 커서 V2는 port 순서를 유지하며 상벌점을 결합한다

#### ChallengerEvaluatorResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/application/service/evaluator/ChallengerEvaluatorResidualTest.java`

1. [L31](../../../src/test/java/com/umc/product/challenger/application/service/evaluator/ChallengerEvaluatorResidualTest.java#L31) · `@Test` · `challenger_권한_matrix` — challenger 생성은 학교 회장단, 수정·삭제는 중앙 총괄단만 허용한다
2. [L50](../../../src/test/java/com/umc/product/challenger/application/service/evaluator/ChallengerEvaluatorResidualTest.java#L50) · `@Test` · `challenger_record_권한_matrix` — challenger record 조회는 학교 회장단, 생성·삭제는 중앙 총괄단만 허용한다
3. [L71](../../../src/test/java/com/umc/product/challenger/application/service/evaluator/ChallengerEvaluatorResidualTest.java#L71) · `@Test` · `challenger_point_생성_수정_matrix` — 상벌점 생성·수정은 대상 기수 중앙국원 또는 같은 학교 회장단에 허용한다
4. [L103](../../../src/test/java/com/umc/product/challenger/application/service/evaluator/ChallengerEvaluatorResidualTest.java#L103) · `@Test` · `challenger_point_null_school과_기본_거부` — 대상 학교가 없으면 상벌점 생성·수정을 거부하고 정의되지 않은 권한도 거부한다

### Domain

#### ChallengerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/challenger/domain/ChallengerTest.java`

1. [L123](../../../src/test/java/com/umc/product/challenger/domain/ChallengerTest.java#L123) · `@Test` · `null_트랙_추가를_거부한다` — null 트랙은 기존 챌린저에 추가할 수 없다
2. [L131](../../../src/test/java/com/umc/product/challenger/domain/ChallengerTest.java#L131) · `@Test` · `null_트랙을_포함한_생성을_거부한다` — 생성 트랙 목록에는 null을 포함할 수 없다

### Persistence

#### ChallengerPersistenceAdapterTest (17개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java`

1. [L42](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L42) · `@Test` · `ID로_조회한다` — ID 조회는 Optional을 그대로 반환한다
2. [L52](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L52) · `@Test` · `필수_ID_조회_누락을_거부한다` — 필수 ID 조회 결과가 없으면 챌린저 없음 예외를 던진다
3. [L61](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L61) · `@Test` · `기본_목록_조회를_위임한다` — 회원·기수 조회와 회원/기수 목록 조회를 repository에 위임한다
4. [L78](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L78) · `@Test` · `null_회원은_존재하지_않는다` — 회원 ID가 null이면 존재 조회를 하지 않고 false를 반환한다
5. [L85](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L85) · `@Test` · `회원_존재를_조회한다` — 회원 ID가 있으면 repository의 존재 결과를 반환한다
6. [L93](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L93) · `@Test` · `빈_회원_ID_목록을_처리한다` — 회원 ID 집합이 null 또는 비어 있으면 목록 조회를 생략한다
7. [L101](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L101) · `@Test` · `회원_ID_목록으로_조회한다` — 회원 ID 집합으로 모든 챌린저를 조회한다
8. [L109](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L109) · `@Test` · `지부_조회와_ID_개수를_계산한다` — 지부 목록 조회와 집합 크기 계산을 수행한다
9. [L118](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L118) · `@Test` · `최신_챌린저_누락을_거부한다` — 회원의 최신 챌린저가 없으면 예외를 던진다
10. [L127](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L127) · `@Test` · `최신_챌린저를_조회한다` — 회원의 최신 챌린저를 반환한다
11. [L136](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L136) · `@Test` · `빈_batch_필수_조회를_처리한다` — batch 필수 조회는 null·빈 회원 집합이면 repository를 호출하지 않는다
12. [L144](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L144) · `@Test` · `batch_필수_조회_누락을_거부한다` — batch 필수 조회에서 일부 회원이 누락되면 예외를 던진다
13. [L155](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L155) · `@Test` · `batch_필수_조회를_수행한다` — batch 필수 조회에서 모든 회원이 존재하면 목록을 반환한다
14. [L168](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L168) · `@Test` · `선택_조회_입력_누락을_처리한다` — 선택 조회는 회원 집합 또는 기수가 없으면 빈 목록을 반환한다
15. [L177](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L177) · `@Test` · `선택_조회를_수행한다` — 선택 조회는 회원 집합과 기수로 목록을 조회한다
16. [L186](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L186) · `@Test` · `검색과_집계를_위임한다` — 최신 목록·검색·집계를 query repository에 위임한다
17. [L213](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceAdapterTest.java#L213) · `@Test` · `변경을_위임한다` — 저장·일괄 저장·삭제를 repository에 위임한다

#### ChallengerPersistenceResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceResidualTest.java`

1. [L31](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceResidualTest.java#L31) · `@Test` · `record_adapter를_위임한다` — record adapter의 조회·저장·삭제와 미존재 예외를 검증한다
2. [L60](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceResidualTest.java#L60) · `@Test` · `point_adapter를_위임한다` — point adapter의 조회·저장·삭제와 미존재 예외를 검증한다
3. [L84](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerPersistenceResidualTest.java#L84) · `@Test` · `point_querydsl을_구성한다` — point QueryDSL은 challenger ID 집합 조회 조건을 구성한다

#### ChallengerQueryRepositoryIntegrationTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerQueryRepositoryIntegrationTest.java`

1. [L81](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerQueryRepositoryIntegrationTest.java#L81) · `@Test` · `주요_조회와_집계를_수행한다` — 지부·paging·파트 집계·포인트·최근 기수 검색을 실제 DB 계약으로 검증한다
2. [L108](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerQueryRepositoryIntegrationTest.java#L108) · `@Test` · `filter와_cursor를_검증한다` — 모든 filter 조합과 cursor 검색·잘못된 cursor를 검증한다
3. [L137](../../../src/test/java/com/umc/product/challenger/adapter/out/persistence/ChallengerQueryRepositoryIntegrationTest.java#L137) · `@Test` · `지부_존재_조건을_구성한다` — 지부 존재 조건 helper는 null과 실제 지부를 구분한다

### REST / Web

#### ChallengerRecordResponseAssemblerTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerRecordResponseAssemblerTest.java`

1. [L46](../../../src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerRecordResponseAssemblerTest.java#L46) · `@Test` · `코드_조회_결과를_조립한다` — 코드 조회 결과에 기수와 학교·지부 정보를 조립한다
2. [L57](../../../src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerRecordResponseAssemblerTest.java#L57) · `@Test` · `아이디_조회_결과를_조립한다` — ID 조회 결과에 기수와 학교·지부 정보를 조립한다

#### ChallengerResponseAssemblerTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerResponseAssemblerTest.java`

1. [L49](../../../src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerResponseAssemblerTest.java#L49) · `@Test` · `챌린저_ID로_조립한다` — 챌린저 ID로 회원·기수·학교 지부를 조회해 단건 응답을 조립한다
2. [L68](../../../src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerResponseAssemblerTest.java#L68) · `@Test` · `빈_이력을_조립한다` — 챌린저 이력이 없으면 기수·지부 batch 조회를 생략한다
3. [L81](../../../src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerResponseAssemblerTest.java#L81) · `@Test` · `중복_기수를_batch로_조립한다` — 중복 기수는 제거하고 회원 학교와 조합해 한 번씩 batch 조회한다
4. [L97](../../../src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerResponseAssemblerTest.java#L97) · `@Test` · `학교가_없으면_지부_조회를_생략한다` — 학교가 없는 회원은 지부 batch 조회를 생략한다
5. [L107](../../../src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerResponseAssemblerTest.java#L107) · `@Test` · `학교_ID_없는_지부_조회` — 학교 ID가 없으면 지부 map 조회 결과도 null이다
6. [L116](../../../src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerResponseAssemblerTest.java#L116) · `@Test` · `매칭_지부가_없는_조회` — 기수 또는 학교에 매칭되는 지부가 없으면 null이다
7. [L130](../../../src/test/java/com/umc/product/challenger/adapter/in/web/assembler/ChallengerResponseAssemblerTest.java#L130) · `@Test` · `null_기수_ID를_거부한다` — 기수 ID가 null인 손상된 이력은 batch 조회 대상에서 제외되지만 조립 시 실패한다

#### ChallengerControllerResidualTest (9개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java`

1. [L59](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java#L59) · `@Test` · `일괄_생성한다` — 일괄 생성은 입력 순서대로 생성 결과를 조립한다
2. [L86](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java#L86) · `@Test` · `비활성화한다` — 비활성화 요청을 대상 ID와 함께 전달한다
3. [L96](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java#L96) · `@Test` · `파트를_변경한다` — 파트를 변경한 뒤 최신 챌린저 응답을 반환한다
4. [L111](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java#L111) · `@Test` · `물리_삭제한다` — 물리 삭제 명령에는 관리자 삭제 사유를 고정한다
5. [L137](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java#L137) · `@Test` · `아이디로_조회한다` — ID로 기록 응답을 조회한다
6. [L146](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java#L146) · `@Test` · `코드를_소비한다` — 코드를 현재 회원의 기록으로 소비한다
7. [L161](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java#L161) · `@Test` · `기록을_생성한다` — 기록을 생성하고 조립된 응답을 반환한다
8. [L180](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java#L180) · `@Test` · `기록을_일괄_생성한다` — 기록 일괄 생성은 모든 요청을 명령으로 변환하고 ID 목록을 반환한다
9. [L221](../../../src/test/java/com/umc/product/challenger/adapter/in/web/ChallengerControllerResidualTest.java#L221) · `@Test` · `전역_검색한다` — 전역 검색 요청과 커서 정보를 전달하고 응답으로 변환한다

#### ChallengerWebDtoResidualTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/dto/ChallengerWebDtoResidualTest.java`

1. [L33](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/ChallengerWebDtoResidualTest.java#L33) · `@Test` · `커서_검색은_유효하지_않은_크기를_기본값으로_보정한다` — 커서 검색은 유효하지 않은 크기를 기본값으로 보정하고 ACTIVE 상태만 조회한다
2. [L55](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/ChallengerWebDtoResidualTest.java#L55) · `@Test` · `커서_검색의_크기_경계값을_보정한다` — 커서 검색은 null 크기를 기본값으로, 큰 크기를 최대값으로 보정한다
3. [L63](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/ChallengerWebDtoResidualTest.java#L63) · `@Test` · `전역_검색은_상태와_크기_경계값을_보정한다` — 전역 검색은 ACTIVE와 GRADUATED를 포함하고 크기 경계값을 보정한다
4. [L85](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/ChallengerWebDtoResidualTest.java#L85) · `@Test` · `수정_요청은_명령으로_변환된다` — 수정 요청은 모든 식별자와 변경 사유를 명령으로 전달한다
5. [L102](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/ChallengerWebDtoResidualTest.java#L102) · `@Test` · `전역_검색_결과를_응답으로_변환한다` — 전역 검색 결과는 항목과 커서 메타데이터를 손실 없이 변환한다
6. [L126](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/ChallengerWebDtoResidualTest.java#L126) · `@Test` · `기록_생성_명령은_조직_단계별_엔티티를_만든다` — 기록 생성 명령은 일반·중앙·지부·학교 조직 규칙으로 엔티티를 만든다
7. [L143](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/ChallengerWebDtoResidualTest.java#L143) · `@Test` · `워크북_요약_레코드는_조회_값을_보존한다` — 워크북 요약 레코드는 모든 조회 값을 보존한다

#### ChallengerInfoResponseTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/challenger/adapter/in/web/dto/response/ChallengerInfoResponseTest.java`

1. [L26](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/response/ChallengerInfoResponseTest.java#L26) · `@Test` · `구버전_응답_별칭을_유지한다` — 역할 없는 구버전 응답은 상벌점 별칭과 회원 상태 별칭을 동일하게 유지한다
2. [L41](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/response/ChallengerInfoResponseTest.java#L41) · `@Test` · `같은_기수_역할을_변환한다` — 역할 포함 응답은 같은 기수의 역할을 API 응답으로 변환한다
3. [L62](../../../src/test/java/com/umc/product/challenger/adapter/in/web/dto/response/ChallengerInfoResponseTest.java#L62) · `@Test` · `공개_응답을_마스킹한다` — 공개 응답은 상벌점·이메일·회원 상태를 제거하고 활동 식별 정보는 보존한다

## 3.25 `authentication` — 125개

### Application Service

#### AuthenticationApplicationResidualTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/application/service/AuthenticationApplicationResidualTest.java`

1. [L55](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationApplicationResidualTest.java#L55) · `@Test` · `legacy_token을_발급한다` — legacy token 발급은 access·refresh token과 refresh row를 함께 생성한다
2. [L74](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationApplicationResidualTest.java#L74) · `@Test` · `secure_token과_sha256_실패를_처리한다` — 보안 token 생성과 SHA-256 미지원 방어 경로를 검증한다
3. [L91](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationApplicationResidualTest.java#L91) · `@Test` · `credential_rehash를_안전하게_처리한다` — 재해시는 정책 갱신이 필요할 때만 수행하고 저장 실패를 로그인에 전파하지 않는다
4. [L108](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationApplicationResidualTest.java#L108) · `@Test` · `verification_email_event를_전달한다` — 메일 발송 event listener는 수신자와 인증 코드를 그대로 전달한다
5. [L120](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationApplicationResidualTest.java#L120) · `@Test` · `member_oauth_query를_처리한다` — MemberOAuth query는 단건을 DTO로 변환하고 미존재를 도메인 예외로 변환한다
6. [L140](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationApplicationResidualTest.java#L140) · `@Test` · `issue_tokens를_위임한다` — AuthenticationService는 token 발급을 issuer에 위임한다
7. [L152](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationApplicationResidualTest.java#L152) · `@Test` · `이메일_인증을_재발급한다` — 이메일 인증 재발급은 새 코드로 세션을 갱신하고 발송 event를 게시한다
8. [L167](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationApplicationResidualTest.java#L167) · `@Test` · `이메일_인증_재발급과_검증을_방어한다` — 최근 발송 세션은 재발급을 제한하고 코드 없는 검증 요청은 즉시 거부한다

#### OAuthAuthenticationServiceEdgeCaseTest (21개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java`

1. [L86](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L86) · `@Test` · `기존_회원으로_로그인한다` — 연결된 provider 계정이면 기존 회원 결과와 성공 지표를 반환한다
2. [L100](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L100) · `@Test` · `email_없는_신규_회원_로그인을_처리한다` — 연결 계정이 없고 email도 없으면 신규 회원 결과를 안전하게 반환한다
3. [L116](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L116) · `@Test` · `access_token_검증_결과로_로그인한다` — access token 로그인은 provider 검증 결과를 공통 로그인 흐름에 전달한다
4. [L131](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L131) · `@Test` · `authorization_code를_검증해_로그인한다` — authorization code 로그인은 code와 redirect URI를 그대로 검증 port에 전달한다
5. [L163](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L163) · `@Test` · `이미_연결된_provider_계정을_거부한다` — 다른 회원이 이미 연결한 provider 계정은 저장하지 않는다
6. [L174](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L174) · `@Test` · `같은_provider_중복_연결을_거부한다` — 회원에게 같은 provider가 이미 있으면 다른 provider ID도 중복 연결하지 않는다
7. [L187](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L187) · `@Test` · `apple_연결_정보를_저장한다` — Apple 연결 정보의 refresh token과 client ID를 누락 없이 저장한다
8. [L215](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L215) · `@Test` · `bulk_provider_id_중복을_거부한다` — bulk 연결에서 하나라도 이미 연결된 provider ID가 있으면 전체 저장을 중단한다
9. [L231](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L231) · `@Test` · `bulk_회원_provider_중복을_거부한다` — bulk 연결에서 회원의 같은 provider 연결이 있으면 전체 저장을 중단한다
10. [L251](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L251) · `@Test` · `bulk_연결을_저장한다` — bulk 연결은 provider별 검증 후 입력 순서에 대응하는 저장 ID를 반환한다
11. [L274](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L274) · `@Test` · `빈_bulk_요청을_처리한다` — 빈 bulk 요청은 검증 조회 없이 빈 목록을 저장하고 빈 ID 목록을 반환한다
12. [L288](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L288) · `@Test` · `존재하지_않는_oauth를_거부한다` — 존재하지 않는 OAuth ID는 변경 없이 not found로 거부한다
13. [L300](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L300) · `@Test` · `다른_회원의_oauth를_거부한다` — 다른 회원의 OAuth 계정은 token revoke 전에 거부한다
14. [L313](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L313) · `@Test` · `apple_token을_폐기한다` — Apple refresh token과 client ID가 모두 있으면 provider token을 먼저 폐기한다
15. [L332](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L332) · `@Test` · `apple_자격_정보가_불완전하면_revoke를_생략한다` — Apple refresh token 또는 client ID가 없으면 외부 revoke만 생략하고 연결을 삭제한다
16. [L351](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L351) · `@Test` · `kakao_token_소유자를_검증하고_폐기한다` — Kakao access token 소유자가 일치하면 검증 후 revoke한다
17. [L365](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L365) · `@Test` · `kakao_token이_없으면_revoke를_생략한다` — Kakao access token이 없으면 외부 revoke를 생략한다
18. [L377](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L377) · `@Test` · `google_token_provider_불일치를_거부한다` — Google access token의 provider가 저장 계정과 다르면 revoke와 삭제를 막는다
19. [L393](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L393) · `@Test` · `google_token_소유자를_검증하고_폐기한다` — Google access token 소유자가 일치하면 검증 후 revoke한다
20. [L412](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L412) · `@Test` · `apple_자격_정보를_갱신한다` — 재로그인한 Apple 계정의 refresh token과 client ID를 함께 갱신한다
21. [L431](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceEdgeCaseTest.java#L431) · `@Test` · `없는_apple_계정을_거부한다` — 연결되지 않은 Apple 계정의 자격 정보는 생성하지 않는다

#### SsoTokenExchangeCommandServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/authentication/application/service/SsoTokenExchangeCommandServiceTest.java`

1. [L270](../../../src/test/java/com/umc/product/authentication/application/service/SsoTokenExchangeCommandServiceTest.java#L270) · `@Test` · `client_redirect_uri_화이트리스트_불일치를_거부한다` — authorization code와 일치해도 client 화이트리스트에 없는 redirect URI는 거부한다
2. [L291](../../../src/test/java/com/umc/product/authentication/application/service/SsoTokenExchangeCommandServiceTest.java#L291) · `@Test` · `android_client_type을_변환한다` — Android SSO service type은 ClientType.ANDROID로 변환한다

### Contract / Misc

#### AuthenticationResidualContractTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/AuthenticationResidualContractTest.java`

1. [L48](../../../src/test/java/com/umc/product/authentication/AuthenticationResidualContractTest.java#L48) · `@Test` · `sso_client_방어_계약` — SSO client는 필수 ID와 origin 정규화 규칙을 강제한다
2. [L63](../../../src/test/java/com/umc/product/authentication/AuthenticationResidualContractTest.java#L63) · `@Test` · `알_수_없는_oauth_provider를_거부한다` — 알 수 없는 OAuth provider registration ID는 도메인 예외로 거부한다
3. [L70](../../../src/test/java/com/umc/product/authentication/AuthenticationResidualContractTest.java#L70) · `@Test` · `oauth_설정_기본값을_적용한다` — OAuth 설정은 누락·잘못된 cache 값을 안전한 기본값으로 정규화한다
4. [L88](../../../src/test/java/com/umc/product/authentication/AuthenticationResidualContractTest.java#L88) · `@Test` · `요청_dto_변환과_마스킹` — 요청 DTO는 command로 변환하면서 비밀번호와 인증 token을 문자열에서 마스킹한다
5. [L107](../../../src/test/java/com/umc/product/authentication/AuthenticationResidualContractTest.java#L107) · `@Test` · `oauth_token_요청을_검증한다` — OAuth token 요청은 둘 중 하나를 요구하고 ID token을 우선한다
6. [L116](../../../src/test/java/com/umc/product/authentication/AuthenticationResidualContractTest.java#L116) · `@Test` · `command_factory와_마스킹` — command factory와 민감정보 마스킹 계약을 보존한다
7. [L131](../../../src/test/java/com/umc/product/authentication/AuthenticationResidualContractTest.java#L131) · `@Test` · `command_필수값을_검증한다` — 필수 OAuth·SSO command 값과 redirect 구성 값을 검증한다
8. [L157](../../../src/test/java/com/umc/product/authentication/AuthenticationResidualContractTest.java#L157) · `@Test` · `token_응답을_매핑한다` — token 재발급 응답은 발급 결과를 그대로 매핑한다

### External Adapter

#### AppleTokenVerifierEdgeCaseTest (14개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java`

1. [L54](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L54) · `@Test` · `email이_없어도_provider_id를_보존한다` — email claim이 없어도 provider ID를 보존한다
2. [L68](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L68) · `@Test` · `issuer_불일치를_거부한다` — issuer가 Apple이 아니면 공통 token 검증 실패로 변환한다
3. [L89](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L89) · `@Test` · `audience_불일치를_거부한다` — audience가 요청 client ID와 다르면 검증 실패로 처리한다
4. [L103](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L103) · `@Test` · `kid_없는_token을_거부한다` — JWT header에 kid가 없으면 유효하지 않은 OAuth token 예외를 보존한다
5. [L119](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L119) · `@Test` · `ios_client_id로_교환한다` — iOS 인가 코드는 iOS client ID로 교환하고 refresh token을 함께 반환한다
6. [L154](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L154) · `@Test` · `web과_android는_web_client_id를_사용한다` — WEB과 ANDROID는 web client ID를 사용한다
7. [L164](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L164) · `@Test` · `id_token_없는_응답을_거부한다` — token endpoint 응답에 id_token이 없으면 검증 실패로 처리한다
8. [L177](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L177) · `@Test` · `token_endpoint_오류를_구분한다` — Apple token endpoint 오류는 invalid access token으로 구분한다
9. [L192](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L192) · `@Test` · `손상된_오류_body도_안전하게_처리한다` — Apple 오류 body가 JSON이 아니어도 token 원문을 노출하지 않고 동일 예외로 처리한다
10. [L212](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L212) · `@Test` · `필수_claim을_담는다` — ES256 client secret에 Apple 필수 issuer·audience·subject를 담는다
11. [L231](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L231) · `@Test` · `private_key를_캐시한다` — private key는 한 번 파싱한 뒤 캐시하여 재사용한다
12. [L243](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L243) · `@Test` · `손상된_private_key를_거부한다` — 손상된 private key는 인증 도메인 예외로 정규화한다
13. [L259](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L259) · `@Test` · `저장된_client_id로_revoke한다` — 저장 당시 client ID와 refresh token을 Apple revoke endpoint에 전달한다
14. [L277](../../../src/test/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifierEdgeCaseTest.java#L277) · `@Test` · `revoke_오류를_변환한다` — revoke endpoint 오류를 token 검증 실패로 변환한다

#### GoogleTokenVerifierEdgeCaseTest (10개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java`

1. [L45](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L45) · `@Test` · `scheme_없는_issuer도_허용한다` — Google이 허용하는 scheme 없는 issuer도 정상 검증한다
2. [L66](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L66) · `@Test` · `issuer_불일치를_거부한다` — Google 이외 issuer는 INVALID_OAUTH_TOKEN으로 거부한다
3. [L87](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L87) · `@Test` · `손상된_jwt_header를_거부한다` — JWT header가 손상되면 유효하지 않은 OAuth token 예외를 보존한다
4. [L103](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L103) · `@Test` · `opaque_token을_tokeninfo로_검증한다` — opaque token은 tokeninfo endpoint에서 등록된 audience를 확인한다
5. [L123](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L123) · `@Test` · `빈_token도_access_token_경로를_사용한다` — 빈 token도 JWT로 오인하지 않고 tokeninfo 검증 경로를 사용한다
6. [L137](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L137) · `@Test` · `null_tokeninfo를_거부한다` — tokeninfo 응답이 없으면 검증 실패로 처리한다
7. [L151](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L151) · `@Test` · `tokeninfo_audience_불일치를_거부한다` — tokeninfo audience가 등록된 client ID가 아니면 거부한다
8. [L167](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L167) · `@Test` · `tokeninfo_http_오류를_구분한다` — tokeninfo HTTP 오류는 invalid access token으로 구분한다
9. [L186](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L186) · `@Test` · `token을_form으로_전달한다` — revoke token을 form body로 전달한다
10. [L200](../../../src/test/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifierEdgeCaseTest.java#L200) · `@Test` · `revoke_http_오류를_변환한다` — revoke HTTP 오류를 token 검증 실패로 정규화한다

#### KakaoTokenVerifierEdgeCaseTest (16개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java`

1. [L47](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L47) · `@Test` · `허용되지_않은_redirect_uri를_거부한다` — 화이트리스트에 없는 redirect URI는 외부 호출 없이 거부한다
2. [L60](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L60) · `@Test` · `access_token으로_fallback한다` — id_token이 없으면 access token userinfo 조회로 안전하게 fallback한다
3. [L89](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L89) · `@Test` · `빈_client_secret은_form에서_제외한다` — client secret이 비어 있으면 토큰 교환 form에 포함하지 않는다
4. [L106](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L106) · `@Test` · `빈_토큰_응답을_거부한다` — 토큰 교환 응답에 사용할 토큰이 없으면 검증 실패로 정규화한다
5. [L119](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L119) · `@Test` · `토큰_endpoint_오류를_전달한다` — 토큰 endpoint의 오류 상태는 유효하지 않은 OAuth token으로 전달한다
6. [L137](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L137) · `@Test` · `null_userinfo를_거부한다` — userinfo 응답이 없으면 provider 정보를 만들지 않는다
7. [L150](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L150) · `@Test` · `null_사용자_id를_거부한다` — userinfo의 사용자 ID가 null이면 검증 실패로 처리한다
8. [L162](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L162) · `@Test` · `profile_없는_응답을_허용한다` — profile이 없어도 사용자 ID와 이메일은 정상적으로 정규화한다
9. [L174](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L174) · `@Test` · `userinfo_http_오류를_구분한다` — userinfo HTTP 오류는 INVALID_OAUTH_TOKEN으로 구분한다
10. [L188](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L188) · `@Test` · `jwt_형식이_아니면_access_token으로_검증한다` — 점이 세 구간이 아닌 token은 access token 경로로 검증한다
11. [L199](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L199) · `@Test` · `잘못된_jwt를_거부한다` — JWT header에서 kid를 읽을 수 없으면 유효하지 않은 token으로 구분한다
12. [L215](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L215) · `@Test` · `access_token으로_연결을_해제한다` — 사용자 access token을 Authorization header로 전달한다
13. [L229](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L229) · `@Test` · `연결_해제_http_오류를_변환한다` — 사용자 연결 해제 HTTP 오류를 검증 실패로 변환한다
14. [L242](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L242) · `@Test` · `admin_key가_없으면_호출하지_않는다` — Admin key가 없으면 provider ID 유무와 관계없이 외부 호출을 생략한다
15. [L254](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L254) · `@Test` · `admin_key로_연결을_해제한다` — Admin 연결 해제는 KakaoAK header와 provider ID를 form으로 전달한다
16. [L271](../../../src/test/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifierEdgeCaseTest.java#L271) · `@Test` · `admin_연결_해제_http_오류를_변환한다` — Admin 연결 해제 HTTP 오류를 검증 실패로 보존한다

#### OAuthTokenVerificationAdapterTest (10개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java`

1. [L38](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L38) · `@Test` · `Google_token을_검증한다` — Google token 검증을 Google verifier에 위임한다
2. [L47](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L47) · `@Test` · `Kakao_token을_검증한다` — Kakao token 검증을 Kakao verifier에 위임한다
3. [L56](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L56) · `@Test` · `Apple_ID_token을_검증한다` — Apple ID token은 web client ID를 audience로 검증한다
4. [L66](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L66) · `@Test` · `Kakao_authorization_code를_검증한다` — Kakao authorization code와 redirect URI를 교환한다
5. [L79](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L79) · `@Test` · `Apple_일반_authorization_code_경로를_거부한다` — Apple authorization code는 일반 교환 경로를 거부한다
6. [L86](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L86) · `@Test` · `Google_authorization_code를_거부한다` — Google authorization code는 지원하지 않는다
7. [L93](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L93) · `@Test` · `Apple_authorization_code를_검증한다` — Apple authorization code와 client type 교환을 전용 verifier에 위임한다
8. [L105](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L105) · `@Test` · `Apple_token을_폐기한다` — Apple refresh token 폐기를 전용 verifier에 위임한다
9. [L113](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L113) · `@Test` · `Kakao_연결을_해제한다` — Kakao 사용자 연결 해제를 전용 verifier에 위임한다
10. [L121](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapterTest.java#L121) · `@Test` · `Google_token을_폐기한다` — Google token 폐기를 전용 verifier에 위임한다

#### OAuthVerifierFailureNormalizationTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/out/external/OAuthVerifierFailureNormalizationTest.java`

1. [L29](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthVerifierFailureNormalizationTest.java#L29) · `@Test` · `kakao_token_교환_transport_실패` — Kakao token 교환의 transport 실패를 공통 검증 실패로 변환한다
2. [L41](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthVerifierFailureNormalizationTest.java#L41) · `@Test` · `kakao_userinfo_transport_실패` — Kakao userinfo transport 실패를 공통 검증 실패로 변환한다
3. [L54](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthVerifierFailureNormalizationTest.java#L54) · `@Test` · `kakao_id_token_파싱_실패` — Kakao ID token 파싱 실패를 공통 검증 실패로 변환한다
4. [L67](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthVerifierFailureNormalizationTest.java#L67) · `@Test` · `kakao_unlink_transport_실패` — Kakao 사용자 및 admin 연결 해제 transport 실패를 공통 검증 실패로 변환한다
5. [L83](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthVerifierFailureNormalizationTest.java#L83) · `@Test` · `google_id_token_파싱_실패` — Google ID token 파싱 실패를 공통 검증 실패로 변환한다
6. [L96](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthVerifierFailureNormalizationTest.java#L96) · `@Test` · `google_transport_실패` — Google tokeninfo 및 revoke transport 실패를 공통 검증 실패로 변환한다
7. [L114](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OAuthVerifierFailureNormalizationTest.java#L114) · `@Test` · `apple_transport_실패` — Apple token 교환 및 revoke transport 실패를 공통 검증 실패로 변환한다

#### OidcPublicKeyResolverTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/out/external/OidcPublicKeyResolverTest.java`

1. [L138](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OidcPublicKeyResolverTest.java#L138) · `@Test` · `header에_kid가_없으면_거부한다` — JWT header에 kid가 없으면 INVALID_OAUTH_TOKEN을 그대로 보존한다
2. [L151](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OidcPublicKeyResolverTest.java#L151) · `@Test` · `synchronized_재확인에서_cache_hit를_사용한다` — 동기화 진입 직후 다른 요청이 채운 cache를 재사용한다
3. [L186](../../../src/test/java/com/umc/product/authentication/adapter/out/external/OidcPublicKeyResolverTest.java#L186) · `@Test` · `jwks_실패를_정규화한다` — JWKS HTTP 오류와 빈 응답을 공통 token 검증 실패로 변환한다

### Persistence

#### AuthenticationPersistenceResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/out/persistence/AuthenticationPersistenceResidualTest.java`

1. [L27](../../../src/test/java/com/umc/product/authentication/adapter/out/persistence/AuthenticationPersistenceResidualTest.java#L27) · `@Test` · `member_oauth_adapter_위임` — MemberOAuth adapter의 모든 조회·저장·삭제 연산을 repository에 위임한다
2. [L54](../../../src/test/java/com/umc/product/authentication/adapter/out/persistence/AuthenticationPersistenceResidualTest.java#L54) · `@Test` · `email_verification_adapter_위임` — EmailVerification adapter는 최신 조회·저장·만료 삭제를 각 repository에 위임한다
3. [L73](../../../src/test/java/com/umc/product/authentication/adapter/out/persistence/AuthenticationPersistenceResidualTest.java#L73) · `@Test` · `email_verification_querydsl_구성` — EmailVerification QueryDSL은 ID 및 이메일 최신 발송 순서 조회를 구성한다

### REST / Web

#### AuthenticationControllerUnitTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationControllerUnitTest.java`

1. [L51](../../../src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationControllerUnitTest.java#L51) · `@Test` · `Google_기존_회원_로그인` — Google 기존 회원은 ID token을 우선 사용하고 클라이언트 정보가 반영된 JWT를 받는다
2. [L71](../../../src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationControllerUnitTest.java#L71) · `@Test` · `Kakao_신규_회원_로그인` — Kakao 신규 회원은 legacy access token을 검증하고 회원가입 검증 토큰을 받는다
3. [L92](../../../src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationControllerUnitTest.java#L92) · `@Test` · `Kakao_authorization_code_로그인` — Kakao authorization code 로그인은 code와 redirect URI를 그대로 전달한다
4. [L109](../../../src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationControllerUnitTest.java#L109) · `@Test` · `Apple_기존_회원_refresh_token_갱신` — Apple 기존 회원은 refresh token과 client ID를 갱신한 뒤 로그인한다
5. [L128](../../../src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationControllerUnitTest.java#L128) · `@Test` · `Apple_refresh_token_없는_기존_회원` — Apple 기존 회원의 code 교환 결과에 refresh token이 없으면 저장 값을 덮어쓰지 않는다
6. [L147](../../../src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationControllerUnitTest.java#L147) · `@Test` · `Apple_신규_회원` — Apple 신규 회원은 refresh token 저장 없이 회원가입 검증 토큰을 받는다

#### AuthenticationWebResidualTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationWebResidualTest.java`

1. [L29](../../../src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationWebResidualTest.java#L29) · `@Test` · `token_재발급을_위임한다` — token 재발급 controller는 command 변환과 응답 매핑을 수행한다
2. [L47](../../../src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationWebResidualTest.java#L47) · `@Test` · `sso_cookie의_domain과_만료를_처리한다` — SSO cookie는 domain을 적용하고 만료 시 max-age를 0으로 제한한다
3. [L66](../../../src/test/java/com/umc/product/authentication/adapter/in/web/AuthenticationWebResidualTest.java#L66) · `@Test` · `referer_origin을_정규화한다` — Referer origin은 scheme·host·port만 보존하고 잘못된 URI를 무시한다

#### CredentialAuthenticationControllerUnitTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/in/web/CredentialAuthenticationControllerUnitTest.java`

1. [L46](../../../src/test/java/com/umc/product/authentication/adapter/in/web/CredentialAuthenticationControllerUnitTest.java#L46) · `@Test` · `자격증명을_등록한다` — 현재 회원에게 비밀번호 자격증명을 등록한다
2. [L55](../../../src/test/java/com/umc/product/authentication/adapter/in/web/CredentialAuthenticationControllerUnitTest.java#L55) · `@Test` · `비밀번호를_변경한다` — 현재 비밀번호와 새 비밀번호를 사용해 비밀번호를 변경한다
3. [L67](../../../src/test/java/com/umc/product/authentication/adapter/in/web/CredentialAuthenticationControllerUnitTest.java#L67) · `@Test` · `이메일로_비밀번호를_초기화한다` — PASSWORD_RESET 용도의 이메일 인증 토큰으로 비밀번호를 초기화한다
4. [L80](../../../src/test/java/com/umc/product/authentication/adapter/in/web/CredentialAuthenticationControllerUnitTest.java#L80) · `@Test` · `이메일_사용_가능성을_조회한다` — 이메일 사용 가능 여부에 원본 이메일을 함께 응답한다
5. [L91](../../../src/test/java/com/umc/product/authentication/adapter/in/web/CredentialAuthenticationControllerUnitTest.java#L91) · `@Test` · `이메일로_로그인한다` — 이메일 로그인 결과의 회원 ID와 두 토큰을 응답한다

#### EmailAuthenticationControllerUnitTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/in/web/EmailAuthenticationControllerUnitTest.java`

1. [L42](../../../src/test/java/com/umc/product/authentication/adapter/in/web/EmailAuthenticationControllerUnitTest.java#L42) · `@Test` · `이메일_code를_검증한다` — 인증 ID와 code를 검증해 이메일 인증 토큰을 응답한다
2. [L58](../../../src/test/java/com/umc/product/authentication/adapter/in/web/EmailAuthenticationControllerUnitTest.java#L58) · `@Test` · `이메일_인증_세션을_생성한다` — 이메일과 인증 목적을 고정한 세션 ID를 응답한다
3. [L71](../../../src/test/java/com/umc/product/authentication/adapter/in/web/EmailAuthenticationControllerUnitTest.java#L71) · `@Test` · `이메일_인증을_재전송한다` — 재전송 요청은 기존 이메일 인증 세션 ID를 그대로 위임한다

#### MemberOAuthControllerUnitTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/in/web/MemberOAuthControllerUnitTest.java`

1. [L42](../../../src/test/java/com/umc/product/authentication/adapter/in/web/MemberOAuthControllerUnitTest.java#L42) · `@Test` · `OAuth_계정을_연결한다` — 검증 토큰의 provider 계정을 현재 회원에게 연결하고 최신 목록을 반환한다
2. [L63](../../../src/test/java/com/umc/product/authentication/adapter/in/web/MemberOAuthControllerUnitTest.java#L63) · `@Test` · `provider_token과_OAuth를_제거한다` — provider token이 주어지면 외부 연결 해제 정보와 함께 OAuth를 제거한다
3. [L83](../../../src/test/java/com/umc/product/authentication/adapter/in/web/MemberOAuthControllerUnitTest.java#L83) · `@Test` · `body_없이_OAuth를_제거한다` — 요청 body가 없으면 provider token 없이 OAuth를 제거한다
4. [L96](../../../src/test/java/com/umc/product/authentication/adapter/in/web/MemberOAuthControllerUnitTest.java#L96) · `@Test` · `내_OAuth_목록을_조회한다` — 현재 회원의 OAuth 목록을 조회한다

### Scheduler

#### EmailVerificationRetentionSchedulerTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/authentication/adapter/in/scheduler/EmailVerificationRetentionSchedulerTest.java`

1. [L64](../../../src/test/java/com/umc/product/authentication/adapter/in/scheduler/EmailVerificationRetentionSchedulerTest.java#L64) · `@Test` · `삭제_대상이_없는_성공을_기록한다` — 삭제 대상이 없어도 success metric을 기록한다
2. [L78](../../../src/test/java/com/umc/product/authentication/adapter/in/scheduler/EmailVerificationRetentionSchedulerTest.java#L78) · `@Test` · `삭제_실패를_기록하고_전파한다` — 삭제 실패는 failure metric을 기록하고 원래 예외를 다시 던진다

## 3.26 `authorization` — 88개

### Application Service

#### AuthoritySnapshotCacheSerializerTest (4개, 보강 파일)

- 위치: `src/test/java/com/umc/product/authorization/application/service/AuthoritySnapshotCacheSerializerTest.java`

1. [L83](../../../src/test/java/com/umc/product/authorization/application/service/AuthoritySnapshotCacheSerializerTest.java#L83) · `@Test` · `reject_empty_or_malformed_payload` — null·공백·잘못된 JSON 캐시 payload는 복원하지 않는다
2. [L98](../../../src/test/java/com/umc/product/authorization/application/service/AuthoritySnapshotCacheSerializerTest.java#L98) · `@Test` · `wrap_serialization_failure` — ObjectMapper 직렬화 실패는 정책 평가 실패로 변환한다
3. [L111](../../../src/test/java/com/umc/product/authorization/application/service/AuthoritySnapshotCacheSerializerTest.java#L111) · `@Test` · `reject_null_json_tree` — ObjectMapper가 null tree를 반환하면 schema 검증에서 거부한다
4. [L122](../../../src/test/java/com/umc/product/authorization/application/service/AuthoritySnapshotCacheSerializerTest.java#L122) · `@Test` · `normalize_null_collections` — 캐시 DTO의 null collection은 불변 빈 collection으로 정규화한다

#### AuthorizationServiceCacheTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/authorization/application/service/AuthorizationServiceCacheTest.java`

1. [L226](../../../src/test/java/com/umc/product/authorization/application/service/AuthorizationServiceCacheTest.java#L226) · `@Test` · `skips_cache_write_when_serialization_fails` — snapshot 직렬화가 실패해도 최신 subject 조회는 성공하고 캐시 저장만 건너뛴다

#### AuthorizationServicePolicyTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/application/service/AuthorizationServicePolicyTest.java`

1. [L85](../../../src/test/java/com/umc/product/authorization/application/service/AuthorizationServicePolicyTest.java#L85) · `@Test` · `등록된_평가기의_결과를_반환한다` — 등록된 리소스 평가기의 결과를 그대로 반환한다
2. [L94](../../../src/test/java/com/umc/product/authorization/application/service/AuthorizationServicePolicyTest.java#L94) · `@Test` · `회원_ID로_subject를_로드해_평가한다` — 회원 ID 기반 check는 subject를 로드한 뒤 동일 평가기에 위임한다
3. [L104](../../../src/test/java/com/umc/product/authorization/application/service/AuthorizationServicePolicyTest.java#L104) · `@Test` · `평가기가_거부하면_false다` — 평가기가 거부한 권한은 false를 반환한다
4. [L112](../../../src/test/java/com/umc/product/authorization/application/service/AuthorizationServicePolicyTest.java#L112) · `@Test` · `평가기가_없으면_예외다` — 리소스 타입에 대응하는 평가기가 없으면 명시적인 예외를 던진다
5. [L124](../../../src/test/java/com/umc/product/authorization/application/service/AuthorizationServicePolicyTest.java#L124) · `@Test` · `중복_평가기_등록은_실패한다` — 같은 리소스 타입 평가기가 중복 등록되면 애플리케이션 시작 시 실패한다
6. [L139](../../../src/test/java/com/umc/product/authorization/application/service/AuthorizationServicePolicyTest.java#L139) · `@Test` · `허용된_요청은_통과한다` — 허용된 요청은 예외나 거부 지표 없이 통과한다
7. [L150](../../../src/test/java/com/umc/product/authorization/application/service/AuthorizationServicePolicyTest.java#L150) · `@Test` · `거부된_요청은_지표와_예외를_남긴다` — 거부된 요청은 보안 지표를 기록하고 접근 거부 예외를 던진다

#### AuthoritySnapshotCacheCommandServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/authorization/application/service/command/AuthoritySnapshotCacheCommandServiceTest.java`

1. [L94](../../../src/test/java/com/umc/product/authorization/application/service/command/AuthoritySnapshotCacheCommandServiceTest.java#L94) · `@Test` · `does_not_evict_without_member_ids` — 회원 ID 목록이 null이거나 비어 있으면 캐시를 제거하지 않는다

#### ChallengerRoleAnyGisuAuthorityTest (11개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java`

1. [L53](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L53) · `@Test` · `존재하지_않는_회원은_모든_기수_무관_권한을_거부한다` — 존재하지 않는 회원은 모든 기수 무관 권한을 거부한다
2. [L68](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L68) · `@Test` · `SUPER_ADMIN은_모든_기수_무관_권한을_허용한다` — SUPER_ADMIN은 챌린저 역할 조회 없이 모든 기수 무관 권한을 허용한다
3. [L84](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L84) · `@Test` · `지원하지_않는_시스템_역할은_입력_예외로_거부한다` — 지원하지 않는 시스템 역할 값은 입력 예외로 거부한다
4. [L97](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L97) · `@Test` · `시스템_역할이_없으면_SUPER_ADMIN_권한을_거부한다` — 시스템 역할이 없으면 SUPER_ADMIN 권한을 거부한다
5. [L110](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L110) · `@Test` · `중앙_역할을_계층에_맞게_구분한다` — 중앙 총괄단과 중앙 일반 운영진을 계층에 맞게 구분한다
6. [L126](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L126) · `@Test` · `학교_역할은_중앙_권한이_아니다` — 학교 역할은 중앙 권한으로 인정하지 않는다
7. [L142](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L142) · `@Test` · `학교_ID가_없으면_입력_예외가_발생한다` — 학교 ID가 없으면 회장단과 관리자 판정을 거부한다
8. [L157](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L157) · `@Test` · `조직_유형과_학교_ID가_모두_일치해야_한다` — 조직 유형과 학교 ID가 모두 일치할 때만 학교 회장단으로 인정한다
9. [L172](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L172) · `@Test` · `같은_학교의_부회장은_학교_회장단이다` — 같은 학교의 부회장은 학교 회장단이다
10. [L192](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L192) · `@Test` · `지부_ID가_없으면_입력_예외가_발생한다` — 지부 ID가 없으면 입력 예외가 발생한다
11. [L201](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleAnyGisuAuthorityTest.java#L201) · `@Test` · `지부장_역할은_조직과_ID가_모두_일치해야_한다` — 지부장 역할은 조직 유형과 지부 ID가 모두 일치해야 한다

#### ChallengerRoleGisuAuthorityTest (13개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java`

1. [L52](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L52) · `@Test` · `null_기수는_모든_기수_기반_권한에서_거부한다` — 기수 기반 권한 조회는 null 기수를 모두 거부한다
2. [L70](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L70) · `@Test` · `null_조직_ID는_거부한다` — 학교와 지부 기반 권한은 null 조직 ID를 거부한다
3. [L82](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L82) · `@Test` · `null_역할은_false다` — 단일 역할 조회는 null 역할을 false로 처리한다
4. [L89](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L89) · `@Test` · `ANY의_null과_빈_배열은_false다` — ANY 역할 조회는 null 또는 빈 역할 배열을 false로 처리한다
5. [L99](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L99) · `@Test` · `ALL의_null과_빈_배열은_true다` — ALL 역할 조회는 존재하는 회원의 null 또는 빈 역할 배열을 true로 처리한다
6. [L114](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L114) · `@Test` · `존재하지_않는_회원은_모든_기수_권한을_거부한다` — 존재하지 않는 회원은 빈 ALL 조건을 포함해 모든 기수 권한을 거부한다
7. [L131](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L131) · `@Test` · `SUPER_ADMIN은_모든_조직_권한을_허용한다` — SUPER_ADMIN은 역할 저장소를 조회하지 않고 모든 조직 권한을 허용한다
8. [L151](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L151) · `@Test` · `단일과_ANY는_정확히_일치하는_역할만_허용한다` — 단일 역할과 ANY 역할은 정확히 일치하는 역할만 허용한다
9. [L179](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L179) · `@Test` · `ALL은_중복을_제거하고_모든_역할을_확인한다` — ALL 역할은 중복 입력을 제거하고 모든 대상 역할의 포함 여부를 확인한다
10. [L209](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L209) · `@Test` · `중앙_권한은_같은_기수의_역할만_인정한다` — 중앙 권한은 같은 기수의 중앙 역할 계층만 인정한다
11. [L221](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L221) · `@Test` · `학교_권한은_모든_범위가_일치해야_한다` — 학교 권한은 조직 유형과 학교 ID와 역할 계층이 모두 일치해야 한다
12. [L236](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L236) · `@Test` · `지부장_권한은_기수와_지부가_일치해야_한다` — 지부장 권한은 같은 기수와 지부에 부여된 지부장 역할만 인정한다
13. [L253](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleGisuAuthorityTest.java#L253) · `@Test` · `기수_범위_port의_계약을_신뢰한다` — 기수 범위 조회 port의 결과는 서비스에서 기수를 재검증하지 않고 신뢰한다

#### ChallengerRoleListQueryTest (9개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java`

1. [L54](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java#L54) · `@Test` · `역할_ID_조회는_기수_정보까지_결합한다` — 역할 ID 조회는 기수 정보까지 결합한다
2. [L68](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java#L68) · `@Test` · `회원_역할_상세_목록은_각_기수를_결합한다` — 회원 역할 상세 목록은 각 역할의 기수를 결합한다
3. [L87](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java#L87) · `@Test` · `특정_기수_역할_목록을_상세_정보로_변환한다` — 특정 기수 역할 목록은 저장소 필터 결과를 상세 정보로 변환한다
4. [L102](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java#L102) · `@Test` · `경량_목록은_기수_조회가_없다` — 경량 목록은 기수 조회 없이 역할 범위만 반환한다
5. [L123](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java#L123) · `@Test` · `기수_역할_타입은_중복을_제거한다` — 기수 역할 타입은 중복을 제거하고 최초 순서를 유지한다
6. [L140](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java#L140) · `@Test` · `학교_역할_타입은_학교_범위로_필터한다` — 학교 역할 타입은 중앙과 다른 학교 역할을 제외하고 중복을 제거한다
7. [L155](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java#L155) · `@Test` · `담당_파트는_null과_중복을_제거한다` — 담당 파트는 null을 제거하고 중복 없이 반환한다
8. [L175](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java#L175) · `@Test` · `null과_빈_ID는_빈_Map이다` — null 또는 빈 챌린저 ID 집합은 저장소 조회 없이 빈 Map을 반환한다
9. [L185](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleListQueryTest.java#L185) · `@Test` · `역할은_챌린저_ID로_그룹화한다` — 챌린저별 역할은 챌린저 ID로 그룹화하고 역할 순서를 유지한다

#### ChallengerRoleQueryServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleQueryServiceTest.java`

1. [L226](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleQueryServiceTest.java#L226) · `@Test` · `compatibility_default_methods_delegate` — 호환 조회 UseCase의 default 메서드는 분리된 list 계약에 위임한다
2. [L249](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleQueryServiceTest.java#L249) · `@Test` · `unsupported_system_role_is_rejected` — 지원하지 않는 system role 값은 조용히 SUPER_ADMIN으로 승격하지 않고 거부한다
3. [L259](../../../src/test/java/com/umc/product/authorization/application/service/query/ChallengerRoleQueryServiceTest.java#L259) · `@Test` · `chapter_president_matches_both_scopes` — 지부장은 기수 무관 및 기수 범위 지부장 정책을 통과한다

#### CheckResourcePermissionValidationTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionValidationTest.java`

1. [L56](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionValidationTest.java#L56) · `@Test` · `빈_query_목록을_거부한다` — null·empty query 목록은 subject를 로드하기 전에 거부한다
2. [L64](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionValidationTest.java#L64) · `@Test` · `null_query를_거부한다` — query 요소 null은 subject를 로드하기 전에 거부한다
3. [L71](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionValidationTest.java#L71) · `@Test` · `resource_type_오류를_구분한다` — resource type null과 evaluator 미등록을 각각 입력 오류와 미지원 오류로 구분한다
4. [L97](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionValidationTest.java#L97) · `@Test` · `resource_ids_계약을_검증한다` — resourceIds는 null은 타입 단위로 허용하지만 empty와 null 요소는 거부한다
5. [L114](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionValidationTest.java#L114) · `@Test` · `permission_types_계약을_검증한다` — permissionTypes는 null은 전체 권한으로 허용하지만 empty와 null 요소는 거부한다
6. [L131](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionValidationTest.java#L131) · `@Test` · `null_필드는_전체_권한을_평가한다` — resourceIds와 permissionTypes가 둘 다 null이면 타입의 전체 지원 권한을 ordinal 순서로 평가한다
7. [L152](../../../src/test/java/com/umc/product/authorization/application/service/query/CheckResourcePermissionValidationTest.java#L152) · `@Test` · `permission_순서를_정규화한다` — 요청 permission 순서와 관계없이 enum ordinal 순서로 안정적으로 반환한다

### Aspect

#### AccessControlAspectTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspectTest.java`

1. [L73](../../../src/test/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspectTest.java#L73) · `@Test` · `인증_정보가_없으면_거부한다` — 인증 정보가 없으면 권한 평가 전에 거부한다
2. [L82](../../../src/test/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspectTest.java#L82) · `@Test` · `인증되지_않은_토큰이면_거부한다` — 인증되지 않은 토큰이면 권한 평가 전에 거부한다
3. [L94](../../../src/test/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspectTest.java#L94) · `@Test` · `다른_principal이면_거부한다` — MemberPrincipal이 아닌 인증 주체이면 잘못된 인증으로 거부한다
4. [L118](../../../src/test/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspectTest.java#L118) · `@Test` · `빈_표현식은_타입_전체_권한이다` — 빈 표현식은 타입 전체 권한으로 평가한다
5. [L134](../../../src/test/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspectTest.java#L134) · `@Test` · `null_표현식은_타입_전체_권한이다` — null 표현식도 타입 전체 권한으로 평가한다
6. [L151](../../../src/test/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspectTest.java#L151) · `@Test` · `메서드_인자를_리소스_id로_변환한다` — 메서드 인자 표현식은 문자열 리소스 ID로 변환한다
7. [L167](../../../src/test/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspectTest.java#L167) · `@Test` · `null_평가값은_타입_전체_권한이다` — 표현식 결과가 null이면 타입 전체 권한으로 평가한다
8. [L184](../../../src/test/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspectTest.java#L184) · `@Test` · `권한이_없으면_원_메서드를_실행하지_않는다` — 권한 평가가 false이면 지정한 메시지의 도메인 예외를 던지고 원 메서드를 실행하지 않는다

### Domain

#### AuthoritySnapshotMatrixTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/domain/AuthoritySnapshotMatrixTest.java`

1. [L34](../../../src/test/java/com/umc/product/authorization/domain/AuthoritySnapshotMatrixTest.java#L34) · `@Test` · `null_컬렉션은_빈_불변_컬렉션으로_정규화한다` — null 컬렉션은 빈 불변 컬렉션으로 정규화한다
2. [L46](../../../src/test/java/com/umc/product/authorization/domain/AuthoritySnapshotMatrixTest.java#L46) · `@Test` · `입력_컬렉션을_방어적으로_복사한다` — 입력 컬렉션 변경이 snapshot에 전파되지 않는다
3. [L67](../../../src/test/java/com/umc/product/authorization/domain/AuthoritySnapshotMatrixTest.java#L67) · `@Test` · `SubjectAttributes와_정보_손실_없이_왕복한다` — SubjectAttributes와 snapshot은 정보 손실 없이 왕복한다
4. [L97](../../../src/test/java/com/umc/product/authorization/domain/AuthoritySnapshotMatrixTest.java#L97) · `@Test` · `SUPER_ADMIN은_모든_권한을_통과한다` — SUPER_ADMIN은 역할 목록이 없어도 모든 권한 정책을 통과한다
5. [L126](../../../src/test/java/com/umc/product/authorization/domain/AuthoritySnapshotMatrixTest.java#L126) · `@Test` · `중앙_권한은_역할_계층과_기수를_적용한다` — 중앙 권한은 역할 계층과 기수 범위를 함께 적용한다
6. [L144](../../../src/test/java/com/umc/product/authorization/domain/AuthoritySnapshotMatrixTest.java#L144) · `@Test` · `학교_권한은_모든_범위를_적용한다` — 학교 권한은 조직 유형과 학교와 기수와 역할 계층을 모두 적용한다
7. [L163](../../../src/test/java/com/umc/product/authorization/domain/AuthoritySnapshotMatrixTest.java#L163) · `@Test` · `지부장_권한은_모든_범위를_적용한다` — 지부장 권한은 조직 유형과 지부와 기수를 모두 적용한다

#### AuthorizationResidualContractTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/domain/AuthorizationResidualContractTest.java`

1. [L21](../../../src/test/java/com/umc/product/authorization/domain/AuthorizationResidualContractTest.java#L21) · `@Test` · `legacy_role_info_factory` — 구버전 역할 Info factory도 entity 필드를 보존한다
2. [L38](../../../src/test/java/com/umc/product/authorization/domain/AuthorizationResidualContractTest.java#L38) · `@Test` · `non_central_role_requires_organization_id` — 중앙 외 역할은 생성과 수정 모두 organization ID가 필수다
3. [L51](../../../src/test/java/com/umc/product/authorization/domain/AuthorizationResidualContractTest.java#L51) · `@Test` · `residual_snapshot_policies` — 중앙 멤버와 지부장 정책은 각 역할 범위에서 true를 반환한다

#### ResourcePermissionContractTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/domain/ResourcePermissionContractTest.java`

1. [L20](../../../src/test/java/com/umc/product/authorization/domain/ResourcePermissionContractTest.java#L20) · `@Test` · `resource_type_code를_역변환한다` — 모든 ResourceType code를 손실 없이 역변환하고 지원 권한 집합을 불변으로 노출한다
2. [L38](../../../src/test/java/com/umc/product/authorization/domain/ResourcePermissionContractTest.java#L38) · `@Test` · `지원하지_않는_권한을_거부한다` — 지원하지 않는 권한은 resource type 전용 오류로 거부한다
3. [L51](../../../src/test/java/com/umc/product/authorization/domain/ResourcePermissionContractTest.java#L51) · `@Test` · `resource_id를_변환한다` — resource ID의 null·Long·문자열 변환과 잘못된 숫자 형식을 구분한다
4. [L67](../../../src/test/java/com/umc/product/authorization/domain/ResourcePermissionContractTest.java#L67) · `@Test` · `필수_값_null을_거부한다` — resource type과 permission null은 생성 시점에 거부한다
5. [L78](../../../src/test/java/com/umc/product/authorization/domain/ResourcePermissionContractTest.java#L78) · `@Test` · `challenger_role_평가기_정책을_검증한다` — ChallengerRole evaluator는 READ는 공개하고 쓰기는 중앙 운영진에게만 허용한다

### Persistence

#### ChallengerRoleAdapterTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/adapter/out/persistence/ChallengerRoleAdapterTest.java`

1. [L42](../../../src/test/java/com/umc/product/authorization/adapter/out/persistence/ChallengerRoleAdapterTest.java#L42) · `@Test` · `query_repository에_조회를_위임한다` — member와 gisu 조회를 QueryDSL repository에 위임한다
2. [L53](../../../src/test/java/com/umc/product/authorization/adapter/out/persistence/ChallengerRoleAdapterTest.java#L53) · `@Test` · `jpa_repository에_조회를_위임한다` — ID·challenger ID 집합 조회를 JPA repository에 위임한다
3. [L65](../../../src/test/java/com/umc/product/authorization/adapter/out/persistence/ChallengerRoleAdapterTest.java#L65) · `@Test` · `필수_get의_not_found를_구분한다` — 필수 get에서 ID가 없으면 CHALLENGER_ROLE_NOT_FOUND를 던진다
4. [L76](../../../src/test/java/com/umc/product/authorization/adapter/out/persistence/ChallengerRoleAdapterTest.java#L76) · `@Test` · `저장과_삭제를_위임한다` — 단건·대량 저장과 삭제를 JPA repository에 위임한다

#### ChallengerRoleQueryRepositoryTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/adapter/out/persistence/ChallengerRoleQueryRepositoryTest.java`

1. [L37](../../../src/test/java/com/umc/product/authorization/adapter/out/persistence/ChallengerRoleQueryRepositoryTest.java#L37) · `@Test` · `find_by_member_id` — 회원 ID로 join된 역할 목록을 조회한다
2. [L47](../../../src/test/java/com/umc/product/authorization/adapter/out/persistence/ChallengerRoleQueryRepositoryTest.java#L47) · `@Test` · `find_by_member_id_and_gisu_id` — 회원 ID와 기수 ID로 join된 역할 목록을 조회한다

### REST / Web

#### ChallengerRoleControllerUnitTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/authorization/adapter/in/web/ChallengerRoleControllerUnitTest.java`

1. [L53](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ChallengerRoleControllerUnitTest.java#L53) · `@Test` · `역할을_생성한다` — 생성 요청의 모든 필드를 command로 전달하고 생성 ID를 응답한다
2. [L81](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ChallengerRoleControllerUnitTest.java#L81) · `@Test` · `역할과_기수를_조회한다` — 조회 응답은 역할과 기수 generation을 함께 노출한다
3. [L108](../../../src/test/java/com/umc/product/authorization/adapter/in/web/ChallengerRoleControllerUnitTest.java#L108) · `@Test` · `역할을_삭제한다` — 삭제 ID를 command로 변환해 관리 UseCase에 전달한다

## 3.27 `organization` — 120개

### Application DTO / Port

#### OrganizationCommandServiceResidualTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/application/port/service/command/OrganizationCommandServiceResidualTest.java`

1. [L57](../../../src/test/java/com/umc/product/organization/application/port/service/command/OrganizationCommandServiceResidualTest.java#L57) · `@Test` · `기수_삭제와_활성화의_경계를_처리한다` — 소속 지부가 있는 기수는 삭제할 수 없고 이미 활성인 기수는 갱신하지 않는다
2. [L74](../../../src/test/java/com/umc/product/organization/application/port/service/command/OrganizationCommandServiceResidualTest.java#L74) · `@Test` · `학교_삭제의_빈_입력을_처리한다` — 학교 일괄 삭제는 빈 입력을 무시하고 null ID만 있으면 회원 조회를 생략한다

#### OrganizationCoreServiceResidualTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/application/port/service/OrganizationCoreServiceResidualTest.java`

1. [L70](../../../src/test/java/com/umc/product/organization/application/port/service/OrganizationCoreServiceResidualTest.java#L70) · `@Test` · `지부를_여러_표면으로_조회한다` — 전체·기수별·학교별 지부를 조회하고 없는 기수-학교 조합은 거부한다
2. [L94](../../../src/test/java/com/umc/product/organization/application/port/service/OrganizationCoreServiceResidualTest.java#L94) · `@Test` · `다른_기수의_학교_소속을_건너뛴다` — 기수-학교 지부 조회는 다른 기수 소속을 건너뛴다
3. [L107](../../../src/test/java/com/umc/product/organization/application/port/service/OrganizationCoreServiceResidualTest.java#L107) · `@Test` · `기수별_지부와_학교를_조립한다` — 기수별 지부·학교 조회는 빈 입력과 학교 없는 지부 및 이중 맵을 처리한다
4. [L151](../../../src/test/java/com/umc/product/organization/application/port/service/OrganizationCoreServiceResidualTest.java#L151) · `@Test` · `학교의_개별_조회_표면을_조립한다` — 학교 이름·상세·링크·미배정 목록을 조회한다
5. [L178](../../../src/test/java/com/umc/product/organization/application/port/service/OrganizationCoreServiceResidualTest.java#L178) · `@Test` · `학교_목록의_빈값과_로고_없음을_처리한다` — 학교 목록 조회는 빈 결과와 로고 없는 학교를 처리한다
6. [L194](../../../src/test/java/com/umc/product/organization/application/port/service/OrganizationCoreServiceResidualTest.java#L194) · `@Test` · `다중_기수_학교를_그룹화한다` — 다중 기수 학교 목록은 중복 로고를 한 번만 조회하고 기수별로 그룹화한다
7. [L243](../../../src/test/java/com/umc/product/organization/application/port/service/OrganizationCoreServiceResidualTest.java#L243) · `@Test` · `스터디_그룹_수명주기를_수행한다` — 스터디 그룹의 생성·수정·멤버·멘토·삭제 수명주기를 수행한다
8. [L272](../../../src/test/java/com/umc/product/organization/application/port/service/OrganizationCoreServiceResidualTest.java#L272) · `@Test` · `멤버_충돌의_경계값을_처리한다` — 멤버가 없으면 충돌 조회를 생략하고 동일 기수·파트 중복 멤버는 거부한다

#### SchoolQueryServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/organization/application/port/service/query/SchoolQueryServiceTest.java`

1. [L119](../../../src/test/java/com/umc/product/organization/application/port/service/query/SchoolQueryServiceTest.java#L119) · `@Test` · `로고가_없는_학교_목록은_파일_조회를_생략한다` — 학교가 있지만 로고 ID가 모두 null이면 파일 링크 조회를 생략한다

#### StudyGroupQueryServiceResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/application/port/service/query/StudyGroupQueryServiceResidualTest.java`

1. [L59](../../../src/test/java/com/umc/product/organization/application/port/service/query/StudyGroupQueryServiceResidualTest.java#L59) · `@Test` · `내_스터디_목록을_조립한다` — 내 스터디 목록은 헤더·멤버·멘토를 batch 조회하고 존재하는 회원만 조립한다
2. [L86](../../../src/test/java/com/umc/product/organization/application/port/service/query/StudyGroupQueryServiceResidualTest.java#L86) · `@Test` · `스터디_이름을_조회한다` — 스터디 이름 목록은 권한 없음과 파트장 권한을 구분한다
3. [L103](../../../src/test/java/com/umc/product/organization/application/port/service/query/StudyGroupQueryServiceResidualTest.java#L103) · `@Test` · `조회_가능한_그룹_ID를_반환한다` — 조회 가능한 그룹 ID는 빈 scope를 차단하고 유효 scope만 위임한다
4. [L114](../../../src/test/java/com/umc/product/organization/application/port/service/query/StudyGroupQueryServiceResidualTest.java#L114) · `@Test` · `기수와_파트로_스터디_ID를_조회한다` — 기수와 파트 조건에 맞는 스터디 ID 목록을 저장소에 위임한다
5. [L123](../../../src/test/java/com/umc/product/organization/application/port/service/query/StudyGroupQueryServiceResidualTest.java#L123) · `@Test` · `멤버가_없는_스터디를_조회한다` — 멤버가 없는 스터디는 batch 회원 조회를 생략하고 빈 목록을 반환한다
6. [L133](../../../src/test/java/com/umc/product/organization/application/port/service/query/StudyGroupQueryServiceResidualTest.java#L133) · `@Test` · `빈_단건_상세를_조립한다` — 단건 상세에서 멤버와 멘토가 없으면 빈 batch 결과로 조립한다

### Application Service

#### OrganizationPermissionEvaluatorResidualTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/application/service/evaluator/OrganizationPermissionEvaluatorResidualTest.java`

1. [L30](../../../src/test/java/com/umc/product/organization/application/service/evaluator/OrganizationPermissionEvaluatorResidualTest.java#L30) · `@Test` · `지원_리소스와_권한을_판정한다` — 각 evaluator는 담당 리소스 타입과 지원 권한을 정확히 판정한다
2. [L59](../../../src/test/java/com/umc/product/organization/application/service/evaluator/OrganizationPermissionEvaluatorResidualTest.java#L59) · `@Test` · `미구현_권한은_거부한다` — 구현하지 않은 권한 타입은 모든 evaluator에서 fail-closed 처리한다

#### OrganizationRemainingServiceTest (7개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/application/service/OrganizationRemainingServiceTest.java`

1. [L87](../../../src/test/java/com/umc/product/organization/application/service/OrganizationRemainingServiceTest.java#L87) · `@Test` · `모든_이력_타입을_비교한다` — 모든 활동 이력 타입의 시작일과 ID를 추출해 최신순으로 비교한다
2. [L133](../../../src/test/java/com/umc/product/organization/application/service/OrganizationRemainingServiceTest.java#L133) · `@Test` · `챕터_수명주기와_예외를_검증한다` — 생성·수정·삭제 수명주기와 중복·소속·권한 예외를 검증한다
3. [L170](../../../src/test/java/com/umc/product/organization/application/service/OrganizationRemainingServiceTest.java#L170) · `@Test` · `기수_조회_경계를_처리한다` — ID·세대 batch·활성·날짜 조회의 빈값과 not-found를 처리한다
4. [L209](../../../src/test/java/com/umc/product/organization/application/service/OrganizationRemainingServiceTest.java#L209) · `@Test` · `학교_역할별_범위를_구분한다` — 학교 운영진이 아니면 거부하고 회장단과 파트장의 조회 범위를 구분한다
5. [L243](../../../src/test/java/com/umc/product/organization/application/service/OrganizationRemainingServiceTest.java#L243) · `@Test` · `일정_생성과_예외를_검증한다` — 스터디·출석 정책·커리큘럼을 검증한 뒤 일정을 저장한다
6. [L292](../../../src/test/java/com/umc/product/organization/application/service/OrganizationRemainingServiceTest.java#L292) · `@Test` · `챕터와_스쿼드_목록을_변환한다` — 챕터·스쿼드 목록을 조회 모델로 변환한다
7. [L302](../../../src/test/java/com/umc/product/organization/application/service/OrganizationRemainingServiceTest.java#L302) · `@Test` · `접근_정책의_null과_타인_요청을_평가한다` — 접근 정책은 null 요청자와 타인 프로필 요청을 관리 권한으로 평가한다

#### UmcProductMemberCommandServiceEdgeCaseTest (28개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java`

1. [L110](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L110) · `@Test` · `멤버와_초기_활동_기간을_생성한다` — 멤버 생성 시 정렬되지 않은 비인접 활동 기간을 모두 저장한다
2. [L133](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L133) · `@Test` · `중복_멤버_생성을_거부한다` — 이미 UMC PRODUCT에 등록된 회원은 중복 생성할 수 없다
3. [L142](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L142) · `@Test` · `초기_활동_기간을_필수로_검증한다` — 초기 활동 기간은 null이거나 빈 목록일 수 없다
4. [L151](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L151) · `@Test` · `초기_활동_기간의_날짜를_검증한다` — 초기 활동 기간의 시작일은 필수이고 종료일은 시작일보다 빠를 수 없다
5. [L160](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L160) · `@Test` · `무기한_기간_뒤의_기간을_거부한다` — 무기한 활동 기간 뒤에는 다른 초기 활동 기간을 둘 수 없다
6. [L169](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L169) · `@Test` · `최대_종료일을_안전하게_검증한다` — LocalDate 최대 종료일은 overflow 없이 중복 기간으로 처리한다
7. [L178](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L178) · `@Test` · `프로필_이미지_누락을_거부한다` — 존재하지 않는 프로필 이미지로 멤버를 생성할 수 없다
8. [L188](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L188) · `@Test` · `프로필_수정_권한을_검증한다` — 다른 회원의 프로필 수정 권한이 없으면 변경을 거부한다
9. [L200](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L200) · `@Test` · `프로필_수정_이미지를_검증한다` — 존재하지 않는 프로필 이미지로 프로필을 수정할 수 없다
10. [L212](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L212) · `@Test` · `멤버와_하위_이력을_삭제한다` — 멤버 삭제는 모든 하위 이력을 먼저 삭제한다
11. [L227](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L227) · `@Test` · `활동_기간을_생성한다` — 겹치지 않는 활동 기간을 추가하고 생성 ID를 반환한다
12. [L240](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L240) · `@Test` · `다른_멤버의_활동_기간_수정을_거부한다` — 다른 멤버 소유의 활동 기간은 수정할 수 없다
13. [L250](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L250) · `@Test` · `활동_기간을_수정한다` — 하위 활동이 모두 새 범위에 포함되면 활동 기간을 수정한다
14. [L268](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L268) · `@Test` · `범위_밖_리더십을_검증한다` — 리더십이 새 활동 기간 범위를 벗어나면 축소를 거부한다
15. [L281](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L281) · `@Test` · `범위_밖_스쿼드_참여를_검증한다` — 스쿼드 참여가 새 활동 기간 범위를 벗어나면 축소를 거부한다
16. [L296](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L296) · `@Test` · `활동_기간을_삭제한다` — 연관 이력이 없는 활동 기간은 삭제한다
17. [L308](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L308) · `@Test` · `연관_활동_기간_삭제를_거부한다` — 챕터·리더십·스쿼드 연관 중 하나라도 있으면 활동 기간을 삭제할 수 없다
18. [L322](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L322) · `@Test` · `챕터_소속을_생성한다` — 활동 기간 안에 겹치지 않는 챕터 소속을 생성한다
19. [L336](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L336) · `@Test` · `챕터_소속의_범위를_검증한다` — 챕터 소속 기간을 포함하는 활동 기간이 없으면 생성을 거부한다
20. [L346](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L346) · `@Test` · `중복_챕터_소속을_거부한다` — 같은 챕터의 기간이 겹치면 소속 생성을 거부한다
21. [L358](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L358) · `@Test` · `챕터_소속을_수정한다` — 본인 소속을 새 챕터와 활동 기간으로 수정한다
22. [L374](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L374) · `@Test` · `다른_멤버의_챕터_소속을_거부한다` — 다른 멤버의 챕터 소속은 수정하거나 삭제할 수 없다
23. [L388](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L388) · `@Test` · `챕터_소속을_삭제한다` — 본인 챕터 소속을 삭제한다
24. [L400](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L400) · `@Test` · `리더십을_생성한다` — 활동 기간 안에 겹치지 않는 리더십을 생성한다
25. [L411](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L411) · `@Test` · `중복_리더십을_거부한다` — 동일 역할 또는 동일 멤버의 리더십 기간 중복을 거부한다
26. [L423](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L423) · `@Test` · `리더십을_수정한다` — 본인 리더십을 수정하고 기존 ID를 중복 검사에서 제외한다
27. [L439](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L439) · `@Test` · `다른_멤버의_리더십을_거부한다` — 다른 멤버의 리더십은 수정하거나 삭제할 수 없다
28. [L452](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberCommandServiceEdgeCaseTest.java#L452) · `@Test` · `리더십을_삭제한다` — 본인 리더십을 삭제한다

#### UmcProductMemberQueryServiceTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/application/service/UmcProductMemberQueryServiceTest.java`

1. [L88](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberQueryServiceTest.java#L88) · `@Test` · `전체_정보를_결합한다` — 멤버 정보와 모든 활동 이력·스쿼드·프로필 링크를 결합한다
2. [L117](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberQueryServiceTest.java#L117) · `@Test` · `누락_연결_정보를_null로_처리한다` — 원본 회원과 PRODUCT 프로필이 누락되어도 null 필드로 graceful하게 조회한다
3. [L143](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberQueryServiceTest.java#L143) · `@Test` · `빈_id_page를_즉시_반환한다` — 빈 ID page는 후속 배치 조회 없이 빈 page를 반환한다
4. [L156](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberQueryServiceTest.java#L156) · `@Test` · `id_page_순서와_누락_entity를_처리한다` — ID page 순서를 유지하고 없어진 entity ID는 건너뛴다
5. [L181](../../../src/test/java/com/umc/product/organization/application/service/UmcProductMemberQueryServiceTest.java#L181) · `@Test` · `active_on으로_필터링하고_정렬한다` — activeOn으로 이력을 필터링하고 최근 시작일·ID 역순으로 정렬한다

#### UmcProductSquadCommandServiceEdgeCaseTest (19개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java`

1. [L78](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L78) · `@Test` · `스쿼드를_생성한다` — 중복되지 않은 코드로 스쿼드를 생성하고 ID를 반환한다
2. [L89](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L89) · `@Test` · `코드_없는_스쿼드를_거부한다` — 코드가 null이면 중복 조회는 생략하지만 domain 생성 규칙에서 거부한다
3. [L97](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L97) · `@Test` · `스쿼드_관리_권한을_검증한다` — 관리 권한이 없으면 스쿼드를 생성할 수 없다
4. [L107](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L107) · `@Test` · `기존_코드와_시작일을_유지하며_수정한다` — 수정 값에 코드와 시작일이 없으면 기존 값을 유지한다
5. [L122](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L122) · `@Test` · `코드와_시작일을_수정한다` — 수정 코드가 있으면 현재 스쿼드 ID를 제외하고 중복을 검사한다
6. [L138](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L138) · `@Test` · `잘못된_스쿼드_기간을_거부한다` — 종료일이 시작일보다 빠른 스쿼드 수정은 거부한다
7. [L148](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L148) · `@Test` · `무기한_스쿼드로_수정한다` — 종료일이 없는 스쿼드 기간은 종료일 없는 참여 이력을 포함한다
8. [L164](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L164) · `@Test` · `종료일_없는_참여_이력을_거부한다` — 종료일이 있는 스쿼드는 종료일 없는 참여 이력을 포함할 수 없다
9. [L178](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L178) · `@Test` · `스쿼드를_삭제한다` — 참여 이력이 없는 스쿼드는 삭제한다
10. [L189](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L189) · `@Test` · `일반_참여_이력을_생성한다` — 활동 기간 안에 겹치지 않는 일반 참여 이력을 생성한다
11. [L202](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L202) · `@Test` · `스쿼드_리더를_생성한다` — 다른 멤버 참여와 겹치지 않는 스쿼드 리더를 생성한다
12. [L214](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L214) · `@Test` · `참여_기간을_검증한다` — 시작일 누락과 역전된 참여 기간을 거부한다
13. [L225](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L225) · `@Test` · `참여_활동_기간의_범위를_검증한다` — 참여 기간을 포함하는 멤버 활동 기간이 없으면 거부한다
14. [L237](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L237) · `@Test` · `멤버_참여_중복을_거부한다` — 동일 스쿼드의 같은 멤버 참여 기간 중복을 거부한다
15. [L249](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L249) · `@Test` · `스쿼드_리더_중복을_거부한다` — 같은 기간에 둘 이상의 스쿼드 리더를 둘 수 없다
16. [L261](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L261) · `@Test` · `참여_이력을_수정한다` — 기존 참여 이력은 본인 ID를 제외한 뒤 수정한다
17. [L277](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L277) · `@Test` · `다른_스쿼드_참여_수정을_거부한다` — 다른 스쿼드의 참여 이력은 수정할 수 없다
18. [L289](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L289) · `@Test` · `참여_이력을_삭제한다` — 소속 스쿼드의 참여 이력을 삭제한다
19. [L302](../../../src/test/java/com/umc/product/organization/application/service/UmcProductSquadCommandServiceEdgeCaseTest.java#L302) · `@Test` · `다른_스쿼드_참여_삭제를_거부한다` — 다른 스쿼드의 참여 이력은 삭제할 수 없다

### Contract / Misc

#### OrganizationDtoResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/adapter/in/OrganizationDtoResidualTest.java`

1. [L72](../../../src/test/java/com/umc/product/organization/adapter/in/OrganizationDtoResidualTest.java#L72) · `@Test` · `요청을_명령과_검색_조건으로_변환한다` — 스터디·기수·학교 요청은 경로 식별자와 본문을 명령·검색 조건으로 보존한다
2. [L94](../../../src/test/java/com/umc/product/organization/adapter/in/OrganizationDtoResidualTest.java#L94) · `@Test` · `멤버_교체_명령의_불변성과_필수값을_검증한다` — 스터디 멤버 교체 명령은 입력을 불변 복사하고 필수 멤버를 검증한다
3. [L109](../../../src/test/java/com/umc/product/organization/adapter/in/OrganizationDtoResidualTest.java#L109) · `@Test` · `스터디_응답을_변환한다` — 스터디 조회 응답은 멘토와 멤버 프로필을 손실 없이 변환한다
4. [L139](../../../src/test/java/com/umc/product/organization/adapter/in/OrganizationDtoResidualTest.java#L139) · `@Test` · `지부와_학교_응답을_변환한다` — 지부·학교 목록 응답은 중첩 학교와 페이지 메타데이터를 변환한다
5. [L173](../../../src/test/java/com/umc/product/organization/adapter/in/OrganizationDtoResidualTest.java#L173) · `@Test` · `기수와_GraphQL_응답을_변환한다` — 기수 목록과 GraphQL 학교 상세는 null 링크·시간 및 중첩 링크를 안전하게 변환한다
6. [L201](../../../src/test/java/com/umc/product/organization/adapter/in/OrganizationDtoResidualTest.java#L201) · `@Test` · `UMC_PRODUCT_응답을_변환한다` — UMC PRODUCT 소속·리더십·스쿼드 응답은 중첩 조회 모델을 변환한다

### Domain

#### OrganizationDomainResidualTest (4개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/domain/OrganizationDomainResidualTest.java`

1. [L35](../../../src/test/java/com/umc/product/organization/domain/OrganizationDomainResidualTest.java#L35) · `@Test` · `조직_구조의_필수값을_검증한다` — 지부·학교 소속·스터디 구성원은 모든 필수 연관과 식별자를 검증한다
2. [L65](../../../src/test/java/com/umc/product/organization/domain/OrganizationDomainResidualTest.java#L65) · `@Test` · `기간_경계를_검증한다` — 기수와 프로덕트 기간은 null·역전·비인접 날짜 경계를 안전하게 처리한다
3. [L83](../../../src/test/java/com/umc/product/organization/domain/OrganizationDomainResidualTest.java#L83) · `@Test` · `프로덕트_조직의_상태를_변경한다` — 프로덕트 조직의 상태 변경과 선택 입력을 실제 엔티티에 반영한다
4. [L102](../../../src/test/java/com/umc/product/organization/domain/OrganizationDomainResidualTest.java#L102) · `@Test` · `프로덕트_소속의_필수값을_검증한다` — 프로덕트 소속 엔티티는 누락된 상위 활동·역할·직책을 각각 거부한다

### GraphQL

#### OrganizationGraphQlControllerResidualTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/adapter/in/graphql/OrganizationGraphQlControllerResidualTest.java`

1. [L26](../../../src/test/java/com/umc/product/organization/adapter/in/graphql/OrganizationGraphQlControllerResidualTest.java#L26) · `@Test` · `중복_batch_source를_병합한다` — 중복 batch source와 중복 지부 조회 모델은 최초 결과를 안정적으로 유지한다

### Persistence

#### OrganizationPersistenceAdapterResidualTest (9개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java`

1. [L66](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java#L66) · `@Test` · `단건_조회_계약을_보장한다` — 단건 조회는 Optional 계약과 not-found 예외를 보장한다
2. [L82](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java#L82) · `@Test` · `batch_조회의_경계값을_처리한다` — scope·ID batch 조회는 null과 빈 입력을 차단하고 유효 입력만 위임한다
3. [L117](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java#L117) · `@Test` · `저장하고_삭제한다` — 저장과 삭제를 JPA 저장소에 위임한다
4. [L147](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java#L147) · `@Test` · `학교_기본_영속성_계약을_보장한다` — 학교 저장·삭제·단건·일괄·존재 검사를 위임하고 not-found를 변환한다
5. [L175](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java#L175) · `@Test` · `학교_조회_모델을_위임한다` — 학교 조회 모델은 QueryDSL 저장소에 위임하고 없는 상세를 변환한다
6. [L211](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java#L211) · `@Test` · `지부_adapter_계약을_보장한다` — 지부 adapter는 존재·조회·목록·저장·삭제 계약을 보장한다
7. [L235](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java#L235) · `@Test` · `지부_학교_adapter_계약을_보장한다` — 지부-학교 adapter는 모든 조회·저장·삭제를 위임하고 not-found를 변환한다
8. [L261](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java#L261) · `@Test` · `기수_adapter_계약을_보장한다` — 기수 adapter는 활성·단건·목록·날짜·저장·삭제 계약과 not-found를 보장한다
9. [L306](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationPersistenceAdapterResidualTest.java#L306) · `@Test` · `일정_adapter_계약을_보장한다` — 일정을 저장하고 그룹 ID 입력의 빈값을 차단하며 ID를 중복 제거한다

#### OrganizationQueryRepositoryIntegrationTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationQueryRepositoryIntegrationTest.java`

1. [L101](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationQueryRepositoryIntegrationTest.java#L101) · `@Test` · `학교_목록을_검색한다` — 학교 목록은 키워드·활성 지부 필터·페이징과 전체 개수를 함께 계산한다
2. [L123](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationQueryRepositoryIntegrationTest.java#L123) · `@Test` · `날짜로_기수를_조회한다` — 기수 날짜 조회는 시작일과 종료일을 포함하는 기수를 반환한다
3. [L130](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationQueryRepositoryIntegrationTest.java#L130) · `@Test` · `학교_상세를_여러_조건으로_조회한다` — 학교 상세·기수별·ID 일괄 조회는 활성 기수 여부와 소속 정보를 보존한다
4. [L150](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationQueryRepositoryIntegrationTest.java#L150) · `@Test` · `학교와_링크를_조회한다` — 학교 엔티티·이름·링크 조회는 fetch join과 링크 그룹화를 수행한다
5. [L172](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationQueryRepositoryIntegrationTest.java#L172) · `@Test` · `지부_학교_소속을_조회한다` — 지부-학교 저장소는 단건·학교·기수 조합을 모두 조회하고 연관을 fetch join한다
6. [L196](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/OrganizationQueryRepositoryIntegrationTest.java#L196) · `@Test` · `프로덕트_멤버를_모든_조건으로_검색한다` — 프로덕트 멤버 검색은 null 조건과 활동일·챕터·직책·리더십·스쿼드 조건을 조합한다

#### StudyGroupQueryRepositoryTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java`

1. [L368](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L368) · `@Test` · `findStudyGroupNames_scope에_해당하는_이름만_정렬해_반환하고_null_scope는_빈_목록` — findStudyGroupNames_scope에_해당하는_이름만_정렬해_반환하고_null_scope는_빈_목록
2. [L388](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/studygroup/StudyGroupQueryRepositoryTest.java#L388) · `@Test` · `findConflictedMemberIds는_동일_기수와_파트를_찾고_수정_대상은_제외한다` — findConflictedMemberIds는_동일_기수와_파트를_찾고_수정_대상은_제외한다

#### UmcProductPersistenceAdapterResidualTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterResidualTest.java`

1. [L50](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterResidualTest.java#L50) · `@Test` · `멤버_adapter_계약을_보장한다` — ID와 회원 ID 조회·잠금·목록·검색·삭제 및 not-found를 처리한다
2. [L93](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterResidualTest.java#L93) · `@Test` · `챕터_adapter_계약을_보장한다` — 챕터의 단건·잠금·목록·존재·삭제와 not-found를 처리한다
3. [L116](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterResidualTest.java#L116) · `@Test` · `챕터_소속_adapter_계약을_보장한다` — 챕터 소속의 단건·일괄·중복·삭제와 not-found를 처리한다
4. [L148](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterResidualTest.java#L148) · `@Test` · `활동_기간_adapter_계약을_보장한다` — 활동 기간의 단건·일괄·포함·중복·삭제와 not-found를 처리한다
5. [L169](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterResidualTest.java#L169) · `@Test` · `리더십_adapter_계약을_보장한다` — 리더십의 단건·일괄·역할·중복·삭제와 입력 경계를 처리한다
6. [L208](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterResidualTest.java#L208) · `@Test` · `스쿼드_adapter_계약을_보장한다` — 스쿼드의 단건·잠금·목록·존재·삭제와 not-found를 처리한다
7. [L230](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterResidualTest.java#L230) · `@Test` · `스쿼드_참여_adapter_계약을_보장한다` — 스쿼드 참여의 단건·일괄·중복·삭제와 not-found를 처리한다
8. [L263](../../../src/test/java/com/umc/product/organization/adapter/out/persistence/umcproduct/UmcProductPersistenceAdapterResidualTest.java#L263) · `@Test` · `제약조건을_도메인_예외로_변환한다` — 제약조건 이름·메시지·미등록 원인을 각각 변환한다

### REST / Web

#### OrganizationControllerResidualTest (6개, 신규 파일)

- 위치: `src/test/java/com/umc/product/organization/adapter/in/web/OrganizationControllerResidualTest.java`

1. [L73](../../../src/test/java/com/umc/product/organization/adapter/in/web/OrganizationControllerResidualTest.java#L73) · `@Test` · `스터디_그룹_명령을_전달한다` — 스터디 그룹 command controller는 생성부터 삭제까지 모든 명령을 변환한다
2. [L97](../../../src/test/java/com/umc/product/organization/adapter/in/web/OrganizationControllerResidualTest.java#L97) · `@Test` · `스터디_그룹을_조회한다` — 스터디 그룹 query controller는 커서 초과분과 단건 상세를 응답으로 변환한다
3. [L114](../../../src/test/java/com/umc/product/organization/adapter/in/web/OrganizationControllerResidualTest.java#L114) · `@Test` · `지부를_명령하고_조회한다` — 지부 controller는 bulk 생성·삭제·단건·학교 포함 조회를 수행한다
4. [L132](../../../src/test/java/com/umc/product/organization/adapter/in/web/OrganizationControllerResidualTest.java#L132) · `@Test` · `학교를_명령하고_조회한다` — 학교 controller는 지부 배정·해제 및 기수별·미배정 조회를 수행한다
5. [L148](../../../src/test/java/com/umc/product/organization/adapter/in/web/OrganizationControllerResidualTest.java#L148) · `@Test` · `일정과_기수를_조회한다` — 스터디 일정과 기수 단건 controller는 use case 결과를 변환한다
6. [L162](../../../src/test/java/com/umc/product/organization/adapter/in/web/OrganizationControllerResidualTest.java#L162) · `@Test` · `UMC_PRODUCT_인증_주체를_검증한다` — UMC PRODUCT command controller는 인증 주체가 없으면 요청을 거부한다

## 3.28 Production test seed — 34개

### Application Service

#### ChallengerRoleSeedServiceTest (1개, 신규 파일)

- 위치: `src/test/java/com/umc/product/test/application/service/ChallengerRoleSeedServiceTest.java`

1. [L24](../../../src/test/java/com/umc/product/test/application/service/ChallengerRoleSeedServiceTest.java#L24) · `@Test` · `역할_생성_위임` — seed 역할 생성은 scope와 담당 파트를 authorization Command에 그대로 전달한다

#### ChallengerSeedServiceTest (3개, 보강 파일)

- 위치: `src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java`

1. [L190](../../../src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java#L190) · `@Test` · `셀_수량_0_처리` — 셀 수량이 0이면 멤버와 챌린저 생성을 모두 생략한다
2. [L209](../../../src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java#L209) · `@Test` · `빈_멤버_batch_결과` — 멤버 batch가 빈 목록을 반환하면 전원 실패로 집계하고 챌린저 생성을 생략한다
3. [L231](../../../src/test/java/com/umc/product/test/application/service/ChallengerSeedServiceTest.java#L231) · `@Test` · `단건_챌린저_생성` — 단건 챌린저 생성은 member·gisu·part를 그대로 위임한다

#### CurriculumSeedServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java`

1. [L273](../../../src/test/java/com/umc/product/test/application/service/CurriculumSeedServiceTest.java#L273) · `@Test` · `하위_단계별_실패_격리` — 주차·워크북 bulk와 미션 실패는 단계별 카운터로 격리된다

#### MemberSeedServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/test/application/service/MemberSeedServiceTest.java`

1. [L157](../../../src/test/java/com/umc/product/test/application/service/MemberSeedServiceTest.java#L157) · `@Test` · `단건_멤버_비밀번호_기본값` — 단건 멤버 생성은 공백 비밀번호를 기본값으로 치환하고 명시값은 보존한다

#### NoticeSeedServiceTest (1개, 보강 파일)

- 위치: `src/test/java/com/umc/product/test/application/service/NoticeSeedServiceTest.java`

1. [L201](../../../src/test/java/com/umc/product/test/application/service/NoticeSeedServiceTest.java#L201) · `@Test` · `모든_scope_실패_집계` — 모든 scope의 생성 실패를 각각 집계하고 ADMIN 파트는 제외한다

#### ProjectApplicationSeedServiceTest (8개, 신규 파일)

- 위치: `src/test/java/com/umc/product/test/application/service/ProjectApplicationSeedServiceTest.java`

1. [L80](../../../src/test/java/com/umc/product/test/application/service/ProjectApplicationSeedServiceTest.java#L80) · `@Test` · `매칭_차수_소속_검증` — 매칭 차수가 지부에 속하지 않으면 시딩을 거절한다
2. [L90](../../../src/test/java/com/umc/product/test/application/service/ProjectApplicationSeedServiceTest.java#L90) · `@Test` · `매칭_차수_OPEN_기간_검증` — 시작 전이거나 종료된 매칭 차수는 시딩을 거절한다
3. [L109](../../../src/test/java/com/umc/product/test/application/service/ProjectApplicationSeedServiceTest.java#L109) · `@Test` · `대상_챌린저가_없으면_스킵` — 매칭 타입에 맞는 ACTIVE 챌린저가 없으면 빈 결과를 반환한다
4. [L126](../../../src/test/java/com/umc/product/test/application/service/ProjectApplicationSeedServiceTest.java#L126) · `@Test` · `대상_프로젝트가_없으면_스킵` — 지원 가능한 프로젝트가 없으면 빈 결과를 반환한다
5. [L138](../../../src/test/java/com/umc/product/test/application/service/ProjectApplicationSeedServiceTest.java#L138) · `@Test` · `단계별_성공과_실패를_격리한다` — 지원 단계별 성공과 실패를 격리하고 결과 상태별 집계를 반환한다
6. [L211](../../../src/test/java/com/umc/product/test/application/service/ProjectApplicationSeedServiceTest.java#L211) · `@Test` · `지원_폼이_없으면_답변_갱신을_생략한다` — 지원 폼이 없으면 답변 갱신을 생략하고 제출한다
7. [L230](../../../src/test/java/com/umc/product/test/application/service/ProjectApplicationSeedServiceTest.java#L230) · `@Test` · `프로젝트_PO_지원_제외` — 프로젝트 PO 본인은 지원 후보에서 제외한다
8. [L244](../../../src/test/java/com/umc/product/test/application/service/ProjectApplicationSeedServiceTest.java#L244) · `@Test` · `무작위_결과_범위` — 무작위 지원 결과 인덱스는 세 상태 범위 안에서 생성된다

#### ProjectScenarioSeedServiceTest (5개, 보강 파일)

- 위치: `src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java`

1. [L346](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L346) · `@Test` · `멤버_필터와_추가_실패_격리` — 비활성·이미 선택된 멤버를 제외하고 개별 추가 실패를 해당 파트에 격리한다
2. [L381](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L381) · `@Test` · `무작위_충원_범위` — 무작위 충원 수는 0부터 quota까지의 범위다
3. [L480](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L480) · `@Test` · `update_failure_recorded` — 프로젝트 정보 갱신 실패는 UPDATE 단계로 기록된다
4. [L497](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L497) · `@Test` · `form_failure_recorded` — 지원 폼 생성 실패는 FORM 단계로 기록된다
5. [L515](../../../src/test/java/com/umc/product/test/application/service/ProjectScenarioSeedServiceTest.java#L515) · `@Test` · `quota_failure_recorded` — 파트 quota 갱신 실패는 QUOTA 단계로 기록된다

#### ProjectSeedServiceTest (2개, 보강 파일)

- 위치: `src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java`

1. [L261](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java#L261) · `@Test` · `PO_후보_없음_스킵` — 모든 후보가 다른 파트 챌린저이면 PO 후보 없음으로 스킵한다
2. [L283](../../../src/test/java/com/umc/product/test/application/service/ProjectSeedServiceTest.java#L283) · `@Test` · `기존_PLAN_PO_재사용` — 기존 PLAN 챌린저를 PO로 재사용하면 챌린저를 중복 생성하지 않는다

#### SeedFactoryTest (5개, 신규 파일)

- 위치: `src/test/java/com/umc/product/test/application/service/SeedFactoryTest.java`

1. [L29](../../../src/test/java/com/umc/product/test/application/service/SeedFactoryTest.java#L29) · `@Test` · `더미_멤버_Command_생성` — 더미 멤버는 필수 약관과 지정 학교를 보존하고 식별 가능한 고유 값을 만든다
2. [L52](../../../src/test/java/com/umc/product/test/application/service/SeedFactoryTest.java#L52) · `@Test` · `더미_공지_Command_생성` — 더미 공지는 전체·지부·학교·파트 대상과 무알림 정책을 정확히 표현한다
3. [L72](../../../src/test/java/com/umc/product/test/application/service/SeedFactoryTest.java#L72) · `@Test` · `더미_커리큘럼_Command_생성` — 더미 커리큘럼은 유효한 주차·워크북·필수 미션 Command를 만든다
4. [L96](../../../src/test/java/com/umc/product/test/application/service/SeedFactoryTest.java#L96) · `@Test` · `더미_프로젝트_문자열_생성` — 프로젝트 더미 문자열은 식별 접두사와 DB 길이 제한을 지킨다
5. [L107](../../../src/test/java/com/umc/product/test/application/service/SeedFactoryTest.java#L107) · `@Test` · `시나리오_quota_정책` — 시나리오 quota는 DESIGN·FE·BE 순서와 허용 범위를 지킨다

### Contract / Misc

#### SeedDtoContractTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/test/SeedDtoContractTest.java`

1. [L76](../../../src/test/java/com/umc/product/test/SeedDtoContractTest.java#L76) · `@Test` · `all_seed_transport_records_are_constructible_and_mappable` — 모든 seed request·response·command·result record를 생성하고 변환한다
2. [L122](../../../src/test/java/com/umc/product/test/SeedDtoContractTest.java#L122) · `@Test` · `certificate_preview_defaults_blank_values` — 증서 미리보기는 공백 입력을 trim하고 안정적인 sample 기본값을 제공한다

### REST / Web

#### SeedControllerTest (3개, 신규 파일)

- 위치: `src/test/java/com/umc/product/test/adapter/in/web/SeedControllerTest.java`

1. [L96](../../../src/test/java/com/umc/product/test/adapter/in/web/SeedControllerTest.java#L96) · `@Test` · `멤버와_챌린저_API_위임` — 멤버·챌린저·역할 seed API는 변환한 Command를 위임한다
2. [L135](../../../src/test/java/com/umc/product/test/adapter/in/web/SeedControllerTest.java#L135) · `@Test` · `도메인_seed_API_위임` — 프로젝트·커리큘럼·공지 seed API는 변환한 Command를 위임한다
3. [L175](../../../src/test/java/com/umc/product/test/adapter/in/web/SeedControllerTest.java#L175) · `@Test` · `프로젝트_삭제_null_요청_처리` — 프로젝트 삭제 요청이 없으면 활성 기수를 뜻하는 null Command를 사용한다

#### TestControllerTest (2개, 신규 파일)

- 위치: `src/test/java/com/umc/product/test/adapter/in/web/TestControllerTest.java`

1. [L68](../../../src/test/java/com/umc/product/test/adapter/in/web/TestControllerTest.java#L68) · `@Test` · `파일과_알림_API_위임` — 파일·알림·이메일 테스트 API는 입력을 변환해 각 UseCase에 위임한다
2. [L106](../../../src/test/java/com/umc/product/test/adapter/in/web/TestControllerTest.java#L106) · `@Test` · `토큰과_상태_API_위임` — 개발용 토큰 API는 기본·사용자 지정 만료와 인증 정보를 보존한다

## 4. 전체 실행 결과와 함께 읽기

- 전체 Gradle 실행: 5,002건, 성공 4,962건, ignored 40건, 실패·오류 0건.
- JaCoCo: Line 38,664/38,664(100%), Class 2,532/2,532(100%), Branch 10,482/11,689(89.67%, 비강제).
- package별 coverage와 계층별 edge case 분류는 [`pr-1171-domain-test-coverage.md`](pr-1171-domain-test-coverage.md)를 참고한다.
