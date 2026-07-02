variable "name_prefix" {
  type        = string
  description = "Network resource name prefix."
  nullable    = false
}

variable "environment" {
  type        = string
  description = "Environment name."
  nullable    = false
}

variable "vpc_cidr" {
  type        = string
  description = "VPC CIDR block."
  nullable    = false

  validation {
    condition     = can(cidrnetmask(var.vpc_cidr))
    error_message = "vpc_cidr must be a valid CIDR block."
  }
}

variable "subnets" {
  type = map(object({
    cidr                    = string
    availability_zone       = string
    map_public_ip_on_launch = bool
    route_table             = string
  }))
  description = "Subnet definitions keyed by subnet Name tag. route_table unmanaged preserves subnets without explicit route table association."
  nullable    = false

  validation {
    condition = alltrue([
      for subnet in var.subnets : contains(["public", "private", "unmanaged"], subnet.route_table)
    ])
    error_message = "subnets[*].route_table must be public, private, or unmanaged."
  }

  validation {
    condition = alltrue([
      for subnet in var.subnets : can(cidrnetmask(subnet.cidr))
    ])
    error_message = "subnets[*].cidr must be valid CIDR blocks."
  }
}

variable "enable_s3_gateway_endpoint" {
  type        = bool
  description = "Whether to create an S3 gateway VPC endpoint for private route table."
  default     = true
}
