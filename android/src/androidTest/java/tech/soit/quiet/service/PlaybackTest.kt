package tech.soit.quiet.service

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.soit.quiet.ext.waitAsync
import tech.soit.quiet.model.bamboo
import tech.soit.quiet.utils.setPlaylist

@RunWith(AndroidJUnit4::class)
class PlaybackTest {

    @get:Rule
    val playerService = MusicPlayerServiceTestRule()


    private val controller get() = playerService.mediaController

    @Test
    fun testPlaySong() = runBlocking(Dispatchers.Main) {
        playerService.mediaBrowser.setPlaylist(listOf(bamboo))


        waitAsync(2) {
            controller.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                    val playingItem = metadata?.description
                    if (playingItem?.mediaId == bamboo.description.mediaId) {
                        it.unlock()
                    }
                }

                override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                    if (state.state == PlaybackStateCompat.STATE_PLAYING) {
                        it.unlock()
                    }
                }

            })
            controller.transportControls.playFromMediaId(bamboo.description.mediaId, null)
        }

        waitAsync {
            controller.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                    if (state.state == PlaybackStateCompat.STATE_PAUSED) {
                        it.unlock()
                        controller.unregisterCallback(this)
                    }
                }
            })
            controller.transportControls.pause()
        }

    }


}