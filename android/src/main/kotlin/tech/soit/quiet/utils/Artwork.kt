package tech.soit.quiet.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.net.HttpURLConnection
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val MEMORY_CACHE_SIZE = 20 * 1024 * 1024 // 20MB

object ArtworkCache : LruCache<Uri, Artwork>(MEMORY_CACHE_SIZE) {


    override fun sizeOf(key: Uri?, value: Artwork): Int {
        return value.bitmap.byteCount
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


private val pendingImages = hashMapOf<Uri, Deferred<Artwork?>>()

suspend fun loadArtworkFromUri(uri: Uri): Artwork? {
    val image = pendingImages.getOrPut(uri) {
        GlobalScope.async(Dispatchers.IO) {
            //TODO resize bitmap
            runCatching { loadImageFormUriInternal(uri) }.getOrNull()?.let {
                createArtwork(it)
            }
        }
    }
    return image.await()
}

private suspend fun loadImageFormUriInternal(uri: Uri): Bitmap = suspendCoroutine { continuation ->
    val urlConnection = URI.create(uri.toString()).toURL().openConnection() as HttpURLConnection
    urlConnection.connect()

    val bitmap = urlConnection.inputStream.use { stream ->
        val bytes = stream.readBytes()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    Thread.sleep(4000)
    continuation.resume(bitmap)

}
