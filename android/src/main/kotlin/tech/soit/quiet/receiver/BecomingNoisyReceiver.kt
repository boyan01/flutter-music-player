package tech.soit.quiet.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import tech.soit.quiet.MusicPlayerSession
import tech.soit.quiet.player.*


class BecomingNoisyReceiverAdapter(
    context: Context,
    private val playerSession: MusicPlayerSessionImpl
) : BaseMusicSessionCallback() {


    private val receiver = BecomingNoisyReceiver(context, playerSession)

    override fun onPlaybackStateChanged(state: PlaybackState) {
        checkShouldRegisterReceiver()
    }


    override fun onMetadataChanged(metadata: MusicMetadata?) {
        checkShouldRegisterReceiver()
    }

    private fun checkShouldRegisterReceiver() {
        when (playerSession.playbackState.state) {
            State.Buffering, State.Playing -> receiver.register()
            else -> receiver.unregister()
        }
    }


}

/**
 * listening for when headphones are unplugged ( or the audio will
 * otherwise case playback to become 'noisy' ).
 */
class BecomingNoisyReceiver(
    private val context: Context,
    private val playerSession: MusicPlayerSession
) : BroadcastReceiver() {

    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    private var registered = false

    fun register() {
        if (!registered) {
            context.registerReceiver(this, intentFilter)
            registered = true
        }
    }


    fun unregister() {
        if (registered) {
            context.unregisterReceiver(this)
            registered = false
        }
    }


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            playerSession.pause()
        }
    }
}