locals {
  subnet_ids_by_role = {
    for role in toset([for subnet in values(var.subnets) : subnet.role]) : role => [
      for subnet_key in sort([
        for name, subnet in var.subnets : "${subnet.availability_zone}|${name}"
        if subnet.role == role
      ]) : aws_subnet.this[split("|", subnet_key)[1]].id
    ]
  }
}

output "vpc_id" {
  description = "VPC ID."
  value       = aws_vpc.this.id
}

output "subnet_ids" {
  description = "Subnet IDs keyed by subnet Name tag."
  value       = { for name, subnet in aws_subnet.this : name => subnet.id }
}

output "public_subnet_ids" {
  description = "Public ALB subnet IDs ordered by AZ."
  value       = try(local.subnet_ids_by_role["public-alb"], [])
}

output "public_app_subnet_ids" {
  description = "Public app subnet IDs used by current ASG launch templates."
  value       = try(local.subnet_ids_by_role["public-app"], [])
}

output "private_app_subnet_ids" {
  description = "Private app subnet IDs for future hardening."
  value       = try(local.subnet_ids_by_role["private-app"], [])
}

output "private_db_subnet_ids" {
  description = "Private DB subnet IDs ordered by AZ."
  value       = try(local.subnet_ids_by_role["private-db"], [])
}

output "public_route_table_id" {
  description = "Public route table ID."
  value       = aws_route_table.public.id
}

output "private_route_table_id" {
  description = "Private route table ID."
  value       = aws_route_table.private.id
}

output "s3_gateway_endpoint_id" {
  description = "S3 gateway endpoint ID, or null when disabled."
  value       = try(aws_vpc_endpoint.s3[0].id, null)
}
