# Ownership: prod resource address 이동 기록은 이 파일에 둔다.
# prod ASG, launch template, instance profile, IAM role, IAM policy, RDS, DB subnet group
# resource 이름을 바꿀 때는 수동 state mv보다 moved block을 우선한다.
#
# moved {
#   from = module.prod_rds.aws_db_instance.this
#   to   = module.database.aws_db_instance.this
# }
