# loadtest — 부하 테스트 (시작점)

부하 테스트에 필요한 건 여기서부터 본다. 실행 코드와 인프라(Terraform)까지 전부 이 폴더 아래에 있다.

## 어디에 뭐가 있나

| 위치 | 역할 |
|------|------|
| `loadtest/k6/` | k6 실행 코드 (script.js, config, lib, scenarios, data). 상세는 `loadtest/k6/README.md` |
| `loadtest/scripts/` | 실행 글루 — `sync-k6.sh`(업로드) · `run-k6.sh`(원격 실행) · `prepare-data.sh`(시딩) |
| `loadtest/terraform/` | 인프라(SUT·generator·monitoring·RDS·ALB). load-test는 배포 env가 아니라 ephemeral 테스트 리그(local backend·self-contained)라 배포 env와 분리해 실행 코드와 함께 둔다 |
| `docs/adr/013-...` | 전략(도구 선택·시나리오 매핑·도메인 우선순위) |
| `docs/superpowers/plans/2026-07-22-load-test-v1-simplification.md` | v1 구현 계획·트레이드오프 |
| `docs/loadtest/` | (실행 결과·분석 문서 — 결과가 쌓이면 여기) |

> 앱 이미지 빌드/푸시는 `scripts/build-app-image.sh` (범용 스크립트라 `scripts/`에 있음). SUT는 이 이미지를 ECR에서 pull 한다.

## 전체 흐름

```
[1] terraform apply         인프라 생성 (SUT+generator+monitoring+RDS)
[2] prepare-data.sh         SUT DB 시딩 → loadtest/k6/data/seed.json 산출
[3] run-k6.sh               k6 스크립트 sync 후 generator에서 원격 실행
[4] Grafana                 결과 확인 (monitoring EC2)
[5] terraform destroy       통째 정리 (비용/데이터)
```

## 직접 폴더에 들어갈 필요 없다 — 전부 repo 루트에서

Terraform은 `-chdir`로, 스크립트는 내부에서 `git rev-parse`로 루트를 찾으므로 `cd` 하지 않아도 된다.

```bash
# 0) 최초 1회: 로컬 값/시크릿 파일 준비 (커밋 금지)
cd loadtest/terraform
cp terraform.tfvars.example terraform.tfvars   # key_name, allowed_cidr, app_image, git_repo_url, aws_profile 채우기
cp load-test.env.example   load-test.env       # Notion 공유 secret 채우기
cd -                                            # 루트로 복귀

# 1) 인프라 (로컬 state — S3 backend 불필요)
terraform -chdir=loadtest/terraform init
terraform -chdir=loadtest/terraform plan
terraform -chdir=loadtest/terraform apply

# 2) 시딩 (데이터 준비의 유일한 소유자)
loadtest/scripts/prepare-data.sh

# 3) 실행: run-k6.sh <profile> <scenario> <rate> [duration]
loadtest/scripts/run-k6.sh smoke  health-check 1   1m     # 시딩 불필요
loadtest/scripts/run-k6.sh load   project-read 300 10m

# 4) 정리 (plan -destroy로 대상 확인 후)
terraform -chdir=loadtest/terraform destroy
```

SSH 키가 기본 키가 아니면 `SSH_KEY=~/.ssh/umc-loadtest.pem loadtest/scripts/run-k6.sh ...` 처럼 넘긴다.

## 시딩 전략 (Tier)

Source of truth 는 `prepare-data.sh`(스키마를 따라가는 시더)다. 규모로 tier 를 나누고, 캐시는 opt-in.

| Tier | 방식 | 언제 | 실행 |
|------|------|------|------|
| **1 (기본)** | api 시더 | smoke·개발·중간 규모(~수천). 매 run 신선, 스키마 자동 대응 | `loadtest/scripts/prepare-data.sh` |
| **2 (opt-in 캐시)** | snapshot 복원 | 10만+ 를 자주 반복할 때만 | tfvars 에 `db_snapshot_identifier` 지정 후 `apply` |

운영 규칙:

1. 평소엔 **Tier 1(api)**. 유일한 source of truth — 스키마 바뀌어도 도메인 코드+Flyway 가 같이 움직여 그냥 동작한다.
2. api 가 느려지면 **먼저 bulk 엔드포인트로**(SEED-001/002 는 한 호출에 N 건). 그래도 아프면 Tier 2.
3. **Tier 2 는 "굽고 → 복원" 캐시.** 굽기:
   ```bash
   # db_snapshot_identifier 를 비운 채 빈 RDS 로 시작
   terraform -chdir=loadtest/terraform apply
   loadtest/scripts/prepare-data.sh                 # (필요 규모만큼 시딩)
   aws rds create-db-snapshot \
     --db-instance-identifier umc-loadtest-pg \
     --db-snapshot-identifier umc-loadtest-<migration_version>-<yyyymmdd>
   # → 출력된 snapshot id 를 tfvars 의 db_snapshot_identifier 에 넣는다. 이후 apply 는 즉시 복원(시딩 0).
   ```
4. **재굽기 트리거 = 스키마(Flyway 마이그레이션) 또는 시드 모양 변경.** snapshot 이름에 마이그레이션 버전을 박아 stale 을 감지한다. 안 바뀌는 동안은 캐시로 무한 재사용.
5. **캐시(snapshot)는 절대 유일 수단이 아니다.** api 시더가 항상 fallback 이자 재굽기 재료다. 얼린 캐시는 스키마 변경 비용을 떠안기 때문.

## 개념 한 줄 정리

- **PROFILE** = 부하 유형(smoke/load/stress/soak), **SCENARIO** = 업무 시나리오(health-check/project-read…). `run-k6.sh`가 둘을 generator의 `run-umc-k6`로 넘기고, `script.js`가 곱해서 고른다.
- **시딩은 k6가 아니라 `prepare-data.sh`가 한다.** k6는 `seed.json`을 읽기만 한다.
- 인프라 변경(user-data 등)은 `terraform apply`가 인스턴스를 교체해 반영하고, k6 스크립트 변경은 `run-k6.sh`의 rsync로 반영한다(인스턴스 재생성 불필요).
