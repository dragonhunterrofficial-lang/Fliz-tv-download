package com.example.fliztv.player

import android.content.Context
import com.example.fliztv.data.RecordingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

object RecordingDownloader {
    private const val DEFAULT_UA = "Mozilla/5.0 (Android 14) AppleWebKit/537.36"
    private const val MAX_RECORDING_DURATION_MS = 4 * 60 * 60 * 1000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableMapOf<String, Job>()
    private val activeCalls = mutableMapOf<String, Call>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .connectionPool(okhttp3.ConnectionPool(4, 30, TimeUnit.SECONDS))
        .build()

    var onError: ((String) -> Unit)? = null

    fun start(context: Context, channelName: String, channelId: String, streamUrl: String, userAgent: String = "", referer: String = "") {
        val recording = RecordingManager.startRecording(channelName, channelId, streamUrl)
        val dir = (context.getExternalFilesDir("fliztv-recordings") ?: File(context.filesDir, "fliztv-recordings")).also { it.mkdirs() }
        val file = File(dir, "${recording.id}.ts")
        val ua = userAgent.ifEmpty { DEFAULT_UA }

        val job = scope.launch {
            try {
                downloadStream(recording.id, streamUrl, file, ua, referer)

                synchronized(activeCalls) { activeCalls.remove(recording.id) }
                synchronized(activeJobs) { activeJobs.remove(recording.id) }
                val bytes = file.length()
                if (bytes > 0) {
                    RecordingManager.completeRecording(recording.id, file.absolutePath, bytes)
                    android.util.Log.i("Recording", "Recording complete: $bytes bytes to ${file.absolutePath}")
                } else {
                    if (file.exists()) file.delete()
                    RecordingManager.failRecording(recording.id)
                    onError?.invoke("Recording produced empty file")
                }
            } catch (e: CancellationException) {
                synchronized(activeCalls) { activeCalls.remove(recording.id) }
                synchronized(activeJobs) { activeJobs.remove(recording.id) }
                if (file.exists() && file.length() > 0) {
                    RecordingManager.completeRecording(recording.id, file.absolutePath, file.length())
                    android.util.Log.i("Recording", "Recording stopped: ${file.length()} bytes")
                } else {
                    if (file.exists()) file.delete()
                    RecordingManager.failRecording(recording.id)
                }
            } catch (e: Exception) {
                synchronized(activeCalls) { activeCalls.remove(recording.id) }
                synchronized(activeJobs) { activeJobs.remove(recording.id) }
                val bytes = file.length()
                if (bytes > 0) {
                    RecordingManager.completeRecording(recording.id, file.absolutePath, bytes)
                    android.util.Log.i("Recording", "Recording stopped with $bytes bytes")
                } else {
                    if (file.exists()) file.delete()
                    RecordingManager.failRecording(recording.id)
                    val msg = e.message ?: "Unknown error"
                    android.util.Log.e("Recording", "Recording failed: $msg")
                    onError?.invoke(msg)
                }
            }
        }
        synchronized(activeJobs) { activeJobs[recording.id] = job }
    }

    private suspend fun downloadStream(recordingId: String, url: String, file: File, ua: String, referer: String) {
        val request = Request.Builder().url(url)
            .header("User-Agent", ua)
            .apply { if (referer.isNotEmpty()) header("Referer", referer) }
            .build()
        val call = client.newCall(request)
        synchronized(activeCalls) { activeCalls[recordingId] = call }

        call.execute().use { response ->
            if (!response.isSuccessful) throw Exception("Server returned ${response.code}")
            val body = response.body ?: throw Exception("No response from server")

            val source = body.source()
            val peekBuf = Buffer()
            source.read(peekBuf, 2048)
            val header = peekBuf.readUtf8()

            val isHls = header.contains("#EXTM3U", ignoreCase = true)

            if (isHls) {
                val restBuf = Buffer()
                source.read(restBuf, Long.MAX_VALUE)
                val playlistText = header + restBuf.readUtf8()

                val actualPlaylistUrl = resolveMediaPlaylist(playlistText, url)
                val actualPlaylist = if (actualPlaylistUrl != url) {
                    fetchUrl(recordingId, actualPlaylistUrl, ua, referer)
                } else {
                    playlistText
                }

                val baseUrl = actualPlaylistUrl.substringBeforeLast("/")
                downloadHlsSegments(recordingId, actualPlaylist, actualPlaylistUrl, baseUrl, file, ua, referer)
                return
            }

            val sink = file.sink().buffer()
            try {
                sink.writeUtf8(header)
                val buf = Buffer()
                while (coroutineContext.isActive) {
                    val read = source.read(buf, 8192)
                    if (read == -1L) break
                    sink.write(buf, read)
                }
            } finally {
                sink.close()
            }
        }
    }

