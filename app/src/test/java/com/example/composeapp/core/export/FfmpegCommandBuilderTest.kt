package com.example.composeapp.core.export

import com.example.composeapp.core.model.ClipEditState
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class FfmpegCommandBuilderTest {
    @Test
    fun buildsTrimFilterAndAudioCommandFromEditState() {
        val command = FfmpegCommandBuilder.videoCommand(
            input = File("/tmp/source video.mp4"),
            output = File("/tmp/output.mp4"),
            startMs = 1_500L,
            durationMs = 4_000L,
            edit = ClipEditState(
                brightness = 0.1f,
                contrast = 1.1f,
                saturation = 1.2f,
                rotation = 90,
                volume = 0.5f,
                fadeInMs = 250L,
            ),
        )

        assertTrue(command.contains("-ss 1.5"))
        assertTrue(command.contains("-t 4.0"))
        assertTrue(command.contains("eq=brightness=0.1"))
        assertTrue(command.contains("transpose=1"))
        assertTrue(command.contains("volume=0.5"))
        assertTrue(command.contains("afade=t=in"))
        assertTrue(command.contains("'/tmp/source video.mp4'"))
    }
}
