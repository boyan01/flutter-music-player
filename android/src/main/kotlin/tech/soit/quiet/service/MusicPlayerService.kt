package tech.soit.quiet.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tech.soit.quiet.receiver.BecomingNoisyReceiver
import tech.soit.quiet.service.NotificationBuilder.Companion.NOW_PLAYING_NOTIFICATION
import tech.soit.quiet.utils.LoggerLevel
import tech.soit.quiet.utils.log
import tech.soit.quiet.utils.toMediaSource


/**
 * music player service of Application
 */
class MusicPlayerService : MediaBrowserServiceCompat() {

    companion object {

        //TODO set from method channel
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" +
                    " (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/13.10586"


        private val audioAttribute = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        private const val ROOT = "/"

    }

    private val mediaSession by lazy {
        val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
        if (sessionIntent == null) {
            log(level = LoggerLevel.WARN) { "application do not have launcher intent ??" }
        }
        return@lazy MediaSessionCompat(this, "MusicService").apply {
            sessionIntent?.let {
                setSessionActivity(PendingIntent.getActivity(this@MusicPlayerService, 0, it, 0))
            }
            isActive = true
        }
    }

    private val notificationBuilder by lazy { NotificationBuilder(this) }

    private val becomingNoisyReceiver by lazy {
        BecomingNoisyReceiver(
            this,
            mediaSession.sessionToken
        )
    }

    private val notificationManager by lazy { NotificationManagerCompat.from(this) }

    private lateinit var mediaController: MediaControllerCompat

    private var isForegroundService = false

    // Wrap a SimpleExoPlayer with a decorator to handle audio focus for us.
    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this).apply {
            setAudioAttributes(audioAttribute, true)
        }
    }


    private fun handleNotification() = GlobalScope.launch(Dispatchers.Main) {

        for (notification in notificationBuilder.notificationGenerator) {

            val playbackState = mediaController.playbackState ?: return@launch
            when (val updatedState = playbackState.state) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {

                    /**
                     * This may look strange, but the documentation for [Service.startForeground]
                     * notes that "calling this method does *not* put the service in the started
                     * state itself, even though the name sounds like it."
                     */
                    if (!isForegroundService) {
                        startService(Intent(applicationContext, this@MusicPlayerService.javaClass))
                        startForeground(NOW_PLAYING_NOTIFICATION, notification)
                        isForegroundService = true
                    } else if (notification != null) {
                        notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                    }
                }
                else -> {
                    if (isForegroundService) {
                        stopForeground(false)
                        isForegroundService = false

                        // If playback has ended, also stop the service.
                        if (updatedState == PlaybackStateCompat.STATE_NONE) {
                            stopSelf()
                        }

                        if (notification != null) {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        } else {
                            stopForeground(true)
                        }
                    }
                }
            }
        }

    }

    override fun onCreate() {
        super.onCreate()
        sessionToken = mediaSession.sessionToken
        mediaController = MediaControllerCompat(this, mediaSession).apply {
            registerCallback(MediaControllerCallback())
        }

        val dataSourceFactory = DefaultDataSourceFactory(
            this, Util.getUserAgent(this, ""), null
        )
        MediaSessionConnector(mediaSession, object : DefaultPlaybackController() {
            override fun getSupportedPlaybackActions(player: Player?): Long {
                return ACTIONS
            }

        }).also { it ->
            it.setPlayer(exoPlayer, object : MediaSessionConnector.PlaybackPreparer {
                override fun onPrepareFromSearch(query: String?, extras: Bundle?) {

                }

                override fun onCommand(
                    player: Player?,
                    command: String?,
                    extras: Bundle?,
                    cb: ResultReceiver?
                ) {
                }

                override fun getCommands(): Array<String>? {
                    return null
                }

                override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
                }

                override fun onPrepare() {
                }


                override fun getSupportedPrepareActions(): Long {
                    return MediaSessionConnector.PlaybackPreparer.ACTIONS
                }

                override fun onPrepareFromMediaId(mediaId: String, extras: Bundle?) {

                    val playlist = extras?.getParcelableArrayList<MediaMetadataCompat>("queue")
                    val title = extras?.getString("queueTitle")

                    val queue = mutableListOf<MediaDescriptionCompat>()
                    if (playlist.isNullOrEmpty()) {
                        // We got a empty playlist from extra, try to load previous queue.
                        queue.addAll(mediaController.queue.orEmpty().map { it.description })
                    } else {
                        queue.addAll(playlist.map { it.description })
                    }

                    if (queue.isEmpty()) {
                        log(LoggerLevel.ERROR) { "Skip play action: try to play $mediaId, but we can not find queue to obtain MediaDescription." }
                        return
                    }

                    val windowIndex = queue.indexOfFirst { it.mediaId == mediaId }
                    if (windowIndex == -1) {
                        log(LoggerLevel.ERROR) { "Skip play action : try to play $mediaId, but this media seems not in queue." }
                        return
                    }

                    val source = queue.toMediaSource(dataSourceFactory)
                    exoPlayer.prepare(source)
                    exoPlayer.seekTo(windowIndex, 0)

                    mediaSession.setQueueTitle(title)
                    log { "prepare for : ${queue[windowIndex].description}" }
                }

            })
            it.setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
                private val window = Timeline.Window()

                override fun getMediaDescription(
                    player: Player,
                    windowIndex: Int
                ): MediaDescriptionCompat {
                    return player.currentTimeline.getWindow(
                        windowIndex,
                        window,
                        true
                    ).tag as MediaDescriptionCompat
                }

            })

            handleNotification()
        }

    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {

        if (parentId != ROOT) return

        //We only have a playlist which user has played from UI
        val mediaItems =
            mediaController.queue.orEmpty()
                .map { MediaItem(it.description, MediaItem.FLAG_PLAYABLE) }
        log { "send result : $mediaItems" }
        result.sendResult(mediaItems.toMutableList())
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        //TODO validate call of client

        log { "on get root : $ROOT" }

        return BrowserRoot(ROOT, null)
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onShuffleModeChanged(shuffleMode: Int) {
            log { "onShuffleModeChanged : $shuffleMode" }
            super.onShuffleModeChanged(shuffleMode)
        }


        override fun onRepeatModeChanged(repeatMode: Int) {
            log { "onRepeatModeChanged  : $repeatMode" }
            mediaSession.setRepeatMode(repeatMode)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            log { "onMetadataChanged : ${metadata?.description}" }
            mediaController.playbackState?.let { updateNotification(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { updateNotification(it) }
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            notifyChildrenChanged(ROOT)
        }

        private fun updateNotification(state: PlaybackStateCompat) {
            when (state.state) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {
                    becomingNoisyReceiver.register()
                }
                else -> {
                    becomingNoisyReceiver.unregister()
                }
            }
            notificationBuilder.updateNotification(mediaSession.sessionToken)
        }


    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        /**
         * By stopping playback, the player will transition to [Player.STATE_IDLE]. This will
         * cause a state change in the MediaSession, and (most importantly) call
         * [MediaControllerCallback.onPlaybackStateChanged]. Because the playback state will
         * be reported as [PlaybackStateCompat.STATE_NONE], the service will first remove
         * itself as a foreground service, and will then call [stopSelf].
         */
        exoPlayer.stop(true)
    }

    override fun onDestroy() {
        mediaSession.apply {
            isActive = false
            release()
        }
        notificationBuilder.destroy()
        super.onDestroy()
    }

}