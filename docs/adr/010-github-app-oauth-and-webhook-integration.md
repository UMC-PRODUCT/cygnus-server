# ADR-010: 단일 GitHub App으로 OAuth 로그인·Webhook 통합·미션 제출·활동 지표 수집을 동시에 수용한다

## Status

Proposed

## Context

UMC PRODUCT는 Spring/Web/Android/iOS/Node 등 개발 파트 챌린저가 다수 존재하며, 챌린저가 자신의 GitHub repository에서 진행하는 활동(Issue 생성, Pull Request 생성/머지, Review, Commit, CI/CD 실행, 배포)이 챌린저 운영과 커리큘럼 미션 평가 양쪽에 의미 있는 신호가 된다.

운영 측면에서 다음 네 가지 요구가 동시에 발생했다.

1. **GitHub 계정으로 서비스 로그인**: 개발 파트 챌린저는 대부분 이미 GitHub 계정을 보유하고 있다. 별도 회원가입을 줄이기 위해 Google/Apple/Kakao와 동일한 위치에서 GitHub OAuth 로그인을 추가해야 한다.
2. **본인 repository 활동의 Discord 통지**: 챌린저가 자신의 repository에 Issue를 열거나 PR을 생성/머지하면, 해당 챌린저가 속한 스터디 그룹/프로젝트 Discord 채널로 안내가 전달되기를 원한다.
3. **PR 코멘트 기반 미션 제출**: 챌린저가 자신의 PR에서 우리 App을 멘션하고 `N주차 미션 제출`이라고 작성하면, 해당 코멘트를 단 사람의 GitHub 계정과 서비스 회원을 `member_oauth`로 연결해 미션 제출 기록을 남긴다. 추후 curriculum 도메인의 미션 평가 흐름과 연동할 수 있어야 한다.
4. **GitHub 활동 지표 수집**: 우리 App이 설치된 Organization/Repository에 대해 지표를 수집·집계하고 운영진에게 조회 API로 제공한다. 운영진과의 협의 결과, 지표는 **차원(scope) × 유형(count / timestamp / duration / ratio / status)** 매트릭스로 정리하기로 했고, 동일한 측정값이 여러 차원에 등장하더라도 차원별 의미가 다른 경우 의도적으로 유지한다(자세한 결정은 Decision §12 참조).
    - **Organization 차원**: 연결/활성 Repository 수, 최근 7일 활동 Repository/Commit/PR 생성/PR Merge 수, Open PR 수, Open Issue 수, 리뷰 대기 PR 수, CI 실패 PR 수, 최근 배포 일시.
    - **Repository 차원**: Repository 이름, default branch, 최근 Commit/PR/Issue 일시, Open PR 수·Open Issue 수·Merged PR 수·Closed Issue 수(누적/현재), 최근 7일 Commit 수·Contributor 수, CI 성공/실패 상태, 최근 배포 상태.
    - **Commit 차원**: 전체/오늘/이번 주/이번 달 카운트, Repository별·사용자별·branch별 카운트, 마지막 Commit 시각, 최근 N일간 Commit이 없는 Repository, 컨벤션 위반 수. 운영 가이드: ✅ 최근 활동 여부 확인 / Repository 정체 여부 확인 / 팀 단위 활동 흐름 파악에 권장. ⚠️ 개인 평가 점수로 직접 사용 / "Commit 수가 많으면 기여도가 높다"는 단정은 금지.
    - **PR 차원 — 기본 카운트**: 생성된 PR 수, Open/Closed/Merged/Draft PR 수, 리뷰 대기 PR 수, Conflict 발생 PR 수, 오래 열린 PR 수, 라벨별·Assignee별·Reviewer별 PR 수.
    - **PR 차원 — 흐름 지표**: 평균 PR 처리 시간(생성→merge), 평균 리뷰 대기 시간(생성→첫 리뷰, Review 차원의 "평균 첫 리뷰 응답 시간"과 동일 측정값·다른 관점), 평균 merge 소요 시간(첫 approve→merge).
    - **PR 차원 — 리스크/크기**: 오래 방치된 PR(예: 3일 이상 리뷰 없음), 리뷰 없이 merge된 PR(Review 차원에도 유지), CI 실패 상태로 남아 있는 PR, 변경 파일 수·추가/삭제 라인 수·commit 수.
    - **Issue 차원**: 평균 처리 시간(생성→close), 오래 열린 Issue 수(예: 7일 이상 open), 담당자 없는 Issue 비율, Bug Issue 비율, close된 Issue 대비 새로 생긴 Issue 비율, Sprint/Milestone 완료율.
    - **Review 차원**: 요청된 Review 수, 완료된 Review 수, Approve/Request changes/Comment 수, 리뷰어별 Review 수·작성자별 받은 Review 수, 평균 첫 리뷰 응답 시간, 평균 Approve까지 걸린 시간, 리뷰 없이 merge된 PR 수(PR 차원에도 유지), 같은 사람이 작성하고 바로 merge한 PR 수.
    - **CI/CD 차원 — Workflow**: 실행 수, 성공 수, 실패 수, 평균 Workflow 실행 시간, 실패한 Workflow 목록, Repository별 최근 CI 상태, PR별 CI 상태.
    - **CI/CD 차원 — 배포**: 최근 배포 성공/실패 상태, 배포 횟수, 배포 실패율, 마지막 배포 시각(= Org 차원 "최근 배포 일시", Repo 차원 "최근 배포 상태"의 source).
    - **사용자(개인) 차원**: 산출물(Commit/PR 생성/PR Merge/Issue 생성/Issue Close/Review/Comment 수), 최근 활동 일시, 참여 Repository 수, 현재 부하(담당 중인 Issue 수, 리뷰 대기 중인 PR 수).
    - **위험 신호 (자동 감지)**: 활동 정체(7일 이상 활동 없는 Repository, 3일 이상 리뷰 없는 PR, 7일 이상 open Issue, 장기 배포 부재), 병목/편중(담당자 없는 Issue 다수, 특정 리뷰어 부하 집중, PR-vs-merge 불균형, Issue-vs-close 불균형), 품질 문제(CI 반복 실패, main/develop 빌드 실패).

기술 환경과 제약은 다음과 같다.

- 서비스는 이미 [OAuthProvider](../../src/main/java/com/umc/product/common/domain/enums/OAuthProvider.java) enum과 `member_oauth` 테이블을 통해 Google/Apple/Kakao OAuth 로그인을 운영 중이다. 신규 provider는 enum과 분기 한 곳을 추가하면 흡수 가능한 패턴이 잡혀 있다.
- 기존 Discord 발송 인프라(`DiscordWebhookAdapter`, `SendDiscordMentionPort`)가 존재하며, GitHub 활동 알림도 이를 재사용한다.
- 커리큘럼 도메인(`com.umc.product.curriculum`)에는 `WorkbookMission`, `MissionSubmission`, `ChallengerMission` 등이 있어 GitHub 활동을 미션 신호로 연결할 여지가 있다.
- GitHub은 동일한 목적에 대해 세 가지 인증 메커니즘(OAuth App, GitHub App, Personal Access Token)을 제공한다. 두 가지를 함께 운영하면 secret/콜백 URL이 두 배로 늘어 운영 복잡도가 빠르게 증가한다.
- GitHub REST API는 Installation Token 기준 시간당 5,000 요청, GraphQL은 5,000 포인트 제한이 있다. 대규모 Organization에서 폴링 전략은 이 한도를 소진할 위험이 있다.
- 활동 지표는 실시간 정밀도가 필요하지 않다. 운영진이 주 단위 또는 일 단위로 확인하는 대시보드 수준이면 충분하다.
- Commit 지표는 운영진의 "활동 흐름 파악" 용도이며, **개인 평가 점수로 직접 사용하지 않는다**는 운영 원칙이 함께 합의되어 있다(commit 수가 많은 것이 곧 기여도 높음을 뜻하지 않는다).

## Decision

우리는 다음과 같이 결정한다.

### 1. 단일 GitHub App으로 OAuth 로그인과 Webhook 수신을 동시에 처리한다

"Request user authorization (OAuth) during installation" 옵션을 켜서 GitHub App 자체를 OAuth provider로 사용한다. 별도 OAuth App을 만들지 않는다. App에는 **user-to-server** 토큰(로그인용)과 **server-to-server** installation token(repository API 호출용) 두 종류의 토큰이 발급된다. 두 토큰의 책임은 명확히 분리한다.

### 2. 로그인은 user-to-server 토큰의 ID 정보만 사용한다

발급받은 user access token은 즉시 `/user`, `/user/emails` 호출에만 사용하고, 서비스 자체 access/refresh token으로 교환한 뒤 메모리에서 폐기한다. user access token을 DB에 영속화하지 않는다. GitHub App의 user-to-server OAuth는 `scope` 파라미터를 사용하지 않으며, 노출 권한은 GitHub App 설정의 Account permissions로 결정된다.

### 3. Webhook 수신 권한과 repository 접근은 Installation 단위로 관리한다

사용자는 GitHub의 "Install this App" 화면에서 본인의 repository(또는 Organization)에 App을 설치한다. 설치 시 GitHub이 보내는 `installation` 이벤트로 `installation_id`와 설치된 repository 목록을 수신해 `github_installation`/`github_repo` 테이블에 저장한다. repository API 호출이 필요할 때마다 App private key로 서명한 App JWT로 installation access token을 발급받아 호출한다. installation access token은 1시간 만료이며 절대 영속화하지 않는다.

