package com.autosleep.app.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Read-only snapshot of one media session that Android exposes to AutoSleep.
 */
data class MediaSessionSnapshot(
    val id: String,
    val packageName: String,
    val playbackState: String,
    val title: String?,
    val artist: String?,
    val durationMs: Long?,
    val pauseSupported: Boolean,
    val actions: Long,
)

/**
 * Current state of the compatibility-oriented media probe.
 */
data class MediaMonitorState(
    val accessGranted: Boolean = false,
    val sessions: List<MediaSessionSnapshot> = emptyList(),
    val logs: List<String> = emptyList(),
)

/**
 * Observes active Android media sessions and experimentally requests pause only when
 * a session explicitly advertises ACTION_PAUSE support.
 *
 * This class deliberately treats session visibility and control as app-dependent.
 * Unsupported and security-restricted cases are emitted to the debug log instead of
 * being treated as universal failures or universal capabilities.
 */
class MediaSessionMonitor(
    context: Context,
    private val onStateChanged: (MediaMonitorState) -> Unit,
) {
    private val appContext = context.applicationContext
    private val mediaSessionManager = appContext.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(
        appContext,
        AutoSleepNotificationListenerService::class.java,
    )
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private var activeControllers: List<MediaController> = emptyList()
    private val controllerCallbacks = mutableMapOf<MediaSession.Token, MediaController.Callback>()
    private var listening = false
    private var currentState = MediaMonitorState()

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        mainHandler.post {
            replaceControllers(
                controllers = controllers.orEmpty(),
                reason = "Active session list changed",
            )
        }
    }

    /** Start observing if the user has enabled Notification Access for AutoSleep. */
    fun start() {
        if (!hasAccess()) {
            stopListening()
            publish(
                accessGranted = false,
                sessions = emptyList(),
                logMessage = "Media access is not enabled. Grant Notification Access to run the compatibility probe.",
            )
            return
        }

        if (!listening) {
            try {
                mediaSessionManager.addOnActiveSessionsChangedListener(
                    activeSessionsListener,
                    listenerComponent,
                    mainHandler,
                )
                listening = true
            } catch (securityException: SecurityException) {
                publish(
                    accessGranted = true,
                    sessions = emptyList(),
                    logMessage = "Android denied the active-session listener: ${securityException.message ?: "security restriction"}",
                )
                return
            }
        }

        refresh("Media monitor started")
    }

    /** Stop callbacks while the Activity is not visible. */
    fun stop() {
        stopListening()
    }

    /** Re-read the active sessions exposed by Android. */
    fun refresh(reason: String = "Manual refresh") {
        if (!hasAccess()) {
            stopListening()
            publish(
                accessGranted = false,
                sessions = emptyList(),
                logMessage = "Cannot refresh: Notification Access is disabled.",
            )
            return
        }

        try {
            replaceControllers(
                controllers = mediaSessionManager.getActiveSessions(listenerComponent),
                reason = reason,
            )
        } catch (securityException: SecurityException) {
            publish(
                accessGranted = true,
                sessions = emptyList(),
                logMessage = "Android rejected active-session access: ${securityException.message ?: "security restriction"}",
            )
        } catch (exception: RuntimeException) {
            publish(
                accessGranted = true,
                sessions = emptyList(),
                logMessage = "Active-session query failed: ${exception.message ?: exception.javaClass.simpleName}",
            )
        }
    }

    /**
     * Request pause for a visible session only when that session advertises ACTION_PAUSE.
     * A successful method call is logged as a request, not as proof that the target app paused.
     */
    fun requestPause(sessionId: String) {
        val controller = activeControllers.firstOrNull { sessionId(it) == sessionId }
        if (controller == null) {
            publish(logMessage = "Pause skipped: session is no longer active.")
            return
        }

        val actions = controller.playbackState?.actions ?: 0L
        val pauseSupported = actions and PlaybackState.ACTION_PAUSE != 0L
        if (!pauseSupported) {
            publish(
                logMessage = "${controller.packageName}: pause unsupported by the advertised media-session actions.",
            )
            return
        }

        try {
            controller.transportControls.pause()
            publish(
                logMessage = "${controller.packageName}: pause requested; waiting for playback-state confirmation.",
            )
        } catch (exception: RuntimeException) {
            publish(
                logMessage = "${controller.packageName}: pause request failed: ${exception.message ?: exception.javaClass.simpleName}",
            )
        }
    }

    /** True when AutoSleep's notification-listener component is enabled in Android settings. */
    fun hasAccess(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            appContext.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS,
        ) ?: return false

        return enabledListeners
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == listenerComponent }
    }

    private fun replaceControllers(
        controllers: List<MediaController>,
        reason: String,
    ) {
        unregisterControllerCallbacks()
        activeControllers = controllers

        activeControllers.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    publishCurrent("${controller.packageName}: playback state changed to ${playbackStateLabel(state?.state)}")
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    publishCurrent("${controller.packageName}: media metadata changed")
                }

                override fun onSessionDestroyed() {
                    mainHandler.post {
                        refresh("${controller.packageName}: session destroyed")
                    }
                }
            }

            try {
                controller.registerCallback(callback, mainHandler)
                controllerCallbacks[controller.sessionToken] = callback
            } catch (exception: RuntimeException) {
                publish(
                    logMessage = "${controller.packageName}: callback registration failed: ${exception.message ?: exception.javaClass.simpleName}",
                )
            }
        }

        publish(
            accessGranted = true,
            sessions = snapshots(),
            logMessage = "$reason: ${activeControllers.size} active session(s) visible.",
        )
    }

    private fun publishCurrent(logMessage: String) {
        publish(
            accessGranted = hasAccess(),
            sessions = snapshots(),
            logMessage = logMessage,
        )
    }

    private fun snapshots(): List<MediaSessionSnapshot> = activeControllers.map { controller ->
        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val actions = playbackState?.actions ?: 0L

        MediaSessionSnapshot(
            id = sessionId(controller),
            packageName = controller.packageName,
            playbackState = playbackStateLabel(playbackState?.state),
            title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString(),
            artist = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString(),
            durationMs = metadata
                ?.getLong(MediaMetadata.METADATA_KEY_DURATION)
                ?.takeIf { it > 0L },
            pauseSupported = actions and PlaybackState.ACTION_PAUSE != 0L,
            actions = actions,
        )
    }

    private fun stopListening() {
        if (listening) {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsListener)
            listening = false
        }
        unregisterControllerCallbacks()
        activeControllers = emptyList()
    }

    private fun unregisterControllerCallbacks() {
        activeControllers.forEach { controller ->
            val callback = controllerCallbacks.remove(controller.sessionToken) ?: return@forEach
            try {
                controller.unregisterCallback(callback)
            } catch (_: RuntimeException) {
                // Session may already be destroyed; there is nothing left to unregister.
            }
        }
        controllerCallbacks.clear()
    }

    private fun publish(
        accessGranted: Boolean = currentState.accessGranted,
        sessions: List<MediaSessionSnapshot> = currentState.sessions,
        logMessage: String? = null,
    ) {
        val logs = if (logMessage == null) {
            currentState.logs
        } else {
            (currentState.logs + "${LocalTime.now().format(timeFormatter)}  $logMessage")
                .takeLast(MAX_LOG_LINES)
        }

        currentState = MediaMonitorState(
            accessGranted = accessGranted,
            sessions = sessions,
            logs = logs,
        )
        onStateChanged(currentState)
    }

    private fun sessionId(controller: MediaController): String =
        "${controller.packageName}:${Integer.toHexString(controller.sessionToken.hashCode())}"

    private fun playbackStateLabel(state: Int?): String = when (state) {
        PlaybackState.STATE_NONE -> "NONE"
        PlaybackState.STATE_STOPPED -> "STOPPED"
        PlaybackState.STATE_PAUSED -> "PAUSED"
        PlaybackState.STATE_PLAYING -> "PLAYING"
        PlaybackState.STATE_FAST_FORWARDING -> "FAST_FORWARDING"
        PlaybackState.STATE_REWINDING -> "REWINDING"
        PlaybackState.STATE_BUFFERING -> "BUFFERING"
        PlaybackState.STATE_ERROR -> "ERROR"
        PlaybackState.STATE_CONNECTING -> "CONNECTING"
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> "SKIPPING_TO_PREVIOUS"
        PlaybackState.STATE_SKIPPING_TO_NEXT -> "SKIPPING_TO_NEXT"
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> "SKIPPING_TO_QUEUE_ITEM"
        else -> "UNKNOWN"
    }

    private companion object {
        const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
        const val MAX_LOG_LINES = 40
    }
}
