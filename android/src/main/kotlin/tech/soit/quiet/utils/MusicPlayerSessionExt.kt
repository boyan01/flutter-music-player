package tech.soit.quiet.utils

import kotlinx.coroutines.CompletableDeferred
import tech.soit.quiet.MusicPlayerSession
import tech.soit.quiet.MusicResult
import tech.soit.quiet.player.MusicMetadata


private class FutureMusicResult : MusicResult.Stub() {

    private val future = CompletableDeferred<MusicMetadata?>()

    override fun onResult(metadata: MusicMetadata?) {
        future.complete(metadata)
    }

    suspend fun await(): MusicMetadata? {
        return future.await()
    }

}

suspend fun MusicPlayerSession.getNext(anchor: MusicMetadata?): MusicMetadata? {
    val result = FutureMusicResult()
    getNext(anchor, result)
    return result.await()
}


suspend fun MusicPlayerSession.getPrevious(anchor: MusicMetadata?): MusicMetadata? {
    return FutureMusicResult().apply { getPrevious(anchor, this) }.await()
}