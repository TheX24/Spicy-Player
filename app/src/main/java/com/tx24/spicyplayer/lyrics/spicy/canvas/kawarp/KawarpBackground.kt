package com.tx24.spicyplayer.lyrics.spicy.canvas.kawarp

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 1:1 port of @kawarp/core 1.2.0 as configured by spicy-lyrics' dynamicBackground.ts.
 *
 * Image-change path (KawarpBlurCore): tint → 8 Kawase passes at 128×128, stored as
 * RGBA_F16 "album" bitmaps (half-float FBO parity). Per-frame path: one fused AGSL
 * shader = the original's BLEND + DOMAIN_WARP + OUTPUT passes, PLUS the
 * `saturate(2.5) brightness(0.65)` CSS filter Spicetify applies to the canvas element
 * itself (spicy-dynamic-bg.css) — composed in one shader since blend/warp/output are
 * pure functions of uv and the CSS filter is just a final per-pixel color transform.
 */
private const val KAWARP_FUSED_AGSL = """
uniform float2 uResolution;
uniform float uTime;
uniform float uBlend;
uniform float uIntensity;
uniform float uSaturation;
uniform float uDithering;
uniform float uScale;
uniform shader texCur;
uniform shader texNext;

const float BLUR_SIZE = 128.0;

float3 mod289v3(float3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
float2 mod289v2(float2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
float3 permute(float3 x) { return mod289v3(((x * 34.0) + 1.0) * x); }

float snoise(float2 v) {
    const float4 C = float4(0.211324865405187, 0.366025403784439,
                            -0.577350269189626, 0.024390243902439);
    float2 i  = floor(v + dot(v, C.yy));
    float2 x0 = v - i + dot(i, C.xx);
    float2 i1 = (x0.x > x0.y) ? float2(1.0, 0.0) : float2(0.0, 1.0);
    float4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289v2(i);
    float3 p = permute(permute(i.y + float3(0.0, i1.y, 1.0)) + i.x + float3(0.0, i1.x, 1.0));
    float3 m = max(0.5 - float3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
    m = m * m; m = m * m;
    float3 x = 2.0 * fract(p * C.www) - 1.0;
    float3 h = abs(x) - 0.5;
    float3 ox = floor(x + 0.5);
    float3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    float3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.y = a0.y * x12.x + h.y * x12.y;
    g.z = a0.z * x12.z + h.z * x12.w;
    return 130.0 * dot(m, g);
}

float hash(float3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

half4 main(float2 fragCoord) {
    float2 vTexCoord = fragCoord / uResolution;

    // OUTPUT pass uv-scale, applied up front (warp is pure in uv, so order commutes)
    float2 uv = clamp((vTexCoord - 0.5) / uScale + 0.5, 0.0, 1.0);

    // DOMAIN_WARP pass
    float t = uTime * 0.05;
    float2 center = uv - 0.5;
    float centerWeight = 1.0 - smoothstep(0.0, 0.7, length(center));
    float n1 = snoise(uv * 0.35 + float2(t, t * 0.7));
    float n2 = snoise(uv * 0.35 + float2(-t * 0.8, t * 0.5) + float2(50.0, 50.0));
    float n3 = snoise(uv * 0.9 + float2(t * 1.2, -t) + float2(100.0, 0.0));
    float n4 = snoise(uv * 0.9 + float2(-t, t * 1.1) + float2(0.0, 100.0));
    float2 warp = float2(n1 * 0.65 + n3 * 0.35, n2 * 0.65 + n4 * 0.35) * centerWeight;
    float2 warpedUV = clamp(uv + warp * uIntensity, 0.0, 1.0);

    // BLEND pass, fused: sample both album textures at the warped coordinate.
    // GL texel centers are at (i+0.5)/N; eval coords map [0,1] → [0,BLUR_SIZE].
    float2 texPos = warpedUV * BLUR_SIZE;
    half4 c1 = texCur.eval(texPos);
    half4 c2 = texNext.eval(texPos);
    half4 color = mix(c1, c2, half(uBlend));

    // OUTPUT pass: vignette, saturation, dithering (in canvas texcoord space)
    float2 vc = vTexCoord - 0.5;
    float vignette = 1.0 - dot(vc, vc) * 0.3;
    color.rgb *= half(vignette);
    half gray = dot(color.rgb, half3(0.299, 0.587, 0.114));
    color.rgb = mix(half3(gray), color.rgb, half(uSaturation));
    float2 pixelPos = floor(vTexCoord * uResolution);
    float noise = hash(float3(pixelPos, floor(uTime * 60.0)));
    color.rgb += half3(half((noise - 0.5) * uDithering));
    // WebGL's default canvas backbuffer is 8-bit UNORM: this is where the browser
    // clamps before CSS ever sees the pixels, same as here.
    color.rgb = clamp(color.rgb, half3(0.0), half3(1.0));

    // spicy-dynamic-bg.css: `.spicy-dynamic-bg { filter: saturate(2.5) brightness(0.65); }`
    // is applied directly to the canvas element by Spicetify, on top of the shader's own
    // internal saturation/vignette/dither above. CSS saturate() is a luma-preserving mix
    // using Rec.709-ish weights (0.213/0.715/0.072), distinct from the shader's own 0.299/
    // 0.587/0.114 (Rec.601) — kept separate to match the spec exactly. Chained CSS filter
    // primitives each rasterize to 8-bit before the next runs, so clamp between them too —
    // skipping this let saturate(2.5) push channels past 1.0 where brightness(0.65)
    // could no longer pull them back down, which is why the port looked too bright.
    half cssGray = dot(color.rgb, half3(0.213, 0.715, 0.072));
    color.rgb = mix(half3(cssGray), color.rgb, half(2.5));
    color.rgb = clamp(color.rgb, half3(0.0), half3(1.0));
    color.rgb *= half(0.65);

    return color;
}
"""

