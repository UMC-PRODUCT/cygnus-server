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
  default     = "dev"
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

variable "dev_secret_source" {
  type        = string
  description = "dev runtime secret source. local_env_file is the default for home-server or local development."
  default     = "local_env_file"

  validation {
    condition     = contains(["local_env_file", "ssm_parameter_store", "secrets_manager"], var.dev_secret_source)
    error_message = "dev_secret_source must be one of local_env_file, ssm_parameter_store, secrets_manager."
  }
}

variable "dev_runtime_env_file_path" {
  type        = string
  description = "Path to a root-only env file on the dev or home-server host."
  default     = "/etc/umc-product/dev.env"
}

variable "dev_ssm_parameter_path" {
  type        = string
  description = "Optional SSM Parameter Store path for shared dev secrets."
  default     = null
  nullable    = true
}

variable "dev_runtime_secret_arn" {
  type        = string
  description = "Optional Secrets Manager ARN when dev intentionally tests AWS secret hydration."
  default     = null
  nullable    = true
}

variable "dev_runtime_secret_kms_key_arn" {
  type        = string
  description = "Optional KMS key ARN for dev Secrets Manager hydration."
  default     = null
  nullable    = true
}

variable "image_tag" {
  type        = string
  description = "dev ASG에 배포할 container image tag."
}
