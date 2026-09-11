plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
    checkstyle
}

apply(from = "gradle/documentation-catalog.gradle.kts")
apply(from = "gradle/dependencies.gradle.kts")
apply(from = "gradle/querydsl.gradle.kts")
apply(from = "gradle/quality.gradle.kts")
apply(from = "gradle/testing.gradle.kts")

group = "com.umc"
version = "2.0.0"
description = "UMC PRODUCT API by Server Team"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

springBoot {
    buildInfo()
}

// 로컬 개발용 dotenv 파일은 소스 디렉터리에 있더라도 배포 JAR에 포함하지 않는다.
tasks.processResources {
    exclude(".env", ".env.*", "**/.env", "**/.env.*")
}

spotless {
    ratchetFrom("origin/develop")

    java {
        target("src/**/*.java")
        importOrder("\\#", "java", "javax", "org", "net", "com", "")
        removeUnusedImports()
        forbidWildcardImports()
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
        formatAnnotations()
    }

    format("misc") {
        target(
            "*.gradle.kts",
            "gradle/**/*.gradle.kts",
            "gradle/**/*.toml",
            ".editorconfig",
            ".github/**/*.yml",
            ".github/**/*.yaml",
            "config/**/*.xml"
        )
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    // processResources 설정이 바뀌더라도 배포 산출물에서 한 번 더 차단한다.
    exclude(".env", ".env.*", "**/.env", "**/.env.*")

    layered {
        enabled.set(true)
    }
}
