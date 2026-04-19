package com.tx24.spicyplayer.ui.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RectF

/**
 * Renders the spicy-lyrics style dynamic background effect.
 *
 * Algorithm: 4 overlapping circles, each showing the same blurred, circular-cropped
 * album cover texture, rotating at different angular velocities and drawn with
 * different alpha values. A color matrix applies saturate(2.5) + brightness(0.65).
 *
 * The circles are (from back to front):
 * 1. Background — center, radius = 1.5× largestAxis, rotation = -0.25× angle, alpha = 1.0
 * 2. Center     — center, radius = 0.75–1.0× largestAxis, rotation = +0.5× angle, alpha = 0.75
 * 3. Left       — bottom-left, radius = 0.75× largestAxis, rotation = +1.0× angle, alpha = 0.5
 * 4. Right      — top-right, radius = 0.5–0.65× largestAxis, rotation = -0.75× angle, alpha = 0.5
 *
 * Ported from:
 * - @spikerko/tools DynamicBackground.ts
 * - @spikerko/tools DBG_ThreeShaders.ts
 * - spicy-lyrics/css/DynamicBG/spicy-dynamic-bg.css (saturate 2.5, brightness 0.65)
 */
class DynamicBackgroundRenderer {

    companion object {
        /** Blur amount matching the original config. */
        const val BLUR_AMOUNT = 45

        /** Rotation speed in radians per second. */
        const val ROTATION_SPEED = 0.25f

        /** CSS saturate(2.5) equivalent. */
        const val SATURATE = 2.5f

        /** CSS brightness(0.65) equivalent. */
        const val BRIGHTNESS = 0.65f

        /** Resolution for the downscaled blur texture. */
        private const val TEXTURE_SIZE = 128
    }

    /** The pre-blurred, circular-cropped current texture bitmap. */
    private var blurredTexture: Bitmap? = null

    /** The pre-blurred, circular-cropped previous texture bitmap for cross-fading. */
    private var previousTexture: Bitmap? = null

    /** The source image URL/path that produced the current texture (for dedup). */
    private var currentSourceId: String? = null

    /** Combined color matrix for saturate + brightness post-processing. */
    private val colorMatrixFilter: ColorMatrixColorFilter

    /** Paint used for drawing the circles. */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** Reusable path for circle clipping. */
    private val clipPath = Path()

    /** Reusable rect for bitmap destination. */
    private val dstRect = RectF()

    init {
        // Build a color matrix that combines saturate(2.5) and brightness(0.65).
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(SATURATE)

        val brightMatrix = ColorMatrix(floatArrayOf(
            BRIGHTNESS, 0f, 0f, 0f, 0f,
            0f, BRIGHTNESS, 0f, 0f, 0f,
            0f, 0f, BRIGHTNESS, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))

        // Combine: first saturate, then brightness
        brightMatrix.preConcat(satMatrix)
        colorMatrixFilter = ColorMatrixColorFilter(brightMatrix)
        paint.colorFilter = colorMatrixFilter
    }

    /**
     * Sets the cover art bitmap to use as the background texture.
     * The bitmap is downscaled, circular-cropped, and blurred.
     * This is an expensive operation and should be called off the main thread.
     *
     * @param source The album cover art bitmap.
     * @param sourceId A unique identifier for deduplication (e.g. file path + blur level).
     * @param blurIntensity Blur intensity 0–100 (default 60).
     */
    fun setImage(source: Bitmap, sourceId: String, blurIntensity: Int = 60) {
        if (sourceId == currentSourceId) return
        
        // Move current to previous for cross-fade
        previousTexture?.recycle()
        previousTexture = blurredTexture
        
        currentSourceId = sourceId

        // 1. Scale down to TEXTURE_SIZE x TEXTURE_SIZE
        val size = TEXTURE_SIZE
        val scaled = Bitmap.createScaledBitmap(source, size, size, true)

        // 2. Crop to a circle
        val circled = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(circled)
        val circleClip = Path()
        circleClip.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
        canvas.clipPath(circleClip)
        canvas.drawBitmap(scaled, 0f, 0f, null)

        // 3. Scale blur amount by intensity (0 → radius 1, 100 → radius BLUR_AMOUNT)
        val effectiveBlur = (BLUR_AMOUNT * blurIntensity / 100f).toInt().coerceAtLeast(1)

        // 4. Create expanded bitmap with padding to accommodate blur spread
        val blurRadius = (effectiveBlur * size / 640f).toInt().coerceAtLeast(1)
        val blurExtent = (3 * blurRadius * 1.5f).toInt()
        val expandedSize = size + blurExtent
        val expanded = Bitmap.createBitmap(expandedSize, expandedSize, Bitmap.Config.ARGB_8888)
        val expandedCanvas = Canvas(expanded)
        val offset = blurExtent / 2f
        expandedCanvas.drawBitmap(circled, offset, offset, null)

        // 5. Apply blur
        blurredTexture = StackBlur.blur(expanded, blurRadius)

        // Recycle intermediates
        if (scaled !== source) scaled.recycle()
        circled.recycle()
        // expanded is consumed by blur which returns a copy
    }

