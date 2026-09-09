import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

val checkDuplicateFlywayMigrationVersions by tasks.registering {
    group = "verification"
    description = "Fails when two Flyway versioned migrations share the same version."

    val migrationFiles = fileTree("src/main/resources/db/migration") {
        include("V*__*.sql")
    }
    inputs.files(migrationFiles)

    doLast {
        val versionPattern = Regex("""^V(.+)__.+\.sql$""")
        val duplicatedVersions = migrationFiles.files
            .groupBy { migrationFile ->
                versionPattern.matchEntire(migrationFile.name)?.groupValues?.get(1)
                    ?: throw GradleException("Invalid Flyway migration filename: ${migrationFile.name}")
            }
            .filterValues { files -> files.size > 1 }

        if (duplicatedVersions.isNotEmpty()) {
            val details = duplicatedVersions.entries
                .sortedBy { it.key }
                .joinToString(System.lineSeparator()) { (version, files) ->
                    val paths = files
                        .sortedBy { it.name }
                        .joinToString(", ") { it.relativeTo(projectDir).path }
                    "  - $version: $paths"
                }

            throw GradleException("Duplicate Flyway migration versions found:${System.lineSeparator()}$details")
        }
    }
}

val mainResources = extensions.getByType<SourceSetContainer>().named("main").get().resources
val checkSensitiveMainResourcesExcluded by tasks.registering {
    group = "verification"
    description = "Fails when .env.* files are included in main resource inputs."

    val sensitiveResources = mainResources.matching {
        include(".env.*", "**/.env.*")
    }
    inputs.files(sensitiveResources)

    doLast {
        if (!sensitiveResources.isEmpty) {
            val names = sensitiveResources.files
                .map { it.name }
                .sorted()
                .joinToString(", ")
            throw GradleException("Sensitive .env.* resources must be excluded: $names")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxHeapSize = "3g"

    // StackOverflowError 의 트레이스는 기본 한도(1024 프레임)에 걸려 잘리고, 잘리는 쪽이 하필
    // 프레임을 소모한 호출자다. 실제로 CI 에서 SOE 가 났을 때 로그만으로는 어느 코드가 스택을
    // 태웠는지 특정할 수 없어 별도 진단 브랜치를 파야 했다. 0 은 무제한을 뜻한다.
    jvmArgs("-XX:MaxJavaStackTraceDepth=0")

    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
    }

    dependsOn(tasks.named("spotlessTest"))
    dependsOn(checkDuplicateFlywayMigrationVersions)
    dependsOn(checkSensitiveMainResourcesExcluded)
}

tasks.named<Test>("test") {
    doFirst {
        println("=".repeat(50))
        println("[test] 테스트를 시작합니다.")
        println("=".repeat(50))
    }

    finalizedBy(tasks.named("jacocoTestReport"))

    doLast {
        println("=".repeat(50))
        println("[test] 테스트가 완료되었습니다.")
        println("=".repeat(50))
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
