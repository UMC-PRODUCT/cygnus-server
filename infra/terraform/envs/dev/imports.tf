# Ownership: dev는 dev ASG, dev launch template, dev EC2 instance profile,
# dev IAM role, dev IAM policy attachment만 import하고 소유한다.
# shared VPC, ALB, target group, security group, DNS record는 dev state로 import하지 않는다.
#
# module resource address가 생긴 뒤 이 위치에 import block을 추가한다.
#
# import {
#   to = module.dev_asg.aws_autoscaling_group.this
#   id = "dev-umc-product-server-asg"
# }
#
# import {
#   to = module.dev_asg.aws_launch_template.this
#   id = "lt-00d81a2f4497849f8"
# }