/** spicy-lyrics `KawarpOptionsStatic`, verbatim. */
private val SPICY_OPTIONS = KawarpOptions(
    warpIntensity = 1f,
    blurPasses = 8,
    animationSpeed = 0.1f,
    saturation = 1.5f,
    dithering = 0.008f,
    transitionDuration = 500f,
    tintIntensity = 0f,
    scale = 1f,
)

/** dynamicBackground.ts: after transitionDuration*2 ms, bump to 1000ms. */
private const val KAWARP_TRANSITION_DURATION_MS = 1000f

@RequiresApi(Build.VERSION_CODES.O)
private fun floatsToF16Bitmap(pixels: FloatArray, size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGBA_F16)
    // Same Color.argb(float...) conversion as before, but built into one IntArray and written
    // with a single setPixels() JNI call instead of size*size (16,384 at BLUR_SIZE) individual
    // setPixel() calls — identical output, far less per-pixel call overhead.
    val colors = IntArray(size * size)
    var i = 0
    for (p in colors.indices) {
        colors[p] = Color.argb(
            pixels[i + 3].coerceIn(0f, 1f),
            pixels[i].coerceIn(0f, 1f),
            pixels[i + 1].coerceIn(0f, 1f),
            pixels[i + 2].coerceIn(0f, 1f),
        )
        i += 4
    }
    bmp.setPixels(colors, 0, size, 0, 0, size, size)
    return bmp
}

