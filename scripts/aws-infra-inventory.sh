#!/usr/bin/env bash
set -euo pipefail

PROFILE="${AWS_PROFILE:-umcproduct-readonly}"
REGION="${AWS_REGION:-ap-northeast-2}"
OUT_DIR="${1:-${OUT_DIR:-docs/infra/aws-current-inventory}}"
LOG_FILE="${OUT_DIR}/inventory.log"

declare -a RESULTS=()
declare -a TEMP_DIRS=()

cleanup_temp_dirs() {
  for temp_dir in "${TEMP_DIRS[@]}"; do
    rm -rf "$temp_dir"
  done
}

trap cleanup_temp_dirs EXIT
trap 'cleanup_temp_dirs; exit 130' INT
trap 'cleanup_temp_dirs; exit 143' TERM

require_command() {
  local command_name="$1"

  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "필수 명령을 찾을 수 없습니다: ${command_name}" >&2
    exit 1
  fi
}

record_result() {
  local name="$1"
  local status="$2"
  local detail="$3"

  RESULTS+=("${name}|${status}|${detail}")
}

aws_regional_json() {
  aws "$@" --profile "$PROFILE" --region "$REGION" --output json
}

aws_global_json() {
  aws "$@" --profile "$PROFILE" --output json
}

collect_json() {
  local name="$1"
  local output_file="$2"
  local fallback="$3"
  shift 3

  local temp_dir
  local raw_file
  local formatted_file
  temp_dir="$(mktemp -d)"
  TEMP_DIRS+=("$temp_dir")
  raw_file="${temp_dir}/raw.json"
  formatted_file="${temp_dir}/formatted.json"

  if "$@" >"$raw_file" 2>>"$LOG_FILE"; then
    if jq -S . "$raw_file" >"$formatted_file" 2>>"$LOG_FILE"; then
      mv "$formatted_file" "$output_file"
      record_result "$name" "OK" "$output_file"
    else
      printf '%s\n' "$fallback" >"$output_file"
      record_result "$name" "FAILED" "JSON 형식 오류; ${LOG_FILE} 확인"
    fi
  else
    printf '%s\n' "$fallback" >"$output_file"
      record_result "$name" "FAILED" "AWS CLI 호출 실패; ${LOG_FILE} 확인"
  fi
}

collect_array() {
  collect_json "$1" "$2" "[]" "${@:3}"
}

collect_object() {
  collect_json "$1" "$2" "{}" "${@:3}"
}

append_json_array() {
  local target_file="$1"
  local addition_json="$2"
  local temp_dir
  local addition_file
  local merged_file

  temp_dir="$(mktemp -d)"
  TEMP_DIRS+=("$temp_dir")
  addition_file="${temp_dir}/addition.json"
  merged_file="${temp_dir}/merged.json"

  printf '%s\n' "$addition_json" >"$addition_file"
  jq -s 'map(if type == "array" then . else [] end) | add' "$target_file" "$addition_file" >"$merged_file"
  mv "$merged_file" "$target_file"
}

json_count() {
  local file="$1"

  jq -r 'if type == "array" then length elif type == "object" and length > 0 then 1 else 0 end' "$file" 2>/dev/null || echo "0"
}

json_value() {
  local file="$1"
  local query="$2"

  jq -r "$query" "$file" 2>/dev/null || echo "n/a"
}

