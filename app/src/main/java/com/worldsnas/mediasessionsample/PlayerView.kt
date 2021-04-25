package com.worldsnas.mediasessionsample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.FileDataSource
import com.worldsnas.mediasessionsample.AyaMediaItem.Companion.STARTING_AYA_ORDER_ID

class PlayerView(
    private val context: Service,
    private val player: SimpleExoPlayer,
    private val sessionToken: MediaSessionCompat.Token

) {
    private val notificationManager = NotificationManagerCompat.from(context)

    private val mediaSourceBag = ConcatenatingMediaSource()
    private val dataSourceFactory = FileDataSource.Factory()

    init {
        player.addListener(object : Player.EventListener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                showPlayingNotification()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                //if playlist is empty we don't want to update our view
                mediaItem ?: return

                //we are not interested in repeating
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) return


                val aya = mediaItem.playbackProperties!!.tag as AyaMediaItem
            }
        })

        player.addMediaSource(mediaSourceBag)

        player.prepare()
    }

    fun play() {
        player.play()
        showPlayingNotification()
    }

    fun pause() {
        player.pause()
        showPlayingNotification()
    }

    fun stop() {
        player.stop()
        showPlayingNotification()
    }

    fun next() {
        player.next()
        showPlayingNotification()
    }

    fun previous() {
        player.previous()
        showPlayingNotification()
    }

    fun playAya(orderId: Long) {
        repeat(mediaSourceBag.size) {
            val mediaItem =
                mediaSourceBag.getMediaSource(it).mediaItem.playbackProperties?.tag as AyaMediaItem
            if (mediaItem.ayaOrder == orderId) {
                player.seekToDefaultPosition(it)
                play()
                return
            }
        }

        error("orderId= $orderId, was not found in the currentPlayList= $mediaSourceBag")
    }

    fun loadAndPlay(ayaMediaItem: AyaMediaItem) {
        loadMediaSource(listOf(ayaMediaItem.createMediaSource()))

        play()
    }

    fun loadAndPlay(items: List<AyaMediaItem>, startingAya: Long = STARTING_AYA_ORDER_ID) {
        loadMediaSource(items.map { it.createMediaSource() })

        if (startingAya == STARTING_AYA_ORDER_ID) {
            play()
        } else {
            playAya(startingAya)
        }
    }

    private fun loadMediaSource(sources: List<MediaSource>) {
        mediaSourceBag.clear()
        mediaSourceBag.addMediaSources(sources)
        player.setMediaSource(mediaSourceBag)
        player.prepare()
    }

    fun isPaused(): Boolean =
        player.playbackState == Player.STATE_READY
                && !player.playWhenReady
                && player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE

    private fun showPlayingNotification() {
        createNotificationChannel()

        val contentIntent = createLauncherIntent()

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_PLAYER)

        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("سوره فاتحه آیه ۱")
            .setContentText("قاری: عبدل باسط")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0, 1, 2, 3)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0)

        if (player.isPlaying) {
            val pendingIntentPause = createActionPendingIntent(PlayerAction.Pause)
            notificationBuilder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_pause_black_24dp,
                    context.resources.getString(R.string.player_notification_pause),
                    pendingIntentPause
                ).build()
            )
        } else {
            val pendingIntentPlay = createActionPendingIntent(PlayerAction.Play)
            notificationBuilder.addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_play_black_24dp,
                    context.resources.getString(R.string.player_notification_play),
                    pendingIntentPlay
                ).build()
            )
        }

        val pendingIntentPrev = createActionPendingIntent(PlayerAction.PreviousAya)
        notificationBuilder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_rewind_black_24dp,
                context.resources.getString(R.string.player_notification_previous_aya),
                pendingIntentPrev
            ).build()
        )

        val pendingIntentNextAya = createActionPendingIntent(PlayerAction.NextAya)
        notificationBuilder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_fast_forward_black_24dp,
                context.resources.getString(R.string.player_notification_next_aya),
                pendingIntentNextAya
            ).build()
        )

        val pendingIntentStop = createActionPendingIntent(PlayerAction.Stop)
        notificationBuilder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_stop_circle_black_24dp,
                context.resources.getString(R.string.player_notification_stop),
                pendingIntentStop
            ).build()
        )

        notificationBuilder.show()
    }

    private fun createLauncherIntent(): PendingIntent {
        val launcherIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)!!.apply {
                //need to pass Aya/page reference here so we can open and scroll
                flags = FLAG_ACTIVITY_NEW_TASK
            }

        return PendingIntent.getActivity(
            context,
            PlayerAction.OpenApp.requestCode,
            launcherIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID_PLAYER,
                context.getString(R.string.player_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.setSound(null, null);
            channel.description =
                context.getString(R.string.player_notification_channel_description)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createActionPendingIntent(action: PlayerAction): PendingIntent =
        PendingIntent.getService(
            context,
            action.requestCode,
            createPlayerIntent(context, action.action),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun createPlayerIntent(context: Context, action: String) =
        Intent(context, PlayerService::class.java).apply {
            this.action = action
        }

    fun showDownloading(
        surahName: String,
        reciterName: String,
        progress: Int?
    ) {
        createNotificationChannel()

        val contentIntent = createLauncherIntent()

        NotificationCompat.Builder(context, CHANNEL_ID_PLAYER)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Downloading")
            .setContentText("Surah $surahName, Reciter $reciterName")
            .setProgress(100, progress ?: 0, progress == null)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0)
            .show()
    }

    fun showDownloadFailed(
        surahName: String,
        reciterName: String,
    ){
        createNotificationChannel()

        val contentIntent = createLauncherIntent()

        NotificationCompat.Builder(context, CHANNEL_ID_PLAYER)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Downloading Failed")
            .setContentText("Downloading Surah $surahName, By Reciter $reciterName, failed. please check your network connection and try again")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0)
            .build()
            .apply {
                notificationManager.notify(NOTIFICATION_ID_DOWNLOAD_FAILED, this)
            }
    }

    private fun NotificationCompat.Builder.show() {
        build().let {
            context.startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, it)
        }
    }

    private fun AyaMediaItem.createMediaSource(): MediaSource =
        ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(toExoMediaItem())
}

private fun AyaMediaItem.toExoMediaItem(): MediaItem =
    MediaItem.Builder()
        .setUri(ayaFile.toUri())
        .setTag(this)
        .build()

private const val CHANNEL_ID_PLAYER = "channel_id_player"
private const val NOTIFICATION_ID_FOREGROUND_SERVICE = 100001
private const val NOTIFICATION_ID_DOWNLOAD_FAILED = 100002
