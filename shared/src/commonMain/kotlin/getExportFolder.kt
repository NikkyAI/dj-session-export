import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
fun getExportFolder(): Path {
    val homepath = getenv("USERPROFILE")?.toKString() ?: error("failed to get %USERPROFILE%")
    return homepath.toPath() / ".dj-session-export"
}