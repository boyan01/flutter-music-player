package tech.soit.quiet

import android.os.Parcel
import android.os.Parcelable

class ArtworkData(
    val color: Int?,
    val image: ByteArray
) : Parcelable {

    constructor(source: Parcel) : this(
        source.readValue(Int::class.java.classLoader) as Int?,
        source.createByteArray()!!
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeValue(color)
        writeByteArray(image)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ArtworkData> = object : Parcelable.Creator<ArtworkData> {
            override fun createFromParcel(source: Parcel): ArtworkData = ArtworkData(source)
            override fun newArray(size: Int): Array<ArtworkData?> = arrayOfNulls(size)
        }
    }
}