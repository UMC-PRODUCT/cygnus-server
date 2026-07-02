provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = {
      Project     = "umc-product-server"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

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
  description = "shared, dev, prod 같은 환경 이름."
}
