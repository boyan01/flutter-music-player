package tech.soit.quiet.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tech.soit.quiet.BackgroundHandle
import tech.soit.quiet.MusicPlayerBackgroundPlugin
import tech.soit.quiet.player.PlayList
import tech.soit.quiet.player.PlayListExt
import tech.soit.quiet.player.PlayMode
import tech.soit.quiet.player.SetPlayModeActionProvider
import tech.soit.quiet.receiver.BecomingNoisyReceiver
import tech.soit.quiet.service.NotificationBuilder.Companion.NOW_PLAYING_NOTIFICATION
import tech.soit.quiet.utils.COMMANDS
import tech.soit.quiet.utils.LoggerLevel
import tech.soit.quiet.utils.log
import tech.soit.quiet.utils.toMediaSource


/**
 * music player service of Application
 */
class MusicPlayerService : MediaBrowserServiceCompat() {

    companion object {

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

    // To interact with Dart in background
    lateinit var backgroundHandle: BackgroundHandle
        private set


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

    private fun Player.performPlay(mediaMetadata: MediaMetadataCompat) {
        this as ExoPlayer
        prepare(mediaMetadata.toMediaSource(this@MusicPlayerService))
        playWhenReady = true
        playList.current = mediaMetadata
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

    // The playlist which set from Dart. only Dart can update player's playlist
    private var playList = PlayList.empty

    override fun onCreate() {
        // start background handle
        backgroundHandle = MusicPlayerBackgroundPlugin.startBackgroundIsolate(this)
        super.onCreate()
        sessionToken = mediaSession.sessionToken
        playList.attach(mediaSession)
        mediaController = MediaControllerCompat(this, mediaSession).apply {
            registerCallback(MediaControllerCallback())
        }

        val sessionConnector = MediaSessionConnector(mediaSession, object : DefaultPlaybackController() {
            override fun getSupportedPlaybackActions(player: Player?): Long {
                return ACTIONS
            }

        }, null /*remove default metadata provider*/)
        exoPlayer.addListener(autoPlayNextListener)
        sessionConnector.setPlayer(exoPlayer, object : MediaSessionConnector.PlaybackPreparer {
            override fun onPrepareFromSearch(query: String?, extras: Bundle?) = Unit

            override fun onCommand(player: Player?, command: String, extras: Bundle?, cb: ResultReceiver?) {

            }


            override fun getCommands(): Array<String>? {
                return COMMANDS
            }

            override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) = Unit

            override fun onPrepare() = Unit

            override fun getSupportedPrepareActions(): Long {
                return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
            }

            override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
                log { "onPrepareFromMediaId : $mediaId" }
                val metadata = if (mediaId == null) {
                    playList.queue.firstOrNull()
                } else {
                    playList.findMetadataByMediaId(mediaId)
                }
                metadata ?: return


                val mediaSource = metadata.toMediaSource(this@MusicPlayerService)
                exoPlayer.prepare(mediaSource)
                playList.current = metadata
            }

        }, SetPlayModeActionProvider(::playList))

        sessionConnector.setQueueNavigator(object : MediaSessionConnector.QueueNavigator {

            override fun onSkipToQueueItem(player: Player, id: Long) {
                val metadata = playList.getMetadataByQueueId(id) ?: return
                player.performPlay(metadata)
            }

            override fun onCurrentWindowIndexChanged(player: Player) {
                log { "onCurrentWindowIndexChanged : ${player.currentWindowIndex}" }
            }

            override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {
                val mode = playList.playMode
                playList = PlayListExt.parsePlayListFromArgument(extras!!)
                playList.playMode = mode
                player?.stop()
                playList.attach(mediaSession)
            }

            override fun getSupportedQueueNavigatorActions(player: Player?): Long {
                if (player == null) return 0L
                return MediaSessionConnector.QueueNavigator.ACTIONS
            }

            override fun onSkipToNext(player: Player?) {
                val next = if (playList.playMode == PlayMode.Single) {
                    // PlayNext order by user, so we should play next music even in single mode.
                    playList.getNext(mediaController.metadata, PlayMode.Sequence)
                } else {
                    playList.getNext(mediaController.metadata)
                }
                next ?: return
                player?.performPlay(next)
            }

            override fun getActiveQueueItemId(player: Player?): Long {
                val metadata = mediaController.metadata
                        ?: return MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()
                return playList.getQueueID(metadata)
            }

            override fun onSkipToPrevious(player: Player?) {
                val previous = if (playList.playMode == PlayMode.Single) {
                    // Order by user, so we should play previous music even in single mode.
                    playList.getPrevious(mediaController.metadata, PlayMode.Sequence)
                } else {
                    playList.getPrevious(mediaController.metadata)
                }
                previous ?: return
                player?.performPlay(previous)
            }

            override fun getCommands(): Array<String> {
                return PlayListExt.commands
            }

            override fun onTimelineChanged(player: Player?) = Unit

        })

        handleNotification()


    }

    private val autoPlayNextListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val next = playList.getNext(mediaController.metadata)
                next?.let {
                    exoPlayer.performPlay(next)
                }
            }
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

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            log { "onMetadataChanged : ${metadata?.description}" }
            mediaController.playbackState?.let { updateNotification(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { updateNotification(it) }
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