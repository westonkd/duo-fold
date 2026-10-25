package com.mccal.folio.duet

/** Duet's look, as numbers over the one fold shader ([DuetShader]). */
data class DuetStyle(
    /** Blur strength, 1 = Folio's original fold. */
    val frost: Float,
    /** How dark the moving half gets toward its outer edge, 1 = original. */
    val darkening: Float,
    /** 0 = flat glass (original); above 0 the moving half tilts toward you in perspective (hingewave's model). */
    val perspective: Float,
    /** Share of the blur already there at the hinge, so the half frosts as one pane (hingewave's `blurFloor`). */
    val blurFloor: Float,
    /**
     * Duo Fold Live's Classic Glass: iphone-duo's eye at a fixed distance looking at the folding strip, instead of
     * hingewave's tilted pane. [perspective] then scales its tilt.
     */
    val classic: Boolean = false,
    /**
     * How far past the hinge the frost carries into the still half, as a share of the whole screen (Duo Fold Live's
     * seam blend, 7% by default there), so the frost fades out instead of stopping at a hard line. 0 = hard edge.
     */
    val softEdge: Float = 0f,
    /**
     * How frosted the open screen is when its panel lights, 1 = fully. Apple's frost follows the hinge; the Fold8 lights
     * its inner panel at about 125 degrees, where Apple's would be about 61%, so iPhone Duo starts there.
     */
    val startAt: Float = 1f,
)

object DuetStyles {
    /**
     * Apple's iPhone Duo, 1:1 with chuspeeism/iphone-duo's model of Apple's own device: only the cover half moves,
     * seen from a fixed eye straight on (Classic Glass's projection), the 72 px blur and 2x shade, a hard edge at the
     * hinge because the camera half never moves, and no screenshot, because Folio's inner right page is the cover's
     * page. It starts at the frost the real hinge angle gives when the inner panel lights.
     */
    val IPHONE = DuetStyle(frost = 1f, darkening = 1f, perspective = 1f, blurFloor = 0f, classic = true, softEdge = 0f, startAt = .61f)

    /** With Reduce Motion on, any style becomes a shade: its darkness stays, frost, tilt and soft edge go. */
    fun reducedMotion(style: DuetStyle) = style.copy(frost = 0f, perspective = 0f, blurFloor = 0f, softEdge = 0f, classic = false)
}
