package com.tx24.spicyplayer.lyrics.spicy

import kotlin.math.abs
import kotlin.math.exp

/**
 * Port of the reference's position pipeline (`spicy-lyrics/src/utils/Gets/GetProgress.ts`):
 * a smooth playback clock built in two stages, exactly like the original.
 *
 * **Stage 1 — anchor extrapolation** (the reference's `Position + (Date.now() - StartedSyncAt)`):
 * the raw player position is only trusted as an *anchor*; between changes it is extrapolated
 * on wall-clock time. This is essential — if the platform position steps coarsely (e.g. a
 * `MediaController` that refreshes over IPC), feeding it raw into a smoother makes the
 * predicted clock rubber-band and eventually *snap* (the sweep visibly jumping to complete).
 * Anchoring makes the measured signal itself continuous first.
 *
 * **Stage 2 — jitter filter** (`normalizeProgress`): a predicted clock advancing on wall-clock
 * time, pulled toward the extrapolated measurement with a frame-rate-independent low-pass
 * (`alpha = 1 − exp(−elapsed / 300ms)`); deltas above 500ms (seeks, track changes) snap.
 *
 * While paused the clock returns the raw measured position and forgets its state; while
 * playing a fixed +100ms lead compensates audio-output latency (the reference's dial).
 */
class PlaybackClock {

    // Stage 1: anchor for extrapolating the (possibly coarse) measured position.
    private var anchorMs = 0.0
    private var anchorAtMs = 0L
    private var lastMeasuredMs = Long.MIN_VALUE

    // Stage 2: predicted clock.
    private var predictedMs = 0.0
    private var updatedAtMs = 0L
    private var initialized = false

    companion object {
        const val PROGRESS_POSITION_OFFSET_MS = 100L
        const val JITTER_RESYNC_THRESHOLD_MS = 500.0
        const val JITTER_TIME_CONSTANT_MS = 300.0
    }

    /** Forgets all predictive state (call on track change). */
    fun reset() {
        initialized = false
        lastMeasuredMs = Long.MIN_VALUE
    }

    /**
     * @param measuredMs the player-reported position (may step coarsely).
     * @param isPlaying whether playback is advancing.
     * @param nowMs a monotonic wall-clock timestamp in ms (frame time).
     * @return the smoothed position, with the +100ms lead applied while playing.
     */
    fun positionMs(measuredMs: Long, isPlaying: Boolean, nowMs: Long): Long {
        if (!isPlaying) {
            reset()
            return measuredMs
        }

        // Stage 1: re-anchor whenever the measurement moves; extrapolate in between.
        if (lastMeasuredMs == Long.MIN_VALUE || measuredMs != lastMeasuredMs) {
            anchorMs = measuredMs.toDouble()
            anchorAtMs = nowMs
            lastMeasuredMs = measuredMs
        }
        val extrapolated = anchorMs + (nowMs - anchorAtMs).coerceAtLeast(0L)

        // Stage 2: predicted clock pulled toward the extrapolated measurement.
        if (!initialized) {
            predictedMs = extrapolated
            updatedAtMs = nowMs
            initialized = true
            return predictedMs.toLong() + PROGRESS_POSITION_OFFSET_MS
        }

        val elapsed = (nowMs - updatedAtMs).coerceAtLeast(0L).toDouble()
        var predicted = predictedMs + elapsed

        val error = extrapolated - predicted
        if (abs(error) > JITTER_RESYNC_THRESHOLD_MS) {
            timber.log.Timber.tag("SpicyClock").d(
                "SNAP err=%.0fms measured=%d extrapolated=%.0f predicted=%.0f",
                error, measuredMs, extrapolated, predicted,
            )
            predicted = extrapolated
        } else {
            val alpha = 1.0 - exp(-elapsed / JITTER_TIME_CONSTANT_MS)
            predicted += error * alpha
        }

        predictedMs = predicted.coerceAtLeast(0.0)
        updatedAtMs = nowMs
        return predictedMs.toLong() + PROGRESS_POSITION_OFFSET_MS
    }
}
