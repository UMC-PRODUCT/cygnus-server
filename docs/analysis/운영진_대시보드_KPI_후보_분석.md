# 운영진 대시보드 KPI 후보 분석

> 작성일: 2026-05-13  
> 갱신일: 2026-07-17 — Trophy 기능 제거 이후 현재 도메인 기준으로 갱신
> 분석 대상: `src/main/java/com/umc/product/**/domain`, `src/main/java/com/umc/product/**/application/port/in`, `src/main/java/com/umc/product/analytics/**`  
> 목적: 현재 백엔드 Entity와 UseCase 기준으로 운영진 대시보드에서 조회할 가치가 높은 KPI 후보를 추리고, 산출 근거와 선정 사유를 정리한다.

---

## 1. 요약

현재 레포지토리는 이미 `analytics` 도메인에 운영진 대시보드 전용 조회 UseCase와 일부 집계 API를 보유하고 있다. 따라서 KPI는 다음 순서로 우선 적용하는 것이 합리적이다.

1. **즉시 대시보드 카드/테이블로 노출 가능한 지표**
   - `AdminDashboardSummaryInfo`, `AdminSchoolSummaryInfo`, `AdminRiskChallengerInfo`, `AdminDashboardActionQueueInfo`로 이미 응답 형태가 정의된 지표
   - 예: 활동 챌린저 수, 신규 가입자 수, 활동 학교/지부 수, 월간 포인트 합계, 진행 중 프로젝트 수, 처리 대기 지원서 수, 학교별 위험군 수

2. **운영 액션을 직접 만드는 지표**
   - 지표가 높거나 낮을 때 운영진이 바로 개입할 수 있는 지표
   - 예: 위험군 챌린저, 미발송 공지, 승인 대기 출석, 매칭 진행 차수, 수료 임박 인원

3. **다음 이터레이션에서 확장할 추이/퍼널 지표**
   - 별도 집계 쿼리나 어댑터 구현이 필요하지만 운영 판단 가치가 높은 지표
   - 예: 워크북 제출/통과율, 프로젝트 지원 전환율, 공지 읽음률, FCM 발송 실패율

---

## 2. 코드 근거 인벤토리

### 2.1 KPI 산출에 직접 쓰기 좋은 Entity

| 도메인 | 주요 Entity | KPI로 활용 가능한 필드/상태 |
| --- | --- | --- |
| `member` | `Member`, `MemberProfile` | `createdAt`, `status`, `schoolId`, `profileImageId`, 프로필 링크 보유 여부 |
| `challenger` | `Challenger`, `ChallengerPoint`, `ChallengerRecord` | `gisuId`, `part`, `status`, `pointValue`, `PointType`, 초대 코드 사용 여부 |
| `organization` | `Gisu`, `Chapter`, `School`, `ChapterSchool`, `StudyGroup`, `StudyGroupMember`, `StudyGroupSchedule` | 현재 기수, 지부/학교 매핑, 스터디 그룹 수, 스터디 일정 수 |
| `authorization` | `ChallengerRole` | 운영진 역할, 조직 스코프, 학교 파트장 담당 파트 |
| `project` | `Project`, `ProjectApplication`, `ProjectMatchingRound`, `ProjectMember`, `ProjectPartQuota` | 프로젝트 상태, 지원서 상태, 매칭 차수 기간/단계, 파트별 TO와 합류 인원 |
| `schedule` | `Schedule`, `ScheduleParticipant`, `ScheduleParticipantAttendance` | 일정 수, 출석 정책 여부, 출석/지각/결석/승인 대기 상태 |
| `curriculum` | `Curriculum`, `WeeklyCurriculum`, `OriginalWorkbook`, `ChallengerWorkbook`, `MissionSubmission`, `MissionFeedback`, `WeeklyBestWorkbook` | 주차별 워크북, 배포 상태, 제출 상태, 피드백 결과, 우수 워크북 |
| `notice` | `Notice`, `NoticeRead`, `NoticeTarget`, `NoticeVote` | 공지 발송 필요 여부, 발송 시각, 필독 여부, 읽음 기록, 투표 기간 |
| `survey` | `Form`, `FormResponse`, `Answer` | 폼 발행 상태, 제출/임시저장 응답 수, 제출 시각 |
| `community` | `Post`, `Comment`, `Scrap`, `Report` | 게시글/댓글/스크랩/신고 활동량 |
| `notification` | `FcmToken`, `FcmOutbox` | 활성 토큰 수, 발송 대기/성공/실패, 재시도 수 |
| `audit` | `AuditLog` | 도메인별 운영 행위, 액션, 수행자, 대상, IP, 발생 시각 |
| `figma`, `llm`, `storage`, `term` | `FigmaWatchedFile`, `FigmaCommentClassification`, `FileMetadata`, `TermConsent` 등 | 동기화 오류, 분류량, 업로드량, 약관 동의 현황 |

