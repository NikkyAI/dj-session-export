import okio.Path
import okio.Path.Companion.toPath

actual fun getPathFromEnv(key: String): Path? {
    return System.getenv(key)?.toPath(true)
}