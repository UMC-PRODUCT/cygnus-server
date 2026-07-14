# 매칭 차수 API 테스트 JSON

`ProjectMatchingRound` CRUD API의 주요 Edge Case를 수동 테스트 또는 API 클라이언트 테스트에 활용할 수 있도록 정리합니다.

## 공통 정보

| 항목       | 값                                                          |
|----------|------------------------------------------------------------|
| Base URL | `/api/v1/project/matching-rounds`                          |
| 인증       | 로그인 사용자의 Bearer Token 필요                                   |
| 생성 메서드   | `POST /api/v1/project/matching-rounds`                     |
| 수정 메서드   | `PATCH /api/v1/project/matching-rounds/{matchingRoundId}`  |
| 삭제 메서드   | `DELETE /api/v1/project/matching-rounds/{matchingRoundId}` |
| 조회 메서드   | `GET /api/v1/project/matching-rounds`                      |
| 시간 형식    | ISO-8601 Instant, 예: `2026-05-10T00:00:00Z`                |

## 테스트 데이터 전제

| 변수                                   | 설명                        | 예시 값   |
|--------------------------------------|---------------------------|--------|
| `GISU_ID`                            | 매칭 차수와 지부가 속한 기수 ID       | `10`   |
| `CHAPTER_ID_OWNED`                   | 요청자가 지부장 권한을 가진 지부 ID     | `1`    |
| `CHAPTER_ID_NOT_OWNED`               | 요청자가 관리 권한을 가지지 않은 지부 ID  | `2`    |
| `MATCHING_ROUND_ID`                  | 수정/삭제 대상 매칭 차수 ID         | `101`  |
| `MATCHING_ROUND_WITH_APPLICATION_ID` | 지원서가 연결된 매칭 차수 ID         | `102`  |
| `CENTRAL_CORE_TOKEN`                 | 중앙운영사무국 총괄단 이상 사용자 토큰     | 환경별 발급 |
| `CHAPTER_PRESIDENT_TOKEN`            | `CHAPTER_ID_OWNED` 지부장 토큰 | 환경별 발급 |
| `NORMAL_MEMBER_TOKEN`                | 관리 권한이 없는 일반 사용자 토큰       | 환경별 발급 |

## Edge Case 요약

| 번호 | 케이스                    | 요청       | 기대 결과              |
|----|------------------------|----------|--------------------|
| 1  | 관리 권한 없는 지부에 생성 요청     | `POST`   | `403 PROJECT-0303` |
| 2  | 기간이 겹치는 매칭 차수 생성 요청    | `POST`   | `409 PROJECT-0302` |
| 3  | 날짜 순서가 잘못된 생성 요청       | `POST`   | `400 PROJECT-0301` |
| 4  | 매칭 차수 부분 수정 가능         | `PATCH`  | `200 OK` 또는 빈 응답   |
| 5  | 관련 지원서가 있는 매칭 차수 삭제 요청 | `DELETE` | `409 PROJECT-0304` |
| 6  | 조회 API 정상 동작           | `GET`    | 필터 조건에 맞는 배열 반환    |
| 7  | 기수 ID 누락/지부 불일치        | `POST`   | `400 Bad Request`  |

---

## 1. 관리 권한이 없는 지부에 대한 매칭 차수 생성 요청

요청자가 `CHAPTER_ID_NOT_OWNED`에 대한 지부장 권한도 없고 중앙운영사무국 총괄단 이상도 아닌 경우를 검증합니다.

| 항목       | 값                                                  |
|----------|----------------------------------------------------|
| Method   | `POST`                                             |
| URL      | `/api/v1/project/matching-rounds`                  |
| Token    | `CHAPTER_PRESIDENT_TOKEN` 또는 `NORMAL_MEMBER_TOKEN` |
| 기대 상태    | `403 Forbidden`                                    |
| 기대 에러 코드 | `PROJECT-0303`                                     |

```json
{
    "name": "기획-디자인 1차 매칭",
    "description": "관리 권한이 없는 지부에 생성 요청",
    "type": "PLAN_DESIGN",
    "phase": "FIRST",
    "gisuId": 10,
    "chapterId": 2,
    "startsAt": "2026-05-10T00:00:00Z",
    "endsAt": "2026-05-12T00:00:00Z",
    "decisionDeadline": "2026-05-13T00:00:00Z"
}
```

## 2. 매칭 차수를 겹쳐서 생성 요청

같은 지부에 이미 `2026-05-10T00:00:00Z ~ 2026-05-13T00:00:00Z` 기간의 매칭 차수가 존재한다고 가정합니다.

| 항목       | 값                                                 |
|----------|---------------------------------------------------|
| Method   | `POST`                                            |
| URL      | `/api/v1/project/matching-rounds`                 |
| Token    | `CHAPTER_PRESIDENT_TOKEN` 또는 `CENTRAL_CORE_TOKEN` |
| 기대 상태    | `409 Conflict`                                    |
| 기대 에러 코드 | `PROJECT-0302`                                    |
| 중복 판단 기준 | 같은 지부 내 `startsAt ~ decisionDeadline` 기간          |

