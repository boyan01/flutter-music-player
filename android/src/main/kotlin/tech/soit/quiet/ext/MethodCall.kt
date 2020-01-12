package tech.soit.quiet.ext

import io.flutter.plugin.common.MethodCall
import tech.soit.quiet.ffi.DartMapObject

val MethodCall.mapArguments: DartMapObject get() = DartMapObject(arguments())