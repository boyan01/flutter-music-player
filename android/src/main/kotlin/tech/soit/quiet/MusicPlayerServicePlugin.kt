package tech.soit.quiet

import android.content.Context
import android.net.Uri
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterJNI
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterMain
import kotlinx.coroutines.withTimeout
import tech.soit.quiet.player.MusicMetadata
import tech.soit.quiet.player.MusicPlayerSessionImpl
import tech.soit.quiet.player.PlayMode
import tech.soit.quiet.player.PlayQueue
import tech.soit.quiet.utils.*

data class Config(
    val enableCache: Boolean = false,
    val userAgent: String?,
    val pauseWhenTaskRemoved: Boolean,
) {

    companion object {
        val Default = Config(enableCache = false, userAgent = null, pauseWhenTaskRemoved = true)
    }

    constructor(map: Map<String, Any>) : this(
        enableCache = map["enableCache"] as? Boolean ?: false,
        userAgent = map["userAgent"] as? String,
        pauseWhenTaskRemoved = map["pauseWhenTaskRemoved"] as? Boolean ?: true
    )

}

class MusicPlayerServicePlugin(
    private val playerSession: MusicPlayerSessionImpl
) : FlutterPlugin, MethodChannel.MethodCallHandler {

    companion object {

        private const val NAME = "tech.soit.quiet/background_callback"

        /**
         * start flutter background isolate
         *
         *
         * Note:
         * The flutter background entry point should be placed in lib/main.dart (the same as main() method)
         *
         */
        fun startServiceIsolate(
            context: Context,
            playerSession: MusicPlayerSessionImpl
        ): MusicPlayerServicePlugin {
            try {
                FlutterMain.startInitialization(context)
            } catch (e: UnsatisfiedLinkError) {
                // in android test mode, we don't have libflutter.so in apk
                //TODO YangBin test compatibility
                throw e
            }
            FlutterMain.ensureInitializationComplete(context, null)

            val engine = FlutterEngine(context, FlutterLoader(), FlutterJNI())
            engine.dartExecutor.executeDartEntrypoint(
                DartExecutor.DartEntrypoint(
                    FlutterMain.findAppBundlePath(),
                    "playerBackgroundService"
                )
            )
            val servicePlugin = MusicPlayerServicePlugin(playerSession)
            engine.plugins.add(servicePlugin)
            return servicePlugin
        }

    }

    var config = Config.Default
        private set

    private var methodChannel: MethodChannel? = null
    private var playerCallback: MusicPlayerCallbackPlugin? = null

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "updateConfig" -> {
                config = Config(call.arguments())
                result.success(null)
            }
            "insertToPlayQueue" -> {
                val list = call.argument<List<Any>>("list")!!.map {
                    @Suppress("UNCHECKED_CAST")
                    MusicMetadata.fromMap(it as Map<String, Any?>)
                }
                val index = call.argument<Int>("index")!!
                playerSession.insertMetadataList(list, index)
                result.success(null)
            }
            else -> result.notImplemented()

        }
    }


    private suspend fun <T> invokeAsyncCast(
        method: String,
        arguments: Any?,
        parseDartResult: (Any?) -> T = {
            @Suppress("UNCHECKED_CAST")
            it as T
        },
        onNotImplement: suspend () -> T
    ): T {
        val methodChannel = this.methodChannel
        if (methodChannel == null) {
            // run background entry point failed
            log(LoggerLevel.ERROR) {
                """
                    We can not run background isolate, if you want custom player behavior, maybe you
                    should add a top function named with playerBackgroundService() at you main.dart
                """.trimIndent()
            }
            return onNotImplement()
        }
        return runCatching {
            withTimeout(10000) {
                parseDartResult(methodChannel.invokeAsync(method, arguments))
            }
        }.onFailure {
            if (it !is NotImplementedError) {
                logError(it)
            }
        }.getOrElse { onNotImplement() }

    }


    suspend fun loadImage(metadata: MusicMetadata, uri: String): Artwork? {
        val bytes = invokeAsyncCast("loadImage", metadata.obj) {
            loadArtworkFromUri(Uri.parse(uri))
        } ?: return null
        return createArtworkFromByteArray(bytes)
    }


    suspend fun getPlayUrl(id: String, fallback: String?): Uri {
        val url = invokeAsyncCast(
            "getPlayUrl", mapOf("id" to id, "url" to fallback)
        ) { fallback } ?: throw IllegalStateException("can not get play uri for $id .")
        return Uri.parse(url)
    }


    suspend fun onPlayNextNoMoreMusic(
        playQueue: PlayQueue,
        playMode: PlayMode
    ): MusicMetadata? {
        return invokeAsyncCast(
            "onPlayNextNoMoreMusic", mapOf(
                "queue" to playQueue.toDartMapObject(),
                "playMode" to playMode.rawValue
            ),
            parseDartResult = {
                @Suppress("UNCHECKED_CAST")
                (it as? Map<String, Any?>)?.let(MusicMetadata.Companion::fromMap)
            }
        ) {
            if (playMode == PlayMode.Shuffle) {
                playQueue.generateShuffleList()
            }
            playQueue.getNext(null, playMode)
        }
    }

    suspend fun onPlayPreviousNoMoreMusic(
        playQueue: PlayQueue,
        playMode: PlayMode
    ): MusicMetadata? {
        return invokeAsyncCast(
            "onPlayPreviousNoMoreMusic", mapOf(
                "queue" to playQueue.toDartMapObject(),
                "playMode" to playMode.rawValue
            ),
            parseDartResult = {
                @Suppress("UNCHECKED_CAST")
                (it as? Map<String, Any?>)?.let(MusicMetadata.Companion::fromMap)
            }
        ) {
            if (playMode == PlayMode.Shuffle) {
                playQueue.generateShuffleList()
            }
            playQueue.getPrevious(null, playMode)
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(binding.binaryMessenger, NAME)
        channel.setMethodCallHandler(this)
        val callback = MusicPlayerCallbackPlugin(channel)
        playerSession.addCallback(callback)

        this.methodChannel = channel
        this.playerCallback = callback
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        this.methodChannel = null
        playerCallback?.let { playerSession.removeCallback(it) }

    }

}