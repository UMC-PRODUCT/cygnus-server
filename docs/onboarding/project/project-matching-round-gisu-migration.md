# 매칭 차수 기수 마이그레이션 운영 계약

## 목적

`V2026.07.13.21.20__add_gisu_to_project_matching_round.sql`은 `project_matching_round.gisu_id`를 도입하고 기존 행을 `chapter.gisu_id`로 역채움한다. 최종 스키마의 `gisu_id`는 `NOT NULL`이며 `gisu(id)`를 참조한다.

신규 API와 애플리케이션 코드는 `gisuId`를 필수 입력으로 취급한다. 신규 writer에는 데이터베이스 역채움 fallback을 사용하지 않으며, 기수-지부 소속 관계와 기수 활동 기간을 애플리케이션 계층에서 검증한다.

## 배포 전 데이터 감사

마이그레이션은 다음 순서로 기존 데이터를 감사하고, 위반 행이 하나라도 있으면 전체 트랜잭션을 롤백한다.

| 감사 코드 | 거부 조건 |
|---|---|
| `PMR_GISU_AUDIT_ORPHAN_CHAPTER` | 매칭 차수가 존재하지 않는 지부를 참조함 |
| `PMR_GISU_AUDIT_NULL_CHAPTER_GISU` | 매칭 차수가 참조한 지부의 `gisu_id`가 `NULL`임 |
| `PMR_GISU_AUDIT_REMAINING_NULL_GISU` | 역채움 이후에도 매칭 차수의 `gisu_id`가 `NULL`임 |
| `PMR_GISU_AUDIT_ROUND_PERIOD` | `gisu.start_at <= starts_at < ends_at < decision_deadline < gisu.end_at`를 만족하지 않음 |
| `PMR_GISU_AUDIT_APPLICATION_SCOPE` | 지원서의 프로젝트와 매칭 차수의 기수 또는 지부가 일치하지 않음 |

실패 메시지에는 최초 위반 행의 `round_id` 또는 `application_id`가 포함된다. 데이터를 보정한 뒤 마이그레이션을 다시 실행한다.

## 롤링 배포 호환 구간

롤링 배포 중 아직 교체되지 않은 ECS task는 `gisu_id` 없이 쓰기를 시도할 수 있다. 이를 위해 다음 임시 데이터베이스 객체를 둔다.

- trigger: `trg_project_matching_round_legacy_gisu`
- function: `fill_project_matching_round_gisu_for_legacy_writer()`

trigger는 `BEFORE INSERT OR UPDATE OF chapter_id, gisu_id`에 실행된다. 함수의 `search_path`는 `pg_catalog`로 고정하고 모든 애플리케이션 relation은 `public.`으로 한정하므로, 호출 세션의 선행 schema나 임시 relation이 `chapter` 조회를 가로챌 수 없다. 조회한 chapter 행은 transaction 종료까지 `FOR SHARE`로 잠가 검증과 저장 사이의 기수 변경을 막는다. 테이블 전체 잠금은 사용하지 않는다.

trigger는 항상 `public.chapter.gisu_id`를 expected 값으로 조회한 뒤 다음 계약을 적용한다.

- chapter가 없으면 `PMR_GISU_TRIGGER_CHAPTER_NOT_FOUND`로 거부한다.
- chapter의 `gisu_id`가 `NULL`이면 `PMR_GISU_TRIGGER_NULL_CHAPTER_GISU`로 거부한다.
- `NEW.gisu_id IS NULL`이면 구버전 writer 호환을 위해 expected 값을 채운다.
- 신규 writer가 명시한 값이 expected 값과 같으면 그대로 보존한다.
- 명시한 값이 expected 값과 다르거나 chapter 변경으로 기존 값이 stale해지면 `PMR_GISU_TRIGGER_GISU_CHAPTER_MISMATCH`로 거부한다.

따라서 신규 writer가 명시한 올바른 `gisu_id`는 덮어쓰지 않으며, 기수-지부 불일치 tuple은 insert와 update 모두에서 저장되지 않는다. 이 호환 장치는 구버전 ECS task만을 위한 임시 수단이며, 신규 API의 검증 책임을 대체하지 않는다.

## 영구 쓰기 무결성

롤링 배포 호환 trigger와 별개로 다음 객체는 제거하지 않는 영구 무결성 장치다.

- trigger/function: `trg_project_matching_round_period` / `enforce_project_matching_round_period()`
- trigger/function: `trg_project_application_matching_scope` / `enforce_project_application_matching_scope()`
- trigger/function: `trg_project_parent_application_scope` / `enforce_project_parent_application_scope()`
- trigger/function: `trg_project_application_form_parent_scope` / `enforce_project_application_form_parent_scope()`

네 함수도 `search_path=pg_catalog`와 `public.` 한정 relation을 사용한다. 기준이 되는 gisu, form/project,
matching round 행은 `FOR SHARE`로 잠가 동일 transaction의 검증 snapshot이 쓰기 직전까지 유지되도록 한다.

