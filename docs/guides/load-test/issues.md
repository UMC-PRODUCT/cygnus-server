# 부하 테스트 이슈 / 서브이슈 (복붙용)

> **구조:** Epic 1개 → Phase 6개를 **서브이슈**로. 각 블록의 `#` 줄 = 이슈 제목, 그 아래 = 본문.
> Phase 6개 생성 후 Epic의 **Sub-issues**로 연결한다. 라벨·마일스톤은 자유.
> 각 이슈는 **외부 문서 없이 단독으로 읽히도록** 맥락을 본문에 담았다(참조는 레포에 있는 코드만).

---

# [Feat] 부하 테스트 — 임계점 탐색 & 병목 튜닝

## 목표

현재 스펙(단일 앱 + RDS db.t4g.small)의 **최대 TPS와 임계점**을 측정하고, 그때의 **병목 자원**을 USE로 특정해 1차 튜닝까지 한다. 인프라(모니터링·앱 메트릭)와 k6 스크립트 v1은 이미 확보 — 남은 건 **환경 구축·관측 보강·시딩·실측**을 서브이슈로 나눠 진행하는 것.

## 어떤 기능이 필요한가요?

현재 스펙(단일 앱 + RDS db.t4g.small)이 **최대 몇 TPS / 어느 부하까지** 버티는지, 그때 **병목 자원**이 무엇인지 부하 테스트로 확인하고 튜닝한다.

### 배경

모집 기간 동안 700명 정도가 꾸준히 지원을 넣는데, 현재 스펙이 이 **지속 부하**를 감당하는지 측정된 적이 없다. 임계점(처리량이 더 안 오르고 응답이 꺾이는 지점)과 병목을 모른 채 운영 중.

### 용어

- **임계점(Saturation Point):** 부하를 올려도 **TPS가 더 안 오르고(평평) p99 응답이 급상승(무릎)** 하는 지점. = 시스템 한계.
- **USE:** 모든 자원을 **사용률(U) · 포화/대기(S) · 에러(E)** 세 축으로 점검하는 병목 진단법. "자원은 남는데 막히는" 포화(S)가 진짜 병목인 경우가 많다.
- **부하 모델 = open(ramping-arrival-rate):** "초당 요청 수(도착률)"를 계단식으로 올린다. VU(가상 유저)는 그 도착률을 맞추려 k6가 자동 투입. 응답이 느려져도 도착률을 유지해 임계점이 드러난다.

### 결정 & 근거

| 결정                                                               | 근거                                                                                                                                                                           |
| ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 목표 = **임계점 관찰** (합격 SLA 숫자 안 정함)                     | TPS 평평 + p99 무릎 '모양'으로 한계가 드러나므로 임의 합격선이 불필요                                                                                                          |
| DB = RDS **db.t4g.small**, 크레딧 **unlimited**                    | 측정 대상 스펙 고정. standard면 크레딧 소진 시 스로틀 → 임계점이 인공물로 오염·재현 불가                                                                                       |
| HikariCP 풀 = **4**                                                | DB vCPU(2) × 2 ([HikariCP 공식 Pool Sizing 가이드](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)). 그 이상은 동시 처리 한계를 못 넘고 대기만 늘어 무의미 |
| SUT = **dev 프로필**                                               | 부하용 토큰(`/test`)·시딩이 dev 전용. 인프라(스펙)만 prod-like로                                                                                                               |
| 부하 모델 = **open (arrival-rate)**                                | closed VU는 느려지면 같이 쉬어(coordinated omission) 임계점을 가림                                                                                                             |
| 생성기 = **별도 EC2**                                              | 같은 박스면 k6 CPU가 앱과 경쟁해 자원 지표가 오염됨                                                                                                                            |
| 트래픽 믹스 = **읽기 90 : 쓰기 10**                                | 실제 트래픽 상위가 전부 읽기(GET)로 압도적                                                                                                                                     |
| 관측 = **전용 ephemeral 스택**(otel-collector 포함, 별도 인스턴스) | 부하 중 trace/log 폭증이 운영 모니터링을 오염·디스크 압박 → 격리. 운영 경로(app→collector→backend) 재현 + `destroy`로 cleanup 자동                                             |