    /**
     * Draws the dynamic background onto the given Android Canvas.
     * Call this from a Compose Canvas's `drawIntoCanvas` block.
     *
     * @param nativeCanvas The native Android Canvas to draw on.
     * @param width The canvas width in pixels.
     * @param height The canvas height in pixels.
     * @param rotationAngle The current rotation angle in radians.
     * @param transitionProgress The progress of the cross-fade transition (0.0 to 1.0).
     */
    fun draw(nativeCanvas: Canvas, width: Float, height: Float, rotationAngle: Float, transitionProgress: Float = 1.0f) {
        val current = blurredTexture
        val previous = previousTexture

        if (current == null && previous == null) return

        val largestAxis = maxOf(width, height)
        val isXLarger = width > height
        val centerX = width / 2f
        val centerY = height / 2f

        // If transitioning, draw previous background first then current with globalAlpha overlay
        // Or better: draw both with their respective alphas.
        
        fun drawAllCircles(texture: Bitmap, globalAlpha: Float) {
            // Background circle
            drawCircle(
                nativeCanvas, texture,
                cx = centerX, cy = centerY,
                radius = largestAxis * 1.5f,
                rotationDegrees = Math.toDegrees((-0.25f * rotationAngle).toDouble()).toFloat(),
                alpha = 1.0f * globalAlpha
            )

            // Center circle
            drawCircle(
                nativeCanvas, texture,
                cx = centerX, cy = centerY,
                radius = largestAxis * if (isXLarger) 1.0f else 0.75f,
                rotationDegrees = Math.toDegrees((0.5f * rotationAngle).toDouble()).toFloat(),
                alpha = 0.75f * globalAlpha
            )

            // Left circle
            drawCircle(
                nativeCanvas, texture,
                cx = 0f, cy = height,
                radius = largestAxis * 0.75f,
                rotationDegrees = Math.toDegrees((1.0f * rotationAngle).toDouble()).toFloat(),
                alpha = 0.5f * globalAlpha
            )

            // Right circle
            drawCircle(
                nativeCanvas, texture,
                cx = width, cy = 0f,
                radius = largestAxis * if (isXLarger) 0.65f else 0.5f,
                rotationDegrees = Math.toDegrees((-0.75f * rotationAngle).toDouble()).toFloat(),
                alpha = 0.5f * globalAlpha
            )
        }

        if (transitionProgress < 1.0f && previous != null) {
            drawAllCircles(previous, 1.0f - transitionProgress)
        }
        
        if (current != null) {
            drawAllCircles(current, transitionProgress)
        }
    }

    /**
     * Draws a single rotated, circular-clipped texture instance.
     */
    private fun drawCircle(
        canvas: Canvas,
        texture: Bitmap,
        cx: Float, cy: Float,
        radius: Float,
        rotationDegrees: Float,
        alpha: Float
    ) {
        canvas.save()

        // Clip to circle
        clipPath.reset()
        clipPath.addCircle(cx, cy, radius, Path.Direction.CW)
        canvas.clipPath(clipPath)

        // Rotate around the circle center
        canvas.rotate(rotationDegrees, cx, cy)

        // Draw texture scaled to fill the circle's bounding box
        val diameter = radius * 2f
        dstRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        paint.alpha = (alpha * 255).toInt()
        canvas.drawBitmap(texture, null, dstRect, paint)

        canvas.restore()
    }

    /**
     * Returns true if this renderer has a prepared texture ready to draw.
     */
    fun hasTexture(): Boolean = blurredTexture != null

    /**
     * Releases bitmap resources.
     */
    fun release() {
        blurredTexture?.recycle()
        blurredTexture = null
        previousTexture?.recycle()
        previousTexture = null
        currentSourceId = null
    }
}
