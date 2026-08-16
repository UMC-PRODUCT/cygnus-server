# 현재 전송 중인 Alert 목록

이 문서는 `monitoring/grafana` 스택에서 Alertmanager 를 통해 Discord 로 전송되는 alert rule 을 정리한다.

## 전송 경로

- Prometheus rule: `config/prometheus/rules/default-alerts.yml`
- Loki LogQL rule: `config/loki/rules/fake/default-log-alerts.yml`
- Routing: Prometheus / Loki Ruler -> Alertmanager -> Discord webhook

`NodeExporterDown` rule 은 현재 주석 처리되어 있어 전송되지 않는다.

## 환경 구분 기준

Discord 알림은 각 alert 의 라벨에서 아래 우선순위로 `환경` 값을 표시한다.

1. `environment`
2. `deployment_environment`
3. `service_name`
4. `application`
5. `job`
6. 위 값이 모두 없으면 `unknown`

local/dev/prod 를 구분하려면 앱 telemetry 에서 `service.name` 또는 `spring.application.name` 을 아래처럼 고정한다.

| 환경  | 권장 값             |
| ----- | ------------------- |
| local | `local-umc-product` |
| dev   | `dev-umc-product`   |
| prod  | `prod-umc-product`  |

Prometheus 의 API/DB 집계 alert 는 `application`, `service_name` 라벨을 유지하도록 `sum by (...)` 기준을 맞췄다. Loki log alert 는 `service_name` 기준으로 집계한다.
Host alert 는 Linux 운영 node-exporter target 만 대상으로 하며, macOS `host.docker.internal` node-exporter target 은 대시보드 확인 용도로만 사용한다.

## Availability

| Alert                  | Severity | For | 발생 조건                                               | 주요 라벨         |
| ---------------------- | -------: | --: | ------------------------------------------------------- | ----------------- |
| `ApiServerDown`        | critical |  2m | API/server 계열 Prometheus target scrape 실패           | `job`, `instance` |
| `PrometheusTargetDown` |  warning |  3m | `node-exporter` 를 제외한 Prometheus target scrape 실패 | `job`, `instance` |
| `PostgreSQLDown`       | critical |  1m | PostgreSQL target down 또는 `pg_up == 0`                | `job`, `instance` |
| `RedisDown`            | critical |  1m | Redis/Valkey target down 또는 `redis_up == 0`           | `job`, `instance` |
| `OTelCollectorDown`    | critical |  1m | OTel Collector target scrape 실패                       | `job`, `instance` |
| `LokiDown`             | critical |  1m | Loki target scrape 실패                                 | `job`, `instance` |
| `PrometheusDown`       | critical |  1m | Prometheus self-scrape 실패                             | `job`, `instance` |
| `AlertmanagerDown`     | critical |  1m | Alertmanager target scrape 실패                         | `job`, `instance` |
| `GrafanaDown`          |  warning |  1m | Grafana target scrape 실패                              | `job`, `instance` |

## Host

| Alert                | Severity | For | 발생 조건                                                | 주요 라벨                |
| -------------------- | -------: | --: | -------------------------------------------------------- | ------------------------ |
| `HighCpuUsage`       |  warning | 10m | CPU usage > 80%                                          | `instance`               |
| `HighCpuUsage`       | critical | 10m | CPU usage > 90%                                          | `instance`               |
| `HighMemoryUsage`    |  warning | 10m | Memory usage > 85%                                       | `instance`               |
| `HighMemoryUsage`    | critical | 10m | Memory usage > 95%                                       | `instance`               |
| `HighDiskUsage`      |  warning | 10m | 관리 대상 filesystem disk usage > 80%                    | `instance`, `mountpoint` |
| `HighDiskUsage`      | critical | 10m | 관리 대상 filesystem disk usage > 90%                    | `instance`, `mountpoint` |
| `LowDiskFreeSpace`   |  warning | 10m | 관리 대상 filesystem 중 하나 이상 free space < 10GB      | `instance`               |
| `ContainerRestarted` |  warning |  0m | 최근 10분 내 container restart 감지                      | `instance`, `name`       |

`HighDiskUsage`, `LowDiskFreeSpace` 는 `tmpfs`, `overlay`, `squashfs`, Docker/Kubernetes/container 임시 mount 를 제외한다.
`LowDiskFreeSpace` 는 작은 system volume 잡음을 줄이기 위해 전체 크기 20GB 초과 filesystem 만 검사하고, mountpoint별 개별 알림 대신 instance별 개수로 집계한다.

## API

