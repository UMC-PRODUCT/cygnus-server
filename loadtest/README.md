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
loadtest/scripts/prepare-data.sh                      # Tier 1: api (smoke·소규모)
SEED_STRATEGY=bulk loadtest/scripts/prepare-data.sh   # Tier 2: bulk (10만+, seeder 프로파일)

# 3) 실행: run-k6.sh <profile> <scenario> <rate> [duration]
loadtest/scripts/run-k6.sh smoke  health-check 1   1m     # 시딩 불필요
loadtest/scripts/run-k6.sh load   project-read 300 10m

# 4) 정리 (plan -destroy로 대상 확인 후)
terraform -chdir=loadtest/terraform destroy
```

SSH 키가 기본 키가 아니면 `SSH_KEY=~/.ssh/umc-loadtest.pem loadtest/scripts/run-k6.sh ...` 처럼 넘긴다.

## 시딩 전략 (Tier)

원칙: **생성기(코드)가 원천, 데이터 파일(snapshot)은 캐시.** source of truth 는 항상 시더다.

| Tier | 방식 | 언제 | 실행 |
|------|------|------|------|
| **1 (기본)** | api 시더 (SeedController) | smoke·개발·소규모(~수천). 매 run 신선, 스키마 자동 대응 | `loadtest/scripts/prepare-data.sh` |
| **2 (대규모)** | bulk 시더 (`seeder` 프로파일, JDBC 배치) | 10만+ 행. 결정적(seed 고정)·학교 스큐 반영 | `SEED_STRATEGY=bulk loadtest/scripts/prepare-data.sh` |
| **3 (opt-in 캐시)** | snapshot 복원 | 같은 대규모 데이터로 자주 반복할 때 | tfvars 에 `db_snapshot_identifier` 지정 후 `apply` |

운영 규칙:

1. 평소엔 **Tier 1(api)** — 도메인 가드를 통과하는 정확한 baseline. 스키마 바뀌어도 도메인 코드+Flyway 가 같이 움직여 그냥 동작한다.
2. 10만+ 가 필요하면 **Tier 2(bulk)** — SUT EC2 에서 앱 이미지를 `seeder` 프로파일로 1회 실행해 JDBC 배치로 직접 적재한다(`BulkSeedService`). `BULK_MEMBER_COUNT`·`BULK_RANDOM_SEED` 로 제어하고, 같은 seed 면 같은 데이터가 나온다(실행 간 비교 가능). **빈 DB 전제** — 재실행은 destroy/apply 후에.
3. **Tier 3 은 bulk 로 구운 DB 를 얼린 캐시.** 굽기:
   ```bash
   # db_snapshot_identifier 를 비운 채 빈 RDS 로 시작
   terraform -chdir=loadtest/terraform apply
   SEED_STRATEGY=bulk loadtest/scripts/prepare-data.sh
   aws rds create-db-snapshot \
     --db-instance-identifier umc-loadtest-pg \
     --db-snapshot-identifier umc-loadtest-<migration_version>-<yyyymmdd>
   # → 출력된 snapshot id 를 tfvars 의 db_snapshot_identifier 에 넣는다. 이후 apply 는 즉시 복원(시딩 0).
   ```
4. **재굽기 트리거 = 스키마(Flyway 마이그레이션) 또는 시드 모양 변경.** snapshot 이름에 마이그레이션 버전을 박아 stale 을 감지한다. bulk 시더의 SQL(`BulkSeedJdbcAdapter`)은 도메인 가드를 우회하므로 스키마 변경 PR 에서 함께 검토한다.
5. **캐시(snapshot)는 절대 유일 수단이 아니다.** 시더(api·bulk)가 항상 fallback 이자 재굽기 재료다. 얼린 캐시는 스키마 변경 비용을 떠안기 때문.

## 개념 한 줄 정리

- **PROFILE** = 부하 유형(smoke/load/stress/soak), **SCENARIO** = 업무 시나리오(health-check/project-read…). `run-k6.sh`가 둘을 generator의 `run-umc-k6`로 넘기고, `script.js`가 곱해서 고른다.
- **시딩은 k6가 아니라 `prepare-data.sh`가 한다.** k6는 `seed.json`을 읽기만 한다.
- 인프라 변경(user-data 등)은 `terraform apply`가 인스턴스를 교체해 반영하고, k6 스크립트 변경은 `run-k6.sh`의 rsync로 반영한다(인스턴스 재생성 불필요).
