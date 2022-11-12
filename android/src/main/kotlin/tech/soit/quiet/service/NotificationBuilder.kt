package tech.soit.quiet.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import tech.soit.quiet.R
import tech.soit.quiet.player.*
import tech.soit.quiet.utils.ArtworkCache

private sealed class NotificationItem {

    class State(
        val state: PlaybackState,
        val notification: Notification?
    ) : NotificationItem()

    object Dismiss : NotificationItem()

}


class NotificationAdapter(
    private val context: Service,
    private val playerSession: MusicPlayerSessionImpl,
    private val mediaSession: MediaSessionCompat
) : BaseMusicSessionCallback(), LifecycleObserver, CoroutineScope by MainScope() {

    private val notificationBuilder = NotificationBuilder(context)

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }


    override fun onMetadataChanged(metadata: MusicMetadata?) {
        updateNotification()
    }

    override fun onPlaybackStateChanged(state: PlaybackState) {
        updateNotification()
    }

    private fun updateNotification() {
        notificationBuilder.updateNotification(mediaSession, playerSession)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        startNotificationRunner()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        notificationBuilder.destroy()
    }

    private var isForegroundService = false

    private fun startNotificationRunner() = launch(Dispatchers.Main) {

        for (item in notificationBuilder.notificationGenerator) {
            when (item) {
                NotificationItem.Dismiss -> dismissNotification()
                is NotificationItem.State -> handlePlayerState(item.state, item.notification)
            }
        }

    }

    @MainThread
    private fun dismissNotification() {
        if (isForegroundService) {
            context.stopForeground(false)
            isForegroundService = false
            context.stopSelf()
            notificationManager.cancel(NotificationBuilder.NOW_PLAYING_NOTIFICATION)
        }
    }

    @MainThread
    private fun handlePlayerState(playbackState: PlaybackState, notification: Notification?) {
        when (val updatedState = playbackState.state) {
            State.Buffering, State.Playing -> {

                /**
                 * This may look strange, but the documentation for [Service.startForeground]
                 * notes that "calling this method does *not* put the service in the started
                 * state itself, even though the name sounds like it."
                 */
                if (!isForegroundService) {
                    context.startService(Intent(context, MusicPlayerService::class.java))
                    context.startForeground(
                        NotificationBuilder.NOW_PLAYING_NOTIFICATION,
                        notification
                    )
                    isForegroundService = true
                } else if (notification != null) {
                    notificationManager.notify(
                        NotificationBuilder.NOW_PLAYING_NOTIFICATION,
                        notification
                    )
                }
            }
            else -> {
                if (isForegroundService) {
                    context.stopForeground(false)
                    isForegroundService = false

                    // If playback has ended, also stop the service.
                    if (updatedState == State.None) {
                        context.stopSelf()
                    }

                    if (notification != null) {
                        notificationManager.notify(
                            NotificationBuilder.NOW_PLAYING_NOTIFICATION,
                            notification
                        )
                    }
                }
            }
        }

    }

}


/**
 * Helper class to encapsulate code for building notifications.
 *
 */
private class NotificationBuilder(private val context: Service) : CoroutineScope by MainScope() {

    companion object {
        const val NOW_PLAYING_CHANNEL: String = "TODO" //TODO build channel from context
        const val NOW_PLAYING_NOTIFICATION: Int = 0xb339

    }

    private val platformNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val skipToPreviousAction = NotificationCompat.Action(
        R.drawable.ic_skip_previous_black_24dp,
        "skip previous",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )
    private val playAction = NotificationCompat.Action(
        R.drawable.ic_play_arrow_black_24dp,
        "play",
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)
    )
    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_pause_black_24dp,
        "pause",
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)
    )
    private val skipToNextAction = NotificationCompat.Action(
        R.drawable.ic_skip_next_black_24dp,
        "skip to next",
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            context,
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )
    private val stopPendingIntent =
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)


    val notificationGenerator = Channel<NotificationItem>()


    fun updateNotification(
        mediaSessionCompat: MediaSessionCompat,
        playerSession: MusicPlayerSessionImpl
    ) {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }
        val metadata = playerSession.current

        if (metadata == null) {
            notificationGenerator.trySend(NotificationItem.Dismiss)
            return
        }

        val playbackState = playerSession.playbackState
        if (playbackState.state == State.None) {
            notificationGenerator.trySend(NotificationItem.State(playbackState, null))
            return
        }

        fun updateNotificationInner(artwork: Bitmap?, color: Int?) {
            notificationGenerator.trySend(
                NotificationItem.State(
                    playbackState, buildNotificationWithIcon(
                        mediaSessionCompat.sessionToken,
                        metadata,
                        playbackState,
                        mediaSessionCompat.controller.sessionActivity,
                        artwork,
                        color
                    )
                )
            )
        }

        val iconUri = metadata.iconUri
        if (iconUri == null) {
            // this description haven't artwork. create notification without image cover
            updateNotificationInner(null, null)
            return
        }

        val iconCacheKey = ArtworkCache.key(metadata)

        if (ArtworkCache[iconCacheKey] != null) {
            val artworkCache = ArtworkCache.get(iconCacheKey)
            updateNotificationInner(artworkCache.bitmap, artworkCache.color)
            return
        }

        updateNotificationInner(null, null)
        launch(Dispatchers.Main) {
            val artwork = playerSession.servicePlugin.loadImage(metadata, iconUri)
            if (artwork != null) {
                ArtworkCache.put(iconCacheKey, artwork)
                updateNotification(mediaSessionCompat, playerSession)
            }
        }
    }

    fun destroy() {
        //clear all cache
        ArtworkCache.evictAll()

        notificationGenerator.close()
    }

    private fun buildNotificationWithIcon(
        sessionToken: MediaSessionCompat.Token,
        metadata: MusicMetadata,
        playbackState: PlaybackState,
        sessionActivity: PendingIntent?,
        largeIcon: Bitmap? = null,
        color: Int? = null
    ): Notification {


        val builder = NotificationCompat.Builder(
            context,
            NOW_PLAYING_CHANNEL
        )

        // Only add actions for skip back, play/pause, skip forward, based on what's enabled.
        builder.addAction(skipToPreviousAction)
        if (playbackState.state == State.Playing) {
            builder.addAction(pauseAction)
        } else {
            builder.addAction(playAction)
        }
        builder.addAction(skipToNextAction)

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(sessionToken)
            .setShowActionsInCompactView(1)
            .setShowCancelButton(true)

        return builder.setContentIntent(sessionActivity)
            .setContentText(metadata.subtitle)
            .setContentTitle(metadata.title)
            .setDeleteIntent(stopPendingIntent)
            .setLargeIcon(largeIcon)
            .setOnlyAlertOnce(true)
            .setColorized(true)
            .setColor(color ?: NotificationCompat.COLOR_DEFAULT)
            .setSmallIcon(R.drawable.ic_music_note_black_24dp)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun shouldCreateNowPlayingChannel() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() =
        platformNotificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel() {
        val notificationChannel = NotificationChannel(
            NOW_PLAYING_CHANNEL,
            "Now Playing",
            NotificationManager.IMPORTANCE_LOW
        )
            .apply {
                description = "show what's playing in ${context.applicationInfo.name}"
            }

        platformNotificationManager.createNotificationChannel(notificationChannel)
    }
}
