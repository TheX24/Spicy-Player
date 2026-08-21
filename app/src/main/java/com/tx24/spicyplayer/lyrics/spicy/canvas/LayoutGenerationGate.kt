package com.tx24.spicyplayer.lyrics.spicy.canvas

internal class LayoutGenerationGate {
    private var generation = 0L

    fun next(): Long = ++generation

    fun isCurrent(candidate: Long): Boolean = candidate == generation
}
