#!/usr/bin/env bash
set -euo pipefail

# 데이터 준비의 "유일한 소유자". k6 는 시딩하지 않는다.
# SEED_STRATEGY 로 방식을 고른다.
#   api      : SeedController(/test/seed/*)를 순서대로 호출해 seed.json 산출 (기본, smoke·소규모)
#   bulk     : SUT EC2 에서 앱 이미지를 seeder 프로파일로 1회 실행해 대규모(10만+) JDBC 배치 적재
#   snapshot : 여기 아님 — RDS 생성 시점이라 rds.tf 의 snapshot_identifier 로 다룬다
#
# 사용법: loadtest/scripts/prepare-data.sh
#   SEED_STRATEGY     : 기본 api
#   SUT_URL           : (선택) SUT 앱 URL. 미설정이면 terraform output sut_app_url 사용.
#   SEED_MEMBER_COUNT   : api 시딩 멤버 수 (기본 30)
#   SCHEDULE_COUNT      : 생성할 이번 달 스케줄 수 (기본 4)
#   NOTICE_GLOBAL_COUNT : 생성할 GLOBAL 공지 수 (기본 5)
#   POINT_PER_CHALLENGER: 챌린저당 상벌점 부여 수 (기본 2)
#   BULK_MEMBER_COUNT   : bulk 멤버 수 (기본 100000)
#   BULK_RANDOM_SEED    : bulk 결정성 seed — 같은 seed 면 같은 데이터 (기본 42)
#   SSH_KEY / SSH_USER  : bulk 의 SUT SSH 접속 (run-k6.sh 와 동일 관례)

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