collect_launch_template_versions() {
  local output_file="${OUT_DIR}/launch-template-versions.json"
  local failures=0
  local collected=0
  local launch_template_count=0

  printf '[]\n' >"$output_file"

  while IFS= read -r launch_template_id; do
    [[ -z "$launch_template_id" ]] && continue
    launch_template_count=$((launch_template_count + 1))

    local sanitized_json

    if sanitized_json="$(
      aws_regional_json ec2 describe-launch-template-versions \
        --launch-template-id "$launch_template_id" 2>>"$LOG_FILE" |
        jq -S '[
          .LaunchTemplateVersions[]
          | {
              LaunchTemplateId,
              LaunchTemplateName,
              VersionNumber,
              DefaultVersion,
              CreateTime,
              CreatedBy,
              LaunchTemplateData: {
                ImageId: .LaunchTemplateData.ImageId,
                InstanceType: .LaunchTemplateData.InstanceType,
                KeyName: .LaunchTemplateData.KeyName,
                IamInstanceProfile: .LaunchTemplateData.IamInstanceProfile,
                SecurityGroupIds: .LaunchTemplateData.SecurityGroupIds,
                NetworkInterfaces: .LaunchTemplateData.NetworkInterfaces,
                BlockDeviceMappings: .LaunchTemplateData.BlockDeviceMappings,
                MetadataOptions: .LaunchTemplateData.MetadataOptions,
                Monitoring: .LaunchTemplateData.Monitoring,
                EbsOptimized: .LaunchTemplateData.EbsOptimized,
                TagSpecifications: .LaunchTemplateData.TagSpecifications,
                UserDataPresent: (.LaunchTemplateData | has("UserData"))
              }
            }
        ]'
    )"; then
      append_json_array "$output_file" "$sanitized_json"
      collected=$((collected + 1))
    else
      failures=$((failures + 1))
    fi
  done < <(jq -r 'if type == "array" then .[].LaunchTemplateId // empty else empty end' "${OUT_DIR}/launch-templates.json")

  if ((launch_template_count == 0)); then
    record_result "ec2.describe-launch-template-versions" "SKIPPED" "launch template 없음"
  elif ((failures == 0)); then
    record_result "ec2.describe-launch-template-versions" "OK" "${collected}개 launch template"
  else
    record_result "ec2.describe-launch-template-versions" "PARTIAL" "${collected}개 성공, ${failures}개 실패; ${LOG_FILE} 확인"
  fi
}

collect_elbv2_listeners_and_rules() {
  local listeners_file="${OUT_DIR}/listeners.json"
  local rules_file="${OUT_DIR}/listener-rules.json"
  local listener_failures=0
  local rule_failures=0
  local listener_collected=0
  local rule_collected=0
  local load_balancer_count=0
  local listener_count=0

  printf '[]\n' >"$listeners_file"
  printf '[]\n' >"$rules_file"

  while IFS= read -r load_balancer_arn; do
    [[ -z "$load_balancer_arn" ]] && continue
    load_balancer_count=$((load_balancer_count + 1))

    local sanitized_json

    if sanitized_json="$(
      aws_regional_json elbv2 describe-listeners \
        --load-balancer-arn "$load_balancer_arn" 2>>"$LOG_FILE" |
        jq -S '[
          .Listeners[]
          | {
              LoadBalancerArn,
              ListenerArn,
              Protocol,
              Port,
              Certificates,
              SslPolicy,
              DefaultActions: [
                (.DefaultActions // [])[]
                | {
                    Type,
                    Order,
                    TargetGroupArn,
                    RedirectConfig,
                    FixedResponseConfig,
                    ForwardConfig
                  }
              ]
            }
        ]'
    )"; then
      append_json_array "$listeners_file" "$sanitized_json"
      listener_collected=$((listener_collected + 1))
    else
      listener_failures=$((listener_failures + 1))
    fi
  done < <(jq -r 'if type == "array" then .[].LoadBalancerArn // empty else empty end' "${OUT_DIR}/load-balancers.json")

  while IFS= read -r listener_arn; do
    [[ -z "$listener_arn" ]] && continue
    listener_count=$((listener_count + 1))

    local sanitized_json

    if sanitized_json="$(
      aws_regional_json elbv2 describe-rules \
        --listener-arn "$listener_arn" 2>>"$LOG_FILE" |
        jq -S '[
          .Rules[]
          | {
              ListenerArn,
              RuleArn,
              Priority,
              Conditions,
              Actions: [
                (.Actions // [])[]
                | {
                    Type,
                    Order,
                    TargetGroupArn,
                    RedirectConfig,
                    FixedResponseConfig,
                    ForwardConfig
                  }
              ],
              IsDefault
            }
        ]'
    )"; then
      append_json_array "$rules_file" "$sanitized_json"
      rule_collected=$((rule_collected + 1))
    else
      rule_failures=$((rule_failures + 1))
    fi
  done < <(jq -r 'if type == "array" then .[].ListenerArn // empty else empty end' "$listeners_file")

  if ((load_balancer_count == 0)); then
    record_result "elbv2.describe-listeners" "SKIPPED" "load balancer 없음"
  elif ((listener_failures == 0)); then
    record_result "elbv2.describe-listeners" "OK" "${listener_collected}개 load balancer"
  else
    record_result "elbv2.describe-listeners" "PARTIAL" "${listener_collected}개 성공, ${listener_failures}개 실패; ${LOG_FILE} 확인"
  fi

  if ((listener_count == 0)); then
    record_result "elbv2.describe-rules" "SKIPPED" "listener 없음"
  elif ((rule_failures == 0)); then
    record_result "elbv2.describe-rules" "OK" "${rule_collected}개 listener"
  else
    record_result "elbv2.describe-rules" "PARTIAL" "${rule_collected}개 성공, ${rule_failures}개 실패; ${LOG_FILE} 확인"
  fi
}

