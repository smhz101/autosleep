package com.autosleep.app.media

data class MediaSessionSnapshot(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val playbackState: Int?,
    val isPlaying: Boolean,
    val canPause: Boolean,
)