```json
{
    "name": "기획-개발자 1차 매칭",
    "description": "기존 매칭 차수 기간과 일부 겹치는 요청",
    "type": "PLAN_DEVELOPER",
    "phase": "FIRST",
    "gisuId": 10,
    "chapterId": 1,
    "startsAt": "2026-05-11T00:00:00Z",
    "endsAt": "2026-05-14T00:00:00Z",
    "decisionDeadline": "2026-05-15T00:00:00Z"
}
```

## 3. 매칭 차수 생성 요청 시 날짜 순서가 잘못된 경우

`gisu.startAt <= startsAt < endsAt < decisionDeadline < gisu.endAt` 조건을 만족하지 않는 요청을 검증합니다.

| 항목       | 값                                                 |
|----------|---------------------------------------------------|
| Method   | `POST`                                            |
| URL      | `/api/v1/project/matching-rounds`                 |
| Token    | `CHAPTER_PRESIDENT_TOKEN` 또는 `CENTRAL_CORE_TOKEN` |
| 기대 상태    | `400 Bad Request`                                 |
| 기대 에러 코드 | `PROJECT-0301`                                    |

### 3-1. `startsAt`이 `endsAt`보다 늦은 경우

```json
{
    "name": "기획-디자인 2차 매칭",
    "description": "startsAt이 endsAt보다 늦은 잘못된 요청",
    "type": "PLAN_DESIGN",
    "phase": "SECOND",
    "gisuId": 10,
    "chapterId": 1,
    "startsAt": "2026-05-12T00:00:00Z",
    "endsAt": "2026-05-10T00:00:00Z",
    "decisionDeadline": "2026-05-13T00:00:00Z"
}
```

### 3-2. `endsAt`이 `decisionDeadline`보다 늦은 경우

```json
{
    "name": "기획-개발자 2차 매칭",
    "description": "endsAt이 decisionDeadline보다 늦은 잘못된 요청",
    "type": "PLAN_DEVELOPER",
    "phase": "SECOND",
    "gisuId": 10,
    "chapterId": 1,
    "startsAt": "2026-05-14T00:00:00Z",
    "endsAt": "2026-05-18T00:00:00Z",
    "decisionDeadline": "2026-05-17T00:00:00Z"
}
```

## 4. 매칭 차수 부분 수정 가능

기존 매칭 차수의 이름, 설명, 유형, 차수, 기간을 수정할 수 있음을 검증합니다.

> 수정 API는 부분 수정 성격의 `PATCH`입니다. 요청 본문에는 수정할 필드만 포함합니다. `gisuId`와 `chapterId`는 생성 후 불변이므로 포함하면 400을 반환합니다.

| 항목     | 값                                                     |
|--------|-------------------------------------------------------|
| Method | `PATCH`                                               |
| URL    | `/api/v1/project/matching-rounds/{MATCHING_ROUND_ID}` |
| Token  | `CHAPTER_PRESIDENT_TOKEN` 또는 `CENTRAL_CORE_TOKEN`     |
| 기대 상태  | `200 OK`                                              |
| 기대 결과  | 응답 바디 없음                                              |

```json
{
    "name": "기획-디자인 1차 매칭 수정",
    "startsAt": "2026-05-20T00:00:00Z",
    "endsAt": "2026-05-22T00:00:00Z",
    "decisionDeadline": "2026-05-23T00:00:00Z"
}
```

수정 후 조회 시 기대되는 주요 확인 사항:

| 필드                 | 기대 값                   |
|--------------------|------------------------|
| `name`             | `기획-디자인 1차 매칭 수정`      |
| `description`      | 기존 값 유지                |
| `type`             | 기존 값 유지                |
| `phase`            | 기존 값 유지                |
| `startsAt`         | `2026-05-20T00:00:00Z` |
| `endsAt`           | `2026-05-22T00:00:00Z` |
| `decisionDeadline` | `2026-05-23T00:00:00Z` |
| `gisuId`           | 기존 매칭 차수의 `gisuId`     |
| `chapterId`        | 기존 매칭 차수의 `chapterId`  |

### 4-1. 부분 수정 결과의 기간 순서가 잘못된 경우

기존 값과 요청 값을 병합한 최종 결과가 `startsAt < endsAt < decisionDeadline`을 만족하지 않으면 실패합니다.

| 항목       | 값                                                     |
|----------|-------------------------------------------------------|
| Method   | `PATCH`                                               |
| URL      | `/api/v1/project/matching-rounds/{MATCHING_ROUND_ID}` |
| 기대 상태    | `400 Bad Request`                                     |
| 기대 에러 코드 | `PROJECT-0301`                                        |

```json
{
    "startsAt": "2026-05-14T00:00:00Z"
}
```

### 4-2. 수정 요청에 `gisuId` 또는 `chapterId`가 포함된 경우

