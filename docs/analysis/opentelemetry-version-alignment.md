# OpenTelemetry 버전 정렬 배경

`opentelemetry-logback-appender-1.0:2.27.0-alpha`는 `LogRecordBuilder#setException(Throwable)` API를 호출한다.
이 API는 `opentelemetry-api:1.61.0`에는 있지만 Spring Boot 기본 관리 버전보다 낮은 OTel API 축에서는 런타임에 없을 수 있다.

따라서 `gradle/dependencies.gradle.kts`는 다음 두 설정으로 런타임 OTel 버전을 appender가 컴파일된 API 축에 맞춘다.

- `extra["opentelemetry.version"] = 1.61.0`
- `io.opentelemetry:opentelemetry-bom:1.61.0`

관련 의존성 역할은 다음과 같다.

- `spring-boot-starter-actuator`: actuator와 metrics 자동 설정의 Spring Boot 진입점
- `micrometer-observation`: HTTP/DB/custom observation을 metrics/tracing과 연결하는 기반
- `micrometer-tracing-bridge-otel`: Micrometer Tracing을 OpenTelemetry SDK로 연결
- `opentelemetry-exporter-otlp`: trace/log/metric을 OTLP로 Collector에 전송
- `opentelemetry-logback-appender-1.0`: Logback 이벤트를 OpenTelemetry Logs로 변환
- `micrometer-registry-prometheus`: `/actuator/prometheus` scrape용 metrics registry
- `micrometer-registry-otlp`: Micrometer metrics를 OTLP endpoint로 push
- `logstash-logback-encoder`: stdout JSON 로그 포맷
- `context-propagation`: 비동기 작업에서 trace/span context 손실 완화

런타임 문제 흐름은 다음과 같다.

1. Tomcat 요청 처리 중 예외가 발생한다.
2. Logback error 로그가 기록된다.
3. OTel appender가 Throwable을 OTel LogRecord로 매핑한다.
4. appender가 `setException(Throwable)`을 호출한다.
5. 런타임 OTel API 버전이 낮으면 `NoSuchMethodError`가 발생한다.

Jackson 업데이트는 JSON 직렬화 계열 정렬이고, 이 OTel 정렬은 tracing/log appender API 호환성 문제이므로 서로 독립적으로 관리한다.
