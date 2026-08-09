# Ownership: dev resource address 이동 기록은 이 파일에 둔다.
# dev ASG, launch template, instance profile, IAM role, IAM policy resource 이름을 바꿀 때는
# 수동 state mv보다 moved block을 우선한다.
#
# moved {
#   from = module.dev_asg.aws_autoscaling_group.this
#   to   = module.dev_compute.aws_autoscaling_group.this
# }
