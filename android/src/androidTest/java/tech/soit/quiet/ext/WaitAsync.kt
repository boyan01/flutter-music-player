package tech.soit.quiet.ext

import kotlinx.coroutines.*
import java.lang.Exception
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class WaitLock internal constructor(private val countDownLatch: CountDownLatch) {

    fun unlock() {
        countDownLatch.countDown()
    }

}

/**
 * wait until all [lockCount] of [WaitLock] has been unlocked
 *
 * timeout : 5 seconds
 */
suspend fun waitAsync(lockCount: Int = 1, msg: String? = "", callback: (lock: WaitLock) -> Unit) =
    GlobalScope.launch {
        val latch = CountDownLatch(lockCount)
        val lock = WaitLock(latch)

        launch(Dispatchers.Main) {
            callback.invoke(lock)
        }.join()

        if (!latch.await(5, TimeUnit.SECONDS)) {
            launch(Dispatchers.Main) { throw RuntimeException("wait failed, msg : $msg") }
        }
    }.join()


/**
 * wait until predicate passed, verify [predicate] every 20 millis
 */
suspend fun waitUntil(msg: String? = null, predicate: () -> Boolean) {
    try {
        withTimeout(3000) {
            GlobalScope.launch {
                while (!predicate()) {
                    delay(20)
                }
            }.join()
        }
    } catch (e: Exception) {
        throw RuntimeException("wait failed : $msg", e)
    }
}