| 항목     | 값                                                     |
|--------|-------------------------------------------------------|
| Method | `PATCH`                                               |
| URL    | `/api/v1/project/matching-rounds/{MATCHING_ROUND_ID}` |
| 기대 상태  | `400 Bad Request`                                     |

```json
{
    "gisuId": 11
}
```

## 5. 관련된 지원서가 있는 경우 매칭 차수 삭제 불가능

`ProjectApplication.appliedMatchingRound`가 대상 매칭 차수를 참조하는 경우 삭제가 차단되는지 검증합니다.

| 항목       | 값                                                                      |
|----------|------------------------------------------------------------------------|
| Method   | `DELETE`                                                               |
| URL      | `/api/v1/project/matching-rounds/{MATCHING_ROUND_WITH_APPLICATION_ID}` |
| Token    | `CHAPTER_PRESIDENT_TOKEN` 또는 `CENTRAL_CORE_TOKEN`                      |
| 기대 상태    | `409 Conflict`                                                         |
| 기대 에러 코드 | `PROJECT-0304`                                                         |

요청 바디 없음.

## 6. 조회 API가 올바르게 동작함을 보장

### 6-1. `chapterId` 기준 목록 조회

| 항목     | 값                                             |
|--------|-----------------------------------------------|
| Method | `GET`                                         |
| URL    | `/api/v1/project/matching-rounds?chapterId=1` |
| 기대 상태  | `200 OK`                                      |
| 기대 결과  | `chapterId = 1`인 매칭 차수 배열                     |

예상 응답:

```json
[
    {
        "id": 101,
        "name": "기획-디자인 1차 매칭",
        "description": "1지부 기획-디자인 1차 매칭",
        "type": "PLAN_DESIGN",
        "phase": "FIRST",
        "gisuId": 10,
        "chapterId": 1,
        "startsAt": "2026-05-10T00:00:00Z",
        "endsAt": "2026-05-12T00:00:00Z",
        "decisionDeadline": "2026-05-13T00:00:00Z",
        "autoDecisionExecutedAt": null,
        "autoDecisionExecutedMemberId": null,
        "createdAt": "2026-05-01T00:00:00Z",
        "updatedAt": "2026-05-01T00:00:00Z"
    }
]
```

### 6-2. `time` 기준 지원 가능 매칭 차수 조회

| 항목     | 값                                                                       |
|--------|-------------------------------------------------------------------------|
| Method | `GET`                                                                   |
| URL    | `/api/v1/project/matching-rounds?chapterId=1&time=2026-05-11T00:00:00Z` |
| 기대 상태  | `200 OK`                                                                |
| 기대 결과  | `startsAt <= time <= endsAt` 조건을 만족하는 배열                                |
| 배열 길이  | 최대 1개                                                                   |

예상 응답:

```json
[
    {
        "id": 101,
        "name": "기획-디자인 1차 매칭",
        "description": "1지부 기획-디자인 1차 매칭",
        "type": "PLAN_DESIGN",
        "phase": "FIRST",
        "gisuId": 10,
        "chapterId": 1,
        "startsAt": "2026-05-10T00:00:00Z",
        "endsAt": "2026-05-12T00:00:00Z",
        "decisionDeadline": "2026-05-13T00:00:00Z",
        "autoDecisionExecutedAt": null,
        "autoDecisionExecutedMemberId": null,
        "createdAt": "2026-05-01T00:00:00Z",
        "updatedAt": "2026-05-01T00:00:00Z"
    }
]
```

### 6-3. `gisuId`, `chapterId`, `time` 선택적 AND 조회

| 항목       | 값                                                           |
|----------|-------------------------------------------------------------|
| Method   | `GET`                                                       |
| URL      | `/api/v1/project/matching-rounds?gisuId=10&chapterId=1&time=2026-05-11T00:00:00Z` |
| 기대 상태    | `200 OK`                                           |
| 기대 결과    | 제공된 조건을 모두 만족하는 배열                              |

요청 바디 없음.

## 생성 성공 예시

정상 생성 케이스의 기준 JSON입니다.

| 항목     | 값                                                 |
|--------|---------------------------------------------------|
| Method | `POST`                                            |
| URL    | `/api/v1/project/matching-rounds`                 |
| Token  | `CHAPTER_PRESIDENT_TOKEN` 또는 `CENTRAL_CORE_TOKEN` |
| 기대 상태  | `200 OK`                                          |

```json
{
    "name": "기획-디자인 3차 매칭",
    "description": "기간이 겹치지 않는 정상 생성 요청",
    "type": "PLAN_DESIGN",
    "phase": "THIRD",
    "gisuId": 10,
    "chapterId": 1,
    "startsAt": "2026-06-01T00:00:00Z",
    "endsAt": "2026-06-03T00:00:00Z",
    "decisionDeadline": "2026-06-04T00:00:00Z"
}
```

예상 응답:

```json
{
    "matchingRoundId": 201
}
```
