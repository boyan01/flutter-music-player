package tech.soit.quiet.utils

import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.CompletableDeferred
import tech.soit.quiet.player.PlayMode
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


private const val COMMAND_GET_PREVIOUS = "getPrevious"

private const val COMMAND_GET_NEXT = "getNext"

val COMMANDS = arrayOf(
        COMMAND_GET_PREVIOUS,
        COMMAND_GET_NEXT
)



typealias CommandHandle = (player: Player?, extras: Bundle?, receiver: ResultReceiver?) -> Unit


private val getPreviousCommandHandle: CommandHandle = { player, extras, receiver ->
    if (player != null && receiver != null) {
        player.nextWindowIndex
        receiver.send(0, null)
    }

}


val commandHandle = mapOf<String, CommandHandle>(
        COMMAND_GET_PREVIOUS to getPreviousCommandHandle
)


suspend fun MediaControllerCompat.sendCommandAsync(command: String, bundle: Bundle? = null): Pair<Int, Bundle?> {
    val result = CompletableDeferred<Pair<Int, Bundle?>>()
    sendCommand(command, bundle, object : ResultReceiver(null) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            result.complete(resultCode to resultData)
        }
    })
    return result.await()
}


/**
 * get previous media item
 */
suspend fun MediaControllerCompat.getPrevious(): MediaMetadataCompat? {
    val (code, bundle) = sendCommandAsync(COMMAND_GET_PREVIOUS)
    return null
}