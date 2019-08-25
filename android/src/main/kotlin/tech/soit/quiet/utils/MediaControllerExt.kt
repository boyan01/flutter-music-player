package tech.soit.quiet.utils

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import tech.soit.quiet.service.MusicPlayerService
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val functionSetPlaylist = "setPlaylist"

/**
 * change current playing list
 */
suspend fun MediaBrowserCompat.setPlaylist(items: List<MediaDescriptionCompat>) {
    val bundle = Bundle(1)
    bundle.putParcelableArrayList("playlist", ArrayList(items))
    sendCustomActionAsync(functionSetPlaylist, bundle)
}


private fun MusicPlayerService.handleSetPlaylist(
    extras: Bundle?,
    result: MediaBrowserServiceCompat.Result<Bundle>
) {
    if (extras == null) {
        //TODO more error description
        result.sendError(null)
        return
    }
    val medias = extras.getParcelableArrayList<MediaDescriptionCompat>("playlist")
    setPlaylist(medias)
    result.sendResult(null)
}


fun MusicPlayerService.handleOnCustomAction(
    action: String,
    extras: Bundle?,
    result: MediaBrowserServiceCompat.Result<Bundle>
) {
    when (action) {
        functionSetPlaylist -> handleSetPlaylist(extras, result)
    }

}


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