collect_route53_records() {
  local output_file="${OUT_DIR}/route53-resource-record-sets.json"
  local failures=0
  local collected=0
  local zone_count=0

  printf '[]\n' >"$output_file"

  while IFS= read -r hosted_zone_id; do
    [[ -z "$hosted_zone_id" ]] && continue
    zone_count=$((zone_count + 1))

    local sanitized_json

    if sanitized_json="$(
      aws_global_json route53 list-resource-record-sets \
        --hosted-zone-id "$hosted_zone_id" 2>>"$LOG_FILE" |
        jq -S --arg hosted_zone_id "$hosted_zone_id" '[
          .ResourceRecordSets[]
          | {
              HostedZoneId: $hosted_zone_id,
              Name,
              Type,
              TTL,
              SetIdentifier,
              Weight,
              Region,
              Failover,
              MultiValueAnswer,
              HealthCheckId,
              AliasTarget,
              ResourceRecordCount: ((.ResourceRecords // []) | length)
            }
        ]'
    )"; then
      append_json_array "$output_file" "$sanitized_json"
      collected=$((collected + 1))
    else
      failures=$((failures + 1))
    fi
  done < <(jq -r 'if type == "array" then .[].Id // empty else empty end' "${OUT_DIR}/route53-hosted-zones.json")

  if ((zone_count == 0)); then
    record_result "route53.list-resource-record-sets" "SKIPPED" "hosted zone 없음"
  elif ((failures == 0)); then
    record_result "route53.list-resource-record-sets" "OK" "${collected}개 hosted zone"
  else
    record_result "route53.list-resource-record-sets" "PARTIAL" "${collected}개 성공, ${failures}개 실패; ${LOG_FILE} 확인"
  fi
}

