package tech.soit.quiet.service

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.soit.quiet.utils.setPlaylist
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@MediumTest
class MusicPlayerServiceTest {

    @get:Rule
    val serverRule = MediaServiceTestRule()

    private lateinit var mediaController: MediaControllerCompat

    private lateinit var mediaBrowser: MediaBrowserCompat

    private val transportControls get() = mediaController.transportControls

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        mediaBrowser = serverRule.connect(
            ComponentName(
                context,
                MusicPlayerService::class.java
            )
        )
        mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken)
    }

    @After
    fun tearDown() {

    }

    @Test
    fun testSetPlayList() = runBlocking {
        val countDownLatch = CountDownLatch(1)
        val playlist = listOf<MediaDescriptionCompat>(
            MediaDescriptionCompat.Builder()
                .setTitle("hello")
                .setMediaId("hello")
                .setSubtitle("hello")
                .build()
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
        mediaBrowser.subscribe(mediaBrowser.root, subscription)
        mediaBrowser.setPlaylist(playlist)
        countDownLatch.await(2, TimeUnit.SECONDS)
        mediaBrowser.unsubscribe(mediaBrowser.root, subscription)
    }

}