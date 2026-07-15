## TL;DR

UMC PRODUCT 백엔드의 **SpringBoot 부팅 시간**과 **Docker 빌드 시간**을 단축하기 위한 현황 분석 및 최적화 방안 정리 보고서입니다. 현재 두 영역 모두 정량 측정값이 없어 우선 baseline 확보를 권장하며, 즉시 적용 가능한 P1 항목만 반영해도 **부팅 시간 약 4~10초 단축**, **Docker 빌드/배포 시간 약 5~15분 단축**이 기대됩니다.

- 부팅 P1: Spring AI 다중 starter 정리, Hikari `minimum-idle` 축소, Hibernate JDBC metadata 차단, `lazy-initialization`(local 한정)
- Docker P1: Layered Jar 도입, GHA Docker layer 캐시 활성화, 베이스 이미지 JDK→JRE 전환, 멀티 플랫폼 빌드 범위 재검토

---

## 1. 배경 및 목적

- 현재 부팅 시간과 Docker 빌드 시간 모두 **측정 hook이 없어 추세 추적이 불가능**한 상태입니다.
- 의존성 규모 증가(73개 JPA 엔티티, 1,518개 Java 파일, Spring AI 3종 starter, OTel/Sentry/Loki 등)와 Spring Boot 3.5 / Java 21 도입으로 **새로운 최적화 옵션(AppCDS, CRaC, Layered Jar 등)이 활용 가능**해졌습니다.
- 본 보고서는 현황을 정량적으로 진단하고 기법별 기대 효과/난이도/리스크를 정리하여, **우선순위 기반 단계적 개선 로드맵**을 제시합니다. 구현은 별도 PR에서 진행합니다.

---

## 2. SpringBoot 부팅 시간 최적화

### 2.1 현황 분석

#### 2.1.1 기본 정보
- Spring Boot 버전: **3.5.15** ([Version Catalog](../../gradle/libs.versions.toml))
- Java 버전: **21** ([build.gradle.kts](../../build.gradle.kts))
- 주요 starter (11종): `web`, `validation`, `websocket`, `aop`, `actuator`, `security`, `data-jpa`, `graphql`, `thymeleaf`, `cache`, `docker-compose` ([dependencies.gradle.kts](../../gradle/dependencies.gradle.kts))
- 주요 외부 라이브러리: QueryDSL 5.1.0, jjwt 0.13.0, Flyway + PostGIS, p6spy 1.10.0, springdoc-openapi 2.8.17, AWS SDK v2 BOM, Firebase Admin SDK 9.7.1, **Spring AI 1.1.8 (OpenAI + VertexAI Gemini + Google GenAI 3종 동시 starter)**, micrometer-tracing-bridge-otel + OTLP exporter + logstash-encoder 9.0, Caffeine, datafaker, BouncyCastle, hibernate-spatial/JTS ([Version Catalog](../../gradle/libs.versions.toml))

#### 2.1.2 부팅 시간에 영향을 미치는 요소
- `@SpringBootApplication` 위치: `src/main/java/com/umc/product/UmcProductApplication.java:7` → base package `com.umc.product` 전체(Java 소스 1,518개)를 component scan + `@ConfigurationPropertiesScan`
- JPA Entity 수: **73개**
- `@Configuration` 수: **약 28개**, `@EnableXxx` 7종 (`@EnableWebSecurity`, `@EnableMethodSecurity`, `@EnableScheduling`, `@EnableJpaAuditing`, `@EnableAsync`, …)
- `@ConditionalOn*` 빈: 약 16개 (주로 LLM/storage/webhook adapter)
- Flyway migration 수: **71개** (`src/main/resources/db/migration/V2026.*.sql`)
- Hikari 설정 (`application.yml:64-75`): `maximum-pool-size=10`, `minimum-idle=10` → 부팅 시 10개 connection 즉시 확보
- `spring.jpa.open-in-view: false` (`application.yml:88`) — 이미 적용됨
- `ddl-auto: validate` (`application.yml:87`) — 73개 엔티티 metadata 검증
- `hibernate.boot.allow_jdbc_metadata_access`, `temp.use_jdbc_metadata_defaults` 모두 **미설정**
- AOT/Native, Lazy init: **미적용**
- p6spy: 운영 환경 포함 모든 환경에서 활성화 ([dependencies.gradle.kts](../../gradle/dependencies.gradle.kts), `P6SpyConfig.java:11`)
- Actuator: `health,info,metrics,prometheus,tracing` 5종 노출 (`application.yml:237`), tracing sampling 1.0
- Spring AI 3종 starter 동시 의존: 실제로는 단일 `LLM_PROVIDER` 사용 ([dependencies.gradle.kts](../../gradle/dependencies.gradle.kts), `application.yml:127-154`)

