package com.mccal.folio

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Dates come from patterns in strings.xml (StandBy, the Big Clock, Lock Cover, the Side Bar, Today View). A typo in one
 * would throw on the phone; here every pattern is read.
 */
class DatePatternsTest {
    private val res = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "CHANGELOG.md").exists() }.let { File(it, "app/src/main/res") }

    private fun pattern(dir: String, name: String): String =
        Regex("""<string name="$name">(.*?)</string>""").find(File(res, "$dir/strings.xml").readText())
            ?.groupValues?.get(1) ?: error("no $name in $dir")

    private val friday = LocalDate.of(2026, 10, 2)

    private fun check(dir: String, locale: Locale, expected: Map<String, String>) = expected.forEach { (name, date) ->
        assertEquals("$name in $dir", date, friday.format(DateTimeFormatter.ofPattern(pattern(dir, name), locale)))
    }

    @Test fun `English keeps the order it always had`() = check("values", Locale.US, mapOf(
        "eeee_mmmm_d" to "Friday, October 2", "mmmm_d" to "October 2", "mmm_d" to "Oct 2",
        "eee_mmm_d" to "Fri Oct 2", "eee_mmm_d_2" to "Fri, Oct 2",
    ))
}
