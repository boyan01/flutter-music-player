package tech.soit.quiet.player

import android.os.Parcel
import android.os.Parcelable
import tech.soit.quiet.ffi.DartMapObject

data class MusicMetadata(val obj: DartMapObject) : Parcelable {

    val mediaId: String? by obj

    val title: String? by obj

    val subtitle: String? by obj

    val duration: Long? by obj

    val iconUri: String? by obj

    val mediaUri: String? by obj


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        obj.writeToParcel(parcel, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MusicMetadata> {
        override fun createFromParcel(parcel: Parcel): MusicMetadata {
            return MusicMetadata(
                requireNotNull(
                    parcel.readParcelable(
                        MusicMetadata::class.java.classLoader
                    )
                )
            )
        }

        override fun newArray(size: Int): Array<MusicMetadata?> {
            return arrayOfNulls(size)
        }
    }


}