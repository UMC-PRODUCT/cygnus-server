# 챌린저 관리 FE API 가이드

> 작성일: 2026-05-06 / 최종 수정: 2026-05-08
>
> 본 문서는 세 가지 플로우에 사용되는 백엔드 API의 request/response 스펙을 FE 관점에서 정리한 것입니다.
>
> - **플로우 A — 운영진 흐름**: 회원 검색 → 회원의 챌린저 기록 조회 → 특정 챌린저 기록 상세 → 상벌점 부여
> - **플로우 B — 챌린저 기록 부여 및 정보 조회**: 6자리 코드 발급/사용/조회 + 신규 챌린저 등록
> - **플로우 C — 챌린저 기록 수정/삭제 및 운영진 기록 관리**: 챌린저 파트 변경, 비활성화, 삭제 + 운영진 역할 기록 관리

---

## 0. 공통 사항

### 0.1 Base URL & 인증

- 모든 엔드포인트는 `/api/v1/...` 접두사를 가진다.
- `[Public]` 으로 표시되지 않은 엔드포인트는 모두 **Authorization 헤더 필수**.
  ```
  Authorization: Bearer {accessToken}
  ```
- 본 문서의 모든 엔드포인트는 인증 필요 (Public 없음).

### 0.2 응답 래핑 (`ApiResponse`)

[GlobalResponseWrapper.java](src/main/java/com/umc/product/global/response/GlobalResponseWrapper.java) 에 의해 **모든 컨트롤러 응답은 자동으로 다음 형태로 래핑**된다.

**성공 응답:**

```json
{
    "success": true,
    "code": "COMMON-200",
    "message": "성공입니다.",
    "result": <컨트롤러가
    리턴한
    값>
}
```

**실패 응답 (에러):**

```json
{
    "success": false,
    "code": "CHALLENGER-0001",
    "message": "챌린저를 찾을 수 없습니다.",
    "result": null
}
```

> 본 문서의 "Response" 섹션에는 **`result` 안에 들어가는 값만** 기술한다. FE 에서는 항상 `response.result` 로 접근해야 한다.

### 0.3 페이지네이션 형식

#### Offset 기반 (`PageResponse<T>`)

[PageResponse.java](src/main/java/com/umc/product/global/response/PageResponse.java)

```json
{
    "content": [
        /* T[] */
    ],
    "page": 0,
    "size": 20,
    "totalElements": 153,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
}
```

- 요청 쿼리: `?page=0&size=20&sort=id,desc` (Spring `Pageable` 표준)

#### Cursor 기반 (`CursorResponse<T>`)

[CursorResponse.java](src/main/java/com/umc/product/global/response/CursorResponse.java)

```json
{
    "content": [
        /* T[] */
    ],
    "nextCursor": 12345,
    "hasNext": true
}
```

- 요청 쿼리: `?cursor=12345&size=20` (`cursor` 미전달 시 첫 페이지). `size` 기본 20, 최대 50.

### 0.4 주요 Enum

| Enum                         | 값                                                                                                                                                                                                                                                   |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ChallengerPart`             | `PLAN`, `DESIGN`, `WEB`, `ANDROID`, `IOS`, `NODEJS`, `SPRINGBOOT`, `ADMIN`                                                                                                                                                                          |
| `ChallengerStatus`           | `ACTIVE`, `GRADUATED`, `EXPELLED`, `WITHDRAWN`                                                                                                                                                                                                      |
| `MemberStatus`               | `ACTIVE`, `INACTIVE`, `WITHDRAWN`                                                                                                                                                                                                                   |
| `ChallengerRoleType`         | `SUPER_ADMIN` / `CENTRAL_PRESIDENT` / `CENTRAL_VICE_PRESIDENT` / `CENTRAL_OPERATING_TEAM_MEMBER` / `CENTRAL_EDUCATION_TEAM_MEMBER` / `CHAPTER_PRESIDENT` / `SCHOOL_PRESIDENT` / `SCHOOL_VICE_PRESIDENT` / `SCHOOL_PART_LEADER` / `SCHOOL_ETC_ADMIN` |
| `ChallengerDeactivationType` | `WITHDRAW` (탈부), `EXPEL` (제명)                                                                                                                                                                                                                       |
| `OrganizationType`           | `CENTRAL`, `CHAPTER`, `SCHOOL`                                                                                                                                                                                                                      |

#### `PointType` (상벌점 유형) — [PointType.java](src/main/java/com/umc/product/challenger/domain/enums/PointType.java)

각 유형은 **고정 점수**를 가지지만, 요청 시 `pointValue` 를 함께 보내면 **그 값으로 덮어쓸 수 있다** (`pointValue` null 이면 아래 표의 기본값 사용).

| Enum 값                           | 기본 점수 | 설명                    |
|----------------------------------|------:|-----------------------|
| `BLOG_CHALLENGE`                 |  +3.0 | 블로그 챌린지 참여 (매주 최대 +3) |
| `BEST_WORKBOOK_V2`               |  +2.0 | 베스트 워크북 (10기~)        |
| `UMC_EVENT_REVIEW`               |  +1.0 | 행사 리뷰어                |
| `PEER_REVIEW_SUBMISSION`         |  +1.0 | PeerReview 작성         |
| `OUT`                            |  +1.0 | 아웃                    |
| `WARNING`                        |   0.0 | 경고                    |
| `CUSTOM`                         |   0.0 | 자체 제도 운영 (ex. 가천대)    |
| `BEST_WORKBOOK`                  |  -0.5 | 베스트 워크북 (~9기, 레거시)    |
| `STUDY_LATE`                     |  -2.0 | 스터디 무단 지각             |
| `STUDY_ABSENT`                   |  -4.0 | 스터디 무단 불참             |
| `EVENT_LATE`                     |  -2.0 | 행사 무단 지각              |
| `EVENT_EARLY_LEAVE`              |  -2.0 | 행사 중도 퇴실              |
| `EVENT_LATE_CANCEL`              |  -4.0 | 행사 기간 외 취소            |
| `EVENT_NO_SHOW`                  | -10.0 | 노쇼 (무단 결석)            |
| `NO_WORKBOOK_MISSION`            |  -4.0 | 과제 미수행                |
| `PART_LEAD_FEEDBACK_LATE`        |  -4.0 | 기간 외 피드백              |
| `SCHOOL_CORE_MEETING_ABSENT`     |  -4.0 | 회의 무단 불참              |
| `SCHOOL_CORE_TASK_NOT_COMPLETED` |  -4.0 | 업무 무단 불이행             |

---

# 플로우 A — 운영진 흐름

운영진이 회원을 검색해서 특정 회원의 챌린저 기록(기수별 활동 이력)을 확인하고, 해당 챌린저 기록에 상벌점을 부여하는 흐름.

```
[A-1] 회원 검색
    ↓ memberId 획득
