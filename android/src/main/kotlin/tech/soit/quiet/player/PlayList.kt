package tech.soit.quiet.player

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import tech.soit.quiet.utils.log
import tech.soit.quiet.utils.sendCommandAsync
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates


/**
 * PlayList of Player
 *
 * change will sync to [android.support.v4.media.session.MediaSessionCompat.setQueue]
 *
 */
class PlayList private constructor(
        private val list: LinkedList<MediaMetadataCompat>,
        private val title: String?,
        private val queueId: String
) {


    companion object {

        const val KEY_PLAYLIST = "tech.soit.quiet.PlayList"
        const val KEY_PLAYLIST_ID = "tech.soit.quiet.QueueId"


        val empty = PlayList(emptyList(), null, "")


        private fun List<MediaMetadataCompat>.index(metadata: MediaMetadataCompat): Int {
            return indexOfFirst {
                it.description.mediaId == metadata.description.mediaId
            }
        }

    }

    constructor(list: List<MediaMetadataCompat>, title: String?, queueId: String)
            : this(LinkedList(list), title, queueId)

    private var observer: PlayListObserver? = null

    var current: MediaMetadataCompat? by Delegates.observable<MediaMetadataCompat?>(null) { _, _, newValue ->
        observer?.sendMetadataChange(newValue)
    }

    var playMode by Delegates.observable(PlayMode.Sequence) { _, _, newValue ->
        observer?.sendPlayModeChange(newValue)
    }

    fun attach(mediaSession: MediaSessionCompat) {
        generateShuffleList() // generate shuffle when attached session
        observer = PlayListObserver(mediaSession)
        publishChange()
        mediaSession.setQueueTitle(title)
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
        observer?.sendQueueChange()
    }

    fun getNext(metadata: MediaMetadataCompat?): MediaMetadataCompat? {
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


    fun getPrevious(metadata: MediaMetadataCompat?): MediaMetadataCompat? {
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

    private inner class PlayListObserver(private val mediaSession: MediaSessionCompat) {

        fun sendQueueChange() {
            val queue = list.mapIndexed { index, metadata ->
                MediaSessionCompat.QueueItem(
                        metadata.description,
                        index.toLong()
                )
            }
            mediaSession.setQueue(queue)
            mediaSession.setExtras(Bundle().apply {
                putParcelableArrayList(KEY_PLAYLIST, ArrayList(list))
                putString(KEY_PLAYLIST_ID, queueId)
            })

        }

        fun sendPlayModeChange(playMode: PlayMode) {
            mediaSession.setShuffleMode(playMode.shuffleMode())
            mediaSession.setRepeatMode(playMode.repeatMode())
        }

        fun sendMetadataChange(metadata: MediaMetadataCompat?) {
            mediaSession.setMetadata(metadata)
        }


    }


}

object PlayListExt {


    private const val COMMAND_UPDATE_PLAYLIST = "updatePlayList"

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