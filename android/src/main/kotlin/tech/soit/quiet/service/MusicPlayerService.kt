package tech.soit.quiet.service

import android.app.PendingIntent
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
import tech.soit.quiet.BackgroundChannel
import tech.soit.quiet.MusicPlayerBackgroundChannel
import tech.soit.quiet.player.*
import tech.soit.quiet.utils.*
import kotlin.properties.Delegates


/**
 * music player service of Application
 */
class MusicPlayerService : MediaBrowserServiceCompat(), PlayModeContainer {


    companion object {

        private val audioAttribute = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        private const val ROOT = "/"

    }

    val mediaSession by lazy {
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


    // To interact with Dart in background
    lateinit var backgroundChannel: BackgroundChannel
        private set


    private val notificationManager by lazy { NotificationManagerCompat.from(this) }

    private lateinit var mediaController: MediaControllerCompat

    private var isForegroundService = false

    // Wrap a SimpleExoPlayer with a decorator to handle audio focus for us.
    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this).apply {
            setAudioAttributes(audioAttribute, true)
        }
    }


    override var playMode: PlayMode by Delegates.observable(PlayMode.Sequence) { _, _, newValue ->
        mediaSession.setRepeatMode(newValue.repeatMode())
        mediaSession.setShuffleMode(newValue.shuffleMode())
    }

    // The playlist which set from Dart. only Dart can update player's playlist
    private var playList = PlayList.empty

    override fun onCreate() {
        // start background handle
        backgroundChannel = MusicPlayerBackgroundChannel.startBackgroundIsolate(this)
        super.onCreate()
        sessionToken = mediaSession.sessionToken
        playList.attach(this)
        mediaController = MediaControllerCompat(this, mediaSession).apply {
        }

        val sessionConnector =
            MediaSessionConnector(mediaSession, object : DefaultPlaybackController() {
                override fun getSupportedPlaybackActions(player: Player?): Long {
                    return ACTIONS
                }

            }, null /*remove default metadata provider*/)
        exoPlayer.addListener(autoPlayNextListener)
        sessionConnector.setPlayer(exoPlayer, object : MediaSessionConnector.PlaybackPreparer {
            override fun onPrepareFromSearch(query: String?, extras: Bundle?) = Unit

            override fun onCommand(
                player: Player?,
                command: String,
                extras: Bundle?,
                cb: ResultReceiver?
            ) {

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
                mediaSession.setMetadata(metadata)
            }

        }, SetPlayModeActionProvider(this))

        sessionConnector.setQueueNavigator(object : MediaSessionConnector.QueueNavigator {

            override fun onSkipToQueueItem(player: Player, id: Long) {
                val metadata = playList.getMetadataByQueueId(id) ?: return
                mediaController.transportControls.playFromMediaId(metadata.mediaId, null)
            }

            override fun onCurrentWindowIndexChanged(player: Player) {
                log { "onCurrentWindowIndexChanged : ${player.currentWindowIndex}" }
            }

            override fun onCommand(
                player: Player?,
                command: String?,
                extras: Bundle?,
                cb: ResultReceiver?
            ) {
                if (command == PlayListExt.COMMAND_UPDATE_PLAYLIST) {
                    playList = PlayListExt.parsePlayListFromArgument(extras!!)
                    player?.stop()
                    playList.attach(this@MusicPlayerService)
                }
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
                mediaController.transportControls.playFromMediaId(next.mediaId, null)
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
                mediaController.transportControls.playFromMediaId(previous.mediaId, null)
            }

            override fun getCommands(): Array<String> {
                return PlayListExt.commands
            }

            override fun onTimelineChanged(player: Player?) = Unit

        })

    }

    private val autoPlayNextListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val next = playList.getNext(mediaController.metadata)
                next?.let {
                    mediaController.transportControls.playFromMediaId(next.mediaId, null)
                }
            }
            if (playbackState == Player.STATE_READY) {
                /// invalidate metadata duration
                val metadata = requireNotNull(mediaController.metadata, { "illegal state" })
                val duration = if (exoPlayer.duration == C.TIME_UNSET) -1 else exoPlayer.duration
                val update = MediaMetadataCompat.Builder(metadata)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    .build()
                mediaSession.setMetadata(update)
            }
        }
    }


    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {

        if (parentId != ROOT) return

        //We only have a playlist which user has played from UI
        val mediaItems = playList.queue.map { MediaItem(it.description, MediaItem.FLAG_PLAYABLE) }
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
        super.onDestroy()
    }


}