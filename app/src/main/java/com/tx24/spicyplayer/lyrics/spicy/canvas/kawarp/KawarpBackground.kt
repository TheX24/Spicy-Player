package com.tx24.spicyplayer.lyrics.spicy.canvas.kawarp

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import com.tx24.spicyplayer.lyrics.spicy.canvas.StackBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A live "Kawarp"-style dynamic background: a domain-warped, blurred cover-art field animated
 * with an AGSL [RuntimeShader]. Ports the feel of the original WebGL Kawarp background; requires
 * API 33+ (guarded by the caller, which falls back to the legacy renderer below that).
 */
private const val KAWARP_AGSL = """
uniform float2 uResolution;
uniform float2 uImageSize;
uniform float uTime;
uniform float uWarp;
uniform shader tex;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / uResolution;
    // Two-octave domain warp driven by time.
    float2 w = float2(
        sin(uv.y * 3.1 + uTime) + 0.5 * sin(uv.x * 5.3 - uTime * 1.7),
        cos(uv.x * 2.7 - uTime * 1.3) + 0.5 * cos(uv.y * 4.9 + uTime)
    );
    uv += w * 0.06 * uWarp;
    uv = clamp(uv, 0.0, 1.0);
    half4 c = tex.eval(uv * uImageSize);
    // Saturation boost + brightness cut, matching the legacy .spicy-dynamic-bg filter.
    half3 rgb = c.rgb;
    half luma = dot(rgb, half3(0.299, 0.587, 0.114));
    rgb = mix(half3(luma), rgb, half(1.8));
    rgb *= half(0.68);
    return half4(rgb, c.a);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun KawarpBackground(
    coverArtBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    blurIntensity: Int = 60,
    animate: Boolean = true,
) {
    val shader = remember { RuntimeShader(KAWARP_AGSL) }
    var time by remember { mutableFloatStateOf(0f) }

    // Prepare a small, blurred, software bitmap for the shader input.
    var prepared by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(coverArtBitmap, blurIntensity) {
        val src = coverArtBitmap ?: run { prepared = null; return@LaunchedEffect }
        prepared = withContext(Dispatchers.Default) {
            val scaled = Bitmap.createScaledBitmap(src, 160, 160, true)
                .copy(Bitmap.Config.ARGB_8888, true)
            val radius = (blurIntensity.coerceIn(0, 100) / 100f * 20f).toInt().coerceAtLeast(1)
            StackBlur.blur(scaled, radius)
        }
    }

    // Fade in each new cover.
    val alpha by animateFloatAsState(
        targetValue = if (prepared != null) 1f else 0f,
        animationSpec = tween(1000),
        label = "kawarpAlpha",
    )

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) time += (now - last) / 1_000_000_000f * 0.6f
                last = now
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val bmp = prepared ?: return@Canvas
        shader.setFloatUniform("uResolution", size.width, size.height)
        shader.setFloatUniform("uImageSize", bmp.width.toFloat(), bmp.height.toFloat())
        shader.setFloatUniform("uTime", time)
        shader.setFloatUniform("uWarp", 1.0f)
        shader.setInputShader("tex", BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        drawRect(brush = ShaderBrush(shader), alpha = alpha.coerceIn(0f, 1f))
    }
}
