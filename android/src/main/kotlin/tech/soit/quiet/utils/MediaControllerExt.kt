package tech.soit.quiet.utils

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


private suspend fun MediaBrowserCompat.sendCustomActionAsync(
    action: String,
    extras: Bundle
) = suspendCoroutine<Bundle?> {
    sendCustomAction(action, extras, object : MediaBrowserCompat.CustomActionCallback() {
        override fun onResult(action: String, extras: Bundle, resultData: Bundle?) {
            it.resume(resultData)
        }

        override fun onError(action: String?, extras: Bundle?, data: Bundle?) {
            //TODO handle exception
            it.resume(data)
        }
    })
}