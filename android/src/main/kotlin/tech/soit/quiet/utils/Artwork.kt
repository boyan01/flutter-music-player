package tech.soit.quiet.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.annotation.WorkerThread
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import kotlinx.coroutines.*
import tech.soit.quiet.player.MusicMetadata
import java.net.HttpURLConnection
import java.net.URI

private const val MEMORY_CACHE_SIZE = 40 * 1024 * 1024 // 20MB

/**
 * key : hash(mediaId, iconUri)
 * value: artwork
 */
object ArtworkCache : LruCache<Int, Artwork>(MEMORY_CACHE_SIZE) {

    fun key(metadata: MusicMetadata): Int? {
        return metadata.iconUri?.hashCode() ?: metadata.mediaId.hashCode()
    }
    override fun sizeOf(key: Int?, value: Artwork?): Int {
        return value?.bitmap?.byteCount ?: 0
    }

}

class Artwork(val color: Int?, val bitmap: Bitmap)


private val targetList = listOf(
    Target.DARK_MUTED,
    Target.LIGHT_MUTED,
    Target.DARK_VIBRANT,
    Target.LIGHT_VIBRANT
)

private fun Palette.getAvailableSwatch(): Palette.Swatch? {
    targetList.forEach {
        val swatch = getSwatchForTarget(it)
        if (swatch != null) return swatch
    }
    return null
}

private fun createArtwork(bitmap: Bitmap): Artwork {
    val generate = Palette.from(bitmap).generate()
    val color = generate.getAvailableSwatch()?.rgb
    return Artwork(color, bitmap)
}


private val pendingUriRequests = hashMapOf<Uri, Deferred<ByteArray?>>()


suspend fun createArtworkFromByteArray(byteArray: ByteArray): Artwork? =
    withContext(Dispatchers.IO) {
        return@withContext runCatching {
            //TODO resize bitmap
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            createArtwork(bitmap)
        }.getOrNull()
    }

suspend fun loadArtworkFromUri(uri: Uri): ByteArray? {
    val image = pendingUriRequests.getOrPut(uri) {
        GlobalScope.async(Dispatchers.IO) {
            runCatching { loadDataFormUriInternal(uri) }.onFailure { logError(it) }.getOrNull()
        }
    }
    return image.await()
}


@WorkerThread
private fun loadDataFormUriInternal(uri: Uri): ByteArray? {
    val urlConnection = URI.create(uri.toString()).toURL().openConnection() as HttpURLConnection
    urlConnection.connect()
    val bytes = urlConnection.inputStream.use { stream ->
        stream.readBytes()
    }
    urlConnection.disconnect()
    return bytes
}
