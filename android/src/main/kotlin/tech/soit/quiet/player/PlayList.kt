package tech.soit.quiet.player

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import tech.soit.quiet.service.MusicPlayerService
import tech.soit.quiet.utils.LoggerLevel
import tech.soit.quiet.utils.log
import tech.soit.quiet.utils.mediaId
import java.util.*
import kotlin.collections.ArrayList


/**
 * PlayList of Player
 *
 * change will sync to [android.support.v4.media.session.MediaSessionCompat.setQueue]
 *
 */
class PlayList private constructor(
        private val list: LinkedList<MediaMetadataCompat>,
        val title: String?,
        val queueId: String
) {


    companion object {

        private const val prefix = "tech.soit.quiet"

        const val KEY_QUEUE = "$prefix.Queue"
        const val KEY_QUEUE_SHUFFLE = "$prefix.QueueShuffle"

        const val KEY_QUEUE_ID = "$prefix.QueueId"
        const val KEY_QUEUE_TITLE = "$prefix.QueueTitle"

        val empty = PlayList(emptyList(), null, "")


        private fun List<MediaMetadataCompat>.index(metadata: MediaMetadataCompat): Int {
            return indexOfFirst {
                it.description.mediaId == metadata.description.mediaId
            }
        }

    }

    constructor(list: List<MediaMetadataCompat>, title: String?, queueId: String)
            : this(LinkedList(list), title, queueId)

    private var service: MusicPlayerService? = null

    val current: MediaMetadataCompat?
        get() = service?.mediaSession?.controller?.metadata

    val playMode: PlayMode
        get() {
            if (service == null) {
                log(LoggerLevel.ERROR) { "service is null" }
            }
            return service?.playMode ?: PlayMode.Sequence
        }

    fun attach(service: MusicPlayerService) {
        this.service = service
        generateShuffleList() // generate shuffle when attached session
        publishChange()
    }


    val queue: List<MediaMetadataCompat> get() = list


    val isEmpty get() = list.isEmpty()


    fun getQueueID(metadata: MediaMetadataCompat): Long {
        return queue.index(metadata).toLong()
    }


    fun getMetadataByQueueId(queueId: Long): MediaMetadataCompat? {
        return queue.getOrNull(queueId.toInt())
    }

    fun findMetadataByMediaId(mediaId: String): MediaMetadataCompat? {
        return queue.firstOrNull {
            it.description.mediaId == mediaId
        }
    }


    fun addItem(item: MediaMetadataCompat, index: Int = list.size) {
        list.add(index, item)
        generateShuffleList()
        publishChange()
    }

    private fun publishChange() {
        service?.mediaSession?.let { mediaSession ->
            val queue = list.mapIndexed { index, metadata ->
                MediaSessionCompat.QueueItem(
                        metadata.description,
                        index.toLong()
                )
            }
            mediaSession.setQueue(queue)
            mediaSession.setExtras(Bundle().apply {
                putParcelableArrayList(KEY_QUEUE, ArrayList(list))
                putString(KEY_QUEUE_ID, queueId)
                putString(KEY_QUEUE_TITLE, title)
                putStringArrayList(KEY_QUEUE_SHUFFLE, ArrayList(shuffleMusicList.map { it.mediaId }))
            })
            mediaSession.setQueueTitle(title)
        }
    }

    fun getNext(metadata: MediaMetadataCompat?, playMode: PlayMode = this.playMode): MediaMetadataCompat? {
        if (list.isEmpty()) {
            log { "empty playlist" }
            return null
        }
        val anchor = metadata ?: /*fast return */ return list[0]
        return when (playMode) {
            PlayMode.Single -> {
                anchor
            }
            PlayMode.Sequence -> {
                //if can not find ,index will be zero , it will right too
                val index = list.index(anchor) + 1
                if (index == list.size) {
                    list[0]
                } else {
                    list[index]
                }
            }
            PlayMode.Shuffle -> {
                when (val index = shuffleMusicList.index(anchor)) {
                    -1 -> list[0]
                    list.size - 1 -> {
                        generateShuffleList()
                        shuffleMusicList[0]
                    }
                    else -> shuffleMusicList[index + 1]
                }
            }
        }
    }


    fun getPrevious(metadata: MediaMetadataCompat?, playMode: PlayMode = this.playMode): MediaMetadataCompat? {
        if (list.isEmpty()) {
            log { "try too play next with empty playlist!" }
            return null
        }
        val anchor = metadata ?: return list[0]
        return when (playMode) {
            PlayMode.Single -> {
                anchor
            }
            PlayMode.Sequence -> {
                when (val index = list.index(anchor)) {
                    -1 -> list[0]
                    0 -> list[list.size - 1]
                    else -> list[index - 1]
                }
            }
            PlayMode.Shuffle -> {
                when (val index = shuffleMusicList.index(anchor)) {
                    -1 -> list[0]
                    0 -> {
                        generateShuffleList()
                        shuffleMusicList[shuffleMusicList.size - 1]
                    }
                    else -> shuffleMusicList[index - 1]
                }
            }
        }
    }


    private val shuffleMusicList = ArrayList<MediaMetadataCompat>()

    /**
     * create shuffle list for [PlayMode.Shuffle]
     */
    private fun generateShuffleList() {
        val list = ArrayList(list)
        var position = list.size - 1
        while (position > 0) {
            //生成一个随机数
            val random = (Math.random() * (position + 1)).toInt()
            //将random和position两个元素交换
            val temp = list[position]
            list[position] = list[random]
            list[random] = temp
            position--
        }
        shuffleMusicList.clear()
        shuffleMusicList.addAll(list)
    }


}

object PlayListExt {


    const val COMMAND_UPDATE_PLAYLIST = "updatePlayList"

    private const val COMMAND_GET_PLAYLIST = "getPlayList"

    val commands = arrayOf(COMMAND_UPDATE_PLAYLIST, COMMAND_GET_PLAYLIST)


    fun updatePlayList(
            controller: MediaControllerCompat,
            queue: List<MediaMetadataCompat>,
            title: String?,
            queueId: String) {
        val data = Bundle().apply {
            putParcelableArrayList("queue", ArrayList(queue))
            putString("title", title)
            putString("queueId", queueId)
        }
        controller.sendCommand(COMMAND_UPDATE_PLAYLIST, data, null)
    }


    fun parsePlayListFromArgument(bundle: Bundle): PlayList {
        return PlayList(
                list = bundle.getParcelableArrayList("queue")!!,
                title = bundle.getString("title"),
                queueId = bundle.getString("queueId")!!
        )
    }


}