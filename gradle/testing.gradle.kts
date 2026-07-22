import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

import java.math.BigDecimal

val snippetsDir = layout.buildDirectory.dir("generated-snippets")
val generatedQuerydslDir = layout.buildDirectory.dir("generated/querydsl")
val generatedQuerydslClassPaths = providers.provider {
    val generatedSourceRoot = generatedQuerydslDir.get().asFile
    fileTree(generatedSourceRoot) {
        include("**/*.java")
    }.files.map { generatedSource ->
        generatedSource.relativeTo(generatedSourceRoot).invariantSeparatorsPath
            .removeSuffix(".java") + ".class"
    }.toSet()
}
val mainSourceSet = extensions.getByType<SourceSetContainer>().named("main")
val jacocoProductionClassDirectories = mainSourceSet.map { sourceSet ->
    sourceSet.output.classesDirs.files.map { classesDirectory ->
        fileTree(classesDirectory) {
            exclude { details -> details.relativePath.pathString in generatedQuerydslClassPaths.get() }
        }
    }
}

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

    outputs.dir(snippetsDir)
    finalizedBy(tasks.named("jacocoTestReport"))

    doLast {
        println("=".repeat(50))
        println("[test] 테스트가 완료되었습니다.")
        println("=".repeat(50))
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(jacocoProductionClassDirectories)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(jacocoProductionClassDirectories)
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal.ONE
            }
            limit {
                counter = "CLASS"
                value = "COVEREDRATIO"
                minimum = BigDecimal.ONE
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
