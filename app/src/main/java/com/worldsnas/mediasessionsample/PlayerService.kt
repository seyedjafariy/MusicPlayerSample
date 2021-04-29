package com.worldsnas.mediasessionsample

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media2.session.MediaSession
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.AssetDataSource
import com.google.android.exoplayer2.upstream.DataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import java.io.File

//TODO launching the service with a play action for the first time is very slow.
// it seems the slowness is happening on the MainThread and causing a lot of frames to be skipped
class PlayerService : MediaBrowserServiceCompat(), CoroutineScope by MainScope() {

    lateinit var notificationManager: NotificationManager
    lateinit var mediaSession: MediaSession
    lateinit var player: SimpleExoPlayer
    lateinit var playerView: PlayerView
    lateinit var playlistManager: PlaylistManager

    var currentPlayList: AyaPlayList? = null

    val RECENT_ROOT_ID = "recent_root_id"
    val MEDIA_ROOT_ID = "media_root_id"

    private val packageValidator by lazy {
        PackageValidator(
            this,
            R.xml.allowed_media_browser_callers
        )
    }

    override fun onBind(intent: Intent?): IBinder? {
        val browserBinder = super.onBind(intent)
        if (browserBinder != null) return browserBinder
        return null //I can create my own binder and return
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        PlayerManager.createPlayer(this)

        player = PlayerManager.getInstance()

        val sessionPlayer = SessionPlayerConnector(player)

        mediaSession = MediaSession.Builder(this, sessionPlayer).build()

        playerView = PlayerView(this, player, mediaSession.sessionCompatToken)

        val sharedPreferences = getSharedPreferences("music-player", Context.MODE_PRIVATE)
        val client = OkHttpClient.Builder().build()
        val downloadDirectory = File(getExternalFilesDir(null), "recites")
        playlistManager =
            PlaylistManager(this, downloadDirectory, playerView, client, sharedPreferences){
                stopForeground(true)
                stopSelf()
            }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        //if the command received is for an aya that we have not downloaded yet
        // we have to stop player and launch downloader service

        intent?.action?.let {
            handleAction(intent, PlayerAction(it))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
        PlayerManager.releasePlayer()
        mediaSession.close()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {

        if (packageValidator.isKnownCaller(clientPackageName, clientUid)) {
            rootHints?.let {
                if (it.getBoolean(BrowserRoot.EXTRA_RECENT)) {
                    // Return a tree with a single playable media item for resumption.
                    val extras = Bundle().apply {
                        putBoolean(BrowserRoot.EXTRA_RECENT, true)
                    }
                    return BrowserRoot(RECENT_ROOT_ID, extras)
                }
            }
            // You can return your normal tree if the EXTRA_RECENT flag is not present.
            return BrowserRoot(MEDIA_ROOT_ID, null)
        }

        return null
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        MediaBrowserCompat.MediaItem(
            MediaDescriptionCompat.Builder()
                .setTitle("سوره: فاتحه قاری: عبدل باسط")
                .setDescription("آیه ۱")
                .setMediaId("00100۰.mp3")

                .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )
        result.sendResult(mutableListOf())
    }

    private fun handleAction(intent: Intent, action: PlayerAction) {
        when (action) {
            PlayerAction.Play -> {
                handlePlay(intent)
            }
            PlayerAction.Pause -> {
                playerView.pause()
            }
            PlayerAction.Stop -> {
                playerView.stop()
            }
            PlayerAction.NextAya -> {
                playerView.next()
            }
            PlayerAction.PreviousAya -> {
                playerView.previous()
            }
        }
    }

    private fun handlePlay(intent: Intent) {
        val newPlayList =
            intent.getParcelableExtra<AyaPlayList>(AyaPlayList.EXTRA_KEY_AYA_MEDIA_ITEM)

        if (newPlayList == null) {
            if (playerView.isPaused()) {
                playerView.play()
            } else {
                playlistManager.loadAndPlay(playlistManager.getLastPlayList())
            }

            //check if stopped and service is not working,
            // if yes we should load last playlist and start playing it
        } else {
            val currentItem = currentPlayList
            if (currentItem == null) {
                currentPlayList = newPlayList
                playlistManager.loadAndPlay(newPlayList)
            } else {
                if (currentItem.isSameListAndReciter(newPlayList)) {
                    //same range, just check for current playing aya
                    val currentMediaItem =
                        player.currentMediaItem?.playbackProperties?.tag as? AyaMediaItem
                    if (currentMediaItem == null) {
                        playlistManager.loadAndPlay(newPlayList)
                    } else if (currentMediaItem.ayaOrder == newPlayList.order.orderId) {
                        playerView.play()
                    } else {
                        playerView.playAya(newPlayList.order.orderId)
                    }
                } else {
                    currentPlayList = newPlayList
                    playlistManager.loadAndPlay(newPlayList)
                }
            }
        }
    }
}
