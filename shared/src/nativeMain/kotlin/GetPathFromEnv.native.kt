import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.Path.Companion.toPath
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getPathFromEnv(key: String): okio.Path? {
    return getenv(key)?.toKString()?.toPath(true)
}