#### 2.1.3 현재 측정값
- 로컬 부팅 시간: **미측정**
- CI 부팅 시간: **미측정** (`.github/workflows/ci.yml`, `cd.yml` 모두 health 대기 없음)
- Dockerfile 헬스체크: 주석 처리 (`docker/app/Dockerfile:41`)

### 2.2 최적화 방안

#### 2.2.1 Spring AI 멀티 provider starter 정리 — P1
- **현황**: 동시 의존 3종 ([dependencies.gradle.kts](../../gradle/dependencies.gradle.kts)). 실제 활성 provider는 `LLM_PROVIDER` 단일 값으로 결정. 미사용 starter도 `ChatModel`, `RetryTemplate`, WebClient/HTTP client, JSON parser 자동구성이 부팅 단계에서 평가됨.
- **기대 효과**: starter 1~2개 제거 시 **-1.0~2.5초** (각 starter 자동구성 ≈ 300~800ms × 2~3, ChatClient/HttpClient init 포함).
- **적용 난이도**: Low
- **리스크**: provider 전환 시 빌드/배포 분리 또는 `@SpringBootApplication(exclude={...})` 누락 위험.
- **우선순위**: **P1**

#### 2.2.2 Hikari `minimum-idle` 축소 + `initialization-fail-timeout` 조정 — P1
- **현황**: `application.yml:67-68` `maximum-pool-size=10`, `minimum-idle=10` 동일 설정. 부팅 시 10개 connection 동기 확보. PostgreSQL 핸드셰이크 한 번 ≈ 30~80ms (PostGIS 활성화 보정 포함) → 직렬화 시 +0.3~0.8초.
- **기대 효과**: `minimum-idle=2` + `initialization-fail-timeout=-1`로 변경 시 **-0.3~0.7초**. 트래픽 도달 시 lazy 확보.
- **적용 난이도**: Low
- **리스크**: 첫 요청 burst 시 connection 확보 지연 (~50ms × N). 대규모 트래픽 직후 시작에는 부적합 — staging 우선 검증.
- **우선순위**: **P1** (특히 local/dev)

#### 2.2.3 Hibernate JDBC metadata 호출 차단 — P1
- **현황**: `application.yml:83-94`에서 `hibernate.boot.allow_jdbc_metadata_access`, `hibernate.temp.use_jdbc_metadata_defaults` 모두 **미설정**. dialect 자동 판별을 위해 부팅 시 JDBC connection을 열고 metadata 1회 round-trip. PostgreSQL 18 + JTS extension lookup 포함 시 +0.3~0.6초.
- **기대 효과**: `spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false` + `hibernate.temp.use_jdbc_metadata_defaults=false` 설정 시 **-0.3~0.6초**. dialect는 Hibernate 6의 공간 타입 지원이 통합된 `org.hibernate.dialect.PostgreSQLDialect`로 명시.
- **적용 난이도**: Low (dialect 명시 한 줄 추가)
- **리스크**: PostgreSQL 버전 업그레이드 시 dialect 갱신 누락 가능.
- **우선순위**: **P1**

#### 2.2.4 `spring.main.lazy-initialization=true` (local 한정) — P1
- **현황**: 미설정. 28개 `@Configuration` × 평균 1.6 `@Bean` + 다수 `@Service/@Repository/@Component` 모두 즉시 초기화.
- **기대 효과**: `local` profile 한정 활성화 시 **-2~5초** (개발 hot-reload 사이클의 체감 효과 큼).
- **적용 난이도**: Low — `application-local.yml`에 `spring.main.lazy-initialization: true`
- **리스크**: 부팅 중에만 발견되는 mis-config가 첫 요청까지 미루어짐. **prod 활성화 금지** (첫 요청 P99 spike).
- **우선순위**: **P1** (DX 개선)

