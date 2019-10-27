package tech.soit.quiet.player

import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector


private const val ACTION_SET_PLAY_MODE = "setPlayMode"

/**
 * transport control: change play mode
 */
fun MediaControllerCompat.TransportControls.setPlayMode(playMode: PlayMode) {
    val bundle = Bundle().apply {
        putSerializable("playMode", playMode)
    }
    sendCustomAction(ACTION_SET_PLAY_MODE, bundle)
}


fun PlaybackStateCompat.getPlayMode(): PlayMode {
    var mode = PlayMode.Sequence
    customActions.forEach {
        if (it.action == ACTION_SET_PLAY_MODE) {
            mode = it.extras.getSerializable("playMode") as? PlayMode
                    ?: PlayMode.Sequence
            return@forEach
        }
    }
    return mode
}


/**
 * custom play mode action provide
 *
 * state will be auto attach to [PlaybackStateCompat]
 *
 */
class SetPlayModeActionProvider(private val playModeContainer: PlayModeContainer) : MediaSessionConnector.CustomActionProvider {

    override fun getCustomAction(): PlaybackStateCompat.CustomAction {
        return PlaybackStateCompat.CustomAction.Builder(
                ACTION_SET_PLAY_MODE, playModeContainer.playMode.name, -1 //TODO icon
        ).setExtras(Bundle().apply {
            putSerializable("playMode", playModeContainer.playMode)
        }).build()
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
        val playMode = extras?.getSerializable("playMode") as? PlayMode
        playModeContainer.playMode = playMode ?: PlayMode.Sequence
    }

}