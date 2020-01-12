package tech.soit.quiet.player

import android.os.Parcel
import android.os.Parcelable
import tech.soit.quiet.ffi.DartMapObject

data class PlayQueue(val obj: DartMapObject) : Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        obj.writeToParcel(parcel, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PlayQueue> {
        override fun createFromParcel(parcel: Parcel): PlayQueue {
            val classLoader = PlayQueue::class.java.classLoader
            return PlayQueue(requireNotNull(parcel.readParcelable(classLoader)))
        }

        override fun newArray(size: Int): Array<PlayQueue?> {
            return arrayOfNulls(size)
        }
    }


}