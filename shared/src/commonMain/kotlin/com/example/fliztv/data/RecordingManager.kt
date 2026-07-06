package com.example.fliztv.data

import com.example.fliztv.currentEpochSeconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Recording(
    val id: String,
    val channelName: String,
    val channelId: String,
    val streamUrl: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val filePath: String = "",
    val isDownloading: Boolean = false,
    val isComplete: Boolean = false,
    val fileSizeBytes: Long = 0L
) {
    val durationMs: Long get() = (endTime - startTime).coerceAtLeast(0L)

    val formattedDate: String get() {
        val totalSec = startTime / 1000
        val d = (totalSec / 86400).toInt()
        val h = (totalSec % 86400) / 3600
        val m = (totalSec % 3600) / 60
        return "${d}d ${h}h ${m}m ago"
    }

    val formattedSize: String get() {
        val kb = fileSizeBytes / 1024
        val mb = kb / 1024
        return if (mb > 0) "${mb}MB" else "${kb}KB"
    }

    val formattedDuration: String get() {
        val totalSec = durationMs / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

object RecordingManager {
    private val recordings = mutableListOf<Recording>()
    private val listeners = mutableListOf<() -> Unit>()
    private var idCounter = 0L
    private var onChanged: ((String) -> Unit)? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun getRecordings(): List<Recording> = synchronized(this) { recordings.toList().reversed() }

    fun isRecording(channelId: String): Boolean = synchronized(this) {
        recordings.any { it.channelId == channelId && it.isDownloading }
    }

    fun getActiveRecording(channelId: String): Recording? = synchronized(this) {
        recordings.find { it.channelId == channelId && it.isDownloading }
    }

    fun setPersistenceCallbacks(save: (String) -> Unit, load: () -> String) {
        synchronized(this) {
            onChanged = save
            val raw = load()
            if (raw.isNotBlank()) {
                try {
                    val loaded = json.decodeFromString<List<Recording>>(raw)
                    recordings.clear()
                    recordings.addAll(loaded)
                    idCounter = recordings.maxOfOrNull { it.id.substringAfterLast("_").toLongOrNull() ?: 0L } ?: 0L
                } catch (_: Exception) {}
            }
        }
    }

    private fun now() = currentEpochSeconds() * 1000

    private fun persist() {
        try {
            onChanged?.invoke(json.encodeToString(recordings.toList()))
        } catch (_: Exception) {}
    }

    fun startRecording(channelName: String, channelId: String, streamUrl: String): Recording = synchronized(this) {
        idCounter++
        val recording = Recording(
            id = "rec_${now()}_$idCounter",
            channelName = channelName,
            channelId = channelId,
            streamUrl = streamUrl,
            startTime = now(),
            isDownloading = true
        )
        recordings.add(recording)
        persist()
        notifyListeners()
        recording
    }

    fun completeRecording(id: String, filePath: String, fileSizeBytes: Long) = synchronized(this) {
        val idx = recordings.indexOfFirst { it.id == id }
        if (idx >= 0) {
            recordings[idx] = recordings[idx].copy(
                isDownloading = false,
                isComplete = true,
                endTime = now(),
                filePath = filePath,
                fileSizeBytes = fileSizeBytes
            )
            persist()
            notifyListeners()
        }
    }

    fun failRecording(id: String) = synchronized(this) {
        recordings.removeAll { it.id == id }
        persist()
        notifyListeners()
    }

    fun deleteRecording(id: String) = synchronized(this) {
        recordings.removeAll { it.id == id }
        persist()
        notifyListeners()
    }

    fun clearAll() = synchronized(this) {
        recordings.clear()
        persist()
        notifyListeners()
    }

    fun addListener(listener: () -> Unit) = synchronized(this) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) = synchronized(this) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val copy = synchronized(this) { listeners.toList() }
        copy.forEach { it() }
    }
}
