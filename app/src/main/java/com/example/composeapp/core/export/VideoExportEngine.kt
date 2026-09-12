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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/** Real offline Android export path. Remuxes selected source media without re-encoding. */
class VideoExportEngine(private val context: Context) {
    suspend fun exportClip(
        source: Uri,
        startMs: Long,
        durationMs: Long,
        displayName: String = "MoreCut_${System.currentTimeMillis()}.mp4",
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val temp = File(context.cacheDir, "export_${System.nanoTime()}.mp4")
            try {
                remux(context.contentResolver, source, temp.absolutePath, startMs, durationMs)
                publish(temp, displayName)
            } finally {
                temp.delete()
            }
        }
    }

    private fun publish(temp: File, displayName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/MoreCut")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val output = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create output video")
        try {
            resolver.openOutputStream(output)?.use { target -> temp.inputStream().use { it.copyTo(target) } }
                ?: error("Unable to open output video")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(output, values, null, null)
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

/** Portable FFmpeg filter graph builder used by the native FFmpeg backend. */
object FfmpegCommandBuilder {
    fun videoFilter(brightness: Float, contrast: Float, saturation: Float, rotation: Int, crop: String? = null, blur: Float = 0f, sharpen: Float = 0f): String {
        val filters = mutableListOf<String>()
        if (crop != null) filters += "crop=$crop"
        if (brightness != 0f || contrast != 1f || saturation != 1f) filters += "eq=brightness=$brightness:contrast=$contrast:saturation=$saturation"
        if (blur > 0f) filters += "boxblur=${blur.coerceAtLeast(1f)}"
        if (sharpen > 0f) filters += "unsharp=5:5:${sharpen.coerceIn(0f,5f)}:5:5:0"
        when ((rotation % 360 + 360) % 360) { 90 -> filters += "transpose=1"; 180 -> filters += "hflip,vflip"; 270 -> filters += "transpose=2" }
        return filters.joinToString(",")
    }
}
