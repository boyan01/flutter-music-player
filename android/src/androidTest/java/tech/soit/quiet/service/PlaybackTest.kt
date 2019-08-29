package tech.soit.quiet.service

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.soit.quiet.ext.waitUntil
import tech.soit.quiet.model.bamboo
import tech.soit.quiet.model.hide
import tech.soit.quiet.model.rise

@RunWith(AndroidJUnit4::class)
class PlaybackTest {

    @get:Rule
    val playerService = MusicPlayerServiceTestRule()


    private val controller get() = playerService.mediaController


    private suspend fun verifyPlayingStateAsync(
        mediaMetadata: MediaMetadataCompat,
        state: Int
    ) {
        waitUntil(
            msg = """
            current playing should be ${mediaMetadata.description}  state should be $state
        """.trimIndent()
        ) {
            controller.metadata?.description?.mediaId == mediaMetadata.description.mediaId
                    && state == controller.playbackState.state
        }
        Assert.assertEquals(
            "current playing media",
            controller.metadata?.description?.mediaId,
            mediaMetadata.description.mediaId
        )
        Assert.assertEquals(
            "current player state",
            state,
            controller.playbackState.state
        )
    }


    private val playList = arrayListOf(bamboo, hide, rise)

    private val playListExtras = Bundle().apply {
        putParcelableArrayList("queue", playList)
        putString("queueTitle", "Testing")
    }

    @Test
    fun testPlaySingle() = runBlocking {

        controller.transportControls.playFromMediaId(bamboo.description.mediaId, playListExtras)
        verifyPlayingStateAsync(playList[0], PlaybackStateCompat.STATE_PLAYING)

        controller.transportControls.pause()
        verifyPlayingStateAsync(playList[0], PlaybackStateCompat.STATE_PAUSED)
    }


    @Test
    fun testSkipToNext() = runBlocking {

        val transportControls = controller.transportControls
        transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
        transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)

        transportControls.playFromMediaId(playList[0].description.mediaId, playListExtras)
        verifyPlayingStateAsync(playList[0], PlaybackStateCompat.STATE_PLAYING)

        transportControls.skipToNext()
        verifyPlayingStateAsync(playList[1], PlaybackStateCompat.STATE_PLAYING)
    }


    @Test
    fun testSkipToNextAtLastPosition() = runBlocking {
        val transportControls = controller.transportControls
        transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
        transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
        waitUntil("shuffle mode has been set") {
            controller.repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL
                    && controller.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE
        }

        transportControls.playFromMediaId(playList.last().description.mediaId, playListExtras)
        verifyPlayingStateAsync(playList.last(), PlaybackStateCompat.STATE_PLAYING)

        transportControls.skipToNext()
        verifyPlayingStateAsync(playList.first(), PlaybackStateCompat.STATE_PLAYING)

    }


    @Test
    fun testSkipToPrevious() = runBlocking {
        val transportControls = controller.transportControls
        transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
        transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)

        transportControls.playFromMediaId(playList[1].description.mediaId, playListExtras)
        verifyPlayingStateAsync(playList[1], PlaybackStateCompat.STATE_PLAYING)

        transportControls.skipToPrevious()
        verifyPlayingStateAsync(playList[0], PlaybackStateCompat.STATE_PLAYING)

    }

    fun testSkipToPreviousAtFirstPosition() = runBlocking {
        val transportControls = controller.transportControls
        transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
        transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)

        transportControls.playFromMediaId(playList.first().description.mediaId, playListExtras)
        verifyPlayingStateAsync(playList.first(), PlaybackStateCompat.STATE_PLAYING)

        transportControls.skipToNext()
        verifyPlayingStateAsync(playList.last(), PlaybackStateCompat.STATE_PLAYING)
    }

}