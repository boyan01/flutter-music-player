package tech.soit.quiet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import tech.soit.quiet.player.PlayQueue
import tech.soit.quiet.service.MusicPlayerService2


private const val UI_PLUGIN_NAME = "tech.soit.quiet/player.ui"

class MusicPlayerUiPlugin : FlutterPlugin {

    private lateinit var playerUiChannel: MusicPlayerUiChannel1
    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(binding.binaryMessenger, UI_PLUGIN_NAME)
        playerUiChannel = MusicPlayerUiChannel1(channel, binding.applicationContext)
        channel.setMethodCallHandler(playerUiChannel)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        playerUiChannel.channel.setMethodCallHandler(null)
    }

}


private class MusicPlayerUiChannel1(
    val channel: MethodChannel,
    context: Context
) : MethodChannel.MethodCallHandler {


    private val remotePlayer = context.startMusicService()

    init {
        remotePlayer.doWhenSessionReady { it.addCallback(MusicPlayerCallbackPlugin(channel)) }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) =
        remotePlayer.doWhenSessionReady { session ->
            when (call.method) {
                "init" -> {
                    //TODO bring music player to last status
                }
                "play" -> session.play()
                "pause" -> session.pause()
                "playFromMediaId" -> session.playFromMediaId(call.arguments())
                "skipToNext" -> session.skipToNext()
                "skipToPrevious" -> session.skipToPrevious()
                "seekTo" -> session.seekTo(call.arguments<Number>().toLong())
                "setPlayMode" -> session.setPlayMode(call.arguments())
                "setPlayQueue" -> session.setPlayQueue(PlayQueue(call.arguments<Map<String, Any>>()))
                else -> result.notImplemented()
            }
        }

}


private fun Context.startMusicService(): RemotePlayer {
    val intent = Intent(this, MusicPlayerService2::class.java)
    intent.action = MusicPlayerService2.ACTION_MUSIC_PLAYER_SERVICE
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

    private var playerSession: MusicPlayerSession? = null

    private val pendingExecution = mutableListOf<(MusicPlayerSession) -> Unit>()

    override fun onServiceDisconnected(name: ComponentName?) {
        playerSession = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        playerSession = MusicPlayerSession.Stub.asInterface(service)
        ArrayList(pendingExecution).forEach(::doWhenSessionReady)
        pendingExecution.clear()
    }

    fun doWhenSessionReady(call: (MusicPlayerSession) -> Unit) {
        val session = playerSession
        if (session == null) {
            pendingExecution.add(call)
        } else {
            call(session)
        }
    }
}