# 인증 필요한 POST (운영진 토큰). 스케줄 생성 등 실제 command API 에 사용.
api_post_auth() {
  local path="$1" body="$2" token="$3"
  curl -fsS -X POST "$BASE_URL$path" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $token" \
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
  #    챌린저 등록 실패 멤버는 seed.json 에서 제외한다 — schedules/me(@CheckAccess SCHEDULE READ)가
  #    챌린저 기록을 요구해 그 멤버로는 홈 시나리오가 403 을 맞기 때문.
  local parts=(WEB ANDROID IOS NODEJS SPRINGBOOT DESIGN PLAN)
  local ts mid cid part i
  local member_ids=()
  local challenger_member_ids=()
  local challenger_ids=()
  local admin_member_id="" admin_challenger_id=""
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
    # 챌린저 등록 — 응답의 challengerId 를 첫 성공분만 운영진 후보로 보관 (스케줄/공지 생성 권한용)
    cid="$(api_post /test/seed/challenger \
      "{\"memberId\":$mid,\"gisuId\":$gisu_id,\"part\":\"$part\"}" \
      | jq -r '.result.challengerId // empty')" \
      || {
        echo "  challenger 등록 실패 memberId=$mid (member/me 는 동작)" >&2
        cid=""
      }
    if [ -z "$admin_member_id" ] && [ -n "$cid" ]; then
      admin_member_id="$mid"
      admin_challenger_id="$cid"
    fi
    if [ -n "$cid" ]; then
      challenger_ids+=("$cid")
      challenger_member_ids+=("$mid")
    fi
    member_ids+=("$mid")
  done
  [ ${#challenger_member_ids[@]} -gt 0 ] || {
    echo "챌린저 등록에 성공한 member 가 없습니다 — 홈 시나리오(schedules/me)가 403 이라 seed.json 을 만들지 않습니다" >&2
    exit 1
  }
  echo "[api] member ${#member_ids[@]}명 생성 (challenger ${#challenger_member_ids[@]}명)"

  # 참여자 memberId / challengerId JSON 배열
  local participants challenger_ids_json
  participants="$(printf '%s\n' "${member_ids[@]}" | jq -R 'tonumber' | jq -sc .)"
  if [ ${#challenger_ids[@]} -gt 0 ]; then
    challenger_ids_json="$(printf '%s\n' "${challenger_ids[@]}" | jq -R 'tonumber' | jq -sc .)"
  else
    challenger_ids_json="[]"
  fi

  # 홈 화면의 일정·공지·상벌점 데이터 채우기 (best-effort — 실패해도 seed.json 은 정상 산출)
  seed_home_data "$gisu_id" "$admin_member_id" "$admin_challenger_id" "$participants" "$challenger_ids_json"

  # seed.json — 전략 무관 동일 스키마. k6 는 gisuId·memberIds 만 쓴다.
  # memberIds 는 챌린저 등록 성공 멤버만 담는다 (schedules/me 403 방지).
  printf '%s\n' "${challenger_member_ids[@]}" \
    | jq -R . \
    | jq -s --arg gisuId "$gisu_id" \
      '{gisuId: $gisuId, chapterId: "", matchingRoundId: "", memberIds: ., targets: []}' \
      >"$SEED_JSON"
  echo "[api] wrote $SEED_JSON (gisuId=$gisu_id, members=${#challenger_member_ids[@]})"
}

# 홈 화면의 일정·공지·상벌점 데이터를 채운다.
# 스케줄엔 seed API 가 없어, 운영진 권한 멤버의 토큰으로 POST /api/v2/schedules 를 호출한다.
# 상벌점은 SEED-007(/test/seed/challenger-points) 벌크 한 방으로 시딩 챌린저 전원에게 부여한다.
# 모두 best-effort — 실패해도 seed.json(멤버·기수)은 정상 산출된다.
seed_home_data() {
  local gisu_id="$1" admin_mid="$2" admin_cid="$3" participants="$4" challenger_ids_json="$5"

  if [ -z "$admin_cid" ] || [ -z "$admin_mid" ]; then
    echo "  운영진 후보(challenger) 없음 — 일정·공지 시딩 건너뜀" >&2
    return 0
  fi

  # 1) 운영진 역할 부여: CENTRAL_PRESIDENT(총괄). 중앙 역할이라 organizationId 불필요.
  #    총괄은 스케줄 생성·GLOBAL 공지·상벌점 권한을 모두 만족한다.
  api_post /test/seed/challenger-role \
    "{\"challengerId\":$admin_cid,\"roleType\":\"CENTRAL_PRESIDENT\",\"gisuId\":$gisu_id}" >/dev/null \
    || {
      echo "  운영진 역할 부여 실패 — 일정·공지 시딩 건너뜀" >&2
      return 0
    }

  # 2) 운영진 토큰 (TEST-007, String 반환이라 래핑 없음)
  local token
  token="$(curl -fsS "$BASE_URL/test/token/access?memberId=$admin_mid")" || {
    echo "  운영진 토큰 발급 실패 — 일정·공지 시딩 건너뜀" >&2
    return 0
  }

  # 3) 이번 달 스케줄 N개 (참여자 = 시딩 멤버 전원). 홈의 schedules/me 가 이걸 반환한다.
  #    k6 홈 시나리오도 "이번 달" 범위로 조회하므로 같은 달에 실행하는 한 일치한다.
  #    tags 는 @NotNull(min 1). 활성 기수 기간 밖 날짜(SCHEDULE-0025)는 개별 실패로 남는다.
  local year month schedule_count sc day
  year="$(date -u +%Y)"
  month="$(date -u +%m)"
  schedule_count="${SCHEDULE_COUNT:-4}"
  for sc in $(seq 1 "$schedule_count"); do
    day="$(printf '%02d' $((sc * 7)))"
    api_post_auth /api/v2/schedules \
      "{\"name\":\"${month#0}월 정기모임 $sc\",\"description\":\"부하테스트 일정\",\"tags\":[\"GENERAL\"],\"startsAt\":\"${year}-${month}-${day}T10:00:00Z\",\"endsAt\":\"${year}-${month}-${day}T12:00:00Z\",\"participantMemberIds\":$participants}" \
      "$token" >/dev/null \
      || echo "  스케줄 생성 실패 ($sc)" >&2
  done
  echo "[api] ${month#0}월 스케줄 ${schedule_count}개 생성 시도 (참여자 전원 포함)"

  # 4) GLOBAL 공지 (author=운영진). 홈의 notices 가 이걸 반환한다.
  local notice_count="${NOTICE_GLOBAL_COUNT:-5}"
  api_post /test/seed/notice \
    "{\"gisuId\":$gisu_id,\"authorMemberId\":$admin_mid,\"globalCount\":$notice_count,\"perChapterCount\":0,\"perSchoolCount\":0,\"perPartCount\":0,\"parts\":null}" >/dev/null \
    || echo "  공지 시딩 실패" >&2
  echo "[api] GLOBAL 공지 ${notice_count}건 시딩 시도"

  # 5) 상벌점 벌크 시딩 (SEED-007). 홈 member/me 의 challengerHistory 상벌점을 채운다.
  #    상점/벌점 타입 순환은 서버(ChallengerPointSeedService)가 소유한다 — 단일 호출·단일 트랜잭션.
  local per="${POINT_PER_CHALLENGER:-2}"
  local granted
  granted="$(api_post /test/seed/challenger-points \
    "{\"challengerIds\":$challenger_ids_json,\"countPerChallenger\":$per,\"description\":\"부하테스트 시딩\"}" \
    | jq -r '.result.grantedPointCount // 0')" \
    || {
      echo "  상벌점 벌크 시딩 실패" >&2
      granted=0
    }
  echo "[api] 상벌점 ${granted}건 부여"
}

# 대규모(10만+) 벌크 시딩. SUT EC2 에서 앱 이미지를 seeder 프로파일 컨테이너로 1회 실행한다.
# 시딩 트래픽이 SUT 앱을 거치지 않고(JDBC 직행) 측정과 분리되며, 같은 seed 면 같은 데이터가 나온다.
# 빈 DB(golden 골격만 있는 상태)를 전제한다 — 재실행은 destroy/apply 또는 snapshot 복원 후에.
seed_bulk() {
  local sut_ip count seed ssh_user
  sut_ip="$(terraform -chdir="$TF_DIR" output -raw sut_public_ip)"
  count="${BULK_MEMBER_COUNT:-100000}"
  seed="${BULK_RANDOM_SEED:-42}"
  ssh_user="${SSH_USER:-ec2-user}"
  local ssh_opts=(-o StrictHostKeyChecking=accept-new)
  if [ -n "${SSH_KEY:-}" ]; then
    ssh_opts+=(-i "$SSH_KEY")
  fi

  echo "[bulk] $ssh_user@$sut_ip 에서 seeder 컨테이너 실행 (members=$count, seed=$seed)"
  # app.env(dev profile·RDS 접속)를 그대로 쓰되 -e 오버라이드로 seeder 프로파일·웹서버 off 를 얹는다.
  # 이미지·네트워크는 하드코딩하지 않고 "실행 중인 app 컨테이너"에서 그대로 뽑는다
  # (valkey 호스트를 compose 서비스명으로 해석하려면 같은 네트워크에 붙어야 하기 때문).
  ssh "${ssh_opts[@]}" "$ssh_user@$sut_ip" "sudo bash -s" <<REMOTE
set -euo pipefail
cd /opt/umc
APP_CID="\$(docker compose ps -q app)"
[ -n "\$APP_CID" ] || { echo "app 컨테이너가 실행 중이 아닙니다 — 앱 기동(compose up)·health 확인 후 다시 실행하세요" >&2; exit 1; }
IMAGE="\$(docker inspect "\$APP_CID" | jq -r '.[0].Config.Image')"
NETWORK="\$(docker inspect "\$APP_CID" | jq -r '.[0].NetworkSettings.Networks | keys[0]')"
echo "[bulk-remote] image=\$IMAGE network=\$NETWORK"
# 같은 태그로 새 이미지를 push 했을 수 있으니 항상 최신을 pull 한다 (시더 수정이 로컬 캐시에 가려지는 것 방지)
REGISTRY="\${IMAGE%%/*}"
REGION="\$(echo "\$REGISTRY" | sed -n 's/.*\.dkr\.ecr\.\([a-z0-9-]*\)\.amazonaws\.com/\1/p')"
aws --region "\$REGION" ecr get-login-password | docker login --username AWS --password-stdin "\$REGISTRY" >/dev/null
docker pull "\$IMAGE" >/dev/null
install -d -m 777 /opt/umc/seed-out
docker run --rm --network "\$NETWORK" --env-file /opt/umc/app.env \
  -e SPRING_PROFILES_ACTIVE=dev,seeder \
  -e APP_BULK_SEED_MEMBER_COUNT=$count \
  -e APP_BULK_SEED_RANDOM_SEED=$seed \
  -e APP_BULK_SEED_SEED_JSON_PATH=/seed-out/seed.json \
  -v /opt/umc/seed-out:/seed-out \
  "\$IMAGE"
chmod a+r /opt/umc/seed-out/seed.json
REMOTE

  scp "${ssh_opts[@]}" "$ssh_user@$sut_ip:/opt/umc/seed-out/seed.json" "$SEED_JSON"
  echo "[bulk] wrote $SEED_JSON"
}

case "$SEED_STRATEGY" in
api)
  BASE_URL="$(resolve_base_url)"
  seed_api
  ;;
bulk)
  seed_bulk
  ;;
snapshot)
  echo "snapshot 전략은 prepare-data.sh 가 아니라 rds.tf 의 snapshot_identifier 로 다룹니다." >&2
  exit 1
  ;;
*)
  echo "알 수 없는 SEED_STRATEGY: $SEED_STRATEGY (api|bulk|snapshot)" >&2
  exit 1
  ;;
esac