#### 2.2.5 Flyway prod 분리 실행 — P2
- **현황**: `application.yml:77-81` `spring.flyway.enabled=true`로 매 부팅마다 71개 migration의 checksum 비교. 평균 10ms/migration × 71 ≈ 0.5~1초.
- **기대 효과**: prod 한정으로 별도 migration job (`java -jar app.jar --spring.profiles.active=migration`)으로 분리 시 **-0.5~1초** + 다중 인스턴스 동시 부팅 시 schema_history lock 경합 제거.
- **적용 난이도**: Med (CD 워크플로 step 추가)
- **리스크**: 마이그레이션 실패 시 배포 차단 흐름 재설계 필요.
- **우선순위**: **P2**

#### 2.2.6 Actuator/Tracing 옵션 정리 — P2
- **현황**: `application.yml:237-279` — Prometheus + OTLP 양쪽 export 동시 활성, tracing sampling=1.0, `MeterRegistry`가 jvm/process/http/jdbc/tomcat/hikaricp/logback/cache 모두 활성.
- **기대 효과**: MeterRegistry/MeterBinder 등록만으로 +0.3~0.6초 소비 추정. 미사용 항목(`cache`/`logback` 카운터) trim 시 **-0.1~0.3초**. Prometheus + OTLP 중복 → 한쪽으로 통합 시 추가 -0.1초.
- **적용 난이도**: Low
- **리스크**: 관측 데이터 누락 — Grafana dashboard와 동기 필요.
- **우선순위**: **P2**

#### 2.2.7 p6spy 운영 환경 비활성화 — P2
- **현황**: [dependencies.gradle.kts](../../gradle/dependencies.gradle.kts)의 `p6spy-spring-boot-starter`가 `implementation`으로 포함 → 모든 환경에서 DataSource가 P6DataSource로 wrapping. `P6SpyConfig.java:11` `@PostConstruct` 항상 실행.
- **기대 효과**: prod에서 비활성화 시 DataSource proxy 초기화 + per-query reflection wrapping 제거. 부팅 **-0.1~0.3초**, 운영 query latency **5~10% 개선** (보너스).
- **적용 난이도**: Low (`decorator.datasource.p6spy.enable-logging: false` + `decorator.datasource.enabled: false` 프로필 분기)
- **리스크**: prod 쿼리 로깅 필요 시 대체 수단 마련 — 이미 OTel tracing으로 SQL span 수집 가능.
- **우선순위**: **P2**

#### 2.2.8 AppCDS / Class Data Sharing — P2
- **현황**: Dockerfile (`docker/app/Dockerfile:28-35`) JVM 옵션에 CDS 관련 플래그 **없음**.
- **기대 효과**: Spring Boot 3.3+ 의 `-Dspring.context.exit=onRefresh -XX:ArchiveClassesAtExit=app.jsa` 한 번 실행 후 `-XX:SharedArchiveFile=app.jsa`로 부팅 → 클래스 로딩 시간 30~40% 단축. 1,500+ Java 파일 + 다량 라이브러리 환경에서 전체 부팅 **-2~4초** 추정.
- **적용 난이도**: Med (Dockerfile multi-stage에서 jsa 생성 → runtime stage에 복사)
- **리스크**: 의존성 변동 시 jsa 재생성 필요. 잘못된 jsa로 부팅 시 silent corruption 가능 → CI 검증 추가 필요.
- **우선순위**: **P2** (운영 환경 효과 가장 큼)

#### 2.2.9 CRaC (Coordinated Restore at Checkpoint) — P3
- **현황**: Spring Boot 3.5 + Corretto 21에서 지원 가능. 현재 미적용.
- **기대 효과**: 체크포인트 복원 부팅 ≈ 200~500ms (95%+ 감소 가능).
- **적용 난이도**: High (Corretto CRaC build + 체크포인트 권한 + DB connection drop/restore handler 모든 stateful bean에 `Resource` 구현)
- **리스크**: 외부 client 다수(S3, Firebase, OTel exporter, Loki4j, Sentry, Spring AI WebClient 등) → 체크포인트 시 모두 disconnect/restore 필요. 운영 안정화까지 1~2달.
- **우선순위**: **P3**

#### 2.2.10 GraalVM Native Image — P3
- **현황**: 플러그인 미적용. QueryDSL annotation processor, p6spy bytecode proxy, hibernate-spatial JTS reflection, AWS SDK / Firebase / Spring AI 등 reflection-heavy library 다수.
- **기대 효과**: 부팅 <500ms + 메모리 사용 50~70% 감소.
- **적용 난이도**: High — reflection hint 다량 필요. Spring AI 3종 starter는 reflection hint 부족 가능성 큼.
- **리스크**: 빌드 시간 10배↑, 디버깅 어려움. 현 시점 ROI 낮음.
- **우선순위**: **P3**

