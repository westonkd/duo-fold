// SPDX-License-Identifier: MIT
// Classic Glass's projection is ported from
// joeconsorti/duo-fold-live (MIT, Copyright (c) 2026 Duo Fold Live contributors), ClassicGlassShader.kt at 8ca4371bf4c8,
// itself adapted from chuspeeism/iphone-duo (MIT). See third_party/duet/PROVENANCE.md.
package com.mccal.folio.duet

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.mccal.folio.FoldGeometry

/**
 * Duet's fold shader: Apple's iPhone Duo, 1:1 with chuspeeism/iphone-duo's model of Apple's own device. Only the
 * moving half changes, seen from a fixed eye straight on (Classic Glass's projection), with the 72 px blur and 2x shade,
 * and a hard edge at the hinge because the camera half never moves. With Reduce Motion it is a shade: the darkness
 * and the light sweep stay, the frost and the tilt go.
 */
@RequiresApi(33)
internal class DuetShader {
    private val shader = RuntimeShader(SOURCE)

    fun effect(width: Float, height: Float, m: Float, cover: Boolean, geometry: FoldGeometry,
        reduceMotion: Boolean, cornerPx: Float = 0f): androidx.compose.ui.graphics.RenderEffect {
        shader.setFloatUniform("size", width, height)
        shader.setFloatUniform("axis", if (geometry.horizontal) 1f else 0f)
        shader.setFloatUniform("hingePos", geometry.hingePx)
        shader.setFloatUniform("side", if (geometry.movingAfterHinge) 1f else -1f)
        shader.setFloatUniform("m", m)
        // 72px on a 1600px-wide canvas in the recreation ≈ 4.5% of width; the cover uses a light version.
        shader.setFloatUniform("maxRadius", width * .045f)
        shader.setFloatUniform("cover", if (cover) 1f else 0f)
        shader.setFloatUniform("frost", if (reduceMotion) 0f else 1f)
        shader.setFloatUniform("corner", cornerPx)
        return RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }

