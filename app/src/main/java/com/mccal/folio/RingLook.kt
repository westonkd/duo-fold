package com.mccal.folio

/**
 * How heavy the status ring is drawn. The stronger ring is thicker and has a clearer empty track; it is on while
 * Android's own Bold text is on, because someone who asked for heavier text wants heavier rings too.
 */
internal data class RingLook(val strokeScale: Float, val trackAlpha: Float) {
    companion object {
        val NORMAL = RingLook(1f, .22f)
        val STRONG = RingLook(1.45f, .40f)

        /**
         * Bold text is on for a defined adjustment above zero. `Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED` is
         * `Int.MAX_VALUE`, which a device or preview can report when it doesn't know, and is not Bold text.
         */
        fun boldTextOn(fontWeightAdjustment: Int) = fontWeightAdjustment in 1 until android.content.res.Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED

        /** [fontWeightAdjustment] is `Configuration.fontWeightAdjustment`: above zero while Bold text is on. */
        fun of(fontWeightAdjustment: Int) = if (boldTextOn(fontWeightAdjustment)) STRONG else NORMAL
    }
}
