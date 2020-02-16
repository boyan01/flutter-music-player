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
import tech.soit.quiet.player.PlayQueue
import tech.soit.quiet.utils.*

typealias BackgroundRegistrar = (registry: FlutterEngine) -> Unit

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
    playerSession: MusicPlayerSession
) : MethodChannel.MethodCallHandler {

    companion object {

        private const val NAME = "tech.soit.quiet/background_callback"

        private var registrar: BackgroundRegistrar? = null

        fun setOnRegisterCallback(callback: BackgroundRegistrar) {
            registrar = callback
        }

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
            playerSession: MusicPlayerSession
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
            registrar?.invoke(engine)

            val channel = MethodChannel(
                ShimPluginRegistry(engine).registrarFor(MusicPlayerServicePlugin::class.java.name).messenger(),
                NAME
            )

            val helper = MusicPlayerServicePlugin(channel, engine.dartExecutor, playerSession)
            channel.setMethodCallHandler(helper)
            return helper
        }
    }

    init {
        playerSession.addCallback(MusicPlayerCallbackPlugin(methodChannel))
    }


    var config = Config.Default

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (call.method == "updateConfig") {
            config = Config(call.arguments())
            result.success(null)
        } else {
            result.notImplemented()
        }
    }


    private suspend inline fun <reified T> MethodChannel.invokeAsyncCast(
        method: String,
        arguments: Any?,
        noinline onNotImplement: suspend () -> T
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
                invokeAsync(method, arguments, onNotImplement)
            }
        }.onFailure {
            logError(it)
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


    suspend fun getNextMusic(
        queue: PlayQueue,
        current: MusicMetadata?,
        playMode: Int
    ): MusicMetadata? {
        return null
    }

    suspend fun getPreviousMusic(
        queue: PlayQueue,
        current: MusicMetadata?,
        playMode: Int
    ): MusicMetadata? {
        return null
    }

    suspend fun getMusicByMediaId(playQueue: PlayQueue, mediaId: String): MusicMetadata? {
        return null
    }


}