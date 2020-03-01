package tech.soit.quiet

import android.content.Context
import android.net.Uri
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterJNI
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.embedding.engine.plugins.shim.ShimPluginRegistry
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
    val userAgent: String?
) {

    companion object {
        val Default = Config(enableCache = false, userAgent = null)
    }

    constructor(map: Map<String, Any>) : this(
        enableCache = map["enableCache"] as? Boolean ?: false,
        userAgent = map["userAgent"] as? String
    )

}

class MusicPlayerServicePlugin(
    private val methodChannel: MethodChannel,
    private val dartExecutor: DartExecutor,
    private val playerSession: MusicPlayerSessionImpl
) : MethodChannel.MethodCallHandler {

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
            registerPlugins(engine)

            val channel = MethodChannel(
                ShimPluginRegistry(engine).registrarFor(MusicPlayerServicePlugin::class.java.name).messenger(),
                NAME
            )

            val helper = MusicPlayerServicePlugin(channel, engine.dartExecutor, playerSession)
            channel.setMethodCallHandler(helper)
            return helper
        }


        private fun registerPlugins(flutterEngine: FlutterEngine) {
            try {
                val generatedPluginRegistrant =
                    Class.forName("io.flutter.plugins.GeneratedPluginRegistrant")
                val registrationMethod =
                    generatedPluginRegistrant.getDeclaredMethod(
                        "registerWith",
                        FlutterEngine::class.java
                    )
                registrationMethod.invoke(null, flutterEngine)
            } catch (e: Exception) {
                log { "Tried to automatically register plugins with FlutterEngine ($flutterEngine) but could not find and invoke the GeneratedPluginRegistrant." }
            }
        }

    }

    init {
        playerSession.addCallback(MusicPlayerCallbackPlugin(methodChannel))
    }


    var config = Config.Default

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


    private suspend inline fun <reified T> MethodChannel.invokeAsyncCast(
        method: String,
        arguments: Any?,
        crossinline parseDartResult: (Any?) -> T = { it as T },
        onNotImplement: () -> T
    ): T {
        if (dartExecutor.isolateServiceId == null) {
            // run background entry point failed
            log(LoggerLevel.ERROR) {
                """
                    We can not run background isolate, if you want custom player behavior, maybe you
                    should add a top function named with playerBackgroundService() at you main.dart
                """.trimIndent()
            }
        }
        return runCatching {
            withTimeout(10000) {
                parseDartResult(invokeAsync(method, arguments))
            }
        }.onFailure {
            if (it !is NotImplementedError) {
                logError(it)
            }
        }.getOrElse { onNotImplement() }

    }


    suspend fun loadImage(metadata: MusicMetadata, uri: String): Artwork? {
        val bytes = methodChannel.invokeAsyncCast("loadImage", metadata.obj) {
            loadArtworkFromUri(Uri.parse(uri))
        } ?: return null
        return createArtworkFromByteArray(bytes)
    }


    suspend fun getPlayUrl(id: String, fallback: String?): Uri {
        val url = methodChannel.invokeAsyncCast(
            "getPlayUrl", mapOf("id" to id, "url" to fallback)
        ) { fallback } ?: throw IllegalStateException("can not get play uri for $id .")
        return Uri.parse(url)
    }


    suspend fun onPlayNextNoMoreMusic(
        playQueue: PlayQueue,
        playMode: PlayMode
    ): MusicMetadata? {
        return methodChannel.invokeAsyncCast(
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
        return methodChannel.invokeAsyncCast(
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

}