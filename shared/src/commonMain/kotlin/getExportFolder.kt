import okio.Path

//@OptIn(ExperimentalForeignApi::class)
fun getExportFolder(): Path {
    val homepath = getPathFromEnv("USERPROFILE") ?: error("failed to get %USERPROFILE%")
    return homepath / ".dj-session-export"
}