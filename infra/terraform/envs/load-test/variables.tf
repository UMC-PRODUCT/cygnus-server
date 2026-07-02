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

# ── 앱 이미지 / 배포 ─────────────────────────────────────────
# app_image 는 이미 registry 에 push 된 dev profile 실행 가능 이미지를 가리켜야 한다.
# Terraform 은 이미지를 빌드하지 않고 EC2 에서 docker compose pull 만 수행한다.
# 기본 SUT 가 t4g.small(arm64)이므로 이미지가 linux/arm64 또는 multi-arch 로 push 되어 있어야 한다.
variable "app_image" {
  description = "앱 Docker 이미지 (예: <account>.dkr.ecr.ap-northeast-2.amazonaws.com/umc-product-server:development-latest)"
  type        = string
}

variable "registry_type" {
  description = "이미지 레지스트리 인증 방식. ecr이면 EC2 IAM role 로 로그인하고, generic이면 SSM credential parameter 를 사용한다."
  type        = string
  default     = "ecr"

  validation {
    condition     = contains(["ecr", "generic", "public"], var.registry_type)
    error_message = "registry_type 은 ecr, generic, public 중 하나여야 합니다."
  }
}

variable "registry_server" {
  description = "컨테이너 레지스트리 호스트. registry_type=ecr 이고 빈 문자열이면 현재 AWS 계정 ECR registry 로 계산한다."
  type        = string
  default     = ""
}

variable "ecr_repository_name" {
  description = "registry_type=ecr 일 때 SUT EC2 role 에 pull 권한을 줄 ECR repository 이름"
  type        = string
  default     = "umc-product-server"
}

variable "registry_credentials_ssm_parameter_name" {
  description = "registry_type=generic 일 때 Docker registry credential JSON 을 담은 SSM SecureString parameter name. ECR/public image면 빈 문자열."
  type        = string
  default     = ""

  validation {
    condition     = var.registry_credentials_ssm_parameter_name == "" || startswith(var.registry_credentials_ssm_parameter_name, "/")
    error_message = "registry_credentials_ssm_parameter_name 은 빈 문자열이거나 / 로 시작하는 SSM parameter name 이어야 합니다."
  }
}

variable "git_repo_url" {
  description = "compose/관측 config 와 k6 스크립트를 가져올 레포 URL. private repo면 read-only 토큰을 포함한 HTTPS URL."
  type        = string

  # monitoring EC2 는 infra/monitoring/grafana 를, generator EC2 는 docs/guides/load-test/k6 를 clone 해서 사용한다.
  # 지금 구조에서는 이 값이 비면 EC2 가 필요한 파일을 받을 수 없다.
  validation {
    condition     = length(trimspace(var.git_repo_url)) > 0
    error_message = "git_repo_url 은 필수입니다. 현재 구조는 EC2 user-data 에서 레포를 clone 해 monitoring config 와 k6 script 를 가져옵니다."
  }
}

variable "app_env_ssm_parameter_path" {
  description = <<-EOT
    앱 런타임 env(app.env) 내용을 담은 AWS SSM Parameter Store path.
    DATABASE_URL/USERNAME/PASSWORD · OTEL_URL · SPRING_PROFILES_ACTIVE · HIKARI_MAX_POOL_SIZE 는
    user_data 가 부하 테스트 값으로 덮어쓰므로 SSM path 에는 그 외 부팅 필수 env(외부 연동 등)를 넣는다.
    빈 문자열이면 빈 app.env 에 부하 테스트 오버라이드만 추가한다.
  EOT
  type        = string
  default     = ""

  validation {
    condition     = var.app_env_ssm_parameter_path == "" || startswith(var.app_env_ssm_parameter_path, "/")
    error_message = "app_env_ssm_parameter_path 는 빈 문자열이거나 / 로 시작하는 SSM parameter path 이어야 합니다."
  }
}

variable "ssm_kms_key_arn" {
  description = "SSM SecureString 에 customer managed KMS key를 사용할 때 SUT EC2 role에 decrypt 권한을 줄 KMS key ARN. AWS managed key를 쓰면 빈 문자열."
  type        = string
  default     = ""

  validation {
    condition     = var.ssm_kms_key_arn == "" || can(regex("^arn:aws[a-zA-Z-]*:kms:", var.ssm_kms_key_arn))
    error_message = "ssm_kms_key_arn 은 빈 문자열이거나 KMS key ARN 이어야 합니다."
  }
}