collect_iam_role_policy_metadata() {
  local output_file="${OUT_DIR}/iam-role-policy-metadata.json"
  local failures=0
  local collected=0
  local role_count=0

  printf '[]\n' >"$output_file"

  while IFS= read -r role_name; do
    [[ -z "$role_name" ]] && continue
    role_count=$((role_count + 1))

    local role_json="{}"
    local attached_policies_json="[]"
    local inline_policy_names_json="[]"
    local combined_json
    local role_failed=0

    if ! role_json="$(
      aws_global_json iam get-role \
        --role-name "$role_name" \
        --query 'Role.{RoleName:RoleName,Arn:Arn,Path:Path,CreateDate:CreateDate,Description:Description,MaxSessionDuration:MaxSessionDuration,PermissionsBoundary:PermissionsBoundary}' 2>>"$LOG_FILE" |
        jq -S .
    )"; then
      role_json="{}"
      role_failed=1
    fi

    if ! attached_policies_json="$(
      aws_global_json iam list-attached-role-policies \
        --role-name "$role_name" 2>>"$LOG_FILE" |
        jq -S '[.AttachedPolicies[] | {PolicyName, PolicyArn}]'
    )"; then
      attached_policies_json="[]"
      role_failed=1
    fi

    if ! inline_policy_names_json="$(
      aws_global_json iam list-role-policies \
        --role-name "$role_name" 2>>"$LOG_FILE" |
        jq -S '.PolicyNames'
    )"; then
      inline_policy_names_json="[]"
      role_failed=1
    fi

    combined_json="$(
      jq -n \
        --arg role_name "$role_name" \
        --argjson role "$role_json" \
        --argjson attached_policies "$attached_policies_json" \
        --argjson inline_policy_names "$inline_policy_names_json" \
        '[{
          RoleName: $role_name,
          Role: $role,
          AttachedPolicies: $attached_policies,
          InlinePolicyNames: $inline_policy_names
        }]'
    )"
    append_json_array "$output_file" "$combined_json"

    if ((role_failed == 0)); then
      collected=$((collected + 1))
    else
      failures=$((failures + 1))
    fi
  done < <(jq -r 'if type == "array" then .[].Roles[]?.RoleName // empty else empty end' "${OUT_DIR}/iam-instance-profiles.json" | sort -u)

  if ((role_count == 0)); then
    record_result "iam.role-policy-metadata" "SKIPPED" "instance profile role 없음"
  elif ((failures == 0)); then
    record_result "iam.role-policy-metadata" "OK" "${collected}개 role"
  else
    record_result "iam.role-policy-metadata" "PARTIAL" "${collected}개 성공, ${failures}개 실패; ${LOG_FILE} 확인"
  fi
}