**이미 존재(새로 안 만듦):** 관측 스택 정의(otel-collector + Prometheus/Tempo/Loki/Grafana/node-exporter)와 앱 메트릭. → 부하테스트엔 이 스택을 **전용 인스턴스로 한 벌 더** 띄우고 **`postgres_exporter`와 `k6 → Prometheus`** 만 추가.

### 조건

- read / write / mixed 각각의 임계점(최대 TPS·부하)이 측정됨.
- 임계점에서 병목 자원이 USE로 특정됨.
- 최소 1회 튜닝 → 재측정으로 임계점 이동을 확인.

### 검증

- 대시보드에서 "TPS 평평 + p99 무릎" 지점과 그 시각의 포화 자원이 리포트로 남음.

## 🔧 TODO (= 서브이슈)

- [ ] Phase 01 · 환경 구축 (Terraform)
- [ ] Phase 02 · 관측성 + 대시보드
- [ ] Phase 03 · 시딩 + 베이스라인
- [ ] Phase 04 · k6 스크립트
- [ ] Phase 05 · 임계점 + USE 진단
- [ ] Phase 06 · 튜닝 + 재측정

> 의존성: **01 → 02·03(병렬) → 04 → 05 → 06**

---

# [Feat] 부하 테스트 Phase 01 · 환경 구축 (Terraform)

## 어떤 기능이 필요한가요?

SUT(측정 대상) · 부하 생성기 · **전용 관측 스택**을 **각각 별도 EC2**로 띄우는 ephemeral(테스트 후 `destroy`) Terraform 환경. 실제 요청 경로는 운영과 같이 **생성기 → ALB → SUT → RDS**로 둔다.

### 배경

부하 결과는 측정 대상이 prod를 닮아야 의미 있다. dev 공유 서버는 스펙·반복성 문제가 있어 전용 prod-like 환경을 IaC로 띄우고 테스트 후 내린다. 부하 생성기를 SUT와 같은 박스에 두면 k6의 CPU가 앱 CPU와 경쟁해 자원 지표가 오염된다. 관측 스택도 운영 것을 재사용하면 부하 중 trace/log 폭증으로 운영 데이터가 오염되므로 전용으로 띄운다.

### 확인된 사실

- 부하 테스트용 앱 이미지는 **개인 AWS 계정 ECR**에 push 된 태그를 사용한다. Terraform은 이미지를 빌드하지 않고, SUT EC2가 IAM role로 ECR에 로그인한 뒤 `docker compose pull`로 이미지를 받는다.
- 기존 개발용 compose(`docker/develop/docker-compose.yml`)는 local Postgres에 의존하므로 그대로 쓰지 않는다. SUT user-data에서 앱 + valkey + node-exporter compose를 생성하고, DB는 Terraform이 만든 RDS endpoint를 주입한다.
- 운영 HTTP 진입점이 ALB이므로 부하 테스트도 **생성기 → ALB:80 → SUT:8080** 경로를 사용한다. 단일 SUT 한 대만 target으로 붙여 "단일 앱 + RDS" 스펙 자체는 유지한다.
- 부하용 토큰 발급(`/test/token/access`)과 시딩은 **dev 프로필 전용** → SUT는 dev로 띄워야 한다.

### 조건

- `terraform apply` 한 번에 VPC + ALB + SUT + 생성기 + 관측 + RDS가 기동.
- SUT: 앱(**dev 프로필**) + RDS **db.t4g.small**(크레딧 unlimited) + HikariCP **pool=4**.
- 생성기: k6 설치된 **별도 EC2**, 같은 AZ, 충분히 큰 타입(생성기가 먼저 병목나지 않게).
- 관측 스택(**otel-collector** + Prometheus/Tempo/Loki/Grafana + postgres_exporter)을 **별도 인스턴스에 전용으로** 띄움(운영 스택 재사용 X) — 운영 데이터 경로를 그대로 재현하면서 격리(폭증·오염·cleanup 회피).
- 관리자 접근은 `allowed_cidr`로 제한한다. Grafana/Prometheus는 EC2 host port 기준으로 각각 `13000`/`19090`, 앱 트래픽은 ALB `80`, SUT management smoke는 `9090`을 사용한다.