    private fun resolveMediaPlaylist(playlistText: String, originalUrl: String): String {
        val lines = playlistText.lines()
        for (i in lines.indices) {
            if (lines[i].trim().startsWith("#EXT-X-STREAM-INF:")) {
                if (i + 1 < lines.size) {
                    val uri = lines[i + 1].trim()
                    if (uri.isNotEmpty() && !uri.startsWith("#")) {
                        return resolveUrl(uri, originalUrl.substringBeforeLast("/"))
                    }
                }
            }
        }
        return originalUrl
    }

    private suspend fun downloadHlsSegments(recordingId: String, initialPlaylist: String, playlistUrl: String, baseUrl: String, file: File, ua: String, referer: String) {
        val seenSegments = mutableSetOf<String>()
        var playlistText = initialPlaylist
        val startMs = System.currentTimeMillis()

        while (coroutineContext.isActive) {
            if (System.currentTimeMillis() - startMs > MAX_RECORDING_DURATION_MS) break
            val segments = parseHlsSegments(playlistText, baseUrl)

            for (segUrl in segments) {
                if (!coroutineContext.isActive) break
                if (segUrl in seenSegments) continue
                seenSegments.add(segUrl)
                downloadSegment(recordingId, segUrl, file, ua, referer)
            }

            val targetDuration = parseTargetDuration(playlistText).coerceIn(1, 30)
            kotlinx.coroutines.delay(targetDuration * 1000L)
            playlistText = fetchUrl(recordingId, playlistUrl, ua, referer)
        }
    }

    private fun fetchUrl(recordingId: String, url: String, ua: String, referer: String): String {
        val request = Request.Builder().url(url)
            .header("User-Agent", ua)
            .apply { if (referer.isNotEmpty()) header("Referer", referer) }
            .build()
        val call = client.newCall(request)
        synchronized(activeCalls) { activeCalls[recordingId] = call }
        call.execute().use { response ->
            if (!response.isSuccessful) throw Exception("Fetch failed: ${response.code}")
            return response.body?.string() ?: throw Exception("Empty response")
        }
    }

    private fun parseHlsSegments(playlistText: String, baseUrl: String): List<String> {
        val segments = mutableListOf<String>()
        val lines = playlistText.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:") || line.startsWith("#EXT-X-BYTERANGE:")) {
                i++
                if (i < lines.size) {
                    val segLine = lines[i].trim()
                    if (segLine.isNotEmpty() && !segLine.startsWith("#")) {
                        segments.add(resolveUrl(segLine, baseUrl))
                    }
                }
            } else if (line.isNotEmpty() && !line.startsWith("#") && !line.startsWith("<") && !line.startsWith("{")) {
                segments.add(resolveUrl(line, baseUrl))
            }
            i++
        }
        return segments
    }

    private fun parseTargetDuration(playlistText: String): Int {
        for (line in playlistText.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXT-X-TARGETDURATION:")) {
                return trimmed.substringAfter(":").trim().toIntOrNull() ?: 10
            }
        }
        return 10
    }

    private fun resolveUrl(segment: String, baseUrl: String): String {
        if (segment.startsWith("http://") || segment.startsWith("https://")) return segment
        return if (segment.startsWith("/")) {
            val host = URL(baseUrl).let { "${it.protocol}://${it.host}${if (it.port > 0) ":${it.port}" else ""}" }
            "$host$segment"
        } else {
            "$baseUrl/$segment"
        }
    }

    private fun downloadSegment(recordingId: String, segUrl: String, outputFile: File, ua: String, referer: String) {
        val request = Request.Builder().url(segUrl)
            .header("User-Agent", ua)
            .apply { if (referer.isNotEmpty()) header("Referer", referer) }
            .build()
        val call = client.newCall(request)
        synchronized(activeCalls) { activeCalls[recordingId] = call }

        call.execute().use { response ->
            if (!response.isSuccessful) throw Exception("Segment fetch failed: ${response.code}")
            val body = response.body ?: throw Exception("No segment data")

            val sink = outputFile.sink(true)
            try {
                val source = body.source()
                val buf = Buffer()
                while (true) {
                    val read = source.read(buf, 8192)
                    if (read == -1L) break
                    sink.write(buf, read)
                }
            } finally {
                sink.close()
            }
        }
    }

    fun stop(channelId: String) {
        val rec = RecordingManager.getActiveRecording(channelId) ?: return
        synchronized(activeJobs) { activeJobs[rec.id]?.cancel() }
        synchronized(activeCalls) { activeCalls[rec.id]?.cancel() }
    }

    fun cleanup() {
        synchronized(activeJobs) {
            activeJobs.values.forEach { it.cancel() }
            activeJobs.clear()
        }
        synchronized(activeCalls) {
            activeCalls.values.forEach { it.cancel() }
            activeCalls.clear()
        }
        scope.cancel()
    }
}
