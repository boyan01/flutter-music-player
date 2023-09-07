package tech.soit.quiet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.os.SystemClock
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import tech.soit.quiet.player.MusicMetadata
import tech.soit.quiet.player.PlayMode
import tech.soit.quiet.player.PlayQueue
import tech.soit.quiet.service.MusicPlayerService
import tech.soit.quiet.utils.getNext
import tech.soit.quiet.utils.getPrevious


private const val UI_PLUGIN_NAME = "tech.soit.quiet/player.ui"

class MusicPlayerUiPlugin : FlutterPlugin {

    private var playerUiChannel: AudioPlayerChannel? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(binding.binaryMessenger, UI_PLUGIN_NAME)
        playerUiChannel = AudioPlayerChannel(channel, binding.applicationContext)
        channel.setMethodCallHandler(playerUiChannel)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        playerUiChannel?.destroy()
    }

}


class AudioPlayerChannel(
    private val channel: MethodChannel,
    private val context: Context
) : MethodChannel.MethodCallHandler {

    companion object {
        var lastCreatedPlayerId = 0L
    }

    init {
        channel.setMethodCallHandler(this)
    }

    private val players = mutableMapOf<Long, AudioPlayer>()

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "createPlayer" -> {
                val uri = Uri.parse(call.argument("uri"))
                val playerId = ++lastCreatedPlayerId
                players[playerId] = AudioPlayer(playerId, uri, context, channel)
                result.success(playerId)
            }
            "prepare" -> {
                val playerId = call.argument<Long>("id")!!
                val playWhenReady = call.argument<Boolean>("playWhenReady")!!
                players[playerId]?.prepare(playWhenReady)
                result.success(null)
            }
            "setPlayWhenReady" -> {
                val playerId = call.argument<Long>("id")!!
                val playWhenReady = call.argument<Boolean>("playWhenReady")!!
                players[playerId]?.setPlayWhenReady(playWhenReady)
                result.success(null)
            }

            "setVolume" -> {
                val playerId = call.argument<Long>("id")!!
                val volume = call.argument<Double>("volume")!!
                players[playerId]?.setVolume(volume.toFloat())
                result.success(null)
            }
            "seekTo" -> {
                val playerId = call.argument<Long>("id")!!
                val position = call.argument<Long>("position")!!
                players[playerId]?.seekTo(position)
                result.success(null)
            }
            "getBufferedPosition" -> {
                val playerId = call.argument<Long>("id")!!
                val position = players[playerId]?.getBufferedPosition()
                result.success(position)
            }
            "dispose" -> {
                val playerId = call.argument<Long>("id")!!
                players[playerId]?.release()
                players.remove(playerId)
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }

        }
    }

    fun destroy() {
        players.values.forEach { it.release() }
        players.clear()
    }

}

