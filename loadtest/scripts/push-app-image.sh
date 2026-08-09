#!/usr/bin/env bash
set -euo pipefail

# 앱 이미지를 빌드해 ECR 로 push 한다 — SUT 와 bulk 시더가 pull 하는 그 이미지.
# 빌드 자체는 범용 scripts/build-app-image.sh 를 재사용하고, 여기는 ECR 로그인·tag·push 만 더한다.
#
# 사용법: loadtest/scripts/push-app-image.sh [IMAGE_REF]
#   IMAGE_REF 미지정 시 loadtest/terraform/terraform.tfvars 의 app_image 를 그대로 쓴다.
#   예: loadtest/scripts/push-app-image.sh <acct>.dkr.ecr.ap-northeast-2.amazonaws.com/umc-product-server:load-test
#
# 환경변수:
#   AWS_PROFILE : 미설정이면 tfvars 의 aws_profile 을 쓴다 (있을 때만)
#   PLATFORM    : 기본 linux/arm64 — 기본 SUT 가 t4g(arm64)라서. x86 SUT 면 linux/amd64 로 넘긴다.
#   SKIP_GRADLE : 1 이면 bootJar 생략 (build-app-image.sh 로 전달)

REPO_ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
TFVARS="$REPO_ROOT/loadtest/terraform/terraform.tfvars"

tfvar() { # tfvars 에서 문자열 변수 값 추출 (없으면 빈 문자열)
  [ -f "$TFVARS" ] || return 0
  sed -n "s/^$1[[:space:]]*=[[:space:]]*\"\(.*\)\"/\1/p" "$TFVARS" | head -1
}

# 1) 대상 이미지 ref 결정 (인자 > tfvars app_image)
IMAGE_REF="${1:-$(tfvar app_image)}"
[ -n "$IMAGE_REF" ] || {
  echo "IMAGE_REF 를 인자로 주거나 terraform.tfvars 에 app_image 를 설정하세요" >&2
  exit 1
}

REGISTRY="${IMAGE_REF%%/*}"
REPO_AND_TAG="${IMAGE_REF#*/}"
REPO="${REPO_AND_TAG%%:*}"
REGION="$(echo "$REGISTRY" | sed -n 's/.*\.dkr\.ecr\.\([a-z0-9-]*\)\.amazonaws\.com/\1/p')"
[ -n "$REGION" ] || {
  echo "ECR registry 형식이 아닙니다: $REGISTRY (load-test 는 ECR 전용)" >&2
  exit 1
}

# AWS_PROFILE: env 우선, 없으면 tfvars 의 aws_profile
if [ -z "${AWS_PROFILE:-}" ]; then
  profile="$(tfvar aws_profile)"
  if [ -n "$profile" ]; then
    export AWS_PROFILE="$profile"
  fi
fi

# 2) ECR repo 존재 확인 — terraform 은 repo 를 만들지 않는다(리그 밖 수명).
aws ecr describe-repositories --region "$REGION" --repository-names "$REPO" >/dev/null 2>&1 || {
  echo "ECR repository '$REPO' 가 없습니다. 먼저 생성하세요:" >&2
  echo "  aws ecr create-repository --region $REGION --repository-name $REPO" >&2
  exit 1
}

# 3) 빌드 — 기본 SUT 가 t4g.small(arm64)이므로 PLATFORM 기본값도 arm64 로 강제한다.
PLATFORM="${PLATFORM:-linux/arm64}"
echo "▶ build ($PLATFORM) → push $IMAGE_REF" >&2
LOCAL_IMG="$(PLATFORM="$PLATFORM" SKIP_GRADLE="${SKIP_GRADLE:-0}" "$REPO_ROOT/scripts/build-app-image.sh" | tail -n1)"

# 4) 로그인 → tag → push
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"
docker tag "$LOCAL_IMG" "$IMAGE_REF"
docker push "$IMAGE_REF"

echo "✅ pushed: $IMAGE_REF"
