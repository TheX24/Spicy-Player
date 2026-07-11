package com.tx24.spicyplayer.uiNowPlaying.spicy.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tx24.spicyplayer.uiNowPlaying.spicy.animation.SpringSimulation
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

    fun updateScroll(
        currentTimeMs: Long,
        dt: Float,
        totalContentHeight: Float,
        targetY: Float?
    ) {
        val timeJump = abs(currentTimeMs - lastFrameSongTime)
        val isFirstFrame = lastFrameSongTime == 0L
        val isSeek = timeJump > 800L

        if (targetY != null) {
            // Clamp the target goal to valid scroll boundaries to prevent 'fighting' 
            // with the boundary constraints at the very top or bottom of the lyrics.
            val clampedGoal = targetY.coerceIn(-totalContentHeight, 0f)
            
            if (isFirstFrame) {
                scrollSpring.resetTo(clampedGoal)
                userScrollOffset = 0f
                lastInteractionTimeMs = 0L
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

        val timeSinceInteraction = System.currentTimeMillis() - lastInteractionTimeMs
        val isInManualMode = isUserScrolling || (timeSinceInteraction < 4000L && lastInteractionTimeMs > 0L)

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
            lastInteractionTimeMs = System.currentTimeMillis()
        }

        val maxScrollDown = 60f
        val maxScrollUp = -totalContentHeight - 60f
        val totalScroll = actualSpringY + userScrollOffset

        if (!isUserScrolling) {
            // Apply inertia if there is remaining velocity.
            if (abs(scrollVelocity) > 0.1f) {
                userScrollOffset += scrollVelocity * dt
                scrollVelocity *= 0.95f 
                if (abs(scrollVelocity) < 10f) scrollVelocity = 0f
                lastInteractionTimeMs = System.currentTimeMillis()
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
        lastDragTimeMs = System.currentTimeMillis()
    }

    fun onDragEnd() {
        isUserScrolling = false
        userScrollDecayTimer = 0f
    }

    fun onDrag(dy: Float) {
        val now = System.currentTimeMillis()
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
    }
}