#### 2.2.11 Component Scan 범위 명시 — P3
- **현황**: `UmcProductApplication.java:7` `@SpringBootApplication` 기본 = base package `com.umc.product` 전체.
- **기대 효과**: `scanBasePackages={...}` 명시 시 **-0.1~0.3초** (체감 작음).
- **적용 난이도**: Med (도메인 추가 시 list 갱신 필요)
- **리스크**: 누락 시 빈 미등록 → 운영 사고. 헥사고날 구조상 효과 미미.
- **우선순위**: **P3**

#### 2.2.12 dev/local 한정 `-XX:TieredStopAtLevel=1` — P3
- **현황**: 미적용.
- **기대 효과**: local/dev 부팅 시 **-1~2초** (C1 only). steady-state TPS 감소.
- **적용 난이도**: Low
- **리스크**: **prod 적용 금지**.
- **우선순위**: **P3**

#### 2.2.13 가상 스레드 (참고) — P3
- **현황**: Java 21 사용 중. `spring.threads.virtual.enabled` 미설정.
- **기대 효과**: 부팅 시간 직접 영향은 거의 없음 (-0.05초 미만).
- **리스크**: JPA/blocking I/O 다수 → virtual thread pinning 가능성. 별도 트랙으로 검증.
- **우선순위**: **P3**

#### 2.2.14 이미 적용된 항목
- `spring.jpa.open-in-view: false` (`application.yml:88`)
- `server.shutdown: graceful` (`application.yml:9`)
- QueryDSL Q클래스 compile-time 생성 ([querydsl.gradle.kts](../../gradle/querydsl.gradle.kts))
- `springdoc-openapi` 기본 비활성화 (`application.yml:175,198` `enabled=false`)

### 2.3 부팅 시간 최적화 요약표

| 우선순위 | 기법 | 기대 효과 | 난이도 |
|---|---|---|---|
| P1 | Spring AI 멀티 starter 정리 | -1.0~2.5초 | Low |
| P1 | Hikari `minimum-idle` 축소 | -0.3~0.7초 | Low |
| P1 | Hibernate JDBC metadata 차단 | -0.3~0.6초 | Low |
| P1 | `lazy-initialization` (local) | -2~5초 (DX) | Low |
| P2 | Flyway 분리 실행 (prod) | -0.5~1초 | Med |
| P2 | Actuator/Tracing 옵션 정리 | -0.1~0.4초 | Low |
| P2 | p6spy prod 비활성화 | -0.1~0.3초 + 쿼리 ↑5~10% | Low |
| P2 | AppCDS / Class Data Sharing | -2~4초 | Med |
| P3 | CRaC | -90%+ (수십 초 → 0.x초) | High |
| P3 | GraalVM Native Image | 부팅 <0.5초 | High |
| P3 | ComponentScan 범위 명시 | -0.1~0.3초 | Med |
| P3 | `TieredStopAtLevel=1` (dev) | -1~2초 (dev) | Low |
| P3 | 가상 스레드 | 부팅 영향 미미 | Low |

**누적 P1+P2 적용 예상**: **-4~10초** (현재 부팅 시간 미측정이라 비율로는 ~30~50% 단축 추정).

---

## 3. Docker 빌드 시간 최적화

### 3.1 현황 분석

#### 3.1.1 Docker 빌드 환경
- Dockerfile 위치: `docker/app/dockerfile` (운영), `docker/dev/dockerfile` (개발용, 현재 미사용 추정)
- 베이스 이미지: `amazoncorretto:21-alpine` (`docker/app/dockerfile:1`) — JDK 포함 (~330MB)
- Multi-stage: **미사용** (CI에서 host의 `bootJar` 산출물만 COPY)
- Layered jar: Gradle `bootJar` 설정은 **적용됨** ([build.gradle.kts](../../build.gradle.kts)). Dockerfile은 아직 fat jar 단일 COPY 방식이다.
- BuildKit: CI에서 `docker/setup-buildx-action@v3` 적용 (`.github/workflows/ci.yml:152-153`)
- Docker plugin (Jib/bootBuildImage): **미적용**

