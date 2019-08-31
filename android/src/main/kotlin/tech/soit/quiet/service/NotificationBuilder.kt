package tech.soit.quiet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import tech.soit.quiet.R
import tech.soit.quiet.utils.*
import java.net.HttpURLConnection
import java.net.URI
import kotlin.coroutines.resume


/**
 * Helper class to encapsulate code for building notifications.
 *
 * TODO action build callback from flutter
 */

class NotificationBuilder(private val context: Context) {

    companion object {
        const val NOW_PLAYING_CHANNEL: String = "TODO" //TODO build channel from context
        const val NOW_PLAYING_NOTIFICATION: Int = 0xb339

        private val targetList = listOf(
            Target.DARK_MUTED,
            Target.LIGHT_MUTED,
            Target.DARK_VIBRANT,
            Target.LIGHT_VIBRANT
        )

        private fun Palette.getAvailableSwatch(): Palette.Swatch? {
            targetList.forEach {
                val swatch = getSwatchForTarget(it)
                if (swatch != null) return swatch
            }
            return null
        }
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


    val notificationGenerator = Channel<Notification?>()

    private var job: Job? = null

    fun updateNotification(
        sessionToken: MediaSessionCompat.Token
    ) {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.Main) {
            val controller = MediaControllerCompat(context, sessionToken)
            if (controller.metadata == null) return@launch

            val playbackState = controller.playbackState
            val description = controller.metadata.description
            if (playbackState.state == PlaybackStateCompat.STATE_NONE) {
                notificationGenerator.send(null)
                return@launch
            }

            notificationGenerator.send(
                buildNotificationWithIcon(
                    sessionToken,
                    description,
                    playbackState,
                    controller.sessionActivity
                )
            )
            if (description.iconBitmap == null && description.iconUri == null) {
                return@launch
            }
            var icon = description.iconBitmap
            var color: Int? = null
            if (icon == null) {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        loadImageFromUri(requireNotNull(description.iconUri))
                    }
                }
                icon = result.getOrElse {
                    it.printStackTrace()
                    log(LoggerLevel.WARN) {
                        "get icon for failed: ${description.iconUri}, msg:  ${it.message}"
                    }
                    null
                }
            }
            if (icon == null) return@launch
            val generate = Palette.from(icon).generate()
            val swatch = generate.getAvailableSwatch()
            if (swatch != null) {
                color = swatch.rgb
            }
            notificationGenerator.send(
                buildNotificationWithIcon(
                    sessionToken,
                    description,
                    playbackState,
                    controller.sessionActivity,
                    icon,
                    color
                )
            )
        }


    }


    private suspend fun loadImageFromUri(iconUri: Uri): Bitmap =
        suspendCancellableCoroutine { continuation ->
            val urlConnection =
                URI.create(iconUri.toString()).toURL().openConnection() as HttpURLConnection
            urlConnection.connect()

            continuation.invokeOnCancellation {
                urlConnection.disconnect()
            }
            val bitmap = urlConnection.inputStream.use { stream ->
                val bytes = stream.readBytes()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            continuation.resume(bitmap)
        }

    private fun buildNotificationWithIcon(
        sessionToken: MediaSessionCompat.Token,
        description: MediaDescriptionCompat,
        playbackState: PlaybackStateCompat,
        sessionActivity: PendingIntent?,
        largeIcon: Bitmap? = null,
        color: Int? = null
    ): Notification {


        val builder = NotificationCompat.Builder(
            context,
            NOW_PLAYING_CHANNEL
        )

        // Only add actions for skip back, play/pause, skip forward, based on what's enabled.
        var playPauseIndex = 0
        if (playbackState.isSkipToPreviousEnabled) {
            builder.addAction(skipToPreviousAction)
            ++playPauseIndex
        }
        if (playbackState.isPlaying) {
            builder.addAction(pauseAction)
        } else if (playbackState.isPlayEnabled) {
            builder.addAction(playAction)
        }
        if (playbackState.isSkipToNextEnabled) {
            builder.addAction(skipToNextAction)
        }

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(sessionToken)
            .setShowActionsInCompactView(playPauseIndex)
            .setShowCancelButton(true)

        return builder.setContentIntent(sessionActivity)
            .setContentText(description.subtitle)
            .setContentTitle(description.title)
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