private fun bitmapToFloats(src: Bitmap): Triple<FloatArray, Int, Int> {
    val w = src.width; val h = src.height
    val ints = IntArray(w * h)
    src.getPixels(ints, 0, w, 0, 0, w, h)
    val out = FloatArray(w * h * 4)
    for (i in ints.indices) {
        val p = ints[i]
        out[i * 4] = ((p shr 16) and 0xFF) / 255f
        out[i * 4 + 1] = ((p shr 8) and 0xFF) / 255f
        out[i * 4 + 2] = (p and 0xFF) / 255f
        out[i * 4 + 3] = ((p ushr 24) and 0xFF) / 255f
    }
    return Triple(out, w, h)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun KawarpBackground(
    coverArtBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    animate: Boolean = true,
    blurIntensity: Int = 60,
) {
    // spicy-lyrics hardcodes blurPasses=8 (no UI slider); this app exposes one shared
    // slider for both engines. Kawase blur passes are much stronger per-step than the
    // legacy StackBlur radius, so scaling this the same way the legacy path does (0-100%
    // -> 0-20px) makes the slider's default (60%) way blurrier than intended for Kawarp.
    // Anchor 60% to the library's own reference default (8 passes) instead of its max (40).
    val blurPasses = (blurIntensity.coerceIn(0, 100) * 8f / 60f).roundToInt().coerceIn(1, 40)
    val shader = remember { RuntimeShader(KAWARP_FUSED_AGSL) }
    val engine = remember { KawarpEngine(SPICY_OPTIONS) }
    val blackAlbum = remember {
        Bitmap.createBitmap(KawarpBlurCore.BLUR_SIZE, KawarpBlurCore.BLUR_SIZE,
            Bitmap.Config.RGBA_F16).apply { eraseColor(Color.BLACK) }
    }

    // currentAlbum/nextAlbum mirror the original's FBO swap: first cover fades in
    // from the initial black texture, later covers crossfade from the previous one.
    var currentAlbum by remember { mutableStateOf(blackAlbum) }
    var nextAlbum by remember { mutableStateOf(blackAlbum) }
    var frameTick by remember { mutableLongStateOf(0L) }

    // playpause handler from dynamicBackground.ts: paused -> 0.1, playing -> 1
    LaunchedEffect(isPlaying) {
        engine.targetAnimationSpeed = if (isPlaying) 1f else 0.1f
    }

    // dynamicBackground.ts bumps transitionDuration 500 → 1000 after 2×500ms.
    LaunchedEffect(Unit) {
        delay((SPICY_OPTIONS.transitionDuration * 2).toLong())
        engine.transitionDuration = KAWARP_TRANSITION_DURATION_MS
    }

    // processNewImage(): blur off the UI thread, swap FBOs, start the transition.
    // Also re-runs on blurIntensity change, matching reblurCurrentImage()'s behavior
    // when blurPasses changes (though we don't preserve currentAlbum's old blur level).
    LaunchedEffect(coverArtBitmap, blurPasses) {
        val src = coverArtBitmap ?: return@LaunchedEffect
        val blurred = withContext(Dispatchers.Default) {
            val (floats, w, h) = bitmapToFloats(src)
            val out = KawarpBlurCore.process(
                floats, w, h,
                blurPasses = blurPasses,
                tintColor = SPICY_OPTIONS.tintColor,
                tintIntensity = SPICY_OPTIONS.tintIntensity,
            )
            floatsToF16Bitmap(out, KawarpBlurCore.BLUR_SIZE)
        }
        currentAlbum = nextAlbum
        nextAlbum = blurred
        engine.startTransition(System.currentTimeMillis())
    }

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) engine.tick((now - last) / 1_000_000_000f)
                last = now
                frameTick = now // invalidate the Canvas
            }
        }
    }

    // The album BitmapShaders and the ShaderBrush wrap objects that only change on a cover swap;
    // recreating them every frame (as the draw block used to) was pure per-frame allocation.
    val texCur = remember(currentAlbum) {
        BitmapShader(currentAlbum, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val texNext = remember(nextAlbum) {
        BitmapShader(nextAlbum, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    Canvas(modifier = modifier.fillMaxSize()) {
        @Suppress("UNUSED_EXPRESSION") frameTick
        val blend = engine.blendFactor(System.currentTimeMillis())
        shader.setFloatUniform("uResolution", size.width, size.height)
        shader.setFloatUniform("uTime", engine.accumulatedTime)
        shader.setFloatUniform("uBlend", blend)
        shader.setFloatUniform("uIntensity", SPICY_OPTIONS.warpIntensity)
        shader.setFloatUniform("uSaturation", SPICY_OPTIONS.saturation)
        shader.setFloatUniform("uDithering", SPICY_OPTIONS.dithering)
        shader.setFloatUniform("uScale", SPICY_OPTIONS.scale)
        shader.setInputShader("texCur", texCur)
        shader.setInputShader("texNext", texNext)
        drawRect(brush = shaderBrush)
    }
}
