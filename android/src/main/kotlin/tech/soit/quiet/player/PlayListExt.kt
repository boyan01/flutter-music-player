package tech.soit.quiet.player

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat

object PlayListExt {


    private const val COMMAND_UPDATE_PLAYLIST = "updatePlayList"

    val commands = arrayOf(COMMAND_UPDATE_PLAYLIST)


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