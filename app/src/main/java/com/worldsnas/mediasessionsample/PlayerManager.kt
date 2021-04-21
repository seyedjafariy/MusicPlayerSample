package com.worldsnas.mediasessionsample

import android.content.Context
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import kotlin.properties.Delegates

//to avoid binding the service we need to hold the player instance statically
object PlayerManager {
    private val playerListeners = mutableListOf<PlayerInstanceEventListener>()

    private var player by Delegates.observable<SimpleExoPlayer?>(null) { _, oldValue, newValue ->
        if (newValue == null) {
            playerListeners.forEach {
                it.playerDestroyed(oldValue!!)
            }
        } else {
            playerListeners.forEach {
                it.playerCreated(newValue)
            }
        }
    }

    fun isInitialized(): Boolean = player != null

    fun createPlayer(context: Context) {
        if (player != null) return

        synchronized(this) {
            if (player != null) return

            player = SimpleExoPlayer.Builder(context).build()

            player!!.setAudioAttributes(
                com.google.android.exoplayer2.audio.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_SPEECH)
                    .build(), true
            )

            player!!.addListener(ExoEventLogger())
        }
    }

    fun getInstance(): SimpleExoPlayer =
        player ?: error("player has not been initialized yet, call createPlayer first")

    fun releasePlayer() {
        val player = player ?: error("player already been released")

        player.stop()
        player.release()

        this.player = null
    }

    fun addListener(listener: PlayerInstanceEventListener) {
        val wasNotInitialized = !isInitialized()
        synchronized(this) {
            playerListeners.add(listener)
            if (wasNotInitialized && isInitialized()){
                listener.playerCreated(player!!)
            }
        }
    }

    fun removeListener(listener: PlayerInstanceEventListener){
        synchronized(this) {
            playerListeners.remove(listener)
        }
    }

    interface PlayerInstanceEventListener {
        fun playerCreated(player: SimpleExoPlayer) {}

        fun playerDestroyed(player: SimpleExoPlayer) {}
    }

    private class ExoEventLogger : Player.EventListener {

        private val TAG_PLAYER_EVENT = "PLAYER EVENT"

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
            Log.d(TAG_PLAYER_EVENT, "onTimelineChanged, timeLine= $timeline, reason= $reason")
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
            Log.d(TAG_PLAYER_EVENT, "onIsLoadingChanged, isLoading= $isLoading")
        }

        override fun onPlaybackStateChanged(state: Int) {
            super.onPlaybackStateChanged(state)
            Log.d(TAG_PLAYER_EVENT, "onPlaybackStateChanged, state= $state")
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            Log.d(
                TAG_PLAYER_EVENT,
                "onPlayWhenReadyChanged, playWhenReady= $playWhenReady, reason= $reason"
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            Log.d(TAG_PLAYER_EVENT, "onIsPlayingChanged, isPlaying= $isPlaying")
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            Log.d(TAG_PLAYER_EVENT, "onRepeatModeChanged, repeatMode= $repeatMode")
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            super.onPlayerError(error)
            Log.d(TAG_PLAYER_EVENT, "onPlayerError, error= $error")
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d(
                TAG_PLAYER_EVENT,
                "onMediaItemTransition, mediaItem= ${mediaItem?.mediaMetadata?.title}, reason= $reason"
            )
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
            Log.d(
                TAG_PLAYER_EVENT,
                "onTracksChanged, trackGroup= $trackGroups, trackSelection= $trackSelections"
            )
        }
    }
}