`trg_project_matching_round_period`는 insert 및 `gisu_id`, `starts_at`, `ends_at`,
`decision_deadline` update마다 다음 전체 순서를 검사한다.

```text
gisu.start_at <= starts_at < ends_at < decision_deadline < gisu.end_at
```

기수가 없으면 `PMR_GISU_TRIGGER_GISU_NOT_FOUND`, 기간이 맞지 않으면
`PMR_GISU_TRIGGER_ROUND_PERIOD`로 거부한다. old writer insert에서는 이름순으로 legacy trigger가 먼저
`gisu_id`를 채운 뒤 period trigger가 검사한다.

`trg_project_application_matching_scope`는 application insert 및
`project_application_form_id`, `applied_matching_round_id` update마다 부모 project와 round의
`gisu_id`, `chapter_id`가 모두 같은지 검사한다. 부모 form/project나 round가 없으면 각각
`PMR_GISU_TRIGGER_APPLICATION_FORM_NOT_FOUND`, `PMR_GISU_TRIGGER_MATCHING_ROUND_NOT_FOUND`, scope가
다르면 `PMR_GISU_TRIGGER_APPLICATION_SCOPE`로 거부한다.

`trg_project_parent_application_scope`는 application이 연결된 project의 `gisu_id`, `chapter_id` update를
반대 방향에서 다시 검증한다. UPDATE가 project row lock을 이미 보유한 상태에서 관련 round만 `FOR SHARE`로 잠그며,
불일치하면 `PMR_GISU_TRIGGER_PROJECT_SCOPE`로 거부한다. `trg_project_application_form_parent_scope`는 form의
`project_id` update 시 새 project와 관련 round를 순서대로 `FOR SHARE`로 잠그고, 불일치하면
`PMR_GISU_TRIGGER_APPLICATION_FORM_SCOPE`로 거부한다.

두 parent 함수는 application/form/project/round를 수정하지 않고 `NEW`만 반환하므로 trigger 재귀가 없다. project
trigger는 application/form row를 역순으로 잠그지 않고, form trigger는 이미 보유한 form row에서 project, round
순서로만 잠근다. child writer는 기존 trigger에서 form/project 뒤 round를 잠그므로 parent-child 동시 쓰기는 대기 후
최신 tuple로 재검증되고 lock cycle이나 TOCTOU를 만들지 않는다. 명시적인 table lock은 사용하지 않는다.

## 제거 조건과 절차

다음 조건을 모두 충족한 뒤 별도의 후속 Flyway migration에서 임시 객체를 제거한다.

1. 이전 애플리케이션 버전의 ECS task가 모두 drain되어 실행 중이지 않다.
2. 전체 Project action ENFORCE 이후 최소 7일 관측 기간 동안 `gisu_id`를 생략한 구버전 writer 트래픽이 없다.
3. 실행 중인 모든 신규 task가 매칭 차수 생성 시 `gisuId`를 명시한다.
4. 다음 데이터 확인 결과가 `0`이다.

```sql
SELECT count(*)
FROM public.project_matching_round
WHERE gisu_id IS NULL;
```

후속 migration의 제거 SQL 계약은 다음과 같다. 현재 migration에는 포함하지 않는다.

```sql
DROP TRIGGER trg_project_matching_round_legacy_gisu ON public.project_matching_round;
DROP FUNCTION public.fill_project_matching_round_gisu_for_legacy_writer();
```

`trg_project_matching_round_period`, `trg_project_application_matching_scope`,
`trg_project_parent_application_scope`, `trg_project_application_form_parent_scope`와 네 함수는 영구 무결성
장치이므로 이 후속 migration에서 제거하지 않는다.

제거 전 객체 존재 여부는 다음 쿼리로 확인할 수 있다.

```sql
SELECT trigger_name
FROM information_schema.triggers
WHERE event_object_table = 'project_matching_round'
  AND trigger_schema = 'public'
  AND trigger_name = 'trg_project_matching_round_legacy_gisu';
```

제거 migration 적용 전후에는 다음 audit이 모두 0인지 다시 확인한다.

```sql
SELECT count(*)
FROM public.project_matching_round round
         JOIN public.gisu ON gisu.id = round.gisu_id
WHERE (gisu.start_at <= round.starts_at
    AND round.starts_at < round.ends_at
    AND round.ends_at < round.decision_deadline
    AND round.decision_deadline < gisu.end_at) IS NOT TRUE;

SELECT count(*)
FROM public.project_application application
         JOIN public.project_application_form form ON form.id = application.project_application_form_id
         JOIN public.project ON project.id = form.project_id
         JOIN public.project_matching_round round ON round.id = application.applied_matching_round_id
WHERE project.gisu_id IS DISTINCT FROM round.gisu_id
   OR project.chapter_id IS DISTINCT FROM round.chapter_id;
```