대부분 Entity가 `BaseEntity`를 상속해 `createdAt`, `updatedAt`을 갖기 때문에, 일/주/월 단위 추이 집계가 가능하다.

### 2.2 이미 존재하는 운영진 대시보드 UseCase

| UseCase | 현재 제공 가능 지표 |
| --- | --- |
| `GetAdminDashboardSummaryUseCase` | 활동 챌린저 수, 신규 가입자 수/전주 대비, 활동 학교/지부 수, 월간 포인트 합계, 진행 중 프로젝트 수, 처리 대기 지원서 수, 챌린저 상태 분포 |
| `GetAdminSchoolSummaryUseCase` | 학교별 활동 챌린저 수, 회장/부회장, 파트장 배치 비율, 평균 포인트, 위험군 수, 이번 주 신규 인원 |
| `GetAdminRiskChallengerUseCase` | 위험군 챌린저 목록, 누적 포인트, 최근 감점 사유/일자 |
| `GetAdminDashboardActionQueueUseCase` | 처리 대기 지원서, 진행 중 매칭 차수, 미발송 공지, 이번 주 신규 위험군, 수료 임박 인원 |
| `GetAdminDashboardContextUseCase` | 운영진 역할 기반 데이터 스코프 |
| `GetAdminOperationsOverviewUseCase` | 지부/학교/파트/출석/스터디/가입 추이형 운영 현황. `AdminOperationsAnalyticsPersistenceAdapter`와 `AdminOperationsAnalyticsQueryRepository`로 집계 구현이 존재한다. |

### 2.3 기능 보존 기준

- `WeeklyBestWorkbook`은 커리큘럼 도메인의 주차별 우수 워크북 선정 결과로 계속 운영한다. 베스트 워크북 KPI는 이 모델만 집계한다.
- `Certificate`는 인증서 발급·검증·폐기 흐름을 보유한 독립 도메인으로 계속 운영한다. 인증서 발급 수와 폐기/검증 실패 수는 별도 KPI 후보로 유지한다.
- 과거 커뮤니티 `Trophy` 모델은 현재 KPI 산출 대상이 아니며, 이 문서에는 현재 운영 지표로 다시 추가하지 않는다.

---

## 3. KPI 선정 기준

- **운영 액션 가능성**: 지표가 이상할 때 담당 운영진이 바로 조치할 수 있어야 한다.
- **권한 스코프 적용 가능성**: 중앙, 지부, 학교, 학교 파트장 범위로 자연스럽게 필터링되어야 한다.
- **현재 데이터 신뢰도**: 이미 Entity 필드 또는 UseCase 응답으로 산출 가능한 지표를 우선한다.
- **비교 가능성**: 현재 값뿐 아니라 전주/전월 대비, 기수/지부/학교/파트 비교가 가능할수록 우선순위를 높인다.
- **N+1 회피 가능성**: FE가 목록을 모두 순회해 계산해야 하는 지표보다 서버 집계 쿼리로 산출 가능한 지표를 우선한다.

---

## 4. P0: 운영진 메인 대시보드에 우선 노출할 KPI

### 4.1 활동 챌린저 수

- **산출 기준**: 현재 기수의 `Challenger.status = ACTIVE` 수
- **코드 근거**: `Challenger.gisuId`, `Challenger.status`, `ChallengerStatus.ACTIVE`, `GetAdminDashboardSummaryUseCase`
- **선정 사유**: 조직 운영 규모와 현재 활성 사용자 풀을 가장 빠르게 보여주는 기본 KPI다. 지부/학교 스코프별 비교에도 적합하다.
- **구현 상태**: `AdminDashboardSummaryInfo.activeChallengerCount`로 제공 가능

### 4.2 신규 가입자 수와 전주 대비 증감률

- **산출 기준**: 최근 7일 `Member.createdAt` 기준 신규 가입자 수, 직전 7일 대비 증감률
- **코드 근거**: `Member.createdAt`, `Challenger.memberId`, `AdminDashboardSummaryInfo.newMemberCountThisWeek`, `newMemberDeltaPercent`
- **선정 사유**: 모집/온보딩 흐름이 정상적으로 작동하는지 빠르게 판단할 수 있다. 급감 시 홍보, 초대 코드, 가입 플로우 장애를 확인해야 한다.
- **구현 상태**: `AdminDashboardSummaryInfo`로 제공 가능

