package tech.soit.quiet.utils

import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


suspend fun MethodChannel.invokeAsync(
    method: String,
    arguments: Any?,
    onNotImplement: suspend () -> Any?
) = suspendCoroutine<Any?> { continuation ->
    invokeMethod(method, arguments, object : MethodChannel.Result {

        override fun notImplemented() {
            GlobalScope.launch {
                continuation.resume(onNotImplement())
            }
        }

        override fun error(errorCode: String?, errorMessage: String?, errorDetails: Any?) {
            GlobalScope.launch {
                continuation.resume(onNotImplement())
            }
        }

        override fun success(result: Any?) {
            continuation.resume(result)
        }
    })
}

suspend inline fun <reified T> MethodChannel.invokeAsyncCast(
    method: String,
    arguments: Any?,
    noinline onNotImplement: suspend () -> T
): T {
    return invokeAsync(method, arguments, onNotImplement) as T
}