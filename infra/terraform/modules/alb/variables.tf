variable "vpc_id" {
  type        = string
  description = "VPC ID where ALB target groups are created."
  nullable    = false
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnet IDs where the internet-facing ALB is attached."
  nullable    = false

  validation {
    condition     = length(var.public_subnet_ids) >= 2
    error_message = "public_subnet_ids must contain at least two subnet IDs."
  }
}

variable "alb_security_group_id" {
  type        = string
  description = "Security group ID attached to the ALB."
  nullable    = false
}

variable "certificate_arn" {
  type        = string
  description = "ACM certificate ARN used by the HTTPS listener."
  nullable    = false
}

variable "dev_host_headers" {
  type        = set(string)
  description = "Host headers routed to the dev EC2 target group."
  nullable    = false

  validation {
    condition = alltrue([
      for host in var.dev_host_headers : host == "dev.api.university.neordinary.com"
    ])
    error_message = "dev_host_headers may only contain dev.api.university.neordinary.com."
  }
}

variable "prod_host_headers" {
  type        = set(string)
  description = "Host headers routed to the prod EC2 target group."
  nullable    = false

  validation {
    condition = alltrue([
      for host in var.prod_host_headers : host == "api.university.neordinary.com"
    ])
    error_message = "prod_host_headers may only contain api.university.neordinary.com."
  }
}

variable "enable_deletion_protection" {
  type        = bool
  description = "Whether deletion protection is enabled on the ALB."
  default     = false
}

variable "idle_timeout_seconds" {
  type        = number
  description = "ALB idle timeout in seconds."
  default     = 60

  validation {
    condition     = var.idle_timeout_seconds >= 1 && var.idle_timeout_seconds <= 4000
    error_message = "idle_timeout_seconds must be between 1 and 4000."
  }
}