### 4.3 활동 학교 수 / 활동 지부 수

- **산출 기준**: 활동 챌린저가 1명 이상 있는 `schoolId`, `chapterId` distinct count
- **코드 근거**: `Member.schoolId`, `ChapterSchool`, `Chapter`, `Challenger.status`, `AdminDashboardSummaryInfo.activeSchoolCount`, `activeChapterCount`
- **선정 사유**: 단순 가입자 수보다 운영 커버리지 관점에서 유용하다. 특정 지부/학교의 활동 공백을 파악하는 출발점이 된다.
- **구현 상태**: `AdminDashboardSummaryInfo`로 제공 가능

### 4.4 월간 포인트 합계

- **산출 기준**: 이번 달 `ChallengerPoint`의 가산/감점 합계. `pointValue`가 있으면 우선 사용하고, 없으면 `PointType.value`를 사용
- **코드 근거**: `ChallengerPoint.pointValue`, `ChallengerPoint.type`, `PointType`, `AdminDashboardSummaryInfo.monthlyPointSum`
- **선정 사유**: 감점 급증은 출석/과제/운영 규칙 준수에 문제가 생겼다는 신호다. 가산점과 감점을 분리해야 운영 건강도를 오해하지 않는다.
- **구현 상태**: `AdminDashboardSummaryInfo.PointSumInfo`로 제공 가능

### 4.5 챌린저 상태 분포

- **산출 기준**: 현재 기수의 `ACTIVE`, `GRADUATED`, `EXPELLED`, `WITHDRAWN` count
- **코드 근거**: `Challenger.status`, `ChallengerStatus`, `AdminDashboardSummaryInfo.challengerStatusDistribution`
- **선정 사유**: 수료/제명/탈부 비율을 한 화면에서 비교할 수 있어 기수 운영 리스크를 조기에 파악할 수 있다.
- **구현 상태**: `AdminDashboardSummaryInfo`로 제공 가능

### 4.6 진행 중 프로젝트 수

- **산출 기준**: `Project.status = IN_PROGRESS` count
- **코드 근거**: `Project.gisuId`, `Project.chapterId`, `Project.productOwnerSchoolId`, `ProjectStatus.IN_PROGRESS`, `AdminDashboardSummaryInfo.projectInProgressCount`
- **선정 사유**: 프로젝트 운영 규모와 현재 매칭/수행 중인 팀 수를 보여준다. 기수 말에는 완료/중단 전환 관리 지표로도 활용된다.
- **구현 상태**: `AdminDashboardSummaryInfo`로 제공 가능

### 4.7 처리 대기 지원서 수

- **산출 기준**: `ProjectApplication.status = SUBMITTED` count
- **코드 근거**: `ProjectApplication.status`, `ProjectApplicationStatus.SUBMITTED`, `ProjectApplicationForm`, `Project`, `AdminDashboardSummaryInfo.pendingApplicationCount`
- **선정 사유**: 운영진이 즉시 처리해야 하는 업무량을 나타낸다. 대기 수가 높으면 지원자 경험과 매칭 일정이 지연된다.
- **구현 상태**: `AdminDashboardSummaryInfo`, `AdminDashboardActionQueueInfo`로 제공 가능

### 4.8 학교별 운영 현황

- **산출 기준**: 학교별 활동 챌린저 수, 회장/부회장, 파트장 배치 비율, 평균 포인트, 위험군 수, 이번 주 신규 인원
- **코드 근거**: `AdminSchoolSummaryInfo`, `Challenger`, `Member`, `School`, `ChapterSchool`, `ChallengerRole`
- **선정 사유**: 운영진 회의에서 가장 많이 필요한 비교 테이블이다. 특히 위험군 수와 파트장 배치율은 “지금 개입할 학교”를 찾는 데 유용하다.
- **구현 상태**: `GetAdminSchoolSummaryUseCase`, `GET /api/v1/admin/schools/summary`로 제공 가능

### 4.9 위험군 챌린저 목록

- **산출 기준**: 활동 챌린저 중 누적 포인트가 `riskThreshold` 이하인 대상, 최근 감점 내역 포함
- **코드 근거**: `AdminRiskChallengerInfo`, `ChallengerPoint`, `PointType`, `GetAdminRiskChallengerUseCase`
- **선정 사유**: KPI 카드보다 더 직접적인 액션 리스트다. 운영진이 개별 챌린저 상담, 공지, 제도 안내로 이어가기 쉽다.
- **구현 상태**: `GET /api/v1/admin/dashboard/risk-challengers`로 제공 가능

