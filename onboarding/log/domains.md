# Domain Log Inventory

수집 기준:

- 대상: `src/main/java/com/umc/product/**`
- 패턴: `log.trace/debug/info/warn/error`, `LoggerFactory.getLogger`
- 기준 브랜치: `feature/logging-hardening`

## 요약

| 도메인 | 현재 로그 호출 | 주요 목적 |
| --- | ---: | --- |
| authentication | 72 | OAuth/ID/PW 인증 흐름, provider 호출 실패, 토큰 revoke, email verification retention |
| authorization | 6 | 권한 evaluator 등록, 권한 평가, 접근 거부 |
| audit | 6 | audit event 발행/저장 실패, details 직렬화 실패 |
| blog | 3 | 지원하지 않는 permission type |
| challenger | 6 | ChallengerRecord 생성/대량생성/검증 실패, deprecated API |
| community | 2 | 지원하지 않는 permission type |
| curriculum | 3 | Workbook 자동 배포 스케줄 |
| figma | 35 | Figma sync/digest/classification, Discord batch 전송, 외부 API 실패 |
| global | 36 | request logging, exception handling, JWT/Firebase/security/bootstrap |
| llm | 16 | LLM provider 선택, 호출 시작/완료/실패, circuit/rate limit |
| maintenance | 1 | maintenance snapshot refresh 실패 |
| notice | 2 | 공지 조회 조건, viewer 지부 정보 fallback |
| notification | 39 | FCM, webhook, SES, server lifecycle |
| project | 3 | matching round deadline 실패, project response assembly fallback |
| storage | 12 | file lifecycle, S3/CloudFront |
| test | 42 | seed/test endpoint diagnostics |

직접 로그 호출이 없는 도메인: `member`, `organization`, `schedule`, `survey`, `term`.
`schedule`은 직접 로그 대신 `@Audited`로 create/update/delete/forceDelete를 audit 대상으로 남긴다.

## authentication

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `OAuthAuthenticationService` | INFO | OAuth login start, existing/new member 분기, token/code 검증 성공. `provider`, `memberId`, `hasEmail` 사용 | provider id/email 원문 제거 완료. 로그인 성공/실패는 audit/security event 후보 |
| `OAuthAuthenticationService` | WARN | OAuth unlink 시 refresh/access token 누락, access token 소유자 불일치 | 계정 연결 해제 실패 원인 파악에 필요. actor audit 보강 필요 |
| `OAuthTokenVerificationAdapter` | INFO/WARN | provider별 verify/code exchange/revoke 시작, 미지원 provider | adapter routing 흐름 확인용. 반복 호출 INFO는 DEBUG 후보 |
| `AppleTokenVerifier` | DEBUG | ID token 검증 시작/성공, private key length | 성공 로그는 DEBUG 적절. private key length/credential 진단 로그는 제거 또는 local profile 제한 후보 |
| `AppleTokenVerifier` | INFO | authorization code exchange 성공, token revoke 시작/성공 | 외부 API structured event가 있으므로 시작/성공 INFO 축소 후보 |
| `AppleTokenVerifier` | ERROR | token endpoint/revoke/JWKS/RSA/client secret 실패. `status`, `errorCode`, `bodyLength`, `kid` | body 원문 미기록은 적절. `kid` 노출 필요성은 검토 가능 |
| `GoogleTokenVerifier` | DEBUG | ID/access token 검증 시작/성공, `hasEmail` | 적절 |
| `GoogleTokenVerifier` | INFO | token revoke 시작/성공 | external_api metric으로 대체 가능 |
| `GoogleTokenVerifier` | ERROR | tokeninfo/revoke 실패, audience 불일치, 검증 중 예외 | `expected` client id list는 설정값 노출 가능성이 있어 축약 후보 |
| `KakaoTokenVerifier` | DEBUG | authorization code/access token 검증 시작/성공 | 적절 |
| `KakaoTokenVerifier` | INFO | token exchange 성공, unlink 시작/성공, admin unlink targetProvided | external_api event와 중복. 성공 INFO 축소 후보 |
| `KakaoTokenVerifier` | WARN | 허용되지 않은 redirect URI, admin key 미설정 skip | 보안/설정 이슈라 유지 가능. redirect URI 원문은 allowlist mismatch 분석에 필요하지만 마스킹 검토 |
| `KakaoTokenVerifier` | ERROR | token exchange/userinfo/unlink 실패, 예외 | status 중심이라 적절 |
| `CredentialRehashService` | INFO/WARN | 비밀번호 해시 정책 갱신 완료/실패, `memberId` | 보안 상태 변화라 유지. audit/security event 후보 |
| `AuthenticationService` | INFO | 비밀번호 재설정 요청에서 가입/자격증명 미존재 시 발송 skip | enumeration 방어 흐름 설명용. metric으로 누적하면 좋음 |
| `EmailVerificationRetentionScheduler` | INFO/DEBUG | email verification retention 삭제 완료/없음, `threshold`, `deleted` | 스케줄러 표준 필드 `jobName`, `durationMs`, `result` 추가 후보 |

