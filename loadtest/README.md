# 부하 테스트 가이드

실행 코드(k6)·인프라(Terraform)·시딩 스크립트가 전부 이 폴더에 있다.
전략적 배경(도구 선택·시나리오 우선순위)은 `docs/adr/013` 참조. 실행 결과·분석은 `docs/loadtest/` 에 쌓는다.

## 빠른 시작

전부 repo 루트에서 실행한다 (Terraform 은 `-chdir`, 스크립트는 내부에서 루트를 찾는다).

```bash
# 0) 최초 1회: 로컬 값/시크릿 준비 (커밋 금지)
cp loadtest/terraform/terraform.tfvars.example loadtest/terraform/terraform.tfvars  # key_name, allowed_cidr, app_image 등
cp loadtest/terraform/load-test.env.example    loadtest/terraform/load-test.env    # Notion 공유 secret

# 0.5) 앱 이미지 빌드 + ECR push (tfvars 의 app_image 로. SUT 기본이 arm64 라 플랫폼 자동 처리)
loadtest/scripts/push-app-image.sh

# 1) 인프라 생성 (로컬 state — S3 backend 불필요)
terraform -chdir=loadtest/terraform init
terraform -chdir=loadtest/terraform apply

# 2) 시딩 → loadtest/k6/data/seed.json 산출
loadtest/scripts/prepare-data.sh                      # 소규모 (smoke·개발, 기본 30명)
SEED_STRATEGY=bulk loadtest/scripts/prepare-data.sh   # 대규모 (10만+, 아래 "시딩" 참조)

# 3) 실행: run-k6.sh <profile> <scenario> <rate> [duration]
loadtest/scripts/run-k6.sh smoke health-check 1   1m    # 시딩 불필요
loadtest/scripts/run-k6.sh smoke home         1   1m    # seed.json 필요
loadtest/scripts/run-k6.sh load  home         300 10m

# 4) 결과 확인: Grafana
terraform -chdir=loadtest/terraform output grafana_url
terraform -chdir=loadtest/terraform output -raw grafana_admin_password

# 5) 정리 (비용! 데이터도 같이 사라진다)
terraform -chdir=loadtest/terraform destroy
```

SSH 키가 기본 키가 아니면 `SSH_KEY=~/.ssh/umc-loadtest.pem` 을 명령 앞에 붙인다 (run-k6.sh·bulk 시딩 공통).

## 시딩 — 3가지 방법

원칙: **시더(코드)가 원천, snapshot 은 캐시.**

| 방법 | 언제 | 실행 |
|------|------|------|
| **api** (기본) | smoke·소규모(~수천). 도메인 가드를 통과해 스키마 변경에 안전 | `loadtest/scripts/prepare-data.sh` |
| **bulk** | 10만+ 행. SUT EC2 에서 앱 이미지를 `seeder` 프로파일로 1회 실행 → JDBC 배치 적재 | `SEED_STRATEGY=bulk loadtest/scripts/prepare-data.sh` |
| **snapshot** (캐시) | 같은 대규모 데이터로 자주 반복할 때 | tfvars 에 `db_snapshot_identifier` 지정 후 `apply` |

알아둘 것:

- bulk 는 **결정적**이다 — 같은 `BULK_RANDOM_SEED`(기본 42)면 같은 데이터가 나와 실행 간 비교가 가능하다. 규모는 `BULK_MEMBER_COUNT`(기본 100000).
- bulk 는 **빈 DB 전제** — 재실행은 destroy/apply(또는 snapshot 복원) 후에. 같은 DB 에 두 번 돌리면 email unique 로 실패한다(의도).
- snapshot 굽기: 빈 RDS → bulk 시딩 → `aws rds create-db-snapshot` → 나온 id 를 tfvars 에 넣으면 이후 apply 는 즉시 복원. **스키마(Flyway)나 시드 모양이 바뀌면 다시 굽는다** — snapshot 이름에 마이그레이션 버전을 박아 stale 을 감지한다.
- k6 는 시딩하지 않는다. `seed.json` 을 읽기만 한다.

## 확장 — 새 시나리오 추가

1. **k6 시나리오 (항상)** — `loadtest/k6/scenarios/<profile>/<이름>.js` 에 `requiresSeed` 와 default 함수를 만들고 `script.js` 의 REGISTRY 에 한 줄 등록. 상세는 `loadtest/k6/README.md`.
2. **시드 데이터가 더 필요하면 (규모로 분기)**
   - 소규모 → SeedController 에 이미 있는지 확인 (SEED-001~007: 멤버·챌린저·역할·프로젝트·지원서·커리큘럼·공지·상벌점). 없으면 SEED-007 패턴을 복제해 시드 API 를 만들고 `prepare-data.sh` 에 호출 스텝을 추가한다. **도메인 지식은 Java(SeedService)에, bash 는 호출 순서만.**
   - 대규모 → `BulkSeedService` 에 seedXxx 단계를 추가한다. 새 테이블당 3곳: ① Row record(`port/out/dto`) ② 포트 메서드 + `BulkSeedJdbcAdapter` SQL ③ 서비스 생성 로직(기존 `rng` 재사용 — 결정성 유지). `ANALYZE_TABLES` 와 Result 카운트도 같이 갱신.
3. **k6 가 고를 대상 ID 가 더 필요하면** — seed.json 의 `targets` 를 채우고 `lib/data.js` 에 pick 함수를 추가한다 (`pickProjectId` 참조). **api/bulk 두 시더가 같은 seed.json 스키마를 산출해야 한다는 것이 유일한 계약.**

주의: 벌크 시더는 시나리오별 픽스처가 아니라 모든 시나리오가 공유하는 **"공유 월드" 하나**를 굽는다. 필요 없는 데이터 모양은 `app.bulk-seed.*` 수치 0 으로 끄고, 새 모양은 월드에 추가한다. `BulkSeedJdbcAdapter` 의 SQL 은 스키마 강결합(속도를 위한 의도)이라 **Flyway 마이그레이션 PR 에서 같이 검토**한다.

## 개념 한 줄

- **PROFILE** = 부하 유형(smoke/load/stress/soak), **SCENARIO** = 업무 시나리오(home/project-read…). `run-k6.sh` 가 둘을 넘기고 `script.js` 가 곱해서 고른다.
- 인프라 변경(user-data 등)은 `terraform apply` 가 인스턴스를 교체해 반영하고, k6 스크립트 변경은 `run-k6.sh` 의 rsync 로 반영된다(인스턴스 재생성 불필요).
- 앱 이미지: 빌드는 `scripts/build-app-image.sh`(범용), **ECR push 까지는 `loadtest/scripts/push-app-image.sh`** — SUT 와 bulk 시더가 이 이미지를 pull 한다.
