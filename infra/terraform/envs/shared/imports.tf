# Ownership: shared는 VPC, subnet, route table, internet gateway, VPC endpoint,
# security group, ALB, listener, listener rule, target group, 그리고
# managed Route53 record를 import하고 소유한다.

import {
  to = module.network.aws_vpc.this
  id = "vpc-0101ab9cb36353cc5"
}

import {
  to = module.network.aws_internet_gateway.this
  id = "igw-00a6087e41662b26b"
}

import {
  to = module.network.aws_subnet.this["server-subnet-pub-2a"]
  id = "subnet-03b5e95c0297bcfeb"
}

import {
  to = module.network.aws_subnet.this["server-subnet-pub-2b"]
  id = "subnet-0f578df7c07593340"
}

import {
  to = module.network.aws_subnet.this["server-subnet-pub-app-2a"]
  id = "subnet-0f1e6fca9afdf0eb7"
}

import {
  to = module.network.aws_subnet.this["server-subnet-pub-app-2b"]
  id = "subnet-0694296b62c8dc7e7"
}

import {
  to = module.network.aws_subnet.this["server-subnet-pri-app-2a"]
  id = "subnet-0996146733a8eb8c1"
}

import {
  to = module.network.aws_subnet.this["server-subnet-pri-app-2b"]
  id = "subnet-0f732a7943b67f494"
}

import {
  to = module.network.aws_subnet.this["server-subnet-pri-db-2a"]
  id = "subnet-0e46fa453b5318c12"
}

import {
  to = module.network.aws_subnet.this["server-subnet-pri-db-2b"]
  id = "subnet-002bbfa36ff0f7b15"
}

import {
  to = module.network.aws_subnet.this["server-subnet-pub-ec2-2a"]
  id = "subnet-0caf472e4677bd195"
}

import {
  to = module.network.aws_route_table.public
  id = "rtb-0339f36c1ea252ba0"
}

import {
  to = module.network.aws_route_table.private
  id = "rtb-0f029c27b8898036c"
}

import {
  to = module.network.aws_route_table_association.public["server-subnet-pub-2a"]
  id = "rtbassoc-083941062e3dbf057"
}

import {
  to = module.network.aws_route_table_association.public["server-subnet-pub-2b"]
  id = "rtbassoc-02f424d2f52ea824e"
}

import {
  to = module.network.aws_route_table_association.public["server-subnet-pub-app-2a"]
  id = "rtbassoc-0981bcaf8f2f5c7b4"
}

import {
  to = module.network.aws_route_table_association.public["server-subnet-pub-app-2b"]
  id = "rtbassoc-00eefd0e720ac960c"
}

import {
  to = module.network.aws_route_table_association.private["server-subnet-pri-app-2a"]
  id = "rtbassoc-01a026b6d678e7d32"
}

import {
  to = module.network.aws_route_table_association.private["server-subnet-pri-app-2b"]
  id = "rtbassoc-00843ffb782b49d8d"
}

import {
  to = module.network.aws_route_table_association.private["server-subnet-pri-db-2a"]
  id = "rtbassoc-0d352e5f4d72d685d"
}

import {
  to = module.network.aws_route_table_association.private["server-subnet-pri-db-2b"]
  id = "rtbassoc-03ce61aa2eda08160"
}

# `server-subnet-pub-ec2-2a`는 현재 public route table에 explicit association이 없다.
# import 전 plan에서 association 생성 또는 main route table drift 여부를 별도 확인한다.

import {
  to = module.network.aws_vpc_endpoint.s3[0]
  id = "vpce-0f77eae66ff7327cd"
}

import {
  to = module.security.aws_security_group.alb
  id = "sg-0b0afeffe76f461cf"
}

import {
  to = module.security.aws_security_group.asg
  id = "sg-0565e1b8e39cac6fb"
}

import {
  to = module.security.aws_security_group.db
  id = "sg-0c1aa556e22fb3981"
}

# Terraform이 소유하는 security group rule은 반드시 import한다.
# DB의 기존 ECS task/bastion ingress는 현재 미소유 rule로 남기고,
# EC2 ASG 전환 완료 후 별도 hardening task에서 제거한다.
import {
  to = module.security.aws_security_group_rule.alb_ingress_http
  id = "sg-0b0afeffe76f461cf_ingress_tcp_80_80_0.0.0.0/0"
}

import {
  to = module.security.aws_security_group_rule.alb_ingress_https
  id = "sg-0b0afeffe76f461cf_ingress_tcp_443_443_0.0.0.0/0"
}

import {
  to = module.security.aws_security_group_rule.alb_egress_all
  id = "sg-0b0afeffe76f461cf_egress_-1_0_0_0.0.0.0/0"
}

import {
  to = module.security.aws_security_group_rule.asg_ingress_app
  id = "sg-0565e1b8e39cac6fb_ingress_tcp_8080_8080_sg-0b0afeffe76f461cf"
}

import {
  to = module.security.aws_security_group_rule.asg_ingress_prometheus
  id = "sg-0565e1b8e39cac6fb_ingress_tcp_9090_9090_sg-0b0afeffe76f461cf"
}

import {
  to = module.security.aws_security_group_rule.asg_ingress_ssh["116.124.253.97/32"]
  id = "sg-0565e1b8e39cac6fb_ingress_tcp_22_22_116.124.253.97/32"
}

import {
  to = module.security.aws_security_group_rule.asg_egress_all
  id = "sg-0565e1b8e39cac6fb_egress_-1_0_0_0.0.0.0/0"
}

import {
  to = module.security.aws_security_group_rule.db_ingress_postgresql
  id = "sg-0c1aa556e22fb3981_ingress_tcp_5432_5432_sg-0565e1b8e39cac6fb"
}

import {
  to = module.security.aws_security_group_rule.db_egress_vpc
  id = "sg-0c1aa556e22fb3981_egress_-1_0_0_0.0.0.0/0"
}

import {
  to = module.alb.aws_lb.this
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:loadbalancer/app/server-alb-default/bf4a4964f52710d8"
}

import {
  to = module.alb.aws_lb_target_group.dev_ec2
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:targetgroup/server-tg-ec2-dev/a987266b77a658a5"
}

import {
  to = module.alb.aws_lb_target_group.prod_ec2
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:targetgroup/server-tg-ec2-prod/635cac1b989d13d1"
}

import {
  to = module.alb.aws_lb_listener.http
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:listener/app/server-alb-default/bf4a4964f52710d8/8c048e92ab2a603d"
}

import {
  to = module.alb.aws_lb_listener.https
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:listener/app/server-alb-default/bf4a4964f52710d8/afdb6e15cc0221f7"
}

import {
  to = module.alb.aws_lb_listener_rule.dev
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:listener-rule/app/server-alb-default/bf4a4964f52710d8/afdb6e15cc0221f7/7e9f55d7f8accc70"
}

import {
  to = module.alb.aws_lb_listener_rule.prod
  id = "arn:aws:elasticloadbalancing:ap-northeast-2:137809407320:listener-rule/app/server-alb-default/bf4a4964f52710d8/afdb6e15cc0221f7/2b920d8231c0e7d8"
}

# ALB import 후 첫 plan에서는 아래 두 변경이 의도된 변경인지 확인한다.
# - HTTPS listener 기본 인증서를 university.neordinary.com ACM 인증서로 전환
# - listener rule host header와 weighted forward에서 legacy domain/IP target group 제거
# Route53 record import block은 DNS module resource address가 생기는 task에서 추가한다.