### 4. Webhook 이벤트는 단일 endpoint에서 수신하고, 서명 검증·멱등성 저장·이벤트 분기를 담당한다

`POST /webhooks/github`에서 HMAC SHA-256 서명(`X-Hub-Signature-256`) 검증 후, `X-GitHub-Delivery`(UUID)를 UNIQUE INSERT로 멱등성을 보장한다. 수신 직후 200 반환하고, 본격 처리는 `ApplicationEventPublisher`로 비동기 위임한다.

구독하는 Webhook 이벤트 목록은 다음과 같다.

| 이벤트                                         | 용도                                                            |
|---------------------------------------------|---------------------------------------------------------------|
| `installation`, `installation_repositories` | Installation/Repo 상태 동기화, 신규 설치 시 backfill 트리거                |
| `pull_request`                              | PR stat UPSERT(라벨/Assignee/Milestone/Reviewer 포함), Discord 발송 |
| `pull_request_review`                       | Review stat 저장(state별 분리), 첫 리뷰/첫 approve 시각 갱신               |
| `pull_request_review_comment`               | Review comment 카운트                                            |
| `issue_comment`                             | PR 코멘트 미션 제출 파싱, comment stat 저장                              |
| `issues`                                    | Issue stat UPSERT(라벨/Assignee/Milestone 포함), Discord 발송       |
| `push`                                      | Commit stat 저장, 커밋 컨벤션 검증                                     |
| `check_run`, `check_suite`                  | CI 상태 저장, PR-CI 매핑                                            |
| `workflow_run`                              | CI/CD 실행 기록 저장(duration·conclusion 포함)                        |
| `deployment`, `deployment_status`           | 배포 상태 저장                                                      |

추가로, "리뷰 요청이 특정 사람에게 몰리는" 위험 신호 탐지를 위해 `pull_request` 이벤트의 `action="review_requested"`/`"review_request_removed"`를 별도 stat 테이블(`github_review_request_stat`)에 적재한다.

### 5. Discord 알림은 기존 발송 인프라를 재사용한다

이벤트 핸들러는 `github` 도메인 안에 두고, `notification` 도메인의 `SendWebhookPort`/`SendDiscordMentionPort`를 호출한다. 어느 Discord 채널·role로 보낼지는 `github_repo_link` 테이블의 `discord_channel_webhook_url`/`mention_role_id`에 따른다.

### 6. 신규 도메인 `com.umc.product.github`을 신설한다

외부 시스템 통합은 별도 컨텍스트로 둔다.

### 7. 커리큘럼 연동은 별도 도메인 이벤트 인터페이스로 분리해 둔다

본 ADR 범위에서는 `GithubPullRequestMergedEvent`, `GithubIssueOpenedEvent`, `GithubMissionSubmittedEvent` 등 도메인 이벤트만 정의하고 발행한다. 이벤트 record 자체는 GitHub-특정 페이로드를 제거한 일반화된 형태로 `com.umc.product.common.event`에 둔다. github 도메인은 webhook payload → 일반 이벤트 변환만 책임지고, curriculum은 일반 이벤트 타입에만 의존한다.

### 8. PR 코멘트 기반 미션 제출은 `issue_comment` Webhook 이벤트로 탐지한다

`issue_comment` 이벤트 중 `issue.pull_request` 필드가 존재하는 경우(= PR 코멘트)에 한해, 코멘트 본문에서 `@{app-bot-login}` 멘션과 `(\d+)주차 미션 제출` 패턴을 정규식으로 추출한다. 탐지 시 다음 흐름을 밟는다.

1. `comment.user.login`(GitHub 계정) → `member_oauth` WHERE `provider=GITHUB` AND `provider_id=login` 으로 서비스 회원 조회.
2. `github_mission_submission` 테이블에 `comment_id` UNIQUE INSERT로 멱등성을 보장하며 기록한다.
3. 회원이 연결되어 있으면 `GithubMissionSubmittedEvent`를 발행한다.
4. 회원 미연결 상태는 `curriculum_status=UNLINKED`로 기록하고, 추후 해당 GitHub 계정이 OAuth 로그인하면 `member_oauth` insert 트리거에서 `UNLINKED` 행을 일괄 백필(`PENDING`으로 전환 + 이벤트 재발행)한다.

App bot 계정 이름(`{app-bot-login}`)은 환경변수로 주입한다.

### 9. 활동 지표는 Webhook 이벤트 수신 시점에 stat 테이블로 즉시 적재하고, 계산 비용이 큰 지표는 야간 배치로 사전 집계한다

순수 API 폴링 방식 대신, Webhook 이벤트를 수신할 때마다 다음 stat 테이블에 UPSERT/INSERT한다.

- `github_pr_stat`: PR 1건 = 1행. 라벨/Assignee/Milestone/Reviewer 정보를 함께 보관해 라벨별·Assignee별·Reviewer별 집계가 단일 테이블에서 가능하다.
- `github_issue_stat`: Issue 1건 = 1행. labels·assignees·milestone 보관.
- `github_review_stat`: 리뷰 1건 = 1행(state별 row).
- `github_review_request_stat`: 리뷰 요청 1건 = 1행. 부하 분산 분석용.
- `github_commit_stat`: commit 1건 = 1행.
- `github_workflow_run_stat`: workflow run 1건 = 1행.
- `github_deployment_stat`: deployment 1건 = 1행.

야간 배치는 다음 두 종류로 분리한다.

- **`github_repo_metrics_snapshot` 갱신 배치(매시간)**: 각 repo의 "지금 시점 카운트형 지표"(Open PR 수, 7일 commit 수, 최근 CI 상태 등)를 캐시 테이블에 UPSERT. 운영진 대시보드 응답 시간을 안정화한다.
- **`github_metrics_daily` 일별 집계 배치(매일 새벽 3시)**: 일자별 PR/Issue/Commit/Review/배포 카운트를 JSONB로 저장. 시계열 트렌드 조회 및 장기 보존용.

stat 테이블 원본은 90일, `github_metrics_daily`는 1년, `github_repo_metrics_snapshot`은 가장 최근 1행만 보관(매시간 UPSERT)한다. 최초 설치 시 과거 데이터 backfill은 GitHub REST API로 1회 호출해 채운다.

### 10. 활동 지표 조회 API는 github 도메인 내 Query 서비스로 운영진에게만 노출한다

다음 Query UseCase를 통해 `/admin/github/metrics/**` 경로로 노출한다. 모든 admin 경로는 SUPER_ADMIN만 접근 가능하다.

| UseCase                            | Endpoint                                            | 책임                        |
|------------------------------------|-----------------------------------------------------|---------------------------|
| `GetGithubOrgMetricsUseCase`       | `GET /admin/github/metrics/orgs/{installationId}`   | Organization 작업 현황        |
| `GetGithubRepoMetricsUseCase`      | `GET /admin/github/metrics/repos/{repoId}`          | Repository 단일 카드 지표       |
| `GetGithubCommitMetricsUseCase`    | `GET /admin/github/metrics/repos/{repoId}/commits`  | Commit 차원(기간/branch/사용자별) |
| `GetGithubPrFlowMetricsUseCase`    | `GET /admin/github/metrics/repos/{repoId}/prs`      | PR 흐름 지표                  |
| `GetGithubIssueFlowMetricsUseCase` | `GET /admin/github/metrics/repos/{repoId}/issues`   | Issue 흐름 지표               |
| `GetGithubReviewMetricsUseCase`    | `GET /admin/github/metrics/repos/{repoId}/reviews`  | Review 지표                 |
| `GetGithubCiCdMetricsUseCase`      | `GET /admin/github/metrics/repos/{repoId}/ci-cd`    | CI/CD 지표                  |
| `GetGithubUserActivityUseCase`     | `GET /admin/github/metrics/users/{memberIdOrLogin}` | 개인 활동 지표(다수 repo 합산)      |
| `GetGithubRiskSignalsUseCase`      | `GET /admin/github/metrics/risk-signals`            | 위험 신호 목록(전사/repo 단위)      |

위험 신호(오래 방치 PR 등)는 야간 배치에서 탐지해 Discord 알림을 별도 채널로 발송한다.

### 11. Commit 지표 응답에는 운영 가이드 문구를 포함한다

API 응답 메타데이터에 `usage_guidance` 필드를 두어 다음 두 항목을 함께 내려준다.

- ✅ **권장 활용**: 최근 활동 여부 확인 / Repository 정체 여부 확인 / 팀 단위 활동 흐름 파악.
- ⚠️ **주의(금지)**: 개인 평가 점수로 직접 사용. "Commit 수가 많으면 기여도가 높다"는 단정.

프런트엔드 대시보드에서 강제로 노출되도록 한다. 이는 운영진이 commit 수치를 평가 도구로 오용하는 것을 구조적으로 억제하기 위한 결정이다. 동일 가이드는 사용자(개인) 차원 응답(`GET /admin/github/metrics/users/{...}`)의 commit 관련 필드에도 적용된다.

### 12. 중복으로 보이는 지표는 "원본 보존 원칙"에 따라 모두 유지하고 출처를 명시한다

요구사항 정리 과정에서 동일 측정값이 여러 차원에 등장하는 경우가 다수 식별되었다(예: "최근 배포 일시"는 Org / CI/CD / Repo 세 차원에 등장). 이를 한 차원으로 통폐합하지 않고, 차원별 의미·집계 단위·UI 노출 위치가 다르다는 운영진 피드백을 받아들여 모두 유지하기로 한다. 대신 **단일 source 데이터에서 파생되도록 구현 단계에서 강제**해 값 불일치를 방지한다.

