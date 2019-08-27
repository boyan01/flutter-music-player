package tech.soit.quiet.service

import android.support.v4.media.MediaBrowserCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.soit.quiet.model.bamboo
import tech.soit.quiet.model.rise
import tech.soit.quiet.utils.setPlaylist
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayListTest {

    @get:Rule
    val playerService = MusicPlayerServiceTestRule()

    @Test
    fun testSetPlayList() = runBlocking {
        val countDownLatch = CountDownLatch(1)
        val playlist = listOf(
            bamboo,
            rise
        )
        val subscription = object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                if (children == playlist) {
                    countDownLatch.countDown()
                }
            }
        }
        val mediaBrowser = playerService.mediaBrowser
        mediaBrowser.subscribe(mediaBrowser.root, subscription)
        mediaBrowser.setPlaylist(playlist)
        countDownLatch.await(2, TimeUnit.SECONDS)
        mediaBrowser.unsubscribe(mediaBrowser.root, subscription)
    }

}