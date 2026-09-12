package com.example.composeapp.core.export

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.composeapp.core.model.ClipEditState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/** Offline export engine. FFmpeg is the primary renderer; remux is retained as a lossless fallback. */
class VideoExportEngine(private val context: Context) {
    suspend fun exportClip(
        source: Uri,
        startMs: Long,
        durationMs: Long,
        edit: ClipEditState = ClipEditState(),
        displayName: String = "MoreCut_${System.currentTimeMillis()}.mp4",
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val input = File(context.cacheDir, "ffmpeg_input_${System.nanoTime()}.mp4")
            val temp = File(context.cacheDir, "ffmpeg_output_${System.nanoTime()}.mp4")
            try {
                context.contentResolver.openInputStream(source)?.use { stream ->
                    input.outputStream().use { stream.copyTo(it) }
                } ?: error("Unable to open source video")

                val command = FfmpegCommandBuilder.videoCommand(
                    input = input,
                    output = temp,
                    startMs = startMs,
                    durationMs = durationMs,
                    edit = edit,
                )
                val session = FFmpegKit.execute(command)
                if (!ReturnCode.isSuccess(session.returnCode) || !temp.exists() || temp.length() == 0L) {
                    remux(context.contentResolver, source, temp.absolutePath, startMs, durationMs)
                }
                publish(temp, displayName)
            } finally {
                input.delete()
                temp.delete()
            }
        }
    }

    private fun publish(temp: File, displayName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/MoreCut")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val output = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create output video")
        try {
            resolver.openOutputStream(output)?.use { target -> temp.inputStream().use { it.copyTo(target) } }
                ?: error("Unable to open output video")
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val published = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                resolver.update(output, published, null, null)
            }
            return output
        } catch (t: Throwable) {
            resolver.delete(output, null, null)
            throw t
        }
    }

    private fun remux(resolver: ContentResolver, source: Uri, outputPath: String, startMs: Long, durationMs: Long) {
        val extractor = MediaExtractor()
        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            resolver.openFileDescriptor(source, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
                val trackMap = IntArray(extractor.trackCount) { -1 }
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) trackMap[i] = muxer.addTrack(format)
                }
                muxer.start()
                started = true
                val startUs = startMs.coerceAtLeast(0L) * 1000L
                val endUs = startUs + durationMs.coerceAtLeast(1L) * 1000L
                val buffer = ByteBuffer.allocateDirect(4 * 1024 * 1024)
                val info = MediaCodec.BufferInfo()
                for (i in 0 until extractor.trackCount) {
                    if (trackMap[i] < 0) continue
                    extractor.selectTrack(i)
                    extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    while (true) {
                        val timeUs = extractor.sampleTime
                        if (timeUs < 0L || timeUs > endUs) break
                        buffer.clear()
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) break
                        info.offset = 0
                        info.size = size
                        info.presentationTimeUs = (timeUs - startUs).coerceAtLeast(0L)
                        info.flags = extractor.sampleFlags
                        muxer.writeSampleData(trackMap[i], buffer, info)
                        if (!extractor.advance()) break
                    }
                    extractor.unselectTrack(i)
                }
            } ?: error("Unable to open source video")
        } finally {
            if (started) runCatching { muxer.stop() }
            muxer.release()
            extractor.release()
        }
    }
}

object FfmpegCommandBuilder {
    fun videoCommand(input: File, output: File, startMs: Long, durationMs: Long, edit: ClipEditState): String {
        val filters = mutableListOf<String>()
        if (edit.cropRight < 1f || edit.cropBottom < 1f || edit.cropLeft > 0f || edit.cropTop > 0f) {
            filters += "crop=iw*${edit.cropRight - edit.cropLeft}:ih*${edit.cropBottom - edit.cropTop}:iw*${edit.cropLeft}:ih*${edit.cropTop}"
        }
        if (edit.brightness != 0f || edit.contrast != 1f || edit.saturation != 1f || edit.exposure != 0f) {
            filters += "eq=brightness=${edit.brightness}:contrast=${edit.contrast}:saturation=${edit.saturation}:gamma=${1f + edit.exposure}"
        }
        if (edit.blur > 0f) filters += "boxblur=${edit.blur.coerceAtLeast(1f)}"
        if (edit.sharpen > 0f) filters += "unsharp=5:5:${edit.sharpen.coerceIn(0f, 5f)}:5:5:0"
        when ((edit.rotation % 360 + 360) % 360) {
            90 -> filters += "transpose=1"
            180 -> filters += "hflip,vflip"
            270 -> filters += "transpose=2"
        }
        if (edit.flipHorizontal) filters += "hflip"
        if (edit.flipVertical) filters += "vflip"
        when (edit.filter) {
            "Cinematic" -> filters += "eq=contrast=1.12:saturation=1.18"
            "Mono" -> filters += "hue=s=0"
            "Vintage" -> filters += "colorbalance=rs=.1:gs=.03:bs=-.03"
        }
        if (edit.effect == "Glow") filters += "unsharp=3:3:-1:3:3:0"

        val audioFilters = mutableListOf<String>()
        if (edit.volume != 1f) audioFilters += "volume=${edit.volume.coerceIn(0f, 2f)}"
        if (edit.fadeInMs > 0L) audioFilters += "afade=t=in:st=0:d=${edit.fadeInMs / 1000f}"
        if (edit.fadeOutMs > 0L) audioFilters += "afade=t=out:st=${(durationMs - edit.fadeOutMs).coerceAtLeast(0L) / 1000f}:d=${edit.fadeOutMs / 1000f}"
        if (edit.reverb > 0f) audioFilters += "aecho=0.8:0.88:60:0.${(edit.reverb * 100).toInt().coerceIn(10, 80)}"

        val vf = filters.joinToString(",")
        val af = audioFilters.joinToString(",")
        val inputPath = shellQuote(input.absolutePath)
        val outputPath = shellQuote(output.absolutePath)
        return buildString {
            append("-y -ss ${startMs.coerceAtLeast(0L) / 1000f} -i $inputPath ")
            append("-t ${durationMs.coerceAtLeast(1L) / 1000f} ")
            append("-map 0:v:0? -map 0:a:0? -c:v libopenh264 -b:v 8M -preset fast ")
            append("-c:a aac -b:a 192k -movflags +faststart ")
            if (vf.isNotBlank()) append("-vf ${shellQuote(vf)} ")
            if (af.isNotBlank()) append("-af ${shellQuote(af)} ")
            append(outputPath)
        }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
