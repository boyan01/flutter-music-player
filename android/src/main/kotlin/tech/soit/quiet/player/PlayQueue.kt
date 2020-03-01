package tech.soit.quiet.player

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.UiThread
import tech.soit.quiet.utils.log


typealias MusicItem = MusicMetadata

private typealias DartObject = Map<String, Any?>

private typealias OnQueueChanged = () -> Unit

class PlayQueue(
    val queueId: String,
    val queueTitle: String,
    queue: List<MusicItem>,
    private val extras: DartObject?,
    shuffleQueue: List<String>?
) : Parcelable {

    companion object {

        val Empty = PlayQueue(
            queueId = "",
            queueTitle = "",
            queue = emptyList(),
            extras = null,
            shuffleQueue = null
        )

        private fun List<MusicItem>.index(metadata: MusicItem): Int {
            return indexOfFirst { it.mediaId == metadata.mediaId }
        }

        @JvmField
        val CREATOR: Parcelable.Creator<PlayQueue> =
            object : Parcelable.Creator<PlayQueue> {
                override fun createFromParcel(source: Parcel): PlayQueue = PlayQueue(source)
                override fun newArray(size: Int): Array<PlayQueue?> = arrayOfNulls(size)
            }
    }

    private val queue: MutableList<MusicItem> = queue.toMutableList()

    private val shuffleMusicList = ArrayList<String>()

    var onQueueChanged: OnQueueChanged? = null

    init {
        if (shuffleQueue == null) {
            generateShuffleList()
        } else {
            require(shuffleQueue.size == queue.size) {
                "shuffle queue size (${shuffleQueue.size}) is not compat with queue size (${queue.size})"
            }
            shuffleMusicList.addAll(shuffleQueue)
        }
    }

    @Suppress("UNCHECKED_CAST")
    constructor(obj: DartObject) : this(
        obj["queueId"].toString(), obj["queueTitle"].toString(),
        (obj["queue"] as List<Map<String, Any?>>).map { MusicItem(it) },
        obj["extras"] as DartObject?,
        obj["shuffleQueue"] as List<String>?
    )

    @UiThread
    fun add(anchorMediaId: String?, music: MusicItem) {
        val index = queue.indexOfFirst { it.mediaId == anchorMediaId } + 1
        queue.add(index, music)
        val i = shuffleMusicList.indexOf(anchorMediaId) + 1
        shuffleMusicList.add(i, music.mediaId)
    }

    @UiThread
    fun remove(mediaId: String) {
        queue.removeAll { it.mediaId == mediaId }
        shuffleMusicList.removeAll { it == mediaId }
    }

    fun insert(index: Int, list: List<MusicMetadata>) {
        if (list.isEmpty()) return
        queue.addAll(index, list)
        shuffleMusicList.addAll(list.shuffled().map { it.mediaId })
    }

    @UiThread
    fun getNext(current: MusicItem?, playMode: PlayMode): MusicItem? {
        return getMusicInternal(current, playMode, true)
    }

    @UiThread
    fun getPrevious(current: MusicItem?, playMode: PlayMode): MusicItem? {
        return getMusicInternal(current, playMode, false)
    }

    fun getByMediaId(mediaId: String): MusicItem? {
        return requireMusicItem(mediaId)
    }

    fun getQueue(): List<MusicItem> = queue

    private fun getMusicInternal(
        anchor: MusicItem?,
        playMode: PlayMode,
        next: Boolean
    ): MusicItem? {
        if (queue.isEmpty()) {
            log { "empty playlist" }
            return null
        }
        // fast return
        if (anchor == null) {
            val index = if (next) 0 else queue.size - 1
            return if (playMode == PlayMode.Shuffle) requireMusicItem(shuffleMusicList[index]) else queue[index]
        }
        return when (playMode) {
            PlayMode.Single, PlayMode.Sequence -> {
                val index = queue.index(anchor) + if (next) 1 else -1
                if (index >= queue.size || index <= -1) {
                    null
                } else {
                    queue[index]
                }
            }
            PlayMode.Shuffle -> {
                val index = shuffleMusicList.indexOf(anchor.mediaId) + if (next) 1 else -1
                if (index >= queue.size || index < 0) {
                    return null
                } else {
                    requireMusicItem(shuffleMusicList[index])
                }
            }
            is PlayMode.Undefined -> null /*playMode undefined,we do not know the play sequence*/
        }
    }


    private fun requireMusicItem(mediaId: String): MusicItem? {
        return queue.firstOrNull { it.mediaId == mediaId }
    }

    @UiThread
    fun generateShuffleList() {
        val list = queue.map { it.mediaId }.toMutableList()
        var position = list.size - 1
        while (position > 0) {
            // generate [random] index
            val random = (Math.random() * (position + 1)).toInt()
            // swap [random] and [position]
            val temp = list[position]
            list[position] = list[random]
            list[random] = temp
            position--
        }
        shuffleMusicList.clear()
        shuffleMusicList.addAll(list)

        // notify queue changed
        onQueueChanged?.invoke()
    }


    fun toDartMapObject(): DartObject {
        return mapOf(
            "extras" to extras,
            "queueId" to queueId,
            "queueTitle" to queueTitle,
            "queue" to queue.map { it.obj },
            "shuffleQueue" to shuffleMusicList
        )
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(queueId)
        writeString(queueTitle)
        writeList(queue)
        writeMap(extras)
        writeList(shuffleMusicList)
    }

    constructor(parcel: Parcel) : this(
        requireNotNull(parcel.readString()),
        requireNotNull(parcel.readString()),
        mutableListOf<MusicItem>().apply {
            parcel.readList(this, PlayQueue::class.java.classLoader)
        },
        mutableMapOf<String, Any>().apply {
            parcel.readMap(
                this,
                PlayQueue::class.java.classLoader
            )
        },
        mutableListOf<String>().apply { parcel.readList(this, PlayQueue::class.java.classLoader) }
    )

}