package tech.soit.quiet.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class WaitLock internal constructor(private val countDownLatch: CountDownLatch) {

    fun unlock() {
        countDownLatch.countDown()
    }

}

suspend fun waitAsync(count: Int = 1, callback: (lock: WaitLock) -> Unit) = GlobalScope.launch {


    val latch = CountDownLatch(count)
    val lock = WaitLock(latch)

    launch(Dispatchers.Main) {
        callback.invoke(lock)
    }.join()
    if (!latch.await(5, TimeUnit.SECONDS)) {
        throw RuntimeException("wait failed")
    }
}.join()