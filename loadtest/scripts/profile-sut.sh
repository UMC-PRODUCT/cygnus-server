#!/usr/bin/env bash
set -euo pipefail

# 부하가 걸린 상태의 SUT 앱 컨테이너를 async-profiler 로 떠서 flame graph 를 회수한다.
# Grafana 는 "CPU 가 얼마나" 까지 답하고, "누가 태우나" 는 이 스크립트가 답한다.
#
# 사용법: loadtest/scripts/profile-sut.sh [초] [이벤트...]
#   예: loadtest/scripts/profile-sut.sh              # 60초, cpu + wall
#       loadtest/scripts/profile-sut.sh 30 cpu       # 30초, cpu 만
#       loadtest/scripts/profile-sut.sh 60 wall      # 대기 시간 분해만
#   SSH_KEY / SSH_USER : run-k6.sh 와 동일 관례.
#   RUN_DIR : (선택) 결과 저장 디렉터리. 미설정 시 loadtest/k6/out.
#
# 실행 타이밍: breakpoint 가 아니라 constant(load/stress) 프로파일로 부하를 고정하고,
# Grafana 에서 포화가 안착한 뒤에 뜬다. 저부하 구간 샘플이 섞이면 판정이 흐려진다.
# 이벤트 선택: CPU 포화면 cpu, Hikari pending 이 쌓이면 wall(대기까지 보임).

REPO_ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
TF_DIR="$REPO_ROOT/loadtest/terraform"
SSH_USER="${SSH_USER:-ec2-user}"

DURATION="${1:-60}"
shift || true
EVENTS=("$@")
if [ ${#EVENTS[@]} -eq 0 ]; then
  EVENTS=(cpu wall)
fi

SUT_IP="$(terraform -chdir="$TF_DIR" output -raw sut_public_ip)"
SSH_OPTS="-o StrictHostKeyChecking=accept-new"
if [ -n "${SSH_KEY:-}" ]; then
  SSH_OPTS="$SSH_OPTS -i $SSH_KEY"
fi

DEST_DIR="${RUN_DIR:-$REPO_ROOT/loadtest/k6/out}"
mkdir -p "$DEST_DIR"
STAMP="$(date +%F-%H%M%S)"

echo "[profile] $SSH_USER@$SUT_IP — ${DURATION}s × ${EVENTS[*]}"

# shellcheck disable=SC2029  # 인자를 원격에서 전개하는 것이 의도다.
ssh $SSH_OPTS "$SSH_USER@$SUT_IP" "sudo bash -s '$DURATION' '$STAMP' '${EVENTS[*]}'" <<'REMOTE'
set -euo pipefail
DURATION="$1"; STAMP="$2"; EVENTS="$3"

CID="$(docker ps -qf name=app)"
[ -n "$CID" ] || { echo "app 컨테이너 없음 — 앱이 떠 있는지 확인" >&2; exit 1; }

# 프로파일러는 컨테이너 안에서 실행한다(호스트에서 붙여도 .so 는 타겟 프로세스가 dlopen 한다).
# 이미 있으면 건너뛴다 — 인스턴스를 새로 만들었을 때만 내려받는다.
if ! docker exec "$CID" /tmp/ap/bin/asprof --version >/dev/null 2>&1; then
  case "$(uname -m)" in
    aarch64) ARCH=linux-arm64 ;;
    x86_64)  ARCH=linux-x64 ;;
    *) echo "지원하지 않는 아키텍처: $(uname -m)" >&2; exit 1 ;;
  esac
  URL="$(curl -s https://api.github.com/repos/async-profiler/async-profiler/releases/latest \
         | grep -o "https://[^\"]*-$ARCH\.tar\.gz" | grep -v debug | head -1)"
  [ -n "$URL" ] || { echo "async-profiler 릴리스 URL 을 못 찾음" >&2; exit 1; }
  echo "[profile] 설치: $URL"
  # /tmp 는 sticky 라 fs.protected_regular 때문에 남의 소유 파일을 덮어쓸 수 없다. 매번 새 파일로 받는다.
  TARBALL="$(mktemp)"
  curl -fsSL "$URL" -o "$TARBALL"
  rm -rf /tmp/ap && mkdir -p /tmp/ap && tar xzf "$TARBALL" -C /tmp/ap --strip-components=1
  rm -f "$TARBALL"
  docker cp /tmp/ap "$CID:/tmp/ap"
  # 앱은 비루트(spring)로 돌고, attach 는 타겟과 같은 UID 여야 한다.
  docker exec -u root "$CID" chown -R "$(docker exec "$CID" id -u):$(docker exec "$CID" id -g)" /tmp/ap
fi

for EV in $EVENTS; do
  OUT="/tmp/flame-$EV-$STAMP.html"
  # wall 은 스레드별로 나눠야 대기 중인 스레드가 구분된다.
  EXTRA=""; [ "$EV" = "wall" ] && EXTRA="-t"
  echo "[profile] -e $EV ${DURATION}s"
  docker exec "$CID" /tmp/ap/bin/asprof -d "$DURATION" -e "$EV" $EXTRA -f "$OUT" 1
  docker cp "$CID:$OUT" "$OUT"
  chmod 644 "$OUT"
done
REMOTE

for EV in "${EVENTS[@]}"; do
  scp $SSH_OPTS "$SSH_USER@$SUT_IP:/tmp/flame-$EV-$STAMP.html" "$DEST_DIR/" \
    || echo "(flame-$EV-$STAMP.html 회수 실패)" >&2
done

echo "결과 저장: $DEST_DIR/flame-*-$STAMP.html — 브라우저로 열면 인터랙티브(검색·확대) 동작"
echo "읽는 법: X축 폭 = 샘플 비율(= CPU 몫). 높이·색은 의미 없음. 우상단 검색으로 Jackson/hibernate 등 용의자 폭 확인."
