import org.gradle.api.artifacts.VersionCatalogsExtension

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun version(alias: String): String = libsCatalog.findVersion(alias).get().requiredVersion

configurations {
    named("compileOnly") {
        extendsFrom(configurations.getByName("annotationProcessor"))
    }
}

// Jackson 2는 Spring Boot 3/JJWT 축, tools.jackson은 logstash-logback-encoder 9.x 축이다.
extra["jackson-bom.version"] = version("jackson2")

// 자세한 배경은 docs/analysis/opentelemetry-version-alignment.md를 참고한다.
extra["opentelemetry.version"] = version("otel")

dependencies {
    // --- Spring Boot Starters (버전 생략: Boot가 관리) ---
    add("implementation", "org.springframework.boot:spring-boot-starter-web")
    add("implementation", "org.springframework.boot:spring-boot-starter-validation")
    add("implementation", "org.springframework.boot:spring-boot-starter-websocket")
    add("implementation", "io.projectreactor.netty:reactor-netty-core")
    add("implementation", "org.springframework.boot:spring-boot-starter-aop")
    add("implementation", "org.springframework.boot:spring-boot-starter-actuator")
    add("implementation", "org.springframework.boot:spring-boot-starter-security")
    add("implementation", "org.springframework.boot:spring-boot-starter-data-jpa")
    add("implementation", "org.springframework.boot:spring-boot-starter-graphql")
    add("annotationProcessor", "org.springframework.boot:spring-boot-configuration-processor")

    // Mock Data
    add("implementation", "net.datafaker:datafaker:${version("datafaker")}")

    // JWT
    add("implementation", "io.jsonwebtoken:jjwt-api:${version("jjwt")}")
    add("runtimeOnly", "io.jsonwebtoken:jjwt-impl:${version("jjwt")}")
    add("runtimeOnly", "io.jsonwebtoken:jjwt-jackson:${version("jjwt")}")

    // --- Encryption  ---
    add("implementation", "org.bouncycastle:bcpkix-jdk18on:${version("bouncycastle")}")
    add("implementation", "org.bouncycastle:bcprov-jdk18on:${version("bouncycastle")}")

    // --- QueryDSL ---
    add("implementation", "com.querydsl:querydsl-jpa:${version("querydsl")}:jakarta")
    add("annotationProcessor", "com.querydsl:querydsl-apt:${version("querydsl")}:jakarta")
    add("annotationProcessor", "jakarta.annotation:jakarta.annotation-api")
    add("annotationProcessor", "jakarta.persistence:jakarta.persistence-api")

    // --- Database ---
    add("implementation", "org.flywaydb:flyway-core")
    add("implementation", "org.flywaydb:flyway-database-postgresql")
    add("runtimeOnly", "org.postgresql:postgresql")

    // --- Spatial / Location ---
    add("implementation", "org.locationtech.jts:jts-core:${version("jts")}")
    add("implementation", "org.hibernate.orm:hibernate-spatial")

    // --- OpenAPI / Swagger ---
    add("implementation", "org.springdoc:springdoc-openapi-starter-webmvc-ui:${version("springdoc")}")
    add("implementation", "org.webjars.npm:markdown-it:${version("markdown-it")}")

    // --- Utils ---
    add("developmentOnly", "org.springframework.boot:spring-boot-docker-compose")

    // --- Lombok ---
    add("compileOnly", "org.projectlombok:lombok")
    add("annotationProcessor", "org.projectlombok:lombok")

    // SQL 출력용 P6Spy
    add("implementation", "com.github.gavlyukovskiy:p6spy-spring-boot-starter:${version("p6spy-spring-boot-starter")}")

    // --- Cloud Service ---
    add("implementation", platform("software.amazon.awssdk:bom:${version("aws")}"))
    add("implementation", "software.amazon.awssdk:s3")
    add("implementation", "software.amazon.awssdk:ssm")
    add("implementation", "software.amazon.awssdk:cloudfront")
    add("implementation", "software.amazon.awssdk:sesv2")

    // --- Email ---
    add("implementation", "org.springframework.boot:spring-boot-starter-mail")
    add("implementation", "org.springframework.boot:spring-boot-starter-thymeleaf")

    // --- PDF / QR ---
    add("implementation", "org.apache.pdfbox:pdfbox:${version("pdfbox")}")
    add("implementation", "com.google.zxing:core:${version("zxing")}")
    add("implementation", "com.google.zxing:javase:${version("zxing")}")

    // --- BOM Alignment ---
    add("implementation", platform("tools.jackson:jackson-bom:${version("jackson3")}"))
    add("implementation", platform("com.google.protobuf:protobuf-bom:${version("protobuf")}"))
    add("implementation", platform("io.opentelemetry:opentelemetry-bom:${version("otel")}"))

    // --- Metrics ---
    add("implementation", "io.micrometer:micrometer-registry-prometheus")
    add("implementation", "io.micrometer:micrometer-registry-otlp")

    // --- Structured Logging (ADR-016) ---
    add("implementation", "net.logstash.logback:logstash-logback-encoder:${version("logstash-logback-encoder")}")

    // --- Tracing ---
    add("implementation", "io.micrometer:micrometer-observation")
    add("implementation", "io.micrometer:micrometer-tracing-bridge-otel")
    add("implementation", "io.opentelemetry:opentelemetry-exporter-otlp")
    add(
        "implementation",
        "io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:${version("otel-instrumentation")}"
    )
    add("implementation", "io.micrometer:context-propagation")

    // Firebase Admin SDK
    add("implementation", "com.google.firebase:firebase-admin:${version("firebase-admin")}")

    // --- Spring AI (LLM provider 통합) ---
    add("implementation", platform("org.springframework.ai:spring-ai-bom:${version("spring-ai")}"))
    add("implementation", "org.springframework.ai:spring-ai-starter-model-openai")
    add("implementation", "org.springframework.ai:spring-ai-starter-model-vertex-ai-gemini")
    add("implementation", "org.springframework.ai:spring-ai-starter-model-google-genai")

    // --- Cache ---
    add("implementation", "org.springframework.boot:spring-boot-starter-cache")
    add("implementation", "com.github.ben-manes.caffeine:caffeine")
    add("implementation", "com.bucket4j:bucket4j_jdk17-core:${version("bucket4j")}")

    // --- Test ---
    add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
    add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    add("testImplementation", "org.springframework.boot:spring-boot-testcontainers")
    add("testImplementation", "org.springframework.graphql:spring-graphql-test")
    add("testImplementation", "org.testcontainers:testcontainers")
    add("testImplementation", "org.testcontainers:junit-jupiter")
    add("testImplementation", "org.testcontainers:postgresql")
    add("testImplementation", "com.navercorp.fixturemonkey:fixture-monkey-starter:${version("fixture-monkey")}")
    add("testImplementation", "io.micrometer:micrometer-tracing-test")
    add("testRuntimeOnly", "jakarta.mail:jakarta.mail-api")

    add("testCompileOnly", "org.projectlombok:lombok")
    add("testAnnotationProcessor", "org.projectlombok:lombok")
}
