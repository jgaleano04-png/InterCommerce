package com.intercommerce.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Enforces that every `domain` package is plain Kotlin. The project is a single Gradle module, so
 * the compiler can't forbid these imports; this test fails the build instead.
 */
class DomainPurityTest {

    private val allowedImports = listOf(
        "kotlin.",
        "kotlinx.coroutines.",
        "java.math.",
        "javax.inject.",
        // paging-common is a pure Kotlin artifact; only the PagingData type crosses into domain.
        "androidx.paging.PagingData",
    )

    @Test
    fun `domain packages import only pure Kotlin and other domain code`() {
        val sourceRoot = File("src/main/java/com/intercommerce")
        val domainFiles = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "${File.separator}domain${File.separator}" in it.path }
            .toList()
        assertTrue("No domain sources found under ${sourceRoot.absolutePath}", domainFiles.isNotEmpty())

        val violations = domainFiles.flatMap { file ->
            file.readLines()
                .filter { it.startsWith("import ") }
                .map { it.removePrefix("import ").trim() }
                .filterNot { import -> allowedImports.any(import::startsWith) || isDomainImport(import) }
                .map { "${file.name}: $it" }
        }

        assertTrue("Domain must not depend on frameworks:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun isDomainImport(import: String) =
        import.startsWith("com.intercommerce.") && ".domain." in import
}
