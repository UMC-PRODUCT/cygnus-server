plugins {
    java
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "4.0.5"
    jacoco
}

group = "com.umc"
version = "2.0.0"
description = "UMC PRODUCT API by Server Team"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Lombok이 compile time에만 포함되도록 설정
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    create("asciidoctorExt")
}

repositories {
    mavenCentral()
}

// 의존성 버전
val springDocVersion = "2.8.17"
val queryDslVersion = "5.1.0"
val jwtVersion = "0.12.5"
val awsVersion = "2.40.12"
val springAiVersion = "1.1.5"
val otelInstrumentationVersion = "2.27.0-alpha"

// REST DOCS
val snippetsDir = file("build/generated-snippets")
val asciiDocsSourceDir = "docs/asciidoc"
val asciiDocsDir = "docs/static"

// QueryDSL Q클래스 생성 경로 설정
val querydslDir = layout.buildDirectory.dir("generated/querydsl").get().asFile

sourceSets {
    main {
        java {
            srcDirs(querydslDir)
        }
    }
}

dependencies {
    // --- Spring Boot Starters (버전 생략: Boot가 관리) ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Mock Data
    implementation("net.datafaker:datafaker:2.5.4")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:${jwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${jwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${jwtVersion}")

    // --- Encryption  ---
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    // --- QueryDSL ---
    // Jakarta 분류가 필요하므로 버전 명시가 안전할 수 있음
    implementation("com.querydsl:querydsl-jpa:${queryDslVersion}:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:${queryDslVersion}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    // APT가 jakarta 클래스를 로딩할 수 있게 명시
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // --- Database ---
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql") // 버전은 Boot가 관리

    // --- Spatial / Location ---
    // JTS (위치 데이터용)
    implementation("org.locationtech.jts:jts-core:1.19.0")
    // Hibernate Spatial (JPA에서 Point 타입 사용)
    implementation("org.hibernate.orm:hibernate-spatial")

    // --- OpenAPI / Swagger ---
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${springDocVersion}")

    // --- Utils ---
    // 서버 시작 시 자동으로 Docker Compose 실행
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // 다들 잘 아는 그 lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SQL 출력용 P6Spy
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.10.0")

    // --- Cloud Service ---
    implementation(platform("software.amazon.awssdk:bom:${awsVersion}"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:cloudfront")  // CloudFront Signed URL
    implementation("com.google.cloud:google-cloud-storage")

    // --- Email ---
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // BOM으로 버전 강제 정렬
    implementation(platform("com.google.protobuf:protobuf-bom:4.29.3"))
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.44.1"))

    // --- Metrics ---
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-registry-otlp")

    // --- Structured Logging (ADR-016) ---
    // dev/staging/prod 환경의 JSON 단일 라인 로그 encoder. local 은 텍스트 유지.
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // --- Tracing ---
    implementation("io.micrometer:micrometer-observation") // 관측 기능: metrics + tracing
    implementation("io.micrometer:micrometer-tracing-bridge-otel") // OpenTelemetry 연동
    implementation("io.opentelemetry:opentelemetry-exporter-otlp") // OTLP Exporter
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:${otelInstrumentationVersion}")
    implementation("io.micrometer:context-propagation") // 비동기 작업에서 context를 잃어버리지 않도록 함

    // Firebase Admin SDK
    implementation("com.google.firebase:firebase-admin:9.7.1")

    // --- Spring AI (LLM provider 통합) ---
    implementation(platform("org.springframework.ai:spring-ai-bom:${springAiVersion}"))
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-model-vertex-ai-gemini")
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai")

    // --- Cache ---
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // --- Test ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.navercorp.fixturemonkey:fixture-monkey-starter:1.1.19") // Fixture 생성에 도움을 주는 친구

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // --- Spring REST Docs ---
    "asciidoctorExt"("org.springframework.restdocs:spring-restdocs-asciidoctor")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
}

springBoot {
    buildInfo()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
    options.generatedSourceOutputDirectory.set(querydslDir)
}

tasks.clean {
    doFirst {
        println("=".repeat(50))
        println("[clean] gradle clean을 시작합니다.")
        println("=".repeat(50))
    }

    doLast {
        querydslDir.deleteRecursively()
        println("[clean] QueryDSL 생성 디렉토리를 삭제하였습니다.")

        println("[clean] gradle clean이 완료되었습니다.")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    doFirst { // 기존에 존재하는 파일들을 지우고 시작함
        println("=".repeat(50))
        println("[test] 테스트를 시작합니다.")
        println("=".repeat(50))
    }

    // 테스트가 터지면 이 곳에 ignoreFailures = true 를 넣으시면 됩니다.
    outputs.dir(snippetsDir)
    finalizedBy(tasks.jacocoTestReport)

    doLast {
        println("=".repeat(50))
        println("[test] 테스트가 완료되었습니다.")
        println("=".repeat(50))
    }
}

// codecov를 위한 Jacoco 설정
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register("generateRestDocsIndex") {
    dependsOn(tasks.test)

    doLast {
        val indexFile = file("docs/asciidoc/index.adoc")

        val content = buildString {
            appendLine("= UMC Product API Documentation")
            appendLine(":doctype: book")
            appendLine(":icons: font")
            appendLine(":source-highlighter: highlightjs")
            appendLine(":toc: left")
            appendLine(":toclevels: 2")
            appendLine(":sectlinks:")
            appendLine()
            appendLine("ifndef::snippets[]")
            appendLine(":snippets: ../../../build/generated-snippets")
            appendLine("endif::[]")
            appendLine()

            // 컨트롤러별로 그룹핑
            val controllerMap = mutableMapOf<String, MutableList<String>>()

            snippetsDir.listFiles()?.forEach { controllerDir ->
                if (controllerDir.isDirectory) {
                    val controllerName = controllerDir.name
                    controllerDir.listFiles()?.forEach { testDir ->
                        if (testDir.isDirectory) {
                            controllerMap
                                .getOrPut(controllerName) { mutableListOf() }
                                .add(testDir.name)
                        }
                    }
                }
            }

            // 정렬 후 출력
            controllerMap.keys.sorted().forEach { controller ->
                val displayName = controller
                    .replace("-controller-test", "")
                    .replace("-", " ")
                    .replaceFirstChar { it.uppercase() }

                appendLine("== $displayName")
                appendLine()

                controllerMap[controller]!!.sorted().forEach { testName ->
                    appendLine("=== $testName")

                    // 실제 존재하는 snippet 타입만 찾기
                    val testDir = file("${snippetsDir}/$controller/$testName")
                    val availableSnippets = listOf(
                        "http-request", "http-response",
                        "path-parameters", "query-parameters",
                        "request-fields", "response-fields"
                    ).filter { snippetType ->
                        File(testDir, "$snippetType.adoc").exists()
                    }

                    // 존재하는 snippet만으로 operation 매크로 생성
                    if (availableSnippets.isNotEmpty()) {
                        val snippetsParam = availableSnippets.joinToString(",")
                        appendLine("operation::$controller/$testName[snippets='$snippetsParam']")
                    } else {
                        // snippet 없으면 기본 operation (모든 snippet 포함 시도)
                        appendLine("operation::$controller/$testName[]")
                    }

                    appendLine()
                }
            }
        }

        indexFile.writeText(content)
        println("[generateRestDocsIndex] index.adoc 자동 생성 완료: ${indexFile.absolutePath}")
    }
}

tasks.asciidoctor { // asciidoctor task 설정
    dependsOn("generateRestDocsIndex")  // asciidoctor 전에 index 먼저 생성
    configurations("asciidoctorExt") // asciidoctorExt 설정 추가
    inputs.dir(snippetsDir) // 테스트를 통해서 생성된 snippets를 입력으로 사용

    baseDirFollowsSourceDir() // .adoc 파일에서 다른 .adoc을 include하여 사용하는 경우에 대한 경로 문제 해결
    setSourceDir(file(asciiDocsSourceDir)) // AsciiDoc 소스 경로 지정 (repo custom)

    // index.adoc 파일만 변환 대상으로 설정
    sources {
        include("**/index.adoc")
    }

    attributes(mapOf("snippets" to snippetsDir.toString())) // .adoc 파일 안에서 {snippets} 변수 사용 가능

    doFirst { // 기존에 존재하는 파일들을 지우고 시작함
        println("=".repeat(50))
        println("[asciidoctor] AsciiDoc 문서 생성을 시작합니다.")
        println("=".repeat(50))
    }

    doLast {
        println("=".repeat(50))
        println("[asciidoctor] AsciiDoc 문서 생성이 완료되었습니다.")
        println("=".repeat(50))
    }
}

val copyDocument = tasks.register<Copy>("copyDocument") { // REST DOCS 복사하는 task
    dependsOn(tasks.asciidoctor) // asciidoctor task에 의존하도록 설정

    doFirst {
        println("=".repeat(50))
        println("[asciidoctor] 생성된 Rest Docs를 복사합니다. 기존 문서들을 삭제됩니다.")
        delete(file(asciiDocsDir))
        println("=".repeat(50))
    }


    from(file("build/docs/asciidoc")) // 기본 생성된 경로
    into(file(asciiDocsDir)) // 파일을 복사할 경로

    doLast {
        println("=".repeat(50))
        println("[copyDocument] AsciiDoc 문서를 docs/static에 복사하였습니니다.")
        println("=".repeat(50))
    }
}

tasks.build {
    dependsOn(tasks.clean) // 빌드 전 clean 수행
    dependsOn(copyDocument) // clean 수행 후에, AsciiDoc 문서 제작

    doLast {
        println("[build] gradle build가 완료되었습니다.")
    }
}

// bootJar에는 document 생성을 제외하도록 함 (test가 실패하더라도 배포는 우선 되도록 하기 위함)
