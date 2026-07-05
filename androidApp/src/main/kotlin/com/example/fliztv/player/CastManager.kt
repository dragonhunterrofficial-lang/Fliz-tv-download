package com.example.fliztv.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import org.json.JSONObject

object CastManager {
    private var castContext: CastContext? = null
    private var router: MediaRouter? = null
    private var routeSelector: MediaRouteSelector? = null
    private var contextRef: Context? = null

    var isCasting by mutableStateOf(false)
        private set
    var deviceName by mutableStateOf("")
        private set
    var currentUrl: String? = null
    var currentTitle: String? = null
    var currentUserAgent: String = ""
    var currentReferer: String = ""

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            isCasting = true
            deviceName = session.castDevice?.friendlyName ?: "Chromecast"
            currentUrl?.let { loadOntoCast(it) }
        }
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            isCasting = true
            deviceName = session.castDevice?.friendlyName ?: "Chromecast"
        }
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionEnded(session: CastSession, error: Int) {
            isCasting = false
            deviceName = ""
        }
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
    }

    fun init(context: Context) {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.addSessionManagerListener(sessionListener, CastSession::class.java)
            router = MediaRouter.getInstance(context)
            routeSelector = MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
                .build()
            contextRef = context
        } catch (_: Exception) {}
    }

    fun showRouteSelector() {
        val ctx = contextRef ?: return
        val sel = routeSelector ?: return
        try {
            val dialog = MediaRouteChooserDialog(ctx)
            dialog.routeSelector = sel
            dialog.show()
        } catch (_: Exception) {}
    }

    fun loadUrl(url: String, title: String = "", userAgent: String = "", referer: String = "") {
        currentUrl = url
        currentTitle = title
        currentUserAgent = userAgent
        currentReferer = referer
        if (isCasting) loadOntoCast(url)
    }

    private fun detectMimeType(url: String): String {
        return when {
            url.contains(".m3u8") || url.contains("m3u8") -> "application/x-mpegURL"
            url.contains(".mpd") || url.contains("mpd") -> "application/dash+xml"
            url.contains(".mp4") || url.contains("mp4") -> "video/mp4"
            url.contains(".ts") -> "video/MP2T"
            url.contains(".webm") -> "video/webm"
            url.contains(".ogg") || url.contains(".ogv") -> "video/ogg"
            url.contains(".avi") -> "video/x-msvideo"
            url.contains(".mkv") -> "video/x-matroska"
            url.contains(".flv") -> "video/x-flv"
            url.contains(".mov") -> "video/quicktime"
            url.contains(".wmv") -> "video/x-ms-wmv"
            url.contains(".3gp") -> "video/3gpp"
            else -> "video/mp4"
        }
    }

    private fun detectStreamType(url: String): Int {
        return when {
            url.contains(".m3u8") || url.contains("m3u8") || url.contains(".mpd") -> {
                MediaInfo.STREAM_TYPE_LIVE
            }
            else -> MediaInfo.STREAM_TYPE_BUFFERED
        }
    }

    private fun loadOntoCast(url: String) {
        try {
            val session = castContext?.sessionManager?.currentCastSession ?: return
            val client = session.remoteMediaClient ?: return

            val title = currentTitle
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
            if (!title.isNullOrEmpty()) metadata.putString(MediaMetadata.KEY_TITLE, title)

            val ua = currentUserAgent
            val ref = currentReferer
            val customData = JSONObject().apply {
                put("streamType", "live")
                if (ua.isNotEmpty()) put("userAgent", ua)
                if (ref.isNotEmpty()) put("referer", ref)
            }

            val mediaInfo = MediaInfo.Builder(url)
                .setStreamType(detectStreamType(url))
                .setContentType(detectMimeType(url))
                .setMetadata(metadata)
                .setCustomData(customData)
                .build()
            client.load(MediaLoadRequestData.Builder().setMediaInfo(mediaInfo).build())
        } catch (_: Exception) {}
    }

    fun disconnect() {
        try {
            castContext?.sessionManager?.endCurrentSession(true)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            castContext?.sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        } catch (_: Exception) {}
    }
}