#### 3.1.2 현재 Dockerfile 요약
```dockerfile
# docker/app/dockerfile - 단일 stage, 사전 빌드된 fat jar 복사 방식
FROM amazoncorretto:21-alpine
WORKDIR /app
RUN apk add --no-cache curl && addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /app/logs && chown -R spring:spring /app
COPY --chown=spring:spring build/libs/*.jar app.jar  # CI의 bootJar 산출물 의존
RUN chmod 444 app.jar
USER spring:spring
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 ..."
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
```

#### 3.1.3 CI에서의 빌드
- 워크플로 파일: `.github/workflows/ci.yml` (`cd.yml`, `cd-ecr.yml`이 `workflow_call`로 재사용)
- 캐시 전략
  - Gradle: `actions/setup-java@v4`의 `cache: gradle` 적용 (`.github/workflows/ci.yml:118`)
  - Docker layer: **비활성화** — `.github/workflows/ci.yml:195` 주석 `"캐시는 추후 빌드 성능 최적화를 통해 개선, 현재는 네트워크 비용이 더 많이 들어서 비활성화 합니다."`
- 빌드 단계: `compileJava` → `test` → `bootJar` → QEMU/Buildx setup → `docker/build-push-action@v6` 멀티 플랫폼 빌드 (`linux/amd64,linux/arm64`, `.github/workflows/ci.yml:194`)

#### 3.1.4 현재 측정값
- 로컬/CI 빌드 시간: **측정값 부재**
- 이미지 크기: 측정값 부재 (베이스 `amazoncorretto:21-alpine`만 약 330MB)

### 3.2 최적화 방안

#### 3.2.1 Spring Boot Layered Jar + Dockerfile 분리 COPY — P1
- **현황**: Gradle의 `bootJar { layered { enabled = true } }`는 적용되어 있다 ([build.gradle.kts](../../build.gradle.kts)). 다만 `docker/app/dockerfile:15`에서 fat jar를 단일 레이어로 COPY해 Docker layer 분리 효과는 아직 사용하지 않는다.
- **기대 효과**: 의존성(`dependencies`, `snapshot-dependencies`, `spring-boot-loader`)과 `application` 클래스 분리 시 **재빌드에서 application 레이어(~5MB)만 갱신**. 일반 소스 변경 PR에서 push/pull 시간 60~80% 감소. ECR push 시 변경 레이어만 업로드되어 CI 후반부 **1~3분 단축** 예상.
- **적용 난이도**: Low (남은 Dockerfile 4~5줄 재구성)
- **리스크**: 거의 없음. `JarLauncher` 메인 클래스 변경 필요 (`org.springframework.boot.loader.launch.JarLauncher`).
- **우선순위**: **P1**

#### 3.2.2 Multi-stage 빌드로 일원화 — P2
- **현황**: 미적용. CI에서 `bootJar`를 호스트에서 실행 후 `docker/app/dockerfile:15`로 산출물만 COPY. 로컬에서 `docker build .` 단독 실행 불가.
- **기대 효과**: Dockerfile 단독 빌드 가능성 확보(DX 개선), CI 단계 통합 시 Gradle 캐시 마운트 활용 가능. 3.2.3과 결합 시 시너지.
- **적용 난이도**: Med (현 워크플로의 `bootJar` 스텝 제거 + 빌드 컨텍스트 정비)
- **리스크**: 빌드가 도커 컨텍스트로 이동하므로 캐시 없이 사용하면 오히려 느려질 수 있음 — 3.2.3과 함께 적용 권장.
- **우선순위**: **P2**

#### 3.2.3 BuildKit `--mount=type=cache`로 Gradle 캐시 마운트 — P2
- **현황**: 미적용. CI는 host-level `actions/setup-java@v4` Gradle 캐시만 활용.
- **기대 효과**: Multi-stage와 결합 시 `~/.gradle/caches`, `~/.gradle/wrapper` 영속 캐시 → 의존성 미변경 빌드에서 **2~5분 절감**.
- **적용 난이도**: Med (3.2.2 선행 필요)
- **리스크**: BuildKit 캐시는 GHA hosted runner 재시작 시 휘발 → `cache-to=type=gha`와 함께 사용해야 효과.
- **우선순위**: **P2**

