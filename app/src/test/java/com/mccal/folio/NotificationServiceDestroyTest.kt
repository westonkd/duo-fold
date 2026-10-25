package com.mccal.folio

import org.junit.Assert.assertTrue
import org.junit.Test

/** A review finding on the notification fixes: a destroyed notification service stops watching. */
class NotificationServiceDestroyTest {
    private val root = generateSequence(java.io.File("").absoluteFile) { it.parentFile }.first { java.io.File(it, "CHANGELOG.md").exists() }
    private fun source(path: String) = java.io.File(root, "app/src/main/java/com/mccal/folio/$path").readText()

    @Test fun `the notification service refuses to register media callbacks once it is destroyed`() {
        val listener = source("NotificationListener.kt")
        assertTrue(listener.substringAfter("override fun onDestroy()").take(120).contains("destroyed = true"))
        val watch = listener.substringAfter("private fun watch(").substringBefore("private fun clearControllerCallbacks")
        assertTrue(watch.contains("if (destroyed) return"))
    }
}
