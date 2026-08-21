package com.tx24.spicyplayer.lyrics.spicy.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tx24.spicyplayer.lyrics.spicy.animation.SpringSimulation
import kotlin.math.abs

internal class ScrollManager(
    private val scrollSpring: SpringSimulation = SpringSimulation(0f, 1.5f, 1.0f)
) {
    var userScrollOffset by mutableFloatStateOf(0f)
    var isUserScrolling by mutableStateOf(false)
        private set
    private var userScrollDecayTimer by mutableFloatStateOf(0f)
    private var lastInteractionTimeMs by mutableLongStateOf(0L)
    private var lastFrameSongTime by mutableLongStateOf(0L)
    private var wasPlaying by mutableStateOf(false)
    
    var animScrollY by mutableFloatStateOf(0f)
        private set

    private var scrollVelocity by mutableFloatStateOf(0f)
    private var lastDragTimeMs by mutableLongStateOf(0L)
    private var snapNextTarget = false

    fun reset() {
        scrollSpring.resetTo(0f)
        userScrollOffset = 0f
        isUserScrolling = false
        userScrollDecayTimer = 0f
        lastInteractionTimeMs = 0L
        lastFrameSongTime = 0L
        wasPlaying = false
        animScrollY = 0f
        scrollVelocity = 0f
        lastDragTimeMs = 0L
        snapNextTarget = false
    }

    fun updateScroll(
        currentTimeMs: Long,
        dt: Float,
        totalContentHeight: Float,
        targetY: Float?,
        snap: Boolean = false,
        targetVisiblePx: Float = Float.POSITIVE_INFINITY,
    ) {
        val timeJump = abs(currentTimeMs - lastFrameSongTime)
        val isFirstFrame = lastFrameSongTime == 0L
        val isSeek = timeJump > 1000L

        if (targetY != null) {
            // Clamp the target goal to valid scroll boundaries to prevent 'fighting' 
            // with the boundary constraints at the very top or bottom of the lyrics.
            val clampedGoal = targetY.coerceIn(-totalContentHeight, 0f)
            
            if (isFirstFrame || snap || snapNextTarget) {
                scrollSpring.setGoal(clampedGoal, replacePosition = true)
                userScrollOffset = 0f
                lastInteractionTimeMs = 0L
                scrollVelocity = 0f
                snapNextTarget = false
            } else if (isSeek) {
                userScrollOffset = 0f
                lastInteractionTimeMs = 0L
                scrollSpring.setGoal(clampedGoal)
            } else {
                scrollSpring.setGoal(clampedGoal)
            }
        }
        
        val springPosBefore = scrollSpring.current
        val actualSpringY = scrollSpring.step(dt)
        val springDelta = actualSpringY - springPosBefore

        val timeSinceInteraction = monotonicNowMs() - lastInteractionTimeMs
        // Resume auto-scroll 750ms after the user stops interacting (reference: USER_SCROLL_COOLDOWN).
        val targetStillDisconnected = userScrollOffset != 0f && targetVisiblePx < 5f
        val isInManualMode = isUserScrolling || targetStillDisconnected ||
            (timeSinceInteraction < 750L && lastInteractionTimeMs > 0L)

        if (isInManualMode) {
            // Cancel out the auto-scroll movement to keep the view static where the user left it.
            userScrollOffset -= springDelta
        }

        val isPlaying = currentTimeMs != lastFrameSongTime
        if (isPlaying && !wasPlaying) {
            lastInteractionTimeMs = 0L // Resume auto-scrolling when playback starts.
        }
        wasPlaying = isPlaying
        lastFrameSongTime = currentTimeMs

        if (isUserScrolling) {
            lastInteractionTimeMs = monotonicNowMs()
        }

        val maxScrollDown = 60f
        val maxScrollUp = -totalContentHeight - 60f
        val totalScroll = actualSpringY + userScrollOffset

        if (!isUserScrolling) {
            // Apply inertia if there is remaining velocity.
            if (abs(scrollVelocity) > 0.1f) {
                userScrollOffset += scrollVelocity * dt
                scrollVelocity *= ScrollPolicyController.flingDecayMultiplier(dt)
                if (abs(scrollVelocity) < 10f) scrollVelocity = 0f
                lastInteractionTimeMs = monotonicNowMs()
            }

            // Boundaries & Focus Recovery.
            if (totalScroll > maxScrollDown) {
                userScrollOffset += (maxScrollDown - totalScroll) * 0.15f
                scrollVelocity = 0f
            } else if (totalScroll < maxScrollUp) {
                userScrollOffset += (maxScrollUp - totalScroll) * 0.15f
                scrollVelocity = 0f
            } else if (isPlaying && !isInManualMode && userScrollOffset != 0f) {
                userScrollDecayTimer += dt
                if (userScrollDecayTimer > 0.2f) {
                    userScrollOffset *= (1f - 0.15f * dt * 60f).coerceIn(0.8f, 0.99f)
                    if (abs(userScrollOffset) < 0.5f) {
                        userScrollOffset = 0f
                        userScrollDecayTimer = 0f
                    }
                }
            } else {
                userScrollDecayTimer = 0f
            }
        } else {
            userScrollDecayTimer = 0f
            // Resistance when scrolling past boundaries during active drag.
            if (totalScroll > maxScrollDown) {
                userScrollOffset += (maxScrollDown - totalScroll) * 0.5f * dt
            } else if (totalScroll < maxScrollUp) {
                userScrollOffset += (maxScrollUp - totalScroll) * 0.5f * dt
            }
        }

        animScrollY = actualSpringY + userScrollOffset
    }

    fun onDragStart() {
        isUserScrolling = true
        userScrollDecayTimer = 0f
        scrollVelocity = 0f
        lastDragTimeMs = monotonicNowMs()
    }

    fun onDragEnd() {
        isUserScrolling = false
        userScrollDecayTimer = 0f
    }

    fun onDrag(dy: Float) {
        val now = monotonicNowMs()
        val dtSec = (now - lastDragTimeMs) / 1000f
        if (dtSec > 0) {
            val instantV = dy / dtSec
            scrollVelocity = scrollVelocity * 0.4f + instantV * 0.6f
        }
        lastDragTimeMs = now
        userScrollOffset += dy
    }

    fun onSeek() {
        // Re-base the auto-scroll spring so it starts its new journey from 
        // the EXACT visual position the user is currently seeing.
        scrollSpring.resetTo(animScrollY)
        userScrollOffset = 0f
        userScrollDecayTimer = 0f
        lastInteractionTimeMs = 0L
        scrollVelocity = 0f
        snapNextTarget = true
    }

    private fun monotonicNowMs(): Long = System.nanoTime() / 1_000_000L
}