### 4.10 운영 액션 큐

- **산출 기준**: 처리 대기 지원서, 진행 중 매칭 차수, 미발송 공지, 이번 주 신규 위험군, 수료 임박 인원
- **코드 근거**: `AdminDashboardActionQueueInfo`, `ProjectApplication`, `ProjectMatchingRound`, `Notice`, `Gisu.period.endAt`, `Challenger`
- **선정 사유**: 대시보드 첫 화면에서 “오늘 처리할 일”을 직접 보여준다. 단순 현황보다 운영 효율에 더 크게 기여한다.
- **구현 상태**: `GetAdminDashboardActionQueueUseCase`, `GET /api/v1/admin/dashboard/action-queue`로 제공 가능

---

## 5. P1: 운영 현황을 더 잘 설명하는 확장 KPI

### 5.1 가입 추이 버킷

- **산출 기준**: `Member.createdAt` 또는 챌린저 생성 기준 일/주별 count
- **코드 근거**: `AdminOperationsOverviewInfo.SignupBucketInfo`, `Member.createdAt`
- **선정 사유**: 단일 “이번 주 신규”보다 모집 흐름의 상승/하락 추세를 잘 보여준다.
- **구현 상태**: `AdminOperationsAnalyticsQueryRepository`의 `listSignupBuckets` 집계로 제공 가능

### 5.2 지부-학교-파트별 챌린저 분포

- **산출 기준**: 지부/학교별 전체 챌린저 수와 `ChallengerPart`별 count
- **코드 근거**: `AdminOperationsOverviewInfo.ChapterSchoolStatusInfo`, `SchoolChallengerStatusInfo`, `Challenger.part`
- **선정 사유**: 파트 불균형, 특정 학교의 과소/과밀 운영을 파악할 수 있다.
- **구현 상태**: `AdminOperationsAnalyticsQueryRepository`의 `listChapterSchoolStatuses` 집계로 제공 가능

### 5.3 파트별 포인트 부여 현황

- **산출 기준**: 지부별/파트별 포인트 부여 건수와 합계
- **코드 근거**: `AdminOperationsOverviewInfo.ChapterPartPointGrantStatusInfo`, `ChallengerPoint`, `Challenger.part`
- **선정 사유**: 특정 파트에 감점이 몰리는지, 운영 기준 적용이 지부별로 편차가 큰지 확인할 수 있다.
- **구현 상태**: `AdminOperationsAnalyticsQueryRepository`의 `listPointGrantStatuses` 집계로 제공 가능. 기간 필터가 중요하므로 기본 30일, 기수 전체 토글을 권장한다.

### 5.4 일정 출석 현황

- **산출 기준**: 일정 수, 출석 필수 일정 수, 출석 기록 수, `AttendanceStatus`별 count
- **코드 근거**: `Schedule`, `ScheduleParticipant`, `ScheduleParticipantAttendance.status`, `AdminOperationsOverviewInfo.ScheduleAttendanceStatusInfo`
- **선정 사유**: 출석 승인 대기, 지각, 결석이 많은 조직을 조기에 찾을 수 있다.
- **구현 상태**: `AdminOperationsAnalyticsQueryRepository`의 `getScheduleAttendanceStatus` 집계로 제공 가능. 승인 대기 상태(`*_PENDING`)와 최종 상태(`PRESENT`, `LATE`, `ABSENT`, `EXCUSED`)를 분리해서 보여줘야 한다.

### 5.5 스터디 그룹 운영 현황

- **산출 기준**: `StudyGroup` 수, `StudyGroupSchedule` 수, 그룹별 멤버/멘토 배치 여부
- **코드 근거**: `StudyGroup`, `StudyGroupMember`, `StudyGroupMentor`, `StudyGroupSchedule`, `GetStudyGroupUseCase`
- **선정 사유**: UMC 운영에서 학습 그룹이 실제로 구성되고 주차별 활동이 잡히는지 확인하는 핵심 지표다.
- **구현 상태**: `AdminOperationsAnalyticsQueryRepository`의 `getStudyGroupStatus` 집계로 그룹 수와 일정 수는 제공 가능하다. 멘토 미배정 그룹 수는 추가 집계가 필요하다.

---