[A-2] 회원 정보 조회 (챌린저 기록 목록 포함)
    ↓ challengerId 선택
[A-3] 챌린저 기록 상세 조회 (상벌점 목록 포함)
    ↓
[A-4] 상벌점 부여 / [A-5] 사유 수정 / [A-6] 삭제
```

---

## A-1. 회원 검색 — `[MEMBER-103]`

[MemberQueryController.java:50-59](src/main/java/com/umc/product/member/adapter/in/web/MemberQueryController.java#L50-L59)

이름 / 닉네임 / 이메일 / 학교명을 키워드로 검색하고, 기수·파트·지부·학교로 필터링한다.
챌린저 기록이 하나라도 있는 회원만 사용할 수 있으며, 챌린저 기록이 없으면 `MEMBER-0014`와 함께 403을 반환한다.

### 요청

```
GET /api/v1/member/search
```

**Query Parameters** (모두 optional)

| 이름          | 타입                    | 설명                    |
|-------------|-----------------------|-----------------------|
| `keyword`   | string                | 이름/닉네임/이메일/학교명 통합 키워드 |
| `gisuId`    | long                  | 기수 ID 필터              |
| `part`      | enum `ChallengerPart` | 파트 필터                 |
| `chapterId` | long                  | 지부 ID 필터              |
| `schoolId`  | long                  | 학교 ID 필터              |
| `page`      | int                   | 페이지 번호(0-base, 기본 0)  |
| `size`      | int                   | 페이지 크기(기본 20)         |
| `sort`      | string                | 정렬, 예: `id,desc`      |

### 응답 (`result` 내부)

[SearchMemberResponse.java](src/main/java/com/umc/product/member/adapter/in/web/dto/response/SearchMemberResponse.java)

```json
{
    "totalCount": 153,
    "page": {
        "content": [
            {
                "memberId": 12,
                "name": "홍길동",
                "nickname": "gildong",
                "email": null,
                "schoolId": 1,
                "schoolName": "가천대학교",
                "profileImageLink": "https://.../profile.png",
                "challengerId": 401,
                "gisuId": 9,
                "gisu": 9,
                "part": "WEB",
                "roleTypes": [
                    "SCHOOL_PART_LEADER"
                ]
            }
        ],
        "page": 0,
        "size": 20,
        "totalElements": 153,
        "totalPages": 8,
        "hasNext": true,
        "hasPrevious": false
    }
}
```

> `challengerId` / `gisuId` / `gisu` / `part` / `roleTypes` 는 해당 회원의 **현재(가장 최근) 챌린저 기록** 한 건 기준으로 채워진다. 전체 기록은 [A-2](#a-2-회원-정보-조회--member-101) 에서 조회한다.

---

## A-2. 회원 정보 조회 — `[MEMBER-101]`

[MemberQueryController.java:32-42](src/main/java/com/umc/product/member/adapter/in/web/MemberQueryController.java#L32-L42)

회원 프로필과 함께 **모든 챌린저 기록(기수별)** 을 한 번에 받아온다.

### 요청

```
GET /api/v1/member/profile/{memberId}
```

**Path Parameters**

| 이름         | 타입   | 설명       |
|------------|------|----------|
| `memberId` | long | 대상 회원 ID |

권한: `@CheckAccess(MEMBER, READ)`

### 응답 (`result` 내부)

[MemberInfoResponse.java](src/main/java/com/umc/product/member/adapter/in/web/dto/response/MemberInfoResponse.java)

```json
{
    "id": 12,
    "name": "홍길동",
    "nickname": "gildong",
    "email": null,
    "schoolId": 1,
    "schoolName": "가천대학교",
    "profileImageLink": "https://.../profile.png",
    "status": null,
    "roles": [
        {
            "id": 88,
            "challengerId": 401,
            "roleType": "SCHOOL_PART_LEADER",
            "organizationType": "SCHOOL",
            "organizationId": 1,
            "responsiblePart": "WEB",
            "gisuId": 9
        }
    ],
    "challengerRecords": [
        {
            "challengerId": 401,
            "memberId": 12,
            "gisuId": 9,
            "gisu": 9,
            "chapterId": 3,
            "chapterName": "수도권",
            "part": "WEB",
            "challengerStatus": "ACTIVE",
            "points": [],
            "challengerPoints": [],
            "totalPoints": 0.0,
            "roles": [],
            "name": "홍길동",
            "nickname": "gildong",
            "email": null,
            "schoolId": 1,
            "schoolName": "가천대학교",
            "profileImageLink": "https://.../profile.png",
            "memberStatus": null,
            "status": null
        }
    ],
    "profile": null
}
```

> **주의 — Public View:** 다른 회원 조회 시에는 보안상 `email`, `status`, `memberStatus` 가 모두 `null` 로 마스킹되며, 각 challengerRecord 의 `points` 도 빈 배열로 비워진다. 본인 조회 (`/me`) 와는 차이가 있음.

> **본인 프로필 조회:** `GET /api/v1/member/me` — 동일 응답 구조이며 마스킹 없음 (이번 플로우에는 운영진이 타인을 보는 경우만 사용).

> `challengerPoints` 는 deprecated 필드로 `points` 와 동일한 값. FE 에서는 `points` 를 사용할 것.

---

## A-3. 챌린저 기록 상세 조회 — `[CHALLENGER-101]`

[ChallengerQueryController.java:44-48](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerQueryController.java#L44-L48)

특정 챌린저 기록 1건의 전체 상세 (상벌점 목록 + 역할 + 회원 정보 포함).

### 요청

```
GET /api/v1/challenger/{challengerId}
```

**Path Parameters**

| 이름             | 타입   | 설명        |
|----------------|------|-----------|
| `challengerId` | long | 챌린저 기록 ID |

### 응답 (`result` 내부)

[ChallengerInfoResponse.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/response/ChallengerInfoResponse.java)

```json
{
    "challengerId": 401,
    "memberId": 12,
    "gisuId": 9,
    "gisu": 9,
    "chapterId": 3,
    "chapterName": "수도권",
    "part": "WEB",
    "challengerStatus": "ACTIVE",
    "points": [
        {
            "id": 9001,
            "challengerId": 401,
            "pointType": "STUDY_LATE",
            "point": -2.0,
            "description": "5/2 스터디 15분 지각",
            "createdAt": "2026-05-02T13:05:00Z"
        }
    ],
    "challengerPoints": [
        /* 위와 동일, deprecated */
    ],
    "totalPoints": -2.0,
    "roles": [
        {
            "challengerRoleId": 88,
            "challengerId": 401,
            "roleType": "SCHOOL_PART_LEADER",
            "organizationType": "SCHOOL",
            "organizationId": 1,
            "responsiblePart": "WEB",
            "gisuId": 9,
            "gisu": 9
        }
    ],
    "name": "홍길동",
    "nickname": "gildong",
    "email": null,
    "schoolId": 1,
    "schoolName": "가천대학교",
    "profileImageLink": "https://.../profile.png",
    "memberStatus": "ACTIVE",
    "status": "ACTIVE"
}
```

> `points[].point` 는 `pointType` 의 기본값 또는 부여 시 명시한 `pointValue` 가 반영된 **실제 적용된 값**.
>
> `totalPoints` 는 모든 `points[].point` 의 합산.

**주요 에러:**

- `CHALLENGER-0001` (404) — 챌린저 기록 없음

---

## A-4. 챌린저 상벌점 부여 — `[POINT-001]`

[ChallengerPointCommandController.java:38-47](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerPointCommandController.java#L38-L47)

권한: `@CheckAccess(CHALLENGER_POINT, WRITE)` — 중앙운영사무국 소속 또는 해당 챌린저의 **학교 회장단** 만 부여 가능.

### 요청

```
POST /api/v1/challenger/{challengerId}/points
Content-Type: application/json
```

**Path Parameters**

| 이름             | 타입   | 설명              |
|----------------|------|-----------------|
| `challengerId` | long | 부여 대상 챌린저 기록 ID |

**Request Body** — [GrantChallengerPointRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/GrantChallengerPointRequest.java)

| 필드            | 타입               | 필수         | 설명                                                                      |
|---------------|------------------|------------|-------------------------------------------------------------------------|
| `pointType`   | `PointType` enum | ✅          | 상벌점 유형                                                                  |
| `pointValue`  | integer          | ⛔ optional | **null 이면 `pointType` 의 기본 점수 사용**. 명시 시 그 값으로 덮어씀 (`CUSTOM` 등 자체 제도용). |
| `description` | string           | ✅          | 부여 사유                                                                   |

```json
{
    "pointType": "STUDY_LATE",
    "pointValue": null,
    "description": "5/2 스터디 15분 지각"
}
```

또는 자체 제도용 커스텀 점수:

```json
{
    "pointType": "CUSTOM",
    "pointValue": -3,
    "description": "가천대 자체 규정 위반"
}
```

### 응답 (`result` 내부)

부여 후의 [ChallengerInfoResponse](#a-3-챌린저-기록-상세-조회--challenger-101) — 새로 부여된 상벌점이 `points` 배열에 포함되어 반환된다.

**주요 에러:**

- `CHALLENGER-0001` (404) — 챌린저 기록 없음
- `CHALLENGER-0004` (400) — 챌린저 상태 비유효 (이미 졸업/탈부 등)
- 권한 부족 — 401/403

---

## A-5. 챌린저 상벌점 사유 수정 — `[POINT-002]`

[ChallengerPointCommandController.java:55-62](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerPointCommandController.java#L55-L62)

권한: `@CheckAccess(CHALLENGER_POINT, EDIT)` — 중앙운영사무국 또는 해당 챌린저의 학교 회장단.

### 요청

```
PATCH /api/v1/challenger/points/{challengerPointId}
Content-Type: application/json
```

**Path Parameters**

| 이름                  | 타입   | 설명                               |
|---------------------|------|----------------------------------|
| `challengerPointId` | long | 수정할 상벌점 row ID (= `points[].id`) |

**Request Body** — [EditChallengerPointRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/EditChallengerPointRequest.java)

| 필드               | 타입     | 필수 | 설명     |
|------------------|--------|----|--------|
| `newDescription` | string | ✅  | 변경할 사유 |

```json
{
    "newDescription": "5/2 스터디 12분 지각으로 정정"
}
```

### 응답

`204 No Content` — `result: null`

```json
{
    "success": true,
    "code": "COMMON-200",
    "message": "성공입니다.",
    "result": null
}
```

**주요 에러:**

- `CHALLENGER-0007` (404) — 상벌점 row 없음

---

## A-6. 챌린저 상벌점 삭제 — `[POINT-003]`

[ChallengerPointCommandController.java:70-76](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerPointCommandController.java#L70-L76)

권한: `@CheckAccess(CHALLENGER_POINT, DELETE)` — **중앙운영사무국 총괄단(`CENTRAL_PRESIDENT`, `CENTRAL_VICE_PRESIDENT`)** 만 가능.

### 요청

```
DELETE /api/v1/challenger/points/{challengerPointId}
```

### 응답

`204 No Content` — `result: null`

---

## A-보조: 챌린저 검색 (운영진용)

회원이 아닌 **챌린저 기록(기수+파트별 활동)** 을 직접 검색할 때 사용. A-1 (회원 검색) 과 결과 단위가 다르다 — A-1 은 회원 단위, 이 API 는 (회원, 기수) 단위.

### A-보조-1. Cursor 기반 — `[CHALLENGER-102]`

[ChallengerSearchController.java:39-50](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerSearchController.java#L39-L50)

```
GET /api/v1/challenger/search/cursor
```

**Query Parameters** ([SearchChallengerCursorRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/SearchChallengerCursorRequest.java))

| 이름          | 타입               | 설명                                           |
|-------------|------------------|----------------------------------------------|
| `cursor`    | long             | 직전 페이지 마지막 challengerId, 첫 페이지 시 미전달         |
| `size`      | int              | 기본 20, 최대 50                                 |
| `keyword`   | string           | 이름 또는 닉네임 통합 검색 (있으면 `name` / `nickname` 무시) |
| `name`      | string           | 이름 부분일치                                      |
| `nickname`  | string           | 닉네임 부분일치                                     |
| `schoolId`  | long             | 학교 ID                                        |
| `chapterId` | long             | 지부 ID                                        |
| `part`      | `ChallengerPart` | 파트                                           |
| `gisuId`    | long             | 기수 ID                                        |

> 모든 필터는 `AND` 조건. `keyword` 제공 시 `name`/`nickname` 은 무시되지만 다른 필터는 유효.
>
> 결과는 **`ACTIVE`** 상태의 챌린저로 한정됨.

**응답 (`result`)** — [CursorSearchChallengerResponse.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/response/CursorSearchChallengerResponse.java)

```json
{
    "cursor": {
        "content": [
            {
                "challengerId": 401,
                "memberId": 12,
                "gisuId": 9,
                "generation": 9,
                "gisu": 9,
                "part": "WEB",
                "name": "홍길동",
                "nickname": "gildong",
                "schoolName": "가천대학교",
                "pointSum": -2.0,
                "profileImageLink": "https://.../profile.png",
                "roleTypes": [
                    "SCHOOL_PART_LEADER"
                ]
            }
        ],
        "nextCursor": 401,
        "hasNext": true
    },
    "partCounts": [
        {
            "part": "PLAN",
            "count": 12
        },
        {
            "part": "DESIGN",
            "count": 8
        },
        {
            "part": "WEB",
            "count": 30
        },
        {
            "part": "ANDROID",
            "count": 5
        },
        {
            "part": "IOS",
            "count": 4
        },
        {
            "part": "NODEJS",
            "count": 7
        },
        {
            "part": "SPRINGBOOT",
            "count": 11
        }
    ]
}
```

> `generation` 은 deprecated, `gisu` 사용 권장.
>
> `partCounts` 는 **현재 필터 조건에 매칭되는 전체** 의 파트별 카운트 (페이지 기준이 아님).

### A-보조-2. Offset 기반 — `[CHALLENGER-103]`

[ChallengerSearchController.java:52-64](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerSearchController.java#L52-L64)

```
GET /api/v1/challenger/search/offset?page=0&size=20&sort=id,desc
```

쿼리 필터는 cursor 와 동일(`cursor` 제외, `page`/`size`/`sort` 추가).

**응답 (`result`)** — [SearchChallengerResponse.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/response/SearchChallengerResponse.java)

```json
{
    "page": {
        "content": [
            /* SearchChallengerItemResponse[] (cursor 응답과 동일) */
        ],
        "page": 0,
        "size": 20,
        "totalElements": 77,
        "totalPages": 4,
        "hasNext": true,
        "hasPrevious": false
    },
    "partCounts": [
        /* 동일 */
    ]
}
```

---

# 플로우 B — 챌린저 기록 부여 및 정보 조회

챌린저 기록(`Challenger`)을 만드는 두 가지 방식과, 기록을 조회하는 API.

```
[방식 1] 현재 기수 챌린저 등록 (관리자가 회원 ID로 직접 생성)
  [B-1] POST /api/v1/challenger
  [B-2] POST /api/v1/challenger/batch  (다건)

