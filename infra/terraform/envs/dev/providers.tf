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
