package tech.soit.quiet.service

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Test rule for [MediaBrowserServiceCompat]
 *
 * @param timeout timeout time in milliseconds.
 */
class MediaServiceTestRule @JvmOverloads constructor(
    private val timeout: Long = DEFAULT_TIMEOUT
) : TestRule {

    companion object {
        private const val DEFAULT_TIMEOUT = 5000L // 5 seconds
    }

    private lateinit var mediaBrowser: MediaBrowserCompat

    private var serviceConnected = false

    /**
     * Connect to a [MediaBrowserServiceCompat], return [MediaBrowserCompat] if succeed.
     *
     * @param serviceComponent component name of [MediaBrowserServiceCompat]
     */
    fun connect(serviceComponent: ComponentName): MediaBrowserCompat =
        runBlocking(context = Dispatchers.Main) {
            val context = ApplicationProvider.getApplicationContext<Context>()

            suspend fun connect() =
                suspendCancellableCoroutine<MediaBrowserCompat> { continuation ->
                    mediaBrowser = MediaBrowserCompat(
                        context,
                        serviceComponent,
                        object : MediaBrowserCompat.ConnectionCallback() {
                            override fun onConnected() {
                                serviceConnected = true
                                continuation.resume(mediaBrowser)
                            }

                            override fun onConnectionFailed() {
                                continuation.resumeWithException(Exception("connect to media service ($serviceComponent) failed"))
                            }

                        },
                        null
                    )
                    mediaBrowser.connect()
                }


            return@runBlocking withTimeout(timeout) {
                connect()
            }
        }


    private fun shutdownService() {
        if (serviceConnected) {
            mediaBrowser.disconnect()
        }
    }


    override fun apply(base: Statement, description: Description?): Statement {
        return ServiceStatement(base)
    }

    private inner class ServiceStatement(
        private val base: Statement
    ) : Statement() {

        override fun evaluate() {
            try {
                base.evaluate()
            } finally {
                shutdownService()
            }
        }
    }

}