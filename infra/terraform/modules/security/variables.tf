variable "vpc_id" {
  type        = string
  description = "VPC ID where security groups are created."
  nullable    = false
}

variable "allowed_ssh_cidrs" {
  type        = list(string)
  description = "CIDR blocks allowed to access EC2 instances over SSH."
  default     = ["116.124.253.97/32"]

  validation {
    condition = alltrue([
      for cidr in var.allowed_ssh_cidrs : can(cidrnetmask(cidr))
    ])
    error_message = "allowed_ssh_cidrs must contain valid CIDR blocks."
  }
}

variable "allow_world_ssh_for_parity" {
  type        = bool
  description = "Whether to include 0.0.0.0/0 in SSH ingress for temporary import parity."
  default     = false
}