구조화 외부 API 이벤트:

- `APPLE`: `EXCHANGE_AUTHORIZATION_CODE`, `REVOKE_TOKEN`, `FETCH_JWKS`
- `GOOGLE`: `VERIFY_ID_TOKEN`, `VERIFY_ACCESS_TOKEN`, `REVOKE_TOKEN`
- `KAKAO`: `EXCHANGE_AUTHORIZATION_CODE`, `GET_USER_INFO`, `UNLINK_USER`, `ADMIN_UNLINK_USER`

추가 계획:

- 이메일 로그인 성공, OAuth 로그인 성공/회원가입 필요, OAuth 계정 연결/해제는 audit 대상으로 선언했다.
- 로그인 실패와 비밀번호 재설정 실패는 사용자 열거 방어를 유지하기 위해 security metric으로 집계한다.
- token verification/revoke 성공률과 provider latency는 `ExternalApiCallLogger`를 통해 metric으로도 기록한다.

## authorization

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `AuthorizationService` | INFO | 등록된 evaluator key set | bootstrap 시 1회라 유지 가능 |
| `AuthorizationService` | DEBUG | 권한 평가 시작, subject memberId/roleCount/challengerCount | 요청마다 발생할 수 있어 DEBUG 유지. subject 전체 `toString`은 제거 완료 |
| `AuthorizationService` | DEBUG | permission check 상세. `memberId`, roles, resource, permission, result | 디버깅용으로 적절 |
| `AuthorizationService` | WARN | permission denied | 보안 이벤트. audit/security event 또는 metric 후보 |
| `AccessControlAspect` | WARN | annotation 기반 접근 거부. `memberId`, resource, permission | 보안 이벤트. audit/security event 후보 |

추가 계획:

- 접근 거부는 WARN 로그와 `OperationalMetrics.recordSecurityEvent`로 집계한다.
- 정상 권한 평가 시작 로그는 DEBUG로 낮췄다.

## audit

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `AuditLogCommandService` | DEBUG | audit 저장 요청의 domain/action/target/actor | 적절 |
| `AuditLogCommandService` | WARN | details JSON 직렬화 실패 | audit detail 누락 가능성이라 유지 |
| `AuditLogEventListener` | ERROR | audit log 저장 실패 | 중요. 알람 후보 |
| `AuditAspect` | ERROR | audit event 발행 오류 | 중요. 알람 후보 |
| `AuditAspect` | DEBUG | SecurityContext memberId/IP 추출 실패 | fallback 진단용 |

추가 계획:

- audit 저장 실패는 metric counter와 alert를 붙인다.
- details 직렬화 실패는 event id 또는 target을 포함해 추적성을 높인다.

## blog

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `BlogSeriesPermissionEvaluator` | WARN | 지원하지 않는 permission type | 개발/설정 오류 진단용 |
| `BlogContentPermissionEvaluator` | WARN | 지원하지 않는 permission type | 개발/설정 오류 진단용 |
| `BlogCommentPermissionEvaluator` | WARN | 지원하지 않는 permission type | 개발/설정 오류 진단용 |

추가 계획:

- permission evaluator 공통 helper로 메시지와 필드를 표준화한다.

## challenger

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `ChallengerSearchController` | WARN | deprecated global search API 호출 | 제거 일정 관리용 |
| `ChallengerRecordCommandService` | INFO | ChallengerRecord 생성 완료. `recordId`, `gisuId`, `schoolId`, `chapterId`, `adminRecord` | 관리성 상태 변화라 유지. audit 추가 완료 |
| `ChallengerRecordCommandService` | INFO | ChallengerRecord 대량 생성 완료. `count` | audit 추가 완료 |
| `ChallengerRecordCommandService` | DEBUG | school/chapter mismatch 검증 실패 상세 | 요청 검증 디버깅용 |

