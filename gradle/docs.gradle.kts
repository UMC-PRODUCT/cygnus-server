import java.io.File
import org.gradle.api.tasks.Copy

val snippetsDir = layout.buildDirectory.dir("generated-snippets").get().asFile
val asciiDocsSourceDir = "docs/asciidoc"
val asciiDocsDir = "docs/static"

tasks.register("generateRestDocsIndex") {
    dependsOn(tasks.named("test"))

    doLast {
        val indexFile = file("docs/asciidoc/index.adoc")
        indexFile.parentFile.mkdirs()

        val content = buildString {
            appendLine("= UMC PRODUCT API Documentation")
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

            controllerMap.keys.sorted().forEach { controller ->
                val displayName = controller
                    .replace("-controller-test", "")
                    .replace("-", " ")
                    .replaceFirstChar { it.uppercase() }

                appendLine("== $displayName")
                appendLine()

                controllerMap[controller]!!.sorted().forEach { testName ->
                    appendLine("=== $testName")

                    val testDir = file("${snippetsDir}/$controller/$testName")
                    val availableSnippets = listOf(
                        "http-request", "http-response",
                        "path-parameters", "query-parameters",
                        "request-fields", "response-fields"
                    ).filter { snippetType ->
                        File(testDir, "$snippetType.adoc").exists()
                    }

                    if (availableSnippets.isNotEmpty()) {
                        val snippetsParam = availableSnippets.joinToString(",")
                        appendLine("operation::$controller/$testName[snippets='$snippetsParam']")
                    } else {
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

val copyDocument = tasks.register<Copy>("copyDocument") {
    dependsOn(tasks.named("asciidoctor"))

    doFirst {
        println("=".repeat(50))
        println("[asciidoctor] 생성된 Rest Docs를 복사합니다. 기존 문서들을 삭제됩니다.")
        delete(file(asciiDocsDir))
        println("=".repeat(50))
    }

    from(file("build/docs/asciidoc"))
    into(file(asciiDocsDir))

    doLast {
        println("=".repeat(50))
        println("[copyDocument] AsciiDoc 문서를 docs/static에 복사하였습니니다.")
        println("=".repeat(50))
    }
}

tasks.named("build") {
    dependsOn(copyDocument)

    doLast {
        println("[build] gradle build가 완료되었습니다.")
    }
}
