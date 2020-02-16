package tech.soit.quiet.player

import android.os.Parcel
import android.os.Parcelable

data class MusicMetadata constructor(val obj: Map<String, Any?>) : Parcelable {

    val mediaId: String get() = obj["mediaId"] as String

    val title: String? get() = obj["title"] as String?

    val subtitle: String? get() = obj["subtitle"] as String

    val duration: Long? get() = (obj["duration"] as Number?)?.toLong()

    val iconUri: String? get() = obj["iconUri"] as String?

    val mediaUri: String? get() = obj["mediaUri"] as String?

    constructor(source: Parcel) : this(
            mutableMapOf<String, Any?>().apply {
                source.readMap(this, MusicMetadata::class.java.classLoader)
            }.toMap()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeMap(obj)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MusicMetadata> =
                object : Parcelable.Creator<MusicMetadata> {
                    override fun createFromParcel(source: Parcel): MusicMetadata = MusicMetadata(source)
                    override fun newArray(size: Int): Array<MusicMetadata?> = arrayOfNulls(size)
                }
    }
}