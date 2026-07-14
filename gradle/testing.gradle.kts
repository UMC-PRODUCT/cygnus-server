import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

val snippetsDir = layout.buildDirectory.dir("generated-snippets")
val testSourceSet = extensions.getByType<SourceSetContainer>().named("test").get()

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
    exclude("**/ProjectPolicyArtifactBootJarTest.class")

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

val generateProjectPolicyArtifacts by tasks.registering(Test::class) {
    group = "policy"
    description = "Regenerates the tracked Project policy review artifact after an intentional source change."
    dependsOn(tasks.named("testClasses"))
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    filter {
        includeTestsMatching(
            "com.umc.product.project.application.authorization.ProjectPolicyArtifactTest"
        )
    }
    systemProperty("project.policy.artifact.mode", "generate")
    reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/$name"))
    reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/$name"))
    binaryResultsDirectory.set(layout.buildDirectory.dir("test-results/$name/binary"))
    outputs.upToDateWhen { false }
}

val verifyProjectPolicyArtifacts by tasks.registering(Test::class) {
    group = "verification"
    description = "Regenerates Project policy review bytes in memory and rejects a stale tracked artifact."
    dependsOn(tasks.named("testClasses"))
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    filter {
        includeTestsMatching(
            "com.umc.product.project.application.authorization.ProjectPolicyArtifactTest"
        )
        includeTestsMatching(
            "com.umc.product.project.application.authorization.ProjectPolicyArtifactMutationTest"
        )
    }
    systemProperty("project.policy.artifact.mode", "verify")
    reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/$name"))
    reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/$name"))
    binaryResultsDirectory.set(layout.buildDirectory.dir("test-results/$name/binary"))
    outputs.upToDateWhen { false }
}

val verifyPackagedProjectPolicyArtifacts by tasks.registering(Test::class) {
    group = "verification"
    description = "Recompiles the exact Project policy bytes extracted from bootJar and verifies the tracked artifact."
    val packagedJar = tasks.named("bootJar")
    dependsOn(tasks.named("testClasses"), packagedJar)
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    filter {
        includeTestsMatching(
            "com.umc.product.project.application.authorization.ProjectPolicyArtifactBootJarTest"
        )
    }
    doFirst {
        systemProperty("project.policy.artifact.bootJar", packagedJar.get().outputs.files.singleFile.absolutePath)
    }
    reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/$name"))
    reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/$name"))
    binaryResultsDirectory.set(layout.buildDirectory.dir("test-results/$name/binary"))
    outputs.upToDateWhen { false }
}

tasks.named("check") {
    dependsOn(verifyProjectPolicyArtifacts, verifyPackagedProjectPolicyArtifacts)
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
