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


class SetPlayModeActionProvider(private val playListProvider: () -> PlayList) : MediaSessionConnector.CustomActionProvider {

    companion object {

        fun getPlayMode(action: PlaybackStateCompat.CustomAction): PlayMode? {
            return if (action.action == ACTION_SET_PLAY_MODE) {
                action.extras.getSerializable("playMode") as? PlayMode
                        ?: PlayMode.Sequence
            } else {
                null
            }
        }

    }

    override fun getCustomAction(): PlaybackStateCompat.CustomAction {
        return PlaybackStateCompat.CustomAction.Builder(
                ACTION_SET_PLAY_MODE, playListProvider().playMode.name, -1 //TODO icon
        ).setExtras(Bundle().apply {
            putSerializable("playMode", playListProvider().playMode)
        }).build()
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
        val playMode = extras?.getSerializable("playMode") as? PlayMode
        playListProvider().playMode = playMode ?: PlayMode.Sequence
    }

}