package com.tabplayer

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class AudioPlayer(context: Context) {
    private val player = ExoPlayer.Builder(context).build()

    fun load(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }
    fun play() { player.play() }
    fun pause() { player.pause() }
    fun seekTo(ms: Long) { player.seekTo(ms) }
    fun currentPosition(): Long = player.currentPosition
    fun duration(): Long = player.duration
    fun isPlaying(): Boolean = player.isPlaying
    fun release() { player.release() }
}