| 지표                                       | 등장 차원                   | 처리 결정                                                            |
|------------------------------------------|-------------------------|------------------------------------------------------------------|
| Open PR 수, Open Issue 수                  | Repo / Org / PR 기본 / 개인 | 차원별 집계 단위가 다름. 모두 유지.                                            |
| 최근 배포 일시 / 마지막 배포 시각 / 최근 배포 상태          | Org / CI/CD / Repo      | `github_deployment_stat` 1개 source에서 파생. 차원별 표현만 다르게 유지.         |
| 리뷰 없이 merge된 PR                          | PR 흐름 / Review          | "PR 품질" 관점과 "리뷰 문화" 관점 양쪽에 유지.                                   |
| 평균 리뷰 대기 시간 / 평균 첫 리뷰 응답 시간              | PR 흐름 / Review          | `github_pr_stat.first_review_at - created_at` 동일 계산식. 양쪽 응답에 노출. |
| 리뷰 대기 PR 수                               | Org / PR 기본 / 개인        | 집계 단위(전체 org / repo / 본인) 다름. 모두 유지.                             |
| 오래 열린 PR / 오래 방치된 PR / 3일 이상 리뷰 없는 PR    | PR 기본 / PR 흐름 / 위험 신호   | "오래"의 정의는 임계값 정의 단계에서 구체화하며, 환경변수(`alert-stale-pr-days`)로 일원화한다. |
| 최근 N일간 Commit 없는 Repo / 7일 이상 활동 없는 Repo | Commit / 위험 신호          | 임계값 정의 단계에서 구체화하며, 환경변수(`alert-inactive-repo-days`)로 일원화한다.      |

구현 측면에서는 위 매트릭스가 곧 "어느 stat 테이블이 어느 응답 필드를 채우는지"의 source-of-truth가 된다. 자세한 매핑은 Implementation Notes의 "지표 source 매트릭스"를 참조한다.

## Alternatives Considered

### A-1. OAuth App + Personal Access Token 조합

장점: 가장 단순하다. App JWT/installation token 흐름이 없다.

단점: PAT는 webhook을 발급할 수 없어 polling이 강제된다. rate limit·지연·대량 사용자 확장성 문제가 생긴다. 사용자가 PAT를 직접 입력하는 UX 장벽이 크다.

선택하지 않은 이유: 이벤트 기반 Discord 통지와 실시간 지표 수집의 핵심 요구를 폴링으로는 충족할 수 없다.

### A-2. OAuth App + 별도 GitHub App 동시 운영

장점: 책임이 시각적으로 분리된다.

단점: client_id/client_secret/private key/webhook secret이 두 세트가 되어 운영 부담이 두 배다. 두 흐름의 식별자(login ↔ installation id)를 백엔드가 직접 이어야 한다.

선택하지 않은 이유: GitHub App의 "Request user authorization during installation" 옵션이 두 요구를 이미 충족한다.

### A-3. Webhook을 별도 Lambda/Worker에서 수신

장점: Spring 애플리케이션에서 외부 노출 엔드포인트를 분리할 수 있다.

단점: 신규 인프라(IaC, 배포, 모니터링)가 추가된다. 서버까지 hop이 생기며 그 구간 인증/멱등성 책임을 새로 정의해야 한다. 2026년 5월 기준 webhook 수신량이 많지 않아 분리의 실익이 작다.

선택하지 않은 이유: Spring Boot 레벨에서 안전하게 구현 가능하다. 폭증 시 superseding ADR로 재검토한다.

### A-4. user access token을 DB에 영속화해 GitHub API를 호출

장점: 어떤 API든 사용자 권한 그대로 호출할 수 있다.

단점: user access token 유출 시 사용자 GitHub 전체 권한이 노출된다. 만료·refresh 누락이 잦다. repository 작업은 봇 ID(App)로 수행하는 것이 audit·UX 양면에서 명확하다.

선택하지 않은 이유: GitHub 권장 패턴(서버는 installation token, 사용자는 user-to-server)과 어긋나고 보안 노출면이 너무 크다.

### A-5. Webhook 페이로드를 동기적으로 처리

장점: 흐름이 단순하다.

단점: GitHub의 응답 SLA(권장 10초, 강제 30초)를 초과할 가능성이 있다. 핸들러 한 부분 실패가 webhook 전체 실패로 이어져 GitHub이 재전송한다.

선택하지 않은 이유: 수신 컨트롤러는 빠른 200 응답 + 멱등 저장만 책임지고 처리는 비동기 분리가 안정적이다.

### A-6. 활동 지표를 GitHub API 폴링 전용으로 수집

장점: 과거 데이터 조회 가능. 코드 단순.

단점: 설치된 repository 수에 비례해 API 호출 수가 선형 증가한다. Installation token 기준 시간당 5,000 요청 한도를 조직 규모에 따라 쉽게 초과한다. 불필요한 중복 호출이 많다.

선택하지 않은 이유: Webhook 이벤트를 수신할 때마다 stat 테이블에 저장하면 추가 API 호출 없이 대부분의 지표를 실시간에 가깝게 유지할 수 있다. API 폴링은 최초 설치 시 backfill로 역할을 한정한다.

### A-7. 활동 지표를 외부 Analytics 서비스(LinearB, Swarmia 등)로 위임

장점: 즉시 사용 가능. 유지보수 부담 없음.

단점: 월정액 비용. 데이터 통제권 상실. curriculum 도메인과의 자체 연동 불가. 커스텀 위험 신호 정책 적용 불가.

선택하지 않은 이유: curriculum 미션 평가와의 연동, 자체 위험 신호 정책이 핵심 요구이므로 외부 서비스에 위임할 수 없다.

### A-8. 활동 지표를 TimescaleDB/ClickHouse 같은 별도 시계열 DB로 저장

장점: 시계열 쿼리 최적화. 대용량 처리에 유리.

단점: 신규 인프라 도입. 2026년 5월 기준 데이터 규모에 비해 과도하다. 운영 부담이 크다.

선택하지 않은 이유: PostgreSQL의 부분 인덱스와 JSONB 집계로 현 규모에서 충분히 대응 가능하다. 규모가 커지면 별도 ADR로 마이그레이션한다.

### A-9. PR/Issue 라벨·Assignee를 별도 정규화 테이블로 분리

장점: 라벨별 카운트가 인덱스만으로 빠르게 처리된다.

단점: PR 1건당 N행이 추가로 생성되어 webhook 단위 작업이 늘어난다. 라벨은 시간이 지나며 변경되므로 매 이벤트마다 diff/sync 로직을 작성해야 한다.

선택하지 않은 이유: PostgreSQL `TEXT[]` + GIN 인덱스로 라벨별 카운트는 충분히 처리 가능하다. PR/Issue 1건 = 1행 원칙을 유지하면 UPSERT 흐름이 단순하다. 정규화가 필요해지면 별도 ADR로 마이그레이션한다.

### A-10. Snapshot 테이블 없이 실시간 stat 테이블 직접 집계

장점: 테이블 수가 줄어든다. 데이터가 항상 최신이다.

단점: Org/Repo 카드형 지표 한 번 조회에 6~7개 stat 테이블을 join·count해야 한다. 운영진 대시보드 응답이 stat 테이블 행 수에 비례해 점진적으로 느려진다.

선택하지 않은 이유: 카드형 지표는 분당 단위 정밀도가 필요 없고, 매시간 갱신되는 `github_repo_metrics_snapshot` 캐시로 응답을 안정화하는 편이 운영진 UX에 유리하다.

## Consequences

### Positive

- GitHub 계정으로 즉시 로그인 가능하며, 동일 App에서 repository 설치까지 자연스럽게 이어진다.
- 모든 GitHub repository API 호출이 봇 ID(App)로 통일되어 audit이 명확하고 사용자 token 만료에 영향을 받지 않는다.
- Webhook 수신 컨트롤러가 가벼워 GitHub 응답 SLA를 안정적으로 만족한다.
- PR 코멘트 한 줄로 미션 제출이 완료되어 챌린저 UX가 단순해진다. 추후 curriculum 연동 시 수동 제출 절차가 불필요해진다.
- Webhook 이벤트 기반 stat 저장은 추가 API 호출 없이 대부분의 활동 지표를 실시간에 가깝게 유지한다.
- 라벨/Assignee/Milestone/Reviewer 차원이 stat 테이블에 함께 저장되어, 새로운 차원의 카운트를 추가할 때 마이그레이션 없이 쿼리만 수정하면 된다.
- 도메인 이벤트(`GithubMissionSubmittedEvent` 등)가 GitHub 구체 구현과 분리되어 curriculum 측은 GitHub을 알 필요 없이 이벤트만 구독한다.
- `usage_guidance` 메타데이터로 "commit 수 = 기여도"라는 오해를 응답 단계에서 구조적으로 억제한다.
- "원본 보존 원칙"(§12)으로 동일 측정값이 차원별 다른 의미로 노출되어도 단일 stat 테이블/계산식에서 파생되도록 강제하므로, 차원 간 값 불일치가 구조적으로 발생하지 않는다.

### Negative

