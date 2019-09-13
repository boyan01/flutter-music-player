package tech.soit.quiet

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tech.soit.quiet.functions.RemoteFunction
import tech.soit.quiet.service.MusicPlayerService
import tech.soit.quiet.utils.log
import tech.soit.quiet.utils.toMap
import tech.soit.quiet.utils.toMediaMetadataCompat

class MusicPlayerPlugin(
    private val channel: MethodChannel,
    private val context: Context
) : MethodCallHandler {

    companion object {

        private const val NAME = "tech.soit.quiet/player"
        private const val NAME_BACKGROUND = "tech.soit.quiet/player.background"

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), NAME)
            val playerPlugin = MusicPlayerPlugin(channel, registrar.context())
            channel.setMethodCallHandler(playerPlugin)
            playerPlugin.connect()
            registrar.addViewDestroyListener {
                playerPlugin.destroy()
                false
            }

            val background = MethodChannel(registrar.messenger(), NAME_BACKGROUND)
            background.setMethodCallHandler { call, result ->
                if (call.method == "registerCallbackHandle") {
                    RemoteFunction.registerCallback(call.arguments<Long>())
                    result.success(null)
                    return@setMethodCallHandler
                }
                result.notImplemented()
            }
        }
    }


    private val connectCallback = object : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            log { "connected , ${mediaBrowser.sessionToken}" }
            mediaController?.unregisterCallback(controllerCallback)
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(controllerCallback)
                controllerCallback.sendInitData()
            }
        }

        override fun onConnectionSuspended() {
            log { "connect suspended" }
            mediaController?.unregisterCallback(controllerCallback)
            mediaController = null
        }

        override fun onConnectionFailed() {
            log { "connect failed" }
            mediaController = null
            //TODO handle connect failed

        }
    }

    private val mediaBrowser: MediaBrowserCompat =
        MediaBrowserCompat(
            context, ComponentName(context, MusicPlayerService::class.java),
            connectCallback, null
        )


    private var mediaController: MediaControllerCompat? = null

    private val controls: MediaControllerCompat.TransportControls? get() = mediaController?.transportControls

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        GlobalScope.launch(Dispatchers.Main) { onMethodCallAsync(call, result) }
    }

    private fun onMethodCallAsync(call: MethodCall, result: MethodChannel.Result) {
        val controls = this.controls
        val mediaController = this.mediaController
        if (controls == null || mediaController == null) {
            result.error("-1", "controls is not available", -1)
            return
        }
        val r: Any = when (call.method) {
            "init" -> controllerCallback.sendInitData()
            /*transport controller*/
            "skipToNext" -> controls.skipToNext()
            "skipToPrevious" -> controls.skipToPrevious()
            "pause" -> controls.pause()
            "play" -> controls.play()
            "playFromMediaId", "prepareFromMediaId" -> {
                val mediaId = call.argument<String>("mediaId")
                val playList = call.argument<List<Map<String, Any>>>("queue")
                val extras = playList?.map { it.toMediaMetadataCompat() }?.let { medias ->
                    Bundle().apply {
                        putParcelableArrayList("queue", ArrayList(medias))
                        putString("queueTitle", call.argument("queueTitle"))
                    }
                }
                if (call.method == "prepareFromMediaId") {
                    controls.prepareFromMediaId(mediaId, extras)
                } else {
                    controls.playFromMediaId(mediaId, extras)
                }
            }
            "seekTo" -> controls.seekTo((call.arguments as Number).toLong())
            "setShuffleMode" -> controls.setShuffleMode(call.arguments as Int)
            "setRepeatMode" -> controls.setRepeatMode(call.arguments as Int)
            /*media controller*/
            "isSessionReady" -> mediaController.isSessionReady
            "getPlaybackState" -> mediaController.playbackState.toMap()
            "getQueue" -> mediaController.queue.map { it.description.toMap() }
            "getQueueTitle" -> mediaController.queueTitle
            "getPlaybackInfo" -> mediaController.playbackInfo.toString() //TODO
            "getRepeatMode" -> mediaController.repeatMode
            "getShuffleMode" -> mediaController.shuffleMode

            "addQueueItem" -> {
                val index = call.argument<Int>("index") ?: 0
                val item = call.argument<Map<*, *>>("item")?.toMediaMetadataCompat() ?: return
                mediaController.addQueueItem(item.description, index)
            }
            "removeQueueItem" -> {
                val item = call.arguments<Map<*, *>>().toMediaMetadataCompat()
                mediaController.removeQueueItem(item.description)
            }
            else -> {
                result.notImplemented()
                return
            }
        }
        result.success(if (r == Unit) null else r)
    }

    private fun destroy() {
        mediaController?.unregisterCallback(controllerCallback)
        mediaController = null

        mediaBrowser.unsubscribe(mediaBrowser.root)
        mediaBrowser.disconnect()
    }

    private fun connect() {
        log { "connect" }
        mediaBrowser.connect()
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {

        fun sendInitData() {
            val mediaController = mediaController ?: return
            channel.invokeMethod(
                "onInit", hashMapOf(
                    "metadata" to mediaController.metadata?.toMap(),
                    "playbackInfo" to null,
                    "playbackState" to mediaController.playbackState?.toMap(),
                    "queue" to mediaController.queue?.map { it.toMap() },
                    "queueTitle" to mediaController.queueTitle,
                    "repeatMode" to mediaController.repeatMode,
                    "shuffleMode" to mediaController.shuffleMode
                )
            )
        }


        override fun onSessionReady() {
            channel.invokeMethod("onSessionReady", null)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            channel.invokeMethod("onPlaybackStateChanged", state.toMap())
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            channel.invokeMethod("onMetadataChanged", metadata.toMap())
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            channel.invokeMethod("onRepeatModeChanged", repeatMode)
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            channel.invokeMethod("onShuffleModeChanged", shuffleMode)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            channel.invokeMethod("onQueueChanged", queue?.map { it.toMap() })
        }

        override fun onAudioInfoChanged(info: MediaControllerCompat.PlaybackInfo) {
            channel.invokeMethod("onAudioInfoChanged", null)
        }

        override fun onSessionDestroyed() {
            channel.invokeMethod("onSessionDestroyed", null)
        }

        override fun onQueueTitleChanged(title: CharSequence?) {
            channel.invokeMethod("onQueueTitleChanged", title.toString())
        }

    }
}