### 검증

- 생성기 → ALB → SUT 요청 1건 성공 + SUT 메트릭(jvm/hikaricp/node)과 RDS 메트릭(postgres_exporter)이 **전용** Grafana에 흐름 + `destroy`로 앱·DB·관측·데이터까지 통째 정리.

## 🔧 TODO

- [ ] 1-1 Terraform 스캐폴드 (VPC/SG/ALB — SUT 8080은 ALB에서만 인바운드)
- [ ] 1-2 SUT: 앱 컨테이너(dev 프로필, ECR 이미지를 `docker compose pull`로 구동) + RDS db.t4g.small/unlimited + pool=4
- [ ] 1-3 부하 생성기 EC2 (k6, 같은 AZ)
- [ ] 1-4 **전용 관측 스택**을 별도 인스턴스에 ephemeral 기동(otel-collector 포함) → 앱·k6가 이 스택으로 push/scrape, node-exporter도 함께

## 📎 관련 코드

- `.github/workflows/ci.yml`, `.github/workflows/cd-asg.yml` (현행 ECR 기반 이미지 빌드/배포 흐름 참고)
- `infra/terraform/load-test/user-data/sut.sh.tftpl` (부하 테스트 SUT compose 생성 + ECR pull)
- `infra/monitoring/grafana/docker-compose.yml` (관측 스택 — 이 한 벌을 전용 인스턴스로 새로 기동)

```
Internet / Admin CIDR
        |
        | Grafana :13000, Prometheus :19090, ALB :80, SUT mgmt smoke :9090
        v
+----------------------------------------------------------------------------------+
| VPC 10.20.0.0/16                                                                 |
|                                                                                  |
|  Public subnets                                      Private RDS subnets          |
|                                                                                  |
|  +--------------------------+   HTTP :80    +-----------------------+            |
|  | Loadgen EC2              |-------------> | ALB                   |            |
|  | - k6                     |               | - listener :80        |            |
|  | - node-exporter :9100    |               | - health :9090        |            |
|  +------------+-------------+               +-----------+-----------+            |
|               |                                         | HTTP :8080              |
|               | k6 remote-write :19090                  v                         |
|               v                             +-----------------------+            |
|  +--------------------------+   OTLP :4318   | SUT EC2               |            |
|  | Monitoring EC2           |<---------------| - app dev :8080/9090  |            |
|  | - otel-collector :4318   |                | - valkey              |            |
|  | - prometheus :19090      |                | - node-exporter :9100 |            |
|  | - grafana :13000         |                +-----------+-----------+            |
|  | - tempo / loki           |                            | JDBC :5432              |
|  | - postgres-exporter:9187 |----------------------------v                         |
|  | - node-exporter :9100    |                    +-------------------+            |
|  +------------+-------------+                    | RDS PostgreSQL    |            |
|               |                                  | db.t4g.small      |            |
|               | scrape :9100                     | db: umc_product   |            |
|               +---------------------> SUT node-exporter                          |
|               +---------------------> Loadgen node-exporter                      |
|                                                  +-------------------+            |
|                                                                                  |
+----------------------------------------------------------------------------------+
```

---

# [Feat] 부하 테스트 Phase 02 · 관측성 + 대시보드

## 어떤 기능이 필요한가요?

부하 테스트 **전용(ephemeral) 관측 스택** 위에서 "Load Test" 대시보드에 USE 메트릭을 **한 시간축**에 모으도록 누락된 exporter와 패널을 채운다. (USE = 자원을 사용률 U·포화 S·에러 E로 점검)

### 배경

