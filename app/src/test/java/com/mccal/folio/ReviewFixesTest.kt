package com.mccal.folio

import org.junit.Assert.assertTrue
import org.junit.Test

/** Findings from the code review of beta.7: an unreadable icon pack is left alone for a while. */
class ReviewFixesTest {
    private val root = generateSequence(java.io.File("").absoluteFile) { it.parentFile }.first { java.io.File(it, "CHANGELOG.md").exists() }
    private fun source(path: String) = java.io.File(root, "app/src/main/java/com/mccal/folio/$path").readText()

    @Test fun `a pack that could not be read is not read again for every icon`() {
        val text = source("IconPacks.kt").substringAfter("private fun mapping(").substringBefore("private fun readMapping")
        assertTrue(text.contains("failedAt[pack] = now"))
        assertTrue(text.indexOf("RETRY_AFTER_MS") in 0 until text.indexOf("readMapping(context, pack)"))
        assertTrue(source("IconPacks.kt").contains("failedAt.clear()"))
    }
}