- GitHub App private key, webhook secret, client_id/client_secret이 환경 자산으로 4개 이상 추가된다. App private key 회전 절차를 운영 가이드에 별도로 마련해야 한다.
- App 권한(scope) 변경 시 기존 Installation 전체에서 재승인이 필요하다. 활동 지표 수집에 필요한 권한(Pull requests RW, Issues RW, Contents RO, Actions RO, Deployments RO, Metadata RO)은 초기 설계에서 보수적으로 확보해야 한다.
- stat 테이블 7종에 webhook 이벤트를 모두 저장하면 행 수가 빠르게 증가한다. 90일 이상 stat 원본 삭제 배치와 인덱스 설계가 필요하다.
- `issue_comment` 기반 미션 제출 파싱은 패턴 오인식(false positive) 위험이 있다. App 멘션 필수화로 완화하지만, 운영자가 잘못 기록된 제출을 취소할 수 있는 admin API가 필요하다.
- `github_repo_metrics_snapshot`은 매시간 UPSERT 시점 ~ 다음 UPSERT 시점 사이에 최대 1시간의 stale window가 존재한다. 즉시 정밀도가 필요한 케이스에는 stat 테이블 직접 쿼리로 우회해야 한다.
- curriculum과의 실제 평가 연동은 본 ADR에서 인터페이스만 합의하므로, 실제 구현은 별도 작업·별도 ADR로 분리된다.
- 중복 지표를 모두 유지(§12)하므로 동일 측정값이 다수 응답 DTO에 등장하며, 응답 어셈블 시점에 source query 결과를 공유 객체로 묶지 않으면 동일 값을 두 번 계산할 수 있다. 이를 방지하기 위해 source 매트릭스 기반 통합 테스트가 필요하다.

### Neutral / Trade-offs

- App JWT/installation token 발급 로직은 이번에 한 번 잘 만들어두면 이후 GitHub 관련 모든 기능에 재사용된다.
- `github_webhook_delivery`와 stat 테이블 7종은 시간이 지나면 행이 누적된다. 보관 정책(stat 90일, 일별 집계 1년)과 정기 삭제 배치가 필요하다.
- "로그인은 했지만 App을 설치하지 않은 사용자" 상태는 정상이다. 개발 파트가 아닌 챌린저(디자인/기획)도 GitHub 로그인을 사용하기 위한 의도된 분리다.
- webhook-only 수집은 최초 설치 이전의 과거 이벤트를 포함하지 않는다. backfill API 호출이 필요하지만, GitHub API rate limit 내에서 순차 처리한다.
- 라벨/Assignee를 `TEXT[]`로 비정규화한 결과, 라벨 이름 변경 시 과거 stat 행을 일괄 갱신해야 라벨 카운트가 정확해진다. 변경 빈도가 낮아 수용 가능한 비용이다.

## Implementation Notes

### 도메인 패키지 구조 (신규)

```
com.umc.product.github/
├── domain/
│   ├── GithubInstallation.java          # installation_id, account_login, owner_member_id
│   ├── GithubRepo.java                  # github_repo_id, full_name, installation_id, last_*_at
│   ├── GithubRepoLink.java              # repo ↔ project/curriculum/discord 채널 매핑
│   ├── GithubWebhookDelivery.java       # X-GitHub-Delivery 멱등성
│   ├── GithubPrStat.java                # PR stat (1 PR = 1 row, UPSERT)
│   ├── GithubIssueStat.java             # Issue stat
│   ├── GithubReviewStat.java            # PR review (state별 row)
│   ├── GithubReviewRequestStat.java     # 리뷰 요청 부하 분산용
│   ├── GithubCommitStat.java            # Commit
│   ├── GithubWorkflowRunStat.java       # CI/CD workflow run
│   ├── GithubDeploymentStat.java        # Deployment
│   ├── GithubRepoMetricsSnapshot.java   # 매시간 캐시 스냅샷
│   ├── GithubMetricsDaily.java          # 일별 집계 스냅샷
│   ├── GithubMissionSubmission.java     # 미션 제출 기록
│   ├── event/
│   │   ├── GithubIssueOpenedEvent.java
│   │   ├── GithubPullRequestOpenedEvent.java
│   │   ├── GithubPullRequestMergedEvent.java
│   │   └── GithubMissionSubmittedEvent.java  # (com.umc.product.common.event 로 이전 예정)
│   └── exception/
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── HandleGithubWebhookUseCase.java
│   │   │   ├── RegisterGithubInstallationUseCase.java
│   │   │   ├── LinkGithubRepoUseCase.java
│   │   │   ├── GetGithubLoginInfoUseCase.java
│   │   │   ├── GetGithubOrgMetricsUseCase.java
│   │   │   ├── GetGithubRepoMetricsUseCase.java
│   │   │   ├── GetGithubCommitMetricsUseCase.java
│   │   │   ├── GetGithubPrFlowMetricsUseCase.java
│   │   │   ├── GetGithubIssueFlowMetricsUseCase.java
│   │   │   ├── GetGithubReviewMetricsUseCase.java
│   │   │   ├── GetGithubCiCdMetricsUseCase.java
│   │   │   ├── GetGithubUserActivityUseCase.java
│   │   │   ├── GetGithubRiskSignalsUseCase.java
│   │   │   └── CancelGithubMissionSubmissionUseCase.java
│   │   └── out/
│   │       ├── LoadGithubInstallationPort.java
│   │       ├── SaveGithubInstallationPort.java
│   │       ├── LoadGithubRepoLinkPort.java
│   │       ├── SaveGithubStatPort.java                # stat 7종 일괄 저장 포트
│   │       ├── LoadGithubStatPort.java                # stat 쿼리 포트
│   │       ├── LoadGithubMetricsSnapshotPort.java
│   │       ├── SaveGithubMetricsSnapshotPort.java
│   │       ├── SaveGithubMissionSubmissionPort.java
│   │       ├── LoadGithubMissionSubmissionPort.java
│   │       ├── ExchangeGithubAccessTokenPort.java
│   │       ├── FetchGithubUserInfoPort.java
│   │       ├── IssueInstallationTokenPort.java
│   │       └── FetchGithubRepoPort.java               # backfill용 API 호출
│   └── service/
│       ├── GithubWebhookCommandService.java           # 서명 검증·멱등 저장·이벤트 분기
│       ├── GithubStatCollectorService.java            # webhook 이벤트 → stat 저장
│       ├── GithubMissionSubmissionService.java        # 미션 제출 파싱·기록
│       ├── GithubInstallationCommandService.java
│       ├── GithubMetricsQueryService.java             # 카드/흐름 지표 조회
│       ├── GithubRiskSignalDetectionService.java      # 위험 신호 탐지(배치)
│       ├── GithubMetricsAggregationService.java       # 야간 집계 + 매시간 snapshot
│       ├── GithubBackfillService.java                 # 신규 설치 시 과거 데이터 적재
│       └── GithubLoginQueryService.java
└── adapter/
    ├── in/
    │   ├── web/                                       # /webhooks/github, /admin/github/*
    │   └── event/                                     # 도메인 이벤트 → Discord 발송 핸들러
    └── out/
        ├── persistence/
        └── external/
            ├── GithubAppProperties.java
            ├── GithubAppJwtFactory.java
            ├── GithubOAuthClient.java
            ├── GithubInstallationClient.java          # installation token, repo API, backfill
            └── GithubWebhookSignatureVerifier.java
```

### 데이터 모델 (Flyway 신규)

**기본 테이블:**

- `github_installation` (id, installation_id UNIQUE, account_login, account_type, owner_member_id NULLABLE, suspended boolean, installed_at, updated_at)
- `github_repo` (id, github_repo_id UNIQUE, full_name, private_repo boolean, default_branch, installation_id FK, last_push_at NULLABLE, last_pr_at NULLABLE, last_issue_at NULLABLE, last_deployment_at NULLABLE)
- `github_repo_link` (id, github_repo_id FK, link_type ENUM('PROJECT','CURRICULUM_WEEK','STANDALONE'), target_id BIGINT NULLABLE, discord_channel_webhook_url, mention_role_id NULLABLE, enabled boolean) — CHECK: `(link_type = 'STANDALONE' AND target_id IS NULL) OR (link_type IN ('PROJECT','CURRICULUM_WEEK') AND target_id IS NOT NULL)`
- `github_webhook_delivery` (id, delivery_id UNIQUE, event_type, action, received_at)

**활동 지표 stat 테이블:**