## 6. P2: 도메인별 전문 운영 KPI 후보

### 6.1 커리큘럼/워크북 KPI

| KPI | 산출 기준 | 코드 근거 | 선정 사유 |
| --- | --- | --- | --- |
| 워크북 배포 완료율 | `OriginalWorkbookStatus.RELEASED / 전체 워크북` | `OriginalWorkbook`, `WeeklyCurriculum` | 주차별 교육 콘텐츠 배포 지연을 탐지한다. |
| 과제 제출률 | `MissionSubmission` 제출 수 / 배포된 `ChallengerWorkbook` 수 | `ChallengerWorkbook`, `MissionSubmission` | 참여도와 학습 이탈을 측정한다. |
| 미평가 제출 수 | `SubmissionStatus.PENDING` count | `MissionSubmission`, `ManageMissionFeedbackUseCase` | 파트장/운영진의 리뷰 병목을 보여준다. |
| Fail/Late 비율 | `SubmissionStatus.FAIL`, `LATE` count / 제출 수 | `SubmissionStatus` | 과제 난이도나 공지 전달 문제를 조기에 발견한다. |
| 우수 워크북 선정 수 | `WeeklyBestWorkbook` count | `WeeklyBestWorkbook` | 파트/학교별 학습 성과를 긍정 지표로 보여준다. |

### 6.2 프로젝트/매칭 KPI

| KPI | 산출 기준 | 코드 근거 | 선정 사유 |
| --- | --- | --- | --- |
| 프로젝트 상태 퍼널 | `DRAFT`, `PENDING_REVIEW`, `IN_PROGRESS`, `COMPLETED`, `ABORTED` count | `Project.status` | 프로젝트 운영 흐름과 병목 단계를 확인한다. |
| 지원서 처리율 | `APPROVED/REJECTED` / `SUBMITTED + APPROVED + REJECTED` | `ProjectApplication.status` | 심사 지연과 운영 처리량을 측정한다. |
| 파트별 TO 충원율 | `ProjectMember` count / `ProjectPartQuota.quota` | `ProjectPartQuota`, `ProjectMember` | 매칭 결과의 균형과 미충원 파트를 파악한다. |
| 매칭 차수 진행률 | 현재 시각이 `startsAt~endsAt`에 포함되는 차수와 `phase` | `ProjectMatchingRound` | 매칭 일정 지연과 차수별 상태를 관리한다. |
| 중단 프로젝트 비율 | `ProjectStatus.ABORTED / 전체 프로젝트` | `Project.status` | 프로젝트 운영 리스크를 추적한다. |

### 6.3 공지/설문 KPI

| KPI | 산출 기준 | 코드 근거 | 선정 사유 |
| --- | --- | --- | --- |
| 필독 공지 미열람 수 | `Notice.mustRead = true` 대상 중 `NoticeRead` 미존재 | `Notice`, `NoticeRead`, `NoticeTarget` | 중요한 운영 공지가 전달되지 않은 대상을 찾는다. |
| 공지 발송 대기 수 | `shouldSendNotification = true` and `notifiedAt is null` | `Notice` | 운영진이 즉시 처리할 발송 누락을 보여준다. |
| 공지 투표 참여율 | `NoticeVote.voteId`에 대한 제출 응답 수 / 대상자 수 | `NoticeVote`, `survey.FormResponse` | 공지 기반 의사결정 참여도를 확인한다. |
| 설문 응답률 | `FormResponseStatus.SUBMITTED / 대상자 수` | `Form`, `FormResponse` | 운영 설문, 프로젝트 지원 폼 등의 회수율을 관리한다. |
| 임시저장 이탈 수 | `FormResponseStatus.DRAFT` count | `FormResponse` | 폼 UX 문제나 응답 완료 독려 대상을 파악한다. |

### 6.4 커뮤니티/신고 KPI

| KPI | 산출 기준 | 코드 근거 | 선정 사유 |
| --- | --- | --- | --- |
| 게시글/댓글 생성 추이 | `Post.createdAt`, `Comment.createdAt` 기간별 count | `Post`, `Comment` | 커뮤니티 활성도를 보여준다. |
| 스크랩/좋아요 참여율 | 스크랩/좋아요 수 / 게시글 수 | `Scrap`, `Post.likeCount`, `Comment.likeCount` | 콘텐츠 반응도를 측정한다. |
| 신고 대기 건수 | `Report.status = PENDING` count | `Report` | 운영진 moderation 업무량을 보여준다. |

### 6.5 시스템 운영 KPI