#### 3.2.4 GitHub Actions Docker layer 캐시 (gha cache) — P1
- **현황**: 미적용. `.github/workflows/ci.yml:179-194`의 `docker/build-push-action@v6`에 `cache-from`/`cache-to` 옵션 부재. `.github/workflows/ci.yml:195`에 비활성화 사유 주석.
- **기대 효과**: `cache-from: type=gha, cache-to: type=gha,mode=max` 추가 시 의존성 레이어 재사용 → 멀티 플랫폼(amd64+arm64) 빌드에서 **5~10분 단축** 예상. 현재 ARM 에뮬레이션 비용까지 캐시 가능.
- **적용 난이도**: Low (워크플로 2줄 추가)
- **리스크**: GHA 캐시 10GB 한도 — Layered jar(3.2.1) 없이 적용 시 fat jar 전체가 캐시되어 빠르게 한도 초과. **3.2.1 선행 권장**.
- **우선순위**: **P1**

#### 3.2.5 ECR Registry 캐시 (`cache-from=type=registry`) — P2
- **현황**: 미적용. ECR 로그인은 되어 있으나(`.github/workflows/ci.yml:169-176`) 캐시 참조 미설정.
- **기대 효과**: ECR에 캐시 매니페스트 저장 → CI runner 간 캐시 공유. GHA 캐시 10GB 한도와 무관. PR/main 캐시 분리 시 ARM 빌드 추가 단축.
- **적용 난이도**: Low (ECR repo 1개 추가 + 워크플로 2줄)
- **리스크**: 매 빌드마다 캐시 매니페스트 push/pull 발생 → 네트워크 비용 증가. `.github/workflows/ci.yml:195`의 비용 우려 재검토 필요.
- **우선순위**: **P2**

#### 3.2.6 멀티 플랫폼 빌드 범위 축소 (`linux/amd64` 단일) — P1 (조건부)
- **현황**: `.github/workflows/ci.yml:194` `platforms: linux/amd64,linux/arm64` 동시 빌드 (QEMU 에뮬레이션, `.github/workflows/ci.yml:150-151`).
- **기대 효과**: 배포 대상이 단일 아키텍처(ECS Fargate/홈서버)면 `linux/amd64`만 빌드 → QEMU 에뮬레이션 비용 제거로 **빌드 시간 약 40~50% 단축**.
- **적용 난이도**: Low
- **리스크**: ARM 호스트(M 시리즈 맥 로컬 검증 등) 사용 시 별도 빌드 필요. **운영 인프라 아키텍처 확인 후 결정**.
- **우선순위**: **P1** (조건부)

#### 3.2.7 베이스 이미지 JDK → JRE 전환 — P1
- **현황**: `docker/app/dockerfile:1` `amazoncorretto:21-alpine` (JDK 포함, ~330MB). 운영 시 javac/jlink는 불필요.
- **기대 효과**: `amazoncorretto:21-alpine-jre` 또는 `eclipse-temurin:21-jre-alpine`(개발용 Dockerfile에 이미 사용, `docker/dev/dockerfile:4`) 전환 시 베이스 약 **150~180MB 절감(~50% 축소)**. ECR pull/deploy 시간도 비례 감소.
- **적용 난이도**: Low (1줄 변경)
- **리스크**: Heap dump 분석을 운영 컨테이너에서 직접 수행하는 경우 jcmd/jmap 부재. `HeapDumpOnOutOfMemoryError`(`docker/app/dockerfile:31`)는 JRE에서도 동작.
- **우선순위**: **P1**

#### 3.2.8 Jib Gradle 플러그인 도입 — P3 (대안)
- **현황**: 미적용.
- **기대 효과**: 데몬리스, 자동 layered, 변경 클래스만 push → CI 빌드 시간 **50~70% 단축** 가능.
- **적용 난이도**: Med (베이스 이미지, non-root 유저, curl 설치, locale, JAVA_OPTS를 Jib 설정으로 이전 필요)
- **리스크**: 현 Dockerfile의 커스텀 사항(non-root, curl, 한국어 locale, `JAVA_OPTS` 표현식)이 Jib 설정으로 모두 마이그레이션돼야 함. 멀티 플랫폼 push는 가능하나 별도 설정.
- **우선순위**: **P3** (대안 경로)

#### 3.2.9 `.dockerignore` 점검 — P3
- **현황**: 이미 적용 (`.dockerignore:1-26`). `build`, `.gradle`, `.git`, `**/build`, `**/node_modules`, `application*.yml` 등 핵심 제외 포함. **`build/libs`는 의도적으로 허용**(`.dockerignore:25-26`).
- **기대 효과**: 추가 개선 여지 적음. `docs/`, `*.md`, `out/` 추가 시 컨텍스트 전송 5~10% 절감.
- **적용 난이도**: Low
- **리스크**: 없음
- **우선순위**: **P3** (미세 조정)

