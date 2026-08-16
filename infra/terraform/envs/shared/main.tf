# shared root 소유 범위:
# - network: VPC, subnet, route table, internet gateway, VPC endpoint
# - security: ALB/ASG/DB security group과 rule
# - alb: ALB, listener, listener rule, target group
# - dns: managed Route53 record
#
# 이후 task에서 alb/dns module block을 추가한다.

module "network" {
  source      = "../../modules/network"
  name_prefix = "server"
  environment = var.environment
  vpc_cidr    = "10.0.0.0/16"

  subnets = {
    server-subnet-pub-2a = {
      cidr                    = "10.0.0.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = true
      route_table             = "public"
      role                    = "public-alb"
    }
    server-subnet-pub-2b = {
      cidr                    = "10.0.1.0/24"
      availability_zone       = "ap-northeast-2b"
      map_public_ip_on_launch = false
      route_table             = "public"
      role                    = "public-alb"
    }
    # ASG launch template ENI가 public IP를 명시적으로 연결하므로 subnet auto-assign은 기존 실사값 false를 유지한다.
    server-subnet-pub-app-2a = {
      cidr                    = "10.0.30.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = false
      route_table             = "public"
      role                    = "public-app"
    }
    # ASG launch template ENI가 public IP를 명시적으로 연결하므로 subnet auto-assign은 기존 실사값 false를 유지한다.
    server-subnet-pub-app-2b = {
      cidr                    = "10.0.31.0/24"
      availability_zone       = "ap-northeast-2b"
      map_public_ip_on_launch = false
      route_table             = "public"
      role                    = "public-app"
    }
    server-subnet-pri-app-2a = {
      cidr                    = "10.0.10.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = false
      route_table             = "private"
      role                    = "private-app"
    }
    server-subnet-pri-app-2b = {
      cidr                    = "10.0.11.0/24"
      availability_zone       = "ap-northeast-2b"
      map_public_ip_on_launch = false
      route_table             = "private"
      role                    = "private-app"
    }
    server-subnet-pri-db-2a = {
      cidr                    = "10.0.20.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = false
      route_table             = "private"
      role                    = "private-db"
    }
    server-subnet-pri-db-2b = {
      cidr                    = "10.0.21.0/24"
      availability_zone       = "ap-northeast-2b"
      map_public_ip_on_launch = false
      route_table             = "private"
      role                    = "private-db"
    }
    server-subnet-pub-ec2-2a = {
      cidr                    = "10.0.2.0/24"
      availability_zone       = "ap-northeast-2a"
      map_public_ip_on_launch = true
      route_table             = "unmanaged"
      role                    = "unmanaged"
    }
  }
}

module "security" {
  source = "../../modules/security"

  vpc_id                     = module.network.vpc_id
  vpc_cidr                   = "10.0.0.0/16"
  allowed_ssh_cidrs          = ["116.124.253.97/32"]
  allow_world_ssh_for_parity = false
}

module "alb" {
  source = "../../modules/alb"

  vpc_id                = module.network.vpc_id
  public_subnet_ids     = module.network.public_subnet_ids
  alb_security_group_id = module.security.alb_security_group_id
  certificate_arn       = var.acm_certificate_arn
  dev_host_headers      = ["dev.api.university.neordinary.com"]
  prod_host_headers     = ["api.university.neordinary.com"]
}
#
# module "dns" {
#   source      = "../../modules/dns"
#   environment = var.environment
# }
