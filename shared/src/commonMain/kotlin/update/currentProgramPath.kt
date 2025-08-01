package update

import okio.Path.Companion.toPath
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform.programName

@OptIn(ExperimentalNativeApi::class)
val currentProgramPath = programName?.toPath() ?: error("cannot find exe")
