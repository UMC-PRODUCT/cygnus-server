import java.io.File
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val checkstyleVersion = libsCatalog.findVersion("checkstyle").get().requiredVersion

extensions.configure<CheckstyleExtension>("checkstyle") {
    toolVersion = checkstyleVersion
    configDirectory.set(layout.projectDirectory.dir("config/checkstyle"))
    configProperties["suppressionFile"] =
        layout.projectDirectory.file("config/checkstyle/naver-checkstyle-suppressions.xml").asFile.absolutePath
    isIgnoreFailures = false
    maxErrors = 0
    maxWarnings = Int.MAX_VALUE
}

val lintBaseRef = providers.gradleProperty("lintBase").orElse("origin/develop")

fun changedJavaFiles(vararg sourceRoots: String) = lintBaseRef.flatMap { baseRef ->
    val diffAgainstBaseProvider = providers.exec {
        commandLine("git", "diff", "--name-only", "--diff-filter=ACMR", "$baseRef...HEAD", "--", *sourceRoots)
        isIgnoreExitValue = true
    }.standardOutput.asText

    val localDiffProvider = providers.exec {
        commandLine("git", "diff", "--name-only", "--diff-filter=ACMR", "HEAD", "--", *sourceRoots)
        isIgnoreExitValue = true
    }.standardOutput.asText

    diffAgainstBaseProvider.zip(localDiffProvider) { baseOutput, localOutput ->
        (baseOutput.lines() + localOutput.lines())
            .asSequence()
            .map(String::trim)
            .filter { it.endsWith(".java") && it.isNotBlank() }
            .distinct()
            .map(::file)
            .filter(File::exists)
            .toList()
    }
}

tasks.withType<Checkstyle>().configureEach {
    classpath = files()
    exclude("**/Q*.java")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<Checkstyle>("checkstyleMain") {
    setSource(files(changedJavaFiles("src/main/java")))
}

tasks.named<Checkstyle>("checkstyleTest") {
    setSource(files(changedJavaFiles("src/test/java")))
}

tasks.register("spotlessTest") {
    group = "verification"
    description = "Runs Spotless checks for Java test sources before executing tests."
    dependsOn(tasks.named("spotlessJavaCheck"))
}
