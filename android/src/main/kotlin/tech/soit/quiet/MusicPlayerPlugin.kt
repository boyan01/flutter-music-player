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
        }
    }


    private val connectCallback = object : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            log { "connected , ${mediaBrowser.sessionToken}" }
            mediaBrowser.subscribe(mediaBrowser.root, mediaSubscription)
            mediaController?.unregisterCallback(controllerCallback)
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(controllerCallback)
            }
        }

        override fun onConnectionSuspended() {
            log { "connect suspended" }
            mediaController?.unregisterCallback(controllerCallback)
            mediaController = null
        }

        override fun onConnectionFailed() {
            log { "connect failed" }
            mediaController?.unregisterCallback(controllerCallback)
            mediaController = null
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
        val controls = this.controls
        val mediaController = this.mediaController
        if (controls == null || mediaController == null) {
            result.error("-1", "controls is not available", -1)
            return
        }
        val r: Any = when (call.method) {
            /*transport controller*/
            "skipToNext" -> controls.skipToNext()
            "skipToPrevious" -> controls.skipToPrevious()
            "pause" -> controls.pause()
            "play" -> controls.play()
            "playWithQinDing" -> controls.playFromMediaId(call.argument<Any>("id").toString(), null)
            "seekTo" -> controls.seekTo(call.arguments as Long)
            "setShuffleMode" -> controls.setShuffleMode(call.arguments as Int)
            "setRepeatMode" -> controls.setRepeatMode(call.arguments as Int)
            /*media controller*/
            "getRepeatMode" -> mediaController.repeatMode
            "isSessionReady" -> mediaController.isSessionReady
            "playbackState" -> mediaController.playbackState.toMap()
            "queue" -> mediaController.queue.map { it.description.toMap() }
            "queueTitle" -> mediaController.queueTitle
            "addQueueItem" -> {
                val index = call.argument<Int>("index") ?: 0
                val item = call.argument<Map<*, *>>("item")?.toMediaMetadataCompat() ?: return
                mediaController.addQueueItem(item, index)
            }
            "removeQueueItem" -> {
                val item = call.arguments<Map<*, *>>().toMediaMetadataCompat()
                mediaController.removeQueueItem(item)
            }
            /*custom*/
            "setPlayList" -> {
                //set current playing play list
                val items = call.arguments<List<Map<*, *>>>().map { it.toMediaMetadataCompat() }
                val bundle = Bundle(1)
                bundle.putParcelableArrayList("playlist", ArrayList(items))
                mediaBrowser.sendCustomAction("setPlaylist", bundle, null)
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

    private val mediaSubscription = object : MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            val musicList = children.map { it.description.toMap() }

            log { "music list : $musicList" }
            channel.invokeMethod("onPlaylistLoaded", musicList)
        }

        override fun onError(parentId: String) {
            log { "error to load $parentId" }
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {

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
            channel.invokeMethod("onQueueChanged", queue?.map { it.description.toMap() })
        }


    }
}
