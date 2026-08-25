package com.autosleep.app.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState

class MediaSessionObserver(context: Context) {
    private val appContext = context.applicationContext
    private val mediaSessionManager =
        appContext.getSystemService(MediaSessionManager::class.java)

    private val listenerComponent = ComponentName(
        appContext,
        AutoSleepNotificationListenerService::class.java,
    )

    fun activeSessions(): Result<List<MediaSessionSnapshot>> = runCatching {
        mediaSessionManager
            .getActiveSessions(listenerComponent)
            .map { controller -> controller.toSnapshot() }
    }

    fun pausePlayingSessions(): Result<Int> = runCatching {
        val controllers = mediaSessionManager.getActiveSessions(listenerComponent)
        var paused = 0

        controllers.forEach { controller ->
            val state = controller.playbackState
            val isPlaying = state?.state == PlaybackState.STATE_PLAYING
            val supportsPause = state?.actions
                ?.and(PlaybackState.ACTION_PAUSE)
                ?.let { it != 0L }
                ?: false

            if (isPlaying && supportsPause) {
                controller.transportControls.pause()
                paused += 1
            }
        }

        paused
    }

    private fun MediaController.toSnapshot(): MediaSessionSnapshot {
        val state = playbackState
        val metadata = metadata
        val actions = state?.actions ?: 0L

        return MediaSessionSnapshot(
            packageName = packageName,
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            playbackState = state?.state,
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            canPause = actions and PlaybackState.ACTION_PAUSE != 0L,
        )
    }
}