| KPI | 산출 기준 | 코드 근거 | 선정 사유 |
| --- | --- | --- | --- |
| FCM 발송 실패율 | `FcmOutbox.status = FAILED` / 전체 outbox | `FcmOutbox`, `ProcessFcmOutboxUseCase` | 알림 인프라 문제를 빠르게 감지한다. |
| FCM 활성 토큰 수 | `FcmToken.isActive = true` count | `FcmToken` | 실제 푸시 도달 가능 사용자 풀을 추정한다. |
| Figma 동기화 실패 파일 수 | `FigmaWatchedFile.enabled = true` and `lastError not null` | `FigmaWatchedFile`, `SyncFigmaCommentsUseCase` | Figma-Discord 운영 자동화 장애를 감지한다. |
| LLM 호출량/토큰 사용량 | `ChatCompletionResult.promptTokens`, `completionTokens` 합계 | `ChatCompleteUseCase`, `LlmMetrics` | LLM 비용과 rate limit 리스크를 추적한다. |
| 파일 업로드량 | `FileMetadata.fileSize`, `category`, `uploadedMemberId` 기간별 합계 | `FileMetadata`, `ManageFileUseCase` | 저장소 비용과 비정상 업로드를 감시한다. |
| 감사 로그 주요 액션 수 | `AuditLog.domain`, `action`, `actorMemberId`, `createdAt` count | `AuditLog`, `GetAuditLogUseCase` | 권한 변경, 삭제, 대량 처리 같은 민감 운영 행위를 추적한다. |

---

## 7. 권장 대시보드 구성

### 7.1 첫 화면 KPI 카드

1. 활동 챌린저 수
2. 신규 가입자 수 및 전주 대비 증감률
3. 활동 학교 수 / 활동 지부 수
4. 월간 포인트 가산/감점 합계
5. 진행 중 프로젝트 수
6. 처리 대기 지원서 수

### 7.2 운영 현황 테이블/리스트

1. 학교별 운영 현황 테이블
2. 위험군 챌린저 리스트
3. 운영 액션 큐
4. 챌린저 상태 분포

### 7.3 다음 이터레이션 차트

1. 가입 추이
2. 지부/학교/파트별 챌린저 분포
3. 파트별 포인트 부여 현황
4. 일정 출석 상태 분포
5. 커리큘럼 제출/피드백 현황
6. 프로젝트 지원/선발 퍼널

---

## 8. 구현 우선순위

| 우선순위 | KPI 묶음 | 이유 | 현재 상태 |
| --- | --- | --- | --- |
| P0 | 요약 카드, 학교별 현황, 위험군 리스트, 액션 큐 | 운영진이 매일 확인하고 즉시 조치할 수 있다. | `analytics` UseCase/Controller/일부 repository 구현 존재 |
| P1 | 가입 추이, 파트별 분포, 포인트 추이, 출석/스터디 현황 | 운영 원인 분석과 회고에 필요하다. | `AdminOperationsOverviewInfo`와 `AdminOperationsAnalyticsQueryRepository` 구현 존재 |
| P2 | 커리큘럼, 프로젝트 퍼널, 공지/설문, 커뮤니티, 시스템 운영 지표 | 도메인별 운영 성숙도를 높인다. | 각 도메인 Entity/UseCase는 있으나 전용 집계 API 필요 |

---

## 9. 주의사항

- FE에서 전체 목록을 내려받아 합산하는 방식은 위험군/포인트/공지 읽음률처럼 데이터가 커지는 지표에 적합하지 않다. 서버 집계 API로 제공해야 한다.
- 모든 지표는 `gisuId`를 기본 스코프로 삼고, 중앙/지부/학교/학교 파트장 권한에 따라 `chapterId`, `schoolId`, `responsiblePart` 필터가 자동 적용되어야 한다.
- 신규 가입자 수는 `Member.createdAt` 기준인지, `Challenger.createdAt` 기준인지 제품 정의가 필요하다. 현재 summary 구현은 `Member.createdAt`을 사용한다.
- 포인트 합산은 `ChallengerPoint.pointValue`와 `PointType.value`가 함께 존재하므로, 현재 analytics 구현처럼 `pointValue` 우선, 없으면 `PointType.value`를 사용하는 기준을 문서화해야 한다.
- 운영 현황 API는 컨트롤러/서비스/어댑터/QueryDSL repository가 존재한다. FE 연결 전에는 실제 응답의 기간 필터, 권한 스코프, 대량 데이터 성능을 별도로 확인해야 한다.
