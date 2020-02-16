package tech.soit.quiet.ffi

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import kotlin.reflect.KProperty

@Parcelize
data class DartMapObject(
    val map: Map<String, @RawValue Any?>
) : Parcelable, Map<String, Any?> by map {


    inline operator fun <reified T : Any?> getValue(any: Any, property: KProperty<*>): T {
        return map.getValue(any, property) as T
    }
}