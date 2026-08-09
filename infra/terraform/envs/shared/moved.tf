# Ownership: shared resource address 이동 기록은 이 파일에 둔다.
# shared network, security, ALB, DNS resource 이름을 바꿀 때는 수동 state mv보다 moved block을 우선한다.
#
# moved {
#   from = module.network.aws_vpc.this
#   to   = module.network.aws_vpc.main
# }
