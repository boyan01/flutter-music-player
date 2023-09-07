package tech.soit.quiet.player

import android.os.Parcel
import android.os.Parcelable

data class PlaybackState constructor(
    val state: State,
    val position: Long,
    val bufferedPosition: Long,
    val speed: Float,
    val error: PlayerError?,
    val updateTime: Long,
    val duration: Long,
) : Parcelable {
    private constructor(source: Parcel) : this(
        State.values()[source.readInt()],
        source.readLong(),
        source.readLong(),
        source.readFloat(),
        source.readParcelable<PlayerError>(PlayerError::class.java.classLoader),
        source.readLong(),
        source.readLong(),
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "state" to state.ordinal,
            "position" to position,
            "bufferedPosition" to bufferedPosition,
            "speed" to speed,
            "error" to error?.toMap(),
            "updateTime" to updateTime,
            "duration" to duration,
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
        writeLong(duration)
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

data class PlayerError(val errorCode: Int, val errorMessage: String) : Parcelable {

    companion object {

        // see lib/model/playback_error.dart ErrorType.source
        const val TYPE_SOURCE = 0

        // see lib/model/playback_error.dart ErrorType.renderer
        const val TYPE_RENDERER = 1

        // see lib/model/playback_error.dart ErrorType.unknown
        const val TYPE_UNKNOWN = 2

        @JvmField
        val CREATOR: Parcelable.Creator<PlayerError> = object : Parcelable.Creator<PlayerError> {
            override fun createFromParcel(parcel: Parcel): PlayerError {
                return PlayerError(parcel)
            }

            override fun newArray(size: Int): Array<PlayerError?> {
                return arrayOfNulls(size)
            }
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!
    ) {
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "type" to 0,
            "message" to errorMessage
        )
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(errorCode)
        parcel.writeString(errorMessage)
    }

    override fun describeContents(): Int {
        return 0
    }


}
