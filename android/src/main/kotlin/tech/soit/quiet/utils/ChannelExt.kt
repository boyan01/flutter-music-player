package tech.soit.quiet.utils

import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


suspend inline fun <reified T> MethodChannel.invokeAsync(
        method: String,
        arguments: Any?,
        noinline onNotImplement: suspend () -> T
): T = withContext(Dispatchers.Main) {
    suspendCoroutine<T> { continuation ->
        invokeMethod(method, arguments, object : MethodChannel.Result {

            override fun notImplemented() {
                GlobalScope.launch(Dispatchers.Main) {
                    continuation.resume(onNotImplement())
                }
            }

            override fun error(errorCode: String?, errorMessage: String?, errorDetails: Any?) {
//                continuation.resumeWithException(Exception("$errorCode , $errorMessage , $errorDetails"))
                notImplemented()
            }

            override fun success(result: Any?) {
                continuation.resume(result as T)
            }
        })
    }

}