비활성 로그:

- `ChallengerQueryController`에 deprecated API 로그 2건이 주석 처리되어 있다.

추가 계획:

- deprecated API 호출 횟수는 metric으로 분리하면 제거 판단에 유용하다.
- ChallengerRecord 생성/삭제/consumeCode는 audit 대상으로 선언했다. consumeCode audit target은 일회용 코드 원문이 아니라 target member id를 사용한다.

## community

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `CommunityPostPermissionEvaluator` | WARN | 지원하지 않는 permission type | 개발/설정 오류 진단용 |
| `CommunityCommentPermissionEvaluator` | WARN | 지원하지 않는 permission type | 개발/설정 오류 진단용 |

추가 계획:

- blog와 같은 permission evaluator 공통 로그 포맷으로 맞춘다.

## curriculum

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `WorkbookAutoReleaseScheduler` | INFO | 자동 배포 스케줄 시작 | batch 시작 추적 |
| `WorkbookAutoReleaseScheduler` | INFO | 자동 배포 완료, 배포 건수 | batch 결과 추적 |
| `WorkbookAutoReleaseScheduler` | ERROR | 자동 배포 중 오류 | 장애 원인 분석 |

추가 계획:

- `jobName`, `durationMs`, `result`, `processed` 형태로 표준화했고 batch metric을 추가했다.

## figma

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `FigmaCommentDispatchRetentionScheduler` | INFO/DEBUG | dispatch retention 삭제 완료/없음. `threshold`, `deleted` | 스케줄러 로그로 적절 |
| `FigmaCommentSyncScheduler` | DEBUG | sync polling window `from`, `to` | 디버깅용 |
| `FigmaCommentDigestService` | INFO | digest 완료. 기간, total, unmatched, skipped, domains | 운영 요약으로 유용. metric 후보 |
| `FigmaIntegrationCommandService` | INFO | Figma 통합 등록 완료. owner/integration id | audit 후보 |
| `FigmaCommentSummaryService` | WARN | access token 확보 실패, comment fetch 실패, routing fallback 없음, Discord batch 실패, page name 해석 실패 | 실패/부분 실패 추적에 필요 |
| `FigmaCommentDomainClassifier` | DEBUG | L1/L2 cache hit, classification 성공 | 진단용 |
| `FigmaCommentDomainClassifier` | WARN | 후보 외 LLM 응답, 호출 실패, batch 실패, cache 저장 실패, 빈 응답, hallucination 의심, JSON 파싱 실패 | LLM 품질/연동 장애 추적 |
| `FigmaCommentClient` | WARN | 댓글 페이지 상한 도달, 댓글 조회 실패. `fileKey`, `cursor`, `status`, `bodyLength` | 적절. 원문 body 미기록 |
| `FigmaFileMetadataClient` | WARN | 파일 메타데이터 조회 실패. `fileKey`, `status`, `bodyLength` | 적절 |
| `FigmaOAuthClient` | ERROR | OAuth code/refresh 실패. `status`, `bodyLength` | 적절 |
| `DiscordMentionWebhookAdapter` | WARN/ERROR/DEBUG | embed size 한도, message size 한도, batch 전송 성공/실패/부분 실패 | Discord payload 제약 진단에 필요 |
| `FigmaCommentClassificationPersistenceAdapter` | DEBUG | classification 동시 저장 race 무시 | race 처리 확인용 |
| `FigmaCommentDispatchPersistenceAdapter` | DEBUG | dispatch saveAll race 무시 | race 처리 확인용 |

구조화 외부 API 이벤트:

- `FIGMA`: `LIST_COMMENTS`, `RESOLVE_FILE_NODES`, `EXCHANGE_OAUTH_CODE`, `REFRESH_OAUTH_TOKEN`
- `DISCORD`: `SEND_FIGMA_DOMAIN_BATCH`

추가 계획:

- Figma dispatch retention scheduler는 batch metric과 표준 로그를 사용한다.
- Figma digest 결과는 후속으로 metric 분리를 검토한다. `total`, `unmatched`, `skippedDispatched`, `sent`, `failed`를 태그/카운터로 관리한다.
- Figma integration 등록은 audit 대상인지 검토한다.
- LLM classification 실패/후보 외 응답은 metric으로 집계해 품질 추이를 본다.

