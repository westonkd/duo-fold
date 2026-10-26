package com.mccal.folio

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

@RequiresApi(33)
internal class PageLandingShader {
    private val shader = RuntimeShader(SOURCE)

    fun effect(width: Float, height: Float, pose: PagePose, page: LandingPage): androidx.compose.ui.graphics.RenderEffect {
        val radians = Math.toRadians((PageLanding.PEAK_DEGREES * pose.lift).toDouble()).toFloat()
        shader.setFloatUniform("size", width, height)
        shader.setFloatUniform("axis", if (page.horizontal) 1f else 0f)
        shader.setFloatUniform("hingePos", page.hingePx)
        shader.setFloatUniform("side", if (page.movingAfterHinge) 1f else -1f)
        shader.setFloatUniform("tilt", radians)
        shader.setFloatUniform("eye", maxOf(width, height) * EYE_DISTANCE)
        shader.setFloatUniform("crease", pose.shade * CREASE_SHADE)
        shader.setFloatUniform("creaseWidth", (if (page.horizontal) height else width) * CREASE_REACH)
        return RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }

    companion object {
        private const val EYE_DISTANCE = 1.8f
        private const val CREASE_SHADE = .2f
        private const val CREASE_REACH = .05f

        private const val SOURCE = """
            uniform shader content;
            uniform float2 size;
            uniform float axis;
            uniform float hingePos;
            uniform float side;
            uniform float tilt;
            uniform float eye;
            uniform float crease;
            uniform float creaseWidth;

            half4 main(float2 p) {
                float along = axis < 0.5 ? p.x : p.y;
                float across = axis < 0.5 ? p.y : p.x;
                float alongCenter = axis < 0.5 ? size.x * 0.5 : size.y * 0.5;
                float acrossCenter = axis < 0.5 ? size.y * 0.5 : size.x * 0.5;
                float shade = crease * exp(-abs(along - hingePos) / max(creaseWidth, 1.0));
                float u = (along - hingePos) * side;
                if (u <= 0.0 || tilt <= 0.0) {
                    half4 still = content.eval(p);
                    return half4(still.rgb * half(1.0 - shade), still.a);
                }
                float s = sin(tilt);
                float c = cos(tilt);
                float d = eye * (along - hingePos) / ((along - alongCenter) * s + side * c * eye);
                d = abs(d);
                float k = eye / (eye - d * s);
                float sourceAlong = hingePos + side * d;
                float sourceAcross = acrossCenter + (across - acrossCenter) / k;
                float2 q = axis < 0.5 ? float2(sourceAlong, sourceAcross) : float2(sourceAcross, sourceAlong);
                half4 moved = content.eval(q);
                return half4(moved.rgb * half(1.0 - shade), moved.a);
            }
        """
    }
}
