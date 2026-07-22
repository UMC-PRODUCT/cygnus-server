# load-test v1 은 로컬 state 를 쓴다. (S3 backend + lock 은 후속 개선)
# destroy 전제의 ephemeral 환경이라 로컬로 충분하고, state 에 secret 이 남으므로 커밋 금지(.gitignore 가 *.tfstate 차단).
terraform {
  backend "local" {}
}