## global

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `ExternalApiCallLogger` | INFO/WARN | `external_api_called` structured event. provider, operation, result, durationMs, errorClass | structured log schema 유지. `OperationalMetrics` 연계 완료 |
| `LoggingInterceptor` | INFO/ERROR | request start/end summary, latency, status, 예외 요약 | 운영 요청 추적용. actuator/static 제외 정책 확인 필요 |
| `GlobalExceptionHandler` | WARN/ERROR | validation, JSON parse, missing parameter, type mismatch, business exception, unhandled exception | 4xx WARN 과다 가능. unhandled ERROR는 적절 |
| `CustomErrorController` | WARN | error controller fallback, business exception | fallback 진단용 |
| `JwtTokenProvider` | WARN/DEBUG | clientType claim 파싱 실패, JWT invalid/expired/unsupported/malformed | 반복 JWT 실패는 DEBUG로 낮춤. security metric 후보 |
| `JwtAuthenticationFilter` | DEBUG | JWT 인증 성공 memberId | 디버깅용 |
| `ApiAuthenticationEntryPoint` | WARN/ERROR | 인증 실패 URI/errorCode/message, unknown JWT error | 보안 이벤트 후보 |
| `QueryStatsJdbcEventListener` | DEBUG | slow query/query stats 요약 | SQL value 노출 정책 유지 필요 |
| `AsyncConfig` | ERROR | async method 예외 | 적절 |
| `SecurityConfig` | INFO | CORS allowed origins | bootstrap 설정 확인용 |
| `FcmConfig` | INFO/DEBUG | Firebase 초기화, 기존 FirebaseApp 확인 | credential/email/private key 세부 로그 제거 완료 |

추가 계획:

- `FcmConfig` credential trace/debug 로그는 제거했다.
- 4xx validation/business exception은 WARN이 아니라 DEBUG/INFO 또는 request summary와 통합하는 방안을 검토한다.
- JWT 실패는 security metric으로 집계하고 로그는 샘플링/레벨 조정한다.

## llm

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `ChatCompletionService` | INFO | 활성 provider, 호출 시작, 호출 완료. prompt/response length, latency, token count | 원문 prompt 미기록이라 적절. metric 후보 |
| `ChatCompletionService` | WARN | guard 차단, domain exception, unexpected exception | 장애 추적용 |
| `LlmCallGuard` | WARN | circuit open, consecutive failures, skipUntil | 운영 알림 후보 |
| `LlmRateLimiter` | DEBUG | token 부족 대기 시간 | 디버깅용 |
| `LlmFallbackConfig` | WARN | fallback provider 설정 | bootstrap 설정 이슈 |
| `SpringAi*ChatCompletionAdapter` | DEBUG/WARN | provider별 성공 length/token, 실패 model/error | provider adapter 진단용 |
| `MockChatCompletionAdapter` | DEBUG | mock echo length | local/test용 |

추가 계획:

- LLM 호출 latency/tokens/failure는 metric으로 분리한다.
- `ExternalApiCallLogger`를 LLM adapter에도 적용할지 결정한다. 현재 `ChatCompletionService`가 유사 정보를 직접 남긴다.

## maintenance

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `MaintenanceStateHolder` | WARN | snapshot refresh 실패, 기존 상태 유지 | 운영 상태 전환에 중요 |

추가 계획:

- maintenance mode 변경/조회 실패를 audit 또는 metric으로 보강한다.

## notice

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `NoticeQueryRepository` | DEBUG | 공지 조회 조건. gisu/chapter/school/part/memberParts/viewerRole | 쿼리 디버깅용 |
| `NoticeViewerInfoAssembler` | DEBUG | 지부 정보 조회 실패 fallback | 조합 실패 진단용 |

추가 계획:

- 공지 생성/수정/삭제/읽음 처리의 audit 대상 여부를 검토한다.