write_summary() {
  local summary_file="${OUT_DIR}/summary.md"
  local account
  local arn
  local public_ssh_groups
  local blackhole_routes
  local launch_templates_with_user_data

  account="$(json_value "${OUT_DIR}/caller-identity.json" '.Account // "n/a"')"
  arn="$(json_value "${OUT_DIR}/caller-identity.json" '.Arn // "n/a"')"
  public_ssh_groups="$(
    jq -r '[
      .[]?
      | select(any(.Ingress[]?;
          (((.IpProtocol == "-1") or (((.FromPort // 65535) <= 22) and ((.ToPort // -1) >= 22)))
          and any(.IpRanges[]?; .CidrIp == "0.0.0.0/0"))))
      | "\(.GroupName)(\(.GroupId))"
    ] | unique | join(", ")' "${OUT_DIR}/security-groups.json" 2>/dev/null
  )"
  blackhole_routes="$(
    jq -r '[
      .[]? as $route_table
      | $route_table.Routes[]?
      | select(.State == "blackhole")
      | "\($route_table.RouteTableId): \(.DestinationCidrBlock // .DestinationIpv6CidrBlock // "unknown") -> \(.NatGatewayId // .GatewayId // .TransitGatewayId // .VpcPeeringConnectionId // "unknown")"
    ] | unique | join("; ")' "${OUT_DIR}/route-tables.json" 2>/dev/null
  )"
  launch_templates_with_user_data="$(
    jq -r '[
      .[]?
      | select(.LaunchTemplateData.UserDataPresent == true)
      | "\(.LaunchTemplateName)(\(.LaunchTemplateId)) v\(.VersionNumber)"
    ] | unique | join(", ")' "${OUT_DIR}/launch-template-versions.json" 2>/dev/null
  )"

  {
    echo "# AWS 인프라 Inventory 요약"
    echo
    echo "- 생성 시각(UTC): $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    echo "- Profile: \`${PROFILE}\`"
    echo "- Region: \`${REGION}\`"
    echo "- Account: \`${account}\`"
    echo "- Principal: \`${arn}\`"
    echo "- 출력 디렉터리: \`${OUT_DIR}\`"
    echo
    echo "## 리소스 개수"
    echo
    echo "| 리소스 | 개수 |"
    echo "| --- | ---: |"
    echo "| VPCs | $(json_count "${OUT_DIR}/vpcs.json") |"
    echo "| Subnets | $(json_count "${OUT_DIR}/subnets.json") |"
    echo "| Route tables | $(json_count "${OUT_DIR}/route-tables.json") |"
    echo "| Internet gateways | $(json_count "${OUT_DIR}/internet-gateways.json") |"
    echo "| NAT gateways | $(json_count "${OUT_DIR}/nat-gateways.json") |"
    echo "| VPC endpoints | $(json_count "${OUT_DIR}/vpc-endpoints.json") |"
    echo "| Security groups | $(json_count "${OUT_DIR}/security-groups.json") |"
    echo "| EC2 instances | $(json_count "${OUT_DIR}/instances.json") |"
    echo "| Launch templates | $(json_count "${OUT_DIR}/launch-templates.json") |"
    echo "| Launch template versions | $(json_count "${OUT_DIR}/launch-template-versions.json") |"
    echo "| Auto Scaling groups | $(json_count "${OUT_DIR}/auto-scaling-groups.json") |"
    echo "| Load balancers | $(json_count "${OUT_DIR}/load-balancers.json") |"
    echo "| Listeners | $(json_count "${OUT_DIR}/listeners.json") |"
    echo "| Listener rules | $(json_count "${OUT_DIR}/listener-rules.json") |"
    echo "| Target groups | $(json_count "${OUT_DIR}/target-groups.json") |"
    echo "| RDS instances | $(json_count "${OUT_DIR}/rds-instances.json") |"
    echo "| RDS DB subnet groups | $(json_count "${OUT_DIR}/rds-db-subnet-groups.json") |"
    echo "| Route53 hosted zones | $(json_count "${OUT_DIR}/route53-hosted-zones.json") |"
    echo "| Route53 record set metadata | $(json_count "${OUT_DIR}/route53-resource-record-sets.json") |"
    echo "| IAM instance profiles | $(json_count "${OUT_DIR}/iam-instance-profiles.json") |"
    echo "| IAM role policy metadata entries | $(json_count "${OUT_DIR}/iam-role-policy-metadata.json") |"
    echo
    echo "## 안전 처리"
    echo
    echo "- Launch template user-data 원문은 저장하지 않는다. \`UserDataPresent\`만 기록한다."
    echo "- Route53 resource record 값은 저장하지 않는다. record metadata와 alias target만 기록한다."
    echo "- IAM policy document 원문은 저장하지 않는다. role, attached policy, inline policy name metadata만 기록한다."
    echo "- SSH를 \`0.0.0.0/0\`에서 허용하는 security group: \`${public_ssh_groups:-없음}\`"
    echo "- Blackhole route: \`${blackhole_routes:-없음}\`"
    echo "- User-data가 있는 launch template version: \`${launch_templates_with_user_data:-없음}\`"
    echo
    echo "## 수집 결과"
    echo
    echo "| 명령 그룹 | 상태 | 상세 |"
    echo "| --- | --- | --- |"
    for result in "${RESULTS[@]}"; do
      IFS='|' read -r name status detail <<<"$result"
      echo "| ${name} | ${status} | ${detail} |"
    done
  } >"$summary_file"

  echo "요약 파일 생성 완료: ${summary_file}"
}

main() {
  require_command aws
  require_command jq

  mkdir -p "$OUT_DIR"
  : >"$LOG_FILE"

  collect_object "sts.get-caller-identity" "${OUT_DIR}/caller-identity.json" \
    aws_global_json sts get-caller-identity
  collect_array "ec2.describe-vpcs" "${OUT_DIR}/vpcs.json" \
    aws_regional_json ec2 describe-vpcs \
    --query 'Vpcs[].{VpcId:VpcId,Cidr:CidrBlock,IsDefault:IsDefault,State:State,Tags:Tags}'
  collect_array "ec2.describe-subnets" "${OUT_DIR}/subnets.json" \
    aws_regional_json ec2 describe-subnets \
    --query 'Subnets[].{SubnetId:SubnetId,VpcId:VpcId,Az:AvailabilityZone,Cidr:CidrBlock,MapPublicIpOnLaunch:MapPublicIpOnLaunch,State:State,Tags:Tags}'
  collect_array "ec2.describe-route-tables" "${OUT_DIR}/route-tables.json" \
    aws_regional_json ec2 describe-route-tables \
    --query 'RouteTables[].{RouteTableId:RouteTableId,VpcId:VpcId,Associations:Associations,Routes:Routes,Tags:Tags}'
  collect_array "ec2.describe-internet-gateways" "${OUT_DIR}/internet-gateways.json" \
    aws_regional_json ec2 describe-internet-gateways \
    --query 'InternetGateways[].{InternetGatewayId:InternetGatewayId,Attachments:Attachments,Tags:Tags}'
  collect_array "ec2.describe-nat-gateways" "${OUT_DIR}/nat-gateways.json" \
    aws_regional_json ec2 describe-nat-gateways \
    --query 'NatGateways[].{NatGatewayId:NatGatewayId,VpcId:VpcId,SubnetId:SubnetId,State:State,ConnectivityType:ConnectivityType,NatGatewayAddresses:NatGatewayAddresses,Tags:Tags}'
  collect_array "ec2.describe-vpc-endpoints" "${OUT_DIR}/vpc-endpoints.json" \
    aws_regional_json ec2 describe-vpc-endpoints \
    --query 'VpcEndpoints[].{VpcEndpointId:VpcEndpointId,ServiceName:ServiceName,VpcEndpointType:VpcEndpointType,State:State,VpcId:VpcId,SubnetIds:SubnetIds,RouteTableIds:RouteTableIds,Groups:Groups,PrivateDnsEnabled:PrivateDnsEnabled,Tags:Tags}'
  collect_array "ec2.describe-security-groups" "${OUT_DIR}/security-groups.json" \
    aws_regional_json ec2 describe-security-groups \
    --query 'SecurityGroups[].{GroupId:GroupId,GroupName:GroupName,Description:Description,VpcId:VpcId,Tags:Tags,Ingress:IpPermissions,Egress:IpPermissionsEgress}'
  collect_array "ec2.describe-instances" "${OUT_DIR}/instances.json" \
    aws_regional_json ec2 describe-instances \
    --query 'Reservations[].Instances[].{InstanceId:InstanceId,ImageId:ImageId,InstanceType:InstanceType,State:State.Name,PrivateIpAddress:PrivateIpAddress,PublicIpAddress:PublicIpAddress,SubnetId:SubnetId,VpcId:VpcId,SecurityGroups:SecurityGroups,IamInstanceProfile:IamInstanceProfile,LaunchTime:LaunchTime,LaunchTemplate:LaunchTemplate,BlockDeviceMappings:BlockDeviceMappings[].{DeviceName:DeviceName,Ebs:Ebs},Tags:Tags}'
  collect_array "ec2.describe-launch-templates" "${OUT_DIR}/launch-templates.json" \
    aws_regional_json ec2 describe-launch-templates \
    --query 'LaunchTemplates[].{LaunchTemplateId:LaunchTemplateId,LaunchTemplateName:LaunchTemplateName,DefaultVersionNumber:DefaultVersionNumber,LatestVersionNumber:LatestVersionNumber,CreateTime:CreateTime,CreatedBy:CreatedBy,Tags:Tags}'
  collect_launch_template_versions
  collect_array "autoscaling.describe-auto-scaling-groups" "${OUT_DIR}/auto-scaling-groups.json" \
    aws_regional_json autoscaling describe-auto-scaling-groups \
    --query 'AutoScalingGroups[].{Name:AutoScalingGroupName,LaunchTemplate:LaunchTemplate,MixedInstancesPolicy:MixedInstancesPolicy,VPCZoneIdentifier:VPCZoneIdentifier,Min:MinSize,Max:MaxSize,Desired:DesiredCapacity,TargetGroupARNs:TargetGroupARNs,HealthCheckType:HealthCheckType,HealthCheckGracePeriod:HealthCheckGracePeriod,Instances:Instances,Tags:Tags}'
  collect_array "elbv2.describe-load-balancers" "${OUT_DIR}/load-balancers.json" \
    aws_regional_json elbv2 describe-load-balancers \
    --query 'LoadBalancers[].{Name:LoadBalancerName,LoadBalancerArn:LoadBalancerArn,DNSName:DNSName,CanonicalHostedZoneId:CanonicalHostedZoneId,Type:Type,Scheme:Scheme,VpcId:VpcId,AvailabilityZones:AvailabilityZones,SecurityGroups:SecurityGroups,State:State.Code,IpAddressType:IpAddressType}'
  collect_elbv2_listeners_and_rules
  collect_array "elbv2.describe-target-groups" "${OUT_DIR}/target-groups.json" \
    aws_regional_json elbv2 describe-target-groups \
    --query 'TargetGroups[].{Name:TargetGroupName,TargetGroupArn:TargetGroupArn,VpcId:VpcId,Protocol:Protocol,Port:Port,TargetType:TargetType,HealthCheckProtocol:HealthCheckProtocol,HealthCheckPath:HealthCheckPath,HealthCheckPort:HealthCheckPort,Matcher:Matcher,HealthyThreshold:HealthyThresholdCount,UnhealthyThreshold:UnhealthyThresholdCount,Interval:HealthCheckIntervalSeconds,Timeout:HealthCheckTimeoutSeconds}'
  collect_array "rds.describe-db-instances" "${OUT_DIR}/rds-instances.json" \
    aws_regional_json rds describe-db-instances \
    --query 'DBInstances[].{Identifier:DBInstanceIdentifier,Engine:Engine,EngineVersion:EngineVersion,Class:DBInstanceClass,Status:DBInstanceStatus,MultiAZ:MultiAZ,StorageType:StorageType,AllocatedStorage:AllocatedStorage,VpcSecurityGroups:VpcSecurityGroups[].VpcSecurityGroupId,DBSubnetGroup:DBSubnetGroup.DBSubnetGroupName,Subnets:DBSubnetGroup.Subnets[].SubnetIdentifier,PubliclyAccessible:PubliclyAccessible,DeletionProtection:DeletionProtection,BackupRetentionPeriod:BackupRetentionPeriod,StorageEncrypted:StorageEncrypted,KmsKeyId:KmsKeyId,Endpoint:Endpoint}'
  collect_array "rds.describe-db-subnet-groups" "${OUT_DIR}/rds-db-subnet-groups.json" \
    aws_regional_json rds describe-db-subnet-groups \
    --query 'DBSubnetGroups[].{DBSubnetGroupName:DBSubnetGroupName,DBSubnetGroupArn:DBSubnetGroupArn,VpcId:VpcId,SubnetGroupStatus:SubnetGroupStatus,Subnets:Subnets[].{SubnetIdentifier:SubnetIdentifier,SubnetAvailabilityZone:SubnetAvailabilityZone.Name,SubnetStatus:SubnetStatus},DBSubnetGroupDescription:DBSubnetGroupDescription}'
  collect_array "route53.list-hosted-zones" "${OUT_DIR}/route53-hosted-zones.json" \
    aws_global_json route53 list-hosted-zones \
    --query 'HostedZones[].{Id:Id,Name:Name,CallerReference:CallerReference,Config:Config,ResourceRecordSetCount:ResourceRecordSetCount}'
  collect_route53_records
  collect_array "iam.list-instance-profiles" "${OUT_DIR}/iam-instance-profiles.json" \
    aws_global_json iam list-instance-profiles \
    --query 'InstanceProfiles[].{InstanceProfileName:InstanceProfileName,Arn:Arn,Path:Path,CreateDate:CreateDate,Roles:Roles[].{RoleName:RoleName,Arn:Arn,Path:Path,CreateDate:CreateDate,Description:Description,MaxSessionDuration:MaxSessionDuration}}'
  collect_iam_role_policy_metadata
  write_summary

  echo "Inventory 생성 완료: ${OUT_DIR}"
}

main "$@"