#### 3.2.10 CI Job 병렬화 (test와 image-build 분리) — P3
- **현황**: `.github/workflows/ci.yml:50-194` 단일 job 내 순차 실행.
- **기대 효과**: `test`는 PR 게이트, `image-build`는 머지/배포 게이트로 분리 시 총 클럭타임 단축. 캐시 공유 설계 필수.
- **적용 난이도**: Med
- **리스크**: 캐시 미설계 시 중복 작업 증가. 3.2.1/3.2.4 적용 후 검토 권장.
- **우선순위**: **P3**

#### 3.2.11 `bootBuildImage` (Buildpacks) — P3 (비권장)
- **현황**: 미적용. [build.gradle.kts](../../build.gradle.kts)는 `springBoot { buildInfo() }`와 layered `bootJar`만 설정한다.
- **기대 효과**: Paketo Buildpack 기반 자동 layered + Memory Calculator. 다만 첫 빌드 시간이 길고, layered jar + Dockerfile 조합이 더 가벼운 경우 많음.
- **적용 난이도**: Low
- **리스크**: 베이스 이미지 통제력 약화, 한국어 timezone 등 커스터마이징 까다로움. 현 Dockerfile 커스텀 사항(`docker/app/dockerfile:8-12, 28-35`) 이전 어려움.
- **우선순위**: **P3** (비권장)

### 3.3 Docker 빌드 최적화 요약표

| 우선순위 | 기법 | 기대 효과 | 난이도 |
|---|---|---|---|
| P1 | Layered Jar + Dockerfile 분리 COPY (3.2.1) | 재빌드 시 layer 갱신 최소화, ECR push 1~3분 절감 | Low |
| P1 | GHA Docker layer 캐시 활성화 (3.2.4) | 의존성 레이어 재사용, 멀티 플랫폼 5~10분 절감 | Low |
| P1 | 멀티 플랫폼 범위 축소 (3.2.6, 조건부) | QEMU 제거로 빌드 시간 40~50% 감소 | Low |
| P1 | 베이스 이미지 JRE 전환 (3.2.7) | 이미지 크기 150~180MB 감소, pull/deploy 단축 | Low |
| P2 | Multi-stage + BuildKit cache mount (3.2.2, 3.2.3) | Gradle 의존성 캐싱 2~5분 절감 | Med |
| P2 | ECR Registry 캐시 (3.2.5) | runner 간 캐시 공유, GHA 한도 회피 | Low |
| P3 | Jib 도입 (3.2.8) | 50~70% 단축 가능, 대안 경로 | Med |
| P3 | `.dockerignore` 미세 조정 (3.2.9) | 컨텍스트 전송 5~10% 절감 | Low |
| P3 | CI Job 병렬화 (3.2.10) | 클럭타임 단축, 캐시 설계 의존 | Med |
| P3 | `bootBuildImage` (3.2.11) | 자동 layered, 비권장 | Low |

---

## 4. 통합 우선순위 로드맵

본 보고서의 권고는 다음 순서로 적용을 제안합니다.

### Phase 0: Baseline 측정 (필수 선행)
- 부팅 시간: `BufferingApplicationStartup` 활성화 + `/actuator/startup` 노출
- Docker 빌드: 워크플로 step별 wall-clock 시간, 이미지 크기, layer 재사용률 기록
- 본 보고서의 정량 수치는 **추정값**이므로 baseline 확보 후 회귀 측정 권장

### Phase 1: 즉시 적용(P1, Low 난이도)
1. **부팅**
   - 2.2.1 Spring AI 멀티 starter 정리 (gradle profile 분기 또는 exclude)
   - 2.2.2 Hikari `minimum-idle=2` + `initialization-fail-timeout=-1` (local/dev 우선)
   - 2.2.3 Hibernate JDBC metadata 차단 + dialect 명시
   - 2.2.4 `spring.main.lazy-initialization=true` (local 한정)
2. **Docker**
   - 3.2.1 Layered Jar 도입 → Dockerfile 분리 COPY
   - 3.2.7 베이스 이미지 JRE 전환
   - 3.2.4 GHA Docker layer 캐시 활성화 (3.2.1과 함께)
   - 3.2.6 단일 플랫폼 빌드 검토 (인프라 확인 필요)