## notification

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `ServerLifecycleAlarmListener` | INFO | 서버 시작/종료 알림 전송 완료 시간 | 운영 이벤트 |
| `FcmAudienceService` | INFO/WARN/ERROR | FCM 비활성화 skip, 대상 없음, active token 없음, 발송 완료, batch 실패, 성공/실패 수 | title 원문 제거 완료. notification metric 추가 |
| `FcmTokenDeactivator` | INFO | invalid token 비활성화 tokenId/memberId | 운영 추적용. metric 후보 |
| `WebhookAlarmAspect` | DEBUG/ERROR | webhook annotation 감지, 처리 오류 | AOP 진단 |
| `WebhookAlarmScheduler` | DEBUG | buffer flush 스케줄 실행 | 디버깅용 |
| `WebhookAlarmService` | DEBUG/INFO/WARN/ERROR | buffer add, flush start, missing adapter, platform send success/failure | title/content 원문 제거 완료. notification metric 추가 |
| `DiscordWebhookAdapter`, `SlackWebhookAdapter`, `TelegramWebhookAdapter` | DEBUG | webhook 전송 완료 parts | title 원문 제거 완료 |
| `SesEmailAdapter` | INFO/ERROR | SES 성공 messageId, 실패 awsErrorCode, recipientPresent | 원문 수신자 제거 완료. external_api event 적용 |
| `SendEmailService` | ERROR | template rendering 실패 recipientPresent | 적절 |

구조화 외부 API 이벤트:

- `AWS_SES`: `SEND_EMAIL`
- `DISCORD`: `SEND_WEBHOOK`
- `SLACK`: `SEND_WEBHOOK`
- `TELEGRAM`: `SEND_WEBHOOK`

추가 계획:

- FCM/웹훅 발송 성공률과 실패율은 metric으로 분리했다.
- title/content 원문은 로그 필드에서 제거했다.

## project

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `MatchingRoundDeadlineHandler` | ERROR | matching round 자동 결정 실패, matchingRoundId | 스케줄러 장애 추적 |
| `ProjectResponseAssembler` | WARN | 팀원 일괄 조회 중 접근 권한 없음, 프로젝트 조회 실패 | response assembly fallback 진단 |

추가 계획:

- matching round auto decision은 batch metric과 `FINALIZE` audit 대상으로 선언했다.

## storage

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `FileCommandService` | INFO | upload URL 생성 완료, upload confirm, file delete 완료 | 파일 lifecycle 추적. audit 추가 완료 |
| `FileCommandService` | WARN | 파일 삭제/confirm 등에서 기대 상태와 다른 경우 | 운영 진단용 |
| `S3StorageProperties` | WARN | CloudFront 활성화 상태에서 signed URL key 누락 | 설정 오류 |
| `S3StorageAdapter` | DEBUG | S3 upload URL 생성, CloudFront signed URL 생성 완료 | signed URL 원문 제거 완료. storage external metric 추가 |
| `S3StorageAdapter` | INFO/WARN/ERROR | S3 삭제 완료, S3/CloudFront/S3 metadata 조회 실패 | 외부 스토리지 장애 추적. 성공/실패 metric 추가 |

추가 계획:

- 파일 생성/확인/삭제는 audit 대상에 추가했다.
- S3/CloudFront 호출은 metric으로 지연 시간/실패율을 남긴다.
- signed URL, signature, object URL 원문은 로그 금지 정책 테스트에 포함했다.

## test

| 위치 | 레벨 | 무엇을 남기는가 | 판단 |
| --- | --- | --- | --- |
| `TestController` | TRACE/DEBUG/INFO/WARN/ERROR | 로그 레벨 테스트, webhook AOP 테스트 | test/admin 목적. 운영 노출 여부 확인 필요 |
| `MemberSeedService` | INFO/ERROR | seed skip/start/complete/fail | seed 작업 추적 |
| `ChallengerSeedService` | INFO/ERROR | challenger seed start/result/fail | seed 작업 추적 |
| `CurriculumSeedService` | INFO/ERROR | curriculum/week/workbook/mission seed result/fail | seed 작업 추적 |
| `NoticeSeedService` | INFO/ERROR | notice seed result/fail | seed 작업 추적 |
| `ProjectSeedService` | INFO/ERROR | project seed start/result/fail | seed 작업 추적 |
| `ProjectApplicationSeedService` | INFO/WARN/ERROR | application seed skip/result/fail | seed 작업 추적 |
| `ProjectScenarioSeedService` | INFO/ERROR | scenario seed progress/fail | seed 작업 추적 |
| `ProjectSeedDataCleanupService` | WARN | seed project data cleanup start/result | destructive cleanup 강조 목적 |

추가 계획:

- seed 로그는 운영 관측성과 분리하기 위해 `seed` logger name 또는 admin operation audit로 분리한다.
- 실패 로그의 `e.toString()`은 errorClass/reasonCode로 표준화하고 stacktrace 필요 여부를 정한다.
