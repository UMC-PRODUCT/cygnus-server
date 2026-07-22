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
#   SEED_MEMBER_COUNT : api 시딩 멤버 수 (기본 30)

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

# 홈 화면 부하 시나리오용 시딩.
# 단건 생성 API(SEED-001-M / SEED-002-C)가 생성 ID 를 반환하므로 그걸로 memberId 를 결정적으로 수집한다.
# (벌크 SEED-001/002 는 ID 를 안 돌려줘서 별도 조회가 필요하다.)
# 응답은 GlobalResponseWrapper 로 감싸져 payload 가 .result 아래 온다(String 반환은 예외).
# SeedController·gisu·schools 는 모두 @Public 이라 시딩에 토큰이 필요 없다.
seed_api() {
  local count="${SEED_MEMBER_COUNT:-30}"
  echo "[api] BASE_URL=$BASE_URL, count=$count"

  # 1) 활성 기수
  local gisu_id
  gisu_id="$(curl -fsS "$BASE_URL/api/v1/gisu/active" | jq -r '.result.gisuId // .result.id // empty')"
  [ -n "$gisu_id" ] || {
    echo "활성 기수 조회 실패 (GET /api/v1/gisu/active)" >&2
    exit 1
  }

  # 2) 그 기수에 속한 학교 하나 (챌린저 생성에 유효한 school 이 필요)
  local school_id
  school_id="$(curl -fsS "$BASE_URL/api/v1/schools/gisu/$gisu_id" | jq -r '.result[0].schoolId // .result[0].id // empty')"
  [ -n "$school_id" ] || {
    echo "기수 $gisu_id 의 학교 조회 실패 (GET /api/v1/schools/gisu/$gisu_id)" >&2
    exit 1
  }
  echo "[api] gisuId=$gisu_id schoolId=$school_id"

  # 3) member + challenger 반복 생성. 단건 API 응답에서 memberId 를 모은다.
  #    챌린저 등록은 best-effort — 실패해도 member/me 는 동작한다(집계 데이터만 빈다).
  local parts=(WEB ANDROID IOS NODEJS SPRINGBOOT DESIGN PLAN)
  local ts mid part i
  local member_ids=()
  ts="$(date +%s)"
  for i in $(seq 1 "$count"); do
    mid="$(api_post /test/seed/member \
      "{\"name\":\"부하테스트$i\",\"nickname\":\"lt$ts-$i\",\"schoolId\":$school_id,\"email\":\"loadtest+$ts-$i@test.umc.it.kr\"}" \
      | jq -r '.result.memberId // .memberId // empty')" || {
      echo "  member 생성 실패 (i=$i)" >&2
      continue
    }
    [ -n "$mid" ] || {
      echo "  member 생성 응답에 memberId 없음 (i=$i)" >&2
      continue
    }
    part="${parts[$(((i - 1) % ${#parts[@]}))]}"
    api_post /test/seed/challenger \
      "{\"memberId\":$mid,\"gisuId\":$gisu_id,\"part\":\"$part\"}" >/dev/null \
      || echo "  challenger 등록 실패 memberId=$mid (member/me 는 동작)" >&2
    member_ids+=("$mid")
  done
  [ ${#member_ids[@]} -gt 0 ] || {
    echo "생성된 member 가 없습니다" >&2
    exit 1
  }
  echo "[api] member ${#member_ids[@]}명 생성"

  # 4) seed.json — 전략(api/sql) 무관 동일 스키마. k6 는 gisuId·memberIds 만 쓴다.
  printf '%s\n' "${member_ids[@]}" \
    | jq -R . \
    | jq -s --arg gisuId "$gisu_id" \
      '{gisuId: $gisuId, chapterId: "", matchingRoundId: "", memberIds: ., targets: []}' \
      >"$SEED_JSON"
  echo "[api] wrote $SEED_JSON (gisuId=$gisu_id, members=${#member_ids[@]})"
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