```sql
-- PR stat (webhook pull_request 이벤트로 UPSERT)
github_pr_stat
( id, installation_id FK, github_repo_id BIGINT,
    pr_number INT, title TEXT,
    state ENUM ('OPEN','CLOSED','MERGED'),
    draft BOOLEAN,
    author_login VARCHAR (100),
    merger_login VARCHAR (100) NULLABLE,
    assignee_logins TEXT [],
    requested_reviewer_logins TEXT [],
    labels TEXT [],
    milestone_title VARCHAR (255) NULLABLE,
    created_at TIMESTAMPTZ,
    first_review_at TIMESTAMPTZ NULLABLE,
    first_approve_at TIMESTAMPTZ NULLABLE,
    merged_at TIMESTAMPTZ NULLABLE,
    closed_at TIMESTAMPTZ NULLABLE,
    additions INT, deletions INT, changed_files INT, commit_count INT,
    has_conflict BOOLEAN,
    last_ci_conclusion ENUM ('SUCCESS','FAILURE','CANCELLED','SKIPPED','PENDING') NULLABLE,
    UNIQUE (installation_id, github_repo_id, pr_number))

-- Issue stat
github_issue_stat
( id, installation_id FK, github_repo_id BIGINT,
    issue_number INT, title TEXT,
    state ENUM ('OPEN','CLOSED'),
    author_login VARCHAR (100),
    assignee_logins TEXT [],
    labels TEXT [],
    milestone_title VARCHAR (255) NULLABLE,
    created_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ NULLABLE,
    UNIQUE (installation_id, github_repo_id, issue_number))

-- PR review stat (state별 row)
github_review_stat
( id, installation_id FK, github_repo_id BIGINT, pr_number INT,
    reviewer_login VARCHAR (100),
    state ENUM ('APPROVED','CHANGES_REQUESTED','COMMENTED','DISMISSED'),
    submitted_at TIMESTAMPTZ,
    UNIQUE (installation_id, github_repo_id, pr_number, reviewer_login, submitted_at))

-- 리뷰 요청 stat (review concentration 위험 신호용)
github_review_request_stat
( id, installation_id FK, github_repo_id BIGINT, pr_number INT,
    reviewer_login VARCHAR (100),
    requested_at TIMESTAMPTZ,
    removed_at TIMESTAMPTZ NULLABLE,
    completed_at TIMESTAMPTZ NULLABLE, -- 첫 리뷰 도달 시점
    UNIQUE (installation_id, github_repo_id, pr_number, reviewer_login, requested_at))

-- Commit stat
github_commit_stat
( id, installation_id FK, github_repo_id BIGINT, sha VARCHAR (40),
    author_login VARCHAR (100),
    committed_at TIMESTAMPTZ,
    branch_name VARCHAR (255),
    additions INT, deletions INT,
    message_convention_valid BOOLEAN,
    UNIQUE (installation_id, github_repo_id, sha))

-- CI/CD workflow run stat
github_workflow_run_stat
( id, installation_id FK, github_repo_id BIGINT,
    workflow_run_id BIGINT UNIQUE,
    workflow_name VARCHAR (255), head_branch VARCHAR (255),
    event_type VARCHAR (50),
    status ENUM ('QUEUED','IN_PROGRESS','COMPLETED'),
    conclusion ENUM ('SUCCESS','FAILURE','CANCELLED','SKIPPED') NULLABLE,
    started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ NULLABLE,
    duration_seconds INT NULLABLE,
    triggered_pr_number INT NULLABLE)

-- 배포 stat
github_deployment_stat
( id, installation_id FK, github_repo_id BIGINT,
    deployment_id BIGINT UNIQUE,
    environment VARCHAR (100),
    status ENUM ('PENDING','SUCCESS','FAILURE','INACTIVE','IN_PROGRESS'),
    created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ)

-- 미션 제출 기록
github_mission_submission
( id, github_user_login VARCHAR (100), member_id BIGINT NULLABLE,
    installation_id FK, github_repo_id BIGINT, pr_number INT,
    comment_id BIGINT UNIQUE, week_number INT,
    submitted_at TIMESTAMPTZ,
    curriculum_status ENUM ('PENDING','SUBMITTED','REJECTED','UNLINKED'),
    created_at TIMESTAMPTZ)

-- Repo 카드형 지표 캐시 (매시간 UPSERT)
github_repo_metrics_snapshot
( id, installation_id FK, github_repo_id BIGINT UNIQUE,
    open_pr_count INT, open_issue_count INT,
    merged_pr_count_total INT, closed_issue_count_total INT,
    commits_last_7d INT, contributors_last_7d INT,
    last_commit_at TIMESTAMPTZ NULLABLE,
    last_pr_at TIMESTAMPTZ NULLABLE,
    last_issue_at TIMESTAMPTZ NULLABLE,
    last_deployment_at TIMESTAMPTZ NULLABLE,
    last_ci_conclusion VARCHAR (20) NULLABLE,
    last_deployment_status VARCHAR (20) NULLABLE,
    snapshot_taken_at TIMESTAMPTZ)

-- 일별 집계 스냅샷 (야간 배치)
github_metrics_daily
( id, installation_id FK, github_repo_id BIGINT NULLABLE,
    metric_date DATE,
    metrics JSONB, -- pr_opened/merged/closed, issue_opened/closed, commits, reviews, deployments
    UNIQUE (installation_id, github_repo_id, metric_date))
```

**인덱스:**

- `github_pr_stat`: `(installation_id, created_at)`, `(installation_id, state)`, GIN(`labels`), GIN(`assignee_logins`), GIN(`requested_reviewer_logins`)
- `github_issue_stat`: `(installation_id, created_at)`, GIN(`labels`), GIN(`assignee_logins`), `(milestone_title)`
- `github_review_stat`: `(installation_id, github_repo_id, pr_number)`, `(reviewer_login, submitted_at)`
- `github_review_request_stat`: `(reviewer_login, requested_at)`, `(installation_id, completed_at)`
- `github_commit_stat`: `(author_login, committed_at)`, `(installation_id, github_repo_id, branch_name)`, `(committed_at)`
- `github_workflow_run_stat`: `(github_repo_id, started_at)`, `(triggered_pr_number)`, `(head_branch, conclusion)`
- `github_deployment_stat`: `(github_repo_id, updated_at)`, `(environment, status)`
- `github_mission_submission`: `(github_user_login)`, `(member_id, week_number)`
- `github_metrics_daily`: `(metric_date)`, `(installation_id, metric_date)`
- `github_repo_metrics_snapshot`: `(installation_id)`
- `github_webhook_delivery`: `(received_at)`

### `member_oauth` 확장

`OAuthProvider` enum에 `GITHUB` 추가. 기존 `member_oauth` 테이블 구조를 그대로 사용한다. GitHub user login을 `provider_id`로, login을 nickname으로 매핑한다. 신규 OAuth 연결 시 `github_mission_submission` 중 `github_user_login=login AND curriculum_status='UNLINKED'` 행을 일괄 업데이트해 백필한다.

### 환경 변수

```yaml
app:
    github:
        app:
            app-id: 123456
            client-id: Iv1.xxxxxxxxxxxx
            client-secret: ${GITHUB_APP_CLIENT_SECRET}
            private-key: ${GITHUB_APP_PRIVATE_KEY}             # PEM, 줄바꿈은 \n으로 인코딩
            webhook-secret: ${GITHUB_APP_WEBHOOK_SECRET}
            bot-login: umc-product[bot]                         # 미션 제출 멘션 탐지용
            setup-url: https://api.umc-product.com/admin/github/installations/setup
        oauth:
            authorize-uri: https://github.com/login/oauth/authorize
            token-uri: https://github.com/login/oauth/access_token
            user-info-uri: https://api.github.com/user
            redirect-uri: https://api.umc-product.com/oauth/github/callback
        metrics:
            snapshot-refresh-cron: "0 5 * * * *"                # 매시간 5분에 repo 스냅샷 갱신
            daily-aggregate-cron: "0 0 3 * * *"                 # 매일 새벽 3시 일별 집계
            risk-signal-check-cron: "0 0 9 * * *"               # 매일 오전 9시 위험 신호 탐지
            stat-retention-days: 90                             # stat 원본 보관 일수
            daily-retention-days: 365                           # 일별 집계 보관 일수
            alert-stale-pr-days: 3                              # 리뷰 없는 Open PR 임계
            alert-stale-issue-days: 7                           # 방치 Open Issue 임계
            alert-inactive-repo-days: 7                         # 미활동 repo 임계
            alert-stale-deployment-days: 14                     # 배포 부재 임계
            alert-ci-failure-streak: 3                          # CI 연속 실패 임계
            alert-reviewer-load-multiplier: 2.0                 # 평균 대비 N배 이상 → 부하 경고
            alert-pr-merge-ratio-min: 0.3                       # 7일간 merge/open 최소 비율
            alert-issue-close-ratio-min: 0.3                    # 7일간 close/open 최소 비율
```

GitHub App 측 설정 페이지에서는 다음 두 URL을 별도로 등록한다.

- **Setup URL**: `app.github.app.setup-url` — 설치 완료 후 이동. `installation_id`, `setup_action` 파라미터.
- **Callback URL**: `app.github.oauth.redirect-uri` — OAuth user-to-server 토큰 교환 진입점. `code`, `state` 파라미터.

### Webhook 처리 흐름

1. `POST /webhooks/github`을 컨트롤러에서 `@RequestBody byte[] rawBody`로 수신한다. Jackson이 InputStream을 소비하기 전에 원본 바이트를 보존해야 HMAC 검증이 깨지지 않는다.
2. `X-Hub-Signature-256` 헤더의 `sha256=…` 부분과 webhook secret으로 HMAC SHA-256을 계산해 `MessageDigest.isEqual`로 상수 시간 비교한다. 실패 시 401, 본문은 로깅하지 않는다.
3. **별도의 짧은 트랜잭션**(`Propagation.REQUIRES_NEW`)에서 `github_webhook_delivery(delivery_id)` UNIQUE INSERT를 시도한다. UNIQUE 위반이면 200으로 즉시 종료.
4. INSERT 성공 후 `X-GitHub-Event` + `payload.action`을 도메인 이벤트로 변환해 `ApplicationEventPublisher.publishEvent(...)`. PR "merged"는 `pull_request` + `action="closed"` + `pull_request.merged=true` 조합으로만 도출된다.
5. 컨트롤러는 200 OK를 즉시 반환한다.
6. `@TransactionalEventListener(phase = AFTER_COMMIT) + @Async("githubWebhookExecutor")` 핸들러가 stat 저장, 미션 제출 파싱, Discord 발송 등 후속 작업을 수행한다.
7. `@EnableAsync`와 전용 `ThreadPoolTaskExecutor`(corePoolSize 4, maxPoolSize 16, queueCapacity 200, `CallerRunsPolicy`)를 명시한다.