### Phase 2: 운영 효과 최적화 (P2)
- 부팅: AppCDS(2.2.8), p6spy prod 비활성화(2.2.7), Actuator/Tracing trim(2.2.6), Flyway 분리(2.2.5)
- Docker: Multi-stage + BuildKit cache(3.2.2/3.2.3), ECR Registry 캐시(3.2.5)

### Phase 3: 장기 검토 (P3)
- CRaC(2.2.9), GraalVM Native(2.2.10), Jib(3.2.8), CI Job 병렬화(3.2.10), 가상 스레드(2.2.13)

### 예상 누적 효과
- **부팅**: P1+P2 적용 시 **-4~10초** (현재 미측정이라 비율로는 30~50% 단축 추정)
- **Docker**: P1 적용 시 일반 PR 빌드 **5~15분 단축**, 이미지 크기 **150~180MB 감소**

---

## 5. 측정 및 검증 방법

### 5.1 부팅 시간
1. `spring.main.bufferedApplicationStartup.enabled=true` 활성화 후 `/actuator/startup` endpoint 노출 (`application.yml:237` exposure에 `startup` 추가). Spring Boot가 phase별 timing을 JSON으로 반환
2. `-Dspring.context.exit=onRefresh`로 부팅 직후 종료 → `time ./gradlew bootRun`으로 본 부팅 비용만 측정 (요청 처리 시간 배제)
3. **JFR (Java Flight Recorder)** 부팅 구간 캡처: `-XX:StartFlightRecording=duration=60s,filename=boot.jfr` 후 JDK Mission Control로 class loading / JIT / GC 비중 확인
4. **CI 측정 hook 추가**: `.github/workflows/ci.yml`에 `bootRun &` + `until curl localhost:9090/actuator/health; do sleep 0.5; done` 패턴으로 매 PR 부팅 시간 시계열화 → 회귀 감지
5. **AppCDS 비교**: `-Xlog:class+load=info,cds=info`로 클래스 로딩 시간 측정 후 jsa 적용 전/후 비교

### 5.2 Docker 빌드
1. **베이스라인**: `.github/workflows/ci.yml`의 `build-and-test` job 시작/종료 시각을 step별 기록(`time` 명령 또는 `gh run view --log` 분석). 최근 10건 평균 + 캐시 hit/miss 케이스 분리
2. **이미지 크기**: `docker image inspect ... --format='{{.Size}}'` + `docker history`로 layer별 크기 베이스라인 확보. 옵션 적용 후 `dive` 또는 `docker image ls` 비교
3. **레이어 캐시 효율**: `docker build --progress=plain` 출력에서 `CACHED` 비율, `docker/build-push-action`의 `actions/cache`/`gha` hit 로그 분석
4. **A/B 실험 절차**
   - PR 1: 3.2.1(Layered Jar) 단독 적용 → 같은 커밋을 두 번 빌드해 캐시 miss/hit 시간 모두 기록
   - PR 2: 3.2.1 + 3.2.4(GHA 캐시) 추가
   - PR 3: 3.2.7(JRE 베이스) 적용 및 이미지 크기 비교
   - PR 4: 3.2.6(단일 플랫폼) 적용 후 ECS/홈서버 정상 기동 확인
5. **회귀 방지**: 각 PR에 `Build Summary` 스텝(`.github/workflows/ci.yml:271-278`)을 확장하여 `IMAGE_SIZE_MB`, `BUILD_DURATION_SEC`를 `$GITHUB_STEP_SUMMARY`에 기록 → 추세 모니터링

---

## 6. 다음 단계 (Action Items)

- [ ] **Phase 0 — Baseline**: 부팅 시간/Docker 빌드 시간 측정 hook 추가 PR
- [ ] **Phase 1.1 — 부팅 P1**: Spring AI starter 정리, Hikari/Hibernate/Lazy init 설정 PR
- [ ] **Phase 1.2 — Docker P1**: Layered Jar + Dockerfile 개편 + GHA 캐시 활성화 PR
- [ ] **Phase 1.3 — Docker P1**: 베이스 이미지 JRE 전환 PR
- [ ] **Phase 1.4 — Docker P1 (검토)**: 멀티 플랫폼 빌드 범위 결정 (인프라팀 확인 필요)
- [ ] **Phase 2**: AppCDS, p6spy/Actuator/Flyway, Multi-stage+BuildKit, Registry 캐시
- [ ] **Phase 3**: CRaC/Native/Jib/CI 병렬화 — Phase 1·2 측정 결과 기반 ROI 재평가
