#!/usr/bin/env bash
set -euo pipefail

# 데이터 준비의 "유일한 소유자". k6 는 시딩하지 않는다.
# SEED_STRATEGY 로 방식을 고른다. v1 은 api 만 구현한다.
#   api      : SeedController(/test/seed/*)를 순서대로 호출해 seed.json 산출 (기본)
#   sql      : (후속) api 결과를 pg_dump --data-only 로 굳힌 seed-baseline.sql 복원
#   snapshot : 여기 아님 — RDS 생성 시점이라 rds.tf 의 snapshot_identifier 로 다룬다
#
# 사용법: loadtest/scripts/prepare-data.sh
#   SEED_STRATEGY     : 기본 api
#   SUT_URL           : (선택) SUT 앱 URL. 미설정이면 terraform output sut_app_url 사용.
#   SEED_MEMBER_COUNT : api 시딩 멤버 수 (기본 50)

REPO_ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
TF_DIR="$REPO_ROOT/loadtest/terraform"
SEED_JSON="$REPO_ROOT/loadtest/k6/data/seed.json"
SEED_STRATEGY="${SEED_STRATEGY:-api}"

resolve_base_url() {
  if [ -n "${SUT_URL:-}" ]; then
    echo "$SUT_URL"
  else
    terraform -chdir="$TF_DIR" output -raw sut_app_url
  fi
}

# seed API 는 도메인 가드를 통과하는 baseline "골격"만 만든다(대용량 아님). 실패 시 즉시 중단.
api_post() {
  local path="$1" body="$2"
  curl -fsS -X POST "$BASE_URL$path" \
    -H 'Content-Type: application/json' \
    -d "$body"
}

# 도메인별 "얇은" 스텝: 어느 엔드포인트를 어떤 순서로 호출하는지만 담는다. 도메인 규칙은 Java(SeedController)에 있다.
# 정확한 요청 body 는 각 Seed*Request DTO 로 검증한다(계약 변경 시 이 스텝만 수정).
seed_api() {
  echo "[api] BASE_URL=$BASE_URL"

  # 1) members (SEED-001). body 확인됨: { count, force }
  echo "[api] seed members (count=${SEED_MEMBER_COUNT:-50})"
  api_post /test/seed/members "{\"count\": ${SEED_MEMBER_COUNT:-50}, \"force\": false}" >/dev/null

  # 2) challengers (SEED-002) — TODO: SeedChallengersRequest 필드(gisuId/chapterIds/parts/countPerPartPerSchool) 확인 후 활성화
  # api_post /test/seed/challengers '{ "gisuId": null, "countPerPartPerSchool": 2 }' >/dev/null

  # 3) project scenarios (SEED-003-S) — TODO: SeedProjectScenariosRequest 필드(projectCount/targetStatus 등) 확인 후 활성화
  # api_post /test/seed/projects/scenarios '{ "projectCount": 5 }' >/dev/null

  # 4) project applications (SEED-006) — TODO: SeedProjectApplicationsRequest 필드(matchingRoundId/chapterId) 확인 후 활성화
  # api_post /test/seed/project-applications '{ ... }' >/dev/null

  assemble_seed_json
}

# seed API 응답은 생성 ID 를 직접 돌려주지 않는다(예: SeedMembersResponse={registered,skipped,reason}).
# 따라서 k6 가 쓸 ID 는 조회로 모아 seed.json 으로 굳힌다.
# 이 assembly 는 전략(api/sql)이 달라도 항상 같은 스키마를 내야 한다 — 그래야 k6 가 전략 불가지가 된다.
assemble_seed_json() {
  echo "[api] assemble seed.json"

  # gisuId 는 공개 조회로 확보 가능.
  local gisu_id
  gisu_id="$(curl -fsS "$BASE_URL/api/v1/gisu/active" | jq -r '.result.gisuId // .gisuId // empty' 2>/dev/null || true)"

  # TODO: 나머지 ID 를 조회로 채운다 (seed.example.json 과 동일 스키마).
  #   - memberIds: 시딩 계정 조회(members query 엔드포인트) 또는 psql 로 seed email-domain 필터
  #   - chapterId: 대상 지부. gisu/chapter 조회로 확보
  #   - matchingRoundId / projectId: project scenario 시딩 후 project query 로 확보
  jq -n --arg gisuId "${gisu_id:-}" '
    {
      gisuId: $gisuId,
      chapterId: "",
      matchingRoundId: "",
      memberIds: [],
      targets: []
    }
  ' >"$SEED_JSON"
  echo "[api] wrote $SEED_JSON (일부 필드는 TODO — 위 주석 참조. gitignore 대상이라 커밋되지 않음)"
}

seed_sql() {
  echo "[sql] (후속) psql < seed-baseline.sql 로 복원. baseline 은 api 결과를 pg_dump --data-only 로 굳힌 아티팩트." >&2
  echo "[sql] v1 에서는 미구현. SEED_STRATEGY=api 를 사용하세요." >&2
  exit 1
}

case "$SEED_STRATEGY" in
api)
  BASE_URL="$(resolve_base_url)"
  seed_api
  ;;
sql)
  seed_sql
  ;;
snapshot)
  echo "snapshot 전략은 prepare-data.sh 가 아니라 rds.tf 의 snapshot_identifier 로 다룹니다." >&2
  exit 1
  ;;
*)
  echo "알 수 없는 SEED_STRATEGY: $SEED_STRATEGY (api|sql|snapshot)" >&2
  exit 1
  ;;
esac
