package tech.soit.quiet.player

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

data class PlaybackState constructor(
    val state: State,
    val position: Long,
    val bufferedPosition: Long,
    val speed: Float,
    val error: PlayerError?,
    val updateTime: Long
) : Parcelable {
    constructor(source: Parcel) : this(
        State.values()[source.readInt()],
        source.readLong(),
        source.readLong(),
        source.readFloat(),
        source.readParcelable<PlayerError>(PlayerError::class.java.classLoader),
        source.readLong()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "state" to state.ordinal,
            "position" to position,
            "bufferedPosition" to bufferedPosition,
            "speed" to speed,
            "error" to null,
            "updateTime" to updateTime
        )
    }


    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(state.ordinal)
        writeLong(position)
        writeLong(bufferedPosition)
        writeFloat(speed)
        writeParcelable(error, 0)
        writeLong(updateTime)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PlaybackState> =
            object : Parcelable.Creator<PlaybackState> {
                override fun createFromParcel(source: Parcel): PlaybackState = PlaybackState(source)
                override fun newArray(size: Int): Array<PlaybackState?> = arrayOfNulls(size)
            }
    }
}


enum class State {
    /**
     * The default playback state and indicated that no media has been added yet,
     * or the performer has been reset and has no content to play
     */
    None,

    /**
     * this item is currently paused
     */
    Paused,

    Playing,
    Buffering,

    /**
     * State indicating this this item is currently in an error state
     */
    Error,
}

@Parcelize
data class PlayerError(val errorCode: Int, val errorMessage: String) : Parcelable