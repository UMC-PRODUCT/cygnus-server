# UMC PRODUCT API 엔드포인트 정리 분석

> 작성일: 2026-05-06
> 갱신일: 2026-07-17 — 커뮤니티 Trophy API 제거 및 WeeklyBestWorkbook 단일 기준 반영
> 대상: `src/main/java/com/umc/product/**/adapter/in/web/**/*Controller.java` 전수 조사
> 목적: 중복되거나 존재할 이유가 없는 API를 식별하고, 사유와 함께 정리 액션을 제안

---

## 0. 판단 기준

| 등급             | 기준                                                                        | 처리 방향                |
|----------------|---------------------------------------------------------------------------|----------------------|
| 🔴 A. 제거 권고    | 미구현(`NotImplementedException`), `@Deprecated(forRemoval=true)`, 운영상 의미 없음 | 컨트롤러 단위 또는 메서드 단위 삭제 |
| 🟠 B. 통합 권고    | 동일 도메인의 동일 자원에 대해 컨트롤러가 의미 없이 쪼개짐, 또는 동일 결과를 다른 경로로 두 번 노출                | 단일 컨트롤러로 합치거나 한쪽 폐기  |
| 🟡 C. 의미 정정    | REST 메서드/명명 오용, 클라이언트가 둘 중 하나를 골라야 하는 사실상 동일 API                          | 쓰기/읽기 시멘틱 재정의        |
| 🟢 D. 위치/명명 오류 | 코드 자체는 필요하지만 위치/이름이 잘못됨                                                   | 리네임/이동               |
| ⚠️ E. 운영 관점 메모 | 제거 대상은 아니나 운영/보안상 점검이 필요                                                  | 빌드/배포 정책 검토          |

---

## 1. 🔴 즉시 제거 / 비활성화 권고

### 1-1. Curriculum v2 워크북 관련 엔드포인트 — 24개 전체

대상 디렉터리: [src/main/java/com/umc/product/curriculum/adapter/in/web/v2/](../../src/main/java/com/umc/product/curriculum/adapter/in/web/v2/)

| 컨트롤러                                                                                                                                                                    | 미구현 메서드 수 |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| [WorkbookQueryV2Controller.java](../../src/main/java/com/umc/product/curriculum/adapter/in/web/v2/WorkbookQueryV2Controller.java)                                       | 3         |
| [ChallengerWorkbookCommandV2Controller.java](../../src/main/java/com/umc/product/curriculum/adapter/in/web/v2/ChallengerWorkbookCommandV2Controller.java)               | 7         |
| [ChallengerWorkbookMissionCommandV2Controller.java](../../src/main/java/com/umc/product/curriculum/adapter/in/web/v2/ChallengerWorkbookMissionCommandV2Controller.java) | 6         |
| [OriginalWorkbookCommandV2Controller.java](../../src/main/java/com/umc/product/curriculum/adapter/in/web/v2/OriginalWorkbookCommandV2Controller.java)                   | 5         |
| [OriginalWorkbookMissionCommandV2Controller.java](../../src/main/java/com/umc/product/curriculum/adapter/in/web/v2/OriginalWorkbookMissionCommandV2Controller.java)     | 3         |

**사유**

- 모든 핸들러가 `throw new NotImplementedException();`만 수행함.
- Swagger 명세에는 정상 API처럼 노출되어 클라이언트가 잘못된 가정으로 통합 작업을 시작할 위험.
- 베스트 워크북 책임은 `curriculum` 도메인의 `WeeklyBestWorkbook`으로 일원화한다. 과거 커뮤니티 모델과의 중복은 릴리스 범위에서 제거한다.

**액션**: 인터페이스 설계가 동결될 때까지 컨트롤러 자체를 비공개(예: 전용 Profile 분리, 또는 단순 삭제 후 PR로 부활) 처리.

### 1-2. `@Deprecated(forRemoval = true)` 메서드 — 3개