class AudioPlayer(
    private val playerId: Long,
    uri: Uri,
    context: Context,
    private val channel: MethodChannel
) : Player.Listener {

    companion object {
        private val audioAttribute = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    // Wrap a SimpleExoPlayer with a decorator to handle audio focus for us.
    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttribute, true)
        .build()


    init {
        val factory: DataSource.Factory = DefaultDataSource.Factory(context)
        player.setMediaSource(
            ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(uri))
        )
        player.addListener(this)
    }

    fun setPlayWhenReady(playWhenReady: Boolean) {
        player.playWhenReady = playWhenReady
    }

    fun prepare(playWhenReady: Boolean) {
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    fun setVolume(volume: Float) {
        player.volume = volume
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun release() {
        player.release()
    }

    fun getBufferedPosition(): Long {
        return player.bufferedPosition
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        notifyPositionChanged()
        channel.invokeMethod(
            "onPlayWhenReadyChanged", mapOf(
                "id" to playerId,
                "playWhenReady" to playWhenReady,
                "reason" to reason,
            )
        )
    }

    private fun notifyPositionChanged() {
        channel.invokeMethod(
            "onPositionChanged", mapOf(
                "id" to playerId,
                "position" to player.currentPosition,
                "duration" to player.duration,
                "updateTime" to SystemClock.elapsedRealtime(),
            )
        )
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        notifyPositionChanged()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        channel.invokeMethod(
            "onPlaybackStateChanged", mapOf(
                "id" to playerId,
                "playbackState" to playbackState,
                "duration" to player.duration,
            )
        )
    }

    override fun onPlayerError(error: PlaybackException) {
        channel.invokeMethod(
            "onPlayerError", mapOf(
                "id" to playerId,
                "errorCode" to error.errorCode,
                "message" to error.message,
            )
        )
    }

}

private class MusicPlayerUiChannel(
    channel: MethodChannel,
    context: Context
) : MethodChannel.MethodCallHandler {

    private val remotePlayer = context.startMusicService()

    private val uiPlaybackPlugin = MusicPlayerCallbackPlugin(channel)

    private var destroyed = false

    init {
        remotePlayer.doWhenSessionReady {
            if (!destroyed) {
                it.addCallback(uiPlaybackPlugin)
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) =
        remotePlayer.doWhenSessionReady { session ->
            val r: Any? = when (call.method) {
                "init" -> {
                    uiPlaybackPlugin.onMetadataChanged(session.current)
                    uiPlaybackPlugin.onPlayModeChanged(session.playMode)
                    uiPlaybackPlugin.onPlayQueueChanged(session.playQueue)
                    uiPlaybackPlugin.onPlaybackStateChanged(session.playbackState)
                    session.current != null
                }
                "play" -> session.play()
                "pause" -> session.pause()
                "playFromMediaId" -> session.playFromMediaId(call.arguments())
                "prepareFromMediaId" -> session.prepareFromMediaId(call.arguments())
                "skipToNext" -> session.skipToNext()
                "skipToPrevious" -> session.skipToPrevious()
                "seekTo" -> session.seekTo(call.arguments<Number>()!!.toLong())
                "setPlayMode" -> session.playMode = call.arguments() ?: PlayMode.Sequence.rawValue
                "setPlayQueue" -> session.playQueue =
                    PlayQueue(call.arguments<Map<String, Any>>()!!)
                "getNext" -> session.getNext(MusicMetadata.fromMap(call.arguments()!!))?.obj
                "getPrevious" -> session.getPrevious(MusicMetadata.fromMap(call.arguments()!!))?.obj
                "insertToNext" -> session.addMetadata(
                    MusicMetadata.fromMap(call.arguments()!!),
                    session.current?.mediaId
                )
                "setPlaybackSpeed" -> session.setPlaybackSpeed(call.arguments<Double>()!!)
                else -> {
                    result.notImplemented()
                    return@doWhenSessionReady
                }
            }

            when (r) {
                Unit -> result.success(null)
                else -> result.success(r)
            }
        }

    fun destroy() {
        destroyed = true
        remotePlayer.playerSession?.removeCallback(uiPlaybackPlugin)
    }

}


private fun Context.startMusicService(): RemotePlayer {
    val intent = Intent(this, MusicPlayerService::class.java)
    intent.action = MusicPlayerService.ACTION_MUSIC_PLAYER_SERVICE
    startService(intent)
    val player = RemotePlayer()
    if (!bindService(intent, player, Context.BIND_AUTO_CREATE)) {
        if (BuildConfig.DEBUG) {
            throw IllegalStateException("can not connect to MusicService")
        }
    }
    return player
}


private class RemotePlayer : ServiceConnection, CoroutineScope by MainScope() {

    var playerSession: MusicPlayerSession? = null
        private set

    private val pendingExecution = mutableListOf<suspend (MusicPlayerSession) -> Unit>()

    override fun onServiceDisconnected(name: ComponentName?) {
        playerSession = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        playerSession = MusicPlayerSession.Stub.asInterface(service)
        ArrayList(pendingExecution).forEach(::doWhenSessionReady)
        pendingExecution.clear()
    }

    fun doWhenSessionReady(call: suspend (MusicPlayerSession) -> Unit) {
        val session = playerSession
        if (session == null) {
            pendingExecution.add(call)
        } else {
            launch { call(session) }
        }
    }
}