[방식 2] 과거 기수 코드 기반 등록 (관리자가 코드 발급 → 회원이 코드 입력)
  [B-3] POST /api/v1/challenger-record           (관리자 — 코드 발급)
  [B-4] POST /api/v1/challenger-record/bulk      (관리자 — 다건 코드 발급)
  [B-5] POST /api/v1/challenger-record/member    (회원 — 코드 사용)

[조회]
  [B-6] GET  /api/v1/challenger-record/code/{code}
  [B-7] GET  /api/v1/challenger-record/id/{id}
```

---

## B-1. 챌린저 직접 생성 — `[CHALLENGER-001]`

[ChallengerCommandController.java:40-46](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandController.java#L40-L46)

권한: `@CheckAccess(CHALLENGER, WRITE)`

### 요청

```
POST /api/v1/challenger
Content-Type: application/json
```

**Request Body** — [CreateChallengerInfoRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/CreateChallengerInfoRequest.java)

| 필드         | 타입               | 필수 | 설명       |
|------------|------------------|----|----------|
| `memberId` | long             | ✅  | 대상 회원 ID |
| `part`     | `ChallengerPart` | ✅  | 파트       |
| `gisuId`   | long             | ✅  | 기수 ID    |

```json
{
    "memberId": 12,
    "part": "WEB",
    "gisuId": 10
}
```

### 응답 (`result`)

생성된 챌린저의 [ChallengerInfoResponse](#a-3-챌린저-기록-상세-조회--challenger-101).

---

## B-2. 챌린저 다건 생성 — `[CHALLENGER-002]`

[ChallengerCommandController.java:52-64](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandController.java#L52-L64)

권한: `@CheckAccess(CHALLENGER, WRITE)`

### 요청

```
POST /api/v1/challenger/batch
Content-Type: application/json
```

**Request Body** — `CreateChallengerInfoRequest[]`

```json
[
    {
        "memberId": 12,
        "part": "WEB",
        "gisuId": 10
    },
    {
        "memberId": 13,
        "part": "DESIGN",
        "gisuId": 10
    }
]
```

### 응답 (`result`)

`ChallengerInfoResponse[]` — 각 항목은 [A-3 응답](#a-3-챌린저-기록-상세-조회--challenger-101) 과 동일.

> 한 건이라도 실패하면 트랜잭션 자체는 항목별로 별도 호출되므로 부분 성공이 발생할 수 있음. FE 에서는 입력 길이와 응답 길이를 비교하여 실패 항목을 식별할 것 (정밀한 실패 보고가 필요하면 단건 API 반복 호출 권장).

---

## B-3. 챌린저 기록 코드 발급 (관리자) — `[CHALLENGER-RECORD-002]`

[ChallengerRecordController.java:89-114](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordController.java#L89-L114)

권한: `@CheckAccess(CHALLENGER_RECORD, WRITE)` — 중앙운영사무국 총괄단 전용. 9기 이전 기수의 활동 이력을 6자리 코드로 발급한다.

### 요청

```
POST /api/v1/challenger-record
Content-Type: application/json
```

**Request Body** — [CreateChallengerRecordRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/CreateChallengerRecordRequest.java)

| 필드                   | 타입                   | 필수 | 설명                         |
|----------------------|----------------------|----|----------------------------|
| `gisuId`             | long                 | ✅  | 기수 ID                      |
| `chapterId`          | long                 | ✅  | 지부 ID                      |
| `schoolId`           | long                 | ✅  | 학교 ID                      |
| `part`               | `ChallengerPart`     | ✅  | 파트                         |
| `memberName`         | string               | ✅  | 챌린저 본명 (코드 사용 시 일치 검증에 사용) |
| `challengerRoleType` | `ChallengerRoleType` | ✅  | 해당 기수에서의 역할                |

```json
{
    "gisuId": 8,
    "chapterId": 3,
    "schoolId": 1,
    "part": "WEB",
    "memberName": "김챌린저",
    "challengerRoleType": "SCHOOL_PRESIDENT"
}
```

### 응답 (`result`)

[ChallengerRecordResponse.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/response/ChallengerRecordResponse.java)

```json
{
    "code": "AB12CD",
    "part": "WEB",
    "gisuId": 8,
    "gisu": 8,
    "schoolId": 1,
    "schoolName": "가천대학교",
    "chapterId": 3,
    "chapterName": "수도권",
    "memberName": "김챌린저",
    "challengerRoleType": "SCHOOL_PRESIDENT",
    "organizationId": 1
}
```

> `code` 가 발급된 6자리 문자열. 회원에게 전달하여 [B-5](#b-5-회원-계정에-챌린저-기록-추가-코드-사용--challenger-record-001) 에서 사용하게 한다.

---

## B-4. 챌린저 기록 코드 다건 발급 — `[CHALLENGER-RECORD-003]`

[ChallengerRecordController.java:123-147](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordController.java#L123-L147)

권한: `@CheckAccess(CHALLENGER_RECORD, WRITE)`

### 요청

```
POST /api/v1/challenger-record/bulk
Content-Type: application/json
```

**Request Body** — `CreateChallengerRecordRequest[]` (B-3 의 객체 배열)

### 응답 (`result`)

성능 이슈로 ID 만 반환 — 실제 코드 / 정보는 [B-7](#b-7-id-로-챌린저-기록-조회--challenger-record-102) 로 개별 조회.

```json
[
    1001,
    1002,
    1003
]
```

---

## B-5. 회원 계정에 챌린저 기록 추가 (코드 사용) — `[CHALLENGER-RECORD-001]`

[ChallengerRecordController.java:43-58](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordController.java#L43-L58)

로그인한 회원이 6자리 코드를 입력하여 본인 계정에 챌린저 기록과 권한을 추가한다. 각 코드는 1회 사용 후 소진된다.

### 요청

```
POST /api/v1/challenger-record/member
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body** — [AddChallengerRecordToMemberRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/AddChallengerRecordToMemberRequest.java)

