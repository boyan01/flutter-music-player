package tech.soit.quiet.service

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
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
 * Test rule for [MusicPlayerService]
 *
 * @param timeout timeout time in milliseconds.
 */
class MusicPlayerServiceTestRule @JvmOverloads constructor(
    private val timeout: Long = DEFAULT_TIMEOUT
) : TestRule {

    companion object {
        private const val DEFAULT_TIMEOUT = 5000L // 5 seconds
    }

    private lateinit var _mediaBrowser: MediaBrowserCompat

    private lateinit var _mediaController: MediaControllerCompat

    val mediaBrowser get() = _mediaBrowser

    val mediaController get() = _mediaController

    private var serviceConnected = false

    /**
     * Connect to a [MediaBrowserServiceCompat], return [MediaBrowserCompat] if succeed.
     *
     * @param serviceComponent component name of [MediaBrowserServiceCompat]
     */
    private fun connect(serviceComponent: ComponentName): MediaBrowserCompat =
        runBlocking(context = Dispatchers.Main) {
            val context = ApplicationProvider.getApplicationContext<Context>()

            suspend fun connect() =
                suspendCancellableCoroutine<MediaBrowserCompat> { continuation ->
                    _mediaBrowser = MediaBrowserCompat(
                        context,
                        serviceComponent,
                        object : MediaBrowserCompat.ConnectionCallback() {
                            override fun onConnected() {
                                serviceConnected = true
                                continuation.resume(_mediaBrowser)
                            }

                            override fun onConnectionFailed() {
                                continuation.resumeWithException(Exception("connect to media service ($serviceComponent) failed"))
                            }

                        },
                        null
                    )
                    _mediaBrowser.connect()
                }


            return@runBlocking withTimeout(timeout) {
                connect()
            }
        }


    private fun connectService() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        _mediaBrowser = connect(
            ComponentName(
                context,
                MusicPlayerService::class.java
            )
        )
        _mediaController = MediaControllerCompat(context, _mediaBrowser.sessionToken)
    }

    private fun shutdownService() {
        if (serviceConnected) {
            _mediaBrowser.disconnect()
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
                connectService()
                base.evaluate()
            } finally {
                shutdownService()
            }
        }
    }

}