부하를 올릴 때 "어디가 터지나"를 보려면 앱·DB·부하결과 메트릭이 같은 대시보드·같은 시간축에 있어야 한다. 임계점이 발생한 **그 시각**에 서버 지표를 대조해야 병목이 보이기 때문. 앱 메트릭은 이미 나오지만 **DB 논리자원과 k6 결과가 빠져** 진단이 불가능하다.

### 확인된 사실

- 관측 스택 compose(`infra/monitoring/grafana/docker-compose.yml`)에 **otel-collector + Prometheus + Tempo + Loki + Grafana + node-exporter**가 한 묶음 → 부하테스트엔 이걸 **전용 인스턴스로 새로 한 벌** 띄워 운영 경로(app→collector→backends)를 재현 + 격리.
- 운영 재현 위해 앱 관측 설정(trace 샘플링 등)은 **prod-like 유지**(부하 중 관측 오버헤드도 측정 대상). 샘플링 낮추기는 '관측 비용 측정'용 2차 실험.
- 앱 메트릭은 micrometer로 전부 노출: jvm / http(p99 histogram) / tomcat / **hikaricp** / cache (`src/main/resources/application.yml`의 `management.metrics`). → "앱 논리자원(커넥션풀)"은 별도 APM 없이 `hikaricp_connections_pending`(풀 대기 = 포화 S)으로 이미 확보됨.
- Terraform bootstrap에는 `postgres_exporter`와 k6 remote-write receiver 설정이 포함된다. 남은 핵심은 이 메트릭을 한 화면에서 보는 "Load Test" 대시보드와 첫 실행 검증이다.

### 조건

- `postgres_exporter`가 SUT의 DB를 가리키며 `pg_up == 1`.
- k6 실행 메트릭(`k6_http_reqs` 등)이 Prometheus에 적재됨.
- 대시보드가 전체 시간축 하나로 통제 + `$instance` 변수로 앱/DB 전환.
- 앱 행에 **JVM Heap · GC pause · Tomcat 스레드 · HikariCP pending** 패널 존재(자바 앱은 OS CPU/RAM보다 JVM·풀이 먼저 포화하므로).
- 관측 스택은 부하테스트 **전용·ephemeral**(otel-collector 포함, 운영 샘플링 유지) — cleanup은 `destroy`로 자동, collector도 USE 관찰 대상(백프레셔).

### 검증

- 부하 1회 실행 → Grafana에서 **같은 시각**에 RPS/p99(k6) ↔ `hikaricp_pending`(앱) ↔ pg 커넥션/락(DB)이 함께 보이면 통과.

## 🔧 TODO

