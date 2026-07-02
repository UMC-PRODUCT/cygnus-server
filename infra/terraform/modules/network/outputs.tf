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
  value = [
    aws_subnet.this["server-subnet-pub-2a"].id,
    aws_subnet.this["server-subnet-pub-2b"].id
  ]
}

output "public_app_subnet_ids" {
  description = "Public app subnet IDs used by current ASG launch templates."
  value = [
    aws_subnet.this["server-subnet-pub-app-2a"].id,
    aws_subnet.this["server-subnet-pub-app-2b"].id
  ]
}

output "private_app_subnet_ids" {
  description = "Private app subnet IDs for future hardening."
  value = [
    aws_subnet.this["server-subnet-pri-app-2a"].id,
    aws_subnet.this["server-subnet-pri-app-2b"].id
  ]
}

output "private_db_subnet_ids" {
  description = "Private DB subnet IDs ordered by AZ."
  value = [
    aws_subnet.this["server-subnet-pri-db-2a"].id,
    aws_subnet.this["server-subnet-pri-db-2b"].id
  ]
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
