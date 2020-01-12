package tech.soit.quiet.service

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import tech.soit.quiet.MusicPlayerSession
import tech.soit.quiet.utils.log

internal class QuietPlaybackController(private val musicPlayerSession: MusicPlayerSession) :
        DefaultPlaybackController() {


    override fun onSetRepeatMode(player: Player, repeatMode: Int) {
        musicPlayerSession.setPlayMode(repeatMode)
    }

    override fun onSetShuffleMode(player: Player, shuffleMode: Int) {
        musicPlayerSession.setPlayMode(shuffleMode)
    }

}

internal class QuietPlaybackPreparer(private val musicPlayerSession: MusicPlayerSession) : MediaSessionConnector.PlaybackPreparer {

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) = Unit

    override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) = Unit

    // only support play from media id
    override fun getSupportedPrepareActions(): Long = PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID

    override fun getCommands(): Array<String> = emptyArray()

    override fun onPrepareFromMediaId(mediaId: String, extras: Bundle?) {
        musicPlayerSession.playFromMediaId(mediaId)
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) = Unit

    override fun onPrepare() = Unit

}
