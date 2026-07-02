variable "aws_region" {
  type        = string
  description = "AWS region."
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
  default     = "prod"
}

variable "shared_state_bucket" {
  type        = string
  description = "shared Terraform state가 저장된 S3 bucket."
}

variable "shared_state_key" {
  type        = string
  description = "shared Terraform state S3 key."
  default     = "umc-product-server/shared/terraform.tfstate"
}

variable "shared_state_region" {
  type        = string
  description = "shared Terraform state가 있는 AWS region."
  default     = "ap-northeast-2"
}

variable "shared_state_profile" {
  type        = string
  description = "shared Terraform state를 읽을 때 사용할 선택적 AWS CLI profile."
  default     = null
  nullable    = true
}

variable "jwt_key_ring_secret_arn" {
  type        = string
  description = "Secrets Manager ARN that stores JWT key rings separated by purpose."
}

variable "oauth_client_secret_arn" {
  type        = string
  description = "Secrets Manager ARN that stores OAuth provider client secrets."
}

variable "data_protection_secret_arn" {
  type        = string
  description = "Secrets Manager ARN that stores long-lived data protection keys such as FIGMA_TOKEN_ENCRYPTION_KEY."
}

variable "database_secret_arn" {
  type        = string
  description = "Secrets Manager ARN for database credentials or managed RDS master credential."
}

variable "runtime_secret_kms_key_arn" {
  type        = string
  description = "KMS key ARN that decrypts prod runtime secret bundles."
}

variable "image_tag" {
  type        = string
  description = "prod ASG에 배포할 container image tag."
}

variable "rds_master_secret_arn" {
  type        = string
  description = "prod RDS 관리형 master credential에 사용할 Secrets Manager ARN."
  default     = null
  nullable    = true
}