- [x] 2-1 `postgres_exporter` 컨테이너 추가 + Prometheus scrape target 등록`
- [x] 2-2 k6 출력 `experimental-prometheus-rw` 연결 + Prometheus remote-write receiver 활성화
- [ ] 2-3 "Load Test" 대시보드 생성 — 시간축 통일 + `$instance` 변수
- [ ] 2-4 앱 행 패널: `jvm_memory_used` / `jvm_gc_pause` / `tomcat_threads_busy` vs max / `hikaricp_connections_pending`
- [ ] 2-5 DB 행 패널: pg 커넥션수/max · 락·blocked · `xact_commit` TPS · 캐시 히트율

## 📎 관련 코드

- `infra/monitoring/grafana/docker-compose.yml` (관측 스택)
- `src/main/resources/application.yml` (`management.metrics` — 노출 중인 앱 메트릭)

---

# [Feat] 부하 테스트 Phase 03 · 시딩 + 베이스라인

## 어떤 기능이 필요한가요?

prod 규모 데이터를 시딩하고, 쓰기 테스트가 동작하도록 **자격 있는 멤버·열린 프로젝트**를 만들어 k6가 읽을 대상 목록을 채운다. 무부하 베이스라인도 기록.

### 배경

빈 DB는 풀스캔도 빨라 인덱스/디스크 I/O 병목이 재현되지 않는다. 또 지원(write) 테스트는 **지원 자격 있는 멤버 + 열린 매칭차수·폼이 있는 프로젝트**가 있어야만 지원서 생성·제출이 통과한다.

### 확인된 사실

- `SeedController` + 시드 서비스 일체(`@Profile("!prod")` + `app.seed.enabled`)가 멤버·프로젝트·지원서를 생성한다.
- 지원서 생성(`POST /api/v1/projects/{id}/applications`)은 `matchingRoundId` + 지원 자격(권한 체크) 필요, 제출(`.../submit`)은 필수 답변이 채워져야 통과.
- k6 스크립트는 `setup()`에서 시드된 **memberId 목록으로 토큰을 발급**하고, **(projectId, matchingRoundId) 목록**으로 지원 저니를 돈다 → 이 ID들을 스크립트 옆 데이터 파일에 채워야 함.

### 조건

- prod 규모 시드 + 스냅샷으로 반복 복원 가능(매 런 동일 초기 상태).
- 자격 있는 memberId / 열린 projectId·matchingRoundId가 k6 데이터 파일에 채워짐.
- 1 VU 스모크 에러율 0%, 베이스라인 p50/p99 기록.

### 검증

- write smoke(1~2회)에서 지원서 생성 → 제출 체인이 2xx로 통과.
- ⚠️ 이게 안 되면 write 테스트는 전부 실패한다(지원할 멤버·프로젝트가 없어서).

## 🔧 TODO

- [ ] 3-1 SeedController로 자격 멤버 + 열린 프로젝트/폼 시딩
- [ ] 3-2 시드 직후 DB 스냅샷 생성 + 복원 절차 문서화
- [ ] 3-3 시드된 memberId / projectId / matchingRoundId를 k6 데이터 파일에 채우기
- [ ] 3-4 1 VU 스모크(0%) + 무부하 베이스라인 기록

## 📎 관련 코드

- `src/main/java/com/umc/product/test/` (SeedController + 시드 서비스 일체)
- `docs/guides/load-test/k6/data/seed.example.json` (채울 ID 풀 템플릿)

---

# [Feat] 부하 테스트 Phase 04 · k6 스크립트

## 어떤 기능이 필요한가요?

읽기 믹스 + 지원 풀체인(지원서 생성 → 답변 저장 → 제출)을 open 모델로 때리는 k6 스크립트. **v1 작성 완료**, 첫 실행 보정만 남음.

### 배경

임계점을 가리지 않으려면 closed(VU 고정) 대신 **arrival-rate(open)** 로 도착률을 밀어넣어야 한다(응답이 느려져도 요청을 계속 발생시켜 한계를 노출). 인증은 OIDC 소셜 로그인이라 부하로 재현 불가 → dev 전용 `/test/token/access`로 토큰을 미리 발급해 쓴다.

### 확인된 사실

- 지원 쓰기 흐름: `GET matching-rounds` → `POST applications`(지원서 생성) → `PUT .../{appId}`(답변 저장) → `POST .../submit`(제출). 답변은 폼 질문 type별(텍스트/옵션/포트폴리오)로 자동 생성.
- 응답이 전역 핸들러로 `ApiResponse`(`result` 필드)에 감싸일 수 있어 파싱 방어가 들어감.
- 작성 완료: 4프로파일(smoke/read/write/mixed), arrival-rate, 토큰 풀 선발급, Prometheus 출력, submit 별도 지표.

### 조건

- smoke에서 read/write 체인이 정상 응답(checks 통과).
- 메트릭이 대시보드에 실시간 표시.

### 검증

- write smoke 1~2회 → 생성/저장/제출 2xx + `submit_duration` 기록.

## 🔧 TODO

- [x] 4-1 `ramping-arrival-rate` + 프로파일(smoke/read/write/mixed)
- [x] 4-2 트래픽 믹스(mixed 90:10) + Top-10 기반 읽기 가중
- [x] 4-3 인증 — `setup()`에서 `/test/token/access` 토큰 풀 선발급
- [x] 4-4 출력 = Prometheus remote-write + submit 별도 지표
- [ ] 4-5 첫 smoke 보정 — 응답 필드명(`result`/`applicationId`/`optionId`), 빠진 reads(`resource-permission` 등) 쿼리 파라미터

## 📎 관련 코드

- `docs/guides/load-test/k6/script.js` (스크립트 본체)
- `docs/guides/load-test/k6/data/seed.example.json` (시드 ID 데이터)

---

# [Feat] 부하 테스트 Phase 05 · 임계점 + USE 진단

## 어떤 기능이 필요한가요?

부하를 올려 임계점을 찾고, **USE(사용률 U·포화 S·에러 E)로 병목 자원 1개를 특정**한다.

### 배경

임계점 = TPS가 평평해지고 p99가 꺾이는 지점. 그 시각의 서버 지표를 대조해야 병목이 보인다. 판독 우선순위는 E(에러) → U(어디가 바쁜가) → S(대기 생기나)이고, "자원은 남는데 막히는" 포화(S)가 진짜 병목인 경우가 많다.

### 확인된 사실

- DB 커넥션 풀=4로 동시 DB 처리 한계가 낮아, 쿼리 튜닝이 필요해지기 전에 `hikaricp_connections_pending`(풀 대기)이 먼저 쌓일 수 있음 — **가설이며 측정으로 확인**(느린 읽기가 먼저일 수도).
- 앱 RPS vs DB TPS(`pg_stat_database_xact_commit`) 비교로 병목이 앱 층인지 DB 층인지 가를 수 있음.

### 조건

- read/write/mixed 각각 임계점 TPS·시각이 확정됨.
- 임계점에서 병목 자원 1개가 USE 근거와 함께 특정됨.
- 앱/DB 층 구분.

### 검증

- "이 TPS에서 이 자원이 병목" 한 줄 + 근거 패널 스크린샷이 리포트로 남음.

## 🔧 TODO

- [ ] 5-1 스냅샷 복원 후 본 측정 (read → write → mixed, 개입 없이 끝까지)
- [ ] 5-2 임계점 = TPS 평평 + p99 무릎, 시각 고정
- [ ] 5-3 USE 판독(E→U→S) — 100% U 부품 또는 U는 여유인데 S가 쌓인 부품 지목
- [ ] 5-4 앱 RPS vs DB TPS로 앱/DB 가르기
- [ ] 5-5 임계점 TPS·병목·근거 스크린샷 리포트 저장

---

# [Feat] 부하 테스트 Phase 06 · 튜닝 + 재측정

## 어떤 기능이 필요한가요?

지목된 병목을 **한 번에 하나씩** 손보고, 동일 조건 재측정으로 임계점이 밀렸는지 확인한다.

### 배경

한 번에 여러 변수를 바꾸면 무엇이 효과였는지 모른다. 매 재측정은 스냅샷 복원으로 clean state에서 한다(런 간 비교 가능하게).

### 확인된 사실

- 풀 크기 조정 시 `앱 pool 합 × 인스턴스 수 < DB max_connections`를 넘기면 병목이 DB 쪽으로 이동한다.
- 인덱스 없는 UPDATE/DELETE는 락 대기를, 인덱스 없는 조회는 디스크 I/O를 유발한다.

### 조건

- 한 번에 한 변수만 변경 + 동일 조건 재측정.
- 튜닝 전/후 임계점 비교표 작성.

### 검증

- 임계점이 오른쪽으로 밀렸는지(개선) 또는 병목이 다음 자원으로 이동했는지 표로 증명.

## 🔧 TODO

- [ ] 6-1 병목별 튜닝 (풀 크기 / 인덱스·쿼리 / GC / 스레드) — Phase 05에서 지목된 것만
- [ ] 6-2 동일 조건 재측정 (스냅샷 복원, 바꾼 변수만 다르게)
- [ ] 6-3 튜닝 전/후 임계점 비교표
- [ ] 6-4 종료 조건 — capacity 결론 + 남은 병목 기록
