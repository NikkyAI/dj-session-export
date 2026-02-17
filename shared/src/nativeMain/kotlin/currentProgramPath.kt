import okio.Path
import okio.Path.Companion.toPath
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform.programName

@OptIn(ExperimentalNativeApi::class)
actual val currentProgramPath: Path = programName?.toPath() ?: error("cannot find exe")
