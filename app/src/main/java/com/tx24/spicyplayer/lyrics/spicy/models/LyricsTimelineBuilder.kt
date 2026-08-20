package com.tx24.spicyplayer.lyrics.spicy.models

private const val NORMAL_INTERLUDE_THRESHOLD_MS = 3_000L
private const val MINIMAL_INTERLUDE_THRESHOLD_MS = 5_000L

/**
 * Builds the presentation timeline from normalized vocal lines. Background vocals remain next
 * to their lead group but never participate in gap detection.
 */
fun buildDisplayTimeline(lines: List<Line>, minimalMode: Boolean): List<Line> {
    if (lines.isEmpty()) return emptyList()

    val threshold = if (minimalMode) MINIMAL_INTERLUDE_THRESHOLD_MS else NORMAL_INTERLUDE_THRESHOLD_MS
    val ordered = lines.sortedWith(compareBy<Line> { it.startMs }.thenBy { it.role.ordinal })
    val leads = ordered.filter { it.role == LineRole.LEAD }
    if (leads.isEmpty()) return ordered

    val interludes = ArrayList<Line>()
    val firstLead = leads.first()
    if (firstLead.startMs >= threshold) {
        interludes += Line(
            words = emptyList(),
            startMs = 0L,
            endMs = firstLead.startMs,
            role = LineRole.INTERLUDE,
        )
    }

    for (index in 0 until leads.lastIndex) {
        val current = leads[index]
        val next = leads[index + 1]
        if (next.startMs - current.endMs >= threshold) {
            interludes += Line(
                words = emptyList(),
                startMs = current.endMs,
                endMs = next.startMs,
                role = LineRole.INTERLUDE,
            )
        }
    }

    return (ordered + interludes).sortedWith(
        compareBy<Line> { it.startMs }.thenBy { if (it.role == LineRole.INTERLUDE) 0 else 1 }
    )
}
