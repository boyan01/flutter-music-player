package tech.soit.quiet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tech.soit.quiet.player.MusicMetadata
import tech.soit.quiet.player.PlayQueue
import tech.soit.quiet.service.MusicPlayerService
import tech.soit.quiet.utils.getNext
import tech.soit.quiet.utils.getPrevious


private const val UI_PLUGIN_NAME = "tech.soit.quiet/player.ui"

class MusicPlayerUiPlugin : FlutterPlugin {

    private var playerUiChannel: MusicPlayerUiChannel? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(binding.binaryMessenger, UI_PLUGIN_NAME)
        playerUiChannel = MusicPlayerUiChannel(channel, binding.applicationContext)
        channel.setMethodCallHandler(playerUiChannel)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        playerUiChannel?.destroy()
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
                "seekTo" -> session.seekTo(call.arguments<Number>().toLong())
                "setPlayMode" -> session.playMode = call.arguments()
                "setPlayQueue" -> session.playQueue = PlayQueue(call.arguments<Map<String, Any>>())
                "getNext" -> session.getNext(MusicMetadata.fromMap(call.arguments()))?.obj
                "getPrevious" -> session.getPrevious(MusicMetadata.fromMap(call.arguments()))?.obj
                "insertToNext" -> session.addMetadata(
                    MusicMetadata.fromMap(call.arguments()),
                    session.current?.mediaId
                )
                "setPlaybackSpeed" -> session.setPlaybackSpeed(call.arguments<Double>())
                else -> null
            }

            when (r) {
                Unit -> result.success(null)
                null -> result.notImplemented()
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


private class RemotePlayer : ServiceConnection {

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
            GlobalScope.launch(Dispatchers.Main) { call(session) }
        }
    }
}