| 위치                                                                                                                                             | 경로                                     | 비고                                                                |
|------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------|-------------------------------------------------------------------|
| [ChallengerSearchController.java:72-86](../../src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerSearchController.java#L72-L86) | `GET /api/v1/challenger/search/global` | description에서 `search/cursor` / `search/offset`로 대체를 명시           |
| [SchoolQueryController.java:28-32](../../src/main/java/com/umc/product/organization/adapter/in/web/SchoolQueryController.java#L28-L32)         | `GET /api/v1/schools/link/{schoolId}`  | `since="v2.0.0", forRemoval=true`. 대체 경로는 `/{schoolId}` 상세 조회로 충분 |
| [SchoolQueryController.java:55-60](../../src/main/java/com/umc/product/organization/adapter/in/web/SchoolQueryController.java#L55-L60)         | `GET /api/v1/schools/unassigned`       | `since="v2.0.0", forRemoval=true`                                 |

**사유**: 명시적으로 `forRemoval=true`인 채 잔존. 호출 로그가 충분 기간 0이면 즉시 제거. 적어도 Swagger에서 숨김 처리(`@Hidden`).

### 1-3. `@Deprecated`만 표시된 메서드 — 1개

- [ChallengerRoleController](../../src/main/java/com/umc/product/authorization/adapter/in/web/ChallengerRoleController.java) `GET /api/v1/authorization/challenger-role/{challengerRoleId}`
- `[STAFF-101]`이지만 `@Deprecated`. 운영진 기록 단건 조회는 `ChallengerRecord` 또는 `ResourcePermission` API로 갈음 가능해 보임. 사용처 0이면 제거.

---

## 2. 🟠 중복 / 통합 권고

### 2-1. 과거 베스트 워크북 이중 모델링 정리 (역사적 기록)

과거에는 커뮤니티의 Trophy 모델과 커리큘럼의 `WeeklyBestWorkbook`이 모두 베스트 워크북을 표현하려는 중복 상태였다. 2026-07-17 릴리스 범위에서는 커뮤니티 Trophy API를 제거하고, `WeeklyBestWorkbook`을 베스트 워크북 선정 결과의 유일한 SSoT로 유지한다. 이 절의 Trophy 언급은 과거 구조를 설명하기 위한 기록이며 현재 API 목록이 아니다.

### 2-2. Notice 컨트롤러 4-way 분할

| 컨트롤러                                                                                                                             | 엔드포인트 수 | 종류                                 |
|----------------------------------------------------------------------------------------------------------------------------------|---------|------------------------------------|
| [NoticeCommandController.java](../../src/main/java/com/umc/product/notice/adapter/in/web/NoticeCommandController.java)           | 5       | Command                            |
| [NoticeQueryController.java](../../src/main/java/com/umc/product/notice/adapter/in/web/NoticeQueryController.java)               | 5       | Query                              |
| [NoticeContentController.java](../../src/main/java/com/umc/product/notice/adapter/in/web/NoticeContentController.java)           | 6       | Command (image/link/vote 추가/교체/삭제) |
| [NoticeVoteResponseController.java](../../src/main/java/com/umc/product/notice/adapter/in/web/NoticeVoteResponseController.java) | 2       | Command (vote response)            |

**사유**: CQRS 분리(Command/Query)는 정당하지만 `NoticeContentController`와 `NoticeVoteResponseController`는 모두 **Command**입니다. 단일 자원(`/notices/{noticeId}`) 하위 내용 변경을 두 컨트롤러로 다시 쪼개는 것은 CLAUDE.md의 "UseCase 단위 분리" 원칙과는 무관한, 사실상 **파일 크기 줄이기 목적의 분할**로 보임.

**액션**: `NoticeCommandController` 단일 컨트롤러로 통합 권고.

### 2-3. `/notices/{noticeId}/read-statics` vs `/read-status`

- [NoticeQueryController.java:113](../../src/main/java/com/umc/product/notice/adapter/in/web/NoticeQueryController.java#L113) `read-statics` — 통계 요약 (집계 수치)
- [NoticeQueryController.java:128](../../src/main/java/com/umc/product/notice/adapter/in/web/NoticeQueryController.java#L128) `read-status` — 회원별 현황 (커서 기반 목록)

**사유**: 의미는 다르지만 **`statics`는 영문 오타(`statistics`의 잘못)** 이며, 두 경로의 차이가 클라이언트 입장에서 직관적이지 않음.

**액션**: `/{noticeId}/read-statistics`(요약) + `/{noticeId}/reads`(목록) 식으로 분리 명명을 정정.

### 2-4. Challenger 검색의 Cursor/Offset 동시 노출

- [ChallengerSearchController.java:39](../../src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerSearchController.java#L39) `GET search/cursor`
- [ChallengerSearchController.java:53](../../src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerSearchController.java#L53) `GET search/offset`

**사유**: 동일 검색 조건에 대해 **페이지네이션 전략을 클라이언트가 골라야 함**. 일반적으로 한 자원에 대해 한 가지 페이지네이션만 정식 지원하는 것이 운영 부담을 줄임. 현재 둘 다 구현되어 있고 문서화 비용이 두 배.

**액션**: 사용 통계로 한쪽으로 일원화 권고(목록/검색은 cursor가 일반적).

### 2-5. Gisu 조회의 `GET /` vs `GET /all`

- [GisuQueryController.java:34](../../src/main/java/com/umc/product/organization/adapter/in/web/GisuQueryController.java#L34) `GET /` — Pageable + `GisuResponse`
- [GisuQueryController.java:45](../../src/main/java/com/umc/product/organization/adapter/in/web/GisuQueryController.java#L45) `GET /all` — 전체 이름 리스트

**사유**: 응답 모델만 다를 뿐 같은 자원의 전체 목록임. School도 동일 패턴(`/all` + `/gisu/{gisuId}`)이 있음. 공개(`@Public`) API에서 페이지네이션이 굳이 필요한지 의문(기수 수가 작음).

**액션**: `GET /` 하나로 통합하고 응답을 명시적인 형태로 정리.

---

## 3. 🟡 REST 의미 / 명명 정정 권고

### 3-1. StudyGroup 멤버/멘토 추가에 PATCH 사용

- [StudyGroupCommandController](../../src/main/java/com/umc/product/organization/adapter/in/web/StudyGroupCommandController.java)
    - `PATCH /{studyGroupId}/members/{memberId}` (addMember)
    - `PATCH /{studyGroupId}/mentors/{mentorId}` (addMentor)

**사유**: 새 자원 생성에 PATCH를 사용하는 것은 RFC 5789 의미와 어긋남(PATCH는 부분 수정). 같은 도메인의 [ProjectCommandController.java:138](../../src/main/java/com/umc/product/project/adapter/in/web/ProjectCommandController.java#L138)에서는 `POST /{projectId}/members`로 일관적임.

**액션**: PUT (idempotent assignment) 또는 POST로 정정.

### 3-2. CustomErrorController의 `@Hidden` 처리는 정상이나 위치/명명 검토

- [CustomErrorController.java](../../src/main/java/com/umc/product/global/exception/CustomErrorController.java) — `ErrorController` fallback. 필요하므로 유지.
- 단, 이름이 일반 `Controller`와 헷갈리니 운영 매뉴얼/내부 문서에 명시 권고. **제거 대상은 아님**.

---

## 4. 🟢 위치/명명 오류

### 4-1. `ChallengerRecordPermissionController`는 컨트롤러가 아님

- [ChallengerRecordPermissionController.java](../../src/main/java/com/umc/product/challenger/application/service/evaluator/ChallengerRecordPermissionController.java)

**사유**: 클래스는 실제로는 `ResourcePermissionEvaluator`를 구현하는 **Spring `@Component`(Authorization Evaluator)**. 패키지도 `application/service/evaluator`로 컨트롤러가 아님이 명확함.

**액션**: `ChallengerRecordPermissionEvaluator`로 리네임. `*Controller.java`로 grep하는 모든 도구(IDE, 문서, 본 분석 자체)에서 노이즈를 만듦.

---

## 5. ⚠️ 보안/운영 관점 추가 메모 (제거 대상은 아니지만 확인 필요)

### 5-1. TestController — `@Profile("local | dev")`

- [TestController.java](../../src/main/java/com/umc/product/test/controller/TestController.java)
- `GET /test/token/access?memberId=...` 로 임의 memberId의 AccessToken 발급, `GET /test/token/refresh` 도 동일.
- 프로파일 가드는 적용되어 있으나, **prod 배포 시 활성 프로파일 누락이 곧 인증 우회로 이어짐**.

**액션**: 이상적으로는 `@Profile`을 클래스에 두는 것 외에 별도 모듈/소스셋으로 분리 검토. 제거 대상은 아님.

### 5-2. FcmController의 매핑 누락 메서드

- [FcmController.java:41-43](../../src/main/java/com/umc/product/notification/adapter/in/web/FcmController.java#L41-L43)
- `resubscribeAllMemberLegacyTopics()`는 `@RequestMapping` 계열 어노테이션이 **구현체에 없음**(인터페이스 `FcmControllerApi`에 정의되어 있을 가능성).
- 인터페이스가 비어있다면 이 메서드는 HTTP로 도달 불가하면서 코드만 남아있음.

**액션**: 의도적이라면 컨트롤러가 아니라 내부 호출용 Service로 분리. (검증 필요)

---

## 6. 요약 액션 아이템

| #  | 대상                                                     | 등급 | 액션                          |
|----|--------------------------------------------------------|----|-----------------------------|
| 1  | Curriculum v2 워크북 24개 (5개 컨트롤러)                        | 🔴 | 컨트롤러 삭제 또는 비공개 처리           |
| 2  | `GET /challenger/search/global`                        | 🔴 | 삭제                          |
| 3  | `GET /schools/link/{schoolId}`                         | 🔴 | 삭제                          |
| 4  | `GET /schools/unassigned`                              | 🔴 | 삭제                          |
| 5  | `GET /authorization/challenger-role/{id}`              | 🔴 | 사용처 확인 후 삭제                 |
| 6  | 과거 Trophy ↔ v2 weekly-best 이중 모델                          | ✅ | Trophy API 제거, `WeeklyBestWorkbook` 유지 완료 |
| 7  | NoticeContentController + NoticeVoteResponseController | 🟠 | NoticeCommandController로 통합 |
| 8  | `read-statics` 명명 (오타)                                 | 🟠 | `read-statistics`로 정정       |
| 9  | Challenger search cursor/offset 병행                     | 🟠 | 한쪽으로 일원화                    |
| 10 | Gisu `GET /` vs `GET /all`                             | 🟠 | 단일화                         |
| 11 | StudyGroup 멤버 추가 PATCH                                 | 🟡 | POST/PUT으로 정정               |
| 12 | `ChallengerRecordPermissionController` 명명              | 🟢 | `…Evaluator`로 리네임           |
| 13 | TestController prod 노출 보호 강화                           | ⚠️ | 빌드 분리 검토                    |
| 14 | FcmController 매핑 누락 메서드                                | ⚠️ | 도달 가능 여부 검증 후 정리            |

---

## 7. 우선순위 제안

가장 큰 영향을 주는 항목 두 가지:

1. **Curriculum v2 미구현 컨트롤러 5개 일괄 정리** — 잘못된 API 명세로 인한 클라이언트 통합 비용 최소화
2. **완료: 베스트 워크북 SSoT 정리** — `WeeklyBestWorkbook` 유지, 과거 Trophy API 제거

Curriculum v2 정리 이후에는 `@Deprecated(forRemoval=true)` 항목과 명명 정정(읽기 통계 오타, StudyGroup PATCH)을 차례로 정리하는 것을 권장합니다. 베스트 워크북 SSoT 항목은 위 릴리스에서 완료했다.