| Alert                           | Severity | For | 발생 조건                                    | 주요 라벨                                      |
| ------------------------------- | -------: | --: | -------------------------------------------- | ---------------------------------------------- |
| `ApiHigh5xxErrorRatio`          | critical |  5m | API 5xx ratio > 5%, request rate > 0.1/s     | `application`, `service_name`                  |
| `ApiHigh5xxErrorCount`          | critical |  3m | 최근 5분 API 5xx count > 10                  | `application`, `service_name`                  |
| `ApiRouteHigh5xxErrorCount`     | critical |  0m | 최근 5분 route 별 API 5xx count > 5          | `application`, `service_name`, `method`, `uri` |
| `ApiHigh4xxErrorRatio`          |  warning | 10m | API 4xx ratio > 30%, request rate > 0.1/s    | `application`, `service_name`                  |
| `ApiHighP95LatencyMilliseconds` |  warning |  5m | millisecond metric 기준 API p95 latency > 2s | `application`, `service_name`                  |
| `ApiHighP95LatencySeconds`      |  warning |  5m | second metric 기준 API p95 latency > 2s      | `application`, `service_name`                  |
| `ApiHighP99LatencyMilliseconds` |  warning |  5m | millisecond metric 기준 API p99 latency > 5s | `application`, `service_name`                  |
| `ApiHighP99LatencySeconds`      |  warning |  5m | second metric 기준 API p99 latency > 5s      | `application`, `service_name`                  |
| `ApiTrafficDropped`             |  warning | 10m | 현재 request rate 가 1시간 전의 20% 미만     | `application`, `service_name`                  |
| `LoginTrafficSpike`             |  warning |  5m | login 관련 API traffic > 5 req/s             | `application`, `service_name`                  |
| `ApiRouteTrafficSpike`          |  warning |  5m | route 별 API traffic > 10 req/s              | `application`, `service_name`, `method`, `uri` |

## Database / Cache

| Alert                       | Severity | For | 발생 조건                                            | 주요 라벨                                         |
| --------------------------- | -------: | --: | ---------------------------------------------------- | ------------------------------------------------- |
| `DbConnectionPoolHighUsage` |  warning |  5m | HikariCP active/max connection ratio > 80%           | `application`, `service_name`, `instance`, `pool` |
| `DbConnectionPoolHighUsage` | critical |  5m | HikariCP active/max connection ratio > 95%           | `application`, `service_name`, `instance`, `pool` |
| `DbConnectionPending`       |  warning |  3m | HikariCP pending connection > 0                      | `application`, `service_name`, `instance`, `pool` |
| `DbConnectionTimeout`       | critical |  0m | 최근 5분 HikariCP connection acquisition timeout > 0 | `application`, `service_name`, `instance`, `pool` |
| `RedisMemoryHighUsage`      |  warning | 10m | Redis/Valkey memory usage > 80%                      | `instance`                                        |
| `RedisRejectedConnections`  |  warning |  0m | 최근 5분 Redis/Valkey rejected connection > 0        | `instance`                                        |

## Observability Stack

| Alert                               | Severity | For | 발생 조건                                     | 주요 라벨              |
| ----------------------------------- | -------: | --: | --------------------------------------------- | ---------------------- |
| `OTelCollectorLogExportFailures`    |  warning |  3m | OTel Collector log export failure rate > 0    | `exporter`, `instance` |
| `OTelCollectorMetricExportFailures` |  warning |  3m | OTel Collector metric export failure rate > 0 | `exporter`, `instance` |
| `OTelCollectorTraceExportFailures`  |  warning |  3m | OTel Collector trace export failure rate > 0  | `exporter`, `instance` |
| `LokiRequestErrors`                 |  warning |  3m | Loki 5xx response 발생                        | Loki internal labels   |
| `LokiDiscardedSamples`              |  warning |  3m | Loki discarded sample 발생                    | Loki internal labels   |
| `SSLCertificateExpiresSoon`         |  warning |  1h | SSL certificate 만료까지 14일 미만            | `instance`             |
| `ExternalHttpHealthCheckFailed`     | critical |  3m | Blackbox HTTP check 실패                      | `instance`             |
| `ApiHealthCheckFailed`              | critical |  2m | `/actuator/health` check 실패                 | `instance`             |

## Logs

| Alert                          | Severity | For | 발생 조건                                  | 주요 라벨      |
| ------------------------------ | -------: | --: | ------------------------------------------ | -------------- |
| `ErrorLogsSpike`               |  warning |  3m | 최근 5분 `ERROR` log > 10                  | `service_name` |
| `StructuredErrorLogsSpike`     |  warning |  3m | 최근 5분 structured `level=ERROR` log > 10 | `service_name` |
| `EmailDomainExceptionSpike`    |  warning |  3m | 최근 5분 `EmailDomainException` > 3        | `service_name` |
| `NullPointerExceptionDetected` |  warning |  0m | `NullPointerException` log 감지            | `service_name` |
| `StructuredLoginFailuresSpike` |  warning |  3m | 최근 5분 `auth_login_failed` event > 5     | `service_name` |
| `EmailSendFailures`            |  warning |  0m | 최근 5분 email 관련 failure log > 3        | `service_name` |
| `StructuredEmailSendFailures`  |  warning |  0m | `email_send_failed` event 감지             | `service_name` |
