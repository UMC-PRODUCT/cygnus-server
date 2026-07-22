# ── 리전 / AZ ────────────────────────────────────────────────
# 모든 기본값은 서울 리전 기준이다. 다른 리전에서 실행하면 AMI, AZ, RDS engine version 가용성을 같이 확인해야 한다.
variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "aws_profile" {
  type        = string
  description = "Terraform 실행에 사용할 AWS CLI profile. null이면 AWS provider 기본 credential chain을 사용한다."
  default     = null
  nullable    = true
}

variable "environment" {
  type        = string
  description = "환경 이름."
  default     = "load-test"
}

variable "az" {
  description = "EC2와 RDS primary를 둘 AZ. 네트워크 레이턴시 변수를 줄이기 위해 한 AZ에 모은다."
  type        = string
  default     = "ap-northeast-2a"
}

variable "az_secondary" {
  description = "RDS subnet group 요건(>=2 AZ)을 위한 보조 AZ. EC2는 두지 않는다."
  type        = string
  default     = "ap-northeast-2c"
}

variable "monitoring_ami_ssm_parameter" {
  description = "Monitoring EC2 AMI SSM 파라미터. 기본값은 x86_64 Amazon Linux 2023."
  type        = string
  default     = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

variable "sut_ami_ssm_parameter" {
  description = "SUT EC2 AMI SSM 파라미터. t4g.small 같은 Graviton 인스턴스를 쓰므로 기본값은 arm64."
  type        = string
  default     = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64"
}

variable "generator_ami_ssm_parameter" {
  description = "Generator EC2 AMI SSM 파라미터. 기본 generator_instance_type(c5.large)에 맞춰 x86_64."
  type        = string
  default     = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# ── 접근 ─────────────────────────────────────────────────────
# allowed_cidr 은 public endpoint 의 유일한 외부 접근 제어다.
# 0.0.0.0/0 으로 열면 Grafana/Prometheus/SUT debug port 가 인터넷에 노출되므로 금지한다.
variable "key_name" {
  description = "기존 EC2 키페어 이름 (SSH 접속용)"
  type        = string
}

variable "allowed_cidr" {
  description = "SSH·Grafana 접근 허용 CIDR (예: 내 공인 IP/32)"
  type        = string
}

# ── 인스턴스 타입 ────────────────────────────────────────────
# SUT 타입은 측정 대상 스펙 자체라 임의로 키우면 안 된다.
# 반대로 generator/monitoring 은 측정 대상이 아니므로 먼저 병목나지 않게 SUT보다 여유 있게 잡는다.
variable "sut_instance_type" {
  description = "SUT(앱) — prod 앱과 동일 타입으로 (현재 스펙 fidelity 핵심)"
  type        = string
  default     = "t4g.small"
}

variable "generator_instance_type" {
  description = "k6 부하 생성기 — 생성기가 먼저 병목나지 않게 넉넉히"
  type        = string
  default     = "c5.large"
}

variable "monitoring_instance_type" {
  description = "관측 스택(otel-collector+prom+tempo+loki+grafana)"
  type        = string
  default     = "t3.large"
}

# ── RDS ──────────────────────────────────────────────────────
# 이 블록은 "측정하고 싶은 DB 스펙"을 정의한다. 현재 이슈 기준 목표는 db.t4g.small 이다.
variable "db_instance_class" {
  description = "RDS 인스턴스 클래스 (측정 대상 DB 스펙)"
  type        = string
  default     = "db.t4g.small"
}

variable "db_engine_version" {
  description = "RDS PostgreSQL 버전. apply 전에 해당 리전에서 가용한 정확한 버전 문자열을 확인한다."
  type        = string
  default     = "18.3"
}

variable "db_allocated_storage" {
  description = "RDS 스토리지(GB)"
  type        = number
  default     = 50
}

variable "db_name" {
  description = "앱이 JDBC URL 에서 접속할 DB 이름. prod 콘솔 DB name '-'는 초기 DB 없음이라는 뜻이라 실제 앱 접속 DB명을 확인해야 한다."
  type        = string
  default     = "umc_product"
}

variable "db_username" {
  description = "RDS master username. 제공된 prod 값 기준 기본값은 postgres."
  type        = string
  default     = "postgres"
}

# 시딩 Tier 2 (opt-in 캐시): 미리 구운 DB snapshot 에서 복원한다.
# 빈 문자열이면 새 빈 RDS 를 만들고 api 시더로 채운다(Tier 1, 기본).
# snapshot id 를 넣으면 apply 시 그 snapshot 을 복원해 시딩 없이 즉시 대규모 데이터로 시작한다.
# 스키마(Flyway)나 시드 모양이 바뀌면 snapshot 을 다시 구워야 한다(stale).
variable "db_snapshot_identifier" {
  description = "복원할 RDS snapshot 식별자. 빈 문자열이면 새 빈 인스턴스를 만든다. (대규모 반복 실행용 opt-in 캐시)"
  type        = string
  default     = ""
}

# ── 앱 이미지 / 배포 ─────────────────────────────────────────
# app_image 는 이미 registry 에 push 된 dev profile 실행 가능 이미지를 가리켜야 한다.
# Terraform 은 이미지를 빌드하지 않고 EC2 에서 docker compose pull 만 수행한다.
# 기본 SUT 가 t4g.small(arm64)이므로 이미지가 linux/arm64 또는 multi-arch 로 push 되어 있어야 한다.
variable "app_image" {
  description = "앱 Docker 이미지 (예: <account>.dkr.ecr.ap-northeast-2.amazonaws.com/umc-product-server:development-latest)"
  type        = string
}

# load-test 는 앱 이미지를 현재 AWS 계정 ECR 에서 pull 한다 (ECR 전용).
# generic/public registry 는 v1 범위 밖 — 필요해지면 그때 재도입한다.
variable "ecr_repository_name" {
  description = "SUT EC2 role 에 ECR pull 권한을 줄 ECR repository 이름"
  type        = string
  default     = "umc-product-server"
}

variable "git_repo_url" {
  description = "compose/관측 config 와 k6 스크립트를 가져올 레포 URL. private repo면 read-only 토큰을 포함한 HTTPS URL."
  type        = string

  # monitoring EC2 는 infra/monitoring/grafana compose 를 clone 해서 사용한다. (generator 는 더 이상 repo 를 clone 하지 않는다 — k6 는 로컬 sync-k6.sh 로 올린다.)
  # 이 값이 비면 monitoring EC2 가 grafana config 를 받을 수 없다.
  validation {
    condition     = length(trimspace(var.git_repo_url)) > 0
    error_message = "git_repo_url 은 필수입니다. monitoring EC2 user-data 가 레포를 clone 해 grafana config 를 가져옵니다."
  }
}

# ── 앱 런타임 env (로컬 파일 주입) ────────────────────────────
# load-test 전용 비밀값(JWT/OAuth/암호화 키 등)은 로컬 load-test.env 파일에 넣고 Terraform 에는 경로만 넘긴다.
# DB/OTEL/Hikari/dev profile 값은 여기 넣지 않는다 — user-data 가 부하 테스트 조건으로 덮어쓴다.
variable "app_env_file_path" {
  description = "SUT app.env 로 주입할 로컬 env 파일 경로. load-test.env.example 을 복사해 load-test.env 를 만든 뒤 실행한다."
  type        = string
  default     = "./load-test.env"

  validation {
    condition     = fileexists(var.app_env_file_path)
    error_message = "app_env_file_path 파일이 존재해야 합니다. load-test.env.example 을 복사해 load-test.env 를 만든 뒤 실행하세요."
  }
}
