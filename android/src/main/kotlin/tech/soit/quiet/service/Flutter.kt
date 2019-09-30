package tech.soit.quiet.service

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterMain
import io.flutter.view.FlutterNativeView
import io.flutter.view.FlutterRunArguments
import tech.soit.quiet.utils.*


class BackgroundCallbackChannel(
    private val methodChannel: MethodChannel
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
        fun startBackgroundIsolate(context: Context): BackgroundCallbackChannel {
            FlutterMain.ensureInitializationComplete(context, null)
            val appBundlePath = FlutterMain.findAppBundlePath()
            val nativeView = FlutterNativeView(context, true)

            val arguments = FlutterRunArguments().apply {
                bundlePath = appBundlePath
                entrypoint = "playerBackgroundService"
                libraryPath = ""
            }
            nativeView.runFromBundle(arguments)
            val channel = MethodChannel(
                nativeView.pluginRegistry.registrarFor(BackgroundCallbackChannel::class.java.name).messenger(),
                NAME
            )
            val helper = BackgroundCallbackChannel(channel)
            channel.setMethodCallHandler(helper)
            return helper
        }
    }


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        result.notImplemented()
    }


    suspend fun loadImage(description: MediaDescriptionCompat, uri: Uri): Artwork? {
        val bytes = methodChannel.invokeAsyncCast("loadImage", description.toMap()) {
            loadArtworkFromUri(uri)
        } ?: return null
        return createArtworkFromByteArray(bytes)
    }


}