# Ownership: prod는 prod ASG, prod launch template, prod EC2 instance profile,
# prod IAM role, prod IAM policy attachment, prod RDS instance, prod DB subnet group을 import하고 소유한다.
# shared VPC, ALB, target group, security group, DNS record는 prod state로 import하지 않는다.
#
# module resource address가 생긴 뒤 이 위치에 import block을 추가한다.
#
# import {
#   to = module.prod_asg.aws_autoscaling_group.this
#   id = "prod-umc-product-server-asg"
# }
#
# import {
#   to = module.prod_asg.aws_launch_template.this
#   id = "lt-099a89fa42efbbac9"
# }
#
# import {
#   to = module.prod_rds.aws_db_instance.this
#   id = "server-rds-prod"
# }