### Stat 적재 규칙

| 이벤트 / action                             | 대상 테이블                                     | 동작                                                                                                                          |
|------------------------------------------|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `pull_request` opened/edited/synchronize | `github_pr_stat`                           | UPSERT(라벨/Assignee/Reviewer/Milestone 동기화)                                                                                  |
| `pull_request` review_requested          | `github_review_request_stat`               | INSERT                                                                                                                      |
| `pull_request` review_request_removed    | `github_review_request_stat`               | UPDATE `removed_at`                                                                                                         |
| `pull_request` closed (merged=true)      | `github_pr_stat`                           | UPDATE `state=MERGED, merger_login, merged_at`                                                                              |
| `pull_request` closed (merged=false)     | `github_pr_stat`                           | UPDATE `state=CLOSED, closed_at`                                                                                            |
| `pull_request_review` submitted          | `github_review_stat`                       | INSERT, PR stat의 `first_review_at`/`first_approve_at` UPDATE, 해당 reviewer의 `github_review_request_stat.completed_at` UPDATE |
| `issues` opened/edited/labeled           | `github_issue_stat`                        | UPSERT                                                                                                                      |
| `issues` closed                          | `github_issue_stat`                        | UPDATE `state=CLOSED, closed_at`                                                                                            |
| `issue_comment` created                  | 미션 제출 파싱 + (집계용) issue stat의 comment_count |
| `push`                                   | `github_commit_stat`                       | INSERT(commits[]) + `github_repo.last_push_at` UPDATE                                                                       |
| `workflow_run` completed                 | `github_workflow_run_stat`                 | UPSERT, PR연결 시 `github_pr_stat.last_ci_conclusion` UPDATE                                                                   |
| `deployment_status`                      | `github_deployment_stat`                   | UPSERT + `github_repo.last_deployment_at` UPDATE                                                                            |

### PR 코멘트 미션 제출 파싱

- 탐지 정규식: `@{bot-login}\s+(\d{1,2})주차\s*미션\s*제출` (한국어 고정).
- `issue_comment` 이벤트에서 `issue.pull_request` 필드 존재 여부로 PR 코멘트 여부를 판별한다.
- 같은 `comment_id`로 중복 이벤트가 들어오는 경우 `github_mission_submission(comment_id) UNIQUE` 제약으로 멱등성을 보장한다.
- `member_oauth(provider=GITHUB, provider_id=login)` 조회 결과가 없으면 `curriculum_status=UNLINKED`로 적재한다. 추후 동일 GitHub login으로 OAuth 가입이 발생하면 `OAuthLoginCommandService` 후처리에서 일괄 백필 + `GithubMissionSubmittedEvent` 재발행.
- 운영자가 잘못된 제출을 취소할 수 있는 `DELETE /admin/github/mission-submissions/{id}` API를 제공한다. 실제 삭제 대신 `curriculum_status=REJECTED`로 전환한다.
- 미션 제출 확인 응답으로 Discord DM이나 PR 코멘트를 다는 것은 본 ADR 범위 밖으로 후속 작업에서 결정한다.

### App JWT / Installation Token 발급

- App JWT: 헤더 `RS256`, 페이로드 `iat`(현재-60s), `exp`(현재+9m), `iss=app_id`.
- Installation token: `POST /app/installations/{installation_id}/access_tokens`. 응답 `token`은 1시간 유효, **Caffeine 캐시에 만료 5분 전까지만 보관**. DB 영속화 금지.

### 활동 지표 쿼리 API 응답 구조

각 endpoint는 `data` + `meta` 구조로 응답하며, `meta.usage_guidance`에 가이드 문구가 포함된다.

