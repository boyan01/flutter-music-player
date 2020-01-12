package tech.soit.quiet.ffi

import android.os.Parcelable
import androidx.annotation.Keep
import tech.soit.quiet.BuildConfig
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

/**
 * Object adapter for Kotlin and Dart.
 */
@Keep
interface DartObject : Parcelable {

    companion object {

        private val supportedTypes = listOf(
            Number::class.createType(),
            String::class.createType(),
            Map::class.createType(),
            List::class.createType()
        )


        inline fun <reified T : DartObject> create(data: Map<String, Any?>): T {
            return create(T::class, data)
        }

        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        fun <T : DartObject> create(kClass: KClass<T>, data: Map<String, Any?>): T {
            require(kClass.isData) { "T is required kotlin data class , but is $kClass" }
            val constructor = requireNotNull(kClass.primaryConstructor)
            return constructor.call(constructor.parameters.map { parameter ->
                return@map when {
                    parameter.type.isSubtypeOf(DartObject::class.createType()) -> create(
                        parameter.type.classifier as KClass<DartObject>,
                        data[parameter.name] as Map<String, Any?>
                    )
                    parameter.type.isSubtypeOf(Enum::class.createType()) -> {
                        val javaType = parameter.type.javaType as Class<Enum<*>>
                        require(javaType.isEnum)
                        javaType.enumConstants.first { enum -> enum.ordinal == data[parameter.name] }

                    }
                    else -> {
                        if (BuildConfig.DEBUG) {
                            require(supportedTypes.contains(parameter.type)) {
                                "current type is unsupported : ${parameter.type}"
                            }
                        }
                        data[parameter.name]
                    }
                }
            })
        }

    }

    fun toMap(): Map<String, Any?> {
        require(this::class.isData) { "current object is not data class" }
        @Suppress("UNCHECKED_CAST")
        val properties = this::class.memberProperties as Collection<KProperty1<DartObject, Any>>
        return properties
            .filter { it.visibility == KVisibility.PUBLIC }
            .map { property: KProperty1<DartObject, *> ->
                return@map when {
                    property.returnType.isSubtypeOf(DartObject::class.createType()) -> {
                        property.name to DartObject::toMap.call(property(this))
                    }
                    property.returnType.isSubtypeOf(Enum::class.createType()) -> {
                        val enum = property(this) as Enum<*>
                        property.name to enum.ordinal
                    }
                    else -> {
                        if (BuildConfig.DEBUG) {
                            require(supportedTypes.contains(property.returnType)) {
                                "current type is unsupported : ${property.returnType}"
                            }
                        }
                        property.name to property(this)
                    }
                }
            }.toMap()
    }

}
