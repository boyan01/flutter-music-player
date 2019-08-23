package tech.soit.quiet.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat

/**
 * listening for when headphones are unplugged ( or the audio will
 * otherwise case playback to become 'noisy' ).
 */
class BecomingNoisyReceiver(
        private val context: Context,
        token: MediaSessionCompat.Token
) : BroadcastReceiver() {
    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val controller = MediaControllerCompat(context, token)

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
            controller.transportControls.pause()
        }
    }
}