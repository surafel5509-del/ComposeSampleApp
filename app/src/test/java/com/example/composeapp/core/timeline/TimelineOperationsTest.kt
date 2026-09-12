package com.example.composeapp.core.timeline

import com.example.composeapp.core.model.Clip
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineOperationsTest {
    @Test
    fun splitClip_createsTwoContiguousSegments() {
        val clip = Clip("clip", "asset", startMs = 1_000, durationMs = 8_000, sourceStartMs = 500)
        val (first, second) = TimelineOperations.splitClip(clip, positionMs = 4_000)

        assertEquals(3_000, first.durationMs)
        assertEquals(4_000, second.startMs)
        assertEquals(5_000, second.durationMs)
        assertEquals(3_500, second.sourceStartMs)
    }
}