| Endpoint                                                                             | 주요 응답 필드                                                                                                                                                                                                                     |
|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET /admin/github/metrics/orgs/{installationId}`                                    | `linked_repo_count`, `active_repo_count`, `active_repo_count_7d`, `commits_7d`, `pr_opened_7d`, `pr_merged_7d`, `open_pr_count`, `open_issue_count`, `awaiting_review_pr_count`, `failing_ci_pr_count`, `last_deployment_at` |
| `GET /admin/github/metrics/repos/{repoId}`                                           | repo 카드 필드(snapshot 기반) + 7d Commit/Contributor + 최근 CI/배포 상태                                                                                                                                                                |
| `GET /admin/github/metrics/repos/{repoId}/commits?from&to&groupBy=author/branch/day` | 그룹별 commit 카운트 + 컨벤션 위반 수 + `meta.usage_guidance`                                                                                                                                                                            |
| `GET /admin/github/metrics/repos/{repoId}/prs?from&to`                               | 기본 카운트(state/draft/conflict/오래열림/라벨/Assignee/Reviewer) + 흐름 지표(평균 처리/리뷰 대기/merge 시간, 리뷰 없이 merge 수, CI 실패 PR 수) + 크기 분포                                                                                                      |
| `GET /admin/github/metrics/repos/{repoId}/issues?from&to`                            | 평균 처리 시간, 7일 이상 open, 담당자 없는 비율, Bug 비율(라벨 기준), close vs new 비율, milestone 완료율                                                                                                                                               |
| `GET /admin/github/metrics/repos/{repoId}/reviews?from&to`                           | 요청/완료/Approve/Changes/Comment 수, 리뷰어별·작성자별 카운트, 평균 첫 리뷰 응답/평균 approve 시간, 리뷰 없이 머지/자가머지 수                                                                                                                                    |
| `GET /admin/github/metrics/repos/{repoId}/ci-cd?from&to`                             | workflow 실행/성공/실패 수, 평균 실행 시간, 실패 목록, 배포 횟수/실패율/마지막 배포 시각                                                                                                                                                                    |
| `GET /admin/github/metrics/users/{memberIdOrLogin}?from&to`                          | Commit/PR 생성/PR Merge/Issue 생성/Issue Close/Review/Comment, 최근 활동, 참여 repo 수, 담당 Issue 수, 리뷰 대기 PR 수                                                                                                                          |
| `GET /admin/github/metrics/risk-signals?installationId&repoId`                       | 위험 신호 목록 + 신호별 임계값/현재 값                                                                                                                                                                                                      |

`from`/`to` 파라미터는 ISO8601, 누락 시 기본값은 최근 7일.

### 지표 source 매트릭스 (중복 지표의 단일 출처 강제)

Decision §12에서 합의한 "원본 보존 원칙"을 구현 단계에서 강제하기 위해, 차원이 달라도 동일 측정값을 노출하는 응답 필드는 반드시 같은 stat 테이블/계산식에서 파생되어야 한다. 응답 DTO 어셈블 시점에 source query 결과를 공유 객체로 만들고 차원별 응답에 주입한다.

| 응답 필드(차원 별)                                                                       | 단일 source                                                                                                    | 계산식 / 비고                                         |
|-----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| Org `last_deployment_at` / Repo `last_deployment_at` / CI/CD `last_deployment_at` | `github_deployment_stat.updated_at` (status=SUCCESS 최신)                                                      | 동일 쿼리 결과를 차원별로 라벨만 다르게 노출.                       |
| Repo `last_ci_conclusion` / CI/CD `recent_repo_ci_status`                         | `github_workflow_run_stat`(repo·default branch 최신 1건)                                                        | 동일 source.                                       |
| PR 흐름 `avg_review_wait_seconds` / Review `avg_first_review_response_seconds`      | `AVG(github_pr_stat.first_review_at - created_at)`                                                           | 동일 계산식. PR/Review 응답에서 동일 값 노출.                  |
| PR 흐름 `merged_without_review_count` / Review `merged_without_review_count`        | `github_pr_stat WHERE state=MERGED AND first_review_at IS NULL`                                              | 동일 source.                                       |
| Org/Repo/개인 `awaiting_review_pr_count`                                            | `github_pr_stat WHERE state=OPEN AND first_review_at IS NULL`                                                | 집계 단위만 다름(installation / repo / reviewer_login). |
| Org/Repo/PR 기본 `open_pr_count`, `open_issue_count`                                | `github_pr_stat`, `github_issue_stat` (state=OPEN)                                                           | 집계 단위 다름, 동일 source.                             |
| PR 기본 `stale_pr_count` / PR 흐름 `long_unreviewed_pr_count` / 위험 신호 `리뷰 없는 Open PR` | `github_pr_stat WHERE state=OPEN AND first_review_at IS NULL AND now() - created_at > {alert-stale-pr-days}` | 환경변수 `alert-stale-pr-days` 단일 임계값으로 통일.          |
| Commit 차원 `inactive_repo_list` / 위험 신호 `미활동 Repository`                           | `github_repo.last_push_at < now() - {alert-inactive-repo-days}`                                              | 환경변수 `alert-inactive-repo-days` 단일 임계값으로 통일.     |

이 매트릭스는 응답 DTO 어셈블 단계의 통합 테스트에서 검증한다(동일 측정값을 가진 두 응답 필드가 다른 값을 갖는 케이스는 회귀 테스트로 차단).

### 위험 신호 탐지 및 Discord 알림

야간 스케줄러(`risk-signal-check-cron`)가 다음 신호를 탐지해 `github_repo_link`에 설정된 Discord 채널(또는 운영진 전용 채널)로 발송한다.

| 신호                 | 데이터 소스                                                                              | 임계값 환경변수                         |
|--------------------|-------------------------------------------------------------------------------------|----------------------------------|
| 미활동 Repository     | `github_commit_stat`, `github_repo.last_push_at`                                    | `alert-inactive-repo-days`       |
| 리뷰 없는 Open PR      | `github_pr_stat.first_review_at IS NULL`                                            | `alert-stale-pr-days`            |
| 방치된 Open Issue     | `github_issue_stat.state='OPEN'`                                                    | `alert-stale-issue-days`         |
| 담당자 없는 Issue 다수    | `github_issue_stat.assignee_logins = '{}'` 비율                                       | (별도 임계값 없음, 비율 보고)               |
| CI 반복 실패           | `github_workflow_run_stat`(repo별 최근 N건 conclusion)                                  | `alert-ci-failure-streak`        |
| main/develop 빌드 실패 | `github_workflow_run_stat(head_branch IN ('main','develop'), conclusion='FAILURE')` | (즉시 보고)                          |
| 리뷰어 부하 집중          | `github_review_request_stat`(reviewer_login별 7일 카운트)                                | `alert-reviewer-load-multiplier` |
| PR-vs-merge 불균형    | `github_pr_stat`(7일 opened vs merged)                                               | `alert-pr-merge-ratio-min`       |
| Issue-vs-close 불균형 | `github_issue_stat`(7일 opened vs closed)                                            | `alert-issue-close-ratio-min`    |
| 장기 배포 부재           | `github_deployment_stat.updated_at` 또는 `github_repo.last_deployment_at`             | `alert-stale-deployment-days`    |

### 멀티 인스턴스 운영 시 한계

`ApplicationEventPublisher`는 in-process 이벤트 버스다. webhook을 수신한 인스턴스에서만 핸들러가 동작한다. Discord 발송과 stat 저장은 webhook 수신 인스턴스에서 처리되므로 영향 없다. 야간 배치/매시간 스냅샷은 ShedLock으로 한 번만 실행되도록 보장한다. 추후 curriculum 평가 작업이 다른 인스턴스에서 필요해지면 외부 메시지 브로커로 격상한다.

### 보안 주의사항

- App private key는 KMS 또는 Spring `Jasypt` 대칭 암호화 후 환경변수로 주입한다. 평문 커밋 금지.
- webhook endpoint는 `permitAll`로 열되, 서명 검증으로만 통제한다. CORS는 차단.
- user access token은 GitHub API 호출 직후 폐기. 로그/응답 어디에도 출력 금지.
- `github_mission_submission.member_id`는 NULLABLE이므로, GitHub 계정과 서비스 회원 연결이 없는 제출 기록이 존재할 수 있다. 이 상태(`UNLINKED`)의 데이터를 curriculum 도메인으로 연동하면 안 된다.
- 활동 지표 응답에 GitHub login은 포함되지만, **개인 평가 점수로 직접 사용할 수 없다는 가이드 문구**가 응답 메타에 포함되도록 강제한다.

## Implementation Plan (Commit 단위)

각 커밋은 독립적으로 빌드/테스트가 통과해야 하며, Conventional Commits 규칙을 따른다. 6개 Phase × 30개 commit으로 분할한다.

### Phase 1 — GitHub OAuth 로그인 (4 commits)

1. `chore: github 도메인 패키지와 GithubAppProperties 스켈레톤 추가`
    - `com.umc.product.github` 하위 빈 패키지 + `GithubAppProperties`(record) + `application*.yml`에 `app.github.*` 키 추가.
    - Setup URL과 OAuth Callback URL을 분리 등록한다.

2. `feat: OAuthProvider enum에 GITHUB 추가`
    - `OAuthProvider.from`에 `"github"` 케이스 추가.
    - GitHub용 검증은 별도 어댑터(`GithubUserInfoClient`)로 분리해 다음 커밋에서 도입한다.

3. `feat: GitHub OAuth user-to-server 토큰 교환 어댑터 구현`
    - `GithubOAuthClient`: `code` → `access_token` 교환(`Accept: application/json` 명시, `scope` 파라미터 없음).
    - `GithubUserInfoClient`: `/user`, `/user/emails`로 GitHub user id/login/primary email 조회.
    - 단위 테스트: `MockRestServiceServer`로 200/4xx 케이스, `403` 분기 포함.

4. `feat: GitHub OAuth 로그인 유즈케이스 통합`
    - `OAuthLoginCommandService`에서 GITHUB 분기 추가. code → user info → `member_oauth` 조회/생성 → 서비스 토큰 발급. user access token은 메서드 종료 시 폐기.
    - 동일 GitHub login의 `github_mission_submission(curriculum_status=UNLINKED)` 행을 조회해 `PENDING`으로 백필 + `GithubMissionSubmittedEvent` 재발행.
    - 테스트: 신규 가입/기존 회원 로그인/UNLINKED 백필 세 시나리오 한국어 `@DisplayName`.

### Phase 2 — GitHub App Installation 수용 (5 commits)

5. `feat: github 통합 기본 테이블 마이그레이션 작성`
    - `github_installation`, `github_repo`, `github_repo_link`, `github_webhook_delivery` Flyway 작성.

6. `feat: github 활동 지표 stat·snapshot 테이블 마이그레이션 작성`
    - stat 테이블 7종(`github_pr_stat`, `github_issue_stat`, `github_review_stat`, `github_review_request_stat`, `github_commit_stat`, `github_workflow_run_stat`, `github_deployment_stat`) + `github_mission_submission` + `github_repo_metrics_snapshot` + `github_metrics_daily` 생성.
    - 인덱스(GIN 포함) 한 번에 정의.

7. `feat: GithubInstallation/GithubRepo 도메인 및 Persistence Adapter 구현`
    - 엔티티/Builder/도메인 메서드(`suspend`, `unsuspend`, `assignOwner`, `touchLastPushAt` 등).
    - JPA Repository와 Persistence Adapter, Save/Load Port.

8. `feat: GitHub App JWT 생성 및 installation access token 발급기 구현`
    - `GithubAppJwtFactory`: RS256 서명, `iat`(now-60s)/`exp`(now+9m)/`iss`(app_id).
    - `IssueInstallationTokenPort` + `GithubInstallationClient`: Caffeine 캐시(만료 5분 전 폐기). DB 영속화 금지.
    - 단위 테스트: 만료 직전 캐시 hit, 만료 후 재발급, 401 응답 시 캐시 invalidate.

9. `feat: GitHub App Setup URL 콜백과 OAuth Callback 분리 구현`
    - `GET /admin/github/installations/setup`: `installation_id`, `setup_action` 처리.
    - `GET /oauth/github/callback`: OAuth user-to-server 토큰 교환 진입점.
    - 두 endpoint를 하나의 컨트롤러에 합치지 않는다.

### Phase 3 — Webhook 수신 및 활동 이벤트 수집 (10 commits)

10. `chore: webhook 보안 인프라 및 Async Executor 구성`
    - Spring Security에 `/webhooks/github` `permitAll` 규칙 추가.
    - `@EnableAsync` + `ThreadPoolTaskExecutor("githubWebhookExecutor")` 빈(corePoolSize 4, maxPoolSize 16, queueCapacity 200, `CallerRunsPolicy`).

11. `feat: GitHub webhook 서명 검증 및 멱등성 저장기 구현`
    - `GithubWebhookSignatureVerifier`: HMAC-SHA256, `MessageDigest.isEqual` 상수 시간 비교.
    - `GithubWebhookDeliveryService.recordIfAbsent(deliveryId)`: `Propagation.REQUIRES_NEW` 트랜잭션 UNIQUE INSERT.

12. `feat: POST /webhooks/github 수신 컨트롤러와 도메인 이벤트 발행 구현`
    - `@RequestBody byte[] rawBody` 수신. 서명 검증 → `recordIfAbsent` → 도메인 이벤트 발행 → 200 OK.
    - `GithubWebhookEventConverter`가 이벤트 타입 + action 분기. PR merged는 `closed+merged=true` 조합.

13. `feat: installation·installation_repositories 이벤트 → installation/repo 동기화`
    - `installation` event(created/deleted/suspend/unsuspend) → `github_installation` UPSERT/상태 전환.
    - `installation_repositories`(added/removed) → `github_repo` 동기화.

14. `feat: pull_request 이벤트 → PR stat UPSERT (라벨/Assignee/Reviewer/Milestone 포함)`
    - `GithubStatCollectorService`에서 `github_pr_stat` UPSERT. `assignee_logins`/`requested_reviewer_logins`/`labels`/`milestone_title` 동기화.
    - `closed+merged=true`일 때 `merger_login`/`merged_at` 갱신.

15. `feat: pull_request review_requested/removed → review_request stat 적재`
    - `github_review_request_stat` INSERT/UPDATE. 리뷰어 부하 분산 분석 기반 데이터.

16. `feat: pull_request_review 이벤트 → review stat 저장 + first_review/approve 갱신`
    - `github_review_stat` INSERT, PR stat의 `first_review_at`/`first_approve_at` 조건부 UPDATE.
    - 동일 reviewer의 `github_review_request_stat.completed_at` 갱신(첫 리뷰 도달 시각).

17. `feat: issues + issue_comment 이벤트 → issue stat 저장`
    - Issue stat UPSERT(open/close 전환, labels/assignees/milestone 동기화).
    - `issue_comment created`는 `github_issue_stat`의 코멘트 카운트와 후속 미션 제출 파서로 분기.

18. `feat: push 이벤트 → commit stat 저장 (커밋 컨벤션 검증 포함)`
    - push payload의 `commits[]` 순회, `github_commit_stat`에 INSERT. Conventional Commits 정규식(`^(feat|fix|chore|docs|test|refactor|style|perf)(\(.+\))?: .+`)으로 `message_convention_valid` 판별.
    - `github_repo.last_push_at` 동기화.

19. `feat: workflow_run + deployment_status 이벤트 → CI/CD stat 저장`
    - `github_workflow_run_stat` UPSERT(completed 시 conclusion, duration 갱신). 연결 PR이 있으면 `github_pr_stat.last_ci_conclusion` 동기화.
    - `github_deployment_stat` UPSERT, `github_repo.last_deployment_at` 갱신.

### Phase 4 — Discord 통지·정리 배치·미션 제출 파서 (5 commits)

20. `feat: github 이벤트 → Discord 통지 핸들러 구현`
    - `@TransactionalEventListener(AFTER_COMMIT) + @Async` 핸들러에서 `github_repo_link` 조회 → Discord embed 생성 → Discord 발송 포트 호출.
    - embed 포맷: `[{repo}] PR opened: {title}\nby {actor}\n{html_url}` 등 이벤트별 한국어 라벨.

21. `chore: stat 테이블 보관 정리 스케줄러 추가`
    - `@Scheduled(cron = "0 0 4 * * *")`로 90일 이상 stat 7종 batch DELETE.
    - `github_webhook_delivery` 90일 이상 건도 함께 삭제.
    - ShedLock으로 멀티 인스턴스에서 한 번만 실행.

22. `feat: GithubMissionSubmission 도메인 및 Persistence Adapter 구현`
    - `GithubMissionSubmission` 엔티티, Save/Load Port, Persistence Adapter.
    - `comment_id` UNIQUE 제약으로 중복 제출 방지.

23. `feat: issue_comment 이벤트에서 미션 제출 패턴 파싱 + member 매핑`
    - `GithubMissionSubmissionService.parseAndRecord(payload)`: 정규식으로 week_number 추출 → `member_oauth` lookup → `github_mission_submission` INSERT.
    - 회원 미연결 시 `UNLINKED` 상태로 저장.

24. `feat: 미션 제출 운영자 취소 API + GithubMissionSubmittedEvent 발행`
    - `DELETE /admin/github/mission-submissions/{id}`: 상태를 `REJECTED`로 전환(실제 삭제 아님).
    - 회원 연결된 제출 건은 `GithubMissionSubmittedEvent(memberId, weekNumber, prUrl, submittedAt)` 발행. 이벤트 정의는 `com.umc.product.common.event`에 위치.

### Phase 5 — 활동 지표 조회 API·집계 배치·위험 신호 (10 commits)

25. `feat: GithubMetricsAggregationService - github_repo_metrics_snapshot 매시간 갱신`
    - `snapshot-refresh-cron`로 각 repo의 카드 지표(open PR/Issue, 7일 commit/contributor, 최근 CI/배포 상태) UPSERT.
    - ShedLock 적용.

26. `feat: GithubMetricsAggregationService - github_metrics_daily 일별 집계 배치`
    - `daily-aggregate-cron`에 전일 stat 데이터를 JSONB로 집계해 UPSERT. `daily-retention-days` 기준 보관.

27. `feat: GetGithubOrgMetricsUseCase + GetGithubRepoMetricsUseCase 구현`
    - snapshot 우선 조회, 미존재 시 fallback 직접 집계.
    - 응답 DTO: `GithubOrgMetricsInfo`, `GithubRepoMetricsInfo` (Info record).

28. `feat: GetGithubCommitMetricsUseCase + commits API 구현`
    - 기간/branch/사용자별 그룹 집계, 컨벤션 위반 카운트.
    - 응답 `meta.usage_guidance` 문구 강제 포함.

29. `feat: GetGithubPrFlowMetricsUseCase + prs API 구현`
    - 기본 카운트(state/draft/conflict/오래열림/라벨/Assignee/Reviewer) + 흐름(평균 처리/리뷰 대기/merge 시간, 리뷰 없이 merge 수, CI 실패 PR 수) + 크기 분포.
    - GIN 인덱스 활용 쿼리는 `@Query` JPQL/Native + QueryDSL 조합.

30. `feat: GetGithubIssueFlowMetricsUseCase + issues API 구현`
    - 평균 처리 시간, 7일 이상 open, 담당자 없는 비율, Bug 라벨 비율, close vs new 비율, milestone 완료율.

31. `feat: GetGithubReviewMetricsUseCase + reviews API 구현`
    - 요청/완료/Approve/Changes/Comment 수, 리뷰어별·작성자별 카운트, 평균 첫 리뷰 응답/평균 approve 시간, 리뷰 없이 머지 수, 자가머지 PR 수(`author_login = merger_login`).

32. `feat: GetGithubCiCdMetricsUseCase + ci-cd API 구현`
    - workflow 실행/성공/실패 수, 평균 실행 시간, 실패 목록, 배포 횟수/실패율/마지막 배포 시각.

33. `feat: GetGithubUserActivityUseCase + users API 구현`
    - Commit/PR 생성/PR Merge/Issue 생성/Issue Close/Review/Comment, 최근 활동, 참여 repo 수, 담당 Issue 수, 리뷰 대기 PR 수.
    - `meta.usage_guidance` 강제 포함.

34. `feat: GithubRiskSignalDetectionService + risk-signals API + Discord 알림`
    - 10종 위험 신호 탐지(미활동 repo / 리뷰 없는 PR / 방치 Issue / 담당자 없는 Issue 다수 / CI 반복 실패 / main·develop 빌드 실패 / 리뷰어 부하 집중 / PR-vs-merge 불균형 / Issue-vs-close 불균형 / 장기 배포 부재).
    - `risk-signal-check-cron`으로 야간 배치 발송, `GET /admin/github/metrics/risk-signals`로 즉시 조회.

### Phase 6 — Backfill·이벤트 정리·운영 가이드 (3 commits)

35. `feat: 신규 설치 시 과거 데이터 backfill 구현`
    - `installation` 이벤트(created) 수신 시, 설치된 각 repo에 대해 GitHub REST API로 최근 PR/Issue/Commit/Workflow/Deployment 목록 조회 후 stat 테이블에 backfill.
    - rate limit 대응: `Thread.sleep` + retry backoff. 별도 `GithubBackfillService`로 격리.

36. `refactor: github 도메인 이벤트를 공용 event 패키지로 이전`
    - `GithubIssueOpenedEvent`, `GithubPullRequestMergedEvent`, `GithubMissionSubmittedEvent`를 `com.umc.product.common.event`로 이전.
    - 페이로드는 GitHub-특정 raw payload가 아닌 일반화된 식별자만 포함.
    - curriculum 도메인은 일반 이벤트 타입에만 의존. `curriculum → github` 직접 의존 제거.

37. `docs: github 통합 운영 가이드 작성`
    - `docs/guides/github-app-integration.md`: App 생성/권한(Pull requests RW, Issues RW, Contents RO, Actions RO, Deployments RO, Metadata RO) 설정, Setup URL·Callback URL 등록, 설치 흐름, repo 매핑, 미션 제출 패턴 설정, webhook secret·private key 회전 절차, 위험 신호 임계값 조정, "commit 수치는 평가 지표가 아님" 운영 원칙, 장애 대응 체크리스트.

> curriculum 측의 실제 평가 로직(PR merge → 미션 완료 처리)은 본 ADR이 정의한 도메인 이벤트를 구독하는 별도 PR로 이어가며, 그 결정은 별도 ADR에서 다룬다.

## References

- 관련 ADR
    - [ADR-001: Apple 로그인 ClientType 라우팅](001-apple-signin-client-type-routing.md) — `*OAuthProperties` 패턴, `OAuthProvider` 분기 패턴 참조
- 기존 코드
    - [OAuthProvider](../../src/main/java/com/umc/product/common/domain/enums/OAuthProvider.java)
    - [DiscordWebhookAdapter](../../src/main/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapter.java)
    - [Curriculum 도메인](../../src/main/java/com/umc/product/curriculum) — 향후 GitHub 이벤트를 구독할 후보
- GitHub 공식 문서
    - [About creating GitHub Apps](https://docs.github.com/en/apps/creating-github-apps/about-creating-github-apps)
    - [Authenticating as a GitHub App](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/authenticating-as-a-github-app-installation)
    - [Generating a user access token for a GitHub App](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-a-user-access-token-for-a-github-app)
    - [Webhook events: pull_request](https://docs.github.com/en/webhooks/webhook-events-and-payloads#pull_request)
    - [Webhook events: pull_request_review](https://docs.github.com/en/webhooks/webhook-events-and-payloads#pull_request_review)
        - [Webhook events: issues](https://docs.github.com/en/webhooks/webhook-events-and-payloads#issues)
    - [Webhook events: issue_comment](https://docs.github.com/en/webhooks/webhook-events-and-payloads#issue_comment)
    - [Webhook events: workflow_run](https://docs.github.com/en/webhooks/webhook-events-and-payloads#workflow_run)
    - [Webhook events: deployment_status](https://docs.github.com/en/webhooks/webhook-events-and-payloads#deployment_status)
        - [Validating webhook deliveries](https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries)
    - [GitHub REST API rate limits](https://docs.github.com/en/rest/overview/rate-limits-for-the-rest-api)