| 필드     | 타입     | 필수 | 설명     |
|--------|--------|----|--------|
| `code` | string | ✅  | 6자리 코드 |

```json
{
    "code": "AB12CD"
}
```

### 응답

`204 No Content` — `result: null`

**주요 에러:**

- `CHALLENGER-0012` (400) — 코드 없음 또는 이미 사용됨
- `CHALLENGER-0013` (400) — 코드의 `memberName` 과 현재 회원 이름 불일치
- `CHALLENGER-0014` (400) — 코드의 학교와 현재 회원 학교 불일치

---

## B-6. 코드로 챌린저 기록 조회 — `[CHALLENGER-RECORD-101]`

[ChallengerRecordController.java:64-70](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordController.java#L64-L70)

권한: `@CheckAccess(CHALLENGER_RECORD, READ)`

### 요청

```
GET /api/v1/challenger-record/code/{code}
```

| 이름     | 타입     | 설명     |
|--------|--------|--------|
| `code` | string | 6자리 코드 |

### 응답 (`result`)

[ChallengerRecordResponse](#b-3-챌린저-기록-코드-발급-관리자--challenger-record-002) 와 동일.

---

## B-7. ID 로 챌린저 기록 조회 — `[CHALLENGER-RECORD-102]`

[ChallengerRecordController.java:76-82](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerRecordController.java#L76-L82)

권한: `@CheckAccess(CHALLENGER_RECORD, READ)`

### 요청

```
GET /api/v1/challenger-record/id/{id}
```

| 이름   | 타입   | 설명                                                                                     |
|------|------|----------------------------------------------------------------------------------------|
| `id` | long | 챌린저 기록(코드) 의 DB ID — [B-4 bulk 응답](#b-4-챌린저-기록-코드-다건-발급--challenger-record-003) 으로 반환됨 |

### 응답 (`result`)

[ChallengerRecordResponse](#b-3-챌린저-기록-코드-발급-관리자--challenger-record-002) 와 동일.

---

## B-부록. 챌린저 기록 변경/삭제

### B-부록-1. 챌린저 비활성화 (제명/탈부) — `[CHALLENGER-003]`

[ChallengerCommandController.java:70-77](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandController.java#L70-L77) · 권한: `@CheckAccess(CHALLENGER, DELETE)`

```
POST /api/v1/challenger/{challengerId}/deactivate
```

**Body** — [DeactivateChallengerRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/DeactivateChallengerRequest.java)

```json
{
    "deactivationType": "EXPEL",
    "modifiedBy": 1,
    "reason": "출석 4회 누락"
}
```

- `deactivationType`: `WITHDRAW` (탈부) | `EXPEL` (제명)

응답: `204 No Content`

### B-부록-2. 챌린저 파트 변경 — `[CHALLENGER-004]`

[ChallengerCommandController.java:84-93](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandController.java#L84-L93) · 권한: `@CheckAccess(CHALLENGER, EDIT)`

```
PATCH /api/v1/challenger/{challengerId}/part
```

**Body** — [EditChallengerPartRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/EditChallengerPartRequest.java)

```json
{
    "newPart": "DESIGN"
}
```

응답: 변경 후 [ChallengerInfoResponse](#a-3-챌린저-기록-상세-조회--challenger-101).

### B-부록-3. 챌린저 하드 삭제 — `[CHALLENGER-005]`

[ChallengerCommandController.java:99-104](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandController.java#L99-L104) · 권한: `@CheckAccess(CHALLENGER, DELETE)`

```
DELETE /api/v1/challenger/{challengerId}
```

응답: `204 No Content`. ⚠️ 복구 불가.

---

# 부록. 엔드포인트 요약 표

## 플로우 A — 운영진 흐름

| #      | 코드             | Method | Path                                            | 권한                        |
|--------|----------------|--------|-------------------------------------------------|---------------------------|
| A-1    | MEMBER-103     | GET    | `/api/v1/member/search`                         | 인증                        |
| A-2    | MEMBER-101     | GET    | `/api/v1/member/profile/{memberId}`             | `MEMBER:READ`             |
| A-3    | CHALLENGER-101 | GET    | `/api/v1/challenger/{challengerId}`             | 인증                        |
| A-4    | POINT-001      | POST   | `/api/v1/challenger/{challengerId}/points`      | `CHALLENGER_POINT:WRITE`  |
| A-5    | POINT-002      | PATCH  | `/api/v1/challenger/points/{challengerPointId}` | `CHALLENGER_POINT:EDIT`   |
| A-6    | POINT-003      | DELETE | `/api/v1/challenger/points/{challengerPointId}` | `CHALLENGER_POINT:DELETE` |
| A-보조-1 | CHALLENGER-102 | GET    | `/api/v1/challenger/search/cursor`              | 인증                        |
| A-보조-2 | CHALLENGER-103 | GET    | `/api/v1/challenger/search/offset`              | 인증                        |

## 플로우 B — 챌린저 기록 부여 및 조회

| #      | 코드                    | Method | Path                                           | 권한                        |
|--------|-----------------------|--------|------------------------------------------------|---------------------------|
| B-1    | CHALLENGER-001        | POST   | `/api/v1/challenger`                           | `CHALLENGER:WRITE`        |
| B-2    | CHALLENGER-002        | POST   | `/api/v1/challenger/batch`                     | `CHALLENGER:WRITE`        |
| B-3    | CHALLENGER-RECORD-002 | POST   | `/api/v1/challenger-record`                    | `CHALLENGER_RECORD:WRITE` |
| B-4    | CHALLENGER-RECORD-003 | POST   | `/api/v1/challenger-record/bulk`               | `CHALLENGER_RECORD:WRITE` |
| B-5    | CHALLENGER-RECORD-001 | POST   | `/api/v1/challenger-record/member`             | 인증                        |
| B-6    | CHALLENGER-RECORD-101 | GET    | `/api/v1/challenger-record/code/{code}`        | `CHALLENGER_RECORD:READ`  |
| B-7    | CHALLENGER-RECORD-102 | GET    | `/api/v1/challenger-record/id/{id}`            | `CHALLENGER_RECORD:READ`  |
| B-부록-1 | CHALLENGER-003        | POST   | `/api/v1/challenger/{challengerId}/deactivate` | `CHALLENGER:DELETE`       |
| B-부록-2 | CHALLENGER-004        | PATCH  | `/api/v1/challenger/{challengerId}/part`       | `CHALLENGER:EDIT`         |
| B-부록-3 | CHALLENGER-005        | DELETE | `/api/v1/challenger/{challengerId}`            | `CHALLENGER:DELETE`       |

---

# 플로우 C — 챌린저 기록 수정/삭제 및 운영진 기록 관리

이미 등록된 챌린저 기록을 수정하거나 상태를 변경하는 흐름. 운영진(회장단 이상)이 백오피스에서 수행한다.

```
[C-1] 챌린저 파트 변경
[C-2] 챌린저 비활성화 (탈부 / 제명)
[C-3] 챌린저 하드 삭제 (복구 불가)
[C-4] 상벌점 부여   → A-4 와 동일
[C-5] 상벌점 사유 수정 → A-5 와 동일
[C-6] 상벌점 삭제   → A-6 와 동일
```

> `ChallengerRecord`(6자리 코드 방식의 과거 기수 기록)는 별도 수정/삭제 API가 **현재 존재하지 않는다**.
> 코드를 잘못 발급했을 경우 BE 팀과 협의하여 직접 처리해야 한다.

---

## C-1. 챌린저 파트 변경 — `[CHALLENGER-004]`

[ChallengerCommandController.java:84-93](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandController.java#L84-L93)

권한: `@CheckAccess(CHALLENGER, EDIT)` — 중앙운영사무국 또는 해당 챌린저 학교 회장단.

### 요청

```
PATCH /api/v1/challenger/{challengerId}/part
Content-Type: application/json
```

**Path Parameters**

| 이름             | 타입   | 설명              |
|----------------|------|-----------------|
| `challengerId` | long | 변경 대상 챌린저 기록 ID |

**Request Body** — [EditChallengerPartRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/EditChallengerPartRequest.java)

| 필드        | 타입               | 필수 | 설명      |
|-----------|------------------|----|---------|
| `newPart` | `ChallengerPart` | ✅  | 변경할 파트명 |

```json
{
    "newPart": "DESIGN"
}
```

### 응답 (`result`)

변경 후의 [ChallengerInfoResponse](#a-3-챌린저-기록-상세-조회--challenger-101).

---

## C-2. 챌린저 비활성화 — `[CHALLENGER-003]`

[ChallengerCommandController.java:70-77](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandController.java#L70-L77)

권한: `@CheckAccess(CHALLENGER, DELETE)` — 중앙운영사무국 총괄단 또는 학교 회장단.

탈부(`WITHDRAWN`) 또는 제명(`EXPELLED`) 처리한다. Hard Delete 가 아니므로 기록은 남는다.

### 요청

```
POST /api/v1/challenger/{challengerId}/deactivate
Content-Type: application/json
```

**Request Body** — [DeactivateChallengerRequest.java](src/main/java/com/umc/product/challenger/adapter/in/web/dto/request/DeactivateChallengerRequest.java)

| 필드                 | 타입                           | 필수 | 설명                            |
|--------------------|------------------------------|----|-------------------------------|
| `deactivationType` | `ChallengerDeactivationType` | ✅  | `WITHDRAW`(탈부) 또는 `EXPEL`(제명) |
| `modifiedBy`       | long                         | ✅  | 처리한 운영진의 회원 ID                |
| `reason`           | string                       | ✅  | 처리 사유                         |

```json
{
    "deactivationType": "EXPEL",
    "modifiedBy": 1,
    "reason": "출석 4회 누락으로 3OUT 적용"
}
```

### 응답

`204 No Content` — `result: null`

**주요 에러:**

- `CHALLENGER-0001` (404) — 챌린저 기록 없음
- `CHALLENGER-0004` (400) — 이미 비활성 상태

---

## C-3. 챌린저 하드 삭제 — `[CHALLENGER-005]`

[ChallengerCommandController.java:99-104](src/main/java/com/umc/product/challenger/adapter/in/web/ChallengerCommandController.java#L99-L104)

권한: `@CheckAccess(CHALLENGER, DELETE)` — **총괄단 전용. 복구 불가.**

잘못 생성된 챌린저 기록 자체를 영구 삭제한다. 상벌점 포함 모든 연결 데이터가 함께 삭제된다.

### 요청

```
DELETE /api/v1/challenger/{challengerId}
```

### 응답

`204 No Content` — `result: null`

> ⚠️ FE 에서는 반드시 **이중 확인 다이얼로그** 를 표시해야 한다. "이 챌린저 기록과 상벌점 전체가 영구 삭제됩니다. 복구할 수 없습니다."

---

# 백오피스 UI 구성 가이드

아래는 각 플로우에 맞는 백오피스 화면 구성 제안이다. FE 팀은 이를 참고하여 실제 컴포넌트와 라우팅을 설계할 것.

---

## 화면 1. 챌린저 목록 / 검색

> 경로 예시: `/backoffice/challengers`

운영진이 챌린저를 검색하고 목록을 확인하는 메인 화면.

### 레이아웃 구성

```
┌─────────────────────────────────────────────────────────┐
│  [기수 셀렉트▼]  [지부 셀렉트▼]  [학교 셀렉트▼]  [파트 셀렉트▼]   │
│  [검색어 입력창──────────────────────────] [검색 버튼]       │
├─────────────────────────────────────────────────────────┤
│  파트별 현황:  기획 12  디자인 8  웹 30  안드로이드 5  …         │
├─────────────────────────────────────────────────────────┤
│  이름   닉네임   학교   파트   상태   총 상벌점   역할   [상세보기] │
│  홍길동  gildong  가천대  웹     ACTIVE   -2.0   파트장   [→]  │
│  …                                                       │
├─────────────────────────────────────────────────────────┤
│  [이전] 페이지 1 / 8  [다음]                               │
└─────────────────────────────────────────────────────────┘
```

### 사용 API

| 동작        | API                                             |
|-----------|-------------------------------------------------|
| 챌린저 검색    | `GET /api/v1/challenger/search/offset` (A-보조-2) |
| 파트별 카운트   | 위 응답의 `partCounts` 활용                           |
| 회원 검색(대안) | `GET /api/v1/member/search` (A-1)               |

### 구현 포인트

- `gisuId` 미선택 시 현재 기수를 기본으로 선택해 요청한다.
- `partCounts` 를 상단 필터 탭 (기획 12 / 디자인 8 / …) 으로 활용한다.
- 상태(`challengerStatus`) 컬럼에 색상 배지 사용: `ACTIVE` → 초록, `EXPELLED` → 빨강, `WITHDRAWN` → 회색.
- 총 상벌점은 양수면 파란색, 음수면 빨간색으로 표시한다.

---

## 화면 2. 챌린저 상세 — 기록 관리

> 경로 예시: `/backoffice/challengers/{challengerId}`

특정 챌린저의 기수별 활동 기록과 상벌점을 관리하는 화면.

### 레이아웃 구성

```
┌───────────────────────────────────────────────────────────┐
│  ← 목록  │  홍길동  (닉네임: gildong)  │  가천대학교            │
│  9기 / 수도권 / 웹 파트  │  상태: ACTIVE  │  총 상벌점: -2.0점   │
├───────────────────────────────────────────────────────────┤
│  [파트 변경 버튼]  [비활성화 버튼]  [삭제 버튼 (위험)]             │
├───────────────────────────────────────────────────────────┤
│  ▼ 상벌점 목록                       [+ 상벌점 부여]           │
│  ┌──────────┬────────┬───────┬────────────────────────────┐│
│  │ 유형       │ 점수   │ 사유   │ 일시          │ 수정  삭제   ││
│  │ STUDY_LATE│ -2.0  │ 5/2지각│ 2026-05-02   │ [✏] [🗑]  ││
│  └──────────┴────────┴───────┴────────────────────────────┘│
└───────────────────────────────────────────────────────────┘
```

### 사용 API

| 동작        | API                                                          | 비고       |
|-----------|--------------------------------------------------------------|----------|
| 챌린저 상세 조회 | `GET /api/v1/challenger/{challengerId}` (A-3)                |          |
| 파트 변경     | `PATCH /api/v1/challenger/{challengerId}/part` (C-1)         |          |
| 비활성화      | `POST /api/v1/challenger/{challengerId}/deactivate` (C-2)    | 모달 확인 필요 |
| 하드 삭제     | `DELETE /api/v1/challenger/{challengerId}` (C-3)             | 이중 확인 필요 |
| 상벌점 부여    | `POST /api/v1/challenger/{challengerId}/points` (A-4)        |          |
| 상벌점 사유 수정 | `PATCH /api/v1/challenger/points/{challengerPointId}` (A-5)  |          |
| 상벌점 삭제    | `DELETE /api/v1/challenger/points/{challengerPointId}` (A-6) | 총괄단 전용   |

### 파트 변경 모달

```
[ 파트 변경 ]
현재 파트: 웹 (WEB)
변경할 파트: [셀렉트▼ 기획 / 디자인 / 웹 / 안드로이드 / iOS / 노드 / 스프링부트 / 운영진]
                                              [취소]  [변경 확인]
```

- `PATCH /api/v1/challenger/{challengerId}/part` → `{ "newPart": "DESIGN" }`
- 성공 시 챌린저 상세 정보를 re-fetch 하여 화면 갱신.

### 비활성화 모달

```
[ 챌린저 비활성화 ]
유형:  ○ 탈부 (WITHDRAW)   ● 제명 (EXPEL)
사유: [텍스트 입력────────────────────────]
처리자 ID: (현재 로그인한 운영진 ID 자동 입력)
                                    [취소]  [처리 확인]
```

- `POST /api/v1/challenger/{challengerId}/deactivate`
- 성공 후 챌린저 상태 배지를 `EXPELLED` / `WITHDRAWN` 으로 갱신.

### 상벌점 부여 모달

```
[ 상벌점 부여 ]
유형 [셀렉트▼]: 스터디 무단 지각 (-2.0) / 스터디 무단 불참 (-4.0) / …
점수 덮어쓰기: [  ] (CUSTOM 또는 학교 자체 제도 사용 시 체크)
  → 체크 시 활성화되는 숫자 입력 필드
사유: [텍스트 입력────────────────────────────]
                                    [취소]  [부여]
```

- `pointValue` 는 "점수 덮어쓰기" 체크 시에만 전송. 미전송이면 enum 기본 점수 사용.
- 부여 후 `points` 목록을 re-fetch.

### 상벌점 사유 수정 인라인 편집

- 목록의 사유 셀을 클릭하면 인라인 `<input>` 으로 전환.
- 포커스 아웃 또는 [저장] 클릭 시 `PATCH` 호출.
- `PATCH /api/v1/challenger/points/{challengerPointId}` → `{ "newDescription": "수정된 사유" }`

### 상벌점 삭제

- [🗑] 클릭 → 확인 팝업("이 상벌점 기록을 삭제하시겠습니까?") → `DELETE /api/v1/challenger/points/{challengerPointId}`
- 총괄단(`CENTRAL_PRESIDENT`, `CENTRAL_VICE_PRESIDENT`)만 버튼이 활성화됨. 아닌 경우 버튼을 disabled 처리하거나 숨김.

---

## 화면 3. 챌린저 신규 등록 (운영진 직접 생성)

> 경로 예시: `/backoffice/challengers/new`

현재 기수 신규 챌린저를 직접 등록하는 화면. 회원 검색 → 기수/파트 선택 → 등록.

### 레이아웃 구성 (단건)

```
[ 챌린저 등록 ]
회원 검색:  [검색어 입력] [검색]  → 결과 목록에서 선택
선택된 회원: 홍길동 (ID: 12) 가천대학교
기수: [셀렉트▼]
파트: [셀렉트▼]
                                    [취소]  [등록]
```

- `POST /api/v1/challenger` → `{ "memberId": 12, "part": "WEB", "gisuId": 10 }`
- 성공 시 생성된 `challengerId` 로 상세 화면 이동.

### 배치(Batch) 등록 — CSV 업로드

```
[ 챌린저 배치 등록 ]
CSV 업로드: [파일 선택]  또는  직접 입력 (회원 ID | 파트 | 기수)
미리보기:
  ┌────────┬─────────┬─────┐
  │ 회원 ID │  파트   │ 기수 │
  │   12   │  WEB   │  10 │
  │   13   │ DESIGN │  10 │
  └────────┴─────────┴─────┘
                            [취소]  [일괄 등록]
```

- `POST /api/v1/challenger/batch`
- 응답 배열 길이 ≠ 입력 길이인 경우 실패 항목을 사용자에게 알린다.

---

## 화면 4. 챌린저 기록 코드 관리 (운영진 — 과거 기수)

> 경로 예시: `/backoffice/challenger-records`

9기 이전 과거 기수 활동 이력을 6자리 코드로 발급하고 현황을 관리하는 화면.

### 레이아웃 구성

```
┌────────────────────────────────────────────────────────────┐
│  [+ 코드 단건 발급]  [+ 코드 일괄 발급 (CSV)]                    │
├──────┬────────┬──────┬─────────┬────────────────┬──────────┤
│ 코드  │ 기수   │ 학교  │ 파트    │ 사용 여부       │ 사용 회원  │
│AB12CD│  8기   │가천대 │ 웹      │ ✅ 2026-05-01  │ 홍길동    │
│XY34ZW│  8기   │가천대 │디자인   │ ❌ 미사용       │ —       │
└──────┴────────┴──────┴─────────┴────────────────┴──────────┘
```

### 코드 단건 발급 모달

```
[ 챌린저 기록 코드 발급 ]
기수:     [셀렉트▼]
지부:     [셀렉트▼]
학교:     [셀렉트▼]
파트:     [셀렉트▼]
본명:     [텍스트 입력 ─ 코드 사용 시 일치 검증에 사용됨]
역할:     [셀렉트▼ 회장 / 부회장 / 파트장 / …]
                                    [취소]  [발급]
```

- `POST /api/v1/challenger-record` — 성공 응답의 `code` 를 화면에 표시하고 클립보드 복사 버튼 제공.

### 코드 일괄 발급 — CSV 업로드

- `POST /api/v1/challenger-record/bulk` 호출.
- 응답으로 받은 ID 배열(`[1001, 1002, …]`) 을 `GET /api/v1/challenger-record/id/{id}` 로 순차 조회하여 발급 결과 테이블 표시.

### 구현 포인트

- **발급된 코드는 삭제 API가 없다**. 잘못 발급된 경우 현재 BE 에서 직접 처리 필요 — 화면에 "코드 삭제 불가" 안내 문구 표시.
- 코드가 미사용 상태인 경우, 관리자 화면에서 코드 문자열 전체를 보여주고 복사 버튼 제공.
- 이미 사용된 코드는 `usedAt` 타임스탬프와 사용 회원 이름을 함께 표시.

---

## 화면 5. 회원 챌린저 기록 추가 (일반 회원 앱 화면)

> 경로 예시: `/my/challenger-record/add` (백오피스가 아닌 회원 앱)

운영진이 발급한 6자리 코드를 입력하여 본인 계정에 과거 챌린저 기록을 추가하는 화면.

### 레이아웃 구성

```
┌────────────────────────────────────┐
│  이전 기수 챌린저 기록 추가             │
│                                    │
│  운영진으로부터 받은 6자리 코드를          │
│  입력해 주세요.                        │
│                                    │
│  [  A  B  1  2  C  D  ]           │
│                                    │
│  [코드 확인 및 추가]                   │
└────────────────────────────────────┘
```

### 플로우

```
1. 6자리 코드 입력
2. POST /api/v1/challenger-record/member  { "code": "AB12CD" }
3. 성공 → "챌린저 기록이 추가되었습니다!" 토스트 메시지
4. 실패 케이스별 안내:
   - 코드 없음 / 이미 사용: "유효하지 않은 코드입니다."
   - 이름 불일치: "코드의 이름과 계정 이름이 일치하지 않습니다."
   - 학교 불일치: "코드의 학교와 계정 학교가 일치하지 않습니다."
```

### 사용 API

| 동작        | API                                           |
|-----------|-----------------------------------------------|
| 코드로 기록 추가 | `POST /api/v1/challenger-record/member` (B-5) |

---

## 화면 설계 시 공통 주의사항

### 권한별 UI 분기

| 역할                                                   | 허용 기능                            |
|------------------------------------------------------|----------------------------------|
| 학교 회장단 (`SCHOOL_PRESIDENT`, `SCHOOL_VICE_PRESIDENT`) | 자기 학교 챌린저 상벌점 부여/수정, 파트 변경, 비활성화 |
| 중앙운영사무국 운영/교육국원                                      | 전체 챌린저 상벌점 부여/수정                 |
| 중앙운영사무국 총괄단                                          | 위 모두 + 상벌점 삭제 + 하드 삭제 + 코드 발급    |
| 슈퍼 관리자 (`SUPER_ADMIN`)                               | 모든 기능                            |

- 403 응답 시: 권한 없음 안내 토스트 표시.
- 버튼 disabled vs 숨김: 버튼을 숨기는 것보다 disabled + 툴팁 표시 방식이 UX 상 권장됨 (권한이 있어야 한다는 것을 인지시킴).

### 비가역적 동작에 대한 UX 가이드

| 동작        | 권장 처리                               |
|-----------|-------------------------------------|
| 챌린저 비활성화  | 확인 모달 1회 ("탈부/제명 처리됩니다. 계속하시겠습니까?") |
| 챌린저 하드 삭제 | 확인 모달 + 챌린저 이름 직접 입력 (오타 방지)        |
| 상벌점 삭제    | 확인 팝업 1회                            |

### 에러 코드 요약

| 코드                | 상황                        |
|-------------------|---------------------------|
| `CHALLENGER-0001` | 챌린저 기록 없음 (404)           |
| `CHALLENGER-0004` | 이미 비활성화된 챌린저에 부여 시도 (400) |
| `CHALLENGER-0007` | 상벌점 기록 없음 (404)           |
| `CHALLENGER-0012` | 코드 없음 또는 이미 사용 (400)      |
| `CHALLENGER-0013` | 코드의 이름과 불일치 (400)         |
| `CHALLENGER-0014` | 코드의 학교와 불일치 (400)         |
