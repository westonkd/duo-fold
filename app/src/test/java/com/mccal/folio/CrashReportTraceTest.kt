package com.mccal.folio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTraceTest {
    @Test fun `a crash report keeps the types and frames but not the messages`() {
        val cause = IllegalArgumentException("content://contacts/people/Maya Lopez")
        val crash = IllegalStateException("tel:+15550100", cause)
        val trace = CrashLog.traceWithoutMessages(crash)
        assertTrue(trace, trace.startsWith("java.lang.IllegalStateException\n"))
        assertTrue(trace, trace.contains("Caused by: java.lang.IllegalArgumentException\n"))
        assertTrue(trace, trace.contains("\tat com.mccal.folio.CrashReportTraceTest"))
        assertFalse(trace.contains("Maya"))
        assertFalse(trace.contains("5550100"))
    }

    @Test fun `a cause that loops back on itself ends the trace`() {
        val first = RuntimeException("a")
        val second = RuntimeException("b", first)
        first.initCause(second)
        val trace = CrashLog.traceWithoutMessages(first)
        assertTrue(trace.lines().count { it.endsWith("RuntimeException") } == 2)
    }
}
