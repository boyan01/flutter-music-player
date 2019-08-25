package tech.soit.quiet.service

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class MusicPlayerServiceTest {

    @get:Rule
    val serverRule = MediaServiceTestRule()

    @Before
    fun setUp() {
        val browserCompat = serverRule.connect(
            ComponentName(
                ApplicationProvider.getApplicationContext(),
                MusicPlayerService::class.java
            )
        )
        println(browserCompat)
    }

    @After
    fun tearDown() {

    }


    @Test
    fun hello() {
        println("Hello World")
    }


}