package com.mccal.folio

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Turkish is the language that catches this: uppercasing "i" there gives "İ", not "I". Anything Folio reads as data,
 * such as the English words a What's New symbol is chosen by, has to be cased in the root locale, or Folio behaves
 * differently depending on the phone's language.
 */
class TurkishLocaleTest {
    private val was: Locale = Locale.getDefault()
    @After fun restore() = Locale.setDefault(was)

    private fun inTurkish(body: () -> Unit) { Locale.setDefault(Locale.forLanguageTag("tr-TR")); body() }

    @Test fun `a What's New title keeps its symbol`() = inTurkish {
        // "Icons" lower-cases to "ıcons" in Turkish, which matches no English word at all.
        assertEquals(WhatsNew.symbol("Icons everywhere"), WhatsNew.symbol("icons everywhere"))
    }
}