    companion object {
        private const val SOURCE = """
            uniform shader content;
            uniform float2 size;
            uniform float m;
            uniform float maxRadius;
            uniform float cover;
            uniform float axis;     // 0: the hinge runs top to bottom (compare x); 1: side to side (compare y)
            uniform float hingePos; // the hinge along that axis
            uniform float side;     // -1: the moving half (or the cover's hinge edge) is before it; +1: after it
            uniform float frost;    // 1: frosted and tilted; 0: Reduce Motion's shade
            uniform float corner;   // the display's corner radius in px

            // Above 0 when a point on the flat screen falls outside the panel's rounded corners. A tilted pane is
            // the panel itself, so where its corners project there is nothing to see: they come out rounded.
            float outsidePanel(float2 q) {
                if (corner <= 0.0) return -1.0;
                float2 d = abs(q - size * 0.5) - (size * 0.5 - corner);
                return length(max(d, 0.0)) - corner;
            }


            // 24-tap disk (3 rings) keeps large radii smooth.
            half4 blur(float2 p, float r) {
                if (r < 0.75) return content.eval(p);
                float a = r * 0.33; float b = r * 0.66; float c = r;
                float a7 = a * 0.7071; float b7 = b * 0.7071; float c7 = c * 0.7071;
                half4 sum = content.eval(p) * 0.08;
                sum += (content.eval(p + float2(a, 0.0)) + content.eval(p + float2(-a, 0.0)) + content.eval(p + float2(0.0, a)) + content.eval(p + float2(0.0, -a))
                      + content.eval(p + float2(a7, a7)) + content.eval(p + float2(-a7, a7)) + content.eval(p + float2(a7, -a7)) + content.eval(p + float2(-a7, -a7))) * 0.05;
                sum += (content.eval(p + float2(b, 0.0)) + content.eval(p + float2(-b, 0.0)) + content.eval(p + float2(0.0, b)) + content.eval(p + float2(0.0, -b))
                      + content.eval(p + float2(b7, b7)) + content.eval(p + float2(-b7, b7)) + content.eval(p + float2(b7, -b7)) + content.eval(p + float2(-b7, -b7))) * 0.04;
                sum += (content.eval(p + float2(c, 0.0)) + content.eval(p + float2(-c, 0.0)) + content.eval(p + float2(0.0, c)) + content.eval(p + float2(0.0, -c))
                      + content.eval(p + float2(c7, c7)) + content.eval(p + float2(-c7, c7)) + content.eval(p + float2(c7, -c7)) + content.eval(p + float2(-c7, -c7))) * 0.025;
                return sum;
            }

            half4 main(float2 p) {
                float mc = clamp(m, 0.0, 1.0);
                float mm = mc * mc * (3.0 - 2.0 * mc) * max(1.0, m); // smoothstep (as in the recreation), scaled by intensity
                float coord = axis < 0.5 ? p.x : p.y;
                float extent = axis < 0.5 ? size.x : size.y;
                if (cover > 0.5) {
                    // Outer screen, as on iPhone Duo: blur and darkness grow away from the hinge edge
                    // toward the free edge. Same curves as the inner half.
                    float eo = clamp(side < 0.0 ? coord / extent : (extent - coord) / extent, 0.0, 1.0);
                    half4 co = blur(p, maxRadius * frost * mm * pow(eo, 1.35));
                    float dO = clamp((eo - 0.2) / 0.8, 0.0, 1.0);
                    float ko = 1.0 - min(1.0, 2.0 * mm * pow(dO, 1.35));
                    return half4(co.rgb * ko, co.a);
                }
                // Inner screen: the half with the cover behind it stays sharp; the moving half is blurred and
                // darkened toward its outer edge.
                float past = side < 0.0 ? coord - hingePos : hingePos - coord;
                if (past >= 0.0) return content.eval(p);
                float e; float hs;
                if (side < 0.0) {
                    hs = max(hingePos, 1.0);
                    e = clamp((hingePos - coord) / hs, 0.0, 1.0);
                } else {
                    hs = max(extent - hingePos, 1.0);
                    e = clamp((coord - hingePos) / hs, 0.0, 1.0);
                }
                float d = clamp((e - 0.2) / 0.8, 0.0, 1.0);
                float k = 1.0 - min(1.0, 2.0 * mm * pow(d, 1.35));
                if (frost > 0.5) {
                    // Duo Fold Live / iphone-duo: an eye at z = 40 over a strip 15.7987 units across (the whole
                    // screen); the half moves in that strip, so its far edge is 7.899 units from the hinge.
                    float a = min(mc, 0.97) * 1.570796327;
                    float e2 = e * cos(a) * 40.0 / max(1.0, 40.0 - e * 7.89935 * sin(a));
                    float c2 = side < 0.0 ? hingePos - e2 * hs : hingePos + e2 * hs;
                    float2 q = axis < 0.5 ? float2(c2, p.y) : float2(p.x, c2);
                    if (outsidePanel(q) > 0.0) return half4(0.0, 0.0, 0.0, 1.0);
                    // Apple's model samples a pre-blurred mip at each of its 25 wide taps; a live screen has no mips,
                    // so those taps show as ghost copies of every edge (seen in DuetRenderCheckTest). Folio's disk
                    // at twice the radius has about the same spread (binomial at spacing r: sigma = r) and no ghosts.
                    half4 g = blur(q, 2.0 * maxRadius * mm * pow(e, 1.35));
                    return half4(g.rgb * k, g.a);
                }
                half4 c = content.eval(p);
                // A soft band of light that leaves the hinge and crosses the half as it clears, brightest mid-way
                // (idea from FoldFX).
                float band = (e - (1.0 - mc)) / 0.1;
                float sweep = exp(-band * band) * 0.55 * mc * (1.0 - mc);
                return half4(c.rgb * k + half3(sweep) * c.a, c.a);
            }
        """
    }
}
