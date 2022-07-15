package tech.soit.quiet.service

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.media3.common.AudioAttributes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import tech.soit.quiet.MusicPlayerServicePlugin
import tech.soit.quiet.ext.UrlUpdatingDataSource
import tech.soit.quiet.player.MusicPlayerSessionImpl
import tech.soit.quiet.receiver.BecomingNoisyReceiverAdapter

class MusicPlayerService : MediaLibraryService(), LifecycleOwner {

    companion object {
        const val ACTION_MUSIC_PLAYER_SERVICE = "tech.soit.quiet.session.MusicSessionService"

        const val META_DATA_PLAYER_LAUNCH_ACTIVITY_ACTION = "tech.soit.quiet.session.LaunchActivityAction"
    }

    private val lifecycle = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycle

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    private val librarySessionCallback = CustomMediaLibrarySessionCallback()

    private lateinit var playerSession: MusicPlayerSessionImpl

    private lateinit var servicePlugin: MusicPlayerServicePlugin

    override fun onCreate() {
        super.onCreate()

        playerSession = MusicPlayerSessionImpl(this, player)
        servicePlugin = MusicPlayerServicePlugin.startServiceIsolate(this, playerSession)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    UrlUpdatingDataSource.Factory(DefaultDataSource.Factory(this), servicePlugin),
                    DefaultExtractorsFactory()
                )
            )
            .build()


        lifecycle.currentState = Lifecycle.State.CREATED

        val applicationInfo = packageManager
            ?.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val destinationAction = applicationInfo?.metaData
            ?.getString(META_DATA_PLAYER_LAUNCH_ACTIVITY_ACTION, null)
        val customIntent = if (destinationAction == null) null else Intent(destinationAction)

        mediaSession = MediaLibrarySession.Builder(this, player, librarySessionCallback)
            .apply {
                if (customIntent != null) {
                    val intent = PendingIntent.getActivity(this@MusicPlayerService, 1000, customIntent, 0)
                    setSessionActivity(intent)
                }
            }
            .build()


        playerSession.addCallback(MusicSessionCallbackAdapter(mediaSession, this))
        playerSession.addCallback(BecomingNoisyReceiverAdapter(this, playerSession))
        val notificationAdapter = NotificationAdapter(this, playerSession, mediaSession)
        playerSession.addCallback(notificationAdapter)
        lifecycle.addObserver(notificationAdapter)
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == ACTION_MUSIC_PLAYER_SERVICE) {
            return playerSession.asBinder()
        }
        return super.onBind(intent)
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (playerSession.servicePlugin.config.pauseWhenTaskRemoved) {
            playerSession.stop()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaSession
    }

    override fun onDestroy() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
        player.release()
        mediaSession.release()
        playerSession.destroy()
        super.onDestroy()
    }


    private class CustomMediaLibrarySessionCallback : MediaLibrarySession.Callback {


    }


}
