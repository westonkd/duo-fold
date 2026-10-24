package com.mccal.folio

import org.junit.Assert.assertTrue
import org.junit.Test

/** Small fixes from the 5 Oct sweep: a few lifecycle hooks stay in place. */
class RobustnessFixesTest {
    private val root = generateSequence(java.io.File("").absoluteFile) { it.parentFile }.first { java.io.File(it, "CHANGELOG.md").exists() }
    private fun source(path: String) = java.io.File(root, "app/src/main/java/com/mccal/folio/$path").readText()

    @Test fun `a change waiting out its delay is written when Home stops or the model is cleared`() {
        assertTrue(source("MainActivity.kt").contains("model.flushPending()"))
        val model = source("LauncherModel.kt")
        assertTrue(model.substringAfter("override fun onCleared()").take(200).contains("flushPending()"))
    }

    @Test fun `the installed icon packs are only asked for off the main thread`() {
        listOf("CustomizationSheet.kt", "LauncherModel.kt").forEach { file ->
            val lines = source(file).lines()
            lines.forEachIndexed { i, line ->
                if (line.contains("IconPacks.installed(")) {
                    val around = lines.subList(maxOf(0, i - 3), i + 1).joinToString("\n")
                    assertTrue("$file:${i + 1} reads icon packs on the main thread", around.contains("Dispatchers.IO"))
                }
            }
        }
    }

    @Test fun `the checkpoint is written off the main thread`() {
        val text = source("Diagnostics.kt").substringAfter("fun checkpoint(").substringBefore("private fun bootCount")
        assertTrue(text.contains("checkpointWriter.execute"))
    }
}
